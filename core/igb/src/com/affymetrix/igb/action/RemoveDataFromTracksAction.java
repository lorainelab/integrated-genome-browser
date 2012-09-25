package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.TrackView;
import static com.affymetrix.igb.shared.Selections.*;

public class RemoveDataFromTracksAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final RemoveDataFromTracksAction ACTION = new RemoveDataFromTracksAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static RemoveDataFromTracksAction getAction() {
		return ACTION;
	}

	protected RemoveDataFromTracksAction() {
		super(IGBConstants.BUNDLE.getString("deleteAction"), null,
				"16x16/actions/remove data.png",
				"22x22/actions/remove data.png", KeyEvent.VK_UNDEFINED);
		this.ordinal = -9007300;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();

		if (IGB.confirmPanel(MessageFormat.format(IGBConstants.BUNDLE.getString("confirmDelete"), seq.getID()), PreferenceUtils.getTopNode(),
				PreferenceUtils.CONFIRM_BEFORE_CLEAR, PreferenceUtils.default_confirm_before_clear)) {
			for (ITrackStyleExtended style : allStyles) {
				String method = style.getMethodName();
				if (method != null) {
					TrackView.getInstance().delete(getSeqMapView().getSeqMap(), method, style);
				}
			}
		}
		getSeqMapView().dataRemoved();	// refresh
	}
}
