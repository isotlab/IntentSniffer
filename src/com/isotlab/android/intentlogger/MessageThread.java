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

import com.isecpartners.android.intentsniffer.R;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

public class MessageThread extends HandlerThread {

	   private String phone_id;
	   private ArrayList<IntentActivity> loggedIntents = new ArrayList<IntentActivity>();
	   private ActivityManager systemService;
	   public volatile Handler handler;
	   
	   public MessageThread(String phone_id, ActivityManager systemService) {
		   super("Worker Thread");
	       this.phone_id = phone_id;
	       this.systemService = systemService;
	   }

	   public void run() {	
		   Looper.prepare();
		   handler = new Handler() {
	              public void handleMessage(Message msg) {
	            	  networkLogger((IntentActivity)msg.obj);
	              }
	          };
	       Looper.loop();
	   }
	   
	   private void networkLogger (IntentActivity obj) {
	    	
	    	if(obj == null)
	    		return;
	    	
	    	if(phone_id == null)
	    		return;
	   
		    // Check if our intent is a duplicate
		    if(!loggedIntents.contains(obj)) {
		    	
		    	Log.d("LOCAL SERVICE", "Adding new intent to set");
		    	loggedIntents.add(obj);
		    	
		    	AndroidHttpClient httpClient = new AndroidHttpClient("http://dev.erikjohnson.ca:8080");
				httpClient.setMaxRetries(5);
				ParameterMap params = httpClient.newParams()
		                .add("phone_id", phone_id)
		                .add("action", obj.getAction())
		                .add("category", obj.getCategory())
		                .add("component", obj.getComponent())
		                .add("details", obj.getDetails());
				httpClient.post("/activity", params, new AsyncCallback() {
		            @Override
		            public void onError(Exception e) {
		            	Log.e("HTTP PHONE RESPONSE", " error: "
								+ e.getMessage());
		            }
					@Override
					public void onComplete(
							com.turbomanage.httpclient.HttpResponse httpResponse) {
						Log.d("HTTP PHONE RESPONSE", " status: "
								+ httpResponse.getStatus());
					}
		        });    
			 
		    }
	    }

	}