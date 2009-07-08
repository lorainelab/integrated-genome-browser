package com.affymetrix.genometryImpl.util;

import java.util.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

public final class GenomeVersionDateComparator implements Comparator<AnnotatedSeqGroup> {
	static String[] month_array = { "Jan",
		"Feb",
		"Mar",
		"Apr",
		"May",
		"Jun",
		"Jul",
		"Aug",
		"Sep",
		"Oct",
		"Nov",
		"Dec" };
	static List months = Arrays.asList(month_array);

	public int compare(AnnotatedSeqGroup group1, AnnotatedSeqGroup group2) {
		String name1 = group1.getID();
		String name2 = group2.getID();
		String[] parts1 = name1.split("_");
		String[] parts2 = name2.split("_");
		int count1 = parts1.length;
		int count2 = parts2.length;
		String yearA = parts1[count1-1];
		String yearB = parts2[count2-1];
		int year1 = -1;
		int year2 = -1;
		try {
			year1 = Integer.parseInt(yearA);
		} catch (Exception ex) { }
		try {
			year2 = Integer.parseInt(yearB);
		} catch (Exception ex) { }
		if (year1 == -1 && year2 == -1) {
			// if neither parses as an integer, then they are considered equal for sorting
			return 0;
		}
		else if (year1 == -1) { return 1; } 
		else if (year2 == -1) { return -1; }
		// want to sort so more recent years are sorted to top
		else if (year1 > year2) { return -1; }  // year1 is more recent
		else if (year2 > year1) { return 1; }   // year2 is more recent
		else {  // year part is same for both group IDs
			// therefore can't determine order from year part, trying month part
			String monthA = parts1[count1-2];
			String monthB = parts2[count2-2];
			int month1 = months.indexOf(monthA);
			int month2 = months.indexOf(monthB);
			if (month1 == -1 && month2 == -1) { return 0; }
			else if (month1 == -1) { return 1; }
			else if (month2 == -1) { return -1; }
			else if (month1 > month2) { return -1; } // month1 is more recent
			else if (month2 > month1) { return 1; } // month2 is more recent
			else {
				return 0;  // year and month are same, considered equivalent for sorting
			}
		}
	}


}
