package com.overs.sailscore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SeriesTabActivity extends Activity {
    private SailscoreDbAdapter mDbHelper;
	protected Long mRowId; // The rowId relates to the series and is carried through from the calling activity
	private TextView seriesName;
    private static final int SERIES_SCORE = 0;
    private static final int SERIES_EDIT = 1;
    private SailscoreHelpers mHelper;
    /** Called when the activity is first created. */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.series_tabs);
        
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab tabA = actionBar.newTab();
        tabA.setText(getString(R.string.enter_by_competitor));
        tabA.setTabListener(new TabListener<SeriesEntriesListFragment>(this, "Tag A", SeriesEntriesListFragment.class));
        actionBar.addTab(tabA);
        
        Tab tabB = actionBar.newTab();
        tabB.setText(getString(R.string.enter_by_race));
        tabB.setTabListener(new TabListener<SeriesRacesListFragment>(this, "Tag B", SeriesRacesListFragment.class));
        actionBar.addTab(tabB);
                
        if (savedInstanceState != null) {
            //mRowId = savedInstanceState.getLong("SAVED_ROW", mRowId);
            int savedIndex = savedInstanceState.getInt("SAVED_INDEX");
            actionBar.setSelectedNavigationItem(savedIndex);
        }
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        mHelper = new SailscoreHelpers();
        //actionBar.setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putInt("SAVED_INDEX", getActionBar().getSelectedNavigationIndex());
    	savedInstanceState.putLong("SAVED_ROW", mRowId); // series ID
    	super.onSaveInstanceState(savedInstanceState);
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
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		seriesName = (TextView) findViewById (R.id.series_name);
		seriesName.setText(seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES)));
		seriesCursor.close();
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
		setRowIdFromIntent();
		fillData();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu items for use in the action bar
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.series_entries_list_menu, menu);
    	return super.onCreateOptionsMenu(menu);
    } 

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Intent edit = new Intent(this, SeriesEditActivity.class);
    	Intent score = new Intent(this, SeriesScoreListActivity.class);
    	edit.putExtra(SailscoreDbAdapter.KEY_ROWID, mRowId);
    	score.putExtra(SailscoreDbAdapter.KEY_ROWID, mRowId);
    	String fileContents;
    	boolean raw;
    	switch(item.getItemId()) {
    	case R.id.menu_settings:
    		startActivityForResult(edit, SERIES_EDIT);
    		return true;
    	case R.id.menu_score:
    		startActivityForResult(score, SERIES_SCORE);
    		return true;
    	case R.id.menu_delete:
    		AlertDialog.Builder builder1 = new AlertDialog.Builder(SeriesTabActivity.this);
    		builder1.setMessage(R.string.confirm_delete_message)
    		.setTitle(R.string.confirm_delete_series)
    		.setCancelable(false)
    		.setPositiveButton(R.string.ok_delete_series, new DialogInterface.OnClickListener() {
    			@Override
				public void onClick(DialogInterface dialog, int id) {
    				mDbHelper.deleteSeries(mRowId);
    				mDbHelper.deleteResultsBySeries(mRowId);
    				dialog.cancel();
    				finish();
    			}
    		})
    		.setNegativeButton(R.string.cancel_delete_series, new DialogInterface.OnClickListener() {
    			@Override
				public void onClick(DialogInterface dialog, int id) {
    				dialog.cancel();
    			}
    		}

    				);
    		builder1.create().show();
    		break;
    	case R.id.menu_export_raw:
    		raw = true;
    		//startActivityForResult(export, SERIES_EXPORT);
    		fileContents = makeCsv(mRowId, raw);
    		sendFile(fileContents, raw);
    		return true;
    	case R.id.menu_export_scored:
    		raw = false;
    		//startActivityForResult(export, SERIES_EXPORT);
    		fileContents = makeCsv(mRowId, raw);
    		sendFile(fileContents, raw);
    		return true;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater mi = getMenuInflater();
    	mi.inflate(R.menu.list_menu_item_longpress, menu);
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
		ActionBar actionBar = getActionBar();
		int index = actionBar.getSelectedNavigationIndex();
		actionBar.setSelectedNavigationItem(index);
    }
    
   

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mDbHelper.close();
    }

	/* Separate method to build the CSV file to be exported.
	 * This makes the assumption that the series is already scored. If it isn't then
	 * the points, ranks, discarded etc just won't be available but the method should still
	 * work through the data and output something sensible.
	 * This method works entirely in text using what is stored in the DB as text apart from
	 * resultCode which is stored as an int anyway.
	 */

    // raw is used to output only the source data or the scored points as well
	private String makeCsv (Long series, boolean raw) {
		String resultString = "";
		// Columns depend on whether we are exporting raw data or everything
		if (!raw) {
			resultString =   "Rank, ";
		}
		resultString =   resultString + "Helm, Crew, Club, Class, SailNo, Rating, ";
		Cursor entriesCursor = mDbHelper.fetchEntriesFromSeries(mRowId);
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);
		int numRaces = seriesCursor.getInt(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_NUMRACES));
		int numEntries = entriesCursor.getCount();
		int seriesType = seriesCursor.getInt(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HCAP));
		for (int race = 1; race <= numRaces; race++) {
			resultString = resultString + "R" + Integer.toString(race) + ", ";
		}
		if (!raw) {
			resultString = resultString + "Total, Nett";
		}
		resultString = resultString + "\n";
		// Iterate through the competitors, one per line, to build up the rows in the csv file
        for (int line = 0; line < numEntries; line++) {
        	// Get the id of the competitor so we can find their results
			Long mEntry = Long.parseLong(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID)));
        	Cursor resultsCursor = mDbHelper.getResults(mEntry, mRowId);
        	resultsCursor.moveToFirst();
        	// Note this relies on the resultsCursor having the results in race order - this should be the case
        	String position = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_POSITION));
        	String helm = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM));
        	String crew = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW));
        	String club = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLUB));
        	String boatclass = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS));
        	String sailno = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO));
        	String py = entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_PY));
        	int iPy = Integer.parseInt(py);
        	// If the series has been scored then non-zero values will be in the database for total and nett points
        	// which is the same in all rows in the cursor
        	float total = resultsCursor.getFloat(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL));
        	float nett = resultsCursor.getFloat(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_NETT));
			String formattedTotal = Float.toString(total);
			String.format(formattedTotal, .1f);
			String formattedNett = Float.toString(nett);
			String.format(formattedNett, .1f);
        	// Whether or not the series has been scored determines what it output in the file.
        	// If it's been scored then the full results, i.e. calculated points are output.
        	// If it's not been scored then just the raw data as entered is output
        	boolean scored = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_SCORED)).equals("1") ? true : false;
        	if (!raw) {
            	resultString = resultString + position + ", ";
        	}
           	resultString = resultString + helm + ", " + crew + ", " + club + ", " + boatclass + ", " + sailno + ", " + py;
            String result;
        	float points;
        	// Now iterate over the race results and add them to the line
        	for (int race = 0; race < numRaces; race++) {
        		// results is a string used to carry either the entered finish position for a fleet series
        		// or a representation of the elapsed time in a handicap series
        		result = "";
        		// Series type determines if we export corrected times or finish places
        		if (seriesType == 1) { // for handicap series we need time information
					String sStartTime = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_START_TIME));
					String sFinishTime = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_FINISH_TIME));
					int sSecs = Integer.parseInt(sStartTime); // The database only stores times in seconds
					int fSecs = Integer.parseInt(sFinishTime);
					// Check for a valid elapsed time and force it to 0 if not
        			if (sSecs >= fSecs) {
        				sSecs = 0;
        				fSecs = 0;
        			}
        			int elapsedSecs = (fSecs - sSecs); // just get the raw elapsed for now
        			int correctedSecs;
					// If not all laps were completed then we need to pro-rata the elapsed time
					int sailedLaps = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_LAPS));
					int totalLaps = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL_LAPS));
					if (sailedLaps !=0 && totalLaps !=0) { // only calculate if pro-rata info is present
						if (sailedLaps != totalLaps) {
							elapsedSecs = elapsedSecs * totalLaps / sailedLaps;
						}
					}
					correctedSecs = elapsedSecs *1000/iPy;
					// Now work out what has to be exported
					// Corrected times are used if scored data is to be output
					// If raw data is exported we'll expect Sailwave to use the PY to calculate corrected times
					// instead of doing it here.
					int secs, mins;
					if (raw) {
						secs = elapsedSecs % 60;
						mins = elapsedSecs / 60;
					} else {
						secs = correctedSecs % 60;
						mins = correctedSecs / 60;
					}
					String formattedSecs = String.format(Locale.getDefault(), "%02d", secs);
	    			result = Integer.toString(mins) + ":" + formattedSecs;
        		} else { // we need the result as entered
            		result = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RESULT));
        		}
        		// At this point we have a result String that has all the elements of the result in it 
        		// except for discard brackets and and result codes
        		points = resultsCursor.getFloat(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_POINTS));
    			String formattedPoints = Float.toString(points);
    			String.format(formattedPoints, .1f);
        		int resultCode = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
        		String rdgPoints = resultsCursor.getString(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
        		// If the series has been scored then there will be a value for discarded or 0 if not
        		boolean discarded = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_DISCARDED)) == 1 ? true : false;
        		resultString = resultString + ", ";
        		// Value is in brackets if discarded.
        		// In theory this can only happen if the series has been scored
        		// Don't mark discards for raw export
        		if (discarded && !raw) {
        			resultString = resultString + "(";
        		}
        		switch (resultCode) {
        		case 0:
        			// Only output the scored result if requested
        			if (scored && !raw) {
        				resultString = resultString + formattedPoints;
        			} else {
        				resultString = resultString + result;
        			}
        			break;
        		// The starters+1 codes only output the code text
        		case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
        			resultString = resultString + mHelper.convertResultCode(resultCode);
        			break;
        		// The codes with defined calculated scores
        		case 10: case 12: case 13: case 14: case 15:
        			if (raw) {
            			resultString = resultString + mHelper.convertResultCode(resultCode);
        			} else if (scored) {
        				resultString = resultString + " " + formattedPoints;
        			} else {
        				resultString = resultString + " " + result;
        			}
        			break;
        		// RDG with specified actual score
        		case 11: 
        			if (raw || !scored) {
        				// Note that this doesn't actually import directly into Sailwave as it stands
        				// as there is no way to import something that has a finish position AND a code AND a redress position.
        				// For now this is the way it displays in Sailwave if you were to enter a result with redress but
        				// the user will need to fix it in Sailwave after importing since it will just show up as a result code
        				// with all the imported text as the code. 
            			resultString = resultString + result + " " + mHelper.convertResultCode(resultCode) + "-" + rdgPoints;
        			} else {
        				// For a scored result we can output it in any convenient format
        				// so we list the entered result, redress code and position and the final scored points.
            			resultString = resultString + result + " " + mHelper.convertResultCode(resultCode) + "-" + rdgPoints + " " + formattedPoints;
        			}
        			break;
        		}
        		if (discarded && !raw) {
        			resultString = resultString + ")";
        		}
        		resultsCursor.moveToNext();
        	}
        	if (!raw) {
            	resultString = resultString + ", " + formattedTotal + ", " + formattedNett + "\n";
        	} else {
        		resultString = resultString + "\n";
        	}
        	entriesCursor.moveToNext();
        	resultsCursor.close();
        }
        entriesCursor.close();
        seriesCursor.close();
		return resultString;
	}

    private void sendFile (String fileContents, boolean raw) {   
		File file   = null;
		File root   = Environment.getExternalStorageDirectory();
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);
		String seriesName = seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES));
		seriesCursor.close();
		if (root.canWrite()){
		    File dir    =   new File (root.getAbsolutePath() + "/" + R.string.app_name);
		     dir.mkdirs();
		     if (raw) {
			     file   =   new File(dir, seriesName + "_data.csv");
		     } else {
			     file   =   new File(dir, seriesName + "_scored.csv");
			 }
		     FileOutputStream out   =   null;
		    try {
		        out = new FileOutputStream(file);
		    } catch (FileNotFoundException e) {
		        e.printStackTrace();
		    }
		    try {
		        out.write(fileContents.getBytes());
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    try {
		        out.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}

		Uri u1  =   null;
		u1  =   Uri.fromFile(file);

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
		sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));
		sendIntent.putExtra(Intent.EXTRA_STREAM, u1);
		sendIntent.setType("text/html");
		startActivity(sendIntent);
	}


 
