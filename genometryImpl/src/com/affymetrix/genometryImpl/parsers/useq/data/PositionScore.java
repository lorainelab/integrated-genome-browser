package com.affymetrix.genometryImpl.parsers.useq.data;
import java.io.*;

/** @author david.nix@hci.utah.edu*/
public class PositionScore extends Position {
	//fields
	protected float score;

	//constructors
	public PositionScore (int position, float score){
		super(position);
		this.score = score;
	}
	
	public String toString(){
		return position+"\t"+score;
	}
	
	public float getScore() {
		return score;
	}
	
	public void setScore(float score) {
		this.score = score;
	}
	/**Sorts by position base, smaller to larger.*/
	public int compareTo(Object other){
		PositionScore se = (PositionScore)other;
		if (position<se.position) return -1;
		if (position>se.position) return 1;
		return 0;
	}
}
