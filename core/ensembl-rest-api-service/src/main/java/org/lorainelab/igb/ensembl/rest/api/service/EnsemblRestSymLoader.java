package org.lorainelab.igb.ensembl.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class EnsemblRestSymLoader extends SymLoader {
    private final String baseUrl;
    private final String track;
    private final String trackType;
    private final String contextRoot;
    private final String contextRootKey;
    private Set<String> chromosomes;

    public EnsemblRestSymLoader(String baseUrl, URI contextRoot, Optional<URI> indexUri, String track, String trackType, GenomeVersion genomeVersion, String contextRootKey) {
        super(contextRoot, indexUri, track, genomeVersion);
        this.baseUrl = baseUrl;
        this.track = track;
        this.trackType = trackType;
        this.contextRoot = contextRoot.toString();
        this.contextRootKey = contextRootKey;
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        return null;
    }


    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        return super.getChromosome(seq);
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        return super.getGenome();
    }

    @Override
    public List<BioSeq> getChromosomeList() {
        return genomeVersion.getSeqList();
    }

    @Override
    protected void init() throws Exception {
        super.init();
    }
}
