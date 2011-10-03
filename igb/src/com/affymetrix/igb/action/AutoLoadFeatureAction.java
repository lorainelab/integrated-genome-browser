package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

/**
 *
 * @author hiralv
 */
public class AutoLoadFeatureAction extends GenericAction {
	private static final long serialVersionUID = 1L;

	private static final AutoLoadFeatureAction singleton = new AutoLoadFeatureAction();
	private final JCheckBox autoload;

	private AutoLoadFeatureAction(){
		super();
		autoload = PreferenceUtils.createCheckBox(PreferenceUtils.AUTO_LOAD, PreferenceUtils.getTopNode(),
				PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		autoload.setToolTipText("Automatically load default features when available (e.g., cytoband and refseq)");
		autoload.addActionListener(this);
	}

	public static JCheckBox getAction(){
		return singleton.autoload;
	}
	
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralLoadUtils.setFeatureAutoLoad(autoload.isSelected());
	}

	@Override
	public String getText() {
		return null;
	}
}
