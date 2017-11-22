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

package com.tripndroid.optisound.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.tripndroid.optisound.service.HeadsetService;

/**
 * This receiver starts our {@link HeadsetService} after system boot. Since
 * Android 2.3, we will always need a persistent process, because we are forced
 * to keep track of all open audio sessions.
 *
 * @Co-founder alankila
 */
public class BootCompletedReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        context.startService(new Intent(context, HeadsetService.class));
    }
}
