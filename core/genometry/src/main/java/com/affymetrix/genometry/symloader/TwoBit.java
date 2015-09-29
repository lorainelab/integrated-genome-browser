package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.TwoBitParser;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.SearchableCharIterator;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class TwoBit extends SymLoader {

    private static final List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("raw");
        pref_list.add("2bit");
    }
    private final Map<BioSeq, SearchableCharIterator> chrMap = new HashMap<>();
    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        // BAM files are generally large, so only allow loading visible data.
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
    }

    public TwoBit(URI uri, Optional<URI> indexUri, GenomeVersion genomeVersion, String seqName) {
        super(uri, indexUri, "", genomeVersion);
        this.isResidueLoader = true;
        try {
            BioSeq retseq = TwoBitParser.parse(uri, genomeVersion, seqName);
            if (retseq != null) {
                chrMap.put(retseq, retseq.getResiduesProvider());
                retseq.removeResidueProvider();
            }
            super.init();
        } catch (Exception ex) {
            Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public TwoBit(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion group) {
        super(uri, indexUri, "", group);
        this.isResidueLoader = true;
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        List<BioSeq> seqs = TwoBitParser.parse(uri, genomeVersion);
        if (seqs != null) {
            for (BioSeq seq : seqs) {
                chrMap.put(seq, seq.getResiduesProvider());
                seq.removeResidueProvider();
            }
        }
        super.init();
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return new ArrayList<>(chrMap.keySet());
    }

    @Override
    public String getRegionResidues(SeqSpan span) throws Exception {
        init();
        BioSeq seq = span.getBioSeq();
        if (chrMap.containsKey(seq)) {
            return chrMap.get(seq).substring(span.getMin(), span.getMax());
        }

        Logger.getLogger(TwoBit.class.getName()).log(Level.WARNING, "Seq {0} not present {1}", new Object[]{seq.getId(), uri.toString()});
        return "";
    }

    @Override
    public List<String> getFormatPrefList() {
        return pref_list;
    }
}
