/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;


/**
 *
 * @author auser
 */
public class RequestFeatureAction extends GenericAction {

private static final long serialVersionUID = 1l;
private static final RequestFeatureAction ACTION = new RequestFeatureAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}

	public static RequestFeatureAction getAction() {
		return ACTION;
	}

	private RequestFeatureAction() {
		super(BUNDLE.getString("requestAFeature"), null,
				"16x16/actions/mail-forward.png",
				"22x22/actions/mail-forward.png",
				KeyEvent.VK_R, null, true);
		this.ordinal = 140;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("http://sourceforge.net/p/genoviz/feature-requests/");
	}
}
