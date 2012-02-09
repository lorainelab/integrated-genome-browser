/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genoviz.swing.JTextButtonCellRenderer;
import com.affymetrix.igb.IGBServiceImpl;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 *
 * @author dcnorris
 */
public class ErrorNotificationCellRenderer extends JTextButtonCellRenderer {

	static final Icon info_icon = CommonUtils.getInstance().getIcon("images/warning.png");
	private VirtualFeature vFeature;

	public ErrorNotificationCellRenderer(VirtualFeature vFeature) {
		super(IGBServiceImpl.getInstance().getFrame());
		this.vFeature = vFeature;
	}

	@Override
	protected JButton getButton() {
		return new JButton(info_icon);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ErrorHandler.errorPanel(vFeature.getFeature().featureName, vFeature.getLastRefreshStatus().toString());
	}
}
