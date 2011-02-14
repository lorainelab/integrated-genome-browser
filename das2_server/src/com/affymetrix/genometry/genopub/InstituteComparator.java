package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

 public class InstituteComparator implements Comparator<Institute>, Serializable {
    public int compare(Institute i1, Institute i2) {
      return i1.getIdInstitute().compareTo(i2.getIdInstitute());
    }
  }
