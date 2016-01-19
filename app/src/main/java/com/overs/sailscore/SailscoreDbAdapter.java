/*
 * This is the main database access class with all the methods needed by all the
 * activities for reading, adding, deleting and updating the contents of the database.
 * There are 3 tables in the database:
 * 	Entries: used to store all the competitors, one row per competitor, with all required details.
 * 	Series: used to list all the series and information about a series, one row per series.
 * 	Results: used to keep track of all races, one row per race.
 * 
 * The results table is the most important as it contains the most detailed information.
 * The results table can be queried to find everything a competitor competes in. 
 */

package com.overs.sailscore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SailscoreDbAdapter {
	private static final String DATABASE_NAME="data";
	private static final String ENTRIES_TABLE="entries";
	private static final String SERIES_TABLE="series";
	private static final String RESULTS_TABLE="results";
	private static final int DATABASE_VERSION=1;
	public static final String KEY_ROWID="_id";
	public static final String KEY_HELM="helm";
	public static final String KEY_CREW="crew";
	public static final String KEY_CLASS="boatclass";
	public static final String KEY_PY="py";
	public static final String KEY_SAILNO="sailnumber";
	public static final String KEY_CLUB="club";
	public static final String KEY_SERIES="series_name";
	public static final String KEY_DISCARD_PROFILE="discard_profile";
	public static final String KEY_NUMRACES="num_races";
	public static final String KEY_HCAP="hcap";
	public static final String KEY_RACE="race";
	public static final String KEY_ENTRY="entry";
	public static final String KEY_RESULT="result";
	public static final String KEY_START_TIME="start_time";
	public static final String KEY_FINISH_TIME="finish_time";
	public static final String KEY_LAPS="sailed_laps";
	public static final String KEY_TOTAL_LAPS="total_laps";
	public static final String KEY_CODE="result_code";
	public static final String KEY_RDG_POINTS="rdg_points";
	public static final String KEY_POINTS="points";
	public static final String KEY_SCORED="scored";
	public static final String KEY_DISCARDED="discarded";
	public static final String KEY_POSITION="position";
	public static final String KEY_TOTAL="total";
	public static final String KEY_NETT="nett";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	// Build the SQL commands to create the 3 database tables we will be using
	private static final String ENTRIES_TABLE_CREATE=
			"create table " + ENTRIES_TABLE + " ("
			+ KEY_ROWID + " integer primary key autoincrement, "
			+ KEY_HELM + " text not null, "
			+ KEY_CREW + " text, "
			+ KEY_CLASS + " text not null, "
			+ KEY_PY + " integer not null, "
			+ KEY_SAILNO + " text not null, "
			+ KEY_CLUB + " text);" ;

	private static final String SERIES_TABLE_CREATE=
			"create table " + SERIES_TABLE + " ("
			+ KEY_ROWID + " integer primary key autoincrement, "
			+ KEY_SERIES + " text not null, "
			+ KEY_DISCARD_PROFILE + " text not null, "
			+ KEY_HCAP + " integer not null, "
			+ KEY_NUMRACES + " integer not null);" ;

	/* Results table contains one row per actual race.
	 * A row is added for each race in a series at the time the series is created.
	 * A row is added for each competitor in a series if a race is added to an existing series.
	 * A row is deleted for each race removed from an existing series for each competitor in that series.
	 * When a race is added the RESULT column may be empty initially. All other columns must contain values.
	 * Note that the value of position is only here because there is nowhere else to put it and it will
	 * hold the same value for all races for a given competitor in a given series.
	 */
	private static final String RESULTS_TABLE_CREATE=
			"create table " + RESULTS_TABLE + " ("
			+ KEY_ROWID + " integer primary key autoincrement, "
			+ KEY_RACE + " integer not null, "
			+ KEY_ENTRY + " integer not null, "
			+ KEY_SERIES + " integer not null, "
			+ KEY_RESULT + " integer, "
			+ KEY_START_TIME + " integer, "
			+ KEY_FINISH_TIME + " integer, "
			+ KEY_LAPS + " integer, "
			+ KEY_TOTAL_LAPS + " integer, "
			+ KEY_CODE + " integer, "
			+ KEY_RDG_POINTS + " integer, "
			+ KEY_POINTS + " real, "
			+ KEY_SCORED + " integer, "
			+ KEY_DISCARDED + " integer, "
			+ KEY_POSITION + " integer, "
			+ KEY_TOTAL + " real, "
			+ KEY_NETT + " real);" ;
	
	private final Context mCtx;
	
	// Call the constructor
	public SailscoreDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}
	
	/* The private class defined here has methods to create the database 
	 * in the first place and upgrade the database at some future time. 
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(ENTRIES_TABLE_CREATE);
			db.execSQL(SERIES_TABLE_CREATE);
			db.execSQL(RESULTS_TABLE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Not used but could upgrade later using ALTER scripts
		}
	}
	
	// A public method to open the database in order to use it.
	public SailscoreDbAdapter open() throws android.database.SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this; // returns itself because the caller needs to access data from this method
	}
	
	// The database must be closed after use so create a method to do it.
	public void close() {
		mDbHelper.close();
	}
	
    /////////////////////////////////////////////////////////////////////
	//////// Methods for managing the competitor entries table   ////////
    /////////////////////////////////////////////////////////////////////

	/* A public method to create a new boat entry.
	 * The fields are passed to this method and a ContentValues object used to package
	 * them up before inserting them into the database.
	 */
	public long createEntry(String helm, String crew, String boatclass, int py, String sailnumber, String club) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_HELM,  helm);
		initialValues.put(KEY_CREW, crew);
		initialValues.put(KEY_CLASS, boatclass);
		initialValues.put(KEY_PY, py);
		initialValues.put(KEY_SAILNO, sailnumber);
		initialValues.put(KEY_CLUB, club);
		return mDb.insert(ENTRIES_TABLE, null, initialValues);
	}
	
	/* The method to delete an entry. A few things about this method:
	 * For now the entry is deleted blind (if it exists). In other words no check is made
	 * yet if the entry is used anywhere. At a later date a check needs to be put in to 
	 * prevent the deletion unless the entry is unused in a series. 
	 * The delete method on the database returns a result that is tested in the return.
	 * Thus the calling code can see if the deletion was successfull (true) or not (false).
	 */
	public boolean deleteEntry(long rowId) {
		return mDb.delete(ENTRIES_TABLE, KEY_ROWID + "=" + rowId, null) >0;
	}
	
	/* This method was from the task reminder app and is used to find all the
	 * information to put in the list view of all entries.
	 */

	public Cursor fetchAllEntries() {
		return mDb.query(ENTRIES_TABLE, new String[] {KEY_ROWID, KEY_HELM, KEY_CREW, KEY_CLASS, KEY_PY, KEY_SAILNO, KEY_CLUB}, null, null, null, null, null);
	}
	
	/* Method used to extract a boat entry for use in a series (or any other use).
	 * The row id is used to identify the entry to extract. In all cases where a boat
	 * entry is used the row id in this database is used to uniquely identify it and
	 * everything about it is then found by querying the database using this method.
	 * This method will therefore get a lot of use! 
	 * The thing that gets returned is a Cursor object containing all the information from
	 * the database query. 
	 */
	public Cursor fetchEntry(long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, ENTRIES_TABLE, new String[] 
				{KEY_ROWID, KEY_HELM, KEY_CREW, KEY_CLASS, KEY_PY, KEY_SAILNO, KEY_CLUB},
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		// Cursor would be null if the entry wasn't found
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	/* Method to update fields in an existing entry.
	 * Uses a ContentValues object again (like when an entry was created) by calling its constructor.
	 * The information to go into the database is bundled up into the ContentValues object and then
	 * the update method called on the database.
	 * Some things to consider with doing this:
	 * If the entry being updated is used in a series then that series will be affected. For now
	 * just let the update happen but just like when deleting an entry some check needs to be performed.
	 * At the very least a field should be added to the database entry to indicate it is in use and this
	 * could then be used to prevent deletion. For upating a confirmation could be requested.
	 */
	public boolean updateEntry (long rowId, String helm, String crew, String boatclass, int py, String sailnumber, String club) {
		ContentValues args = new ContentValues(); // call constructor
		args.put(KEY_HELM, helm);
		args.put(KEY_CREW, crew);
		args.put(KEY_CLASS, boatclass);
		args.put(KEY_PY, py);
		args.put(KEY_SAILNO, sailnumber);
		args.put(KEY_CLUB, club);
		// The thing that gets returned is true or false depending on the success of the update.
		return mDb.update(ENTRIES_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

    /////////////////////////////////////////////////////////////////////
	//////// Methods for managing the series table               ////////
    /////////////////////////////////////////////////////////////////////

	public long createSeries(String seriesName, int numRaces, String discardProfile, int hcap) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SERIES,  seriesName);
		initialValues.put(KEY_NUMRACES, numRaces);
		initialValues.put(KEY_DISCARD_PROFILE, discardProfile);
		initialValues.put(KEY_HCAP, hcap);
		return mDb.insert(SERIES_TABLE, null, initialValues);
	}
	
	public boolean deleteSeries(long rowId) {
		return mDb.delete(SERIES_TABLE, KEY_ROWID + "=" + rowId, null) >0;
	}
	
	public Cursor fetchAllSeries() {                            
		return mDb.query(SERIES_TABLE, new String[] {KEY_ROWID, KEY_SERIES, KEY_DISCARD_PROFILE, KEY_HCAP, KEY_NUMRACES}, null, null, null, null, null);
	}
	
	public Cursor fetchSeries(Long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, SERIES_TABLE, new String[] 
				{KEY_ROWID, KEY_SERIES, KEY_DISCARD_PROFILE, KEY_HCAP, KEY_NUMRACES},
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		// Cursor would be null if the entry wasn't found
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public boolean updateSeries (long rowId, String series_name, int numRaces, String discardProfile, int hcap) {
		ContentValues args = new ContentValues();
		args.put(KEY_SERIES, series_name);
		args.put(KEY_DISCARD_PROFILE, discardProfile);
		args.put(KEY_HCAP, hcap);
		args.put(KEY_NUMRACES, numRaces);
		// The thing that gets returned is true or false depending on the success of the update.
		return mDb.update(SERIES_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

    /////////////////////////////////////////////////////////////////////
	//////// Methods for managing the results table              ////////
    /////////////////////////////////////////////////////////////////////

	// To update a result we need to know what race in what series is being added for what competitor
	// The result row will already exist as it is added when an entry is selected for a series
	public boolean updateResult (Long entry, Long series, Long race, int result, int start, int finish,
			int laps, int totalLaps, int resultCode, int rdg_points, float points, int scored, int discarded,
			int position, float total, float nett) {
		ContentValues args = new ContentValues();
		args.put(KEY_RACE, race);
		args.put(KEY_ENTRY, entry);
		args.put(KEY_SERIES, series);
		args.put(KEY_RESULT, result);
		args.put(KEY_START_TIME, start);
		args.put(KEY_FINISH_TIME, finish);
		args.put(KEY_LAPS, laps);
		args.put(KEY_TOTAL_LAPS, totalLaps);
		args.put(KEY_CODE, resultCode);
		args.put(KEY_RDG_POINTS, rdg_points);
		args.put(KEY_POINTS, points);
		args.put(KEY_SCORED, scored);
		args.put(KEY_DISCARDED, discarded);
		args.put(KEY_POSITION, position);
		args.put(KEY_TOTAL, total);
		args.put(KEY_NETT, nett);
		// First need to locate the id of the race
		Cursor mCursor = mDb.query(true, RESULTS_TABLE, new String[] 
				{KEY_ROWID, KEY_RACE, KEY_ENTRY, KEY_SERIES, KEY_RESULT, KEY_START_TIME, KEY_FINISH_TIME, 
				KEY_LAPS, KEY_TOTAL_LAPS, KEY_CODE, KEY_RDG_POINTS, KEY_POINTS, KEY_SCORED, KEY_DISCARDED, KEY_POSITION, KEY_TOTAL, KEY_NETT},
				KEY_ENTRY + " = " + entry + " AND " + KEY_RACE + " = " + race + " AND " + KEY_SERIES + " = " + series, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		int rowId = mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID));
		mCursor.close();
		return mDb.update(RESULTS_TABLE, args, KEY_ROWID + " = " + rowId, null) > 0;
	}
	// To update a result we need to know what race in what series is being added for what competitor
	// The result row will already exist as it is added when an entry is selected for a series
	public boolean updatePoints (Long entry, Long series, int race, float points) {
		ContentValues args = new ContentValues();
		args.put(KEY_RACE, race);
		args.put(KEY_ENTRY, entry);
		args.put(KEY_SERIES, series);
		args.put(KEY_POINTS, points);
		// First need to locate the id of the race
		Cursor mCursor = mDb.query(true, RESULTS_TABLE, new String[] 
				{KEY_ROWID, KEY_RACE, KEY_ENTRY, KEY_SERIES, KEY_POINTS},
				KEY_ENTRY + " = " + entry + " AND " + KEY_RACE + " = " + race + " AND " + KEY_SERIES + " = " + series, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		int rowId = mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID));
		mCursor.close();
		return mDb.update(RESULTS_TABLE, args, KEY_ROWID + " = " + rowId, null) > 0;
	}

	public boolean updateSingleResult (Long entry, Long series, int race, int result) {
		ContentValues args = new ContentValues();
		args.put(KEY_RACE, race);
		args.put(KEY_ENTRY, entry);
		args.put(KEY_SERIES, series);
		args.put(KEY_RESULT, result);
		// First need to locate the id of the race
		Cursor mCursor = mDb.query(true, RESULTS_TABLE, new String[] 
				{KEY_ROWID, KEY_RACE, KEY_ENTRY, KEY_SERIES, KEY_RESULT},
				KEY_ENTRY + " = " + entry + " AND " + KEY_RACE + " = " + race + " AND " + KEY_SERIES + " = " + series, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		int rowId = mCursor.getInt(mCursor.getColumnIndex(KEY_ROWID));
		mCursor.close();
		return mDb.update(RESULTS_TABLE, args, KEY_ROWID + " = " + rowId, null) > 0;
	}

	
	// Method to add a row in the results table, used when selecting competitors or adding races
	public boolean addResultRow (Long entry, Long series, int race, int result, int start, int finish, int laps, 
			int totalLaps, int resultCode, int rdg_points, int points, int scored, int discarded, int position, int total, int nett) {
		ContentValues args = new ContentValues();
		args.put(KEY_RACE, race);
		args.put(KEY_ENTRY, entry);
		args.put(KEY_SERIES, series);
		args.put(KEY_RESULT, result);
		args.put(KEY_START_TIME, start);
		args.put(KEY_FINISH_TIME, finish);
		args.put(KEY_LAPS, laps);
		args.put(KEY_TOTAL_LAPS, totalLaps);
		args.put(KEY_CODE, resultCode);
		args.put(KEY_RDG_POINTS, rdg_points);
		args.put(KEY_POINTS, points);
		args.put(KEY_SCORED, scored);
		args.put(KEY_DISCARDED, discarded);
		args.put(KEY_POSITION, position);
		args.put(KEY_TOTAL, total);
		args.put(KEY_NETT, nett);
		return mDb.insert(RESULTS_TABLE, null, args) > 0;
	}
	
	// Method to delete an entry from a series
	public boolean deleteResultsBySeriesEntry(Long seriesId, String entry) {
		String series = seriesId.toString();
		return mDb.delete(RESULTS_TABLE, KEY_ENTRY + "=" + entry + " AND " + KEY_SERIES + "=" + series, null) >0;
	}

	// Method to delete all results for an entry
	public boolean deleteResultsByEntry(String entry) {
		return mDb.delete(RESULTS_TABLE, KEY_ENTRY + "=" + entry, null) >0;
	}

	// Method to delete all results for a series
	public boolean deleteResultsBySeries(Long seriesId) {
		String series = seriesId.toString();
		return mDb.delete(RESULTS_TABLE, KEY_SERIES + "=" + series, null) >0;
	}
	
	// Method to delete an individual result by series and race number
	public boolean deleteResultByRace(Long seriesId, int raceNo) {
		String series = seriesId.toString();
		String race = Integer.toString(raceNo);
		return mDb.delete(RESULTS_TABLE, KEY_SERIES + "=" + series + " AND " + KEY_RACE + "=" + race, null) >0;
	}

	// Get all results for a given competitor in a given series
	public Cursor getResults(Long entry, Long series) {
		String mSeries = series.toString();
		String mEntry = entry.toString();
		Cursor mCursor = mDb.query(true, RESULTS_TABLE, new String[] 
				{KEY_ROWID, KEY_RACE, KEY_RESULT, KEY_START_TIME, KEY_FINISH_TIME, KEY_LAPS, KEY_TOTAL_LAPS, KEY_CODE, KEY_RDG_POINTS, KEY_POINTS, KEY_SCORED, KEY_DISCARDED, KEY_POSITION, KEY_TOTAL, KEY_NETT},
				KEY_ENTRY + "=" + mEntry + " AND " + KEY_SERIES + "=" + mSeries, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/* Get all entries in a given series.
	 * Works by using a table join to look for lines in the results table where the series _id is
	 * found in the series column and using it to locate competitors in the entries table for
	 * that series. 
	 * This would normally return a result line for each race in the series so we filter the
	 * unwanted ones by only returning results for race 1 (since all series have at least one race)
	 */
	public Cursor fetchEntriesFromSeries(Long seriesId) {
		String series = seriesId.toString();
		final String mQuery = "SELECT entries._id, entries.helm, entries.crew, entries.boatclass, entries.sailnumber, entries.club, entries.py "
				+ "FROM entries INNER JOIN results ON entries._id = results.entry WHERE race = 1 AND series_name = ?";
		Cursor mCursor = mDb.rawQuery(mQuery, new String[]{String.valueOf(series)});
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	// Get all the results for a given race so that the times can be converted to points
	public Cursor fetchRaceByNumber(int race, Long seriesId) {
		String sRace = Integer.toString(race);
		String sSeries = Long.toString(seriesId);
		final String mQuery = "SELECT results._id, results.entry, results.start_time, results.finish_time, " +
				"results.sailed_laps, results.total_laps, results.result_code, results.result, results.rdg_points "
				+ "FROM results WHERE race = ? and series_name = ?";
		Cursor mCursor = mDb.rawQuery(mQuery, new String[]{String.valueOf(sRace), String.valueOf(sSeries)});
	if (mCursor != null) {
		mCursor.moveToFirst();
	}
	return mCursor;
	}
	
	// Get all the competitors for a given race so that their information can be displayed
	public Cursor fetchEntriesByRace(Long race, Long seriesId) {
		String sRace = Long.toString(race);
		String sSeries = Long.toString(seriesId);
		final String mQuery = "SELECT entries._id, entries.helm, entries.crew, entries.boatclass, entries.sailnumber, entries.club, entries.py, " +
				"results.result, results.result_code, results.rdg_points, results.start_time, results.finish_time, results.sailed_laps,  " +
				"total_laps "
				+ "FROM entries INNER JOIN results ON entries._id = results.entry WHERE race = ? AND series_name = ?";
//		final String mQuery = "SELECT entries._id, entries.helm, entries.crew, entries.boatclass, entries.sailnumber, entries.club, entries.py "
//				+ "FROM entries INNER JOIN results ON entries._id = results.entry WHERE race = ? AND series_name = ?";
		Cursor mCursor = mDb.rawQuery(mQuery, new String[]{String.valueOf(sRace), String.valueOf(sSeries)});
	if (mCursor != null) {
		mCursor.moveToFirst();
	}
	return mCursor;
	}
	
}
	

