package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

public class AnnotationComparator implements Comparator, Serializable {
	public int compare(Object o1, Object o2) {
		Annotation a1 = (Annotation)o1;
		Annotation a2 = (Annotation)o2;


		return a1.getIdAnnotation().compareTo(a2.getIdAnnotation());

	}
}
