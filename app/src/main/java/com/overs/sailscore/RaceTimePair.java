/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class RaceTimePair implements Comparable<RaceTimePair>{
	private int race = 0;
	private Float elapsedTime = null;
	private int result;
	private int discarded = 0;
	private int resultCode = 0;
	public RaceTimePair (int race, float result) {
		this.race = race;
		this.elapsedTime = result;
	}
	public int getRace() {
		return race;
	}
	public void setRace(int race) {
		this.race = race;
	}
	public float getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(float result) {
		this.elapsedTime = result;
	}
	@Override
	public int compareTo(RaceTimePair arg0) {
		return Float.compare(elapsedTime,  arg0.getElapsedTime());
	}
	public int isDiscarded() {
		return discarded;
	}
	public void setDiscarded(int discarded) {
		this.discarded = discarded;
	}
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}
	

}
