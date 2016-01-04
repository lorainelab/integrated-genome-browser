package com.affymetrix.genometry;

import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.NotResponding;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.lorainelab.igb.synonymlookup.services.ChromosomeSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.SpeciesSynonymsLookup;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container for {@link com.affymetrix.genometry.BioSeq} objects typically corresponding to a 
 * genome assembly in Integrated Genome Browser.
 * 
 */
public class GenomeVersion {

    private static final Logger logger = LoggerFactory.getLogger(GenomeVersion.class);
    private final String UNKNOWN_ID = "UNKNOWN_SYM_";
    private int unknown_id_no = 1;
    final private String name;
    private String speciesName;
    private String description;
    private final Set<DataContainer> dataContainers;
    private boolean useSynonyms;
    final private Map<String, BioSeq> id2seq;
    private List<BioSeq> seqlist; //lazy copy of id2seq.values()
    private final Map<String, Integer> type_id2annot_id = Maps.newConcurrentMap();
    private final SetMultimap<String, String> uri2Seqs = HashMultimap.<String, String>create();
    private static GenomeVersionSynonymLookup genomeVersionSynonymLookup;
    private static ChromosomeSynonymLookup chrSynLookup;
    private static SpeciesSynonymsLookup speciesSynLookup;
    private final LocalDataProvider localDataSetProvider;
    private boolean id2seq_dirty_bit; // used to keep the lazy copy

