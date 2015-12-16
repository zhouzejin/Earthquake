package com.sunny.earthquake;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class EarthquakeListWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// 迭代处于活动状态的Widget的数组
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			
			// 创建用于启动EarthquakeRemoteViewsService的Intent
			Intent intent = new Intent(context, EarthquakeRemoteViewsService.class);
			// 将APP Widget的ID添加到Intent extra中
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			
			// 为APP Widget布局实例化RemoteViews对象
			RemoteViews views = new RemoteViews(context.getPackageName(), 
					R.layout.collection_widget_earthquake);
			
			// 设置RemoteViews对象来使用一个RemoteViews适配器
			views.setRemoteAdapter(R.id.widget_list_view, intent);
			
			// List中没有Item时显示空View
			views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text);
			
			// 创建一个PendingIntent模板，为ListView的每个Item提供交互性
			Intent templateIntent = new Intent(context, EarthquakeActivity.class);
			templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent templatePendingIntent = 
					PendingIntent.getActivity(context, 0, templateIntent, 
							PendingIntent.FLAG_UPDATE_CURRENT);
			views.setPendingIntentTemplate(R.id.widget_list_view, templatePendingIntent);
			
			// 通知AppWidgetManager使用修改后的远程View更新Widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

}
