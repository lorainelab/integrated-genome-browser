/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class UCSCViewAction extends AbstractAction implements SeqSelectionListener {
	private static final long serialVersionUID = 1l;
	private static final SeqMapView SEQ_MAP = IGB.getSingleton().getMapView();
	private static final String UCSC_DAS_URL = "http://genome.cse.ucsc.edu/cgi-bin/das/dsn";
	private static final String UCSC_URL = "http://genome.ucsc.edu/cgi-bin/hgTracks?";
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final Set<String> UCSCSources = Collections.<String>synchronizedSet(new HashSet<String>());

	public UCSCViewAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("viewRegionInUCSCBrowser")),
				MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));

		GenometryModel model = GenometryModel.getGenometryModel();
		model.addSeqSelectionListener(this);
		this.seqSelectionChanged(new SeqSelectionEvent(this, Collections.<BioSeq>singletonList(model.getSelectedSeq())));
	}

	public void actionPerformed(ActionEvent ae) {
		String query = getUCSCQuery();

		if (!query.isEmpty()) {
			GeneralUtils.browse(UCSC_URL + query);
		} else {
			Application.errorPanel("Don't have UCSC information for genome " + SEQ_MAP.getAnnotatedSeq().getVersion());
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		this.setEnabled(evt.getSelectedSeq() != null);
	}

	/** Returns the genome UcscVersion in UCSC two-letter plus number format, like "hg17". */
	private static String getUcscGenomeVersion(String version) {
		initUCSCSources();
		String ucsc_version = LOOKUP.findMatchingSynonym(UCSCSources, version);
		return UCSCSources.contains(ucsc_version) ? ucsc_version : "";
	}

	private static void initUCSCSources() {
		synchronized(UCSCSources) {
			if (UCSCSources.isEmpty()) {
				DasServerInfo ucsc = new DasServerInfo(UCSC_DAS_URL);
				UCSCSources.addAll(ucsc.getDataSources().keySet());
			}
		}
	}

	/**
	 * generates part of UCSC query url for current genome coordinates.
	 * @return query URL for current view. "ucsc version not resolvable" on error.
	 */
	public static String getUCSCQuery(){
		BioSeq aseq = SEQ_MAP.getAnnotatedSeq();

		if (aseq == null) { return ""; }

        String UcscVersion = getUcscGenomeVersion(aseq.getVersion());
        if(!UcscVersion.isEmpty()){
            return "db=" + UcscVersion + "&position=" + getRegionString();
        }

        return "";
    }

	/**
	 *  Returns the current position in the format used by the UCSC browser.
	 *  This format is also understood by GBrowse and the MapRangeBox of IGB.
	 *  @return a String such as "chr22:15916196-31832390", or null.
	 */
	private static String getRegionString() {
		Rectangle2D.Double vbox = SEQ_MAP.getSeqMap().getView().getCoordBox();

		return SEQ_MAP.getAnnotatedSeq().getID() + ":" + (int)vbox.x + "-" + (int)(vbox.x + vbox.width);
	}
}
