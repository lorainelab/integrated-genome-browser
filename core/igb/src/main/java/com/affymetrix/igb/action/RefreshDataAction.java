package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;

/**
 *
 * @author sgblanch
 * @version $Id: RefreshDataAction.java 11329 2012-05-01 17:18:29Z anuj4159 $
 */
public class RefreshDataAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final RefreshDataAction ACTION = new RefreshDataAction();

    public RefreshDataAction(JComponent comp) {
        super(BUNDLE.getString("refreshDataButton"), BUNDLE.getString("refreshDataTip"), "toolbarButtonGraphics/general/Refresh16.gif", null, KeyEvent.VK_UNDEFINED);
//		KeyStroke ks = MenuUtil.addAccelerator(comp, this, getId());
//		if (ks != null) {
//			this.putValue(MNEMONIC_KEY, ks.getKeyCode());
//		}
//	}
    }

    public RefreshDataAction() {
        super(BUNDLE.getString("refreshDataButton"), BUNDLE.getString("refreshDataTip"), "toolbarButtonGraphics/general/Refresh16.gif", null, KeyEvent.VK_UNDEFINED);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static RefreshDataAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        super.actionPerformed(ae);
        GeneralLoadView.getLoadView().setShowLoadingConfirm(true);
        GeneralLoadView.getLoadView().loadVisibleFeatures();
    }
}
