package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

public class GenomeVersionComparator implements Comparator, Serializable {
	public int compare(Object o1, Object o2) {
		GenomeVersion v1 = (GenomeVersion)o1;
		GenomeVersion v2 = (GenomeVersion)o2;


		if (v1.getBuildDate() != null && v2.getBuildDate() != null) {
			return v2.getBuildDate().compareTo(v1.getBuildDate());
		} else if (v1.getBuildDate() != null) {
			return 1;
		} else if (v2.getBuildDate() != null) {
			return 2;
		} else {
			return v1.getName().compareTo(v2.getName());
		}

	}
}
