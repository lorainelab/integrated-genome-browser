package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class NewSymLoadedEvent extends EventObject{
	private static final long serialVersionUID = 1L;
	private final List<? extends SeqSymmetry> new_syms;
	private final BioSeq seq;
	
	/**
	 *  Constructs a NewSymLoadedEvent.
	 *  @param seq BioSeq on which syms were loaded;
	 *  @param syms a List of SeqSymmetry's.  Can be empty, but should not be null.
	 *   (If null, will default to {@link Collections#EMPTY_LIST}.)
	 */
	public NewSymLoadedEvent(Object src, BioSeq seq, List<? extends SeqSymmetry> syms) {
		super(src);
		this.seq = seq;
		if (syms == null) {
			this.new_syms = Collections.<SeqSymmetry>emptyList();
		} else {
			this.new_syms = syms;
		}
	}

	/** @return a List of SeqSymmetry's.  May be empty, but will not be null.
	*/
	public List<? extends SeqSymmetry> getNewSyms() {
		return new_syms;
	}
	
	/**
	 * @return BioSeq on which new syms were loaded
	 */
	public BioSeq getSeq(){
		return seq;
	}
}
