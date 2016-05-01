package net.programmierecke.radiodroid2;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class Utils {
	public static String getCacheFile(Context ctx, String theURI) {
		StringBuffer chaine = new StringBuffer("");
		try{
			String aFileName = Utils.getBase64(theURI);

			File file = new File(ctx.getFilesDir().getAbsolutePath() + "/"+aFileName);
			Date lastModDate = new Date(file.lastModified());

			Date now = new Date();
			long millis = now.getTime() - file.lastModified();
			long secs = millis / 1000;
			long mins = secs/60;
			long hours = mins/60;

			Log.w("UTIL","File last modified : "+ lastModDate.toString() + " secs="+secs+"  mins="+mins+" hours="+hours);

			if (hours < 1) {
				FileInputStream aStream = ctx.openFileInput(aFileName);
				BufferedReader rd = new BufferedReader(new InputStreamReader(aStream));
				String line = "";
				while ((line = rd.readLine()) != null) {
					chaine.append(line);
				}
				rd.close();
				Log.w("UTIL", "used cache for:" + theURI);
				return chaine.toString();
			}
			Log.w("UTIL", "do not use cache, because too old:" + theURI);
			return null;
		}
		catch(Exception e){
			Log.e("UTIL",""+e);
		}
		return null;
	}

	public static void writeFileCache(Context ctx, String theURI, String content){
		try{
			String aFileName = Utils.getBase64(theURI);
			FileOutputStream aStream = ctx.openFileOutput(aFileName, Context.MODE_PRIVATE);
			aStream.write(content.getBytes("utf-8"));
			aStream.close();
		}
		catch(Exception e){
			Log.e("UTIL","could not write to cache file for:"+theURI);
		}
	}

	public static String downloadFeed(Context ctx, String theURI, boolean forceUpdate) {
		if (!forceUpdate) {
			String cache = getCacheFile(ctx, theURI);
			if (cache != null) {
				return cache;
			}
		}

		StringBuffer chaine = new StringBuffer("");
		try{
			URL url = new URL(theURI);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("User-Agent", "RadioDroid2 ("+BuildConfig.VERSION_NAME+")");
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
			String line = "";
			while ((line = rd.readLine()) != null) {
				chaine.append(line);
			}

			String s = chaine.toString();
			writeFileCache(ctx,theURI,s);
			Log.w("UTIL","wrote cache file for:"+theURI);
			return s;
		} catch (Exception e) {
			Log.e("UTIL",""+e);
		}

		return null;
	}

	public static String getRealStationLink(Context ctx, String stationId){
		String result = Utils.downloadFeed(ctx, "http://www.radio-browser.info/webservice/json/url/" + stationId, true);
		if (result != null) {
			JSONObject jsonObj = null;
			JSONArray jsonArr = null;
			try {
				jsonArr = new JSONArray(result);
				jsonObj = jsonArr.getJSONObject(0);
				return jsonObj.getString("url");
			} catch (Exception e) {
				Log.e("UTIL", "" + e);
			}
		}
		return null;
	}

	public static String getBase64(String theOriginal) {
		return Base64.encodeToString(theOriginal.getBytes(), Base64.URL_SAFE | Base64.NO_PADDING);
	}
}
