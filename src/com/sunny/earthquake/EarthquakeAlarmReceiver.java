package com.sunny.earthquake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EarthquakeAlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_REFRESH_EARTHQUAKE_ALARM = 
			"com.sunny.earthquake.ACTION_REFRESH_EARTHQUAKE_ALARM";

	@Override
	public void onReceive(Context context, Intent intent) {
		// Intent startIntent = new Intent(context, EarthquakeUpdateService.class);
		// 利用IntentService代替Service
		Intent startIntent = new Intent(context, EarthquakeUpdateIntentService.class);
		context.startService(startIntent);
	}

}
