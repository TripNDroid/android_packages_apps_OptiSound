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

package com.tripndroid.optisound.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tripndroid.optisound.R;
import com.tripndroid.optisound.activity.opticontrols;
import com.tripndroid.optisound.activity.OptiSound;

/**
* <p>This calls listen to events that affect DSP function and responds to them.</p>
* <ol>
* <li>new audio session declarations</li>
* <li>headset plug / unplug events</li>
* <li>preference update events.</li>
* </ol>
*
* @Co-founder alankila
*/
public class HeadsetService extends Service
{
	/**
	* Helper class representing the full complement of effects attached to one
	* audio session.
	*
	* @Co-founder alankila
	*/
	Bitmap iconLarge;

	public static int modeEffect;

	public final static UUID EFFECT_TYPE_CUSTOM = UUID.fromString("ee1a4bcb-c787-4bf1-a079-07fd35e3d9a7");
	public final static UUID EFFECT_OPTISOUND = UUID.fromString("d46ca451-be8f-4d6c-a2a3-52926c3a0439");

	public class OSModule {

		public AudioEffect OptiSOUND;

		public OSModule(int sessionId) {
			try
			{
				/*
				* AudioEffect constructor is not part of SDK. We use reflection
				* to access it.
				*/
				OptiSOUND = AudioEffect.class.getConstructor(UUID.class,
					UUID.class, Integer.TYPE, Integer.TYPE).newInstance(
						EFFECT_TYPE_CUSTOM, EFFECT_OPTISOUND, 0, sessionId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			if (OptiSound.devMsgDisplay) {
				if (OptiSOUND != null) {
					AudioEffect.Descriptor dspDescriptor = OptiSOUND.getDescriptor();
					if (dspDescriptor.uuid.equals(EFFECT_OPTISOUND))
						Toast.makeText(HeadsetService.this, "Effect name: " + dspDescriptor.name + "\nType id: " + dspDescriptor.type
							+ "\nUnique id: " + dspDescriptor.uuid + "\nImplementor: " + dspDescriptor.implementor, Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(HeadsetService.this, "Effect load failed!\n", Toast.LENGTH_SHORT).show();
				}
			}
		}

		public void release() {
			OptiSOUND.release();
		}

		/**
		* Proxies call to AudioEffect.setParameter(byte[], byte[]) which is
		* available via reflection.
		*
		* @param audioEffect
		* @param parameter
		* @param value
		*/
		private byte[] IntToByte(int[] input)
		{
			int int_index, byte_index;
			int iterations = input.length;
			byte[] buffer = new byte[input.length * 4];
			int_index = byte_index = 0;
			for (; int_index != iterations;)
			{
				buffer[byte_index] = (byte)(input[int_index] & 0x00FF);
				buffer[byte_index + 1] = (byte)((input[int_index] & 0xFF00) >> 8);
				buffer[byte_index + 2] = (byte)((input[int_index] & 0xFF0000) >> 16);
				buffer[byte_index + 3] = (byte)((input[int_index] & 0xFF000000) >> 24);
				++int_index;
				byte_index += 4;
			}
			return buffer;
		}
		private int byteArrayToInt(byte[] encodedValue)
		{
			int value = (encodedValue[3] << 24);
			value |= (encodedValue[2] & 0xFF) << 16;
			value |= (encodedValue[1] & 0xFF) << 8;
			value |= (encodedValue[0] & 0xFF);
			return value;
		}
		private void setParameterIntArray(AudioEffect audioEffect, int parameter, int value[])
		{
			try
			{
				byte[] arguments = new byte[]
				{
					(byte)(parameter), (byte)(parameter >> 8),
					(byte)(parameter >> 16), (byte)(parameter >> 24)
				};
				byte[] result = IntToByte(value);
				Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class, byte[].class);
				setParameter.invoke(audioEffect, arguments, result);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		private void setParameterInt(AudioEffect audioEffect, int parameter, int value)
		{
			try
			{
				byte[] arguments = new byte[]
				{
					(byte)(parameter), (byte)(parameter >> 8),
					(byte)(parameter >> 16), (byte)(parameter >> 24)
				};
				byte[] result = new byte[]
				{
					(byte)(value), (byte)(value >> 8),
					(byte)(value >> 16), (byte)(value >> 24)
				};
				Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class, byte[].class);
				setParameter.invoke(audioEffect, arguments, result);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		private void setParameterShort(AudioEffect audioEffect, int parameter, short value)
		{
			try
			{
				byte[] arguments = new byte[]
				{
					(byte)(parameter), (byte)(parameter >> 8),
					(byte)(parameter >> 16), (byte)(parameter >> 24)
				};
				byte[] result = new byte[]
				{
					(byte)(value), (byte)(value >> 8)
				};
				Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class, byte[].class);
				setParameter.invoke(audioEffect, arguments, result);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		private int getParameter(AudioEffect audioEffect, int parameter)
		{
			try
			{
				byte[] arguments = new byte[]
				{
					(byte)(parameter), (byte)(parameter >> 8),
					(byte)(parameter >> 16), (byte)(parameter >> 24)
				};
				byte[] result = new byte[4];
				Method getParameter = AudioEffect.class.getMethod("getParameter", byte[].class, byte[].class);
				getParameter.invoke(audioEffect, arguments, result);
				return byteArrayToInt(result);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	public class LocalBinder extends Binder
	{
		public HeadsetService getService()
		{
			return HeadsetService.this;
		}
	}

	private final LocalBinder mBinder = new LocalBinder();

	/**
	* Known audio sessions and their associated audioeffect suites.
	*/
	private final Map<Integer, OSModule> mAudioSessions = new HashMap<Integer, OSModule>();

	/**
	* Is a wired headset plugged in?
	*/
	public static boolean mUseHeadset;

	/**
	* Is bluetooth headset plugged in?
	*/
	public static boolean mUseBluetooth;

	private float[] mOverriddenEqualizerLevels;

	private int[] eqLevels = new int[10];
	/**
	* Receive new broadcast intents for adding DSP to session
	*/
	private String oldImpulseName = "";
	private float oldQuality = 0;
	private int oldnormalise = 0;
	private int forcedShrink = 0;

	public static OSModule OptiSOUNDGbEf;
	private SharedPreferences preferencesMode;
	public static int dspModuleSamplingRate = 0;
	private final BroadcastReceiver mAudioSessionReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
		if (sessionId == 0)
			return;
		if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION))
		{
			//if (modeEffect == 0)
				//return;
			if (!mAudioSessions.containsKey(sessionId)) {
				OSModule fxId = new OSModule(sessionId);
				if (fxId.OptiSOUND == null)
				{
					Log.e(OptiSound.TAG, "Audio session load fail");
					fxId.release();
					fxId = null;
				}
				else
					mAudioSessions.put(sessionId, fxId);
				updateDsp(false, true);
			}
		}
		if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION))
		{
			OSModule gone = mAudioSessions.remove(sessionId);
			if (gone != null)
				gone.release();
			gone = null;
		}
	}
	};
	/**
	* Update audio parameters when preferences have been updated.
	*/
	private final BroadcastReceiver mPreferenceUpdateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
	{
		updateDsp(false, true);
	}
	};

	/**
	* This code listens for changes in bluetooth and headset events. It is
	* adapted from google's own MusicFX application, so it's presumably the
	* most correct design there is for this problem.
	*/
	private final BroadcastReceiver mRoutingReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
	{
		final String action = intent.getAction();
		final boolean prevUseHeadset = mUseHeadset;
		if (action.equals(AudioManager.ACTION_HEADSET_PLUG))
			mUseHeadset = intent.getIntExtra("state", 0) == 1;
		if (prevUseHeadset != mUseHeadset)
			updateDsp(true, false);
	}
	};

