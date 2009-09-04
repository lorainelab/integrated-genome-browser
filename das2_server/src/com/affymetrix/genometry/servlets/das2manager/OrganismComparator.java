package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

public class OrganismComparator implements Comparator, Serializable {
	public int compare(Object o1, Object o2) {
		Organism org1 = (Organism)o1;
		Organism org2 = (Organism)o2;


		return org1.getBinomialName().compareTo(org2.getBinomialName());
		
	}
}
