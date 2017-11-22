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

package com.tripndroid.optisound.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.tripndroid.optisound.R;
import com.tripndroid.optisound.preference.EqualizerPreference;
import com.tripndroid.optisound.preference.SummariedListPreference;
import com.tripndroid.optisound.service.HeadsetService;

public final class DSPScreen extends PreferenceFragment
{
    protected static final String TAG = DSPScreen.class.getSimpleName();

    private final OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            /* If the listpref is updated, copy the changed setting to the eq. */
            if ("dsp.tone.eq".equals(key))
            {
                String newValue = sharedPreferences.getString(key, null);
                if (!"custom".equals(newValue))
                {
                    Editor e = sharedPreferences.edit();
                    e.putString("dsp.tone.eq.custom", newValue);
                    e.commit();
                    /* Now tell the equalizer that it must display something else. */
                    EqualizerPreference eq = (EqualizerPreference)
                                             getPreferenceScreen().findPreference("dsp.tone.eq.custom");
                    eq.refreshFromPreference();
                }
            }
            /* If the equalizer surface is updated, select matching pref entry or "custom". */
            if ("dsp.tone.eq.custom".equals(key))
            {
                String newValue = sharedPreferences.getString(key, null);
                String desiredValue = "custom";
                SummariedListPreference preset = (SummariedListPreference)
                                                 getPreferenceScreen().findPreference("dsp.tone.eq");
                for (CharSequence entry : preset.getEntryValues())
                {
                    if (entry.equals(newValue))
                    {
                        desiredValue = newValue;
                        break;
                    }
                }
                /* Tell listpreference that it must display something else. */
                if (!desiredValue.equals(preset.getEntry()))
                {
                    Editor e = sharedPreferences.edit();
                    e.putString("dsp.tone.eq", desiredValue);
                    e.commit();
                    preset.refreshFromPreference();
                }
            }
            if (!"dsp.convolver.resampler".equals(key))
            	getActivity().sendBroadcast(new Intent(OptiSound.ACTION_UPDATE_PREFERENCES));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String config = getArguments().getString("config");
        getPreferenceManager().setSharedPreferencesName(
            OptiSound.SHARED_PREFERENCES_BASENAME + "." + config);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
        try
        {
            int xmlId = R.xml.class.getField(config + "_preferences").getInt(null);
            addPreferencesFromResource(xmlId);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        getPreferenceManager().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getPreferenceManager().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
