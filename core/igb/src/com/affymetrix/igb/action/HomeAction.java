package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.view.SeqGroupView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class HomeAction extends GenericAction{
	private static final long serialVersionUID = 1L;
	private static final HomeAction ACTION = new HomeAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static HomeAction getAction() {
		return ACTION;
	}

	private HomeAction() {
		super("Home Position", "Zoom out fully", "16x16/actions/go-home.png",
				"22x22/actions/go-home.png", KeyEvent.VK_UNDEFINED);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqGroupView.getInstance().getSpeciesCB().setSelectedItem(SeqGroupView.SELECT_SPECIES);
	}
}
