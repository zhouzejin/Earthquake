package com.sunny.earthquake;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class EarthquakeProvider extends ContentProvider {
	
	public static final Uri CONTENT_URI = 
			Uri.parse("content://com.sunny.earthquakeprovider/earthquakes");
	/**
	 * 列名
	 */
	public static final String KEY_ID = "_id";
	public static final String KEY_DATE = "date";
	public static final String KEY_DETAILS = "details";
	public static final String KEY_SUMMARY = "summary";
	public static final String KEY_LOCATION_LAT = "latitude";
	public static final String KEY_LOCATION_LNG = "longitude";
	public static final String KEY_MAGNITUDE = "magnitude";
	public static final String KEY_LINK = "link";
	
	/**
	 * 区分不同URI请求的常量
	 */
	private static final int QUAKES = 1;
	private static final int QUAKE_ID = 2;
	
	private static final UriMatcher uriMatcher;
	
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.sunny.earthquakeprovider", "earthquakes", QUAKES);
		uriMatcher.addURI("com.sunny.earthquakeprovider", "earthquakes/#", QUAKE_ID);
	}
	
	EarthquakeDatabaseHelper dbHelper;

	public EarthquakeProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		
		dbHelper = new EarthquakeDatabaseHelper(context, 
				EarthquakeDatabaseHelper.DATABASE_NAME, null, 
				EarthquakeDatabaseHelper.DATABASE_VERSION);
		
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE);
		
		switch (uriMatcher.match(uri)) {
		case QUAKE_ID:
			qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			break;
		}
		
		// If no sort order is specified, sort by date / time
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = KEY_DATE;
		} else {
			orderBy = sortOrder;
		}
		
		Cursor c = qb.query(database, 
				projection, 
				selection, selectionArgs, 
				null, null, 
				orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			return "vnd.android.cursor.dir/vnd.sunny.earthquake";
		
		case QUAKE_ID:
			return "vnd.android.cursor.item/vnd.sunny.earthquake";
			
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		
		long rowID = database.insert(
				EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, KEY_DETAILS, values);
		if (rowID > 0) {
			Uri insertUri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		int count;
		
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			count = database.delete(
					EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, selection, selectionArgs);
			break;
			
		case QUAKE_ID:
			String segment = uri.getPathSegments().get(1);
			count = database.delete(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, 
					KEY_ID + "=" + segment + 
					(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		int count;
		
		switch (uriMatcher.match(uri)) {
		case QUAKES:
			count = database.update(
					EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, 
					values, selection, selectionArgs);
			break;
			
		case QUAKE_ID:
			String segment = uri.getPathSegments().get(1);
			count = database.update(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, values, 
					KEY_ID + "=" + segment + 
					(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	/**
	 * Helper class for opening, creating, and managing database version control
	 */
	private static class EarthquakeDatabaseHelper extends SQLiteOpenHelper {
		
		private static final String TAG = "EarthquakeProvider";

		private static final String DATABASE_NAME = "earthquakes.db";
		private static final int DATABASE_VERSION = 1;
		private static final String EARTHQUAKE_TABLE = "earthquakes";

		private static final String DATABASE_CREATE = 
				"create table "+ EARTHQUAKE_TABLE + " (" 
				+ KEY_ID + " integer primary key autoincrement, " 
				+ KEY_DATE + " INTEGER, " 
				+ KEY_DETAILS + " TEXT, " 
				+ KEY_SUMMARY + " TEXT, " 
				+ KEY_LOCATION_LAT + " FLOAT, " 
				+ KEY_LOCATION_LNG + " FLOAT, " 
				+ KEY_MAGNITUDE + " FLOAT, " 
				+ KEY_LINK + " TEXT);";
		
		// private SQLiteDatabase earthquakeDB; // 数据库文件

		public EarthquakeDatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, DATABASE_CREATE);
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// 记录版本升级
			Log.w(TAG, "Upgrading from version " + oldVersion + " to " + newVersion 
					+ ", which will destory all old data!");
			
			// 最简单的升级方式-删除旧的表，创建新表
			String sql = "DROP TABLE IF IT EXISTS " + EARTHQUAKE_TABLE;
			Log.i(TAG, sql);
			db.execSQL(sql);
			onCreate(db);
		}
		
	}

}
