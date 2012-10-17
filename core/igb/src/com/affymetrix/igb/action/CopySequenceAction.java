/*
 * Copy the whole sequence in sequence viewer
 */
package com.affymetrix.igb.action;

import com.affymetrix.igb.view.AbstractSequenceViewer;
import java.awt.event.ActionEvent;

public class CopySequenceAction extends CopyResiduesAction{
	
	public CopySequenceAction(String text, AbstractSequenceViewer sv) {
		super(text, sv);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if(sv != null) {
			sv.copyAction(true);
		}
	}
}
