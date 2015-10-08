package com.sunny.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.app.ListFragment;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;

public class EarthquakeListFragment extends ListFragment {
	
	private static final String TAG = "EARTHQUAKE";
	
	private Handler handler = new Handler();
	
	ArrayAdapter<Quake> aa;
	ArrayList<Quake> earthquakes = new ArrayList<Quake>();

	public EarthquakeListFragment() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		int layoutID = android.R.layout.simple_list_item_1;
		aa = new ArrayAdapter<Quake>(getActivity(), layoutID, earthquakes);
		setListAdapter(aa);
		
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				refreshEarthquakes();
			}
		});
		thread.start();
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
				earthquakes.clear();
				
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
			
		}
	}
	
	private void addNewQuake(Quake _quake) {
		// 将新地震添加到地震列表中
		earthquakes.add(_quake);
		
		// 向ArrayAdapter通知数据改变
		aa.notifyDataSetChanged();
	}

}
