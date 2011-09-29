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
		super();
		this.url = url;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse(url);
	}

	@Override
	public String getText() {
		return BUNDLE.getString("trackInfo");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Information16.gif";
	}
}
