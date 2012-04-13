/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
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

	public static RequestFeatureAction getAction() {
		return ACTION;
	}

	private RequestFeatureAction() {
		super(BUNDLE.getString("requestAFeature"), null, "toolbarButtonGraphics/development/Application16.gif", KeyEvent.VK_R, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("http://sourceforge.net/tracker/?group_id=129420&atid=714747");
	}
}
