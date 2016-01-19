/*
 * This list activity is for viewing and working with an individual series.
 * The main list view shows the entries in a series.
 * When an entry is clicked it goes to the EntryResults list activity for entering and editing race
 * results for that entry.
 * This activity is really only an interim step towards editing the race results for a competitor.
 */

package com.overs.sailscore;
import android.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SeriesRacesListFragment extends ListFragment{
    private SailscoreDbAdapter mDbHelper;
	private Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private static final int RESULTS_EDIT = 1;
    private boolean seriesType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
     return inflater.inflate(R.layout.series_entry_list, container, false);
    }

	/** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new SailscoreDbAdapter(getActivity());
        mDbHelper.open();
        mRowId = getActivity().getIntent().getExtras().getLong(SailscoreDbAdapter.KEY_ROWID);
		fillData();
		setRetainInstance(true);
    }

    /* All this does is to create a list of string numbers from 1 to numRaces and
     * make a list view so that each one, which is a race, can be clicked to 
     * enter results for that race.
     */
    @Override
	public void onSaveInstanceState(Bundle outState) {
    	outState.putLong("SAVED_ROW", mRowId); // series ID
    	super.onSaveInstanceState(outState);
    }
 
	private void fillData() {
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		int numRaces = seriesCursor.getInt(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_NUMRACES));
		seriesCursor.close();
		final String[] raceNumbers = new String[numRaces];
	    for (int i = 0; i < raceNumbers.length; ++i) {
	    	raceNumbers[i] = getString(R.string.race) + " " + Integer.toString(i+1);
	      }
 	    final AlternateRowArrayAdapter adapter = new AlternateRowArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, raceNumbers);
	        setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {	
        super.onListItemClick(l, v, position, id);
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		int iSeriesType = seriesCursor.getInt(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HCAP));
		if (iSeriesType == 1) {
			seriesType = true; // handicap - enter times
		} else {
			seriesType = false; // fleet - enter positions
		}
		seriesCursor.close();
        // The id value from the click is in fact the entry id from the entries table
        if (seriesType) { // handicap series
        	Intent i = new Intent(getActivity(), RaceTimesEntryListActivity.class);
        	Long raceId = Long.valueOf(id+1); // Competitor IDs start at 1
        	i.putExtra(SailscoreDbAdapter.KEY_RACE, raceId); // the id of the entry in the series
        	i.putExtra(SailscoreDbAdapter.KEY_SERIES, mRowId); // id of the series
        	startActivityForResult(i, RESULTS_EDIT);
        } else { // fleet series
        	Intent i = new Intent(getActivity(), RaceResultsEntryListActivity.class);
        	Long raceId = Long.valueOf(id+1); // Competitor IDs start at 1
        	i.putExtra(SailscoreDbAdapter.KEY_RACE, raceId); // the id of the entry in the series
        	i.putExtra(SailscoreDbAdapter.KEY_SERIES, mRowId); // id of the series
        	startActivityForResult(i, RESULTS_EDIT);
        }
    }
	
	@Override
	public void onResume() {
		super.onResume();
		fillData();
	}
	
}
