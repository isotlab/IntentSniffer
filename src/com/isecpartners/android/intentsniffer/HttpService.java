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

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class HttpService extends Service {
    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
    
    private Runnable busyLoop = new Runnable() {
        public void run() {
            int count = 1;
            while(true) {
                count ++;
                try {
                	ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            		List<RecentTaskInfo> rti = am.getRecentTasks(1000,
            				ActivityManager.RECENT_WITH_EXCLUDED);
            		for (RecentTaskInfo c : rti) {
            			Intent cur = c.baseIntent;
            			networkLogger(cur);
            		}
            		Thread.sleep(1000);
                } catch (Exception ex) {
                    Log.e("LOCAL SERVICE", ex.getLocalizedMessage());
                }                    
            }
        }
    };
    private Thread httpThread = new Thread(busyLoop);
    
    private void networkLogger (Intent i) {
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
	    	Log.e("LOGGINGRECEIVER", " error logging intent: "
					+ e.getMessage());
	    } catch (IOException e) {
	    	Log.e("LOGGINGRECEIVER", " error logging intent: "
					+ e.getMessage());
	    }
    
    }
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        HttpService getService() {
            return HttpService.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        if (! httpThread.isAlive()) {
            httpThread.start();
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

        // Set the icon, scrolling text and timestamp
        @SuppressWarnings("deprecation")
		Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, IntentSniffer.class), 0);

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