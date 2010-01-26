package com.affymetrix.genometryImpl.event;

import java.util.EventObject;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

/**
 *  Events used to let listeners know about changes in the
 *  ID-to-Symmetry Mapping in an AnnotatedSeqGroup.
 */
public final class SymMapChangeEvent extends EventObject {
	transient AnnotatedSeqGroup group;
	static final long serialVersionUID = 1L;

	public SymMapChangeEvent(Object src, AnnotatedSeqGroup seq_group) {
		super(src);
		this.group = seq_group;
	}
}
