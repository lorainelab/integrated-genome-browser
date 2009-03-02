package com.affymetrix.igb.general;

import java.util.prefs.Preferences;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.SeqMapView;


public final class Persistence {
	// For now need to use full URIs for DAS2 genome autoload defaults
//  public static String DEFAULT_DAS2_SERVER_URL = "http://netaffxdas.affymetrix.com/das2/sources";

//	private final static String DEFAULT_DAS2_SERVER_URL = Das2Discovery.getDas2Urls().get(Das2Discovery.DEFAULT_DAS2_SERVER_NAME);
//	private final static String DEFAULT_DAS2_SOURCE_URI = "http://netaffxdas.affymetrix.com/das2/genome/H_sapiens";
//	private final static String DEFAULT_DAS2_VERSION_URI = "http://netaffxdas.affymetrix.com/das2/genome/H_sapiens_Mar_2006";
//	private final static String DEFAULT_SELECTED_GENOME = "H_sapiens_Mar_2006";
	//  public static String DEFAULT_SELECTED_SEQ = "http://netaffxdas.affymetrix.com/das2/H_sapiens_Mar_2006/chr21";
	private final static String DEFAULT_SELECTED_SEQ = "chr21";
	private final static boolean DEBUG = false;

	private final static String SPECIES_NAME = "SPECIES_NAME";	// species
	private final static String GENOME_ID = "GENOME_ID";  // full genome version ID if gets MD5-compressed in node creation
	private final static String SEQ_ID = "SEQ_ID";  // full seq ID if gets MD5-compressed in node creation
	//private final static String DAS2_SERVER_URL_PREF = "DAS2_SERVER_URL_PREF";
	//private final static String DAS2_SOURCE_URI_PREF = "DAS2_SOURCE_URI_PREF";
	//private final static String DAS2_VERSION_URI_PREF = "DAS2_VERSION_URI_PREF";
	private final static String SELECTED_GENOME_PREF = "SELECTED_GENOME_PREF";
	private final static String SELECTED_SEQ_PREF = "SELECTED_SEQ_PREF";
	private final static String SEQ_MIN_PREF = "SEQ_MIN_PREF";
	private final static String SEQ_MAX_PREF = "SEQ_MAX_PREF";
	private final static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

