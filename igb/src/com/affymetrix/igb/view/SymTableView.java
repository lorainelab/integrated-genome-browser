/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import java.util.*;

import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.*;
import java.text.NumberFormat;

public final class SymTableView extends PropertySheet implements SymSelectionListener {

    static int testcount = 0;
    List<SeqSymmetry> currentSyms = Collections.<SeqSymmetry>emptyList();

    public SymTableView() {
        this(true, true);
    }

    public SymTableView(boolean useDefaultKeystrokes) {
        this(useDefaultKeystrokes, true);
    }

    public SymTableView(boolean useDefaultKeystrokes, boolean isSymSelectionListener) {
        super(useDefaultKeystrokes);
        setPreferredSize(new java.awt.Dimension(100, 250));
        setMinimumSize(new java.awt.Dimension(100, 250));
        if (isSymSelectionListener) {
            SingletonGenometryModel.getGenometryModel().addSymSelectionListener(this);
        }
    }

    /*
    public void setDefaultColumnOrder(List<String> columns) {
        default_order = new Vector<String>(columns);
    }*/

    public void symSelectionChanged(SymSelectionEvent evt) {
        Object src = evt.getSource();
        // if selection event originally came from here, then ignore it...
        if (src == this) {
            return;
        }
        List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
        SeqMapView mapView = null;
        if (src instanceof SeqMapView) {
            mapView = (SeqMapView) src;
        }
        showSyms(selected_syms, mapView);
    }

    private void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap) {
        currentSyms = selected_syms;

        int symCount = selected_syms.size();
        Vector<Map<String, Object>> propvec = new Vector<Map<String, Object>>();
        for (int i = 0; i < symCount; i++) {
            SeqSymmetry sym = selected_syms.get(i);
            Map<String, Object> props = determineProps(sym, seqMap);
            testcount++;
            propvec.add(props);
        }
        Map[] prop_array = propvec.toArray(new Map[propvec.size()]);

        Vector<String> prop_order = determineOrder();
        this.showProperties(prop_array, prop_order, "");
    }

    private static Map<String, Object> determineProps(SeqSymmetry sym, SeqMapView seqMap) {
        Map<String, Object> props = null;
        if (sym instanceof SymWithProps) {
            // using Propertied.cloneProperties() here instead of Propertied.getProperties()
            //   because adding start, end, id, and length as additional key-val pairs to props Map
            //   and don't want these to bloat up sym's properties
            props = ((SymWithProps) sym).cloneProperties();
        }
        if (props == null && sym instanceof DerivedSeqSymmetry) {
            SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
            if (original_sym instanceof SymWithProps) {
                props = ((SymWithProps) original_sym).cloneProperties();
            }
        }
        if (props == null) {
            // make an empty hashtable if sym has no properties...
            props = new Hashtable<String, Object>();
        }
        String symid = sym.getID();
        if (symid != null) {
            props.put("id", symid);
        }
        if (seqMap != null) {
            SeqSpan span = seqMap.getViewSeqSpan(sym);
            if (span != null) {
                String chromID = span.getBioSeq().getID();
                props.put("chromosome", chromID);
                props.put("start",
                        NumberFormat.getIntegerInstance().format(span.getStart()));
                props.put("end",
                        NumberFormat.getIntegerInstance().format(span.getEnd()));
                props.put("length",
                        NumberFormat.getIntegerInstance().format(span.getLength()));
                props.remove("seq id"); // this is redundant if "chromosome" property is set
                if (props.containsKey("method") && !props.containsKey("type")) {
                    props.put("type", props.get("method"));
                    props.remove("method");
                }
            }
        }
        return props;
    }

    // The general order these fields should show up in.
    private static Vector<String> determineOrder() {
        Vector<String> prop_order;

        prop_order = new Vector<String>(18);
        prop_order.add("gene name");
        prop_order.add("name");
        prop_order.add("id");
        prop_order.add("chromosome");
        prop_order.add("start");
        prop_order.add("end");
        prop_order.add("length");
        prop_order.add("type");
        prop_order.add("same orientation");
        prop_order.add("query length");
        prop_order.add("# matches");
        prop_order.add("# target inserts");
        prop_order.add("# target bases inserted");
        prop_order.add("# query bases inserted");
        prop_order.add("# query inserts");
        prop_order.add("seq id");
        prop_order.add("cds min");
        prop_order.add("cds max");

        return prop_order;
    }
}


