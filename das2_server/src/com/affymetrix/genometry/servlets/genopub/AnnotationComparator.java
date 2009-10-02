package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

import com.affymetrix.genometry.genopub.Annotation;

public class AnnotationComparator implements Comparator, Serializable {
	public int compare(Object o1, Object o2) {
		Annotation a1 = (Annotation)o1;
		Annotation a2 = (Annotation)o2;


		return a1.getIdAnnotation().compareTo(a2.getIdAnnotation());

	}
}
