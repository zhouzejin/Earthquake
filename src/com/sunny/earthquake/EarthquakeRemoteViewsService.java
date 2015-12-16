package com.sunny.earthquake;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class EarthquakeRemoteViewsService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new EarthquakeRemoteViewsFactory(getApplicationContext());
	}
	
	class EarthquakeRemoteViewsFactory implements RemoteViewsFactory {
		
		private Context context;
		private Cursor cursor;

		public EarthquakeRemoteViewsFactory(Context context) {
			super();
			this.context = context;
		}
		
		private Cursor executeQuery() {
			String[] projection = new String[] {
				EarthquakeProvider.KEY_ID, 
				EarthquakeProvider.KEY_MAGNITUDE, 
				EarthquakeProvider.KEY_DETAILS
			};
			
			Context appContext = getApplicationContext();
			SharedPreferences prefs = 
					PreferenceManager.getDefaultSharedPreferences(appContext);
			int minimumMagnitude = 
					Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
			
			String where = EarthquakeProvider.KEY_MAGNITUDE + " > " + minimumMagnitude;
			
			return context.getContentResolver().query(EarthquakeProvider.CONTENT_URI, 
					projection, where, null, null);
		}

		@Override
		public void onCreate() {
			cursor = executeQuery();
		}

		@Override
		public void onDataSetChanged() {
			cursor = executeQuery();
		}

		@Override
		public void onDestroy() {
			cursor.close();
		}

		@Override
		public int getCount() {
			if (cursor != null)
				return cursor.getCount();
			else
				return 0;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			// 将Cursor移动到所需的索引
			cursor.moveToPosition(position);
			
			// 提取当前游标行的值
			int idIdx = cursor.getColumnIndex(EarthquakeProvider.KEY_ID);
			int magnitudeIdx = cursor.getColumnIndex(EarthquakeProvider.KEY_MAGNITUDE);
			int detailsIdx = cursor.getColumnIndex(EarthquakeProvider.KEY_DETAILS);
			
			String id = cursor.getString(idIdx);
			String magnitude = cursor.getString(magnitudeIdx);
			String details = cursor.getString(detailsIdx);
			
			// 创建一个新的RemoteViews对象，并使用该对象来填充列表中的Item
			RemoteViews rv = new RemoteViews(context.getPackageName(), 
					R.layout.widget_earthquake);
			
			rv.setTextViewText(R.id.widget_magnitude, magnitude);
			rv.setTextViewText(R.id.widget_details, details);
			
			// 创建一个特定于Item的fillInIntent,用于填充AppWidgetProvider中创建的PendingIntent
			Intent fillInIntent = new Intent();
			Uri uri = Uri.withAppendedPath(EarthquakeProvider.CONTENT_URI, id);
			fillInIntent.setData(uri);
			
			rv.setOnClickFillInIntent(R.id.widget_magnitude, fillInIntent);
			rv.setOnClickFillInIntent(R.id.widget_details, fillInIntent);
			
			return rv;
		}

		@Override
		public RemoteViews getLoadingView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public long getItemId(int position) {
			if (cursor != null) {
				return cursor.getLong(cursor.getColumnIndex(EarthquakeProvider.KEY_ID));
			} else {
				return position;
			}
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return true;
		}
	}

}
