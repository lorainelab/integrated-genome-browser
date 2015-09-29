package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.graph.BarParser;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Bar extends SymLoader {

    private List<BioSeq> seqs;

    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadStrategy.GENOME);
    }

    public Bar(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }
        super.init();

        InputStream dis = null;
        try {
            dis = LocalUrlCacher.getInputStream(uri.toURL());
            seqs = BarParser.getSeqs(uri.toString(), dis, genomeVersion, GenometryModel.getInstance(), true);
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(dis);
        }
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return seqs;
    }

    @Override
    public List<GraphSym> getGenome() throws Exception {
        InputStream bis = null;
        try {
            init();
            bis = LocalUrlCacher.getInputStream(uri.toURL());
            return BarParser.parse(uri.toString(), bis, GenometryModel.getInstance(), genomeVersion, null, 0, Integer.MAX_VALUE, uri.toString(), false, true);
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(bis);
        }
    }

    @Override
    public List<GraphSym> getChromosome(BioSeq seq) throws Exception {
        InputStream bis = null;
        try {
            init();
            bis = LocalUrlCacher.getInputStream(uri.toURL());
            return BarParser.parse(uri.toString(), bis, GenometryModel.getInstance(), genomeVersion, seq, 0, seq.getMax() + 1, uri.toString(), false, true);
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(bis);
        }
    }

    @Override
    public List<GraphSym> getRegion(SeqSpan span) throws Exception {
        InputStream bis = null;
        try {
            init();
            bis = LocalUrlCacher.getInputStream(uri.toURL());
            return BarParser.parse(uri.toString(), bis, GenometryModel.getInstance(), genomeVersion, span.getBioSeq(), span.getMin(), span.getMax(), uri.toString(), false, true);
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(bis);
        }
    }

    @Override
    public boolean isMultiThreadOK() {
        return true;
    }
}
