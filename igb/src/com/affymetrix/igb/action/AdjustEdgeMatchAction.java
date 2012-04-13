package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.glyph.EdgeMatchAdjuster;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class AdjustEdgeMatchAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final AdjustEdgeMatchAction ACTION = new AdjustEdgeMatchAction();

	public static AdjustEdgeMatchAction getAction() {
		return ACTION;
	}

	private AdjustEdgeMatchAction() {
		super(MessageFormat.format(BUNDLE.getString("menuItemHasDialog"), BUNDLE.getString("adjustEdgeMatchFuzziness")), null, null, KeyEvent.VK_F);
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		SeqMapView map_view = IGB.getSingleton().getMapView();
		EdgeMatchAdjuster.showFramedThresholder(map_view.getEdgeMatcher(), map_view);
	}
}
