/*
 * This class defines a data type for use in the EntriesSelectListActivity.
 * It allows a single data type to be bound to a row in the list using the
 * EntriesSelectListAdapter custom list adapter.
 */

package com.overs.sailscore;

import java.util.ArrayList;

//public class ScoresObj implements Comparable<ScoresObj> {
public class ScoresObj {
	private Long id = (long) 0;
	private int position = 0;
	private String boatClass = "";
	private String sailno = "";
	private String helm = "";
	private String crew = "";
	private String club = "";
	private float grossPts;
	private float nettPts;
	private ArrayList<RaceResultPair> raceResults;
	private String sRaceResults;
	
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
	
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}

	public float getGrossPts() {
		return grossPts;
	}

	public void setGrossPts(float grossPts) {
		this.grossPts = grossPts;
	}

	public float getNettPts() {
		return nettPts;
	}

	public void setNettPts(float netPoints) {
		this.nettPts = netPoints;
	}

	public String getCrew() {
		return crew;
	}

	public void setCrew(String crew) {
		this.crew = crew;
	}

	public ArrayList<RaceResultPair> getRaceResults() {
		return raceResults;
	}

	public void setRaceResults(ArrayList<RaceResultPair> raceResults) {
		this.raceResults = raceResults;
	}

	public String getsRaceResults() {
		return sRaceResults;
	}

	public void setsRaceResults(String sRaceResults) {
		this.sRaceResults = sRaceResults;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/* The method to compare two objects in this class is the core of the scoring program
	 * It needs to do the following things:
	 * 1. Take the nett points values of the two objects and if they are not equal, return a result
	 *    for the one that is lower.
	 * 2. If the nett points are equal then sort the results in order of point value and return a 
	 *    result for the one where the first position of difference puts it at a lower total.
	 * 3. If the end of the list of results is reached then return a result for the one that had
	 *    a lower value in the last race.
	 * 4. If the last race had equal points values for them both then work back through the races
	 *    and return a result for the one with the lowest result for the first race at which they
	 *    differ.
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	
	/*
	@Override
	public int compare(ScoresObj raceResultsLhs, ScoresObjRhs) {
		// Declare local holder for the race results part of each ScoresObj being passed in for comparison
		ArrayList<RaceResultPair> raceResultsLhs = new ArrayList<RaceResultPair>();
		ArrayList<RaceResultPair> raceResultsRhs = new ArrayList<RaceResultPair>();
		int nettPointsLhs = this.nettPts;
		int nettPointsRhs = arg0.getNettPts();
		int numRaces = raceResultsLhs.size();
		// First step split scores based on nett points
		if (nettPointsLhs < nettPointsRhs) {
			return -1;
		} else if (nettPointsRhs < nettPointsLhs) {
			return 1;
		} else {
			// Next step - arrange scores in increasing order for both lists and work through them to find differences
			Collections.sort(raceResultsLhs);
			Collections.sort(raceResultsRhs);
			int result;
			for (result = 0; result<numRaces; result++) {
				if (raceResultsLhs.get(result).getResult() == raceResultsRhs.get(result).getResult()) {
					continue;
				} else {
					break; // capture value of result to indicate where the difference is
				}
			}
			if (result < numRaces) {
				result--; // take one off because loop counter moves on before exiting
				// We've found a difference before the end of the result list so return the one with the lowest number at this result
				if (raceResultsLhs.get(result).getResult() < raceResultsRhs.get(result).getResult()) {
					return -1;
				} else {
					return 1;
				}
			} else {
				// Next step:
				// We have reached the end and not managed to split the tie so we need to look at the results
				// from the end backwards, starting with the last race
				// First though we need to re-sort the results in order of race rather than order of result
				// To do this we need to create a different comparator 
				Collections.sort(raceResultsLhs, new Comparator<RaceResultPair>() {
					public int compare(RaceResultPair p1, RaceResultPair p2) {
						return p1.getRace() - p2.getRace();
					}
				});
				Collections.sort(raceResultsRhs, new Comparator<RaceResultPair>() {
					public int compare(RaceResultPair p1, RaceResultPair p2) {
						return p1.getRace() - p2.getRace();
					}
				});
				for (result = numRaces-1; result>=0; result--) {
					if (raceResultsLhs.get(result).getResult() == raceResultsRhs.get(result).getResult()) {
						continue;
					} else {
						break; // capture result where there is a difference
					}
				}
				result++; // Add one back on because loop counter will have decremented below 0 before exit
				// Now we found a break in the tie or reached the beginning again
				if (result != 0) {
					return (raceResultsLhs.get(result).getResult() < raceResultsRhs.get(result).getResult() ? -1 : 1);
				} else {
					// If result is 0 then we have somehow made it all the way back to the beginning and still not
					// broken the tie.
					// This could only happen if more than one competitor had no results at all.
					return 0;
				}
			}
		}
	}
	*/

}