    static {
        Bundle bundle = FrameworkUtil.getBundle(GenomeVersion.class);
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            
            ServiceReference<GenomeVersionSynonymLookup> genomeVersionSynLookupReference = bundleContext.getServiceReference(GenomeVersionSynonymLookup.class);
            genomeVersionSynonymLookup = bundleContext.getService(genomeVersionSynLookupReference);

            ServiceReference<ChromosomeSynonymLookup> chrSynLookupReference = bundleContext.getServiceReference(ChromosomeSynonymLookup.class);
            chrSynLookup = bundleContext.getService(chrSynLookupReference);
            
            ServiceReference<SpeciesSynonymsLookup> speciesSynLookupReference = bundleContext.getServiceReference(SpeciesSynonymsLookup.class);
            speciesSynLookup = bundleContext.getService(speciesSynLookupReference);
        }
    }

    public GenomeVersion(String name) {
        this.name = name;
        useSynonyms = true;
        id2seq = Collections.synchronizedMap(new LinkedHashMap<>());
        id2seq_dirty_bit = false;
        seqlist = new ArrayList<>();
        dataContainers = Sets.newConcurrentHashSet();
        localDataSetProvider = new LocalDataProvider();
    }

    final public String getName() {
        return name;
    }

    final public void setDescription(String descr) {
        description = descr;
    }

    final public String getDescription() {
        return description;
    }

    final public String getSpeciesName() {
        if (!Strings.isNullOrEmpty(speciesName)) {
            return speciesName;
        }

        String specName = speciesSynLookup.getSpeciesName(name);

        if (!Strings.isNullOrEmpty(specName)) {
            this.speciesName = specName;
        }

        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    final public void addDataContainer(DataContainer dataContainer) {
        this.dataContainers.add(dataContainer);
    }

    final public void removeDataContainer(DataContainer dataContainer) {
        this.dataContainers.remove(dataContainer);
    }

    final public Set<DataContainer> getAvailableDataContainers() {
        return dataContainers.stream()
                .filter(dc -> dc.getDataProvider() != null)
                .filter(dc -> dc.getDataProvider().getStatus() != Disabled)
                .filter(dc -> dc.getDataProvider().getStatus() != NotResponding)
                .collect(Collectors.toSet());
    }

    final public Set<DataContainer> getDataContainers() {
        return dataContainers;
    }

    public LocalDataProvider getLocalDataSetProvider() {
        return localDataSetProvider;
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
     * Returns a List of BioSeq objects. Will not return null. The list is in the same order as in {@link #getSeq(int)}.
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
     * Sets whether or not to use the SynonymLookup class to search for synonymous BioSeqs when using the getSeq(String)
     * method. If you set this to false and then add new sequences, you should probably NOT later set it back to true
     * unless you are sure you did not add some synonymous sequences.
     */
    public final void setUseSynonyms(boolean b) {
        useSynonyms = b;
    }

    public void loadChromosomeSynonyms(InputStream istr) throws IOException {
        chrSynLookup.loadSynonyms(istr, false);
    }

    /**
     * Gets a sequence based on its name, possibly taking synonyms into account. See {@link #setUseSynonyms(boolean)}.
     *
     * @param synonym the string identifier of the requested BioSeq
     * @return a BioSeq for the given synonym or null
     */
    public BioSeq getSeq(String synonym) {
        BioSeq bioSeq = id2seq.get(synonym.toLowerCase());
        if (useSynonyms && bioSeq == null) {
            // Try and find a synonym.
            // First look up species specific synonym
            if (chrSynLookup != null) {
                bioSeq = findSeqSynonym(synonym, chrSynLookup);
            }

            // If synonym is not found in local then lookup global list
            if (bioSeq == null) {
                bioSeq = findSeqSynonym(synonym, chrSynLookup);
            }

        }
        return bioSeq;
    }

    private BioSeq findSeqSynonym(String synonym, ChromosomeSynonymLookup lookup) {
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
     * For the given symmetry, tries to find in the group a sequence that is pointed to by that symmetry.
     *
     * @return the first sequence it finds (by iterating through sym's spans), or null if none is found. PRECONDITION:
     * sym != null.
     */
    public BioSeq getSeq(SeqSymmetry sym) {
        final int spancount = sym.getSpanCount();
        for (int i = 0; i < spancount; i++) {
            SeqSpan span = sym.getSpan(i);
            BioSeq seq1 = span.getBioSeq();
            String seqid = seq1.getId();
            BioSeq seq2 = id2seq.get(seqid.toLowerCase());
            if ((seq2 != null) && (seq1 == seq2)) {
                return seq2;
            }
        }
        return null;
    }

    public final boolean isSynonymous(String synonym) {
        return name.equals(synonym) || genomeVersionSynonymLookup.isSynonym(name, synonym);
    }

    public boolean removeSeqsForUri(String uri) {
        return !uri2Seqs.removeAll(uri).isEmpty();
    }

    public final BioSeq addSeq(String seqid, int length) {
        return addSeq(seqid, length, "");
    }

    /**
     * Returns the BioSeq with the given id (or synonym), creating it if necessary, and increasing its length to the
     * given sym if necessary.
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
        String seqID = seq.getId();
        final BioSeq oldseq = id2seq.get(seqID.toLowerCase());
        if (oldseq == null) {
            synchronized (this) {
                id2seq_dirty_bit = true;
                id2seq.put(seqID.toLowerCase(), seq);
                seq.setGenomeVersion(this);
            }
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
     * Get unique id for id/trackName combination. Note this does not auto-increment, in order for the name to be
     * reproducible if we need to load from the same combination again.
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
     * Returns input id if no GraphSyms on any seq in the given seq group are already using that id. Otherwise uses id
     * to build a new unique id. The id returned is unique for GraphSyms on all seqs in the given group.
     */
    public static String getUniqueGraphID(String id, GenomeVersion seq_group) {
        String result = id;
        for (BioSeq seq : seq_group.getSeqList()) {
            result = getUniqueGraphID(result, seq);
        }
        return result;
    }

    /**
     * Returns input id if no GraphSyms on seq with given id. Otherwise uses id to build a new id that is not used by a
     * GraphSym (or top-level container sym ) currently on the seq. The id returned is only unique for GraphSyms on that
     * seq, may be used for graphs on other seqs.
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenomeVersion other = (GenomeVersion) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    public GenomeVersionSynonymLookup getGenomeVersionSynonymLookup() {
        return genomeVersionSynonymLookup;
    }

    public ChromosomeSynonymLookup getChrSynLookup() {
        return chrSynLookup;
    }

    public SpeciesSynonymsLookup getSpeciesSynLookup() {
        return speciesSynLookup;
    }

    public void setGenomeVersionSynonymLookup(GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        GenomeVersion.genomeVersionSynonymLookup = genomeVersionSynonymLookup;
    }

    public void setChrSynLookup(ChromosomeSynonymLookup chrSynLookup) {
        GenomeVersion.chrSynLookup = chrSynLookup;
    }

    public void setSpeciesSynLookup(SpeciesSynonymsLookup speciesSynLookup) {
        GenomeVersion.speciesSynLookup = speciesSynLookup;
    }
    
}
