package com.affymetrix.igb.util;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genoviz.swing.InfoLabel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author hiralv
 */
public class IGBUtils {

	public static JPanel setInfoLabel(JComponent component, String tooltip){
		JLabel infolabel = new InfoLabel(CommonUtils.getInstance().getIcon("images/info.png"));
		infolabel.setToolTipText(tooltip);
		
		JPanel pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		pane.add(component, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.PAGE_START;
		c.insets = new Insets(0,0,10,0);  
		pane.add(infolabel, c);
		
		return pane;
	}
}
