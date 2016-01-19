package com.overs.sailscore;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
//import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RaceTimesEntryListActivity extends ListActivity {
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private Long raceId; // This also comes from the intent and is needed to put results against the right race
	private Button mSaveResultsButton;
	private TextView mRaceText;
	private ArrayList<EntryTimesObj> combinedList;
	private RaceTimesEntryListAdapter mAdapter;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_times_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mSaveResultsButton = (Button) findViewById(R.id.confirm_results_button);
        mRaceText = (TextView) findViewById(R.id.race_times_id);
        combinedList = new ArrayList<EntryTimesObj>();
        mAdapter = new RaceTimesEntryListAdapter (this, combinedList);
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        registerButtonListener();
    }
	
/* 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 	    switch (item.getItemId()) {
 	    // Respond to the action bar's Up/Home button
 	    case android.R.id.home:
 	        NavUtils.navigateUpFromSameTask(this);
 	        return true;
 	    }
 	    return super.onOptionsItemSelected(item);
 	} 	
*/ 	
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
        		Cursor entriesCursor = mDbHelper.fetchEntriesFromSeries(mRowId); 
        		// This loop goes through the competitors to get the results for one race
        		// We need to know the ID of the competitor to do this
        		for (int i = 0; i< startTimes.length; i++) {
        			Long entryId = entriesCursor.getLong(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
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
        					resultCodes[i] = 0;
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
        			entriesCursor.moveToNext();
        		}
				setResult(1);
        		finish();
        	}
        });
	}

	private void fillData() {
		// This cursor returns all results for the (raceId) race in the (mRowId) series
		Cursor raceCursor = mDbHelper.fetchEntriesByRace(raceId, mRowId); // fetch all races in series
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId); // Just to get the name of the series
		String seriesName = seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES));
		String race = "Series: " + seriesName + ", Race: " + Long.toString(raceId);
		mRaceText.setText(race);
		seriesCursor.close();
		boolean entriesToDisplay = false;
		// Process the cursor to populate the elements in the ArrayList before binding it to the view
		if (raceCursor != null && raceCursor.moveToFirst()) {
			entriesToDisplay = true;
			for (int i = 0; i < raceCursor.getCount(); i++) {
				// Declare a new resultObj for each line in the resultsCursor (i.e. each result)
				EntryTimesObj combinedObj = new EntryTimesObj();
				String helm = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM));
				String crew = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW));
				String boatClass = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS));
				String sailNo = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO));
				String competitor = helm + "\n" + crew + "\n" + boatClass + " " + sailNo;
				combinedObj.setCompetitor(competitor);
				// Try this method instead of the commented out one
				String sStartTime = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_START_TIME));
				String sFinishTime = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_FINISH_TIME));
				int sSecs = Integer.parseInt(sStartTime); // The database only stores times in seconds
				int fSecs = Integer.parseInt(sFinishTime);
				int resultCode = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				int redressPosition = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
				int sMins = sSecs/60;
				sSecs %= 60;
				int fMins = fSecs/60;
				fSecs %= 60;
				int lapsSailed = Integer.parseInt(raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_LAPS)));
				int totalLaps = Integer.parseInt(raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL_LAPS)));

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
				raceCursor.moveToNext();
			}
		}
		raceCursor.close();
        final ListView list = getListView();
        if (entriesToDisplay) {
        	list.setAdapter(mAdapter);
        	list.setItemsCanFocus(true);
        }
	}
	
    // Here the rowId is the ID of the series to work with from the series table
	// and the entryId also from the intent is the entry id
	private void setIdsFromIntent() {
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_SERIES)
					: null;
			raceId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_RACE)
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
			String sSMins = RaceTimesEntryListAdapter.combinedList.get(i).getsMins();
			String sSSecs = RaceTimesEntryListAdapter.combinedList.get(i).getsSecs();
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
			String sFMins = RaceTimesEntryListAdapter.combinedList.get(i).getfMins();
			String sFSecs = RaceTimesEntryListAdapter.combinedList.get(i).getfSecs();
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
			resultCodes[i] = RaceTimesEntryListAdapter.combinedList.get(i).getSpinPosition();
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
				laps[i] = laps.equals("") ? 0 : Integer.parseInt(RaceTimesEntryListAdapter.combinedList.get(i).getLaps());
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
				laps[i] = laps.equals("") ? 0 : Integer.parseInt(RaceTimesEntryListAdapter.combinedList.get(i).getTotalLaps());
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
				positions[i] = positions.equals("") ? 0 : Integer.parseInt(RaceTimesEntryListAdapter.combinedList.get(i).getRedressPosition());
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
			codePriorities[i] = RaceTimesEntryListAdapter.combinedList.get(i).getCodePriority();
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
