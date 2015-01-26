package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

/**
 *
 * @author hiralv
 */
public class LoadPartialSequenceAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final LoadPartialSequenceAction ACTION = new LoadPartialSequenceAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static LoadPartialSequenceAction getAction() {
        return ACTION;
    }

    private LoadPartialSequenceAction() {
        super(MessageFormat.format(BUNDLE.getString("load"), BUNDLE.getString("sequenceInViewCap")), null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GeneralLoadView.getLoadView().loadResidues(true);
    }
}
