package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

 public class UserGroupComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
      UserGroup g1 = (UserGroup)o1;
      UserGroup g2 = (UserGroup)o2;
      
      
      return g1.getIdUserGroup().compareTo(g2.getIdUserGroup());
      
    }
  }
