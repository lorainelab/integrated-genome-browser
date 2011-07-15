package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.swing.MenuUtil;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class FeatureInfoAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	final String url ;

	public FeatureInfoAction(String url){
		super(BUNDLE.getString("trackInfo"), MenuUtil.getIcon("toolbarButtonGraphics/general/Information16.gif"));
		this.url = url;
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse(url);
	}

}
