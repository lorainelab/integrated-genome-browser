package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

import com.affymetrix.genometry.genopub.User;

 public class UserComparator implements Comparator, Serializable {
    public int compare(Object o1, Object o2) {
      User u1 = (User)o1;
      User u2 = (User)o2;
      
      
      return u1.getIdUser().compareTo(u2.getIdUser());
      
    }
  }
