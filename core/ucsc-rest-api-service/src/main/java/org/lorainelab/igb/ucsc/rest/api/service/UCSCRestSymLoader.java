package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscGeneSym;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenePred;
import org.lorainelab.igb.ucsc.rest.api.service.model.TrackDataDetails;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils.checkValidAndSetUrl;

@Slf4j
public class UCSCRestSymLoader extends SymLoader {
    public static final String CHROM = "chrom";
    public static final String START = "start";
    public static final String END = "end";
    public static final String GENE_PRED = "genePred";
    private String baseUrl;
    private String track;
    private String trackType;
    private final String contextRoot;
    private Set<String> chromosomes;


    public UCSCRestSymLoader(String baseUrl, URI contextRoot, Optional<URI> indexUri, String track, String trackType, GenomeVersion genomeVersion) {
        super(contextRoot, indexUri, track, genomeVersion);
        this.baseUrl = baseUrl;
        this.track = track;
        this.trackType = trackType;
        this.contextRoot = contextRoot.toString();
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        final int min = overlapSpan.getMin() == 0 ? 1 : overlapSpan.getMin();
        final int max = overlapSpan.getMax() - 1;
        URIBuilder uriBuilder = new URIBuilder(contextRoot);
        uriBuilder.addParameter(CHROM, getChromosomeSynonym(baseUrl, overlapSpan, genomeVersion));
        uriBuilder.addParameter(START, String.valueOf(min));
        uriBuilder.addParameter(END, String.valueOf(max));
        String validUrl = checkValidAndSetUrl(uriBuilder.toString());
        URL trackDataUrl = new URL(validUrl);
        String data = Resources.toString(trackDataUrl, Charsets.UTF_8);
        if(trackType.equalsIgnoreCase(GENE_PRED)){
            TrackDataDetails<GenePred> trackDataDetails = new Gson().fromJson(data, new TypeToken<TrackDataDetails<GenePred>>(){}.getType());
            if(Objects.nonNull(trackDataDetails))
                trackDataDetails.setTrackData(data, track, trackType);
            List<GenePred> trackDataList= trackDataDetails.getTrackData();
            List<UcscGeneSym> ucscGeneSyms = trackDataList.stream().map(trackData -> {
                int[] emins = Arrays.stream(trackData.exonStarts.split(",")).mapToInt(Integer::parseInt).toArray();
                int[] emaxs = Arrays.stream(trackData.exonEnds.split(",")).mapToInt(Integer::parseInt).toArray();
                boolean forward = trackData.strand.equals("+");
                return new UcscGeneSym(track, trackData.name2, trackData.name,
                        overlapSpan.getBioSeq(), forward, trackData.txStart, trackData.txEnd,
                        trackData.cdsStart, trackData.cdsEnd, emins, emaxs);
            }).collect(Collectors.toList());
            return ucscGeneSyms;
        }
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
    public List<BioSeq> getChromosomeList() throws Exception {
        return genomeVersion.getSeqList();
    }

    @Override
    protected void init() throws Exception {
        super.init();
    }

    private String getChromosomeSynonym(String baseUrl, SeqSpan span, GenomeVersion genomeVersion) {
        BioSeq currentSeq = span.getBioSeq();
        if (chromosomes == null) {
            chromosomes = UCSCRestServerUtils.retrieveAssemblyInfoByContextRoot(baseUrl, genomeVersion.getName()).keySet();
        }
        return genomeVersion.getGenomeVersionSynonymLookup().findMatchingSynonym(chromosomes, currentSeq.getId());
    }
}
