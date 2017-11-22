/*
 * Copyright (C) 2017 TripNDroid Mobile Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tripndroid.optisound.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.tripndroid.optisound.R;
import com.tripndroid.optisound.activity.OptiSound;
import com.tripndroid.optisound.activity.opticontrols;
import com.tripndroid.optisound.service.HeadsetService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class SummariedListPreferenceWithCustom extends ListPreference
{
    public SummariedListPreferenceWithCustom(Context context, AttributeSet set)
    {
        super(context, set);
    }

    // Read file list from path
    public static void getFileNameList(File path, String fileExt, ArrayList<String> fileList)
    {
        if (path.isDirectory())
        {
            File[] files = path.listFiles();
            if (null == files) return;
            for (File file : files) getFileNameList(file, fileExt, fileList);
        }
        else
        {
            String filePath = path.getAbsolutePath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (fileName.toLowerCase(Locale.US).endsWith(fileExt))
                fileList.add(fileName);
        }
    }
    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        try
        {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                Log.i(OptiSound.TAG, "External storage not mounted");
                setEntries(new String[0]);
                setEntryValues(new String[0]);
                String tip = getContext().getResources().getString(R.string.text_ir_dir_isempty);
                tip = String.format(tip, OptiSound.impulseResponsePath);
                Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
                super.onPrepareDialogBuilder(builder);
                return;
            }
            final String kernelPath = OptiSound.impulseResponsePath;
            File kernelFile = new File(kernelPath);
            if (!kernelFile.exists())
            {
                Log.i(OptiSound.TAG, "Impulse response directory does not exists");
                kernelFile.mkdirs();
                kernelFile.mkdir();
            }
            ArrayList<String> kernelList = new ArrayList<String>();
            getFileNameList(kernelFile, ".irs", kernelList);
            getFileNameList(kernelFile, ".wav", kernelList);
            getFileNameList(kernelFile, ".flac", kernelList);
            if (kernelList.isEmpty())
            {
                String tip = getContext().getResources().getString(R.string.text_ir_dir_isempty);
                tip = String.format(tip, OptiSound.impulseResponsePath);
                Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
            }
            else Collections.sort(kernelList);
            final String[] kernelArray = new String[kernelList.size()];
            final String[] arrayValue = new String[kernelList.size()];
            for (int i = 0; i < kernelList.size(); i++)
            {
                kernelArray[i] = kernelList.get(i);
                arrayValue[i] = kernelPath + kernelList.get(i);
            }
            setEntries(kernelArray);
            setEntryValues(arrayValue);
            super.onPrepareDialogBuilder(builder);
        }
        catch (Exception e)
        {
            setEntries(new String[0]);
            setEntryValues(new String[0]);
            String tip = getContext().getResources().getString(R.string.text_ir_dir_isempty);
            tip = String.format(tip, OptiSound.impulseResponsePath);
            Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
            super.onPrepareDialogBuilder(builder);
        }
    }

    @Override
    public void setValue(String value)
    {
        super.setValue(value);
        try
        {
            CharSequence[] entries = getEntries();
            CharSequence[] entryValues = getEntryValues();
            if (entries == null || entryValues == null)
            {
                if (value == null)
                {
                    setSummary("");
                    return;
                }
                if (value.isEmpty())
                {
                    setSummary("");
                    return;
                }
                if (value.contains("/"))
                {
                    String fileName = value.substring(value.lastIndexOf("/") + 1);
                    setSummary(fileName);
                    return;
                }
                setSummary(value);
                return;
            }
            if(getKey().equals("dsp.convolver.resampler"))
            {
                if(HeadsetService.dspModuleSamplingRate > 0)
                {
                    for (int i = 0; i < entryValues.length; i++)
                    {
                        if (entryValues[i].equals(value))
                        {
                            Runnable runner = new ResamplerThread(OptiSound.impulseResponsePath, entries[i].toString(), HeadsetService.dspModuleSamplingRate);
                            new Thread(runner).start();
                            break;
                        }
                    }
                }
                else
                	Toast.makeText(OptiSound.actUi, OptiSound.actUi.getString(R.string.resamplererror), Toast.LENGTH_LONG).show();
            }
            else
            {
                for (int i = 0; i < entryValues.length; i++)
                {
                    if (entryValues[i].equals(value))
                    {
                        setSummary(entries[i]);
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            setSummary("");
        }
    }

    public void refreshFromPreference()
    {
        onSetInitialValue(true, null);
    }
}
class ResamplerThread implements Runnable {
	String path;
	String filename;
	int tarSmpRate;
	   ResamplerThread(String p, String f, int s) {
	       path = p;
	       filename = f;
	       tarSmpRate = s;
	   }
	   public void run() {
           final String finalName = opticontrols.OfflineAudioResample(path, filename, tarSmpRate);
           new Handler(Looper.getMainLooper()).post(new Runnable() {
        	    @Override
        	    public void run() {
        	           File f = new File(finalName);
        	           if(f.exists() && !f.isDirectory())
        	           	Toast.makeText(OptiSound.actUi, OptiSound.actUi.getString(R.string.resamplerstr, tarSmpRate, finalName), Toast.LENGTH_LONG).show();
        	    }
        	});
	   }
	}
