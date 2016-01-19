package com.overs.sailscore;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class TimesEntryListActivity extends ListActivity {
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private Long entryId; // This also comes from the intent and is needed to put results against the right boat
	private Button mSaveResultsButton;
	private TextView mEntryText;
	private ArrayList<TimesObj> combinedList;
	private TimesEntryListAdapter mAdapter;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.times_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mSaveResultsButton = (Button) findViewById(R.id.confirm_results_button);
        mEntryText = (TextView) findViewById(R.id.entry_result_id);
        combinedList = new ArrayList<TimesObj>();
        mAdapter = new TimesEntryListAdapter (this, combinedList);
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        registerButtonListener();
    }
	
	private void registerButtonListener() {
        mSaveResultsButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		int[] startTimes = getStartTimes();
        		int[] finishTimes = getFinishTimes();
        		int[] resultCodes = getEnteredCodes();
        		int[] lapsSailed = getEnteredLaps();
        		int[] totalLaps = getEnteredTotals();
        		int[] redress = getEnteredRedress();
        		boolean[] codePriorities = getEnteredPriorities();
        		for (int i = 0; i< startTimes.length; i++) {
        			Long raceId = Long.valueOf(i+1);
        			// Firstly if there is a 0 result and a 0 resultCode, fix it to be DNC
        			// Note that if the save button was never pressed we get a DNC 
        			// as a result of this fix.
        			if (finishTimes[i] == 0) {
        				if (!codePriorities[i]) {
            				resultCodes[i] = 1; // DNC code inserted if no result and no code
        				}
        			}
        			if (codePriorities[i]) {
        				switch(resultCodes[i]) {
        				case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10:
        					startTimes[i] = 0;
        					finishTimes[i] = 0;
        					lapsSailed[i] = 0;
        					totalLaps[i] = 0;
        					redress[i] = 0;
        					break;
        				case 11:
        					break;
        				case 12: case 13: case 14: case 15:
    						redress[i] = 0; // remove anything that was entered
    						break;
        				}
        			}
        			else {
        				resultCodes[i] = 0;
        				redress[i] = 0;
        			}
        			mDbHelper.updateResult(entryId, mRowId, raceId, 0, startTimes[i], finishTimes[i], lapsSailed[i], totalLaps[i], resultCodes[i], redress[i], 0, 0, 0, 0, 0, 0);
        		}
				setResult(1);
        		finish();
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
				// Declare a new resultObj for each line in the resultsCursor (i.e. each result)
				TimesObj combinedObj = new TimesObj();
				combinedObj.setRaceNumber(Integer.toString(resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RACE))));
				// Try this method instead of the commented out one
				String sStartTime = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_START_TIME));
				String sFinishTime = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_FINISH_TIME));
				int sSecs = Integer.parseInt(sStartTime); // The database only stores times in seconds
				int fSecs = Integer.parseInt(sFinishTime);
				int resultCode = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				int redressPosition = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
				int sMins = sSecs/60;
				sSecs %= 60;
				int fMins = fSecs/60;
				fSecs %= 60;
				int lapsSailed = Integer.parseInt(resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_LAPS)));
				int totalLaps = Integer.parseInt(resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL_LAPS)));
				// These are default values to be overridden later
				combinedObj.setSpinPosition(resultCode);
				combinedObj.setRedressPosition("");
				switch (resultCode) {
				case 11:
					combinedObj.setRedressPosition(Integer.toString(redressPosition));
				case 0: case 12: case 13: case 14: case 15:
					if (sMins == 0) {
						combinedObj.setsMins("");
					} else {
						combinedObj.setsMins(Integer.toString(sMins));
					}
					if (sSecs == 0) {
						combinedObj.setsSecs("");
					} else {
						combinedObj.setsSecs(Integer.toString(sSecs));
					}
					if (fMins == 0) {
						combinedObj.setfMins("");
					} else {
						combinedObj.setfMins(Integer.toString(fMins));
					}
					if (fSecs == 0) {
						combinedObj.setfSecs("");
					} else {
						combinedObj.setfSecs(Integer.toString(fSecs));
					}
					if (lapsSailed == 0) {
						combinedObj.setLaps("");
					} else {
						combinedObj.setLaps(Integer.toString(lapsSailed));
					}
					if (totalLaps == 0) {
						combinedObj.setTotalLaps("");
					} else {
						combinedObj.setTotalLaps(Integer.toString(totalLaps));
					}
					break;
				case 1: case 2: case 3: case 4:
				case 5: case 6: case 7:	case 8:
				case 9: case 10: 
					combinedObj.setsMins("");
					combinedObj.setsSecs("");
					combinedObj.setfMins("");
					combinedObj.setfSecs("");
					combinedObj.setLaps("");
					combinedObj.setTotalLaps("");
					break;
				}
				
				// Final adjustment if all the time values are 0 then we change the
				// spinner to show DNC
				if (resultCode == 0 && sMins == 0 && sSecs == 0 && fMins == 0 && fSecs == 0) {
					combinedObj.setSpinPosition(1);
				}
				
				// Finally add the completed object to the list
				combinedList.add(combinedObj);
				resultsCursor.moveToNext();
			}
		}
		resultsCursor.close();
        final ListView list = getListView();
        list.setAdapter(mAdapter);
        list.setItemsCanFocus(true);
	}
	
    // Here the rowId is the ID of the series to work with from the series table
	// and the entryId also from the intent is the entry id
	private void setIdsFromIntent() {
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_SERIES)
					: null;
			entryId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_ENTRY)
					: null;
		}
	}
	
	// Method to get results entered in the listView
	private int[] getStartTimes() {
		//String [] resultCodes = this.getResources().getStringArray(R.array.result_codes);
		ListView list = getListView();
		int[] times;
		int resultCount = list.getCount();
		times = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			String sSMins = TimesEntryListAdapter.combinedList.get(i).getsMins();
			String sSSecs = TimesEntryListAdapter.combinedList.get(i).getsSecs();
			int startMins;
			int startSecs;

			try {
				startMins = sSMins.equals("") ? 0 : Integer.parseInt(sSMins);
			} catch (NumberFormatException e) {
				startMins = 0; // for now just put something in that won't break
			}
			try {
				startSecs = sSSecs.equals("") ? 0 : Integer.parseInt(sSSecs);
			} catch (NumberFormatException e) {
				startSecs = 0; // for now just put something in that won't break
			}
			times[i] = startMins * 60 + startSecs;
		}
		return times;
	}

	// Method to get results entered in the listView
	private int[] getFinishTimes() {
		//String [] resultCodes = this.getResources().getStringArray(R.array.result_codes);
		ListView list = getListView();
		int[] times;
		int resultCount = list.getCount();
		times = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			String sFMins = TimesEntryListAdapter.combinedList.get(i).getfMins();
			String sFSecs = TimesEntryListAdapter.combinedList.get(i).getfSecs();
			int finishMins;
			int finishSecs;

			try {
				finishMins = sFMins.equals("") ? 0 : Integer.parseInt(sFMins);
			} catch (NumberFormatException e) {
				finishMins = 0; // for now just put something in that won't break
			}
			try {
				finishSecs = sFSecs.equals("") ? 0 : Integer.parseInt(sFSecs);
			} catch (NumberFormatException e) {
				finishSecs = 0; // for now just put something in that won't break
			}
			times[i] = finishMins * 60 + finishSecs;
		}
		return times;
	}

	// Method to return the result codes for all the entered results
	private int[] getEnteredCodes() {
		ListView list = getListView();
		int[] resultCodes;
		int resultCount = list.getCount();
		resultCodes = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			resultCodes[i] = TimesEntryListAdapter.combinedList.get(i).getSpinPosition();
		}
		return resultCodes;
	}

	// Method to return the result codes for all the entered results
	private int[] getEnteredLaps() {
		ListView list = getListView();
		int[] laps;
		int resultCount = list.getCount();
		laps = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			try {
				laps[i] = laps.equals("") ? 0 : Integer.parseInt(TimesEntryListAdapter.combinedList.get(i).getLaps());
			} catch (NumberFormatException e) {
				laps[i] = 0; // for now just put something in that won't break
			}
		}
		return laps;
	}

	private int[] getEnteredTotals() {
		ListView list = getListView();
		int[] laps;
		int resultCount = list.getCount();
		laps = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			try {
				laps[i] = laps.equals("") ? 0 : Integer.parseInt(TimesEntryListAdapter.combinedList.get(i).getTotalLaps());
			} catch (NumberFormatException e) {
				laps[i] = 0; // for now just put something in that won't break
			}
		}
		return laps;
	}

	// Method to return the result codes for all the entered results
	private int[] getEnteredRedress() {
		ListView list = getListView();
		int[] positions;
		int resultCount = list.getCount();
		positions = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			try {
				positions[i] = positions.equals("") ? 0 : Integer.parseInt(TimesEntryListAdapter.combinedList.get(i).getRedressPosition());
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
			codePriorities[i] = TimesEntryListAdapter.combinedList.get(i).getCodePriority();
		}
		return codePriorities;
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mRowId != null) {
			outState.putLong(SailscoreDbAdapter.KEY_ROWID, mRowId);
		}
	}

	
	@Override
	protected void onPause() {
		super.onPause();
		mDbHelper.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		// clear the previous view. If we don't the previous
		// contents remain and new ones are added below.
		combinedList.clear();
		setIdsFromIntent();
		fillData();
	}

}