// Activity proper ends here

// Everything from here on is to do with the tab listener

 public static class TabListener<T extends ListFragment> 
     implements ActionBar.TabListener{
     
        private final Activity myActivity;
        private final String myTag;
        private final Class<T> myClass;

        public TabListener(Activity activity, String tag, Class<T> cls) {
            myActivity = activity;
            myTag = tag;
            myClass = cls;
        }

  @Override
  public void onTabSelected(Tab tab, FragmentTransaction ft) {

   Fragment myFragment = myActivity.getFragmentManager().findFragmentByTag(myTag);
   
   // Check if the fragment is already initialized
         if (myFragment == null) {
             // If not, instantiate and add it to the activity
             myFragment = Fragment.instantiate(myActivity, myClass.getName());
             ft.add(android.R.id.content, myFragment, myTag);
         } else {
             // If it exists, simply attach it in order to show it
             ft.attach(myFragment);
         }
   
  }

  @Override
  public void onTabUnselected(Tab tab, FragmentTransaction ft) {
   
   Fragment myFragment = myActivity.getFragmentManager().findFragmentByTag(myTag);
   
   if (myFragment != null) {
             // Detach the fragment, because another one is being attached
             ft.detach(myFragment);
         }
   
  }

  @Override
  public void onTabReselected(Tab tab, FragmentTransaction ft) {
   // TODO Auto-generated method stub
   
  }
 
 }
  
}