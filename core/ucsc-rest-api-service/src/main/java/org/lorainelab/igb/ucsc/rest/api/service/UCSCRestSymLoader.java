package org.lorainelab.igb.ucsc.rest.api.service;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.AbstractPSLParser;
import com.affymetrix.genometry.parsers.BedParser;
import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.parsers.graph.WiggleData;
import com.affymetrix.genometry.symloader.BedUtils;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.*;
import org.lorainelab.igb.ucsc.rest.api.service.utils.ApiResponseHandler;
import org.lorainelab.igb.ucsc.rest.api.service.utils.UCSCRestServerUtils;

import java.awt.Color;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.affymetrix.genometry.tooltip.ToolTipConstants.*;
import static org.lorainelab.igb.ucsc.rest.api.service.model.TrackDataDetails.*;

@Slf4j
public class UCSCRestSymLoader extends SymLoader {
    public static final String CHROM = "chrom";
    public static final String START = "start";
    public static final String END = "end";
    private final String baseUrl;
    private final String track;
    private final String trackType;
    private final String contextRoot;
    private final String contextRootKey;
    private Set<String> chromosomes;
    private TrackDetails trackDetails;

    public UCSCRestSymLoader(String baseUrl, URI contextRoot, Optional<URI> indexUri, String track, String trackType, TrackDetails trackDetails, GenomeVersion genomeVersion, String contextRootKey) {
        super(contextRoot, indexUri, track, genomeVersion);
        this.baseUrl = baseUrl;
        this.track = track;
        this.trackType = trackType;
        this.contextRoot = contextRoot.toString();
        this.contextRootKey = contextRootKey;
        this.trackDetails = trackDetails;
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        final int min = overlapSpan.getMin() == 0 ? 1 : overlapSpan.getMin();
        final int max = overlapSpan.getMax() - 1;
        URIBuilder uriBuilder = new URIBuilder(contextRoot);
        uriBuilder.addParameter(CHROM, getChromosomeSynonym(baseUrl, overlapSpan, genomeVersion, contextRootKey));
        uriBuilder.addParameter(START, String.valueOf(min));
        uriBuilder.addParameter(END, String.valueOf(max));
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(uriBuilder.toString());
            String responseBody = httpClient.execute(httpget, new ApiResponseHandler());
            if (trackType.equalsIgnoreCase(GENE_PRED)) {
                TrackDataDetails<GenePred> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<GenePred>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<GenePred> trackDataList = trackDataDetails.getTrackData();
                List<UcscBedSymWithProps> ucscGeneSyms = new ArrayList<>();
                if(!trackDataList.isEmpty()) {
                    ucscGeneSyms = trackDataList.stream().map(trackData -> {
                        boolean forward = (trackData.getStrand() == null) || trackData.getStrand().isEmpty() || (trackData.getStrand().equals("+") || trackData.getStrand().equals("++"));
                        int txMin = Math.min(trackData.getTxStart(), trackData.getTxEnd());
                        int txMax = Math.max(trackData.getTxStart(), trackData.getTxEnd());
                        int[] emins = Arrays.stream(trackData.getExonStarts().split(",")).mapToInt(Integer::parseInt).toArray();
                        int[] emaxs = Arrays.stream(trackData.getExonEnds().split(",")).mapToInt(Integer::parseInt).toArray();
                        return new UcscBedSymWithProps(track, determineSeq(genomeVersion, trackData.getChrom(), 0),
                                txMin, txMax, trackData.getName(), trackData.getScore(),
                                forward, trackData.getCdsStart(), trackData.getCdsEnd(), emins, emaxs, null, trackData.getName2());
                    }).collect(Collectors.toList());
                }
                return ucscGeneSyms;
            } else if (trackType.equalsIgnoreCase(PSL)) {
                TrackDataDetails<Psl> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<Psl>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<Psl> trackDataList = trackDataDetails.getTrackData();
                List<UcscPslSym> ucscPslSyms = new ArrayList<>();
                if(!trackDataList.isEmpty()) {
                    ucscPslSyms = trackDataList.stream().map(this::getUcscPslSym).collect(Collectors.toList());
                }
                return ucscPslSyms;
            } else if (BED_FORMATS.contains(trackType.toLowerCase())) {
                TrackDataDetails<BedTrackTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<BedTrackTypeData>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<BedTrackTypeData> trackDataList = trackDataDetails.getTrackData();
                List<UcscBedSymWithProps> ucscBedSymWithProps = new ArrayList<>();
                if(!trackDataList.isEmpty()) {
                    ucscBedSymWithProps = trackDataList.stream().map(trackData -> {
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
                                blockMins, blockMaxs, trackData.getProps(), trackData.getGeneName());
                    }).collect(Collectors.toList());
                }
                return ucscBedSymWithProps;
            } else if (trackType.equalsIgnoreCase(BIG_WIG) || trackType.equalsIgnoreCase(WIG)) {
                TrackDataDetails<WigTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<WigTypeData>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<WigTypeData> trackDataList = trackDataDetails.getTrackData();
                int[] xData = trackDataList.stream().mapToInt(WigTypeData::getStart).toArray();
                int[] wData = trackDataList.stream().mapToInt(track -> track.getEnd() - track.getStart()).toArray();
                float[] yData = new float[trackDataList.size()];
                for (int i = 0; i < trackDataList.size(); i++) {
                    yData[i] = trackDataList.get(i).getValue();
                }
                if (trackType.equalsIgnoreCase(WIG))
                    WiggleData.sortXYZDataOnX(xData, yData, wData);
                GraphIntervalSym graphIntervalSym = new GraphIntervalSym(xData, wData, yData, track, overlapSpan.getBioSeq());
                List<SeqSymmetry> symList = new ArrayList<>();
                symList.add(graphIntervalSym);
                return symList;
            } else if (trackType.equalsIgnoreCase(NARROW_PEAK)) {
                TrackDataDetails<NarrowPeakTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<NarrowPeakTypeData>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<NarrowPeakTypeData> trackDataList = trackDataDetails.getTrackData();
                List<NarrowPeakSym> narrowPeakSyms = new ArrayList<>();
                if(!trackDataList.isEmpty())  {
                    narrowPeakSyms = trackDataList.stream().map(this::getNarrowPeakSym).collect(Collectors.toList());
                }
                return narrowPeakSyms;
            } else if (BAR_CHART_FORMATS.contains(trackType.toLowerCase())) {
                TrackDataDetails<BarChartTypeData> trackDataDetails = new Gson().fromJson(responseBody, new TypeToken<TrackDataDetails<BarChartTypeData>>() {
                }.getType());
                if (Objects.nonNull(trackDataDetails))
                    trackDataDetails.setTrackData(responseBody, track, trackType, trackDataDetails.getChrom());
                List<BarChartTypeData> trackDataList = trackDataDetails.getTrackData();
                List<UcscBedSymWithProps> ucscBedSymsWithProps = new ArrayList<>();
                if(!trackDataList.isEmpty()) {
                     trackDataList.forEach(trackData -> getBarChartBedSym(trackData, ucscBedSymsWithProps));
                }
                return ucscBedSymsWithProps;
            }
        }
        return null;
    }

    private void getBarChartBedSym(BarChartTypeData trackData, List<UcscBedSymWithProps> ucscBedSymsWithProps) {
        boolean forward = (trackData.getStrand() == null) || trackData.getStrand().isEmpty() || (trackData.getStrand().equals("+") || trackData.getStrand().equals("++"));
        int txMin = Math.min(trackData.getChromStart(), trackData.getChromEnd());
        int txMax = Math.max(trackData.getChromStart(), trackData.getChromEnd());
        String[] barChartBarCategories = Objects.nonNull(trackDetails) ? trackDetails.getBarChartBarCategories() : null;
        String[] barChartColors = Objects.nonNull(trackDetails) ? trackDetails.getBarChartColors() : null;
        float[] expScoresArray = trackData.getExpScoresArray();
        if(Objects.nonNull(barChartBarCategories) && Objects.nonNull(expScoresArray) && Objects.nonNull(barChartColors)){
            for(int i = 0; i< trackData.getExpCount(); i++){
                Map<String, Object> props = new HashMap<>();
                props.put("name2", trackData.getName2());
                Color decodedRGBColor = Color.decode(barChartColors[i]);
                props.put(TrackLineParser.ITEM_RGB, decodedRGBColor);
                ucscBedSymsWithProps.add(new UcscBedSymWithProps(track, determineSeq(genomeVersion, trackData.getChrom(), 0),
                                txMin, txMax, barChartBarCategories[i], expScoresArray[i],
                                forward, Integer.MIN_VALUE, Integer.MIN_VALUE,
                                new int[]{txMin}, new int[]{txMax}, props, trackData.getName()));
            }
        }else {
            ucscBedSymsWithProps.add(new UcscBedSymWithProps(track, determineSeq(genomeVersion, trackData.getChrom(), 0),
                     txMin, txMax, trackData.getName(), trackData.getScore(),
                     forward, Integer.MIN_VALUE, Integer.MIN_VALUE,
                     new int[]{txMin}, new int[]{txMax}, null, trackData.getName2()));
        }
    }

    private UcscPslSym getUcscPslSym(Psl trackData) {
        String strandstring = trackData.strand;
        boolean same_orientation = true, qforward = true, tforward = true;
        if (strandstring.length() == 1) {
            same_orientation = strandstring.equals("+");
            qforward = (strandstring.charAt(0) == '+');
        } else if (strandstring.length() == 2) {
            same_orientation = (strandstring.equals("++") || strandstring.equals("--"));
            qforward = (strandstring.charAt(0) == '+');
            tforward = (strandstring.charAt(1) == '+');
        }
        BioSeq qseq = determineSeq(genomeVersion, trackData.qName, trackData.qSize);
        BioSeq tseq = determineSeq(genomeVersion, trackData.tName, trackData.tSize);
        List<Object> child_arrays = AbstractPSLParser.calcChildren(qseq, tseq, qforward, tforward,
                trackData.blockSizes.split(","), trackData.qStarts.split(","), trackData.tStarts.split(","));
        int[] blocksizes = new int[0];
        int[] qmins = new int[0];
        int[] tmins = new int[0];
        if (child_arrays != null) {
            blocksizes = (int[]) child_arrays.get(0);
            qmins = (int[]) child_arrays.get(1);
            tmins = (int[]) child_arrays.get(2);
        }
        return new UcscPslSym(track, trackData.matches, trackData.misMatches, trackData.repMatches, trackData.nCount,
                trackData.qNumInsert, trackData.qBaseInsert, trackData.tNumInsert, trackData.tBaseInsert, same_orientation,
                qseq, trackData.qStart, trackData.qEnd, tseq, trackData.tStart, trackData.tEnd,
                trackData.blockCount, blocksizes, qmins, tmins, false);
    }

    private NarrowPeakSym getNarrowPeakSym(NarrowPeakTypeData trackData) {
        int chromMin = Math.min(trackData.chromStart, trackData.chromEnd);
        int chromMax = Math.max(trackData.chromStart, trackData.chromEnd);
        if (trackData.name == null || trackData.name.equals("."))
            trackData.name = "";
        float score = BedUtils.parseScore(trackData.score);
        boolean isForwardStrand;
        if (trackData.strand == null || trackData.strand.equals("."))
            isForwardStrand = (trackData.chromStart <= trackData.chromEnd);
        else
            isForwardStrand = trackData.strand.equals("+");
        int peakStart = trackData.peak;
        peakStart = peakStart + chromMin;
        int peakStop = peakStart + 1;
        int[] blockMins = new int[1];
        blockMins[0] = chromMin;
        int[] blockMaxs = new int[1];
        blockMaxs[0] = chromMax;
        NarrowPeakSym narrowPeakSym = new NarrowPeakSym(track, determineSeq(genomeVersion, trackData.getChrom(), 0),
                chromMin, chromMax, trackData.name, score,
                isForwardStrand, peakStart, peakStop, blockMins, blockMaxs);
        if (!trackData.name.isEmpty()) {
            narrowPeakSym.setProperty(ID, trackData.name);
        }
        if (score != Float.NEGATIVE_INFINITY) {
            narrowPeakSym.setProperty(SCORE, score);
        }
        narrowPeakSym.setProperty(SIGNAL_VALUE, trackData.signalValue);
        if (peakStart != Integer.MIN_VALUE) {
            narrowPeakSym.setProperty(PEAK, peakStart);
        }
        if (trackData.pValue != Integer.MIN_VALUE) {
            narrowPeakSym.setProperty(P_VALUE, trackData.pValue);
        }
        if (trackData.qValue != Float.NEGATIVE_INFINITY) {
            narrowPeakSym.setProperty(Q_VALUE, trackData.qValue);
        }
        return narrowPeakSym;
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

    public String getTrackType() {
        return trackType;
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
