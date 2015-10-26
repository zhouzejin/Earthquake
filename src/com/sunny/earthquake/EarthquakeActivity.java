package com.sunny.earthquake;

import android.app.Activity;
import android.app.FragmentManager;
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
import android.view.MenuItem;
import android.widget.SearchView;

public class EarthquakeActivity extends Activity {
	
	static final private int MENU_PREFERECES = Menu.FIRST+1;
	static final private int MENU_UPDATE = Menu.FIRST+2;
	
	private static final int SHOW_PREFERENCES = 1;
	
	public boolean autoUpdateChecked = false;
	public int minimumMagnitude = 0;
	public int updateFreq = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_earthquake);
		
		updateFromPreferences();
		
		// 使用Search Manager获取与此Activity关联的SearchableInfo
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		
		// 将Activity的SearchableInfo与搜索视图进行绑定
		SearchView searchView = (SearchView) findViewById(R.id.searchView);
		searchView.setSearchableInfo(searchableInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_PREFERECES, Menu.NONE, R.string.menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case (MENU_PREFERECES): {
			// Intent i = new Intent(this, PreferencesActivity.class);
			Class<?> c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? 
					PreferencesActivity.class : FragmentPreferencesActivity.class;
			Intent i = new Intent(this, c);
			
			startActivityForResult(i, SHOW_PREFERENCES);
			return true;
			}
		}
		
		return false;
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

		if (requestCode == SHOW_PREFERENCES)
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
		t.start();
	}

}
