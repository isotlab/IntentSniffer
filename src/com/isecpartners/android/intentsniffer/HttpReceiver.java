package com.isecpartners.android.intentsniffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HttpReceiver extends BroadcastReceiver {

	protected String mName = "HttpLogger";
	protected IntentSniffer mSniffer;

	/**
	 * @param name
	 *            of this logging receiver, might give hints to the IntentFilter
	 *            used to reach it etc.
	 */
	public HttpReceiver(String name, IntentSniffer is) {
		super();
		this.mName = name;
		this.mSniffer = is;
	}

	public void onReceive(Context c, Intent i) {
	 // Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://dev.erikjohnson.ca:8080/activity");
	    String details = i.toString();
	    
	    try {
	        // Add your data
	        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("phone_id", "1"));
	        nameValuePairs.add(new BasicNameValuePair("activity_type_id", "1"));
	        nameValuePairs.add(new BasicNameValuePair("details", details));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        System.out.println(response);
	        
	    } catch (ClientProtocolException e) {
	    	Log.e("LOGGINGRECEIVER", mName + " error logging intent: "
					+ e.getMessage());
	    } catch (IOException e) {
	    	Log.e("LOGGINGRECEIVER", mName + " error logging intent: "
					+ e.getMessage());
	    }
	}
}
