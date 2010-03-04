package com.affymetrix.igb.event;

import com.affymetrix.genometryImpl.BioSeq;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public interface ViewSeqListener {
	public void viewSeqChanged(BioSeq seq);
}
