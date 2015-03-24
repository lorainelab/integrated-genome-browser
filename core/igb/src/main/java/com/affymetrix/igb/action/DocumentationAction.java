package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.GeneralUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author sgblanch
 * @version $Id: DocumentationAction.java 11358 2012-05-02 13:28:22Z anuj4159 $
 */
public class DocumentationAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final DocumentationAction ACTION = new DocumentationAction();
    private final int TOOLBAR_INDEX = 8;

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static DocumentationAction getAction() {
        return ACTION;
    }

    private DocumentationAction() {
        super(BUNDLE.getString("documentation"), BUNDLE.getString("goToIGBUserGuideTooltip"),
                "16x16/actions/documentation.png",
                "22x22/actions/documentation.png",
                KeyEvent.VK_D, null, false);
        this.ordinal = 110;
        setKeyStrokeBinding("F1");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralUtils.browse("http://wiki.transvar.org/confluence/display/igbman");
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
