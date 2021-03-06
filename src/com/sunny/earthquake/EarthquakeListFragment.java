package com.sunny.earthquake;

import java.util.Date;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class EarthquakeListFragment extends ListFragment 
	implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String TAG = "EARTHQUAKELISTFRAGMENT";
	
	private Handler handler = new Handler();
	
	// ArrayAdapter<Quake> aa;
	// ArrayList<Quake> earthquakes = new ArrayList<Quake>();
	SimpleCursorAdapter adapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		int layoutID = android.R.layout.simple_list_item_1;
		// aa = new ArrayAdapter<Quake>(getActivity(), layoutID, earthquakes);
		// setListAdapter(aa);
		adapter = new SimpleCursorAdapter(getActivity(), layoutID, null, 
				new String[] { EarthquakeProvider.KEY_SUMMARY }, 
				new int[] { android.R.id.text1 }, 0);
		setListAdapter(adapter);
		
		getLoaderManager().initLoader(0, null, this);
		
		/*Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				refreshEarthquakes();
			}
		});
		thread.start();*/
		// 网络处理的代码已经放在了Service中，这里就不需要在后台线程中执行该方法了。
		refreshEarthquakes();
	}
	
	@SuppressLint("SimpleDateFormat")
	public void refreshEarthquakes() {
		// 必须在UI线程上初始化和重启Loader，所以需要使用handler在主线程上重启Loader
		// 这里重启Loader并不是为了通知CursorAdapter数据变化，
		// 而是为了使CursorAdapter的Cursor保持最新（这里指获取数据的规则），从而让其他调用者获取到正确的数据
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				getLoaderManager().restartLoader(0, null,
						EarthquakeListFragment.this);
			}
		});
		
		// 利用Service完成以下的工作
		/*getActivity().startService(new Intent(getActivity(),
				EarthquakeUpdateService.class));*/
		// 利用IntentService代替Service
		getActivity().startService(new Intent(getActivity(), 
				EarthquakeUpdateIntentService.class));
		Log.i(TAG, "refreshEarthquakes()");
		
		// 获得XML
		/*URL url;
		try {
			String quakeFeed = getString(R.string.quake_feed);
			url = new URL(quakeFeed);
			
			URLConnection connection;
			connection = url.openConnection();
			
			HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
			int responseCode = httpURLConnection.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = httpURLConnection.getInputStream();
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				
				// 分析地震源
				Document dom = db.parse(in);
				Element docEle = dom.getDocumentElement();
				
				// 清除旧的地震数据
				// earthquakes.clear();
				
				// 获得每个地震项的列表
				NodeList nl = docEle.getElementsByTagName("event");
				Log.d(TAG, "Length: " + nl.getLength());
				if (nl != null && nl.getLength() > 0) {
					for (int i = 0; i < nl.getLength(); i++) {
						Element entry = (Element) nl.item(i);
						
						String link = entry.getAttribute("publicID").split(":")[1];
						Log.d(TAG, "link: " + link);
						
						Element description = 
								(Element) entry.getElementsByTagName("description").item(0);
						Element origin = 
								(Element) entry.getElementsByTagName("origin").item(0);
						Element magnitude = 
								(Element) entry.getElementsByTagName("magnitude").item(0);
						
						String details = description.getLastChild().getTextContent();
						Log.d(TAG, "details: " + details);
						
						Node node = origin.getFirstChild();
						String date = node.getFirstChild().getTextContent();
						Log.d(TAG, "date: " + date);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
						Date qDate = new GregorianCalendar(0, 0, 0).getTime();
						try {
							qDate = sdf.parse(date);
							Log.d(TAG, "qDate: " + qDate.toString());
						} catch (ParseException e) {
							Log.e(TAG, "ParseException", e);
						}
						
						node = node.getNextSibling();
						String longitude = node.getFirstChild().getTextContent();
						Log.d(TAG, "longitude: " + longitude);
						node = node.getNextSibling();
						String latitude = node.getFirstChild().getTextContent();
						Log.d(TAG, "latitude: " + latitude);
						Location location = new Location("dummyGPS");
						location.setLongitude(Double.valueOf(longitude));
						location.setLatitude(Double.valueOf(latitude));
						
						node = magnitude.getFirstChild();
						String mag = node.getFirstChild().getTextContent();
						Log.d(TAG, "mag: " + mag);
						double qMagnitude = Double.valueOf(mag);
						
						final Quake quake = new Quake(qDate, details, location, qMagnitude, link);
						
						// 处理一个新发现的地震
						handler.post(new Runnable() {
							
							@Override
							public void run() {
								addNewQuake(quake);
							}
						});
					}
				}
			}
		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException", e);
		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "ParserConfigurationException", e);
		} catch (SAXException e) {
			Log.e(TAG, "SAXException", e);
		} finally {
			
		}*/
	}
	
	/**
	 * 移动到了EarthquakeUpdateService中
	 */
	/*private void addNewQuake(Quake _quake) {
		EarthquakeActivity activity = (EarthquakeActivity) getActivity();
		if (_quake.getMagnitude() > activity.minimumMagnitude) { // 过滤震级低的地震
			// 将新地震添加到地震列表中
			earthquakes.add(_quake);			
		}
		
		// 向ArrayAdapter通知数据改变
		aa.notifyDataSetChanged();
		
		ContentResolver cr = getActivity().getContentResolver();
		String where = EarthquakeProvider.KEY_DATE + "=" + _quake.getDate().getTime();
		
		Cursor query = cr.query(EarthquakeProvider.CONTENT_URI, null, where, null, null);
		if (query.getCount() == 0) { //该地震是新的地震
			ContentValues values = new ContentValues();
			
			values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
			values.put(EarthquakeProvider.KEY_DETAILS, _quake.getDetails());
			values.put(EarthquakeProvider.KEY_SUMMARY, _quake.toString());
			
			double lat = _quake.getLocation().getLatitude();
			double lng = _quake.getLocation().getLongitude();
			values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
			values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
			values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
			values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());
			
			// 由于EarthquakeProvider的insert函数中执行了
			// getContext().getContentResolver().notifyChange(uri, null);
			// notifyChange(uri, null)方法默认向CursorAdapter对象发送数据变化的通知
			// 所以，这里不需要手打调用CursorAdapter的notifyDataSetChanged()方法。
			cr.insert(EarthquakeProvider.CONTENT_URI, values);
		}
		query.close();
	}*/

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] {
				EarthquakeProvider.KEY_ID, 
				EarthquakeProvider.KEY_SUMMARY
		};
		
		// 过滤震级低的地震
		EarthquakeActivity activity = (EarthquakeActivity) getActivity();
		String where = EarthquakeProvider.KEY_MAGNITUDE + " > " + 
				activity.minimumMagnitude;
		
		CursorLoader loader = new CursorLoader(getActivity(), 
				EarthquakeProvider.CONTENT_URI, projection, where, null, null);
		
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data); // 保持Cursor最新
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		ContentResolver cr = getActivity().getContentResolver();
		Cursor result = 
				cr.query(ContentUris.withAppendedId(EarthquakeProvider.CONTENT_URI, id), 
						null, null, null, null);
		
		if (result.moveToFirst()) {
			Date date = new Date(result.getLong(
					result.getColumnIndex(EarthquakeProvider.KEY_DATE)));
			String details = result.getString(
					result.getColumnIndex(EarthquakeProvider.KEY_DETAILS));
			double magnitude = result.getDouble(
					result.getColumnIndex(EarthquakeProvider.KEY_MAGNITUDE));
			String linkString = result.getString(
					result.getColumnIndex(EarthquakeProvider.KEY_LINK));
			double lat = result.getDouble(
					result.getColumnIndex(EarthquakeProvider.KEY_LOCATION_LAT));
			double lng = result.getDouble(
					result.getColumnIndex(EarthquakeProvider.KEY_LOCATION_LNG));
			
			Location location = new Location("db");
			location.setLatitude(lat);
			location.setLongitude(lng);
			
			Quake quake = new Quake(date, details, location, magnitude, linkString);
			
			DialogFragment newFragment = EarthquakeDialogFragment.newInstance(getActivity(), quake);
			newFragment.show(getFragmentManager(), "dialog");
		}
	}

}
