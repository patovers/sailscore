/*
 * This is a custom list adapter used to bind data to the EntriesSelectListActivity list view.
 */

package com.overs.sailscore;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

    public class ResultsEntryListAdapter extends BaseAdapter {
	protected static ArrayList<ResultObj> combinedList;
	private int[] colors = new int[] { Color.parseColor("#F0F0F0"), Color.parseColor("#D2E4FC") }; 
	private Context mContext;
	private LayoutInflater mInflater;
	public ResultsEntryListAdapter(Context context, ArrayList<ResultObj> listToDisplay) {
		combinedList = listToDisplay;
		mInflater = LayoutInflater.from(context);
		mContext = context;
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

	public int getViewTypeCount() {                 

	    return getCount();
	}

	@Override
	public int getItemViewType(int position) {

	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final int pos = position; // is this right? I'm getting reused data in the list
		final ViewHolder holder;
		combinedList.get(pos).setCodePriority(true);
		// Add an on click listener to this method that will update the source data in the calling activity when items change
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.result_row, null);
			holder = new ViewHolder();
			holder.race_line = (TextView) convertView.findViewById(R.id.race_line);
			holder.race_number = (TextView) convertView.findViewById(R.id.race_number);
			holder.result = (EditText) convertView.findViewById(R.id.result);
			holder.result_code_spinner = (Spinner) convertView.findViewById(R.id.result_code_spinner);
			holder.rdg_pos = (EditText) convertView.findViewById(R.id.rdg_pos);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
		            mContext, R.array.result_codes, R.layout.codes_spinner_item);
		    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		    holder.result_code_spinner.setAdapter(adapter);
			convertView.setTag(holder);	
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		/* If the user enters a result code using the spinner then put the result code into the
		 * data structure and set the numerical result (string) to a null value.
		 */
		
		final OnItemSelectedListener spinnerListener = new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> av, View v, int spinPosition, long id) {
				combinedList.get(pos).setSpinPosition(spinPosition);
				if (spinPosition == 0) {
					combinedList.get(pos).setCodePriority(false);
				} else {
					combinedList.get(pos).setCodePriority(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		};
		
		
		final TextWatcher resultTextWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0) {
				} else {
					combinedList.get(pos).setResult(s.toString());
					combinedList.get(pos).setCodePriority(false);
				}
			}
	          @Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	          @Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
	       };
		
		final TextWatcher rdgTextWatcher = new TextWatcher() {
	          @Override
	          public void afterTextChanged(Editable s) {
	        	  if (s.length() == 0) {
	        	  } else {
	        		  combinedList.get(pos).setRedressPosition(s.toString());
	        		  combinedList.get(pos).setCodePriority(true);
	        	  }
	          }
	          @Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	          @Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
	       };
			
		
		/* If the user enters a numerical result then check it is positive and put it into the
		 * data structure. At the same time set the spin position to 0 to indicate this is a 
		 * valid result.
		 */

		holder.result_code_spinner.setOnItemSelectedListener(spinnerListener);
		holder.result.addTextChangedListener(resultTextWatcher);
		holder.rdg_pos.addTextChangedListener(rdgTextWatcher);

		holder.race_line.setText(R.string.race_line);
		holder.race_number.setText(combinedList.get(pos).getRaceNumber());
		holder.result_code_spinner.setSelection(combinedList.get(pos).getSpinPosition());
		holder.result.setText(combinedList.get(pos).getResult());
		holder.rdg_pos.setText(combinedList.get(pos).getRedressPosition());

		int colorPos = pos % colors.length;  
		convertView.setBackgroundColor(colors[colorPos]);
		holder.race_line.setTextColor(Color.BLACK);
		holder.race_number.setTextColor(Color.BLACK);
		holder.result.setTextColor(Color.BLACK);
		if (pos == 0) {
			holder.result.requestFocus();
		}
		return convertView;
	}

	static class ViewHolder {
		TextView race_line;
		TextView race_number;
		EditText result;
		Spinner result_code_spinner;
		EditText rdg_pos;
	}
}
