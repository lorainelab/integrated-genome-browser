package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class FeatureInfoAction extends GenericAction {
	private static final long serialVersionUID = 1L;

	final String url ;

	public FeatureInfoAction(String url){
		super(BUNDLE.getString("trackInfo"), "16x16/actions/info.png", null);
		this.url = url;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse(url);
	}
}
