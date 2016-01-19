/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class ResultObj {
	private String race_number = "";
	private String result = "";
	private int spin_position = 0;
	private String redressPosition="";
	private boolean codePriority = false;
	

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

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
