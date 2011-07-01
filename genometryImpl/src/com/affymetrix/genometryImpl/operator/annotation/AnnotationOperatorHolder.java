package com.affymetrix.genometryImpl.operator.annotation;

import java.util.ArrayList;
import java.util.List;

public class AnnotationOperatorHolder {
	private static AnnotationOperatorHolder instance = new AnnotationOperatorHolder();
	private AnnotationOperatorHolder() {
		super();
	}
	public static AnnotationOperatorHolder getInstance() {
		return instance;
	}
	private List<AnnotationOperator> annotationOperators = new ArrayList<AnnotationOperator>();

	public void addAnnotationOperator(AnnotationOperator annotationOperator) {
		annotationOperators.add(annotationOperator);
	}

	public void removeAnnotationOperator(AnnotationOperator annotationOperator) {
		annotationOperators.remove(annotationOperator);
	}

	public List<AnnotationOperator> getAnnotationOperators() {
		return annotationOperators;
	}
}
