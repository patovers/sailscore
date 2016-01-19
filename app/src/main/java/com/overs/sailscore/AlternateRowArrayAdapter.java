/*
 * This is a custom list adapter used to bind data to the SeriesScoreListActivity list view.
 */

package com.overs.sailscore;

import android.content.Context;  
import android.graphics.Color;  
import android.view.View;  
import android.view.ViewGroup;  
import android.widget.ArrayAdapter;  
import android.widget.TextView;
  
/** 
* Cursor adapter to show alternate rows in different colours 
* to aid readability
*/  
public class AlternateRowArrayAdapter extends ArrayAdapter<Object>{  
  
	private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") };  
	public AlternateRowArrayAdapter(Context context, int layout, String[] values) {
		super(context, layout, values);
	}  
	
	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View view = super.getView(position, convertView, parent);  
		//TextView textView = (TextView) super.getView(position, convertView, parent);
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		textView.setTextColor(Color.BLACK);
		int colorPos = position % colors.length;  
		view.setBackgroundColor(colors[colorPos]);
		return view;  
	}  
}  
