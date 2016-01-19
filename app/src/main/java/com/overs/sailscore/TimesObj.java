/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class TimesObj {
	private String race_number = "";
	private String sMins = "";
	private String sSecs = "";
	private String fMins = "";
	private String fSecs = "";
	private int spin_position = 0;
	private String lapsSailed = "";
	private String totalLaps = "";
	private String redressPosition="";
	private boolean codePriority;
	

	public String getRaceNumber() {
		return race_number;
	}

	public void setRaceNumber(String race_number) {
		this.race_number = race_number;
	}
	
	public int getSpinPosition() {
		return spin_position;
	}

	public void setSpinPosition(int spin_position) {
		this.spin_position = spin_position;
	}

	public String getsMins() {
		return sMins;
	}

	public void setsMins(String sMins) {
		this.sMins = sMins;
	}

	public String getsSecs() {
		return sSecs;
	}

	public void setsSecs(String sSecs) {
		this.sSecs = sSecs;
	}

	public String getfMins() {
		return fMins;
	}

	public void setfMins(String fMins) {
		this.fMins = fMins;
	}

	public String getfSecs() {
		return fSecs;
	}

	public void setfSecs(String fSecs) {
		this.fSecs = fSecs;
	}

	public String getLaps() {
		return lapsSailed;
	}

	public void setLaps(String laps) {
		this.lapsSailed = laps;
	}

	public String getTotalLaps() {
		return totalLaps;
	}

	public void setTotalLaps(String totalLaps) {
		this.totalLaps = totalLaps;
	}

	public String getRedressPosition() {
		return redressPosition;
	}

	public void setRedressPosition(String redressPosition) {
		this.redressPosition = redressPosition;
	}

	public boolean getCodePriority() {
		return codePriority;
	}

	public void setCodePriority(boolean codePriority) {
		this.codePriority = codePriority;
	}
}
