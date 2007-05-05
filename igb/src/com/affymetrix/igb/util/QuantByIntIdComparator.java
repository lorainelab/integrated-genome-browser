package com.affymetrix.igb.util;

import java.util.Comparator;
import affymetrix.calvin.data.ProbeSetQuantificationData;

/**
 *  Assumes objects to compare are both ProbeSetQuantificationData objects, and 
 *     they have their integer IDs set
 */
public class QuantByIntIdComparator implements Comparator {
  public int compare(Object objA, Object objB) {
    ProbeSetQuantificationData dataA = (ProbeSetQuantificationData)objA;
    ProbeSetQuantificationData dataB = (ProbeSetQuantificationData)objB;
    int idA = dataA.getId();
    int idB = dataB.getId();
    if (idA < idB) { return -1; }
    else if (idA > idB) { return 1; }
    else {  return 0; }
  }
}
