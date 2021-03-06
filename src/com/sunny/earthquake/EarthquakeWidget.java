package com.sunny.earthquake;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.Toast;

public class EarthquakeWidget extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// Create a Pending Intent that will open the main Activity
		Intent intent = new Intent(context, EarthquakeActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		
		// Apply the On Click Listener to both Text Views
		RemoteViews views = new RemoteViews(context.getPackageName(), 
				R.layout.widget_earthquake);
		views.setOnClickPendingIntent(R.id.widget_magnitude, pendingIntent);
		views.setOnClickPendingIntent(R.id.widget_details, pendingIntent);
		
		// Notify the App Widget Manager to update
		appWidgetManager.updateAppWidget(appWidgetIds, views);
		
		// Update the Widget UI with the latest Earthquake details
		updateQuake(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (EarthquakeUpdateIntentService.QUAKES_REFRESHED.equals(intent.getAction())) {
			updateQuake(context);
			Toast.makeText(context, "Widget Update!", Toast.LENGTH_LONG).show();
		}
	}

	public void updateQuake(Context context, AppWidgetManager appWidgetManager, 
			int[] appWidgetIds) {
		Cursor lastEarthquake;
		ContentResolver cr = context.getContentResolver();
		lastEarthquake = cr.query(EarthquakeProvider.CONTENT_URI, 
				null, null, null, null);
		
		String magnitude = "--";
		String details = "-- None --";
		
		if (lastEarthquake != null) {
			try {
				if (lastEarthquake.moveToFirst()) {
					int magColumn = lastEarthquake
							.getColumnIndexOrThrow(EarthquakeProvider.KEY_MAGNITUDE);
					int detailsColumn = lastEarthquake
							.getColumnIndexOrThrow(EarthquakeProvider.KEY_DETAILS);

					magnitude = lastEarthquake.getString(magColumn);
					details = lastEarthquake.getString(detailsColumn);
				}
			} finally {
				lastEarthquake.close();
			}
		}
		
		// 创建一个新的RomoteViews对象以设置由Widget的TextView元素所显示的文本
		final int N = appWidgetIds.length; // 可能有多个Widget实例
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(), 
					R.layout.widget_earthquake);
			views.setTextViewText(R.id.widget_magnitude, magnitude);
			views.setTextViewText(R.id.widget_details, details);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
	
	public void updateQuake(Context context) {
		ComponentName thisWidget = new ComponentName(context, 
				EarthquakeWidget.class);
		
		AppWidgetManager appWidgetManager = 
			       AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		
		updateQuake(context, appWidgetManager, appWidgetIds);
	}

}
