package com.sunny.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class EarthquakeUpdateIntentService extends IntentService {
	
	public static String TAG = "EARTHQUAKE_UPDATE_INTENT_SERVICE";
	
	public EarthquakeUpdateIntentService() {
		super("EarthquakeUpdateIntentService");
	}
	
	public EarthquakeUpdateIntentService(String name) {
		super(name);
	}

	private AlarmManager alarmManager;
	private PendingIntent alarmIntent;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service-onCreate()");
		
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		String ALARM_ACTION = EarthquakeAlarmReceiver.ACTION_REFRESH_EARTHQUAKE_ALARM;
		Intent intentToFire = new Intent(ALARM_ACTION);
		alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Service-onHandleIntent()");
		
		// 检索SharedPreference
		Context context = getApplicationContext();
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(context);
		
		int updateFreq = 
				Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
		boolean autoUpdateChecked = 
				prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);
		
		if (autoUpdateChecked) {
			int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long timeToRefresh = SystemClock.elapsedRealtime() + 
					updateFreq*60*1000;
			alarmManager.setInexactRepeating(alarmType, timeToRefresh, 
					updateFreq*60*1000, alarmIntent);
		} else 
			alarmManager.cancel(alarmIntent);
		
		// 不需要显示地建立后台进行来执行刷新操作，IntentService基类会做这项工作
		refreshEarthquakes();
	}

	@SuppressLint("SimpleDateFormat")
	public void refreshEarthquakes() {
		// 获得XML
		URL url;
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
						
						addNewQuake(quake); // 已经是在主线程中，不需要再同步给UI线程
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
			// IntentService会在执行完毕后自我终止，不用显示调用该方法。
			// stopSelf();
		}
	}
	
	private void addNewQuake(Quake _quake) {
		ContentResolver cr = getContentResolver();
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
	}
	
}
