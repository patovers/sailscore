/*
 * This list activity is for creating and editing the details for competitor entries.
 */

package com.overs.sailscore;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class EntriesListActivity extends ListActivity{
    private SailscoreDbAdapter mDbHelper;
    private static final int ENTRY_CREATE = 0;
    private static final int ENTRY_EDIT = 0;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entries_list);
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

	private void fillData() {	
		Cursor entriesCursor = mDbHelper.fetchAllEntries();
		// Note that managed cursors are discouraged now but this one doesn't seem to cause any problems
		//startManagingCursor(entriesCursor);
		String[] from = new String[]{SailscoreDbAdapter.KEY_HELM, SailscoreDbAdapter.KEY_CREW, SailscoreDbAdapter.KEY_CLASS, SailscoreDbAdapter.KEY_SAILNO, SailscoreDbAdapter.KEY_CLUB};
		int[] to = new int[]{R.id.helm, R.id.crew, R.id.boatclass, R.id.sailno, R.id.club};
		AlternateRowCursorAdapter entries = new AlternateRowCursorAdapter(this, R.layout.entry_row, entriesCursor, from, to);
		setListAdapter(entries);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, EntryEditActivity.class);
        i.putExtra(SailscoreDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ENTRY_EDIT);
    }
	
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater mi = getMenuInflater();
    	mi.inflate(R.menu.list_menu_item_longpress, menu);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater mi = getMenuInflater();
    	mi.inflate(R.menu.entries_list_menu, menu);
    	return true;
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	mDbHelper.open();
    	fillData();
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.menu_delete:
    			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    			Long id = info.id;
    			String entry = id.toString();
    			mDbHelper.deleteResultsByEntry(entry); // clear all races first
    			mDbHelper.deleteEntry(info.id);        // followed by the entry
    			fillData();
    			return true;
    		}
    	return super.onContextItemSelected(item);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.menu_insert:
    		createEntry();
    		return true;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }
    
    private void createEntry() {
    	Intent i = new Intent(this, EntryEditActivity.class);
    	startActivityForResult(i, ENTRY_CREATE);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	mDbHelper.open();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mDbHelper.close();
    }
    
}
