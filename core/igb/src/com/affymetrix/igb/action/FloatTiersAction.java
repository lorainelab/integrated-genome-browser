package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;
import static com.affymetrix.igb.shared.Selections.*;

public class FloatTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final FloatTiersAction ACTION = new FloatTiersAction();

	private Enabler enabler = new Enabler();

	private class Enabler implements SymSelectionListener {

		@Override
		public void symSelectionChanged(SymSelectionEvent evt) {
			// Only pay attention to selections from the main SeqMapView or its map.
			// Ignore the splice view as well as events coming from this class itself.
			Object src = evt.getSource();
			SeqMapView gviewer = getSeqMapView();
			if (!(src == gviewer || src == gviewer.getSeqMap())) {
				return;
			}

			boolean hasFloater = false;
			boolean hasAnchored = false;
			for (GraphState gs : graphStates) {
				boolean floating = gs.getTierStyle().getFloatTier();
				hasFloater |= floating;
				hasAnchored |= !floating;
			}
			ACTION.setEnabled(hasAnchored);

		}
		
	};

	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION.enabler);
		ACTION.setEnabled(false);
	}
	
	public static FloatTiersAction getAction() {
		return ACTION;
	}

	public FloatTiersAction() {
		super("Float Track", "16x16/actions/Float.png", "22x22/actions/Float.png");
		this.ordinal = -6006511;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean something_changed = false;
		for (GraphState state : graphStates) {
				ITrackStyleExtended style = state.getTierStyle();
				boolean is_floating = style.getFloatTier();
				if (!is_floating) {
					// figure out correct height
					Rectangle2D.Double coordbox = new Rectangle2D.Double(0, style.getY(), 0, style.getHeight());
					Rectangle pixbox = new Rectangle();
					getSeqMapView().getSeqMap().getView().transformToPixels(coordbox, pixbox);
					style.setY(pixbox.y);
					style.setHeight(pixbox.height);
					
					style.setFloatTier(true);
					state.setShowLabel(true);
					
					something_changed = true;
				}
			
		}
		if (something_changed) {
			updateViewer();
		}
	}

	private void updateViewer() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SeqMapView v = getSeqMapView();
				GenometryModel m = GenometryModel.getGenometryModel();
				BioSeq s = m.getSelectedSeq();
				v.setAnnotatedSeq(s, true, true);
				v.postSelections(); // to disable partner.
			}
		});
	}
}
