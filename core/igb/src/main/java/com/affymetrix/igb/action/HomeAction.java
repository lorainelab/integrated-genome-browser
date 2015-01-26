package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.view.SeqGroupView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class HomeAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final HomeAction ACTION = new HomeAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static HomeAction getAction() {
        return ACTION;
    }

    private HomeAction() {
        super("Home", "Home", "16x16/actions/home_updated.png",
                "22x22/actions/home_updated.png", KeyEvent.VK_H);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        SeqGroupView.getInstance().getSpeciesCB().setSelectedItem(SeqGroupView.SELECT_SPECIES);
    }
}
