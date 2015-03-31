package com.affymetrix.genometry;

import com.affymetrix.genometry.general.GenericServer;
import com.affymetrix.genometry.general.GenericVersion;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.SpeciesLookup;
import com.affymetrix.genometry.util.SynonymLookup;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

/**
 *
 * @version $Id: AnnotatedSeqGroup.java 9896 2012-01-20 16:34:31Z hiralv $
 */
public class AnnotatedSeqGroup {

    private final String UNKNOWN_ID = "UNKNOWN_SYM_";
    private int unknown_id_no = 1;
    final private String id;
    private String organism;
    private String description;
    final private Set<GenericVersion> gVersions = new CopyOnWriteArraySet<>();	// list of (visible) GenericVersions associated with this group
    private boolean use_synonyms;
    final private Map<String, BioSeq> id2seq;
    private List<BioSeq> seqlist; //lazy copy of id2seq.values()
    private boolean id2seq_dirty_bit; // used to keep the lazy copy
    private final Map<String, Integer> type_id2annot_id = Maps.newConcurrentMap();
    private final SetMultimap<String, String> uri2Seqs = HashMultimap.<String, String>create();

    private SynonymLookup chrLookup;

    public AnnotatedSeqGroup(String gid) {
        id = gid;
        use_synonyms = true;
        id2seq = Collections.synchronizedMap(new LinkedHashMap<>());
        id2seq_dirty_bit = false;
        seqlist = new ArrayList<>();
    }

    final public String getID() {
        return id;
    }

    final public void setDescription(String str) {
        description = str;
    }

    final public String getDescription() {
        return description;
    }

    /**
     * may want to move set/getOrganism() to an AnnotatedGenome subclass
     */
    final public void setOrganism(String org) {
        organism = org;
    }

    final public String getOrganism() {
        if (organism != null && !("".equals(organism))) {
            return organism;
        }

        String org = SpeciesLookup.getSpeciesName(id);

        if (org != null && !("".equals(org))) {
            this.organism = org;
        }

        return organism;
    }

    final public void addVersion(GenericVersion gVersion) {
        this.gVersions.add(gVersion);
    }

    /**
     * Only return versions that should be visible.
     */
    final public Set<GenericVersion> getEnabledVersions() {
        Set<GenericVersion> versions = new CopyOnWriteArraySet<>();
        for (GenericVersion v : gVersions) {
            if (v.gServer.isEnabled()) {
                versions.add(v);
            }
        }
        return versions;
    }

    final public GenericVersion getVersionOfServer(GenericServer gServer) {
        for (GenericVersion v : gVersions) {
            if (v.gServer.equals(gServer)) {
                return v;
            }
        }
        return null; // No Associated version with provided server.
    }

    /**
     * Return all versions.
     */
    final public Set<GenericVersion> getAllVersions() {
        return Collections.unmodifiableSet(gVersions);
    }

    final public void addType(String type, Integer annot_id) {
        type_id2annot_id.put(type, annot_id);
    }

    final public void removeType(String type) {
        type_id2annot_id.remove(type);
    }

    public final Set<String> getTypeList() {
        return type_id2annot_id.keySet();
    }

    public final Integer getAnnotationId(String type) {
        return type_id2annot_id.get(type);
    }

    /**
     * Returns a List of BioSeq objects. Will not return null. The list is in
     * the same order as in {@link #getSeq(int)}.
     */
    public List<BioSeq> getSeqList() {
        if (id2seq_dirty_bit) {
            // lazily keep the seqlist up-to-date
            seqlist = new ArrayList<>(id2seq.values());
            id2seq_dirty_bit = false;
        }
        return Collections.unmodifiableList(seqlist);
    }

    /**
     * Returns the sequence at the given position in the sequence list.
     */
    public BioSeq getSeq(int index) {
        final List<BioSeq> seq_list = getSeqList();
        if (index < seq_list.size()) {
            return seq_list.get(index);
        }
        return null;
    }

    /**
     * Returns the number of sequences in the group.
     */
    public int getSeqCount() {
        return id2seq.size();
    }

    /**
     * Sets whether or not to use the SynonymLookup class to search for
     * synonymous BioSeqs when using the getSeq(String) method. If you set this
     * to false and then add new sequences, you should probably NOT later set it
     * back to true unless you are sure you did not add some synonymous
     * sequences.
     */
    public final void setUseSynonyms(boolean b) {
        use_synonyms = b;
    }

    public void loadSynonyms(InputStream istr) throws IOException {
        if (chrLookup == null) {
            chrLookup = new SynonymLookup();
        }
        chrLookup.loadSynonyms(istr, false);
    }

    /**
     * Gets a sequence based on its name, possibly taking synonyms into account.
     * See {@link #setUseSynonyms(boolean)}.
     *
     * @param synonym the string identifier of the requested BioSeq
     * @return a BioSeq for the given synonym or null
     */
    public BioSeq getSeq(String synonym) {
        BioSeq aseq = id2seq.get(synonym.toLowerCase());
        if (use_synonyms && aseq == null) {
            // Try and find a synonym.
            // First look up species specific synonym
            if (chrLookup != null) {
                aseq = findSeqSynonym(synonym, chrLookup);
            }

            // If synonym is not found in local then lookup global list
            if (aseq == null) {
                aseq = findSeqSynonym(synonym, SynonymLookup.getChromosomeLookup());
            }

        }
        return aseq;
    }

    private BioSeq findSeqSynonym(String synonym, SynonymLookup lookup) {
        BioSeq aseq = null;
        for (String syn : lookup.getSynonyms(synonym, false)) {
            aseq = id2seq.get(syn.toLowerCase());
            if (aseq != null) {
                break;
            }
        }
        return aseq;
    }

