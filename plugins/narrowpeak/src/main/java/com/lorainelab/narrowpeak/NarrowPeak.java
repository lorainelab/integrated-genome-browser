package com.lorainelab.narrowpeak;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.BioSeqComparator;
import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.symloader.BedUtils;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.NarrowPeakSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.PEAK;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.P_VALUE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.Q_VALUE;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SIGNAL_VALUE;
import com.affymetrix.genometry.util.LoadUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.LoggerFactory;

/**
 * @author dcnorris
 */
public class NarrowPeak extends SymLoader {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NarrowPeak.class);

    private static final List<LoadUtils.LoadStrategy> SUPPORTED_LOAD_STRATEGIES = Lists.newArrayList(
            LoadUtils.LoadStrategy.NO_LOAD,
            LoadUtils.LoadStrategy.VISIBLE,
            LoadUtils.LoadStrategy.GENOME);
    private final String trackName;
    private final Map<String, BioSeq> chromosomeReference;
    private final List<BioSeq> chromosomes;

    private static final Predicate<String> IS_NOT_TRACK_LINE = line -> !line.startsWith("track");
    private static final Predicate<String> IS_NOT_COMMENT_LINE = line -> !line.startsWith("#");
    private static final Predicate<String> IS_NOT_BROWSER_LINE = line -> !line.startsWith("browser");
    private static final Predicate<String> IS_PARSEABLE_LINE = IS_NOT_COMMENT_LINE.and(IS_NOT_TRACK_LINE).and(IS_NOT_BROWSER_LINE);

    NarrowPeak(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
        trackName = uri.toString();
        chromosomes = Lists.newArrayList();
        chromosomeReference = Maps.newHashMap();
        parseTrackLine();
        initializeChromosomes();
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        return chromosomes;
    }

    @Override
    public List<LoadUtils.LoadStrategy> getLoadChoices() {
        return SUPPORTED_LOAD_STRATEGIES;
    }

    private void initializeChromosomes() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            bufferedReader.lines()
                    .map(line -> line.trim())
                    .filter(IS_PARSEABLE_LINE)
                    .forEach(line -> {
                        Iterable<String> splitLine = Splitter.on("\t").trimResults().split(line);
                        final Iterator<String> iterator = splitLine.iterator();
                        if (iterator.hasNext()) {
                            String chromosome = iterator.next();
                            BioSeq seq = genomeVersion.getSeq(chromosome);
                            if (seq != null) {
                                if (!chromosomes.contains(seq)) {
                                    chromosomes.add(seq);
                                    chromosomeReference.put(chromosome, seq);
                                }
                            }
                        }

                    });
            Collections.sort(chromosomes, new BioSeqComparator());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public List<SeqSymmetry> getGenome() throws Exception {
        List<SeqSymmetry> dataModel = new ArrayList<>();
        for (BioSeq chromosome : chromosomes) {
            dataModel.addAll(getChromosome(chromosome));
        }
        return dataModel;
    }

    @Override
    public List<SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        return parse(seq, seq.getMin(), seq.getMax());
    }

    @Override
    public List<SeqSymmetry> getRegion(SeqSpan span) throws Exception {
        return parse(span.getBioSeq(), span.getMin(), span.getMax());
    }

    private List<SeqSymmetry> parse(BioSeq seq, int requestMin, int requestMax) {
        List<SeqSymmetry> dataModelContent = Lists.newArrayList();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            Iterator<String> iterator = bufferedReader.lines().iterator();
            while (iterator.hasNext()) {
                String line = iterator.next().trim();
                if (IS_PARSEABLE_LINE.test(line)) {
                    if (!parseLineToDataModel(line, requestMin, requestMax, dataModelContent)) {
                        break;
                    }
                }
            }
            Collections.sort(chromosomes, new BioSeqComparator());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return dataModelContent;
    }

    private boolean parseLineToDataModel(String line, int requestMin, int requestMax, List<SeqSymmetry> dataModelContent) {
        List<String> fields = Splitter.on("\t").trimResults().splitToList(line);
        if (fields.size() < 3) {
            logger.error("line in narrowpeak file could not be visualized, missing required columns: {}", line);
            return true;
        }
        String chrom = fields.get(0);
        int chromStart = Integer.parseInt(fields.get(1)); // start field
        int chromEnd = Integer.parseInt(fields.get(2)); // stop field
        String name = null;
        boolean isForwardStrand;
        if (fields.size() >= 4) {
            name = fields.get(3);
        }
        float score = Float.NEGATIVE_INFINITY;
        if (fields.size() >= 5) {
            score = BedUtils.parseScore(fields.get(4));
        }
        if (fields.size() >= 6) {
            if (fields.get(5).equals(".")) {
                isForwardStrand = (chromStart <= chromEnd);
            } else {
                isForwardStrand = fields.get(5).equals("+");
            }
        } else {
            isForwardStrand = (chromStart <= chromEnd);
        }
        int min = Math.min(chromStart, chromEnd);
        int max = Math.max(chromStart, chromEnd);

        if (!BedUtils.checkRange(min, max, requestMin, requestMax)) {
            return max <= requestMax;
        }

        String signalValue = "";
        if (fields.size() >= 7) {
            signalValue = fields.get(6);
        }
        float pValue = Float.NEGATIVE_INFINITY;
        if (fields.size() >= 8) {
            pValue = BedUtils.parseScore(fields.get(7));
        }
        float qValue = Float.NEGATIVE_INFINITY;
        if (fields.size() >= 9) {
            qValue = BedUtils.parseScore(fields.get(8));
        }

        int peakStart = Integer.MIN_VALUE;
        int peakStop = Integer.MIN_VALUE;
        if (fields.size() >= 10) {
            peakStart = Integer.parseInt(fields.get(9));
            peakStart = peakStart + min;
            peakStop = peakStart + 1;
        }
        int[] blockMins = new int[1];
        blockMins[0] = min;
        int[] blockMaxs = new int[1];
        blockMaxs[0] = max;
        NarrowPeakSym narrowPeakSym = new NarrowPeakSym(uri.toString().toLowerCase(), chromosomeReference.get(chrom), min, max, name, score, isForwardStrand, peakStart, peakStop, blockMins, blockMaxs);
        if (!signalValue.isEmpty()) {
            narrowPeakSym.setProperty(SIGNAL_VALUE, signalValue);
        }
        if (peakStart != Integer.MIN_VALUE) {
            narrowPeakSym.setProperty(PEAK, peakStart);
        }
        if (pValue != Float.NEGATIVE_INFINITY) {
            narrowPeakSym.setProperty(P_VALUE, pValue);
        }
        if (qValue != Float.NEGATIVE_INFINITY) {
            narrowPeakSym.setProperty(Q_VALUE, qValue);
        }
        dataModelContent.add(narrowPeakSym);
        return true;
    }

    private void parseTrackLine() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String firstLine = bufferedReader.readLine();
            if (!IS_NOT_TRACK_LINE.test(firstLine)) {
                TrackLineParser trackLineParser = new TrackLineParser(firstLine);
                Map<String, String> trackLineContent = trackLineParser.getTrackLineContent();
                //...this sets global state and is poor design... TODO refactor track style abstractions to resolve this mess
                TrackLineParser.createTrackStyle(trackLineContent, trackName, "narrowPeak");
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
