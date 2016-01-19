/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

public class RaceResultPair implements Comparable<RaceResultPair>{
	private Long race = (long) 0;
	private int result = 0;
	private int discarded = 0;
	private int resultCode = 0;
	private int rdgPoints = 0;
	private float points = 0;
	public RaceResultPair (long race, int result) {
		this.race = race;
		this.result = result;
	}
	public long getRace() {
		return race;
	}
	public void setRace(long race) {
		this.race = race;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
	}
	// This method compares the two result values BUT if either are discarded then the result
	// is modified like this:
	//   If both are discarded or neither are discarded then the actual comparison stands
	//   If one or other of the results are discarded then this forces the decision one
	//   way or the other without considering the actual values.
	// Note also that two of the combinations can't happen. i.e. a value smaller than another but also discarded.
	@Override
	public int compareTo(RaceResultPair arg0) {
		if (this.discarded == arg0.discarded) {
			return Float.compare(this.points,  arg0.getPoints());
		} else {
			return this.discarded - arg0.discarded;
		}
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
	public float getPoints() {
		return points;
	}
	public void setPoints(float points) {
		this.points = points;
	}
	public int getRdgPoints() {
		return rdgPoints;
	}
	public void setRdgPoints(int rdgPoints) {
		this.rdgPoints = rdgPoints;
	}
	

}
