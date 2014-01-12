package com.isotlab.android.intentlogger;

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

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class LoggerThread extends Thread {

	   private String phone_id;
	   private ArrayList<IntentActivity> loggedIntents = new ArrayList<IntentActivity>();
	   private ActivityManager systemService;
	   public volatile Handler handler;
	   
	   public LoggerThread(String phone_id, ActivityManager systemService) {
		   super("LoggerThread");
	       this.phone_id = phone_id;
	       this.systemService = systemService;
	   }

	   public void run() {	
		   while(true) {
               try {
               	ActivityManager am = systemService;
           		List<RecentTaskInfo> rti = am.getRecentTasks(1000,
           				ActivityManager.RECENT_WITH_EXCLUDED);
           		for (RecentTaskInfo c : rti) {
           			Intent cur = c.baseIntent;
           			networkLogger(new IntentActivity(cur));
           		}
           		Thread.sleep(10000);
               } catch (Exception ex) {
                   Log.e("LOCAL SERVICE", ex.getMessage());
               }                    
           }
	   }
	   
	   private void networkLogger (IntentActivity obj) {
	    	
	    	if(obj == null)
	    		return;
	    	
	    	if(phone_id == null)
	    		return;
	   
		    // Check if our intent is a duplicate
		    if(!loggedIntents.contains(obj)) {
		    	
		    	Log.d("LOCAL SERVICE", "Adding new intent to set");
		    	
		    	HttpClient httpclient = new DefaultHttpClient();
			    HttpPost httppost = new HttpPost("http://dev.erikjohnson.ca:8080/activity");

			    loggedIntents.add(obj);
			    
			    try {
			        // Add your data
			        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(2);
			        nameValuePairs.add(new BasicNameValuePair("phone_id", phone_id));
			        nameValuePairs.add(new BasicNameValuePair("action", obj.getAction()));
			        nameValuePairs.add(new BasicNameValuePair("category", obj.getCategory()));
			        nameValuePairs.add(new BasicNameValuePair("component", obj.getComponent()));
			        nameValuePairs.add(new BasicNameValuePair("details", obj.getDetails()));
			        nameValuePairs.add(new BasicNameValuePair("timestamp", obj.getTimestamp()));
			        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			        
			        // Execute HTTP Post Request
			        HttpResponse response = httpclient.execute(httppost);
			        
			    } catch (ClientProtocolException e) {
			    	Log.e("LOGGINGRECEIVER", " error logging intent: "
							+ e.getMessage());
			    } catch (IOException e) {
			    	Log.e("LOGGINGRECEIVER", " error logging intent: "
							+ e.getMessage());
			    }
		    } else {
		    	//pass
		    }
	    }

	}