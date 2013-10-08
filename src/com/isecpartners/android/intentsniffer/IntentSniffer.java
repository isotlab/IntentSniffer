package com.isecpartners.android.intentsniffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Intent.FilterComparison;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Three attempted mechanisms. 1. looking at recent task's intents (from their
 * starting) 2. Registering receiver without an Action filter, while the docs
 * say this works, the source disagrees :( 3. registering receivers for all
 * known types of data, determining all this is a bit tricky so we walk around
 * registration information to find what we need. Note that currently scheme and
 * type are static (although type includes the wild card, so no need to fix that
 * one).
 * 
 * @author Jesse, iSEC Partners, Inc.
 * 
 */
public class IntentSniffer extends Activity {
	// controls
	public TextView mTextView = null;
	public Button mUpdate = null;
	public Button mRegisterNull = null;
	public CheckBox mShowRecent = null;
	public CheckBox mShowBroadcasts = null;
	public CheckBox mShowDetails = null;

	public boolean mDetails = false;
	// list of every source we are outputing
	public Set<String> mReporting = new HashSet<String>();

	// Known actions, mostly stuff in the SDK which isn't easy to load
	// dynamically
	public String[] mKnownBroadcastActions = {
			"android.provider.Telephony.SECRET_CODE",
			"android.provider.Telephony.SPN_STRINGS_UPDATED",
			"com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED",
			"com.android.mms.transaction.MESSAGE_SENT",
			"android.intent.action.ANR", "android.intent.action.stk.command",
			"android.intent.action.stk.session_end",
			"com.android.im.SmsService.SMS_STATUS_RECEIVED" };

	// classes that have broadcast actions, this list is incomplete (a start).
	// These classes are not all visible to SDK, so loading them at runtime for
	// reflection on the actual device.
	public String[] actionHarboringClasses = {
			"android.content.Intent",
			"android.bluetooth.BluetoothIntent",
			"android.bluetooth.BluetoothA2dp",
			// Application specific, and would be dangerous to load (as their
			// static initializer might run etc.)
			// "com.android.mms.transaction.MessageStatusReceiver",
			// "com.android.mms.transaction.SmsReceiverService",
			// "com.android.internal.telephony.gsm.stk.AppInterface",
			"com.android.internal.location.GpsLocationProvider",
			"com.android.internal.telephony.TelephonyIntents",
			"android.provider.Telephony.Intents",
			"android.proivder.Contacts.Intents",
			"com.android.mms.util.RateController",
			"android.media.AudioManager", "android.net.wifi.WifiManager",
			"android.telephony.TelephonyManager",
			"android.appwidget.AppWidgetManager",
			"android.net.ConnectivityManager" };

	public String[] mKnownSchemes = { ContentResolver.SCHEME_ANDROID_RESOURCE,
			ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE,
			"http", "https", "mailto", "wtai", "tel", "imap", "pop3", "local",
			"geo", "", "ftp", "svn", "ssh", "im", "package", "voicemail",
			"about", "mmsto", "mms", "smsto", "sms", "market",
			"google.streetview", "rtsp", "android_secret_code", "lastfm" };

	public String[] mKnownMimeTypes = { "*", "vnd.android.cursor.dir",
			"vnd.android.cursor.item", "video", "audio", "application", "text",
			"image", "vnd.android-dir" }; // * is the only important one

	public String[] mKnownCategories = { "android.intent.category.HOME",
			"android.intent.category.LAUNCHER", "video", "audio",
			"application", "text", "image", "vnd.android-dir" };

	// current receivers
	public List<BroadcastReceiver> mReceivers = new ArrayList<BroadcastReceiver>();

	// keeping track
	public int mNumReflected = 0;
	public int mNumDug = 0;

	// Maps received intents to their sources. Sources are human readable
	// strings. Storing every intent is a little wasteful, and the GC doesn't
	// get to reclaim its data, some intents can actually be a little large. We
	// could certainly pare this down a little, especially if we move to testing
	// with high volume and data intent sources, but in practice it has been
	// fine so far.
	private final HashMap<String, Collection<FilterComparison>> mReceivedIntents = new HashMap<String, Collection<FilterComparison>>();

	public static final String TAG = "IntentSniffer";

	// work
	public static final String RECENT_SOURCE = "recent tasks";
	public static final String ACTION_ONLY_SOURCE = "known action";
	public static final String ACTION_AND_DATA_SOURCE = "known action and data";
	public static final String ACTION_AND_DATA_TYPE_SOURCE = "known action and data and type";
	// don't work
	public static final String WILD_ACTION_SOURCE = "wild action";
	public static final String WILD_ACTION_AND_DATA_SOURCE = "wild action known data";

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		try {
			initControls();
		} catch (Throwable e) {
			Toast.makeText(this, "error: " + e.getMessage(), Toast.LENGTH_LONG)
					.show();
			// mTextView.append(e.getMessage() );
		}
		
		startService(new Intent(this, HttpService.class));
	}

	protected void initControls() {
		mTextView = (TextView) findViewById(R.id.output);
		mUpdate = (Button) findViewById(R.id.updateView);
		mRegisterNull = (Button) findViewById(R.id.registerNull);
		mShowRecent = (CheckBox) findViewById(R.id.recent);
		mShowBroadcasts = (CheckBox) findViewById(R.id.broadcasts);
		mShowDetails = (CheckBox) findViewById(R.id.showDetails);

		loadKnownActions();
		loadKnownCategories();

		Log.d(TAG, "known broadcast action count: "
				+ mKnownBroadcastActions.length);
		Log.d(TAG, "known category action count: " + mKnownCategories.length);
		mUpdate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updateView();
			}
		});

		mRegisterNull.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				registerBuddy(null, true);
			}
		});

		mShowRecent.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton c, boolean isChecked) {
				if (isChecked) {
					updateWithRecents();
					mReporting.add(RECENT_SOURCE);
				} else {
					mReporting.remove(RECENT_SOURCE);
				}
				updateView();
			}
		});

		mShowBroadcasts
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton c,
							boolean isChecked) {
						registerBuddy(null, isChecked);
						if (isChecked) {
							mReporting.add(ACTION_ONLY_SOURCE);
							mReporting.add(ACTION_AND_DATA_SOURCE);
							mReporting.add(ACTION_AND_DATA_TYPE_SOURCE);
							mReporting.add(WILD_ACTION_SOURCE);
							mReporting.add(WILD_ACTION_AND_DATA_SOURCE);

						} else {
							mReporting.remove(ACTION_ONLY_SOURCE);
							mReporting.remove(ACTION_AND_DATA_SOURCE);
							mReporting.remove(ACTION_AND_DATA_TYPE_SOURCE);
							mReporting.remove(WILD_ACTION_SOURCE);
							mReporting.remove(WILD_ACTION_AND_DATA_SOURCE);
						}
						updateView();
					}
				});

		mShowDetails.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton c, boolean isChecked) {
				mDetails = isChecked;
				updateView();
			}
		});
	} // initControls

	// Menu definition
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1, Menu.NONE, "Update Actions");
		menu.add(Menu.NONE, 2, Menu.NONE, "Update Categories");
		menu.add(Menu.NONE, 3, Menu.NONE, "Show Stats");
		return true;
	}

	// Menu handling
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			updateKnownActions();
			return true;
		case 2:
			updateKnownCategories();
			return true;
		case 3:
			String s = mKnownBroadcastActions.length + " known actions, "
					+ mKnownCategories.length + " categories, "
					+ mKnownSchemes.length
					+ " schemes. Type uses wild card to match all. ";
			s += (mNumReflected != 0) ? "Found " + mNumReflected
					+ " actions by reflection and " + mNumDug
					+ " by walking manifest registrations."
					: "No recent update.";
			Toast.makeText(this, s, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	protected boolean loadKnownActions() {
		ArrayList<String> loaded = new ArrayList<String>();
		try {
			String line;
			BufferedReader r = new BufferedReader(new InputStreamReader(
					openFileInput("actions")));
			line = r.readLine();
			while (line != null) {
				loaded.add(line);
				line = r.readLine();
			}

		} catch (IOException ioe) {
			Log.w(TAG, "failed to read stored actions");
			Toast.makeText(this, "Please update saved actions / categories.",
					Toast.LENGTH_LONG).show();
			return false;
		}
		mKnownBroadcastActions = loaded.toArray(new String[loaded.size()]);
		return true;
	}

	protected boolean loadKnownCategories() {
		ArrayList<String> loaded = new ArrayList<String>();
		try {
			String line;
			BufferedReader r = new BufferedReader(new InputStreamReader(
					openFileInput("categories")));
			line = r.readLine();
			while (line != null) {
				loaded.add(line);
				line = r.readLine();
			}

		} catch (IOException ioe) {
			Log.w(TAG, "failed to read stored actions");
			return false;
		}
		mKnownCategories = loaded.toArray(new String[loaded.size()]);
		return true;
	}

	protected void saveKnownActions() {
		try {
			PrintWriter r = new PrintWriter(openFileOutput("actions",
					MODE_PRIVATE));
			for (String a : mKnownBroadcastActions)
				r.println(a);
			r.flush();
			r.close();
		} catch (IOException ioe) {
			Log.w(TAG, "failed to save known actions");
		}

	}

	protected void saveKnownCategories() {
		try {
			PrintWriter r = new PrintWriter(openFileOutput("categories",
					MODE_PRIVATE));
			for (String c : mKnownCategories)
				r.println(c);
			r.flush();
			r.close();
		} catch (IOException ioe) {
			Log.w(TAG, "failed to save known actions");
		}
	}

	/*
	 * Grabs the recent tasks from the ActivityManager.
	 */
	protected void updateWithRecents() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RecentTaskInfo> rti = am.getRecentTasks(1000,
				ActivityManager.RECENT_WITH_EXCLUDED);
		StringBuffer log = new StringBuffer();
		int count = 0;
		for (RecentTaskInfo c : rti) {
			count++;
			Intent cur = c.baseIntent;
			log.append("received: " + rti.toString() + "\n");
			receiveIntent(RECENT_SOURCE, cur);
			Log.d(TAG, "recent intent added: " + cur.toString()
					+ cur.hashCode());
		}

	}

	protected void updateKnownCategories() {
		Set<String> l = new HashSet<String>();
		// reflect across whatever Intent class is installed on the user's
		// system to grab all static "CATEGORY" fields
		for (Field f : Intent.class.getFields()) {
			// the Intent class has a bunch of constants named "CATEGORY_*",
			// grabbing em...cheap eh.
			if (f.getName().startsWith("CATEGORY")
					&& f.getType() == String.class) {
				try {
					l.add((String) f.get(null));
				} catch (IllegalAccessException e) {
					// should "adjust" protection level of field and retry.
					// but I believe there is no need right now. Maybe a
					// future constant will be private on some platform.
					Log.d(TAG, "Access error on: " + f.getName());
				}
			}
		}

		mKnownCategories = l.toArray(new String[l.size()]);
		saveKnownCategories();
	}

	protected void updateKnownActions() {
		Set<String> l = new HashSet<String>();
		List<Class> classes = new ArrayList<Class>();

		for (String cur : actionHarboringClasses)
			try {
				classes.add(Class.forName(cur));
			} catch (ClassNotFoundException cne) {
				Log.e(TAG, "missing class " + cne.getMessage());
			}

		for (Class c : classes)
			for (Field f : c.getFields()) {
				// actions constants tend to start or end with ACTION
				if ((f.getName().startsWith("ACTION") || f.getName().endsWith(
						"ACTION"))
						&& f.getType() == String.class) {
					try {
						l.add((String) f.get(null));
					} catch (Exception e) {
						// should "adjust" protection level of field and retry.
						Log.d(TAG, "Access error on: " + c.getName() + " : "
								+ f.getName() + " " + e.getMessage());
					}
				}
			}
		this.mNumReflected = l.size();
		findMoreActions(l);
		mKnownBroadcastActions = l.toArray(new String[l.size()]);
		saveKnownActions();
	}

	protected void findMoreActions(Set<String> l) {
		// walk the XML registrations for every package on the system looking
		// for places where broadcast receivers are registered.
		for (PackageInfo pi : getPackageManager().getInstalledPackages(
				PackageManager.GET_DISABLED_COMPONENTS)) {
			try {
				XmlResourceParser x = createPackageContext(pi.packageName, 0)
						.getAssets().openXmlResourceParser(
								"AndroidManifest.xml");
				int eventType = x.getEventType();
				// looking for receiver tag, containing action tag(s), contains
				// a name attribute has a value that is the known action.
				int state = 0; // 0 out, 1 in receiver
				String n;
				while (eventType != XmlPullParser.END_DOCUMENT) {
					switch (eventType) {
					case XmlPullParser.START_TAG:
						n = x.getName();
						n = (null == n) ? "" : n.toLowerCase();
						if (n.equals("receiver"))
							state = 1;
						else if (1 == state && n.equals("action"))
							for (int i = 0; i < x.getAttributeCount(); i++)
								if (x.getAttributeName(i).equalsIgnoreCase(
										"name"))
									l.add(x.getAttributeValue(i));
						break;
					case XmlPullParser.END_TAG:
						n = x.getName();
						n = (null == n) ? "" : n.toLowerCase();
						if (n.equals("receiver"))
							state = 0;
						break;
					}
					eventType = x.nextToken();
				}
			} catch (IOException ioe) {
				Log.e(TAG, "IOException opening package: " + pi.packageName);
			} catch (NameNotFoundException name) {
				Log.e(TAG, "NameNotFoundException opening package: "
						+ pi.packageName);
			} catch (XmlPullParserException e) {
				Log.e(TAG, "Exception reading manifest XML for package: "
						+ pi.packageName);
			}
		}
		mNumDug = l.size() - mNumReflected;
	}

	/**
	 * Main reporting interface for internal and external callers to log
	 * received intents. Used by receivers or any other implemented classes
	 * registered by the system to move intents into the sniffer. Callers
	 * provide the source of their intent, as well as the intent itself.
	 * 
	 * @param receiverName
	 * @param i
	 */
	public void receiveIntent(String receiverName, Intent i) {
		Collection<FilterComparison> l = mReceivedIntents.get(receiverName);
		if (null == l) {
			l = new LinkedHashSet<FilterComparison>();
			mReceivedIntents.put(receiverName, l);
		}
		l.add(new FilterComparison(i));
	}

	/**
	 * Registers or unregisters some set of action receivers, if no action is
	 * provided, it just acts on everything the sniffer knows about.
	 * 
	 * @param action
	 *            the action to register or unregister, or null for all known
	 * @param register
	 *            true means register, false means unregister
	 */
	public void registerBuddy(String action, boolean register) {
		if (action == null && register)
			try {
				HttpReceiver lr;
				IntentFilter many;

				many = new IntentFilter();
				for (String a : mKnownBroadcastActions)
					many.addAction(a); // more the better
				for (String cat : mKnownCategories)
					many.addCategory(cat); // the more the matchier

				/*
				 * lr = new LoggingReceiver(ACTION_ONLY_SOURCE, this);
				 * this.registerReceiver(lr, many); mReceivers.add(lr);
				 */
				for (String s : mKnownSchemes)
					many.addDataScheme(s);

				lr = new HttpReceiver(ACTION_AND_DATA_SOURCE, this);
				this.registerReceiver(lr, many);
				mReceivers.add(lr);

				for (String s : mKnownMimeTypes) {
					try {
						many.addDataType(s + "/*");
					} catch (MalformedMimeTypeException mmte) {
						Log.d(TAG, "Bad MIME type: " + mmte.getMessage());
					}
				}

				lr = new HttpReceiver(ACTION_AND_DATA_TYPE_SOURCE, this);
				this.registerReceiver(lr, many);
				mReceivers.add(lr);

				// NO ACTION
				many = new IntentFilter();
				for (String s : mKnownSchemes)
					many.addDataScheme(s);
				for (String cat : mKnownCategories)
					many.addCategory(cat);

				lr = new HttpReceiver(WILD_ACTION_AND_DATA_SOURCE, this);
				this.registerReceiver(lr, many);
				mReceivers.add(lr);

			} catch (Exception e) {
				Log.e("IntentSniffer", "Error registering for reciever: "
						+ action + e.toString() + e.getMessage() + "\n");
			}
		if (!register) {
			for (BroadcastReceiver r : mReceivers)
				this.unregisterReceiver(r);
			mReceivers.clear();
		}
	}

	public void updateView() {
		StringBuffer newText = new StringBuffer();
		for (Entry<String, Collection<FilterComparison>> c : mReceivedIntents
				.entrySet()) {
			String s = c.getKey();
			if (c.getValue() != null && mReporting.contains(s))
				for (FilterComparison i : c.getValue()) {
					Intent cur = i.getIntent();
					newText.append(cur.toString());
					if (cur.hasFileDescriptors())
						newText.append(" Contains file descriptor ");
					if (mDetails)
						newText.append(describeDetails(cur));
					newText.append(" from ");
					newText.append(s);
					newText.append("\n\n");
				}
		}
		mTextView.setText(newText);
		
	}

	protected StringBuffer describeDetails(Intent i) {
		StringBuffer sb = new StringBuffer();
		try {
			Bundle b = i.getExtras();
			if (null == b)
				return sb;
			sb.append(" extras {");
			for (String key : b.keySet()) {
				sb.append(key);
				sb.append(" - (");
				sb.append(null == b.get(key) ? "null" : b.get(key).toString());
				sb.append(")\n");
			}
			sb.append("}");
		} catch (RuntimeException rte) {
			sb.append("Can't describe bundle: ");
			sb.append(rte.toString());
		}
		if (i.getCategories() != null)
			for (String c : i.getCategories()) {
				sb.append("has category: ");
				sb.append(c);
				sb.append("\n");
			}
		return sb;
	}
}