package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

import com.affymetrix.genometry.genopub.Segment;

 public class SegmentComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
    	Segment s1 = (Segment)o1;
    	Segment s2 = (Segment)o2;
      
      
      return s1.getIdSegment().compareTo(s2.getIdSegment());
      
    }
  }
