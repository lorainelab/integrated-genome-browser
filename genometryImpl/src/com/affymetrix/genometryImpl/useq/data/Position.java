package com.affymetrix.genometryImpl.useq.data;

/** @author david.nix@hci.utah.edu*/
public class Position implements Comparable {
	//fields
	protected int position;

	//constructors
	public Position (int position){
		this.position = position;
	}

	//methods
	public String toString(){
		return Integer.toString(position);
	}

	/**Sorts by position base, smaller to larger.*/
	public int compareTo(Object other){
		Position se = (Position)other;
		if (position<se.position) return -1;
		if (position>se.position) return 1;
		return 0;
	}

	//getters and setters
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public boolean isContainedBy(int beginningBP, int endingBP) {
		if (position >= beginningBP && position < endingBP) return true;
		return false;
	}


}
