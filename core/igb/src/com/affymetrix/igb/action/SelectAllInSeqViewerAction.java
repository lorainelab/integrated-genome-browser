package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.view.AbstractSequenceViewer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class SelectAllInSeqViewerAction extends GenericAction{
	private static final SelectAllInSeqViewerAction ACTION = new SelectAllInSeqViewerAction(BUNDLE.getString("selectAll"), null);
	
	AbstractSequenceViewer sv = null;
	
	public SelectAllInSeqViewerAction(String text, AbstractSequenceViewer sv) {
		super(text, null, null, null, KeyEvent.VK_A);
		this.sv = sv;
	}
	
	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SelectAllInSeqViewerAction getAction () {
		return ACTION;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if(sv != null) {
			sv.selectAll();
		}
	}
}
