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

package com.affymetrix.genometryImpl.event;

import java.util.*;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;

public final class SeqSelectionEvent extends EventObject {
	List<MutableAnnotatedBioSeq> selected_seqs;
	MutableAnnotatedBioSeq primary_selection = null;
	static final long serialVersionUID = 1L;

	/**
	 *  Constructor.
	 *  @param src The source of the event.
	 *  @param seqs a List of AnnotatedBioSeq's that have been selected.
	 *   (If null, will default to {@link Collections#EMPTY_LIST}.)
	 */
	public SeqSelectionEvent(Object src, List<MutableAnnotatedBioSeq> seqs) {
		super(src);
		this.selected_seqs = seqs;
		if (selected_seqs == null) { selected_seqs = Collections.<MutableAnnotatedBioSeq>emptyList(); }
		if (selected_seqs.size() > 0) {
			primary_selection = selected_seqs.get(0);
		}
	}

	public SeqSelectionEvent(Object src, MutableAnnotatedBioSeq seq) {
		super(src);
		selected_seqs = new ArrayList<MutableAnnotatedBioSeq>(1);
		if (seq != null) {
			primary_selection = seq;
			selected_seqs.add(seq);
		}
	}

	/**
	 *  @return a List of AnnotatedBioSeq's that have been selected.
	 *   The List can be empty, but will not be null.
	 */
	public List getSelectedSeqs() {
		return selected_seqs;
	}

	/** Gets the first entry in the list {@link #getSelectedSeqs()}.
	 *  @return an MutableAnnotatedBioSeq or null.
	 */
	public MutableAnnotatedBioSeq getSelectedSeq() {
		return primary_selection;
	}

	@Override
		public String toString() {
			return "SeqSelectionEvent: seq count: " + selected_seqs.size() +
				" first seq: '" + (primary_selection == null ? "null" : primary_selection.getID()) +
				"' source: " + this.getSource();
		}
}
