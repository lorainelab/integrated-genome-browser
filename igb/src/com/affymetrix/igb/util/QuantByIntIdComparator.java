package com.affymetrix.igb.util;

import java.util.Comparator;
import affymetrix.calvin.data.ProbeSetQuantificationData;

/**
 *  Assumes objects to compare are both ProbeSetQuantificationData objects, and 
 *     they have their integer IDs set
 */
public class QuantByIntIdComparator implements Comparator<ProbeSetQuantificationData> {
  public int compare(ProbeSetQuantificationData dataA, ProbeSetQuantificationData dataB) {
    int idA = dataA.getId();
    int idB = dataB.getId();
    if (idA < idB) { return -1; }
    else if (idA > idB) { return 1; }
    else {  return 0; }
  }
}
