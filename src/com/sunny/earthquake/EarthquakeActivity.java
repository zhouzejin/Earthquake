package com.sunny.earthquake;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

public class EarthquakeActivity extends Activity {
	
	public static String TAG = "EARTHQUAKEACTIVITY";
	
	static final private int MENU_PREFERECES = Menu.FIRST+1;
	static final private int MENU_UPDATE = Menu.FIRST+2;
	
	private static final int SHOW_PREFERENCES = 1;
	
	public boolean autoUpdateChecked = false;
	public int minimumMagnitude = 0;
	public int updateFreq = 0;
	
	TabListener<EarthquakeListFragment> listTabListener;
	TabListener<EarthquakeMapFragment> mapTabListener;
	
	private static String ACTION_BAR_INDEX = "ACTION_BAR_INDEX";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_earthquake);
		
		updateFromPreferences();
		
		// 使用Menu代替
		/*// 使用Search Manager获取与此Activity关联的SearchableInfo
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		
		
		// 将Activity的SearchableInfo与搜索视图进行绑定
		SearchView searchView = (SearchView) findViewById(R.id.searchView);
		searchView.setSearchableInfo(searchableInfo);*/
		
		ActionBar actionBar = getActionBar();
		View fragmentContainer = findViewById(R.id.fl_fragment_container);
		
		// 如果列表和地图Fragment都可用，使用Pad导航
		boolean tabletLayout = fragmentContainer == null;
		
		// 如果不是Pad，使用操作栏Tab
		if (!tabletLayout) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			actionBar.setDisplayShowTitleEnabled(false);
			
			// 创建并添加列表Tab键
			Tab listTab = actionBar.newTab();
			listTabListener = new TabListener<EarthquakeListFragment>(this,
					EarthquakeListFragment.class, R.id.fl_fragment_container);
			listTab.setText("List")
					.setContentDescription("List of earthquakes")
					.setTabListener(listTabListener);
			actionBar.addTab(listTab);
			
			// 创建并添加地图Tab键
			Tab mapTab = actionBar.newTab();
			mapTabListener = new TabListener<EarthquakeMapFragment>(this,
					EarthquakeMapFragment.class, R.id.fl_fragment_container);
			mapTab.setText("Map")
					.setContentDescription("Map of earthquakes")
					.setTabListener(mapTabListener);
			actionBar.addTab(mapTab);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		View fragmentContainer = findViewById(R.id.fl_fragment_container);
		boolean tabletLayout = fragmentContainer == null;
		
		if (!tabletLayout) {
			// 保存当前操作栏Tab键的选择
			int actionBarIndex = getActionBar().getSelectedTab().getPosition();
			SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
			editor.putInt(ACTION_BAR_INDEX, actionBarIndex);
			editor.apply();
			
			// 分离每个Fragment
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			if (mapTabListener.fragment != null)
				ft.detach(mapTabListener.fragment);
			if (listTabListener.fragment != null)
				ft.detach(listTabListener.fragment);
			ft.commit();
		}
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		View fragmentContainer = findViewById(R.id.fl_fragment_container);
		boolean tabletLayout = fragmentContainer == null;
		
		if (!tabletLayout) {
			// 获得重建的Fragment并把他们分配给相关的TabListener
			listTabListener.fragment = 
					getFragmentManager().findFragmentByTag(EarthquakeListFragment.class.getName());
			mapTabListener.fragment = 
					getFragmentManager().findFragmentByTag(EarthquakeMapFragment.class.getName());
			
			// 还原之前的操作栏Tab选择
			SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
			int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
			getActionBar().setSelectedNavigationItem(actionBarIndex);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		View fragmentContainer = findViewById(R.id.fl_fragment_container);
		boolean tabletLayout = fragmentContainer == null;
		
		if (!tabletLayout) {
			SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
			int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
			getActionBar().setSelectedNavigationItem(actionBarIndex);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		// menu.add(0, MENU_PREFERECES, Menu.NONE, R.string.menu_preferences);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		
		// 该代码从OnCreate移到此处-检索Search View并配置和启用它
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		// SearchView searchView = (SearchView) findViewById(R.id.searchView);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		searchView.setSearchableInfo(searchableInfo);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case (R.id.menu_refresh): {
			startService(new Intent(this, EarthquakeUpdateIntentService.class));
			Log.i(TAG, "menu_refresh");
			return true;
		}
		case (R.id.menu_preferences): {
			// Intent i = new Intent(this, PreferencesActivity.class);
			Class<?> c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? 
					PreferencesActivity.class : FragmentPreferencesActivity.class;
			Intent i = new Intent(this, c);
			
			startActivityForResult(i, SHOW_PREFERENCES);
			return true;
			}
		default: 
			return false;
		}
	}
	
	private void updateFromPreferences() {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		minimumMagnitude = 
				Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
		updateFreq = 
				Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
		autoUpdateChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// 加入新的Fragment后，EarthquakeListFragment可能不总在页面上，因此直接启动服务更新数据
		/*if (requestCode == SHOW_PREFERENCES)
			updateFromPreferences();

		
		FragmentManager fm = getFragmentManager();
		final EarthquakeListFragment earthquakeList = (EarthquakeListFragment) fm
				.findFragmentById(R.id.fgm_earthquake_list);

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				earthquakeList.refreshEarthquakes();
			}
		});
		t.start();*/
		if (requestCode == SHOW_PREFERENCES) {
			updateFromPreferences();
			startService(new Intent(this, EarthquakeUpdateIntentService.class));
			Log.i(TAG, "SHOW_PREFERENCES");
		}
	}
	
	public static class TabListener<T extends Fragment> 
		implements ActionBar.TabListener {
		
		private Fragment fragment;
		private Activity activity;
		private Class<T> fragmentClass;
		private int fragmentContainer;

		public TabListener(Activity activity, Class<T> fragmentClass,
				int fragmentContainer) {
			super();
			this.activity = activity;
			this.fragmentClass = fragmentClass;
			this.fragmentContainer = fragmentContainer;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment == null) {
				String fragmentName = fragmentClass.getName();
				fragment = Fragment.instantiate(activity, fragmentName);
				ft.add(fragmentContainer, fragment, fragmentName);
			} else 
				ft.attach(fragment);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null)
				ft.detach(fragment);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null)
				ft.attach(fragment);
		}
		
	}

}
