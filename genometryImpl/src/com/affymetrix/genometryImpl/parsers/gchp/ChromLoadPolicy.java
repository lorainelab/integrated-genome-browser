package com.affymetrix.genometryImpl.parsers.gchp;

import java.util.ArrayList;
import java.util.List;

public abstract class ChromLoadPolicy {
  
  public ChromLoadPolicy() {}

  public abstract boolean shouldLoadChrom(int chromNum);
  
  static final ChromLoadPolicy LOAD_ALL = new ChromLoadPolicy() {
    @Override
    public boolean shouldLoadChrom(int chromNum) {
      return true;
    }
  };
  
  static final ChromLoadPolicy LOAD_NOTHING = new ChromLoadPolicy() {
    @Override
    public boolean shouldLoadChrom(int chromNum) {
      return false;
    }
  };
  
  public static ChromLoadPolicy getLoadAllPolicy() {
    return LOAD_ALL;
  }

  public static ChromLoadPolicy getLoadNothingPolicy() {
    return LOAD_NOTHING;
  }
  
  public static ChromLoadPolicy getLoadListedChromosomesPolicy(List<Integer> list) {
    final List<Integer> chromList = new ArrayList<Integer>(list);
    return new ChromLoadPolicy() {
      @Override 
      public boolean shouldLoadChrom(int chromNum) {
        return chromList.contains(chromNum);  
      }
    };
  }
}