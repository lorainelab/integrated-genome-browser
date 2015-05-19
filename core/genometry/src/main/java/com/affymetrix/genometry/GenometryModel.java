package com.affymetrix.genometry;

import com.affymetrix.genometry.event.GroupSelectionEvent;
import com.affymetrix.genometry.event.GroupSelectionListener;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenometryModel {

    private static final Logger logger = LoggerFactory.getLogger(GenometryModel.class);

    /**
     * Ann's comment: There is a lot of logic related to selection of
     * SeqSymmetry objects. It appears that SeqSymmetry on many different BioSeq
     * objects can be selected simultaneously. Why? What does selection mean in
     * this context?
     */
    private final Map<String, GenomeVersion> genomeVersions = new LinkedHashMap<>();
	// LinkedHashMap preserves the order things were added in, which is nice for QuickLoad

    // maps sequences to lists of selected symmetries
    private final Map<BioSeq, List<SeqSymmetry>> seq2selectedGraphSymsHash = new HashMap<>();

    private final Set<SeqSelectionListener> seq_selection_listeners = new CopyOnWriteArraySet<>();
    private final Set<GroupSelectionListener> group_selection_listeners = new CopyOnWriteArraySet<>();
    private final Set<SymSelectionListener> sym_selection_listeners = new CopyOnWriteArraySet<>();

    private GenomeVersion selectedGenomeVersion = null;
    private BioSeq selectedBioSeq = null;

    private GenometryModel() {
    }

    public static GenometryModel getInstance() {
        return GenometryModelHolder.INSTANCE;
    }

    /**
     * *
     * Clear out all instance variables from genometry model so that it can be
     * reloaded.
     */
    public void resetGenometryModel() {
        this.genomeVersions.clear();
        this.seq2selectedGraphSymsHash.clear();

        this.seq_selection_listeners.clear();
        group_selection_listeners.clear();
        sym_selection_listeners.clear();

        selectedGenomeVersion = null;
        selectedBioSeq = null;
    }

    /**
     * Returns a Map of String names to GenomeVersion objects.
     *
     * @return
     */
    public Map<String, GenomeVersion> getSeqGroups() {
        return Collections.unmodifiableMap(genomeVersions);
    }

    public synchronized List<String> getSeqGroupNames() {
        List<String> list = new ArrayList<>(genomeVersions.keySet());
        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    public GenomeVersion getSeqGroup(String group_syn) {
        if (StringUtils.isBlank(group_syn)) {
            return null;
        }
        GenomeVersion genomeVersion = genomeVersions.get(group_syn);
        if (genomeVersion == null) {
            // try and find a synonym
            for (GenomeVersion curgroup : genomeVersions.values()) {
                if (curgroup.isSynonymous(group_syn)) {
                    return curgroup;
                }
            }
        }
        return genomeVersion;
    }

    /**
     * Returns the seq group with the given id, creating a new one if there
     * isn't an existing one.
     *
     * @param genomeVersionName
     * @return a non-null GenomeVersion
     */
    public synchronized GenomeVersion addGenomeVersion(String genomeVersionName) {
        // if GenomeVersion with same or synonymous id already exists, then return it
        GenomeVersion genomeVersion = getSeqGroup(genomeVersionName);
        // otherwise create a new GenomeVersion
        if (genomeVersion == null) {
            genomeVersion = new GenomeVersion(genomeVersionName);
            genomeVersions.put(genomeVersion.getName(), genomeVersion);
            //fireModelChangeEvent(GenometryModelChangeEvent.SEQ_GROUP_ADDED, group);
        }
        return genomeVersion;
    }

    public void removeGenomeVersion(String genomeVersionName) {
        genomeVersions.remove(genomeVersionName);
    }

    public synchronized void addSeqGroup(GenomeVersion genomeVersion) {
        genomeVersions.put(genomeVersion.getName(), genomeVersion);
        //fireModelChangeEvent(GenometryModelChangeEvent.SEQ_GROUP_ADDED, group);
    }

    public GenomeVersion getSelectedGenomeVersion() {
        return selectedGenomeVersion;
    }

    public void refreshCurrentGenome() {
        fireGroupSelectionEvent(this, ImmutableList.of(selectedGenomeVersion));
    }

    // TODO: modify so that fireGroupSelectionEvent() is only called if
    //     group arg is different than previous selected_group
    public void setSelectedGenomeVersion(GenomeVersion genomeVersion) {
        selectedGenomeVersion = genomeVersion;
        selectedBioSeq = null;
        List<GenomeVersion> glist = new ArrayList<>();
        glist.add(selectedGenomeVersion);
        fireGroupSelectionEvent(this, glist);
    }

    public void addGroupSelectionListener(GroupSelectionListener listener) {
        group_selection_listeners.add(listener);
    }

    public void removeGroupSelectionListener(GroupSelectionListener listener) {
        group_selection_listeners.remove(listener);
    }

    private void fireGroupSelectionEvent(Object src, List<GenomeVersion> glist) {
        GroupSelectionEvent evt = new GroupSelectionEvent(src, glist);
        for (GroupSelectionListener listener : group_selection_listeners) {
            listener.groupSelectionChanged(evt);
        }
    }

    public Optional<BioSeq> getSelectedSeq() {
        return Optional.ofNullable(selectedBioSeq);
    }

    public void setSelectedSeq(BioSeq seq) {
        setSelectedSeq(seq, this);
    }

    /**
     * currently seq selection events _need_ to be fired even if seq is same as
     * previously selected seq, because in some SeqSelectionListeners (such as
     * SeqMapView) the side effects of receiving a SeqSelectionEvent are
     * important even if selected seq is same.
     */
    public void setSelectedSeq(BioSeq seq, Object src) {
        logger.debug("seq = " + (seq == null ? null : seq.getId()));
        selectedBioSeq = seq;
        ArrayList<BioSeq> slist = new ArrayList<>();
        slist.add(selectedBioSeq);
        fireSeqSelectionEvent(src, slist);
    }

    public void addSeqSelectionListener(SeqSelectionListener listener) {
        seq_selection_listeners.add(listener);
    }

    public void removeSeqSelectionListener(SeqSelectionListener listener) {
        seq_selection_listeners.remove(listener);
    }

    /**
     * SeqSelectionListeners are notified in the order they were added as
     * listeners (order is important when one listener relies on another's state
     * -- for example in IGB some listeners assume main SeqMapView has been
     * notified first)
     */
    void fireSeqSelectionEvent(Object src, List<BioSeq> slist) {
        SeqSelectionEvent evt = new SeqSelectionEvent(src, slist);
        for (SeqSelectionListener listener : seq_selection_listeners) {
            listener.seqSelectionChanged(evt);
        }
    }

    public void addSymSelectionListener(SymSelectionListener listener) {
        sym_selection_listeners.add(listener);
    }

    public void removeSymSelectionListener(SymSelectionListener listener) {
        sym_selection_listeners.remove(listener);
    }

    private void fireSymSelectionEvent(Object src, List<RootSeqSymmetry> all_syms, List<SeqSymmetry> graph_syms) {
        logger.debug("Firing event: " + all_syms.size() + " " + graph_syms.size());
        SymSelectionEvent sevt = new SymSelectionEvent(src, all_syms, graph_syms);
        for (SymSelectionListener listener : sym_selection_listeners) {
            listener.symSelectionChanged(sevt);
        }
    }

    /**
     * Sets the selected symmetries. The symmetries can be on multiple
     * sequences, and selecting them will not automatically change the selected
     * sequence. All the SymSelectionListener's will be notified.
     *
     * @param all_syms A List of SeqSymmetry objects to select.
     * @param graph_syms A List of Graph SeqSymmetry objects to select.
     * @param src The object responsible for selecting the sequences.
     */
    public void setSelectedSymmetries(List<RootSeqSymmetry> all_syms, List<SeqSymmetry> graph_syms, Object src) {
        setSelectedSymmetries(graph_syms);
        fireSymSelectionEvent(src, all_syms, graph_syms); // Note this is the complete list of selections
    }

    /**
     * NOTE - legacy code, this is only used by the IGBScript language Sets the
     * selected symmetries AND selects one of the sequences that they lie on.
     * The symmetries can be on multiple sequences. If the current bio seq
     * contains one of the symmetries, the seq will not be changed. Otherwise
     * the seq will be changed to one of the ones which has a selected symmetry.
     * The SeqSelectionListener's will be notified first (only if the seq
     * changes), and then the SymSelectionListener's will be notified.
     *
     * @param graph_syms A List of SeqSymmetry objects to select.
     * @param src The object responsible for selecting the sequences.
     */
    public void setSelectedSymmetriesAndSeq(List<SeqSymmetry> graph_syms, Object src) {
        List<BioSeq> seqs_with_selections = setSelectedSymmetries(graph_syms);
        final Optional<BioSeq> selectedSeq = getSelectedSeq();
        if (selectedSeq.isPresent() && !seqs_with_selections.contains(selectedSeq.get())) {
            if (getSelectedSymmetries(selectedSeq.get()).isEmpty()) {
                BioSeq seq = null;
                if (!seqs_with_selections.isEmpty()) {
                    seq = seqs_with_selections.get(0);
                }
                setSelectedSeq(seq, src);
            }
        }
        List<RootSeqSymmetry> all_syms = Collections.<RootSeqSymmetry>emptyList();
        fireSymSelectionEvent(src, all_syms, graph_syms); // Note this is the complete list of selections
    }

    /**
     * Sets the selected symmetries. The symmetries can be on multiple
     * sequences, and selecting them will not automatically change the selected
     * sequence. This non-public version does NOT send a SymSelectionEvent.
     *
     * @param syms A List of SeqSymmetry objects to select.
     * @return The List of sequences with selections on them after this
     * operation.
     */
    private List<BioSeq> setSelectedSymmetries(List<SeqSymmetry> syms) {

        logger.debug("SetSelectedSymmetries called, number of syms: " + syms.size());

        HashMap<BioSeq, List<SeqSymmetry>> seq2GraphSymsHash = new HashMap<>();

        // for each ID found in the ID2sym hash, add it to the owning sequences
        // list of selected symmetries
        HashSet<BioSeq> all_seqs = new HashSet<>(); // remember all seqs found
        for (SeqSymmetry sym : syms) {
            if (sym == null) {
                continue;
            }
            BioSeq seq = null;
            if (getSelectedGenomeVersion() != null) { //fixes NPE
                seq = getSelectedGenomeVersion().getSeq(sym);
            }
            if (seq == null) {
                continue;
            }
            // prepare the list to add the sym to based on the seq ID
            List<SeqSymmetry> symlist = seq2GraphSymsHash.get(seq);
            if (symlist == null) {
                symlist = new ArrayList<>();
                seq2GraphSymsHash.put(seq, symlist);
            }
            // add the sym to the list for the correct seq ID
            symlist.add(sym);
            all_seqs.add(seq);
        }

        // clear all the existing selections first
        clearSelectedSymmetries(); // do not send an event yet

        // now perform the selections for each sequence that was matched
        for (Map.Entry<BioSeq, List<SeqSymmetry>> entry : seq2GraphSymsHash.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Syms " + entry.getValue().size() + " on seq " + entry.getKey().getId());
            }
            setSelectedSymmetries(entry.getValue(), entry.getKey()); // do not send an event yet
        }

        return new ArrayList<>(all_seqs);
    }

    // Selects a List of SeqSymmetry objects for a particular BioSeq.
    // Does not send a selection event.
    private void setSelectedSymmetries(List<SeqSymmetry> syms, BioSeq seq) {
        if (seq == null) {
            return;
        }
        // Should it complain if any of the syms are not on the specified seq?
        // (This is not an issue since this is not called from outside of this class.)

        if (logger.isDebugEnabled()) {
            logger.debug("GenometryModel.setSelectedSymmetries() called, ");
            logger.debug("    syms = " + syms);
        }
        // set the selected syms for the sequence
        if (syms != null && !syms.isEmpty()) {
            seq2selectedGraphSymsHash.put(seq, syms);
        } else {
            seq2selectedGraphSymsHash.remove(seq); // to avoid memory leaks when a seq is deleted
        }
    }

    /**
     * Get the list of selected symmetries on the specified sequence.
     *
     * @return A List of the selected SeqSymmetry objects, can be empty, but not
     * null
     */
    public final List<SeqSymmetry> getSelectedSymmetries(BioSeq seq) {
        List<SeqSymmetry> selections = seq2selectedGraphSymsHash.get(seq);
        if (selections == null) {
            selections = new ArrayList<>();
            // NO:  This is a memory leak: seq2selectedSymsHash.put(seq, selections);
        }
        return selections;
    }

    /**
     * Clears the symmetry selection for every sequence. Notifies all the
     * selection listeners.
     *
     * @param src The object to use as the event source for the
     * SymSelectionEvent.
     */
    public void clearSelectedSymmetries(Object src) {
        clearSelectedSymmetries();
        fireSymSelectionEvent(src, Collections.<RootSeqSymmetry>emptyList(), Collections.<SeqSymmetry>emptyList());
    }

    /**
     * Clears the symmetry selection for every sequence. Does not notifies the
     * selection listeners.
     */
    private void clearSelectedSymmetries() {
        seq2selectedGraphSymsHash.values().forEach(List<SeqSymmetry>::clear);
        seq2selectedGraphSymsHash.clear();
    }

    private static class GenometryModelHolder {

        private static final GenometryModel INSTANCE = new GenometryModel();
    }
}
