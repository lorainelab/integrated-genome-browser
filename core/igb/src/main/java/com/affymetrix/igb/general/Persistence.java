package com.affymetrix.igb.general;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;
import java.util.prefs.Preferences;

public final class Persistence {

    private final static String GENOME_ID = "GENOME_ID";  // full genome version ID if gets MD5-compressed in node creation
    private final static String SEQ_ID = "SEQ_ID";  // full seq ID if gets MD5-compressed in node creation
    private final static String SELECTED_GENOME_PREF = "SELECTED_GENOME_PREF";
    private final static String SELECTED_SEQ_PREF = "SELECTED_SEQ_PREF";
    private final static String SEQ_MIN_PREF = "SEQ_MIN_PREF";
    private final static String SEQ_MAX_PREF = "SEQ_MAX_PREF";
    private final static GenometryModel gmodel = GenometryModel.getInstance();

    /**
     * Saves information on current genomeVersion
     * Using Preferences node: [igb_root_pref]/genomes/[group_id]
     * Using PreferenceUtils to convert node names if they are too long
     * tagvals:
     * GENOME_ID
     * SEQ_ID
     * SELECTED_GENOME_PREF
     * SELECTED_SEQ_PREF
     */
    public static void saveCurrentView(SeqMapView gviewer) {
        GenomeVersion genomeVersion = gmodel.getSelectedGenomeVersion();
        if (gmodel.getSelectedSeq() != null) {
            BioSeq seq = gmodel.getSelectedSeq().orElse(null);
            saveGroupSelection(genomeVersion);
            saveSeqSelection(seq);
            saveSeqVisibleSpan(gviewer);
        }
    }

    public static void saveGroupSelection(GenomeVersion genomeVersion) {
        Preferences genomes_node = PreferenceUtils.getGenomesNode();
        if (genomes_node == null || genomeVersion == null) {
            return;
        }
        genomes_node.put(SELECTED_GENOME_PREF, genomeVersion.getName());

        Preferences group_node = PreferenceUtils.getSubnode(genomes_node, genomeVersion.getName(), true);
        //  encodes id via MD5 if too long, also remove forward slashes ("/")
        group_node.put(GENOME_ID, genomeVersion.getName());  // preserve actual ID, no MD5 encoding, no slash removal

    }

    /**
     * Restore selection of genomeVersion.
     *
     * @return the restored genomeVersion which is an GenomeVersion.
     */
    public static GenomeVersion restoreGroupSelection() {
        Preferences genomes_node = PreferenceUtils.getGenomesNode();
        String group_id = genomes_node.get(SELECTED_GENOME_PREF, "");
        if (group_id == null || group_id.length() == 0) {
            return null;
        }
        return gmodel.getSeqGroup(group_id);
    }

    /**
     * Save information on which seq is currently being viewed
     * Using Preferences node: [igb_root_pref]/genomes/[group_id],
     * {SELECTED_SEQ_PREF ==> seq_id }
     * Using PreferenceUtils to convert node names if they are too long
     */
    public static void saveSeqSelection(BioSeq seq) {
        if (seq == null) {
            return;
        }

        GenomeVersion current_group = seq.getGenomeVersion();
        if (current_group == null) {
            return;
        }
        Preferences genomes_node = PreferenceUtils.getGenomesNode();
        Preferences group_node = PreferenceUtils.getSubnode(genomes_node, current_group.getName(), true);
        //  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
        group_node.put(SELECTED_SEQ_PREF, seq.getId());
    }

    /**
     * Restore the selected chromosome.
     *
     * @param genomeVersion
     * @return restore the selected chromosome which is a BioSeq
     */
    public static BioSeq restoreSeqSelection(GenomeVersion genomeVersion) {
        Preferences genomes_node = PreferenceUtils.getGenomesNode();
        Preferences group_node = PreferenceUtils.getSubnode(genomes_node, genomeVersion.getName(), true);
        //  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
        String seq_id = group_node.get(SELECTED_SEQ_PREF, "");
        if (seq_id == null || seq_id.length() == 0) {
            return null;
        }

        BioSeq seq = genomeVersion.getSeq(seq_id);
        // if selected or default seq can't be found, use first seq in genomeVersion
        if (seq == null && genomeVersion.getSeqCount() > 0) {
            seq = genomeVersion.getSeq(0);
        }
        return seq;
    }

    /**
     * Saving visible span info for currently viewed seq
     * Uses Preferences node: [igb_root_pref]/genomes/[group_id]/seqs/[seq_id]
     * {SEQ_MIN_PREF ==> viewspan.getMin() }
     * {SEQ_MAX_PREF ==> viewspan.getMax() }
     * {ID ==> seq_id }
     * Using PreferenceUtils to convert node names if they are too long
     */
    public static void saveSeqVisibleSpan(SeqMapView gviewer) {
        SeqSpan visible_span = gviewer.getVisibleSpan();
        if (visible_span != null) {
            BioSeq seq = visible_span.getBioSeq();
            if (seq != null) {
                GenomeVersion genomeVersion = seq.getGenomeVersion();
                Preferences genomes_node = PreferenceUtils.getGenomesNode();
                Preferences group_node = PreferenceUtils.getSubnode(genomes_node, genomeVersion.getName(), true);  //  encodes id via MD5 if too long
                Preferences seqs_node = PreferenceUtils.getSubnode(group_node, "seqs");
                Preferences seq_node = PreferenceUtils.getSubnode(seqs_node, seq.getId(), true);  //  encodes id via MD5 if too long
                seq_node.put(SEQ_ID, seq.getId());   // in case node name is MD5 encoded
                seq_node.putInt(SEQ_MIN_PREF, visible_span.getMin());
                seq_node.putInt(SEQ_MAX_PREF, visible_span.getMax());
            }
        }
    }

    /**
     * Assumes that correct seq has already been set in gviewer (usually due to
     * gviewr bein a SeqSelectionListener on gmodel)
     */
    public static SeqSpan restoreSeqVisibleSpan(SeqMapView gviewer) {
        BioSeq seq = gviewer.getViewSeq();
        if (seq == null) {
            return null;
        }

        GenomeVersion genomeVersion = seq.getGenomeVersion();
        Preferences genomes_node = PreferenceUtils.getGenomesNode();
        Preferences group_node = PreferenceUtils.getSubnode(genomes_node, genomeVersion.getName(), true);  //  encodes id via MD5 if too long
        Preferences seqs_node = PreferenceUtils.getSubnode(group_node, "seqs");
        Preferences seq_node = PreferenceUtils.getSubnode(seqs_node, seq.getId(), true);  //  encodes id via MD5 if too long
        int seq_min = seq_node.getInt(SEQ_MIN_PREF, 0);
        int seq_max = seq_node.getInt(SEQ_MAX_PREF, seq.getLength());
        SeqSpan span = new SimpleSeqSpan(seq_min, seq_max, seq);
        gviewer.zoomTo(span);
        return span;
    }
}
