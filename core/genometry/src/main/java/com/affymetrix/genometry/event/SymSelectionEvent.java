package com.affymetrix.genometry.event;

import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public final class SymSelectionEvent extends EventObject {

    private final List<SeqSymmetry> selected_graph_syms;
    private final List<RootSeqSymmetry> all_selected_syms;
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a SymSelectionEvent.
     *
     * @param src The source of the event
     * @param all_syms a List of Graph SeqSymmetry's. Can be empty, but should not be null.
     * @param graph_syms a List of SeqSymmetry's. Can be empty, but should not be null.
     * (If null, will default to {@link Collections#EMPTY_LIST}.)
     */
    public SymSelectionEvent(Object src, List<RootSeqSymmetry> all_syms, List<SeqSymmetry> graph_syms) {
        super(src);
        if (all_syms == null) {
            this.all_selected_syms = Collections.<RootSeqSymmetry>emptyList();
        } else {
            this.all_selected_syms = all_syms;
        }
        if (graph_syms == null) {
            this.selected_graph_syms = Collections.<SeqSymmetry>emptyList();
        } else {
            this.selected_graph_syms = graph_syms;
        }
    }

    /**
     * @return a List of all selected Symmetry's. May be empty, but will not be null.
     */
    public List<RootSeqSymmetry> getAllSelectedSyms() {
        return all_selected_syms;
    }

    /**
     * @return a List of selected GraphSymmetry's. May be empty, but will not be null.
     */
    public List<SeqSymmetry> getSelectedGraphSyms() {
        return selected_graph_syms;
    }

}
