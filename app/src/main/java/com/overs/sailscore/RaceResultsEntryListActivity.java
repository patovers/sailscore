package com.overs.sailscore;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RaceResultsEntryListActivity extends ListActivity {
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private Long raceId; // This also comes from the intent and is needed to put results against the right race
	private Button mSaveResultsButton;
	private TextView mRaceText;
	private ArrayList<EntryResultObj> combinedList;
	private RaceResultsEntryListAdapter mAdapter;
	//private ListView mListView;


	/** Called when the activity is first created. */
 	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.race_results_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mSaveResultsButton = (Button) findViewById(R.id.confirm_results_button);
        mRaceText = (TextView) findViewById(R.id.race_result_id);
        combinedList = new ArrayList<EntryResultObj>();
        mAdapter = new RaceResultsEntryListAdapter (this, combinedList);
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        registerButtonListener();
    }

 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 	    switch (item.getItemId()) {
 	    // Respond to the action bar's Up/Home button
 	    case android.R.id.home:
 	        NavUtils.navigateUpFromSameTask(this);
 	        return true;
 	    }
 	    return super.onOptionsItemSelected(item);
 	} 	
 	
	private void registerButtonListener() {
        mSaveResultsButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		int[] resultsList = getEnteredResults(); // Get all the result values that have been entered
        		int[] resultCodes = getEnteredCodes();
        		int[] redress = getEnteredRedress();
        		boolean[] codePriorities = getEnteredPriorities();
        		Cursor entriesCursor = mDbHelper.fetchEntriesFromSeries(mRowId); 
        		// This loop goes through the competitors to get the results for one race
        		// We need to know the ID of the competitor to do this
        		for (int i = 0; i< resultsList.length; i++) {
        			Long entryId = entriesCursor.getLong(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
        			// Firstly if there is a 0 result and a 0 resultCode, fix it to be DNC
        			// Note that if a result was never entered or a code was never selected we get a DNC 
        			// as a result of this fix.
        			// If there is a codePriority with 0 result then treat the code as real
        			if (resultsList[i] == 0) {
        				if (!codePriorities[i]) {
        					resultCodes[i] = 1;
        				}
        			}
        			if (codePriorities[i]) { // indicates result code was modified
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
        			// Update the database with a result for each competitor
        			//                     entry    series  race   result                     code            redress
        			mDbHelper.updateResult(entryId, mRowId, raceId, resultsList[i], 0, 0, 0, 0, resultCodes[i], redress[i], 0, 0, 0, 0, 0, 0);
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
				EntryResultObj combinedObj = new EntryResultObj();
				String helm = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM));
				String crew = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW));
				String boatClass = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS));
				String sailNo = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO));
				String competitor = helm + "\n" + crew + "\n" + boatClass + " " + sailNo;
				combinedObj.setCompetitor(competitor);
				String result = raceCursor.getString(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RESULT));
				int resultCode = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				int redressPosition = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
				combinedObj.setSpinPosition(resultCode);
				combinedObj.setRedressPosition("");
				switch (resultCode) {
				// Valid result, RDG and DPIz\\
				case 11:
					combinedObj.setRedressPosition(Integer.toString(redressPosition));
				case 0:	case 12: case 13: case 14: case 15:
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
	private int[] getEnteredResults() {
		ListView list = getListView();
		int[] resultsList;
		int resultCount = list.getCount();
		resultsList = new int[resultCount];
		for (int i=0;i<resultCount;i++) {
			String sResult = RaceResultsEntryListAdapter.combinedList.get(i).getResult();
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
			resultCodes[i] = RaceResultsEntryListAdapter.combinedList.get(i).getSpinPosition();
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
				positions[i] = positions.equals("") ? 0 : Integer.parseInt(RaceResultsEntryListAdapter.combinedList.get(i).getRedressPosition());
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
			codePriorities[i] = RaceResultsEntryListAdapter.combinedList.get(i).getCodePriority();
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
		setIdsFromIntent();
		fillData();
	}

}
