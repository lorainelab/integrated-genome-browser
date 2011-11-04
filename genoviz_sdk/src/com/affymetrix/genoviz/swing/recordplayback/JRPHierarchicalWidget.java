package com.affymetrix.genoviz.swing.recordplayback;

import javax.swing.JComponent;

public interface JRPHierarchicalWidget extends JRPWidget {
	public JComponent getSubComponent(String subId);
}
