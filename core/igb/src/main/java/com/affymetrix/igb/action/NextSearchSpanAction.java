package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import java.awt.event.ActionEvent;

public class NextSearchSpanAction extends GenericAction {

    private static final long serialVersionUID = 1L;
//	private static final NextSearchSpanAction ACTION = new NextSearchSpanAction();
//
//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
//	
//	public static NextSearchSpanAction getAction() {
//		return ACTION;
//	}

    private NextSearchSpanAction() {
        super(IGBConstants.BUNDLE.getString("nextSearchSpan"), "16x16/actions/go-last.png", "22x22/actions/go-last.png");
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        IGB.getSingleton().getMapView().getMapRangeBox().nextSpan();
    }
}