    /**
     * For the given symmetry, tries to find in the group a sequence that is
     * pointed to by that symmetry.
     *
     * @return the first sequence it finds (by iterating through sym's spans),
     * or null if none is found. PRECONDITION: sym != null.
     */
    public BioSeq getSeq(SeqSymmetry sym) {
        final int spancount = sym.getSpanCount();
        for (int i = 0; i < spancount; i++) {
            SeqSpan span = sym.getSpan(i);
            BioSeq seq1 = span.getBioSeq();
            String seqid = seq1.getID();
            BioSeq seq2 = id2seq.get(seqid.toLowerCase());
            if ((seq2 != null) && (seq1 == seq2)) {
                return seq2;
            }
        }
        return null;
    }

    public final boolean isSynonymous(String synonym) {
        return id.equals(synonym) || SynonymLookup.getDefaultLookup().isSynonym(id, synonym);
    }

    public boolean removeSeqsForUri(String uri) {
        return !uri2Seqs.removeAll(uri).isEmpty();
    }

    public final BioSeq addSeq(String seqid, int length) {
        return addSeq(seqid, length, "");
    }

    /**
     * Returns the BioSeq with the given id (or synonym), creating it if
     * necessary, and increasing its length to the given sym if necessary.
     */
    public final BioSeq addSeq(String seqid, int length, String uri) {
        checkNotNull(seqid);
        checkNotNull(uri);
        uri2Seqs.put(uri, seqid);

        BioSeq aseq = getSeq(seqid);
        if (aseq != null) {
            if (aseq.getLength() < length) {
                aseq.setLength(length);
            }
        } else {
            aseq = createBioSeq(seqid, length);
            this.addSeq(aseq);
        }
        return aseq;
    }

    protected BioSeq createBioSeq(String seqid, int length) {
        return new BioSeq(seqid, length);
    }

    /**
     * Adds the BioSeq to the group.
     */
    private void addSeq(BioSeq seq) {
        String seqID = seq.getID();
        final BioSeq oldseq = id2seq.get(seqID.toLowerCase());
        if (oldseq == null) {
            synchronized (this) {
                id2seq_dirty_bit = true;
                id2seq.put(seqID.toLowerCase(), seq);
                seq.setSeqGroup(this);
            }
        } else {
            throw new IllegalStateException("ERROR! tried to add seq: " + seqID + " to AnnotatedSeqGroup: "
                    + this.getID() + ", but seq with same id is already in group");
        }
    }

    /**
     * Finds all symmetries with the given case-insensitive ID.
     *
     * @return a non-null List, possibly an empty one.
     */
    final public Set<SeqSymmetry> findSyms(String id) {
        Set<SeqSymmetry> results = new HashSet<>();
        for (BioSeq seq : getSeqList()) {
            seq.search(results, id);
        }
        return results;
    }

    public void searchHints(Set<String> results, Pattern regex, int limit) {
        for (BioSeq seq : getSeqList()) {
            seq.searchHints(results, regex, limit);
        }
    }

    public void search(Set<SeqSymmetry> syms, Pattern regex, int limit) {
        for (BioSeq seq : getSeqList()) {
            seq.search(syms, regex, limit);
        }
    }

    public void searchProperties(Set<SeqSymmetry> syms, Pattern regex, int limit) {
        for (BioSeq seq : getSeqList()) {
            seq.searchProperties(syms, regex, limit);
        }
    }

    public Set<String> getSymmetryIDs(String symID) {
        return Collections.<String>emptySet();
    }

    public String getUniqueID() {
        return UNKNOWN_ID + unknown_id_no++;
    }

    /**
     * Get unique id for id/trackName combination. Note this does not
     * auto-increment, in order for the name to be reproducible if we need to
     * load from the same combination again.
     *
     * @param id
     * @param trackName
     * @return unique but reproducible ID
     */
    public static String getUniqueGraphTrackID(String id, String trackName) {
        if (trackName == null || trackName.length() == 0) {
            trackName = "_EMPTY";
        }
        return id + "_TRACK_" + trackName;
    }

    /**
     * Returns input id if no GraphSyms on any seq in the given seq group are
     * already using that id. Otherwise uses id to build a new unique id. The id
     * returned is unique for GraphSyms on all seqs in the given group.
     */
    public static String getUniqueGraphID(String id, AnnotatedSeqGroup seq_group) {
        String result = id;
        for (BioSeq seq : seq_group.getSeqList()) {
            result = getUniqueGraphID(result, seq);
        }
        return result;
    }

    /**
     * Returns input id if no GraphSyms on seq with given id. Otherwise uses id
     * to build a new id that is not used by a GraphSym (or top-level container
     * sym ) currently on the seq. The id returned is only unique for GraphSyms
     * on that seq, may be used for graphs on other seqs.
     */
    public static String getUniqueGraphID(String id, BioSeq seq) {
        if (id == null) {
            return null;
        }
        if (seq == null) {
            return id;
        }

        int prevcount = 0;
        String newid = id;
        while (seq.getAnnotation(newid) != null) {
            prevcount++;
            newid = id + "." + prevcount;
        }
        return newid;
    }

    /**
     * Create a temporary shallow-copy genome, to avoid any side-effects.
     *
     * @param oldGenome
     * @return tempGenome
     */
    public static AnnotatedSeqGroup tempGenome(AnnotatedSeqGroup oldGenome) {
        AnnotatedSeqGroup tempGenome = new AnnotatedSeqGroup(oldGenome.getID());
        tempGenome.setOrganism(oldGenome.getOrganism());
        for (BioSeq seq : oldGenome.getSeqList()) {
            tempGenome.addSeq(seq.getID(), seq.getLength());
        }
        return tempGenome;
    }

}
