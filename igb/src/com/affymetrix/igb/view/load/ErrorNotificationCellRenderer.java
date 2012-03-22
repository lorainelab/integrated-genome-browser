package com.affymetrix.igb.view.load;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genoviz.swing.JTextButtonCellRenderer;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 *
 * @author dcnorris
 */
public class ErrorNotificationCellRenderer extends JTextButtonCellRenderer {
	private static final long serialVersionUID = 1L;
	static final Icon info_icon = CommonUtils.getInstance().getIcon("images/stop.png");
	private VirtualFeature vFeature;

	public ErrorNotificationCellRenderer(VirtualFeature vFeature) {
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
