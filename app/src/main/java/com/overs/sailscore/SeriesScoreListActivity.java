/*
 * This list activity is for displaying results from a series.
 * On entry it is given the rowId of the series in order to look up the entries and results.
 * All score calculations are performed in this class.
 */

package com.overs.sailscore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;


public class SeriesScoreListActivity extends ListActivity{
	private SailscoreDbAdapter mDbHelper;
	private SailscoreHelpers mHelper;
	// Array of objects for all competitors's details and result
	private ArrayList<ScoresObj> combinedList;
	private Long mRowId; // the series identification
	private SeriesScoreListAdapter mAdapter;
	private TextView seriesName;
	float avgAllRaces = 0;
	float netPoints = 0;
	//float grossPoints = 0;
	private static Font catFont = new Font(Font.FontFamily.HELVETICA, 18,
		      Font.BOLD);
	
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.series_score_list);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        mDbHelper = new SailscoreDbAdapter(this);
        mDbHelper.open();
        mHelper = new SailscoreHelpers();
        combinedList = new ArrayList<ScoresObj>();
        mAdapter = new SeriesScoreListAdapter (this, combinedList);
		mRowId = savedInstanceState != null
				? savedInstanceState.getLong(SailscoreDbAdapter.KEY_ROWID)
				: null;
    }

	   // Local class to allow sorting of results for an individual race
		private class CompetitorRace {
			public CompetitorRace(int rowId, Long entryId, int result, int code, int rdgPoints, float points) {
				this.entryId = entryId;
				this.result = result;
				this.code = code;
				this.rdgPoints = rdgPoints;
				this.points = points;
			}
			Long entryId;
			int result;
			int code;
			int rdgPoints;
			float points;
		}
		
		private class CompetitorTimes {
			public CompetitorTimes(int rowId, Long entryId, int startTime, int finishTime, int laps, int totalLaps, int code, int result, int rdgPoints, float proRataTime, float points) {
				this.entryId = entryId;
				this.code = code;
				this.rdgPoints = rdgPoints;
				this.proRataTime = proRataTime;
				this.points = points;
			}
			Long entryId;
			int code;
			int rdgPoints;
			float proRataTime;
			float points;
		}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.series_score_menu, menu);
		return true;
	}
	
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Bitmap bmp;
    	switch(item.getItemId()) {
    	case R.id.score_menu_image:
    		bmp = getWholeListViewItemsToBitmap();
    		saveBitmap(bmp);
    		return true;
    	case R.id.score_menu_pdf:
    		bmp = getWholeListViewItemsToBitmap();
    		savePDF(bmp);
    		return true;
    	}
    	return super.onMenuItemSelected(featureId, item);
    }

	
	private void fillData() {
		/* This method is the wrapper for all the scoring functions and displaying of results:
		 * 1. For each competitor in the series work out which results to discard
		 * 2. For each competitor in the series work out the nett and gross points
		 * 3. For all competitors split any ties (how to represent?)
		 * 4. For all competitors rank in ascending order of points
		 * 5. Bind all the data to the view
		 */
		
		// While the calculations are taking place we show that it's thinking
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(getString(R.string.progress));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.show();
		
		ListView list = getListView();
		// get all the entries in the series to apply scoring process to
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		seriesName = (TextView) findViewById (R.id.series_name);
		seriesName.setText(seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES)));
		String discardProfile = seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_DISCARD_PROFILE));
		int numRaces = Integer.parseInt(seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_NUMRACES)));
		int numDiscards = parseDiscardProfile(numRaces, discardProfile);
		boolean seriesType = (seriesCursor.getInt(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HCAP)) == 1) ? true : false;
		Cursor entriesCursor = mDbHelper.fetchEntriesFromSeries(mRowId); 
		int numEntries = entriesCursor.getCount();
		
		// If the series is a handicap then we need to work out the finish positions for each race from the times before working
		// on the finishing order for the whole series.
		if (seriesType) {
			convertTimesToPoints(numEntries, numRaces);
		} else
			// If it is a fleet series then we just convert places to points, dividing any tied finishers
			convertPlacesToPoints(numEntries, numRaces);
		
		if (entriesCursor != null && entriesCursor.moveToFirst()) {
			// Each pass round this loop calculates the numerical scores for each race for that competitor
			for (int entry = 1; entry <= numEntries; entry++) {
				// Before we can get the results for the Nth entry in the list we need its actual ID from the cursor
				Long mEntry = Long.parseLong(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID)));
				// Create an instance of the list to put results into
		        ArrayList<RaceResultPair> resultList = new ArrayList<RaceResultPair>();
		        resultList = getPoints(mEntry, numEntries);
		        //ArrayList<RaceTimePair> timesList = new ArrayList<RaceTimePair>();
				// Competitor information goes into an instance of the ScoresObj class
				ScoresObj combinedObj = new ScoresObj();
				
				// Get all the basic info for the competitor
				combinedObj.setId(mEntry);
				combinedObj.setHelm(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_HELM)));
				combinedObj.setCrew(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CREW)));
				combinedObj.setSail(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SAILNO)));
				combinedObj.setBoatClass(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLASS)));
				combinedObj.setClub(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_CLUB)));
			  
				// Add in the RDGa values
				float grossPoints = 0;
				int numResults = resultList.size();
		        for (int resultNum = 0; resultNum < numResults; resultNum++) {
		        	// RDGa is average of all races
		        	if (resultList.get(resultNum).getResultCode() == 12) {
		        		resultList.get(resultNum).setPoints(avgAllRaces);
		        	}
		        	grossPoints += resultList.get(resultNum).getPoints();
		        }

				// Calculate net points by discarding scores
		        float netPoints = removeDiscards (grossPoints, numDiscards, resultList); // Deduct the correct number of discarded scores
		        
		        // NetPts object in scoresObj is an Integer (so we can use compare methods later) so need to convert
		        //Integer netPointsInt = (int) netPoints;
		        //Integer grossPointsInt = (int) grossPoints;
		        combinedObj.setNettPts(netPoints);
		        combinedObj.setGrossPts(grossPoints);
		        // Check if this assignment actually works
		        combinedObj.setRaceResults(resultList);
				combinedList.add(combinedObj);
				// And do the next one
				entriesCursor.moveToNext();
            }
			
			// Now that we have an array with all the entry details with results we need to put them in order of nett points
			// With the discarded ones at the end so that they can be ignored for this purpose.
			// Trying this way to do it from StackOverflow - using custom comparable
			// This might be better moved into the class definition for ScoresObj
			//Collections.sort(combinedList, new ScoresObj());
			Collections.sort(combinedList, new Comparator<ScoresObj> () {
				@Override
				public int compare(ScoresObj arg0, ScoresObj arg1) {
					// Declare local holder for the race results part of each ScoresObj being passed in for comparison
					ArrayList<RaceResultPair> raceResultsLhs = new ArrayList<RaceResultPair>();
					ArrayList<RaceResultPair> raceResultsRhs = new ArrayList<RaceResultPair>();
					raceResultsLhs = arg0.getRaceResults();
					raceResultsRhs = arg1.getRaceResults();
					float nettPointsLhs = arg0.getNettPts();
					float nettPointsRhs = arg1.getNettPts();
					int numRaces = raceResultsLhs.size();
					// First step split scores based on nett points
					if (nettPointsLhs < nettPointsRhs) {
						return -1;
					} else if (nettPointsRhs < nettPointsLhs) {
						return 1;
					} else { 
						// nettPoints for these two are equal so have to  split the tie
						// Next step - arrange scores in increasing order for both lists and work through them to find differences
						Collections.sort(raceResultsLhs); // Sort in order of best to worst with discards at the end
						Collections.sort(raceResultsRhs);
						int result; // declare here so we can use it outside the scope of the for loop
						for (result = 0; result<numRaces; result++) {
							// If either are discarded then the tie is broken and we stop there
							if (   (raceResultsLhs.get(result).isDiscarded() == 1)
								|| (raceResultsRhs.get(result).isDiscarded() == 1) ) {
								break;
							}
							// Otherwise we carry on until there is a difference
							if (raceResultsLhs.get(result).getPoints() == raceResultsRhs.get(result).getPoints()) {
								continue;
							} else {
								break; // capture value of result to indicate where the difference is
							}
						}
						if ((result + 1) < numRaces) {
							// We've found a difference before the end of the result list so return the one with the lowest 
							// number at this result.
							// Note add 1 because the loop counter stops at one less than numRaces
							if (raceResultsLhs.get(result).getPoints() < raceResultsRhs.get(result).getPoints()) {
								return -1;
							} else {
								return 1;
							}
						} else {
							// Next step:
							// We have reached the end and not managed to split the tie so we need to look at the results
							// from the end backwards, starting with the last race and including discards as well
							// First though we need to re-sort the results in order of race rather than order of result
							// To do this we need to create a different comparator 
							Collections.sort(raceResultsLhs, new Comparator<RaceResultPair>() {
								@Override
								public int compare(RaceResultPair p1, RaceResultPair p2) {
									return (int) (p1.getRace() - p2.getRace());
								}
							});
							Collections.sort(raceResultsRhs, new Comparator<RaceResultPair>() {
								@Override
								public int compare(RaceResultPair p1, RaceResultPair p2) {
									return (int) (p1.getRace() - p2.getRace());
								}
							});
							for (result = numRaces-1; result>=0; result--) {
								if (raceResultsLhs.get(result).getPoints() == raceResultsRhs.get(result).getPoints()) {
									continue;
								} else {
									break; // capture result where there is a difference
								}
							}
							// Now we found a break in the tie or reached the beginning again
							if (result != -1) {
								return (raceResultsLhs.get(result).getPoints() < raceResultsRhs.get(result).getPoints() ? -1 : 1);
							} else {
								// If result is 0 then we have somehow made it all the way back to the beginning and still not
								// broken the tie.
								// This could only happen if more than one competitor had no results at all.
								return 0;
							}
						}
					}
				}
			});
			
	    }
		
		// Call the method to insert the position values now that the list has been sorted into overall finishing position
		insertRanks();

		// But before displaying the results we need to update the data in the results table for the scored series
		// This loop iterates over the rows in order of competitors in the series, not order of position
		entriesCursor.moveToFirst(); // Reuse the entries cursor that we still have
		for (int entry=0; entry < combinedList.size(); entry++) {
			// get the actual entry ID rather than the item in the list
			//Long mEntry = Long.parseLong(entriesCursor.getString(entriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID)));
			// Need to find the position of the competitor with id=entry
			Long id = combinedList.get(entry).getId();
			int rank = combinedList.get(entry).getPosition();
			// Finally we need to create a string of all the race results for display in the row of the listView
			// This can only be done here because up to now we didn't have all the information and races were in the wrong order.
			// Pull out the race results to work on:
			ArrayList<RaceResultPair> resultList = new ArrayList<RaceResultPair>(); 
	        resultList = combinedList.get(rank-1).getRaceResults();
	        // Sort the individual races into order of race for display
	        // Note that they would only have ended up in the right order if the sorting had gone all the way to the final stage
	        // to split a tie. Otherwise they would normally be in order of best finishing positions within each competitor.
	        Collections.sort(resultList, new Comparator<RaceResultPair>() {
				@Override
				public int compare(RaceResultPair p1, RaceResultPair p2) {
					return (int) (p1.getRace() - p2.getRace());
				}
			});
	        // Put the ordered list of results for this competitor back into combinedList
	        combinedList.get(entry).setRaceResults(resultList);
	        // Before displaying what we have, we need to update a few columns in the database
		   	Cursor resultsCursor = mDbHelper.getResults(id, mRowId);
		   	// This loop goes through all the races for each competitor so the cursor has to have returned results in race order
			for (int race=0; race < numRaces; race++) {
				// These won't just fit into the combinedList format because combinedList is sorted into a new
				// order as a result of the scoring process
				int startTime = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_START_TIME));
				int finishTime = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_FINISH_TIME));
				int laps = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_LAPS));
				int totalLaps = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL_LAPS));
				int rdgPoints = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
				// This is a bodge for  now. Should really fix this properly
				if (combinedList.get(entry).getRaceResults().get(race).getPoints() == 200000) {
					combinedList.get(entry).getRaceResults().get(race).setResultCode(1);
				}
				mDbHelper.updateResult(
					id, // competitor id
					mRowId, // series
					combinedList.get(entry).getRaceResults().get(race).getRace(),       
					combinedList.get(entry).getRaceResults().get(race).getResult(),
					startTime,
					finishTime,
					laps,
					totalLaps,
					combinedList.get(entry).getRaceResults().get(race).getResultCode(),
					rdgPoints, //TODO this is saving into the wrong race
					//combinedList.get(entry).getRaceResults().get(race).getRdgPoints(),
					combinedList.get(entry).getRaceResults().get(race).getPoints(), 
					1, // scored
					combinedList.get(entry).getRaceResults().get(race).isDiscarded(),
					rank,
					combinedList.get(entry).getGrossPts(),
					combinedList.get(entry).getNettPts());
				resultsCursor.moveToNext();
			}
			resultsCursor.close();

	        
	        
	        // Now we can build the string representing all the individual races for each entry.
	        String sResults = new String();
	        sResults = buildResultsString(seriesType, resultList);
			// And put the whole thing into the array
	        combinedList.get(rank-1).setsRaceResults(sResults);
			entriesCursor.moveToNext();
		}
		
		entriesCursor.close();
		seriesCursor.close();
		// Final part is to bind the ranked, tie split, race ordered data to the view using the custom adapter
		dialog.cancel();
        list.setAdapter(mAdapter);
			
    }
	
