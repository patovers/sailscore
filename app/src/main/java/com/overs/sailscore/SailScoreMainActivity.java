/*
 * This is the landing page when the app is started. All other main activities are accessible via
 * buttons in this activity.
 */

package com.overs.sailscore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SailScoreMainActivity extends Activity {
	private Button mAllEntriesButton;
	private Button mNewEntryButton;
	private Button mAllSeriesButton;
	private Button mNewSeriesButton;
	private Button mHelpButton;
    private static final int ENTRY_CREATE = 0;
    private static final int SERIES_CREATE = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mAllEntriesButton = (Button) findViewById(R.id.all_entries_button);
        mNewEntryButton = (Button) findViewById(R.id.new_entry_button);
        mAllSeriesButton = (Button) findViewById(R.id.all_series_button);
        mNewSeriesButton = (Button) findViewById(R.id.new_series_button);
        mHelpButton = (Button) findViewById(R.id.instructions_button);
		// For now all we need to do is start the activity. There is nothing else to do.
		registerAllButtonListeners();
    }
      
/*
        public void onClick(View v) {
	        Intent i = new Intent(this, EntriesListActivity.class);
	        startActivity(i);
		}
*/
    
	public void registerAllButtonListeners() {
        final Intent showEntries = new Intent(this, EntriesListActivity.class);
        final Intent newEntry = new Intent(this, EntryEditActivity.class);
        final Intent newSeries = new Intent(this, SeriesEditActivity.class);
        final Intent showSeries = new Intent(this, SeriesListActivity.class);
        final Intent help = new Intent(this, InstructionsActivity.class);
        /* Other intents to complete later:
         * showSeries opens the series list activity
         * newSeries goes directly to the create series activity
         */
		mAllEntriesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        startActivity(showEntries);
			}
		});
		mNewEntryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(newEntry, ENTRY_CREATE);
			}
		});	
		mAllSeriesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(showSeries);
		}
		});
		mNewSeriesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(newSeries, SERIES_CREATE);
		}
		});
		
		mHelpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(help);
		}
		});
		

		
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    }

	
}