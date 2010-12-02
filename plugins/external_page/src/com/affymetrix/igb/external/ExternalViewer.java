package com.affymetrix.igb.external;

import java.awt.CardLayout;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import com.affymetrix.igb.osgi.service.IGBService;

/**
 * Container panel for the external views
 * Shows up as tab in IGB
 * Allows selection of subviews with combobox
 *
 * @author Ido M. Tamir
 */
public class ExternalViewer extends JComponent {
	private static final long serialVersionUID = 1L;

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("external");
	private static final String[] names = {UCSCView.viewName};
	final JComboBox ucscBox;

	public ExternalViewer(IGBService igbService) {
		this.setLayout(new CardLayout());
		ucscBox = createBox();

		final UCSCView ucsc = new UCSCView(ucscBox, igbService);

		add(ucsc, ucsc.getViewName());
	}

	private JComboBox createBox() {
		JComboBox box = new JComboBox(names);
		box.setPrototypeDisplayValue("ENSEMBL");
		box.setMaximumSize(box.getPreferredSize());
		box.setEditable(false);
		return box;
	}

	
}
