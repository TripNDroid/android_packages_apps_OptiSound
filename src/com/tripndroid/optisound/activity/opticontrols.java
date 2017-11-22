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

import android.util.Log;

public class opticontrols {
	
  static {
    try {
    	System.load("/system/lib/libopticontrols.so");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load.\n" + e);
      System.exit(1);
    }
  }

    /* Impulse Response Utils */
    public static native int[] GetLoadImpulseResponseInfo(String path);
    public static native int[] ReadImpulseResponseToInt(int targetSampleRate);
    public static native String OfflineAudioResample(String path, String filename, int targetSampleRate);

}
