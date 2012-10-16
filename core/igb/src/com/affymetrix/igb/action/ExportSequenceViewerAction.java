
package com.affymetrix.igb.action;

import java.awt.Adjustable;
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
	private final Adjustable scroller;
	
	public ExportSequenceViewerAction(Component comp, Adjustable scroller) {
		super(BUNDLE.getString("exportImage"), "16x16/actions/Sequence_Viewer_export.png", "22x22/actions/Sequence_Viewer_export.png");
		this.comp = comp;
		this.scroller = scroller;
	}

	@Override
	public Component determineSlicedComponent() {
		return comp;
	}

	@Override
	public Adjustable getScroller() {
		return scroller;
	}
}
