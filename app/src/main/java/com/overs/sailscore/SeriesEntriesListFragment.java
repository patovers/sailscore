/*
 * This list activity is for viewing and working with an individual series.
 * The main list view shows the entries in a series.
 * When an entry is clicked it goes to the EntryResults list activity for entering and editing race
 * results for that entry.
 * This activity is really only an interim step towards editing the race results for a competitor.
 */

package com.overs.sailscore;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SeriesEntriesListFragment extends ListFragment{
    private SailscoreDbAdapter mDbHelper;
	protected Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private boolean seriesType;
    private static final int RESULTS_EDIT = 1;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    	Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.series_entry_list, container, false);
    }

	/** Called when the activity is first created. */
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new SailscoreDbAdapter(getActivity());
        mDbHelper.open();
        mRowId = getActivity().getIntent().getExtras().getLong(SailscoreDbAdapter.KEY_ROWID);
		fillData();
		setRetainInstance(true);
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
    	outState.putLong("SAVED_ROW", mRowId); // series ID
    	super.onSaveInstanceState(outState);
    }
 
    private void fillData() {
		Cursor entriesCursor = mDbHelper.fetchEntriesFromSeries(mRowId); // rowId in series table
		//Create arrays to specify the fields we want
		String[] from = new String[]{SailscoreDbAdapter.KEY_HELM, SailscoreDbAdapter.KEY_CREW, SailscoreDbAdapter.KEY_CLASS, SailscoreDbAdapter.KEY_SAILNO, SailscoreDbAdapter.KEY_CLUB};
		// and arrays of the fields we want to bind in the view
		int[] to = new int[]{R.id.helm, R.id.crew, R.id.boatclass, R.id.sailno, R.id.club};
		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter entries = new AlternateRowCursorAdapter(getActivity(), R.layout.entry_row, entriesCursor, from, to);
		setListAdapter(entries);
		// For some reason we can't close the cursor while it is bound to the view
		//entriesCursor.close();
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
		// Look for the additional container in the layout to display the edit pane alongside the competitor or race
        // The id value from the click is in fact the entry id from the entries table
        if (seriesType) { // handicap series
        	Intent i = new Intent(getActivity(), TimesEntryListActivity.class);
        	Long entryId = Long.valueOf(id);
        	i.putExtra(SailscoreDbAdapter.KEY_ENTRY, entryId); // the id of the entry in the series
        	i.putExtra(SailscoreDbAdapter.KEY_SERIES, mRowId); // id of the series
        	startActivityForResult(i, RESULTS_EDIT);
          } else { // fleet series
            Intent i = new Intent(getActivity(), ResultsEntryListActivity.class);
            Long entryId = Long.valueOf(id);
            i.putExtra(SailscoreDbAdapter.KEY_ENTRY, entryId); // the id of the entry in the series
            i.putExtra(SailscoreDbAdapter.KEY_SERIES, mRowId); // id of the series
            startActivityForResult(i, RESULTS_EDIT);
          }
    }
	
	@Override
	public void onResume() {
		super.onResume();
        mDbHelper.open();
		fillData();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mDbHelper.close();
	}
	
}
