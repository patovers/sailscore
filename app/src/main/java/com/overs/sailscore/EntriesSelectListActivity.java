/*
 * This list activity is for selecting competitor entries in a race series.
 * The list view presents the user with a list of all the entries and a 
 * checkbox to select an entry for addition to a series.
 * If a checkbox is check the results table is populated with a row for each race in the series
 * for that competitor entry.
 * If a checkbox that was checked is subsequently unchecked the result rows are deleted for that entry.
 */

package com.overs.sailscore;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class EntriesSelectListActivity extends ListActivity{
	private Button mConfirmSelectionButton;
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId;
	private ArrayList<rowObj> combinedList;
	private EntriesSelectListAdapter mAdapter;


    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entries_select_list);
        mConfirmSelectionButton = (Button) findViewById(R.id.confirm_entries_button);
        mDbHelper = new SailscoreDbAdapter(this);
        combinedList = new ArrayList<rowObj>();
        mAdapter = new EntriesSelectListAdapter (this, combinedList);
        mDbHelper.open();
        registerAllButtonListeners();
    }

	public void registerAllButtonListeners() {
        mConfirmSelectionButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId); // To get the number of races
        		int numRaces = seriesCursor.getInt(seriesCursor.getColumnIndexOrThrow(SailscoreDbAdapter.KEY_NUMRACES));
        		seriesCursor.close();
        		boolean[] entriesSelected = getSelected(); // bitmap of currently selected entries in the list
				if (entriesSelected.length == 0) {
					AlertDialog.Builder builder1 = new AlertDialog.Builder(EntriesSelectListActivity.this);
					builder1.setMessage(R.string.select_enough_competitors)
					.setTitle(R.string.invalid_competitor_selection)
					.setCancelable(false)
					.setPositiveButton(R.string.selection_accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							//mNumRaces.selectAll();
							//mNumRaces.requestFocus();
						}
					});
					builder1.create().show();
					finish();
				} else {
					boolean[] entriesDeselected = getDeselected(entriesSelected); // bitmap of old entries to delete
					boolean[] entriesToAdd = getToAdd(entriesSelected); // bitmap of new entries to add
					insertResultRows(entriesToAdd, numRaces); // add lines to results table
					deleteResultRows(mRowId, entriesDeselected); // remove lines from results table
					finish();
				}
        	}
        });
	}

	/* Method to return a boolean array of all entries where the ones that are in the series are set to true
	 * and the ones not in the series are set to false.
	 * This method is useful in other methods where a list of current entries is required before deciding
	 * which ones to add or delete in the series being worked on.
	 */
	private boolean[] getListOfEntries (Long series) {
		boolean[] listOfEntries;
		Cursor selectionCursor = mDbHelper.fetchEntriesFromSeries(series);
		Cursor entriesCursor = mDbHelper.fetchAllEntries();
		listOfEntries = new boolean[entriesCursor.getCount()];
		int index = 0;
		if (entriesCursor != null && entriesCursor.moveToFirst()) { // moveToFirst returns false if cursor is empty
			do {
				//Find the id of each entry
				int entryId = entriesCursor.getInt(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
				// Now see if the entryId matches one of those in the selectionCursor
				if (selectionCursor != null && selectionCursor.moveToFirst()) {
					do {
						int currentId = selectionCursor.getInt(selectionCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
						if (currentId == entryId) {
							listOfEntries[index] = true;
						}
					} while (selectionCursor.moveToNext());
				}
				index++;
			} while (entriesCursor.moveToNext());
		}
		selectionCursor.close();
		entriesCursor.close();
		return listOfEntries;
	}


	/* Method to add a result row in the database for all entries where a 1 is set in the array
	 * 
	 */
	private void insertResultRows(boolean[] entriesSelected, int numRaces) {
		Cursor entryCursor = mDbHelper.fetchAllEntries();
		entryCursor.moveToFirst();
		for (int entry=0; entry < entriesSelected.length; entry++) {
			if (entriesSelected[entry] == true) {
				Long entryId = entryCursor.getLong(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
				for (int race= 1; race <= numRaces; race++) {
					//                      competitor series  race  result (default no code)
					mDbHelper.addResultRow (entryId, mRowId, race, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0);
				}
			}
			entryCursor.moveToNext();
		}
		entryCursor.close();
	}
	
	// Method to delete all result rows for an entry no longer in the series
	private void deleteResultRows(Long seriesId, boolean[] entriesDeselected) {
		Cursor entryCursor = mDbHelper.fetchAllEntries();
		entryCursor.moveToFirst();
		for (int i=0; i<entriesDeselected.length; i++) {
			if (entriesDeselected[i] == true) {
				int entryId = entryCursor.getInt(entryCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
				mDbHelper.deleteResultsBySeriesEntry(seriesId, Integer.toString(entryId));
			}
			entryCursor.moveToNext();
		}
		entryCursor.close();
	}

	// Method to get list of all checked items in the listView
	private boolean[] getSelected () {
		ListView list = getListView();
		boolean[] listSelected;
		// We need the total in the list, not just those visible
		// which we would get with getChildCount
		int EntryCount = list.getCount(); 
		listSelected = new boolean[EntryCount];
		for (int i=0;i<EntryCount;i++) {
			// To get the full data set we need to look inside the adapter
			Boolean cboxState = EntriesSelectListAdapter.combinedList.get(i).getCheckState();
			listSelected[i] = cboxState;
		}
		return listSelected;
	}
	
	/* Method to get the list of entries to delete from all the ones in the result table
	 * Takes all currently checked as input and reads the database for all those already in the series
	 * and comes back with a list of those to remove as a boolean array representing all entries.
	 */
	private boolean[] getDeselected (boolean[] listSelected) {
		boolean[] listOfEntries;
		boolean[] listDeselected;
		listDeselected = new boolean[listSelected.length];
		listOfEntries = new boolean[listSelected.length];
		listOfEntries = this.getListOfEntries(mRowId);
		// Work out which ones are old ones to get deleted
		for(int i=0; i<listSelected.length; i++) {
			if (listOfEntries[i] == true && listSelected[i] == false) {
				listDeselected[i] = true;
			}
		}
		return listDeselected;
	}
	
	private boolean[] getToAdd (boolean[] listSelected) {
		boolean[] listOfEntries;
		boolean[] listToAdd;
		listToAdd = new boolean[listSelected.length];
		listOfEntries = new boolean[listSelected.length];
		listOfEntries = this.getListOfEntries(mRowId);
		// Work out which ones are old ones to get deleted
		for(int i=0; i<listSelected.length; i++) {
			if (listOfEntries[i] == false && listSelected[i] == true) {
				listToAdd[i] = true;
			}
		}
		return listToAdd;
	}

    // Here the rowId is the ID of the series to work with from the series table
	private void setRowIdFromIntent() {
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_ROWID)
					: null;
		}
	}


	private void fillData() {
		Cursor entriesCursor = mDbHelper.fetchAllEntries();
		int EntryCount = entriesCursor.getCount();
		boolean[] listOfEntries;
		listOfEntries = new boolean[EntryCount];
		listOfEntries = this.getListOfEntries(mRowId);
		// Fill the list with the entries from the entriesCursor
		if (entriesCursor != null && entriesCursor.moveToFirst()) {
			for (int i = 0; i < entriesCursor.getCount(); i++) {
				// Declare a new rowObj(ect) for each line in the entriesCursor (i.e. each entry)
				rowObj combinedObj = new rowObj();
				combinedObj.setHelm(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM)));
				combinedObj.setCrew(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW)));
				combinedObj.setSail(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO)));
				combinedObj.setBoatClass(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS)));
				// The checkbox is conditionally set using the listOfEntries
				combinedObj.setCheckState(listOfEntries[i]); 
				// Finally add the completed object to the list
				combinedList.add(combinedObj);
				entriesCursor.moveToNext();
			}
		}
		entriesCursor.close();

		// Experimental sorting to display the competitors in alphabetic order
/*		Collections.sort(combinedList, new Comparator<rowObj> () {
			@Override
			public int compare(rowObj arg0, rowObj arg1) {
				return (String.CASE_INSENSITIVE_ORDER.compare(arg0.getHelm(), arg1.getHelm()));
				
			}});
*/
/*		Collections.sort(combinedList, new Comparator<rowObj> () {
			@Override
			public int compare(rowObj arg0, rowObj arg1) {
				return (Integer.parseInt(arg0.getSail()) - Integer.parseInt(arg1.getSail()));
			}});
*/		
        final ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
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
		setRowIdFromIntent(); // Get the series Id
		combinedList.clear();
		fillData();
	}

    
}
