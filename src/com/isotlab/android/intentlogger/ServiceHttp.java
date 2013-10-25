
package com.isotlab.android.intentlogger;
import java.util.Random;

import com.isecpartners.android.intentsniffer.R;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ServiceHttp extends Service {
    private NotificationManager mNM;
    public static final String TAG = "IntentSniffer";

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
    private String phone_id;
    private LoggerThread httpThread;
    private MessageThread messageThread;
    
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        ServiceHttp getService() {
            return ServiceHttp.this;
        }
    }

    @Override
    public void onCreate() {
    	Log.d(TAG, "Created Service for Http Loggin");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(TAG, "Recevied intent "  + startId + ": " + intent);
    	phone_id = intent.getStringExtra("phone_id");
    	
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        
    	if(httpThread == null){
    		 httpThread = new LoggerThread(phone_id, (ActivityManager) getSystemService(ACTIVITY_SERVICE));
    	}
    	
    	if(messageThread == null) {
    		messageThread = new MessageThread(phone_id, (ActivityManager) getSystemService(ACTIVITY_SERVICE));
    		messageThread.start();
    	}
    	
    	if (! httpThread.isAlive()) {
            httpThread.start();
        }
    	
    	if(intent.getSerializableExtra("IntentActivity") != null){
        	// We are sending the intent to send a message :)
        	Message msg = new Message();
        	IntentActivity activity = (IntentActivity) intent.getSerializableExtra("IntentActivity");
        	msg.obj = activity;
        	messageThread.handler.sendMessage(msg);
        	Log.d(TAG, "Wowzers we are sending over an intent manually! "  + startId + ": " + intent);
        }
    	
    	
    	
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }


    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and
        @SuppressWarnings("deprecation")
		Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ActivitySniffer.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}