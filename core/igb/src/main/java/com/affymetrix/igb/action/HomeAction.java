package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenomeVersionSelectionEvent;
import com.affymetrix.igb.view.SeqGroupView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

@Component(name = HomeAction.COMPONENT_NAME, immediate = true, provide = GenericAction.class)
public class HomeAction extends GenericAction {

    public static final String COMPONENT_NAME = "HomeAction";
    private static final long serialVersionUID = 1L;
    private final int TOOLBAR_INDEX = 17;

    public HomeAction() {
        super("Home", "Home", "16x16/actions/home_updated.png",
                "22x22/actions/home_updated.png", KeyEvent.VK_H);
        setKeyStrokeBinding("ctrl H");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GenometryModel.getInstance().setSelectedGenomeVersion(null);
        GenometryModel.getInstance().setSelectedSeq(null);
        SeqGroupView.getInstance().getSpeciesCB().setSelectedItem(SeqGroupView.SELECT_SPECIES);
        SeqGroupView.getInstance().getVersionCB().setSelectedItem(SeqGroupView.SELECT_GENOME);
        SeqGroupView.getInstance().groupSelectionChanged(new GenomeVersionSelectionEvent(this, null));
    }

    @Override
    public boolean isToolbarDefault() {
        return true;
    }

    @Override
    public int getToolbarIndex() {
        return TOOLBAR_INDEX;
    }
}