	/**
	 *  Saves information on current group
	 *  Using Preferences node: [igb_root_pref]/genomes/[group_id]
	 *  Using UnibrowPrefsUtils to convert node names if they are too long
	 *  tagvals:
	 *      GENOME_ID
	 *      SEQ_ID
	 *      SELECTED_GENOME_PREF
	 *      SELECTED_SEQ_PREF
	 *      DAS2_SERVER_URL_PREF
	 *      DAS2_SOURCE_URI_PREF
	 *      DAS2_VERSION_URI_PREF
	 */
	public static void saveCurrentView(SeqMapView gviewer) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		MutableAnnotatedBioSeq seq = gmodel.getSelectedSeq();
		saveGroupSelection(group);
		saveSeqSelection(seq);
		saveSeqVisibleSpan(gviewer);
	}

	/**
	 * bootstrap bookmark from Preferences for last genome / sequence / region
	 * @param gviewer
	 * @return
	 */
	public static AnnotatedSeqGroup restoreLastView(SeqMapView gviewer) {
		AnnotatedSeqGroup group = restoreGroupSelection();
		if (group != null) {
			try {
				restoreSeqSelection(group);
				restoreSeqVisibleSpan(gviewer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return group;
	}

	public static void saveGroupSelection(AnnotatedSeqGroup group) {
		Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
		if (genomes_node == null || group == null) {
			return;
		}
		genomes_node.put(SELECTED_GENOME_PREF, group.getID());

		Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, group.getID(), true);
		//  encodes id via MD5 if too long, also remove forward slashes ("/")
		group_node.put(GENOME_ID, group.getID());  // preserve actual ID, no MD5 encoding, no slash removal

	}

	/**
	 * Restore selection of species, genome version, chromosome.
	 * @return
	 */
	public static AnnotatedSeqGroup restoreGroupSelection() {
		Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
		String group_id = genomes_node.get(SELECTED_GENOME_PREF, "");
		if (group_id == null || group_id.length() == 0) {
			return null;
		}

		Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, group_id, true);
		//  encodes id via MD5 if too long, also remove forward slashes ("/")

		if (DEBUG) {
			System.out.println("Restoring group:");
//			System.out.println("     " + server_url);
//			System.out.println("     " + source_id);
//			System.out.println("     " + version_id);
		}

		return null;
	}

	/**
	 *  Save information on which seq is currently being viewed
	 *  Using Preferences node: [igb_root_pref]/genomes/[group_id], {SELECTED_SEQ_PREF ==> seq_id }
	 *  Using UnibrowPrefUtils to convert node names if they are too long
	 */
	public static void saveSeqSelection(AnnotatedBioSeq seq) {
		if (seq == null || !(seq instanceof SmartAnnotBioSeq)) {
			return;
		}

		AnnotatedSeqGroup current_group = ((SmartAnnotBioSeq) seq).getSeqGroup();
		Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
		Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, current_group.getID(), true);
		//  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
		group_node.put(SELECTED_SEQ_PREF, seq.getID());
	}

	private static MutableAnnotatedBioSeq restoreSeqSelection(AnnotatedSeqGroup group) {
		Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
		Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, group.getID(), true);
		//  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
		String seq_id = group_node.get(SELECTED_SEQ_PREF, DEFAULT_SELECTED_SEQ);
		MutableAnnotatedBioSeq seq = group.getSeq(seq_id);
		// if selected or default seq can't be found, use first seq in group
		if (seq == null && group.getSeqCount() > 0) {
			seq = group.getSeq(0);
		}
		if (gmodel.getSelectedSeq() != seq) {
			gmodel.setSelectedSeq(seq);
		}
		return seq;
	}

	/**
	 *  Saving visible span info for currently viewed seq
	 *  Uses Preferences node: [igb_root_pref]/genomes/[group_id]/seqs/[seq_id]
	 *                                {SEQ_MIN_PREF ==> viewspan.getMin() }
	 *                                {SEQ_MAX_PREF ==> viewspan.getMax() }
	 *                                {ID ==> seq_id }
	 *  Using UnibrowPrefUtils to convert node names if they are too long
	 */
	public static void saveSeqVisibleSpan(SeqMapView gviewer) {
		SeqSpan visible_span = gviewer.getVisibleSpan();
		if (visible_span != null) {
			BioSeq seq = visible_span.getBioSeq();
			if (seq instanceof SmartAnnotBioSeq) {
				AnnotatedSeqGroup group = ((SmartAnnotBioSeq) seq).getSeqGroup();
				Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
				Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, group.getID(), true);  //  encodes id via MD5 if too long
				Preferences seqs_node = UnibrowPrefsUtil.getSubnode(group_node, "seqs");
				Preferences seq_node = UnibrowPrefsUtil.getSubnode(seqs_node, seq.getID(), true);  //  encodes id via MD5 if too long
				seq_node.put(SEQ_ID, seq.getID());   // in case node name is MD5 encoded
				seq_node.putInt(SEQ_MIN_PREF, visible_span.getMin());
				seq_node.putInt(SEQ_MAX_PREF, visible_span.getMax());
			}
		}
	}

	/**
	 *  Assumes that correct seq has already been set in gviewer (usually due to gviewr bein a SeqSelectionListener on gmodel)
	 */
	private static SeqSpan restoreSeqVisibleSpan(SeqMapView gviewer) {
		BioSeq seq = gviewer.getViewSeq();
		if (!(seq instanceof SmartAnnotBioSeq)) {
			return null;
		}

		AnnotatedSeqGroup group = ((SmartAnnotBioSeq) seq).getSeqGroup();
		Preferences genomes_node = UnibrowPrefsUtil.getGenomesNode();
		Preferences group_node = UnibrowPrefsUtil.getSubnode(genomes_node, group.getID(), true);  //  encodes id via MD5 if too long
		Preferences seqs_node = UnibrowPrefsUtil.getSubnode(group_node, "seqs");
		Preferences seq_node = UnibrowPrefsUtil.getSubnode(seqs_node, seq.getID(), true);  //  encodes id via MD5 if too long
		int seq_min = seq_node.getInt(SEQ_MIN_PREF, 0);
		int seq_max = seq_node.getInt(SEQ_MAX_PREF, seq.getLength());
		SeqSpan span = new SimpleSeqSpan(seq_min, seq_max, seq);
		gviewer.zoomTo(span);
		return span;
	}
}

