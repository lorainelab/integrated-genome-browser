
package com.affymetrix.igb.action;

import java.awt.Component;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 * Modified by nick
 */
public class ExportSequenceViewerAction extends ExportComponentAction{
	private static final long serialVersionUID = 1l;
	private final Component comp;

	public ExportSequenceViewerAction(Component comp) {
		super(BUNDLE.getString("exportImage"), null, null);
		this.comp = comp;
	}

	@Override
	public Component determineSlicedComponent() {
		return comp;
	}
}
