/*
 * This is a custom list adapter used to bind data to the SeriesScoreListActivity list view.
 */

package com.overs.sailscore;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SeriesScoreListAdapter extends BaseAdapter {
	protected static ArrayList<ScoresObj> combinedList;
	private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") }; 

	private LayoutInflater mInflater;
	
	public SeriesScoreListAdapter(Context context, ArrayList<ScoresObj> listToDisplay) {
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
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.score_row, null);
			holder = new ViewHolder();
			holder.position = (TextView) convertView.findViewById(R.id.position);
			holder.helm = (TextView) convertView.findViewById(R.id.helm);
			holder.crew = (TextView) convertView.findViewById(R.id.crew);
			holder.sailno = (TextView) convertView.findViewById(R.id.sailno);
			holder.boatclass = (TextView) convertView.findViewById(R.id.boatclass);
			holder.club = (TextView) convertView.findViewById(R.id.club);
			holder.gross_points = (TextView) convertView.findViewById(R.id.gross_pts);
			holder.nett_points = (TextView) convertView.findViewById(R.id.net_pts);
			holder.race_results = (TextView) convertView.findViewById(R.id.race_results);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.position.setText(Integer.toString(combinedList.get(position).getPosition()));
		holder.helm.setText(combinedList.get(position).getHelm());
		holder.crew.setText(combinedList.get(position).getCrew());
		holder.sailno.setText(combinedList.get(position).getSail());
		holder.boatclass.setText(combinedList.get(position).getBoatClass());
		holder.club.setText(combinedList.get(position).getClub());
		holder.gross_points.setText(Float.toString(combinedList.get(position).getGrossPts()));
		holder.nett_points.setText(Float.toString(combinedList.get(position).getNettPts()));
		holder.race_results.setText(combinedList.get(position).getsRaceResults());
		int colorPos = position % colors.length;  
		convertView.setBackgroundColor(colors[colorPos]);
		holder.position.setTextColor(Color.BLACK);
		holder.helm.setTextColor(Color.BLACK);
		holder.crew.setTextColor(Color.BLACK);
		holder.sailno.setTextColor(Color.BLACK);
		holder.boatclass.setTextColor(Color.BLACK);
		holder.gross_points.setTextColor(Color.BLACK);
		holder.nett_points.setTextColor(Color.BLACK);
		holder.race_results.setTextColor(Color.BLACK);
		return convertView;
	}

	static class ViewHolder {
		TextView position;
		TextView helm;
		TextView crew;
		TextView sailno;
		TextView club;
		TextView boatclass;
		TextView gross_points;
		TextView nett_points;
		TextView race_results;
	}
}
