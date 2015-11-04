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

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypehandlerRegistry;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.SupportsGeneName;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.TypedSym;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Top-level annots attached to a BioSeq.
 */
public final class TypeContainerAnnot extends RootSeqSymmetry implements TypedSym {

    private static final FileTypeCategory DEFAULT_CATEGORY = FileTypeCategory.Annotation;
    private TreeMap<String, Set<SeqSymmetry>> id2sym_hash;	// list of names -> sym
    private final String ext;
    private final boolean index;
    private final String type;

    public TypeContainerAnnot(String type) {
        this(type, "", false);
    }

    public TypeContainerAnnot(String type, String ext, boolean index) {
        super();
        this.setProperty("method", type);
        this.setProperty(CONTAINER_PROP, Boolean.TRUE);
        this.type = type;
        this.ext = ext;
        this.index = index;
        id2sym_hash = !index ? null : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public String getType() {
        return type;
    }

    @Override
    public FileTypeCategory getCategory() {
        FileTypeCategory category = null;
        FileTypeHandler handler = FileTypehandlerRegistry.getFileTypeHolder().getFileTypeHandler(ext);
        if (handler != null) {
            category = handler.getFileTypeCategory();
        }
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }
        return category;
    }

    @Override
    public void addChild(SeqSymmetry sym) {
        super.addChild(sym);
        if (index) {
            addToIndex(sym.getID(), sym);
            indexGeneName(sym);
            if (sym instanceof GFF3Sym) {
                int childCount = sym.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    addToIndex(sym.getChild(i).getID(), sym.getChild(i));
                    indexGeneName(sym);
                }
            }
        }
    }

    private void indexGeneName(SeqSymmetry sym) {
        if (sym instanceof SupportsGeneName) {
            addToIndex(((SupportsGeneName) sym).getGeneName(), sym);
        }
    }

    private void addToIndex(String key, SeqSymmetry sym) {
        if (key != null && key.length() > 0) {
            Set<SeqSymmetry> seq_list = id2sym_hash.get(key);
            if (seq_list == null) {
                seq_list = new LinkedHashSet<>();
                id2sym_hash.put(key, seq_list);
            }
            seq_list.add(sym);
        }
    }

    @Override
    public void search(Set<SeqSymmetry> results, String id) {
        if (id2sym_hash == null || id == null) {
            return;
        }
        Set<SeqSymmetry> sym_list = id2sym_hash.get(id);
        if (sym_list != null) {
            results.addAll(sym_list);
        }
    }

    @Override
    public void searchHints(Set<String> results, Pattern regex, int limit) {
        if (id2sym_hash == null || regex == null) {
            return;
        }
        final Matcher matcher = regex.matcher("");
        int size = Math.min(limit, id2sym_hash.size());
        int count = results.size();

        for (String key : id2sym_hash.keySet()) {
            matcher.reset(key);
            if (matcher.matches()) {
                results.add(key);

                count++;
                if (count > size) {
                    break;
                }
            }
        }
    }

    @Override
    public void search(Set<SeqSymmetry> results, Pattern regex, int limit) {
        if (id2sym_hash == null || regex == null) {
            return;
        }
        int size;
        int count;
        if (limit > 0) {
            size = Math.min(limit, id2sym_hash.size());
            count = results.size();
        } else {
            size = -1;
            count = Integer.MIN_VALUE;
        }
        final Matcher matcher = regex.matcher("");
        Thread current_thread = Thread.currentThread();
        for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
            if (current_thread.isInterrupted() || count > size) {
                break;
            }

            String seid = ent.getKey();
            Set<SeqSymmetry> val = ent.getValue();
            if (seid != null && val != null) {
                matcher.reset(seid);
                if (matcher.matches()) {
                    results.addAll(val);
                    count++;
                }
            }
        }
    }

    @Override
    public void searchProperties(Set<SeqSymmetry> results, Pattern regex, int limit) {
        if (id2sym_hash == null || regex == null) {
            return;
        }
        int size;
        int count;
        if (limit > 0) {
            size = Math.min(limit, id2sym_hash.size());
            count = results.size();
        } else {
            size = -1;
            count = Integer.MIN_VALUE;
        }

        final Matcher matcher = regex.matcher("");
        SymWithProps swp;
        String match;
        Thread current_thread = Thread.currentThread();
        for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
            if (current_thread.isInterrupted()) {
                break;
            }

            for (SeqSymmetry seq : ent.getValue()) {
                if (current_thread.isInterrupted()) {
                    break;
                }

                if (seq instanceof SymWithProps) {
                    swp = (SymWithProps) seq;

                    // Iterate through each properties.
                    if (swp.getProperties() != null) {
                        for (Map.Entry<String, Object> prop : swp.getProperties().entrySet()) {
                            if (current_thread.isInterrupted() || count > size) {
                                break;
                            }

                            if (prop.getValue() != null) {
                                match = prop.getValue().toString();
                                matcher.reset(match);
                                if (matcher.matches()) {
                                    results.add(seq);
                                    count++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        id2sym_hash = null;
    }
}
