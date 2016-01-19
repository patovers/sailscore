/*
 * This activity allows information about a series, e.g. how many races, to be edited.
 */

package com.overs.sailscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


public class SeriesEditActivity extends Activity{
	private SailscoreDbAdapter mDbHelper;
	private Long mRowId;
	private EditText mSeriesName;
	private EditText mNumRaces;
	private EditText mDiscardProfile;
	private Button mSaveButton;
	private Button mSelectButton;
	private RadioGroup mSeriesType;
	private RadioButton mFleetButton;
	private RadioButton mHcapButton;
    private static final int ENTRY_SELECT = 0;
    private int hcap;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.series_edit);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
		mSeriesName = (EditText) findViewById(R.id.series_name);
		mNumRaces = (EditText) findViewById(R.id.num_races);
		mDiscardProfile = (EditText) findViewById(R.id.discard_profile);
		mSaveButton = (Button) findViewById(R.id.save_button);
		mSelectButton = (Button) findViewById(R.id.select_entries_button);
		mSeriesType = (RadioGroup) findViewById(R.id.radioSeriesType);
		mFleetButton = (RadioButton) findViewById(R.id.radioFleet);
		mHcapButton = (RadioButton) findViewById(R.id.radioHcap);
		// Here mRowId is the identity of the series in the seriesList
		mDbHelper = new SailscoreDbAdapter(this);
		// Open the series list database because we need the series name from it
		mDbHelper.open();
		mRowId = savedInstanceState != null
				? savedInstanceState.getLong(SailscoreDbAdapter.KEY_ROWID)
				: null;
		registerButtonListenersAndSetDefaultText();
	}
    
    
    /* Method to handle button clicks.
     * View has two buttons, one for confirm/save and one for selecting entries.
     * Confirm/save returns to the calling activity.
     * Select starts the EntrySelect activity and expects to return here.
     */
	private void registerButtonListenersAndSetDefaultText() {
        final Intent selectEntries = new Intent(this, EntriesSelectListActivity.class);
		mSaveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// saveState returns a result that indicates errors in the input
				switch (saveState()) {
				case -1: case -2:
					AlertDialog.Builder builder1 = new AlertDialog.Builder(SeriesEditActivity.this);
					builder1.setMessage(R.string.set_number_of_races)
					.setTitle(R.string.invalid_num_races)
					.setCancelable(false)
					.setPositiveButton(R.string.discard_accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							mNumRaces.selectAll();
							mNumRaces.requestFocus();
						}
					});
					builder1.create().show();
					break;
				case -3: case -4:
					AlertDialog.Builder builder2 = new AlertDialog.Builder(SeriesEditActivity.this);
					builder2.setMessage(R.string.badly_formed_discard_profile)
					.setTitle(R.string.bad_discard_profile)
					.setCancelable(false)
					.setPositiveButton(R.string.discard_accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							mDiscardProfile.selectAll();
							mDiscardProfile.requestFocus();
						}
					});
					builder2.create().show();
					break;
				default:	
					setResult(RESULT_OK);
					Toast.makeText(SeriesEditActivity.this, getString(R.string.series_saved_message), Toast.LENGTH_SHORT).show();
					finish();
					break;
				}
			}
		});
		
		
		mSelectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// saveState returns a result that indicates errors in the input
				switch (saveState()) {
				case -1: case -2:
					AlertDialog.Builder builder1 = new AlertDialog.Builder(SeriesEditActivity.this);
					builder1.setMessage(R.string.set_number_of_races)
					.setTitle(R.string.invalid_num_races)
					.setCancelable(false)
					.setPositiveButton(R.string.discard_accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							mNumRaces.selectAll();
							mNumRaces.requestFocus();
						}
					});
					builder1.create().show();
					break;
				case -3: case -4:
					AlertDialog.Builder builder2 = new AlertDialog.Builder(SeriesEditActivity.this);
					builder2.setMessage(R.string.badly_formed_discard_profile)
					.setTitle(R.string.bad_discard_profile)
					.setCancelable(false)
					.setPositiveButton(R.string.discard_accept, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							mDiscardProfile.selectAll();
							mDiscardProfile.requestFocus();
						}
					});
					builder2.create().show();
					break;
				default:	
					selectEntries.putExtra(SailscoreDbAdapter.KEY_ROWID, mRowId);
					startActivityForResult(selectEntries, ENTRY_SELECT);
				break;
				}
			}
		});
		
	}
	
	/* Method to get the rowId from the calling activity.
	 * The rowId is the ID of the series to be worked on.
	 */
	private void setRowIdFromIntent() {
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null
					? extras.getLong(SailscoreDbAdapter.KEY_ROWID)
					: null;
		}
	}
	
	/* Method to handle returning from entry selection.
	 * Opens database, gets the series ID and fills in the info for it.
	 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
		mDbHelper.open();
		setRowIdFromIntent();
    	populateFields();
    }


	@Override
	protected void onPause() {
		super.onPause();
		mDbHelper.close();
	}
	
	/* Standard onResume to restore the view.
	 * Does the same things as if returning from entry selection.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		setRowIdFromIntent();
		populateFields(); 
	}
	
	/* In this method we have to deal with both the seriesList and the series databases.
	 * So, the series is used to create an entry in the seriesList
	 * and also a whole new database for the actual series.
	 * Later we will be able to put results into the new series database.
	 */
	private int saveState() {
		String series = mSeriesName.getText().toString();
		int numRaces;
		try {
			numRaces = Integer.parseInt(mNumRaces.getText().toString());
		} catch (NumberFormatException e) {
			return -1;
		}
		if (numRaces > 99 || numRaces < 1) {
			return -2;
		}
		String discardProfile = mDiscardProfile.getText().toString();
		// First have to check the discard profile is well formed
		String delimeter = "[^0-9]";
		String[] discardList = discardProfile.split(delimeter);
		String fixedProfile = "";
		int discardValue = 0;
		for (int i = 0; i < discardList.length; i++) {
			// work through the characters inspecting any that are not numeric
			int lastDiscardValue = discardValue;
			try {
				discardValue = Integer.parseInt(discardList[i]);
				if (lastDiscardValue > discardValue) {
					return -3;
				}
			} catch (NumberFormatException e) {
				return -4;
			}
			fixedProfile = fixedProfile + discardList[i] + " ";
		}
		discardProfile = fixedProfile;
		// Continue if we have a well formed series of incrementing integers separated by commas.
		// If there is no rowId the series doesn't exist yet.
		int selected = mSeriesType.getCheckedRadioButtonId();
		RadioButton selectedButton = (RadioButton) findViewById(selected);
		String selString = selectedButton.getText().toString();
		hcap = (selString.equalsIgnoreCase("handicap")) ? 1 : 0;
		if (mRowId == null) {
			long id = mDbHelper.createSeries(series, numRaces, discardProfile, hcap);
			if (id > 0) {
				mRowId = id;
			}
		}else {
			// If the series already exists we just update the row in the list
			// At this point the number of races may have changed so need to update
			// various tables as well as the series info.
			Cursor seriesFromDb = mDbHelper.fetchSeries(mRowId);
			Cursor entriesFromDb = mDbHelper.fetchEntriesFromSeries(mRowId);
			int oldNumRaces = Integer.parseInt(seriesFromDb.getString(seriesFromDb.getColumnIndex(SailscoreDbAdapter.KEY_NUMRACES)));
			seriesFromDb.close();
			int numEntries = entriesFromDb.getCount();
			if (oldNumRaces > numRaces) { // we have races to delete
				for (int i=numRaces+1; i<=oldNumRaces; i++) {
					mDbHelper.deleteResultByRace(mRowId, i); // pass it the series id and race number
				}
			}
			else if (oldNumRaces < numRaces) { // we have races to add
				entriesFromDb.moveToFirst();
				for (int entry=1; entry<=numEntries; entry++) {
					Long entryId = entriesFromDb.getLong(entriesFromDb.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
					for (int i=oldNumRaces+1; i<=numRaces; i++) {
						mDbHelper.addResultRow(entryId, mRowId, i, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0);
					}
					entriesFromDb.moveToNext();
				}
			}
			entriesFromDb.close();
			mDbHelper.updateSeries(mRowId, series, numRaces, discardProfile, hcap);
		}
		return 0;
	}
	
	
	private void populateFields() {
		if (mRowId != null && mRowId != 0) {
			Cursor series = mDbHelper.fetchSeries(mRowId);
			mSeriesName.setText(series.getString(
					series.getColumnIndexOrThrow(SailscoreDbAdapter.KEY_SERIES)));
			mNumRaces.setText(series.getString(
					series.getColumnIndexOrThrow(SailscoreDbAdapter.KEY_NUMRACES)));
			mDiscardProfile.setText(series.getString(
					series.getColumnIndexOrThrow(SailscoreDbAdapter.KEY_DISCARD_PROFILE)));
			int hcap = series.getInt(series.getColumnIndex(SailscoreDbAdapter.KEY_HCAP));
			if (hcap == 0) {
				mFleetButton.setChecked(true);
				mHcapButton.setChecked(false);
			} else {
				mFleetButton.setChecked(false);
				mHcapButton.setChecked(true);
			}
			series.close();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mRowId != null) {
			outState.putLong(SailscoreDbAdapter.KEY_ROWID, mRowId);
		}
	}
	
}


