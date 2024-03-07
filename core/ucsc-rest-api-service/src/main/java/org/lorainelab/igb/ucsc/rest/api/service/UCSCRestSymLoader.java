package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.AbstractPSLParser;
import com.affymetrix.genometry.parsers.BedParser;
import com.affymetrix.genometry.parsers.graph.WiggleData;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.GraphIntervalSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscGeneSym;
import com.affymetrix.genometry.symmetry.impl.UcscPslSym;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.*;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.lorainelab.igb.ucsc.rest.api.service.model.TrackDataDetails.*;
import static org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils.HTTP_RESPONSE_OKAY;
import static org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils.checkValidAndSetUrl;

@Slf4j
public class UCSCRestSymLoader extends SymLoader {
    public static final String CHROM = "chrom";
    public static final String START = "start";
    public static final String END = "end";
    private String baseUrl;
    private String track;
    private String trackType;
    private final String contextRoot;
    private final String contextRootKey;
    private Set<String> chromosomes;

    public UCSCRestSymLoader(String baseUrl, URI contextRoot, Optional<URI> indexUri, String track, String trackType, GenomeVersion genomeVersion, String contextRootKey) {
        super(contextRoot, indexUri, track, genomeVersion);
        this.baseUrl = baseUrl;
        this.track = track;
        this.trackType = trackType;
        this.contextRoot = contextRoot.toString();
        this.contextRootKey = contextRootKey;
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        final int min = overlapSpan.getMin() == 0 ? 1 : overlapSpan.getMin();
        final int max = overlapSpan.getMax() - 1;
        URIBuilder uriBuilder = new URIBuilder(contextRoot);
        uriBuilder.addParameter(CHROM, getChromosomeSynonym(baseUrl, overlapSpan, genomeVersion, contextRootKey));
        uriBuilder.addParameter(START, String.valueOf(min));
        uriBuilder.addParameter(END, String.valueOf(max));
        String validUrl = checkValidAndSetUrl(uriBuilder.toString());
        CloseableHttpClient httpClient = HttpClients.createDefault();
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == HTTP_RESPONSE_OKAY) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
        HttpGet httpget = new HttpGet(validUrl);
        String responseBody = httpClient.execute(httpget, responseHandler);
        if(trackType.equalsIgnoreCase(GENE_PRED)){
            TrackDataDetails<GenePred> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<GenePred>>(){}.getType());
            if(Objects.nonNull(trackDataDetails))
                trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
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
        else if(trackType.equalsIgnoreCase(PSL)){
            TrackDataDetails<Psl> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<Psl>>(){}.getType());
            if(Objects.nonNull(trackDataDetails))
                trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
            List<Psl> trackDataList= trackDataDetails.getTrackData();
            List<UcscPslSym> ucscPslSyms = trackDataList.stream().map(trackData -> {
                String strandstring = trackData.strand;
                boolean same_orientation = true, qforward = true, tforward = true;
                if (strandstring.length() == 1) {
                    same_orientation = strandstring.equals("+");
                    qforward = (strandstring.charAt(0) == '+');
                    tforward = true;
                } else if (strandstring.length() == 2) {
                    same_orientation = (strandstring.equals("++") || strandstring.equals("--"));
                    qforward = (strandstring.charAt(0) == '+');
                    tforward = (strandstring.charAt(1) == '+');
                }
                BioSeq qseq = determineSeq(genomeVersion, trackData.qName, trackData.qSize);
                BioSeq tseq = determineSeq(genomeVersion, trackData.tName, trackData.tSize);
                List<Object> child_arrays = AbstractPSLParser.calcChildren(qseq, tseq, qforward, tforward,
                        trackData.blockSizes.split(","), trackData.qStarts.split(","), trackData.tStarts.split(","));
                int[] blocksizes = (int[]) child_arrays.get(0);
                int[] qmins = (int[]) child_arrays.get(1);
                int[] tmins = (int[]) child_arrays.get(2);
                return new UcscPslSym(track, trackData.matches, trackData.misMatches, trackData.repMatches, trackData.nCount,
                        trackData.qNumInsert, trackData.qBaseInsert, trackData.tNumInsert, trackData.tBaseInsert, same_orientation,
                        qseq, trackData.qStart, trackData.qEnd, tseq, trackData.tStart, trackData.tEnd,
                        trackData.blockCount, blocksizes, qmins, tmins, false);
            }).collect(Collectors.toList());
            return ucscPslSyms;
        }
        else if(BED_FORMATS.contains(trackType.toLowerCase())) {
            TrackDataDetails<BedTrackTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<BedTrackTypeData>>(){}.getType());
            if(Objects.nonNull(trackDataDetails))
                trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
            List<BedTrackTypeData> trackDataList= trackDataDetails.getTrackData();
            List<UcscBedSymWithProps> ucscBedSymWithProps = trackDataList.stream().map(trackData -> {
                boolean forward = (trackData.getStrand() == null) || trackData.getStrand().isEmpty() || (trackData.getStrand().equals("+") || trackData.getStrand().equals("++"));
                int txMin = Math.min(trackData.getChromStart(), trackData.getChromEnd());
                int txMax = Math.max(trackData.getChromStart(), trackData.getChromEnd());
                int[] blockMins = trackData.getChromStartsArray() != null
                        ? BedParser.makeBlockMins(txMin, trackData.getChromStartsArray())
                        : new int[]{txMin};
                int[] blockMaxs = trackData.getBlockSizesArray() != null
                        ? BedParser.makeBlockMaxs(trackData.getBlockSizesArray(), blockMins)
                        : new int[]{txMax};
                return new UcscBedSymWithProps(track, determineSeq(genomeVersion, trackData.getChrom(), 0),
                        txMin, txMax, trackData.getName(), trackData.getScore(),
                        forward, trackData.getThickStart(), trackData.getThickEnd(),
                        blockMins, blockMaxs, trackData.getProps());
            }).collect(Collectors.toList());
            return ucscBedSymWithProps;
        } else if (trackType.equalsIgnoreCase(BIG_WIG) || trackType.equalsIgnoreCase(WIG)) {
            TrackDataDetails<WigTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<WigTypeData>>(){}.getType());
            if(Objects.nonNull(trackDataDetails))
                trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
            List<WigTypeData> trackDataList= trackDataDetails.getTrackData();
            int[] xData = trackDataList.stream().mapToInt(WigTypeData::getStart).toArray();
            int[] wData = trackDataList.stream().mapToInt(track -> track.getEnd() - track.getStart()).toArray();
            float[] yData = new float[trackDataList.size()];
            for(int i=0; i<trackDataList.size(); i++){
                yData[i] = trackDataList.get(i).getValue();
            }
            if(trackType.equalsIgnoreCase(WIG))
                WiggleData.sortXYZDataOnX(xData, yData, wData);
            GraphIntervalSym graphIntervalSym = new GraphIntervalSym(xData, wData, yData, track, overlapSpan.getBioSeq());
            List<SeqSymmetry> symList = new ArrayList<>();
            symList.add(graphIntervalSym);
            return symList;
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

    private String getChromosomeSynonym(String baseUrl, SeqSpan span, GenomeVersion genomeVersion, String contextRootKey) {
        BioSeq currentSeq = span.getBioSeq();
        if (chromosomes == null) {
            chromosomes = UCSCRestServerUtils.retrieveAssemblyInfoByContextRoot(baseUrl, contextRootKey).keySet();
        }
        GenomeVersionSynonymLookup genomeVersionSynonymLookup = genomeVersion.getGenomeVersionSynonymLookup();
        return genomeVersionSynonymLookup.findMatchingSynonym(chromosomes, currentSeq.getId());
    }

    private static BioSeq determineSeq(GenomeVersion query_group, String qname, int qsize) {
        BioSeq qseq = query_group.getSeq(qname);
        if (qseq == null) {
            qseq = query_group.addSeq(qname, qsize);
        }
        if (qseq.getLength() < qsize) {
            qseq.setLength(qsize);
        }
        return qseq;
    }
}
