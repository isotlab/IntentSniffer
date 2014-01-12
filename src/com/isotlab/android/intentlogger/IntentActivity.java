package com.isotlab.android.intentlogger;

import java.io.Serializable;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.content.Intent;
import android.util.Log;

public class IntentActivity implements Serializable{

	private String action;
	private String category;
	private String component;
	private String details;
	private String timestamp;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss:SSS";
	
	public IntentActivity (Intent intent) {
		this.action = intent.getAction() != null ? intent.getAction() : "NULL";
	    this.component = intent.getComponent() != null ? intent.getComponent().flattenToString() : "NULL";
	    // For now just get the first Category...
	    this.category = intent.getCategories() != null && intent.getCategories().iterator().hasNext() ? intent.getCategories().iterator().next(): "NULL";
	    this.details = intent.getExtras() != null ? intent.getExtras().toString() : "NULL";
	    this.timestamp = now();
	}
	
	public IntentActivity (String action, String category, String component, String details) {
		this.action = action;
	    this.component = component;
	    // For now just get the first Category...
	    this.category = category;
	    this.details = details;
	    this.timestamp = now();
	    System.out.print(this);
	}

	
	@Override
	public boolean equals(Object obj){
	    if (obj == null) return false;
	    if (obj == this) return true;
	    if (!(obj instanceof IntentActivity)) {
	    	Log.d("INTENT ACTIVITY", "Not equal an intent activity");
	    	return false;
	    }
	    IntentActivity compare = (IntentActivity)obj;
	    
	    if (compare.getAction().equalsIgnoreCase(this.action) &&
	    	compare.getCategory().equalsIgnoreCase(this.category) &&
	    	compare.getComponent().equalsIgnoreCase(this.component) &&
	    	compare.getDetails().equalsIgnoreCase(this.details)
	    	&& compare.getTimestamp().equalsIgnoreCase(this.timestamp)) {
	    	return true;
	    }
	    else
	    	Log.d("INTENT ACTIVITY", "Not equal at all");
	    	return false;
	}
	
	@Override
	public int hashCode() {
		Log.d("INTENT ACTIVITY", "returning hashcode");
		return this.action.hashCode() + this.category.hashCode() + this.component.hashCode();
	}
	
	public String getAction() {
		return action;
	}


	public String getCategory() {
		return category;
	}


	public String getComponent() {
		return component;
	}


	public String getDetails() {
		return details;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
		}
}
