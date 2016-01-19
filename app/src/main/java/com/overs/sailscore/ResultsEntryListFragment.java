package com.overs.sailscore;

import java.util.ArrayList;

import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class ResultsEntryListFragment extends ListFragment {
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private Long entryId; // This also comes from the intent and is needed to put results against the right boat
	private Button mSaveResultsButton;
	private TextView mEntryText;
	private ArrayList<ResultObj> combinedList;
	private ResultsEntryListAdapter mAdapter;
	
	
	/** Called when the activity is first created. */
 	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.results_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mSaveResultsButton = (Button) getActivity().findViewById(R.id.confirm_results_button);
        mEntryText = (TextView) getActivity().findViewById(R.id.entry_result_id);
        combinedList = new ArrayList<ResultObj>();
        mAdapter = new ResultsEntryListAdapter (getActivity(), combinedList);
        mDbHelper = new SailscoreDbAdapter(getActivity());
        mDbHelper.open();
        Bundle args = new Bundle();
        args = getArguments();
        mRowId = args.getLong("series_name", 0);
        entryId = args.getLong("entry", 1);
        //getArguments().getLong(SailscoreDbAdapter.KEY_ENTRY, entryId);
        registerButtonListener();
    }
 	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    	Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.results_list, container, false);
    }
 	
	private void registerButtonListener() {
        mSaveResultsButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		int[] resultsList = getEnteredResults(); // Get all the result values that have been entered
        		int[] resultCodes = getEnteredCodes();
        		int[] redress = getEnteredRedress();
        		boolean[] codePriorities = getEnteredPriorities();
        		Long raceId;
        		for (int i = 0; i< resultsList.length; i++) {
        			raceId = Long.valueOf(i+1);
        			// Firstly if there is a 0 result and a 0 resultCode, fix it to be DNC
        			// Note that if the save button was never pressed we get a DNC 
        			// as a result of this fix.
        			if (resultsList[i] == 0 && resultCodes[i] ==0) {
        				resultCodes[i] = 1;
        				codePriorities[i] = true;
        			}
        			// Then we look to see if anything was actually altered before saving
        			if (codePriorities[i]) {
        				switch (resultCodes[i]) {
        					case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10:
        						resultsList[i] = 0; // clear anything that was entered
        						redress[i] = 0;
        						break;
        					case 11: // nothing to do
        						break;
        					case 12: case 13: case 14: case 15:
        						redress[i] = 0; // remove anything that was entered
        						break;
        				}
        			} else {
        				resultCodes[i] = 0;
        				redress[i] = 0;
        			}
        			mDbHelper.updateResult(entryId, mRowId, raceId, resultsList[i], 0, 0, 0, 0, resultCodes[i], redress[i], 0, 0, 0, 0, 0, 0);
        		}
				//setResult(1);
        		//finish();
        	}
        });
	}
	
	private void fillData() {
		// This cursor returns all results for the (entryId) competitor in the (mRowId) series
		Cursor entryCursor = mDbHelper.fetchEntry(entryId);
		String helm = entryCursor.getString(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM));
		String crew = entryCursor.getString(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW));
		String boatClass = entryCursor.getString(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS));
		String sailNo = entryCursor.getString(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO));
		String entry = helm.toString() + ", " + crew.toString() + ", " + boatClass.toString() + " " + sailNo.toString();
		entryCursor.close();
		mEntryText.setText(entry);
		Cursor resultsCursor = mDbHelper.getResults(entryId, mRowId);
		// Process the cursor to populate the elements in the ArrayList before binding it to the view
		if (resultsCursor != null && resultsCursor.moveToFirst()) {
			for (int i = 0; i < resultsCursor.getCount(); i++) {
				// Declare a new resultObj(ect) for each line in the resultsCursor (i.e. each result)
				ResultObj combinedObj = new ResultObj();
				combinedObj.setRaceNumber(Integer.toString(resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RACE))));
				// Try this method instead of the commented out one
				String result = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RESULT));
				int resultCode = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				int redressPosition = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));

				combinedObj.setSpinPosition(resultCode);
				combinedObj.setRedressPosition("");
				switch (resultCode) {
				// Valid result, RDG and DPIz\\
				case 11:
					combinedObj.setRedressPosition(Integer.toString(redressPosition));
				case 0:	case 12: case 13: case 14: case 15:
					if (result.equals("0")) {
						combinedObj.setResult("");						
					} else {
						combinedObj.setResult(result);						
					}
					combinedObj.setResult(result.equals("0") ? "" : result);
					break;
				// Next group require a code only so clear the result and redress fields
				case 1: case 2: case 3: case 4:
				case 5: case 6: case 7:	case 8:
				case 9: case 10:
					combinedObj.setResult("");
					break;
				}

				// Final adjustment if result is 0 then we change the
				// spinner to show DNC
				if (resultCode == 0 && result.equals("0")) {
					combinedObj.setSpinPosition(1);
				}

				// Finally add the completed object to the list
				combinedList.add(combinedObj);
				resultsCursor.moveToNext();
			}
		}
		resultsCursor.close();
        //final ListView list = (ListView) mListView.findViewById(android.R.id.list);
        final ListView list = getListView();
        list.setAdapter(mAdapter);
        list.setItemsCanFocus(true);
	}
	
    // Here the rowId is the ID of the series to work with from the series table
	// and the entryId also from the intent is the entry id
