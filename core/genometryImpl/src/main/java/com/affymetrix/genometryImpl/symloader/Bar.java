package com.affymetrix.genometryImpl.symloader;

import java.io.*;
import java.util.*;
import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

public final class Bar extends SymLoader {

    private List<BioSeq> seqs;

    private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadStrategy.GENOME);
    }

    public Bar(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
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
            seqs = BarParser.getSeqs(uri.toString(), dis, group, GenometryModel.getGenometryModel(), true);
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
            return BarParser.parse(uri.toString(), bis, GenometryModel.getGenometryModel(), group, null, 0, Integer.MAX_VALUE, uri.toString(), false, true);
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
            return BarParser.parse(uri.toString(), bis, GenometryModel.getGenometryModel(), group, seq, 0, seq.getMax() + 1, uri.toString(), false, true);
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
            return BarParser.parse(uri.toString(), bis, GenometryModel.getGenometryModel(), group, span.getBioSeq(), span.getMin(), span.getMax(), uri.toString(), false, true);
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
