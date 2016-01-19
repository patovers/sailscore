/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class rowObj {
	private boolean cbox_state = false;
	private String boatClass = "";
	private String helm = "";
	private String crew = "";
	private String sailno = "";
	
	public void setHelm (String helm) {
		this.helm = helm;
	}
	
	public String getHelm () {
		return helm;
	}
	
	public void setSail (String sailno) {
		this.sailno = sailno;
	}
	
	public String getSail () {
		return sailno;
	}
	
	public void setBoatClass (String boatclass) {
		this.boatClass = boatclass;
	}
	
	public String getBoatClass () {
		return boatClass;
	}
	
	public void setCheckState (Boolean state) {
		this.cbox_state = state;
	}
	
	public Boolean getCheckState () {
		return cbox_state;
	}

	public String getCrew() {
		return crew;
	}

	public void setCrew(String crew) {
		this.crew = crew;
	}
}
