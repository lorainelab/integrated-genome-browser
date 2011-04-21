package com.affymetrix.igb.util;

import com.affymetrix.genoviz.swing.InfoLabel;
import com.affymetrix.igb.IGB;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author hiralv
 */
public class IGBUtils {

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb
	 *  @return null if the image file is not found or can't be opened.
	 */
	public static ImageIcon getIcon(String name) {
		ImageIcon icon = null;
		try {
			URL url = IGB.class.getResource(name);
			if (url != null) {
				return new ImageIcon(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}

	public static JPanel setInfoLabel(JComponent component, String tooltip){
		JLabel infolabel = new InfoLabel(getIcon("info_icon.gif"));
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
