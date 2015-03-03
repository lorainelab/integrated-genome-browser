package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author sgblanch
 * @version $Id: ClampViewAction.java 11335 2012-05-01 18:00:52Z anuj4159 $
 */
public class ClampViewAction extends SeqMapViewActionA{
    public static final String COMPONENT_NAME = "ClampViewAction";
    private static final long serialVersionUID = 1l;
    private static final ClampViewAction ACTION = new ClampViewAction();

    private ClampViewAction() {
        super(BUNDLE.getString("clampToView"), null, "16x16/actions/Clamp_to_view.png", "22x22/actions/Clamp_to_view.png", KeyEvent.VK_V);
        this.putValue(SELECTED_KEY, false);
    }

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ClampViewAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        getSeqMapView().toggleHorizontalClamp();
    }

    @Override
    public boolean isToggle() {
        return true;
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
