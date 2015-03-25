/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.SymWithProps;
import java.util.Hashtable;
import java.util.Map;

public class SingletonSymWithProps extends MutableSingletonSeqSymmetry
        implements SymWithProps {

    Map<String, Object> props;

    public SingletonSymWithProps(int start, int end, BioSeq seq) {
        super(start, end, seq);
    }

    public SingletonSymWithProps(CharSequence id, int start, int end, BioSeq seq) {
        super(id, start, end, seq);
    }

    /**
     * Returns the properties map, or null.
     */
    public Map<String, Object> getProperties() {
        return props;
    }

    @Override
    public String getID() {
        if (id != null) {
            return id.toString();
        }
        if (props != null) {
            return (String) props.get("id");
        }
        return null;
    }

    /**
     * Creates a clone of the properties Map. Uses the same type of Map class
     * (HashMap, TreeMap, etc.) as the original.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cloneProperties() {
        if (props == null) {
            return null;
        }
        try {
            Map<String, Object> newprops = props.getClass().newInstance();
            newprops.putAll(props);
            return newprops;
        } catch (Exception ex) {
            System.out.println("problem trying to clone SingletonSymWithProps properties, "
                    + "returning null instead");
            return null;
        }
    }

    public boolean setProperty(String name, Object val) {
        if (props == null) {
            props = new Hashtable<>();
        }
        props.put(name, val);
        return true;
    }

    public Object getProperty(String name) {
        if (props == null) {
            return null;
        }
        return props.get(name);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }
}
