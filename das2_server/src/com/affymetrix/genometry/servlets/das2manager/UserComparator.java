package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

 public class UserComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
      User u1 = (User)o1;
      User u2 = (User)o2;
      
      
      return u1.getIdUser().compareTo(u2.getIdUser());
      
    }
  }
