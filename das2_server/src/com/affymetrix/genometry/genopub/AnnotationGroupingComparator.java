package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

import com.affymetrix.genometry.genopub.AnnotationGrouping;

 public class AnnotationGroupingComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
      AnnotationGrouping ag1 = (AnnotationGrouping)o1;
      AnnotationGrouping ag2 = (AnnotationGrouping)o2;
      
      
      return ag1.getIdAnnotationGrouping().compareTo(ag2.getIdAnnotationGrouping());
      
    }
  }
