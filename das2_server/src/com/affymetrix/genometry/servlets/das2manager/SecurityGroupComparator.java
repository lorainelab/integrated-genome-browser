package com.affymetrix.genometry.servlets.das2manager;


import java.io.Serializable;
import java.util.Comparator;

 public class SecurityGroupComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
      SecurityGroup g1 = (SecurityGroup)o1;
      SecurityGroup g2 = (SecurityGroup)o2;
      
      
      return g1.getIdSecurityGroup().compareTo(g2.getIdSecurityGroup());
      
    }
  }
