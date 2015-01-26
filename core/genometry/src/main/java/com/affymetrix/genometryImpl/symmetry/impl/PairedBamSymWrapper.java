/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.TypedSym;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dcnorris
 */
public final class PairedBamSymWrapper implements TypedSym, SymWithProps, MultiTierSymWrapper {

    private static final int FIRST_CHILD = 0;
    private static final int SECOND_CHILD = 1;
    private BAMSym forwardStrandSym;
    private BAMSym negativeStrandSym;
    private Map<String, Object> props;

    public PairedBamSymWrapper(BAMSym forwardStrandSym, BAMSym negativeStrandSym) {
        this.forwardStrandSym = forwardStrandSym;
        this.negativeStrandSym = negativeStrandSym;
    }

    @Override
    public String getID() {
        return forwardStrandSym.getName();
    }

    public String getName() {
        return forwardStrandSym.getName();
    }

    @Override
    public int getSpanCount() {
        return 1;
    }

    @Override
    public SeqSpan getSpan(BioSeq seq) {
        if (forwardStrandSym.getBioSeq().equals(seq)) {
            SimpleSeqSpan seqSpan = new SimpleSeqSpan(forwardStrandSym.getMin(), negativeStrandSym.getMax(), forwardStrandSym.getBioSeq());
            return seqSpan;
        }
        return null;
    }

    @Override
    public SeqSpan getSpan(int index) {
        SimpleSeqSpan seqSpan = new SimpleSeqSpan(forwardStrandSym.getMin(), negativeStrandSym.getMax(), forwardStrandSym.getBioSeq());
        return seqSpan;
    }

    @Override
    public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getSpan(int index, MutableSeqSpan span) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BioSeq getSpanSeq(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getChildCount() {
        return CHILD_COUNT;
    }

    @Override
    public SeqSymmetry getChild(int index) {
        if (index == 0) {
            return forwardStrandSym;
        } else if (index == 1) {
            return negativeStrandSym;
        }
        throw new IndexOutOfBoundsException();
    }

    public BAMSym getForwardStrandSym() {
        return forwardStrandSym;
    }

    public void setForwardStrandSym(BAMSym forwardStrandSym) {
        this.forwardStrandSym = forwardStrandSym;
    }

    public BAMSym getNegativeStrandSym() {
        return negativeStrandSym;
    }

    public void setNegativeStrandSym(BAMSym negativeStrandSym) {
        this.negativeStrandSym = negativeStrandSym;
    }

    @Override
    public String getType() {
        return forwardStrandSym.getType();
    }

    @Override
    public Map<String, Object> getProperties() {
        return cloneProperties();
    }

    @Override
    public Map<String, Object> cloneProperties() {
        HashMap<String, Object> tprops = new HashMap<>();
        tprops.put("type", getType());
        tprops.put("name", getName());
        if (props != null) {
            tprops.putAll(props);
        }
        return tprops;
    }

    @Override
    public Object getProperty(String key) {
        if (props == null) {
            props = cloneProperties();
        }
        return props.get(key);

    }

    @Override
    public boolean setProperty(String name, Object val) {
        if (props == null) {
            props = cloneProperties();
        }
        props.put(name, val);
        return true;
    }

    public SeqSymmetry getPairConnector() {
        PairedConnectorSym toReturn;
        int min = getForwardStrandSym().getMax();
        int max = getNegativeStrandSym().getMin();
        toReturn = new PairedConnectorSym(getType(), getForwardStrandSym().getBioSeq(), min+1, max-1, getName(), true, null, null) {
            @Override
            public SeqSymmetry getChild(int index) {
                return null;
            }
        };

        return toReturn;
    }

    @Override
    public SeqSymmetry getFirstChild() {
        return getChild(FIRST_CHILD);
    }

    @Override
    public SeqSymmetry getSecondChild() {
        return getChild(SECOND_CHILD);
    }

}
