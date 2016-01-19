/*
 * This is a custom list adapter used to bind data to the SeriesScoreListActivity list view.
 */

package com.overs.sailscore;

import android.content.Context;  
import android.database.Cursor;  
import android.graphics.Color;  
import android.view.View;  
import android.view.ViewGroup;  
import android.widget.SimpleCursorAdapter;;
  
/** 
* Cursor adapter to show alternate rows in different colours 
* to aid readability
*/  
public class AlternateRowCursorAdapter extends SimpleCursorAdapter{  
  
	private static int flags = FLAG_REGISTER_CONTENT_OBSERVER;
	private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") };  
	public AlternateRowCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to, flags);
	}  
	
	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View view = super.getView(position, convertView, parent);  
		int colorPos = position % colors.length;  
		view.setBackgroundColor(colors[colorPos]);  
		return view;  
	}  
}  
