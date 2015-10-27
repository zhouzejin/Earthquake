package com.sunny.earthquake;

import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class EarthquakeSearchResultsActivity extends ListActivity implements
		LoaderCallbacks<Cursor> {
	
	private static String QUERY_EXTRA_KEY = "QUERY_EXTRA_KEY";
	
	private SimpleCursorAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 将Adapter于ListView绑定
		adapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_list_item_1, 
				null, 
				new String[] { EarthquakeProvider.KEY_SUMMARY }, 
				new int[] { android.R.id.text1 }, 
				0);
		setListAdapter(adapter);
		
		getLoaderManager().initLoader(0, null, this);
		
		parseIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		parseIntent(getIntent());
	}

	@SuppressWarnings("static-access")
	private void parseIntent(Intent intent) {
		// If the Activity was started to service a Search request,
	    // extract the search query.
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String searchQuery = intent.getStringExtra(SearchManager.QUERY);
			
			// Perform the search, passing in the search query as an argument
		    // to the Cursor Loader
			Bundle args = new Bundle();
			args.putString(QUERY_EXTRA_KEY, searchQuery);
			
			// Restart the Cursor Loader to execute the new query.
			getLoaderManager().restartLoader(0, args, this);
		} else if (intent.ACTION_VIEW.equals(intent.getAction())) {
			Toast.makeText(this, intent.getDataString(), Toast.LENGTH_LONG)
				.show();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String query = "0";
		
		if (args != null) {
			query = args.getString(QUERY_EXTRA_KEY);
		}
		
		// Construct the new query in the form of a Cursor Loader.
		String[] projection  = { EarthquakeProvider.KEY_ID, EarthquakeProvider.KEY_SUMMARY };
		String where = EarthquakeProvider.KEY_SUMMARY + " LIKE \"%" + query + "%\"";
		String[] whereArgs = null;
		String sortOrder = EarthquakeProvider.KEY_SUMMARY + " COLLATE LOCALIZED ASC";
		
		return new CursorLoader(this, EarthquakeProvider.CONTENT_URI, 
				projection, where, whereArgs, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

}
