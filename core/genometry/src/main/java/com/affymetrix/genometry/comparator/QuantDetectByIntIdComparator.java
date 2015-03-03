package com.affymetrix.genometry.comparator;

import affymetrix.calvin.data.ProbeSetQuantificationDetectionData;
import java.util.Comparator;

/**
 * Assumes objects to compare are both ProbeSetQuantificationDetectionData object, and
 * they have their integer IDs set
 */
public final class QuantDetectByIntIdComparator implements Comparator<ProbeSetQuantificationDetectionData> {

    public int compare(ProbeSetQuantificationDetectionData dataA, ProbeSetQuantificationDetectionData dataB) {
        int idA = dataA.getId();
        int idB = dataB.getId();
        return Integer.valueOf(idA).compareTo(idB);
    }
}
