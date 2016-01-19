package com.overs.sailscore;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SeriesListActivity extends ListActivity{
    private SailscoreDbAdapter mDbHelper;
    private static final int SERIES_CREATE = 0;
    private static final int SERIES_EDIT = 1;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.series_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        // This activity needs access to two databases:
        // The first stores the data for a single series
        // The second keeps a record of each database created for a series
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        fillData();
    }

	private void fillData() {
		Cursor seriesCursor = mDbHelper.fetchAllSeries();
		seriesCursor.moveToFirst();
		//startManagingCursor(seriesCursor);
		//Create arrays to specify the fields we want (only the helm and crew for now)
		String[] from = new String[]{SailscoreDbAdapter.KEY_SERIES, SailscoreDbAdapter.KEY_NUMRACES};
		// and arrays of the fields we want to bind in the view
		int[] to = new int[]{R.id.series_name, R.id.num_races};
		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter series = new AlternateRowCursorAdapter(this, R.layout.series_row, seriesCursor, from, to);
		setListAdapter(series);
		//seriesCursor.close();
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, SeriesTabActivity.class);
        i.putExtra(SailscoreDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, SERIES_EDIT);
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater mi = getMenuInflater();
    	mi.inflate(R.menu.series_list_menu, menu);
    	return true;
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	mDbHelper.open();
    	fillData();
    }
    

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.menu_insert:
    		createSeries();
    		return true;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }
    
    private void createSeries() {
    	Intent i = new Intent(this, SeriesEditActivity.class);
    	startActivityForResult(i, SERIES_CREATE);
    }

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		fillData();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mDbHelper.close();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
