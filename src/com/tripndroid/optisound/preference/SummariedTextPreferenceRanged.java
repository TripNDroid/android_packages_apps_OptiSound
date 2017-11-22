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

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import com.tripndroid.optisound.R;
import com.tripndroid.optisound.activity.OptiSound;

public class SummariedTextPreferenceRanged extends EditTextPreference
{
    public SummariedTextPreferenceRanged(Context context, AttributeSet set)
    {
        super(context, set);
    }
    @Override
    public void setText(String value)
    {
    	String key = getKey();
    	float valueFloat = 0;
    	try
    	{
            valueFloat = Float.parseFloat(value);
    	}
    	catch(NumberFormatException e)
    	{
    	}
    	if(key.equals("dsp.masterswitch.finalgain") || key.equals("dsp.headphone.tailverb"))
    	{
            if(valueFloat < 1)
            	value = "1";
            if(valueFloat > 200)
            	value = "200";
            setSummary(value);
    	}
    	if(key.equals("dsp.compression.threshold"))
    	{
            if(valueFloat < -80)
            	valueFloat = -80;
            if(valueFloat > 0)
            	valueFloat = 0;
            setSummary(value+" dB");
            value = Integer.toString((int)valueFloat);
    	}
    	if(key.equals("dsp.compression.knee"))
    	{
            if(valueFloat < 0)
            	value = "0";
            if(valueFloat > 40)
            	value = "40";
            setSummary(value+" dB");
    	}
    	if(key.equals("dsp.compression.ratio"))
    	{
    		if(valueFloat == 0)
    			value = "1";
            if(valueFloat < -20)
            	value = "-20";
            if(valueFloat > 20)
            	value = "20";
            setSummary("1:"+value);
    	}
    	if(key.equals("dsp.compression.attack") || key.equals("dsp.compression.release"))
    	{
            if(valueFloat < 0.00001)
            	value = "0.00001";
            if(valueFloat > 0.99999)
            	value = "0.99999";
            setSummary(value);
    	}
    	if(key.equals("dsp.convolver.normalise"))
    	{
            if(valueFloat < 0.00001)
            	value = "0.00001";
            if(valueFloat > 0.99999)
            	value = "0.99999";
            setSummary(Float.toString(valueFloat*100)+"%");
    	}
    	if(key.equals("dsp.bass.freq"))
    	{
            if(valueFloat < 40)
            	value = "40";
            if(valueFloat > 250)
            	value = "250";
            setSummary(value+"Hz");
    	}
    	if(key.equals("dsp.headphone.roomsize") || key.equals("dsp.headphone.reverbtime"))
    	{
            if(valueFloat < 5)
            	value = "5";
            if(valueFloat > 300)
            	value = "300";
            setSummary(value+"m");
    	}
    	if(key.equals("dsp.headphone.damping"))
    	{
            if(valueFloat < 1)
            	value = "1";
            if(valueFloat > 100)
            	value = "100";
            setSummary(value+"%");
    	}
    	if(key.equals("dsp.headphone.inbandwidth"))
    	{
            if(valueFloat < 1)
            	value = "1";
            if(valueFloat > 150)
            	value = "150";
            setSummary(value+OptiSound.actUi.getString(R.string.hfcomponents));
    	}
    	if(key.equals("dsp.headphone.earlyverb"))
    	{
            if(valueFloat < 1)
            	value = "1";
            if(valueFloat > 100)
            	value = "100";
            setSummary(value);
    	}
    	if(key.equals("dsp.analogmodelling.tubedrive"))
    	{
            if(valueFloat < 0)
            	value = "0";
            if(valueFloat > 12)
            	value = "12";
            setSummary(value);
    	}
    	if(key.equals("dsp.analogmodelling.tubebass") || key.equals("dsp.analogmodelling.tubemid") || key.equals("dsp.analogmodelling.tubetreble"))
    	{
            if(valueFloat < 0)
            	value = "0";
            if(valueFloat > 10)
            	value = "10";
            setSummary(value);
    	}
    	if(key.equals("dsp.analogmodelling.tubetonestack"))
    	{
            if(valueFloat < 0)
            	value = "0";
            if(valueFloat > 24)
            	value = "24";
            setSummary(value);
    	}	 
        super.setText(value);
    }

    public void refreshFromPreference()
    {
        onSetInitialValue(true, null);
    }
}
