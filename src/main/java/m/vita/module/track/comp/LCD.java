/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
 */

package m.vita.module.track.comp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;

import java.io.File;

import m.vita.module.track.service.IterationData;
import m.vita.module.track.service.PowerData;
import m.vita.module.track.util.ForegroundDetector;
import m.vita.module.track.util.Recycler;
import m.vita.module.track.util.SystemInfo;

public class LCD extends PowerComponent {
	public static class LcdData extends PowerData {
		private static Recycler<LcdData> recycler = new Recycler<LcdData>();

		public static LcdData obtain() {
			LcdData result = recycler.obtain();
			if (result != null)
				return result;
			return new LcdData();
		}

		@Override
		public void recycle() {
			recycler.recycle(this);
		}

		public int brightness;
		public boolean screenOn;

		private LcdData() {
		}

		public void init(int brightness, boolean screenOn) {
			this.brightness = brightness;
			this.screenOn = screenOn;
		}
	}

	private final String TAG = "NT1LCD";
	private static final String[] BACKLIGHT_BRIGHTNESS_FILES = {
			"/sys/devices/virtual/leds/lcd-backlight/brightness",
			"/sys/devices/platform/trout-backlight.0/leds/lcd-backlight/brightness", };

	private Context context;
	private ForegroundDetector foregroundDetector;
	private BroadcastReceiver broadcastReceiver;
	private boolean screenOn;

	private String brightnessFile;

	public LCD(Context context) {
		this.context = context;
		screenOn = true;

		if (context == null) {
			return;
		}

		foregroundDetector = new ForegroundDetector(
				(ActivityManager) context
						.getSystemService(Context.ACTIVITY_SERVICE));
		broadcastReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				synchronized (this) {
					if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
						screenOn = false;
					} else if (intent.getAction().equals(
							Intent.ACTION_SCREEN_ON)) {
						screenOn = true;
					}
				}
			};
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		context.registerReceiver(broadcastReceiver, intentFilter);

		for (int i = 0; i < BACKLIGHT_BRIGHTNESS_FILES.length; i++) {
			if (new File(BACKLIGHT_BRIGHTNESS_FILES[i]).exists()) {
				brightnessFile = BACKLIGHT_BRIGHTNESS_FILES[i];
			}
		}
	}

	@Override
	protected void onExit() {
		if(broadcastReceiver != null )context.unregisterReceiver(broadcastReceiver);
		super.onExit();
	}

	@Override
	public IterationData calculateIteration(long iteration) {
		IterationData result = IterationData.obtain();

		boolean screen;
		synchronized (this) {
			screen = screenOn;
		}

		int brightness;
		if (brightnessFile != null) {
			brightness = (int) SystemInfo.getInstance().readLongFromFile(brightnessFile);
		} else {
			try {
				brightness = Settings.System.getInt(
						context.getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS);
			} catch (Settings.SettingNotFoundException ex) {
				Log.w(TAG, "Could not retrieve brightness information");
				return result;
			}
		}
		if (brightness < 0 || 255 < brightness) {
			Log.w(TAG, "Could not retrieve brightness information");
			return result;
		}

		LcdData data = LcdData.obtain();
		data.init(brightness, screen);
		result.setPowerData(data);

		if (screen) {
			LcdData uidData = LcdData.obtain();
			uidData.init(brightness, screen);
			result.addUidPowerData(foregroundDetector.getForegroundUid(),
					uidData);
		}

		return result;
	}

	@Override
	public boolean hasUidInformation() {
		return true;
	}

	@Override
	public String getComponentName() {
		return "LCD";
	}
}