	private final BroadcastReceiver mBtReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
	{
		final String action = intent.getAction();
		if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED))
		{
			int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
				BluetoothProfile.STATE_CONNECTED);
			if (state == BluetoothProfile.STATE_CONNECTED && !mUseBluetooth)
			{
				mUseBluetooth = true;
				updateDsp(true, false);
			}
			else if (mUseBluetooth)
			{
				mUseBluetooth = false;
				updateDsp(true, false);
			}
		}
		else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
		{
			String stateExtra = BluetoothAdapter.EXTRA_STATE;
			int state = intent.getIntExtra(stateExtra, -1);
			if (state == BluetoothAdapter.STATE_OFF && mUseBluetooth)
			{
				mUseBluetooth = false;
				updateDsp(true, false);
			}
		}
	}
	};
	private void foregroundPersistent(String mFXType)
	{
		int mIconIDSmall = getResources().getIdentifier("ic_stat_icon", "drawable", getApplicationInfo().packageName);
		Intent notificationIntent = new Intent(this, OptiSound.class);
		PendingIntent contentItent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification statusNotify = new Notification.Builder(this)
			.setPriority(Notification.PRIORITY_MIN)
			.setAutoCancel(false)
			.setOngoing(true)
			.setDefaults(0)
			.setWhen(System.currentTimeMillis())
			.setSmallIcon(mIconIDSmall)
			.setLargeIcon(iconLarge)
			.setTicker("OptiSound " + mFXType)
			.setContentTitle("OptiSound controls the " + mFXType + " output")
			.setContentText("")
			.setContentIntent(contentItent)
			.build();
		startForeground(1, statusNotify);
	}
	@Override
	public void onCreate()
	{
		super.onCreate();
		IntentFilter audioFilter = new IntentFilter();
		audioFilter.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
		audioFilter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		registerReceiver(mAudioSessionReceiver, audioFilter);
		final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
		registerReceiver(mRoutingReceiver, intentFilter);
		registerReceiver(mPreferenceUpdateReceiver, new IntentFilter(OptiSound.ACTION_UPDATE_PREFERENCES));
		final IntentFilter btFilter = new IntentFilter();
		btFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		btFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBtReceiver, btFilter);
		iconLarge = BitmapFactory.decodeResource(getResources(), R.drawable.eq);
		preferencesMode = getSharedPreferences(OptiSound.SHARED_PREFERENCES_BASENAME + "." + "settings", 0);
		if (!preferencesMode.contains("dsp.app.modeEffect"))
			preferencesMode.edit().putInt("dsp.app.modeEffect", 1).commit();
		//modeEffect = preferencesMode.getInt("dsp.app.modeEffect", 0);
		if (OptiSOUNDGbEf != null)
		{
			OptiSOUNDGbEf.release();
			OptiSOUNDGbEf = null;
		}
		/*if (modeEffect == 0)
		{
			if (OptiSOUNDGbEf == null)
				OptiSOUNDGbEf = new OSModule(0);
			if (OptiSOUNDGbEf.OptiSOUND == null)
			{
				Toast.makeText(HeadsetService.this, "Library load failed(Global effect)", Toast.LENGTH_SHORT).show();
				OptiSOUNDGbEf.release();
				OptiSOUNDGbEf = null;
			}
		}*/
		updateDsp(true, false);
		String arch = System.getProperty("os.arch");
		if(arch.contains("arm"))
			forcedShrink = 1;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(mAudioSessionReceiver);
		unregisterReceiver(mRoutingReceiver);
		unregisterReceiver(mPreferenceUpdateReceiver);
		stopForeground(true);
		if (iconLarge != null)
		{
			iconLarge.recycle();
			iconLarge = null;
		}
		mAudioSessions.clear();
		if (OptiSOUNDGbEf != null)
			OptiSOUNDGbEf.release();
		OptiSOUNDGbEf = null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		//modeEffect = preferencesMode.getInt("dsp.app.modeEffect", 0);
		/*if (modeEffect == 0)
		{
			if (OptiSOUNDGbEf == null) {
				OptiSOUNDGbEf = new OSModule(0);
				if (OptiSOUNDGbEf.OptiSOUND == null) {
					Log.e(OptiSound.TAG, "Global audio session load fail, reload it now!");
					OptiSOUNDGbEf.release();
					OptiSOUNDGbEf = null;
					return super.onStartCommand(intent, flags, startId);
				}
				updateDsp(false, true);
				return super.onStartCommand(intent, flags, startId);
			}
			if (OptiSOUNDGbEf.OptiSOUND == null) {
				Log.e(OptiSound.TAG, "Global audio session load fail, reload it now!");
				OptiSOUNDGbEf.release();
				OptiSOUNDGbEf = new OSModule(0);
				if (OptiSOUNDGbEf.OptiSOUND == null) {
					Log.e(OptiSound.TAG, "Global audio session load fail, reload it now!");
					OptiSOUNDGbEf.release();
					OptiSOUNDGbEf = null;
					return super.onStartCommand(intent, flags, startId);
				}
				return super.onStartCommand(intent, flags, startId);
			}
			Log.i(OptiSound.TAG, "Global audio session created!");
			updateDsp(false, true);
		}*/
		return super.onStartCommand(intent, flags, startId);
	}
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	/**
	* Gain temporary control over the global equalizer.
	* Used by OptiSound when testing a new equalizer setting.
	*
	* @param levels
	*/
	public void setEqualizerLevels(float[] levels)
	{
		mOverriddenEqualizerLevels = levels;
		updateDsp(false, false);
	}

	public static String getAudioOutputRouting()
	{
		if (mUseBluetooth)
			return "bluetooth";
		if (mUseHeadset)
			return "headset";
		return "speaker";
	}

	/**
	* Push new configuration to audio stack.
	*/
	protected void updateDsp(boolean notify, boolean updateConvolver)
	{
		//modeEffect = preferencesMode.getInt("dsp.app.modeEffect", 0);
		final String mode = getAudioOutputRouting();
		SharedPreferences preferences = getSharedPreferences(OptiSound.SHARED_PREFERENCES_BASENAME + "." + mode, 0);
		if (notify)
		{
			if (mode == "bluetooth")
				foregroundPersistent(getString(R.string.bluetooth_title));
			else if (mode == "headset")
				foregroundPersistent(getString(R.string.headset_title));
			else
				foregroundPersistent(getString(R.string.speaker_title));
			Intent intent = new Intent("dsp.activity.updatePage");
			sendBroadcast(intent);
		}
		/*if (modeEffect == 0)
		{
			try
			{
				updateDsp(preferences, OptiSOUNDGbEf, updateConvolver, 0);
			}
			catch (Exception e)
			{
			}
		}
		else
		{*/
			for (Integer sessionId : new ArrayList<Integer>(mAudioSessions.keySet()))
			{
				try
				{
					updateDsp(preferences, mAudioSessions.get(sessionId), updateConvolver, sessionId);
				}
				catch (Exception e)
				{
					mAudioSessions.remove(sessionId);
				}
			}
		//}
	}

	private void updateDsp(SharedPreferences preferences, OSModule session, boolean updateMajor, int sessionId)
	{
		boolean masterSwitch = preferences.getBoolean("dsp.masterswitch.enable", false);
		session.OptiSOUND.setEnabled(masterSwitch);
		if (masterSwitch)
		{
			int compressorEnabled = preferences.getBoolean("dsp.compression.enable", false) ? 1 : 0;
			int bassBoostEnabled = preferences.getBoolean("dsp.bass.enable", false) ? 1 : 0;
			int equalizerEnabled = preferences.getBoolean("dsp.tone.enable", false) ? 1 : 0;
			int reverbEnabled = preferences.getBoolean("dsp.headphone.enable", false) ? 1 : 0;
			int stereoWideEnabled = preferences.getBoolean("dsp.stereowide.enable", false) ? 1 : 0;
			int convolverEnabled = preferences.getBoolean("dsp.convolver.enable", false) ? 1 : 0;
			int analogModelEnabled = preferences.getBoolean("dsp.analogmodelling.enable", false) ? 1 : 0;
			dspModuleSamplingRate = session.getParameter(session.OptiSOUND, 20000);
			if (dspModuleSamplingRate == 0)
			{
				Toast.makeText(HeadsetService.this, R.string.dspcrashed, Toast.LENGTH_LONG).show();
				return;
			}
			session.setParameterShort(session.OptiSOUND, 1500, Short.valueOf(preferences.getString("dsp.masterswitch.clipmode", "1")));
			session.setParameterShort(session.OptiSOUND, 1501, Short.valueOf(preferences.getString("dsp.masterswitch.finalgain", "100")));
			if (compressorEnabled == 1 && updateMajor)
			{
				session.setParameterShort(session.OptiSOUND, 100, Short.valueOf(preferences.getString("dsp.compression.pregain", "12")));
				session.setParameterShort(session.OptiSOUND, 101, (short)Math.abs(Short.valueOf(preferences.getString("dsp.compression.threshold", "-60"))));
				session.setParameterShort(session.OptiSOUND, 102, Short.valueOf(preferences.getString("dsp.compression.knee", "30")));
				session.setParameterShort(session.OptiSOUND, 103, Short.valueOf(preferences.getString("dsp.compression.ratio", "12")));
				session.setParameterShort(session.OptiSOUND, 104, (short)(Float.valueOf((preferences.getString("dsp.compression.attack", "0.01")))*1000));
				session.setParameterShort(session.OptiSOUND, 105, (short)(Float.valueOf((preferences.getString("dsp.compression.release", "0.01")))*1000));
			}
			session.setParameterShort(session.OptiSOUND, 1200, (short)compressorEnabled); // Compressor switch
			if (bassBoostEnabled == 1 && updateMajor)
			{
				session.setParameterShort(session.OptiSOUND, 112, Short.valueOf(preferences.getString("dsp.bass.mode", "80")));
				short filtertype = Short.valueOf(preferences.getString("dsp.bass.filtertype", "0"));
				session.setParameterShort(session.OptiSOUND, 113, filtertype);
				short freq = Short.valueOf(preferences.getString("dsp.bass.freq", "55"));
				session.setParameterShort(session.OptiSOUND, 114, freq);
				if(filtertype == 1 && freq < 80)
					if(OptiSound.devMsgDisplay)
						Toast.makeText(HeadsetService.this, R.string.preferredlpf, Toast.LENGTH_SHORT).show();
			}
			session.setParameterShort(session.OptiSOUND, 1201, (short)bassBoostEnabled); // Bass boost switch
			if (equalizerEnabled == 1)
			{
				/* Equalizer state is in a single string preference with all values separated by ; */
				if (mOverriddenEqualizerLevels != null)
				{
					for (short i = 0; i < mOverriddenEqualizerLevels.length; i++)
						eqLevels[i] = Math.round(mOverriddenEqualizerLevels[i] * 10000);
				}
				else
				{
					String[] levels = preferences.getString("dsp.tone.eq.custom", "0;0;0;0;0;0;0;0;0;0").split(";");
					for (short i = 0; i < levels.length; i++)
						eqLevels[i] = Math.round(Float.valueOf(levels[i]) * 10000);
				}
				session.setParameterIntArray(session.OptiSOUND, 115, eqLevels);
			}
			session.setParameterShort(session.OptiSOUND, 1202, (short)equalizerEnabled); // Equalizer switch
			if (reverbEnabled == 1 && updateMajor)
			{
				session.setParameterShort(session.OptiSOUND, 127, Short.valueOf(preferences.getString("dsp.headphone.modeverb", "1")));
				session.setParameterShort(session.OptiSOUND, 128, Short.valueOf(preferences.getString("dsp.headphone.preset", "0")));
				session.setParameterShort(session.OptiSOUND, 129, Short.valueOf(preferences.getString("dsp.headphone.roomsize", "50")));
				session.setParameterShort(session.OptiSOUND, 130, Short.valueOf(preferences.getString("dsp.headphone.reverbtime", "50")));
				session.setParameterShort(session.OptiSOUND, 131, Short.valueOf(preferences.getString("dsp.headphone.damping", "50")));
				session.setParameterShort(session.OptiSOUND, 133, Short.valueOf(preferences.getString("dsp.headphone.inbandwidth", "80")));
				session.setParameterShort(session.OptiSOUND, 134, Short.valueOf(preferences.getString("dsp.headphone.earlyverb", "50")));
				session.setParameterShort(session.OptiSOUND, 135, Short.valueOf(preferences.getString("dsp.headphone.tailverb", "50")));
			}
			session.setParameterShort(session.OptiSOUND, 1203, (short)reverbEnabled); // Reverb switch
			if (stereoWideEnabled == 1 && updateMajor)
				session.setParameterShort(session.OptiSOUND, 137, Short.valueOf(preferences.getString("dsp.stereowide.mode", "0")));
			session.setParameterShort(session.OptiSOUND, 1204, (short)stereoWideEnabled); // Stereo widener switch
			if (convolverEnabled == 1 && updateMajor)
			{
				String mConvIRFilePath = preferences.getString("dsp.convolver.files", "");
				float quality = Float.valueOf(preferences.getString("dsp.convolver.quality", "1"));
				int normalise = (int) (Float.valueOf((preferences.getString("dsp.convolver.normalise", "0.2")))*1000);
				if(oldImpulseName.equals(mConvIRFilePath) && oldQuality == quality && oldnormalise == normalise)
					return;
				oldImpulseName = mConvIRFilePath;
				oldQuality = quality;
				oldnormalise = normalise;
				session.setParameterShort(session.OptiSOUND, 1205, (short)0);
				String mConvIRFileName = mConvIRFilePath.replace(OptiSound.impulseResponsePath, "");
				int[] impinfo = opticontrols.GetLoadImpulseResponseInfo(mConvIRFilePath);
				if (impinfo == null)
					Toast.makeText(HeadsetService.this, R.string.impfilefault, Toast.LENGTH_SHORT).show();
				else
				{
					if (impinfo[2] != dspModuleSamplingRate)
						Toast.makeText(HeadsetService.this, getString(R.string.unmatchedsamplerate, mConvIRFilePath, impinfo[2], dspModuleSamplingRate), Toast.LENGTH_LONG).show();
					int[] impulseResponse = opticontrols.ReadImpulseResponseToInt(dspModuleSamplingRate);
					if (impulseResponse == null)
						Toast.makeText(HeadsetService.this, R.string.memoryallocatefail, Toast.LENGTH_LONG).show();
					else
					{
						int arraySize2Send = 4096;
						impinfo[1] = impulseResponse.length / impinfo[0];
						int impulseCutted = (int)(impulseResponse.length * quality);
						if (impinfo[0] == 2 && impulseCutted > 800000 && forcedShrink == 1)
							impulseCutted = 800000;
						if (impinfo[0] == 4 && impulseCutted > 131072 && forcedShrink == 1)
							impulseCutted = 131072;
						int[] sendArray = new int[arraySize2Send];
						int numTime2Send = (int)Math.ceil((double)impulseCutted / arraySize2Send); // Send number of times that have to send
						int[] sendBufInfo = new int[] {impulseCutted, impinfo[0], normalise, numTime2Send};
						session.setParameterIntArray(session.OptiSOUND, 9999, sendBufInfo); // Send buffer info for module to allocate memory
						int[] finalArray = new int[numTime2Send*arraySize2Send]; // Fill final array with zero padding
						System.arraycopy(impulseResponse, 0, finalArray, 0, impulseCutted);
						for (int i = 0; i < numTime2Send; i++)
						{
							System.arraycopy(finalArray, arraySize2Send * i, sendArray, 0, arraySize2Send);
							session.setParameterShort(session.OptiSOUND, 10003, (short)i); // Increment sliced buffer
							session.setParameterIntArray(session.OptiSOUND, 12000, sendArray); // Commit buffer
						}
						session.setParameterShort(session.OptiSOUND, 10004, (short)1); // Notify send array completed and resize array in native side
						if (OptiSound.devMsgDisplay)
						{
							if (impinfo[0] == 1)
								Toast.makeText(HeadsetService.this, getString(R.string.convolversuccess, mConvIRFileName, impinfo[2], getString(R.string.mono_conv), impinfo[1], (int)impulseCutted), Toast.LENGTH_SHORT).show();
							else if (impinfo[0] == 2)
								Toast.makeText(HeadsetService.this, getString(R.string.convolversuccess, mConvIRFileName, impinfo[2], getString(R.string.stereo_conv), impinfo[1], (int)impulseCutted / 2), Toast.LENGTH_SHORT).show();
							else if (impinfo[0] == 4)
								Toast.makeText(HeadsetService.this, getString(R.string.convolversuccess, mConvIRFileName, impinfo[2], getString(R.string.fullstereo_conv), impinfo[1], (int)impulseCutted / 4), Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
			else
			{
				oldImpulseName = "";
				oldQuality = 0;
				oldnormalise = 0;
			}
			session.setParameterShort(session.OptiSOUND, 1205, (short)convolverEnabled); // Convolver switch
			if (analogModelEnabled == 1 && updateMajor)
			{
				session.setParameterShort(session.OptiSOUND, 150, (short) (Float.valueOf(preferences.getString("dsp.analogmodelling.tubedrive", "2"))*1000));
				session.setParameterShort(session.OptiSOUND, 151, (short) (Float.valueOf(preferences.getString("dsp.analogmodelling.tubebass", "8"))*1000));
				session.setParameterShort(session.OptiSOUND, 152, (short) (Float.valueOf(preferences.getString("dsp.analogmodelling.tubemid", "5.6"))*1000));
				session.setParameterShort(session.OptiSOUND, 153, (short) (Float.valueOf(preferences.getString("dsp.analogmodelling.tubetreble", "4.5"))*1000));
				session.setParameterShort(session.OptiSOUND, 154, (short) (Float.valueOf(preferences.getString("dsp.analogmodelling.tubetonestack", "5"))*1000));
			}
			session.setParameterShort(session.OptiSOUND, 1206, (short)analogModelEnabled); // Analog modelling switch
		}
	}
}
