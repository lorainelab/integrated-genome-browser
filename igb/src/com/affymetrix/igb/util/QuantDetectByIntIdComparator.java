package com.affymetrix.igb.util;

import java.util.Comparator;
import affymetrix.calvin.data.ProbeSetQuantificationDetectionData;

/**
 *  Assumes objects to compare are both ProbeSetQuantificationDetectionData object, and 
 *     they have their integer IDs set
 */
public class QuantDetectByIntIdComparator implements Comparator {
  public int compare(Object objA, Object objB) {
    ProbeSetQuantificationDetectionData dataA = (ProbeSetQuantificationDetectionData)objA;
    ProbeSetQuantificationDetectionData dataB = (ProbeSetQuantificationDetectionData)objB;
    int idA = dataA.getId();
    int idB = dataB.getId();
    if (idA < idB) { return -1; }
    else if (idA > idB) { return 1; }
    else {  return 0; }
  }
}
