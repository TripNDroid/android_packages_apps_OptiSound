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
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SummariedListPreference extends ListPreference
{
    public SummariedListPreference(Context context, AttributeSet set)
    {
        super(context, set);
    }

    @Override
    public void setValue(String value)
    {
        super.setValue(value);
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();
        for (int i = 0; i < entryValues.length; i++)
        {
            if (entryValues[i].equals(value))
            {
                setSummary(entries[i]);
                break;
            }
        }
    }

    public void refreshFromPreference()
    {
        onSetInitialValue(true, null);
    }
}
