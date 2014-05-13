package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: DocumentationAction.java 11358 2012-05-02 13:28:22Z anuj4159 $
 */
public class DocumentationAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final DocumentationAction ACTION = new DocumentationAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static DocumentationAction getAction() {
		return ACTION;
	}

	private DocumentationAction() {
		super(BUNDLE.getString("documentation"), BUNDLE.getString("goToIGBUserGuideTooltip"),
				"16x16/actions/documentation.png",
				"22x22/actions/documentation.png",
				KeyEvent.VK_D, null, true);
		this.ordinal = 110;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("http://wiki.transvar.org/confluence/display/igbman");
	}
}
