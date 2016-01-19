/*
 * This is a custom list adapter used to bind data to the EntriesSelectListActivity list view.
 */

package com.overs.sailscore;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class EntriesSelectListAdapter extends BaseAdapter {
	protected static ArrayList<rowObj> combinedList;

	private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") }; 

	private LayoutInflater mInflater;

	public EntriesSelectListAdapter(Context context, ArrayList<rowObj> listToDisplay) {
		combinedList = listToDisplay;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return combinedList.size();
	}

	@Override
	public Object getItem(int position) {
		return combinedList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.entry_row_select, null);
			holder = new ViewHolder();
			holder.helm = (TextView) convertView.findViewById(R.id.helm);
			holder.crew = (TextView) convertView.findViewById(R.id.crew);
			holder.sailno = (TextView) convertView.findViewById(R.id.sailno);
			holder.boatclass = (TextView) convertView.findViewById(R.id.boatclass);
			holder.check = (CheckBox) convertView.findViewById(R.id.check1);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		/* The click listener is required to spot when the checkbox state changes and update the cached view.
		 * Without this the checkbox states are not maintaine when the list is scrolled.
		 */
		
		final OnClickListener checkListener = new OnClickListener() {
			@Override
			public void onClick (View v) {
				CheckBox cb = (CheckBox) v;
				Boolean check = cb.isChecked();
				combinedList.get(position).setCheckState(check);
			}
		};
		
		holder.check.setOnClickListener(checkListener);
		//holder.check.setButtonDrawable(android.R.drawable.checkbox_off_background);
		
		holder.helm.setText(combinedList.get(position).getHelm());
		holder.sailno.setText(combinedList.get(position).getSail());
		holder.crew.setText(combinedList.get(position).getCrew());
		holder.boatclass.setText(combinedList.get(position).getBoatClass());
		holder.check.setChecked(combinedList.get(position).getCheckState());
		int colorPos = position % colors.length;  
		convertView.setBackgroundColor(colors[colorPos]);  
		holder.helm.setTextColor(Color.BLACK);
		holder.sailno.setTextColor(Color.BLACK);
		holder.boatclass.setTextColor(Color.BLACK);
		return convertView;
	}

	static class ViewHolder {
		TextView helm;
		TextView crew;
		TextView sailno;
		TextView boatclass;
		CheckBox check;
	}
}
