/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class EntryResultObj {
	private String competitor = "";
	private String result = "";
	private String sMins = "";
	private String sSecs = "";
	private String fMins = "";
	private String fSecs = "";
	private int spinPosition = 0;
	private String lapsSailed = "";
	private String totalLaps = "";
	private String redressPosition="";
	private boolean codePriority;
	
	public String getCompetitor() {
		return competitor;
	}
	public void setCompetitor(String competitor) {
		this.competitor = competitor;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
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
	public int getSpinPosition() {
		return spinPosition;
	}
	public void setSpinPosition(int spin_position) {
		this.spinPosition = spin_position;
	}
	public String getLapsSailed() {
		return lapsSailed;
	}
	public void setLapsSailed(String lapsSailed) {
		this.lapsSailed = lapsSailed;
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
