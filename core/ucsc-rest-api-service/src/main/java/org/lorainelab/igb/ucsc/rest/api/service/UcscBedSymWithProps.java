package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;

import java.util.Hashtable;
import java.util.Map;

public class UcscBedSymWithProps extends UcscBedSym {
    /**
     * Constructs a SeqSymmetry optimized for BED-file format. This object is optimized for the case where all optional
     * columns in the bed file are used. If you are using only the first few columns, it would be more efficient to use
     * a different SeqSymmetry object.
     *
     * @param type
     * @param seq
     * @param txMin
     * @param txMax
     * @param name
     * @param score     an optional score, or Float.NEGATIVE_INFINITY to indicate no score.
     * @param forward
     * @param cdsMin    the start of the CDS region, "thinEnd", or Integer.MIN_VALUE. If cdsMin = Integer.MIN_VALUE or
     *                  cdsMin = cdsMax, then there is no CDS.
     * @param cdsMax    the end of the CDS region, "thickEnd", or Integer.MIN_VALUE.
     * @param blockMins
     * @param blockMaxs
     */
    public UcscBedSymWithProps(String type, BioSeq seq, int txMin, int txMax, String name, float score,
                               boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs,
                               Map<String, Object> props) {
        super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs);
        if (props != null) {
            if(this.props != null)
                this.props.putAll(props);
            else
                this.props = new Hashtable<>(props);
        }
    }

    public Map<String, Object> getProperties() {
        return cloneProperties();
    }

    public Map<String, Object> cloneProperties() {
        Map<String, Object> tprops = super.cloneProperties();
        if (props != null) {
            tprops.putAll(props);
        }
        return tprops;
    }

    public Object getProperty(String key) {
        if (super.getProperty(key) != null) {
            return super.getProperty(key);
        }
        else if(props != null){
            return props.get(key);
        }
        return null;
    }

}
