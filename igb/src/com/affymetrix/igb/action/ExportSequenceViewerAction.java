
package com.affymetrix.igb.action;

import java.text.MessageFormat;
import java.awt.Component;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ExportSequenceViewerAction extends ExportComponentAction{
	private static final long serialVersionUID = 1l;
	private final Component comp;

	public ExportSequenceViewerAction(Component comp) {
		super();
		this.comp = comp;
	}

	@Override
	public Component determineSlicedComponent() {
		return comp;
	}

	@Override
	public String getText() {
		return MessageFormat.format("{0} {1}",BUNDLE.getString("export"),BUNDLE.getString("mainView"));
	}
}
