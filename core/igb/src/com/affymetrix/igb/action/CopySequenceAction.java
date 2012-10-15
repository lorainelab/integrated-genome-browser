/*
 * Copy the original whole sequence in sequence viewer
 */
package com.affymetrix.igb.action;

import com.affymetrix.igb.view.AbstractSequenceViewer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author fwang4
 */
public class CopySequenceAction extends CopyResiduesAction{
	
	public CopySequenceAction(String text, AbstractSequenceViewer sv) {
		super(text, sv);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if(sv != null) {
			sv.copyWholeSeqAction();
		}
	}
}
