/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author auser
 */
public class ViewGenomicSequenceInSeqViewerAction  extends AbstractAction{
	private static final long serialVersionUID = 1l;
	public static ViewGenomicSequenceInSeqViewerAction singleton = new ViewGenomicSequenceInSeqViewerAction();
	private ViewGenomicSequenceInSeqViewerAction() {
		super(BUNDLE.getString("ViewGenomicSequenceInSeqViewer"));
	}

	public static ViewGenomicSequenceInSeqViewerAction getAction(){
		return singleton;
	}

	public void actionPerformed(ActionEvent e) {
		try {
			SequenceViewer sv = new SequenceViewer();
			sv.startSequenceViewer();
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex);
		}
	}

}