/*	private void setIdsFromIntent() {
		if (mRowId == null) {
			Bundle extras = getActivity().getIntent().getExtras();
			mRowId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_SERIES)
					: null;
			entryId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_ENTRY)
					: null;
		}
	}
*/	
	// Method to get results entered in the listView
	private int[] getEnteredResults() {
		ListView list = getListView();
		int[] resultsList;
		int resultCount = list.getCount();
		resultsList = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			//int position = (int) ResultsEntryListAdapter.combinedList.get(i).getSpinPosition();
			String sResult = ResultsEntryListAdapter.combinedList.get(i).getResult();
			int result;
			try {
				result = sResult.equals("") ? 0 : Integer.parseInt(sResult);
			} catch (NumberFormatException e) {
				result = 0; // for now just put something in that won't break
			}
			if (result > 0) {
				resultsList[i] = result; // Race has a definite result
			} else {
				resultsList[i] = 0;
			}
		}
		return resultsList;
	}

		// Method to return the result codes for all the entered results
	private int[] getEnteredCodes() {
		ListView list = getListView();
		int[] resultCodes;
		int resultCount = list.getCount();
		resultCodes = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			resultCodes[i] = ResultsEntryListAdapter.combinedList.get(i).getSpinPosition();
		}
		return resultCodes;
	}

	private int[] getEnteredRedress() {
		ListView list = getListView();
		int[] positions;
		int resultCount = list.getCount();
		positions = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			try {
				positions[i] = positions.equals("") ? 0 : Integer.parseInt(ResultsEntryListAdapter.combinedList.get(i).getRedressPosition());
			} catch (NumberFormatException e) {
				positions[i] = 0; // for now just put something in that won't break
			}
		}
		return positions;
	}

	private boolean[] getEnteredPriorities() {
		ListView list = getListView();
		boolean[] codePriorities;
		int resultCount = list.getCount();
		codePriorities = new boolean[resultCount];
		for (int i=0;i<resultCount;i++) {
			codePriorities[i] = ResultsEntryListAdapter.combinedList.get(i).getCodePriority();
		}
		return codePriorities;
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mRowId != null) {
			outState.putLong(SailscoreDbAdapter.KEY_ROWID, mRowId);
		}
	}

	
	@Override
	public void onPause() {
		super.onPause();
		mDbHelper.close();
	}

	@Override
	public void onResume() {
		super.onResume();
		mDbHelper.open();
		// clear the previous view. If we don't the previous
		// contents remain and new ones are added below.
		combinedList.clear();
		//setIdsFromIntent();
		fillData();
	}

}