/* 
 * Method to take each individual race and rank the competitors in it to give
 * a points value for their result. The database is then updated with the points values
 * which can then be used to rank the competitors in the series.
 * This is the first stage of scoring and does not make any adjustments for redress
 * because races are treated individually here. 
 * Takes numEntries as an input to allow correct points for DNC, DNS etc.
 * Takes numRaces to allow iteration over all races.
 */
	private void convertPlacesToPoints(int numEntries, int numRaces) {
		// Iterate to build a list of the entries in each race
		int rowId; Long entryId; int code = 0; Float points = (float) 0; int rdgPoints = 0; int result = 0;
		for (int raceNumber = 1; raceNumber <= numRaces; raceNumber++) {
			// Get the results for race N. mRowId is the ID of the series.
			Cursor raceCursor = mDbHelper.fetchRaceByNumber(raceNumber, mRowId);
			// Declare an array to hold all competitors in this race
			ArrayList<CompetitorRace> wholeRace = new ArrayList<CompetitorRace>();
			
			// Start by populating the array with the finish positions for all competitors in the race
			for (int entry = 1; entry<=numEntries; entry++) {
				rowId = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
				entryId = raceCursor.getLong(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_ENTRY));
				code = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				result = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RESULT));
				rdgPoints = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));

				// First step is to put in results for the starters plus one group
				switch (code) {
				case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
					result = numEntries + 1;
					break;
				case 10: // Duty needs special treatment later
					result = numEntries + 1;
					break;
				}
				
				// Then put in a fix to ensure result is non-zero, though it shouldn't be of course
				if (result == 0) {
					result = numEntries + 1;
					code = 1; // Force it to DNC
				}
				
				// Now put all the info into an instance of the CompetitorRace class
				CompetitorRace competitorRace = new CompetitorRace (rowId, entryId, result, code, rdgPoints, points);
				wholeRace.add(competitorRace);
				raceCursor.moveToNext();
			}
			raceCursor.close();
			// At this point we have an array for each race containing the result data for each competitor
			

			// Sort the competitors into order of finish position (not including any changes for redress).
			// Any competitor given redress has the points value overridden later (by protest committee)
			// but mustn't affect other boat's positions here.
			// This comparison is complicated by the fact that some boats have scoring penalties
			// and mustn't be shared with others that don't, even if the actual scores are the same
			Collections.sort(wholeRace, new Comparator<CompetitorRace>() {
				@Override
				public int compare(CompetitorRace lhs, CompetitorRace rhs) {
					if (lhs.result == rhs.result) {
						if ((lhs.code == 14 || lhs.code == 15) && (rhs.code == 14 || rhs.code == 15)) {
							return 0;
						} else if (lhs.code == 14 || lhs.code == 15) {
							return 1;
						} else if (rhs.code == 14 || rhs.code == 15) {
							return -1;
						} else {
							return 0;
						}
					} else {
						return (lhs.result - rhs.result); // sort by actual finish position
					}
				}
			});
			
			// With the results in the right order we now need to apportion any tied points
			// row 0 corresponds to 1st place, 1 to 2nd place, etc.
			// Iterate over the competitors until the last but 1 row which is dealt with separately
			boolean match = false; // used to spot a tie with a further tie in the next row
			int accPoints = 0;
			int location = 0;
			Float avgPoints = (float) 0;
			Float interim = (float) 0;
			for (int row = 0; row<numEntries-1; row++) {
				// Look for a tie with the next competitor
				// Note that if the next competitor has a scoring penalty the match is ignored
				// even if the results are the same
				if (wholeRace.get(row).result == wholeRace.get(row+1).result
						&& wholeRace.get(row+1).code != 14 && wholeRace.get(row+1).code != 15) {
					if (match) {
						accPoints += row + 1; // total up the points
					} else {
						// start off a possible group of matches
						// by setting accumulated points to the first position with a tie
						accPoints = row + 1;
						match = true;
						location = row;
					}
				} else {
					if (match) { // we have accumulated matches that now need to be processed
						accPoints += row+1; // add on the one we are at now since that matches the previous one
						match = false; // only need to reset if it was true
						interim = (float) row;
						interim -= location;
						interim ++;
						avgPoints = accPoints/interim;
						// what we need is this (average of the positions that were tied for):
						//avgPoints = (float) (accPoints / (row - location + 1));
						accPoints = 0; // ready for the next group of matches
						for (int updateRow = location; updateRow <= row; updateRow++) {
							if (wholeRace.get(updateRow).result <= numEntries) { // finds a legal finish position
								wholeRace.get(updateRow).points = avgPoints;
							} else { // it's a starters + 1 code
								wholeRace.get(updateRow).points = wholeRace.get(updateRow).result;
							}
						}
					} else { // Or we just don't have a match so we put in the points
						if (wholeRace.get(row).result <= numEntries
								&& wholeRace.get(row).code != 14 && wholeRace.get(row).code != 15) { // finds a legal finish position
							wholeRace.get(row).points = (float) row + 1;
						} else { // it's a starters + 1 code
							wholeRace.get(row).points = wholeRace.get(row).result;
						}
					}
				}
			}
			// Finally we need to account for the last row
			if (match) { // there was a match between the last row processed above and the one we are processing now
				// interim holds the number of tied competitors in the group of matched scores
				interim = (float) numEntries; // add on the one we have here
				interim -= location;
				// no need to adjust by 1 like in the main loop above
				accPoints += numEntries; // should be one more than the loop count from above
				avgPoints = accPoints/interim;
				for (int updateRow = location; updateRow <= numEntries -1; updateRow++) {
					if (wholeRace.get(updateRow).result <= numEntries) {
						wholeRace.get(updateRow).points = avgPoints;
					} else {
						wholeRace.get(updateRow).points = wholeRace.get(updateRow).result;
					}
				}
			} else {
					wholeRace.get(numEntries-1).points = numEntries; 
			}
			
			// Now we need to adjust any results that have scoring penalties or RDG
			for (int row = 0; row<numEntries; row++) {
				code = wholeRace.get(row).code;
				switch (code) {
				case 14: case 15:
					wholeRace.get(row).points = wholeRace.get(row).result + numEntries * (float) 0.2;
					break;
				case 11:
					wholeRace.get(row).points = wholeRace.get(row).rdgPoints;
					break;
				}
				// round to one decimal place
				points = wholeRace.get(row).points * 10;
				points = (float) Math.round(points)/10;
				wholeRace.get(row).points = points;
			}

			
			// After processing all results the database needs to be updated
			for (int row = 0; row < numEntries; row++) {
				mDbHelper.updatePoints(	wholeRace.get(row).entryId, // entryId
										mRowId, 					// series
										raceNumber, 				// raceNumber
										wholeRace.get(row).points);	// points
			}
		}
	}

	private void convertTimesToPoints(int numEntries, int numRaces) {
		// Iterate to build a list of the entries in each race
		int rowId; Long entryId; int startTime = 0; int finishTime = 0; int laps = 0; int result;
		int elapsedTime; int totalLaps = 0; int code = 0; int py; Float points = (float) 0; int rdgPoints = 0;
		for (int raceNumber = 1; raceNumber <= numRaces; raceNumber++) {
			// Get the results for race N. mRowId is the ID of the series.
			Cursor raceCursor = mDbHelper.fetchRaceByNumber(raceNumber, mRowId);
			// Declare an array to hold all competitors in this race
			ArrayList<CompetitorTimes> wholeRace = new ArrayList<CompetitorTimes>();
			// Populate the array with the finish positions for all competitors in the race
			for (int entry = 1; entry<=numEntries; entry++) {
				float correctedTime;
				float proRataTime = 20000;
				rowId = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_ROWID));
				entryId = raceCursor.getLong(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_ENTRY));
				result = 0; // this is needed to convert the ranked elapsed times into a position
				code = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
				startTime = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_START_TIME));
				finishTime = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_FINISH_TIME));
				elapsedTime = finishTime - startTime;
				laps = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_LAPS));
				totalLaps = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_TOTAL_LAPS));
				Cursor pyCursor = mDbHelper.fetchEntry(entryId);
				py = pyCursor.getInt(pyCursor.getColumnIndex(SailscoreDbAdapter.KEY_PY));
				rdgPoints = raceCursor.getInt(raceCursor.getColumnIndex(SailscoreDbAdapter.KEY_RDG_POINTS));
				pyCursor.close();

				switch (code) {
				case 0:
					// Put in a fix to ensure result is non-zero, though it shouldn't be of course
					if (elapsedTime <= 0) {
						result = numEntries + 1; // assume a DNC
						proRataTime = 200000; // A number greater than 48 hours
						code = 1; // Force it to DNC
					} else if (totalLaps == 0) { // assume all laps completed, no pro rata
						correctedTime = elapsedTime*1000/py;
						proRataTime = correctedTime;
					} else { // pro rata
						if (laps == 0) { // unless sailed laps hasn't been entered
							laps = totalLaps;
						}
						correctedTime = elapsedTime*1000/py;
						proRataTime = correctedTime * totalLaps / laps;
					}
				break;
				// Next step is to put in results for the starters plus one group
				case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
					result = numEntries + 1;
					break;
				case 10: // Duty needs special treatment later
					result = numEntries + 1;
					break;
				}
				
				
				// Now put all the info into an instance of the CompetitorTimes class
				CompetitorTimes race = new CompetitorTimes (rowId, entryId, startTime, finishTime, laps, totalLaps, code, result, rdgPoints, proRataTime, points);
				wholeRace.add(race);
				raceCursor.moveToNext();
			}
			raceCursor.close();
			// At this point all competitors should have non-zero values for proRataTime so we can rank them
			Collections.sort(wholeRace, new Comparator<CompetitorTimes>() {
				@Override
				public int compare(CompetitorTimes lhs, CompetitorTimes rhs) {
					if (lhs.proRataTime == rhs.proRataTime) {
						if ((lhs.code == 14 || lhs.code == 15) && (rhs.code == 14 || rhs.code == 15)) {
							return 0;
						} else if (lhs.code == 14 || lhs.code == 15) {
							return 1;
						} else if (rhs.code == 14 || rhs.code == 15) {
							return -1;
						} else {
							return 0;
						}
					} else {
						return (int) (lhs.proRataTime - rhs.proRataTime); // compare the finish positions first
					}
				}
			});
						
			// With the results in the right order we now need to apportion any tied points and put in points for all results
			boolean match = false;
			int accPoints = 0;
			int location = 0;
			int matchCount = 0;
			Float avgPoints = (float) 0;
			for (int row = 0; row<numEntries-1; row++) {
				if (wholeRace.get(row).proRataTime == wholeRace.get(row+1).proRataTime) {
					if (match) {
						if (wholeRace.get(row).code != 14 && wholeRace.get(row).code != 15) {
							accPoints += row + 1;
							matchCount ++;
						}
					} else {
						// start off a possible group of matches
						// by setting accumulated points to the first position with a tie
						accPoints = (wholeRace.get(row).code != 14 && wholeRace.get(row).code != 15) ?
								row +1 :
								0; 
						match = true;
						location = row; // note where the matches start
						matchCount = 1; // need to count matches so we can eliminate SCP and ZFP matches
					}
				} else {
					if (match) { // we have accumulated matches that now need to be processed
						if (wholeRace.get(row).code != 14 && wholeRace.get(row).code != 15) {
							accPoints += row + 1;
							matchCount ++;
						}
						match = false; // only need to reset if it was true
						// accPoints could be 0 if the only matches also had SCP or ZFP
						avgPoints = matchCount == 0 || accPoints == 0 ? (float) row : (float) (accPoints/matchCount);
						accPoints = 0; // ready for the next group of matches
						for (int updateRow = location; updateRow <= row; updateRow++) {
							if (wholeRace.get(updateRow).proRataTime < 200000) {
								if (wholeRace.get(updateRow).code != 14 && wholeRace.get(updateRow).code != 15) {
									wholeRace.get(updateRow).points = avgPoints;
								} else {
									// Give an unaltered value to those with scoring penalties
									wholeRace.get(updateRow).points = (float) updateRow +1;
								}
							} else {
								wholeRace.get(updateRow).points = (float) numEntries+1;
							}
						}
					} else { // Or we just don't have a match so we put in the points
						if (wholeRace.get(row).proRataTime < 200000) { // finds a legal finish position
							wholeRace.get(row).points = (float) row + 1;
						} else { // it's a starters + 1 code
							wholeRace.get(row).points = (float) numEntries+1;
						}
					}
				}
			}
			// Finally we need to account for the last row
			if (match) { // there was a match between the last row processed above and the one we are processing now
				if (wholeRace.get(numEntries-1).code != 14 && wholeRace.get(numEntries-1).code != 15) {
					accPoints += numEntries;
					matchCount ++;
				}
				// accPoints could be 0 if the only matches also had SCP or ZFP
				if (matchCount == 0 || accPoints == 0) {
					avgPoints = (float) numEntries;
				} else {
					avgPoints = (float) accPoints;
					avgPoints = avgPoints/matchCount;
				}
				//avgPoints = matchCount == 0 || accPoints == 0 ? (float) numEntries : (float) (accPoints/matchCount);
				accPoints = 0; // ready for the next group of matches
				for (int updateRow = location; updateRow <= numEntries-1; updateRow++) {
					if (wholeRace.get(updateRow).proRataTime < 200000) {
						if (wholeRace.get(updateRow).code != 14 && wholeRace.get(updateRow).code != 15) {
							wholeRace.get(updateRow).points = avgPoints;
						} else {
							// Give an unaltered value to those with scoring penalties
							wholeRace.get(updateRow).points = (float) updateRow +1;
						}
					} else {
						wholeRace.get(updateRow).points = (float) numEntries+1;
					}
				}


				
			} else {
				if (wholeRace.get(numEntries-1).proRataTime < 200000) { // finds a legal finish position
					wholeRace.get(numEntries-1).points = numEntries;
				} else {
					wholeRace.get(numEntries-1).points = (float) numEntries+1;
				}
			}
			
			// Now we need to adjust any results that have scoring penalties
			for (int row = 0; row<numEntries-1; row++) {
				code = wholeRace.get(row).code;
				switch (code) {
				case 14: case 15:
					wholeRace.get(row).points = wholeRace.get(row).points + numEntries * (float) 0.2;
					break;
				case 11:
					wholeRace.get(row).points = wholeRace.get(row).rdgPoints;
					break;
				}
				// round to one decimal place
				points = wholeRace.get(row).points * 10;
				points = (float) Math.round(points)/10;
				wholeRace.get(row).points = points;
			}

			// After processing all results the database needs to be updated
			for (int row = 0; row < numEntries; row++) {
				mDbHelper.updatePoints(	wholeRace.get(row).entryId, // entryId
										mRowId, 					// series
										raceNumber, 				// raceNumber
										wholeRace.get(row).points);	// the rest
				// This is a bit of a hack as it hijacks the result value in the database to store the pro-rata time (for display purposes only)
				mDbHelper.updateSingleResult(	wholeRace.get(row).entryId, // entryId
						mRowId, 					// series
						raceNumber, 				// raceNumber
						(int) wholeRace.get(row).proRataTime);	// the rest
			}
		}
	}

	
	// Method to take the number of races and the text description of the discard profile
	// and return how many races to discard.
	private int parseDiscardProfile(int numRaces, String discardProfile) {
		int numDiscards = 0; // Default do no discards
		String delimeter = " ";
		String[] discardList = discardProfile.split(delimeter);
		// If the discard profile isn't long enough to cover all the races just return the last value
		if (discardList.length < numRaces) {
			numDiscards = Integer.parseInt(discardList[discardList.length-1]);
		} else {
			numDiscards = Integer.parseInt(discardList[numRaces-1]);
		}
		return numDiscards;
	}

	// Take this bit of code out to a separate method to reduce higher level complexity
	// This is the part where the database is queried for all the results and we build
	// up the list of actual numerical points for each race
	private ArrayList<RaceResultPair> getPoints (Long mEntry, int numEntries) {
    	netPoints = 0;
    	float sumPoints = 0;
    	float avgPoints = 0;
    	avgAllRaces = 0;
    	int numGoodResults = 0;
    	Cursor resultsCursor = mDbHelper.getResults(mEntry, mRowId);
        int numResults = resultsCursor.getCount();
        ArrayList<RaceResultPair> resultList = new ArrayList<RaceResultPair>();
        
        // The number of results in the cursor should be the actual number of races in the series
        // since we always score a race even if there was no actual result.
        for (int resultNum = 0; resultNum < numResults; resultNum++) {
        	// Start with a new instance of the result object
    		RaceResultPair raceResult = new RaceResultPair(0,0);
        	// Points value from the database will be correct unless result code is RDGa or RDGb
    		// RDGa is calculated here but RDGb is done after returning because the average
    		// of all races isn't known until all races have been processed.
	        float points = resultsCursor.getFloat(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_POINTS));
	        int result = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_RESULT));
	        int resultCode = resultsCursor.getInt(resultsCursor.getColumnIndex(SailscoreDbAdapter.KEY_CODE));
	        // Before we can process the results we need to fix ones that are missing
	        // Set any un-entered results to the code for DNC
	        if (points == 0 && resultCode == 0) {
	        	resultCode = 1;
	        }
			switch (resultCode) {
				// First group are normal results or where the finish position is actually used
				case 0: 
					raceResult.setResultCode(0);
					sumPoints += points; // keep tally of sum for average if we get a RDG
					numGoodResults++;
					avgPoints = sumPoints/(resultNum + 1);
					break;
				// Next group are the 'starters plus one' group
				case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9:
					raceResult.setResultCode(resultCode);
					sumPoints += numEntries + 1;
					numGoodResults++;
					avgPoints = sumPoints/numGoodResults;
					points = numEntries + 1;
					break;
				// A duty is scored as the average of all in the series
				case 10:
					raceResult.setResultCode(10);
					sumPoints += Math.round(numResults/2);
					numGoodResults++;
					avgPoints = sumPoints/numGoodResults;
					break;
				case 11:
					raceResult.setResultCode(resultCode);
					sumPoints += points; // keep tally of sum for average if we get a RDG
					numGoodResults++;
					avgPoints = sumPoints/(resultNum + 1);
					break;
				// Mark RDGa so that average can be added in later
				case 12:
					raceResult.setResultCode(12);
					points = -1;
					break;
				// RDGb is average of all races so far
				// Only update the result but leave the running average as it was
				case 13:
					raceResult.setResultCode(13);
					points = avgPoints;
					break;
				// SCP and ZFP are a 20% scoring penalty
				// but these have already been calculated.
				case 14: case 15:
					raceResult.setResultCode(resultCode);
					sumPoints += points;
					numGoodResults++;
					avgPoints = sumPoints/numGoodResults;
					break;
				}
			raceResult.setRace(resultNum + 1);
	        raceResult.setResult(result);
			raceResult.setPoints(points);
			resultList.add(raceResult);
			resultsCursor.moveToNext();
        }
        
        // At the end we can say the average of all races scored is now known:
        avgAllRaces = avgPoints;
        resultsCursor.close();
		return resultList;

	}
	
	// Method to calculate the points from the number of discards and total points.
	// RRS A2 says if a boat has two or more equal worst scores then the scores for races sailed earliest in the
	// series shall be excluded. To do this we have to rank the races in score order and then in race order.
	private float removeDiscards(float grossPoints, int numDiscards, ArrayList<RaceResultPair> raceResults) {
		// Sort the list first into order of min to max points value
		// This sort needs to rank the earliest races first if results are equal
		Collections.sort(raceResults, new Comparator<RaceResultPair> () {
			@Override
			public int compare(RaceResultPair arg0, RaceResultPair arg1) {
				// Declare local holder for the race results part of each ScoresObj being passed in for comparison
				//ArrayList<RaceResultPair> raceLhs = new ArrayList<RaceResultPair>();
				//ArrayList<RaceResultPair> raceRhs = new ArrayList<RaceResultPair>();
				long race0 = arg0.getRace();
				long race1 = arg1.getRace();
				float points0 = arg0.getPoints();
				float points1 = arg1.getPoints();
				// Start off as if just comparing the points values
				if (points0 < points1) {
					return -1;
				} else if (points0 > points1) {
					return 1;
				} else {
					// but if points are equal then return the one with the earliest race in the series
					// race0 and race1 cannot be equal so it will always return one or the other
					return (int) (race1 - race0);
				}
			}
		});
		//Collections.sort(raceResults);
		float netPoints = grossPoints;
		int numResults = raceResults.size();
		int additionalDiscards;
		// Only do this part if there are non-zero discards and not all are to be discarded (illegal so ignore)
		if ((numDiscards > 0) && (numDiscards < raceResults.size())) {
			additionalDiscards = 0;
			// Work back through results to discard worst first
			for (int resultNum = numResults - 1; resultNum >= numResults - numDiscards; resultNum--) {
				// For DNE and DGM codes we don't discard but need to discard other (better) results instead
				if (   (raceResults.get(resultNum).getResultCode() == 4 ) // DNE - should add a method to get these
					|| (raceResults.get(resultNum).getResultCode() == 8)) { // DGM
					additionalDiscards++;
					continue;
				}
				netPoints -= raceResults.get(resultNum).getPoints();
				raceResults.get(resultNum).setDiscarded(1);
			}
			// If there were DNE or DGM codes while processing discards then we need to discard additional scores
			// it should be impossible to discard more than numResults so no need to check range.
			if (additionalDiscards != 0) {
				for (int resultNum = numResults - 1 - numDiscards;
						resultNum >= numResults - numDiscards - additionalDiscards; // ???
						resultNum--) {
					netPoints -= raceResults.get(resultNum).getPoints();
					raceResults.get(resultNum).setDiscarded(1);
				}
				additionalDiscards = 0;
			}
		}
		return netPoints;
	}

	// Simple method to add the positions to the array
	// This is probably not the best way to do this but it can't be done until
	// all the scores are known and entries are in the right order.
	private void insertRanks () {
		for (int i = 0; i< combinedList.size(); i++) {
			// Get the object from the array
			ScoresObj combinedObj = new ScoresObj();
			combinedObj = combinedList.get(i);
			// Update the object
			combinedObj.setPosition(i+1);
			// Stick it back in the array
			combinedList.set(i, combinedObj);
		}
	}
	
	private String buildResultsString(boolean seriesType, ArrayList<RaceResultPair> raceResults) {
		String sResults = "All races: ";
		for (int i=0; i<raceResults.size(); i++) {
			boolean discarded = raceResults.get(i).isDiscarded() == 1 ? true : false;
			sResults = sResults + "R" + Integer.toString(i+1) + ":";
			if (discarded) {
				sResults = sResults + "(";
			}
			int resultCode = raceResults.get(i).getResultCode();
			int result = raceResults.get(i).getResult();
			float points = raceResults.get(i).getPoints();
			// We need to do some fixes here because if no results have ever been entered
			// we need to display it as a DNC even if the resultCode comes back as 0.
			// We also need to switch an elapsed time of 20000 to a DNC.
			if (resultCode == 0 && result == 0) {
				resultCode = 1;
			}
			if (seriesType && (result == 200000)) {
				resultCode = 1;
			}
			// Display result and associated code
			if (resultCode > 0) {
				sResults = sResults + mHelper.convertResultCode(resultCode);
			} else {
				sResults = sResults + Integer.toString(result);
			}
			String formattedResult = Float.toString(points);
			String.format(formattedResult, .1f);
			sResults = sResults + ", " + formattedResult;
			if (discarded) {
				sResults = sResults + ")";
			}
			if (i != raceResults.size() -1) {
				sResults = sResults + ", ";
			}
		}
		return sResults;
		
	}

	public Bitmap getWholeListViewItemsToBitmap() {

	    ListView listview    = getListView();
	    ListAdapter adapter  = listview.getAdapter(); 
	    int itemscount       = adapter.getCount();
	    int allitemsheight   = 0;
	    List<Bitmap> bmps    = new ArrayList<Bitmap>();

	    for (int i = 0; i < itemscount; i++) {

	        View childView      = adapter.getView(i, null, listview);
	        childView.measure(MeasureSpec.makeMeasureSpec(listview.getWidth(), MeasureSpec.EXACTLY), 
	                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

	        childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
	        childView.setDrawingCacheEnabled(true);
	        childView.buildDrawingCache();
	        bmps.add(childView.getDrawingCache());
	        allitemsheight+=childView.getMeasuredHeight();
	    }

	    Bitmap bigbitmap    = Bitmap.createBitmap(listview.getMeasuredWidth(), allitemsheight, Bitmap.Config.ARGB_8888);
	    Canvas bigcanvas    = new Canvas(bigbitmap);

	    Paint paint = new Paint();
	    int iHeight = 0;

	    for (int i = 0; i < bmps.size(); i++) {
	        Bitmap bmp = bmps.get(i);
	        bigcanvas.drawBitmap(bmp, 0, iHeight, paint);
	        iHeight+=bmp.getHeight();

	        bmp.recycle();
	        bmp=null;
	    }


	    return bigbitmap;
	}
	
	public void saveBitmap(Bitmap bmp) {
		String path = Environment.getExternalStorageDirectory().toString();
		path = path + "/" + "sailscore";
		String folder = "sailscore"; // For now put all images in one place
		File f = new File(Environment.getExternalStorageDirectory(),
                folder);
        if (!f.exists()) {
            f.mkdirs();
        }
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		String series = seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES));
		seriesCursor.close();
		OutputStream fOut = null;
		File file = new File(path, series+".jpg"); // the File to save to
		try {
			fOut = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
		try {
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void savePDF(Bitmap bmp) {
		String path = Environment.getExternalStorageDirectory().toString();
		path = path + "/" + "sailscore";
		String folder = "sailscore"; // For now put all images in one place
		File f = new File(Environment.getExternalStorageDirectory(),
                folder);
        if (!f.exists()) {
            f.mkdirs();
        }
		Cursor seriesCursor = mDbHelper.fetchSeries(mRowId);  // Just need the name of the series
		String series = seriesCursor.getString(seriesCursor.getColumnIndex(SailscoreDbAdapter.KEY_SERIES));
		seriesCursor.close();
		OutputStream fOutPng = null;
		File pngFile = new File(path, series+".png"); // the File to save to
		try {
			fOutPng = new FileOutputStream(pngFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bmp.compress(Bitmap.CompressFormat.PNG, 100, fOutPng); // temporarily save to PNG format before conversion to PDF
		URL pdfIn = null;
		try {
			pdfIn = new URL("file://"+path + "/" + series + ".png");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			fOutPng.flush();
			fOutPng.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Document document = new Document();
		document.addTitle("Series: "+series);
	    document.addAuthor("Sailscore");
	    document.addCreator("Sailscore, using itext");
		File pdfFile = new File(path, series+".pdf"); // the File to save to
		try {
	        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
	        document.open();
	        Paragraph preface = new Paragraph();
	        preface.add(new Paragraph(("Sailscore results for "+ series+"\n"), catFont));
	        Image image = Image.getInstance(pdfIn);
	        //image.scaleAbsolute(400f, 200f);
	        image.scalePercent((float) 25.0);
	        document.add(preface);
	        document.add(image);
	        document.close();
	    } catch(Exception e){
	      e.printStackTrace();
	    }
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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(SailscoreDbAdapter.KEY_ROWID, mRowId);
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		setRowIdFromIntent(); // Get the series Id
		combinedList.clear();
		fillData();
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mDbHelper.close();
    }

}
