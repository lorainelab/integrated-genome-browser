package com.gene.bigbedhandler;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.BedParser;
import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BPTreeChildNodeItem;
import org.broad.igv.bbfile.BPTreeLeafNodeItem;
import org.broad.igv.bbfile.BPTreeNode;
import org.broad.igv.bbfile.BedFeature;
import org.broad.igv.bbfile.BigBedIterator;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import java.util.Arrays;

public class BigBedSymLoader extends SymLoader {

    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadStrategy.GENOME);
    }
    private BBFileReader bbReader;
    private BBFileHeader bbFileHdr;
    private List<BioSeq> chromosomeList;
    private Map<String, String> cleanSeq2Seq;

    public BigBedSymLoader(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, featureName, genomeVersion);
    }

    private void initbbReader() {
        String uriString = uri.toString();
        try {
            bbReader = new BBFileReader(uriString, SeekableStreamFactory.getInstance().getStreamFor(uriString));
        } catch (IOException x) {
            Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
        }
        if (!bbReader.isBigBedFile()) {
            throw new IllegalStateException("Big Bed processor cannot open " + uri.toString());
        }
        bbFileHdr = bbReader.getBBFileHeader();
        if (bbFileHdr.getVersion() < 3) {
            ErrorHandler.errorPanel("file version not supported " + bbFileHdr.getVersion());
            throw new UnsupportedOperationException("file version not supported " + bbFileHdr.getVersion());
        }
    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public void init() {
        if (this.isInitialized) {
            return;
        }

        initbbReader();

        Map<String, BioSeq> seqMap = new HashMap<>();
        for (BioSeq seq : genomeVersion.getSeqList()) {
            seqMap.put(seq.getId(), seq);
        }
        chromosomeList = new ArrayList<>();
        cleanSeq2Seq = new HashMap<>();
        Map<String, Integer> chromosomeNameMap = new HashMap<>();
        findAllChromosomeNamesAndSizes(bbReader.getChromosomeIDTree().getRootNode(), chromosomeNameMap);

        for (String seqID : chromosomeNameMap.keySet()) {
            String cleanSeqID = seqID;
            int pos = seqID.indexOf((char) 0);
            if (pos > -1) {
                cleanSeqID = seqID.substring(0, pos);
            }
            cleanSeq2Seq.put(cleanSeqID, seqID);
            BioSeq seq = seqMap.get(cleanSeqID);
            if (seq == null) {
                chromosomeList.add(genomeVersion.addSeq(cleanSeqID, chromosomeNameMap.get(seqID), uri.toString()));
            } else {
                chromosomeList.add(seq);
            }
        }
        this.isInitialized = true;
    }

    @Override
    public List<BioSeq> getChromosomeList() {
        init();
        return chromosomeList;
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() {
        init();
        List<BioSeq> allSeq = getChromosomeList();
        List<SeqSymmetry> retList = new ArrayList<>();
        for (BioSeq seq : allSeq) {
            retList.addAll(getChromosome(seq));
        }
        return retList;
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
        init();
        String seqString = cleanSeq2Seq.get(seq.getId());
        return parse(seq, bbReader.getBigBedIterator(seqString, 0, seqString, Integer.MAX_VALUE, true));
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
        List<? extends SeqSymmetry> regions = null;
        init();
        String seqString = cleanSeq2Seq.get(span.getBioSeq().getId());
        try {
            regions = parse(span.getBioSeq(), bbReader.getBigBedIterator(seqString, span.getStart(), seqString, span.getEnd(), false));
        } catch (RuntimeException x) {
            if (x.getMessage().startsWith("No wig data found")) {
                Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.WARNING, x.getMessage());
                regions = new ArrayList<>();
            } else {
                throw x;
            }
        }
        return regions;
    }

    private List<? extends SeqSymmetry> parse(BioSeq seq, BigBedIterator bedIterator) {
        List<SeqSymmetry> symList = new ArrayList<>();
        int definedFieldCount = bbReader.getBBFileHeader().getDefinedFieldCount();
        int fieldCount = bbReader.getBBFileHeader().getFieldCount();
        try {
            BedFeature bedFeature = null;
            while (bedIterator.hasNext() && (!Thread.currentThread().isInterrupted())) {
                bedFeature = bedIterator.next();
                if (bedFeature == null) {
                    break;
                }
                if (definedFieldCount < 3) {
                    return symList;
                }

                String seq_name = null;
                String geneName = null;
                int min;
                int max;
                String itemRgb = "";
                int thick_min = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_min is not used
                int thick_max = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_max is not used
                float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
                boolean forward;
                int[] blockSizes = null;
                int[] blockStarts = null;
                int[] blockMins = null;
                int[] blockMaxs = null;

                seq_name = bedFeature.getChromosome(); // seq id field
                int beg = bedFeature.getStartBase(); // start field
                int end = bedFeature.getEndBase(); // stop field
                if (definedFieldCount >= 4) {
                    geneName = bedFeature.getRestOfFields()[0];
                    if (geneName == null || geneName.length() == 0) {
                        geneName = genomeVersion.getName();
                    }
                }
                if (definedFieldCount >= 5) {
                    score = Float.parseFloat(bedFeature.getRestOfFields()[1]);
                } // score field
                if (definedFieldCount >= 6) {
                    forward = !(bedFeature.getRestOfFields()[2].equals("-"));
                } else {
                    forward = (beg <= end);
                }

                min = Math.min(beg, end);
                max = Math.max(beg, end);

                if (definedFieldCount >= 8) {
                    thick_min = Integer.parseInt(bedFeature.getRestOfFields()[3]); // thickStart field
                    thick_max = Integer.parseInt(bedFeature.getRestOfFields()[4]); // thickEnd field
                }
                if (definedFieldCount >= 9) {
                    itemRgb = bedFeature.getRestOfFields()[5];
                } 
                if (definedFieldCount >= 12) {
                    int blockCount = Integer.parseInt(bedFeature.getRestOfFields()[6]); // blockCount field
                    blockSizes = BedParser.parseIntArray(bedFeature.getRestOfFields()[7]); // blockSizes field
                    if (blockCount != blockSizes.length) {
                        System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + geneName + " on " + seq_name);
                        continue;
                    }
                    blockStarts = BedParser.parseIntArray(bedFeature.getRestOfFields()[8]); // blockStarts field
                    if (blockCount != blockStarts.length) {
                        System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + geneName + " on " + seq_name);
                        continue;
                    }
                    blockMins = BedParser.makeBlockMins(min, blockStarts);
                    blockMaxs = BedParser.makeBlockMaxs(blockSizes, blockMins);
                } else {
                    /*
                     * if no child blocks, make a single child block the same size as the parent
                     * Very Inefficient, ideally wouldn't do this
                     * But currently need this because of GenericAnnotGlyphFactory use of annotation depth to
                     *     determine at what level to connect glyphs -- if just leave blockMins/blockMaxs null (no children),
                     *     then factory will create a line container glyph to draw line connecting all the bed annots
                     * Maybe a way around this is to adjust depth preference based on overall depth (1 or 2) of bed file?
                     */
                    blockMins = new int[1];
                    blockMins[0] = min;
                    blockMaxs = new int[1];
                    blockMaxs[0] = max;
                }
                if (max > seq.getLength()) {
                    seq.setLength(max);
                }

                SymWithProps bedline_sym;
                bedline_sym = new UcscBedSym(this.bbFileHdr.getPath(), seq, min, max, geneName, score, forward, thick_min, thick_max, blockMins, blockMaxs);
                if (itemRgb != null) {
                    java.awt.Color c = null;
                    try {
                        c = TrackLineParser.reformatColor(itemRgb);
                    } catch (Exception e) {
                        Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.SEVERE, "Could not parse a color from String '{}'", itemRgb);
                    }
                    if (c != null) {
                        bedline_sym.setProperty(TrackLineParser.ITEM_RGB, c);
                    }
                }

                //Add optional fields to sym
                String[] restOfFields = Arrays.copyOfRange(bedFeature.getRestOfFields(),definedFieldCount-3,fieldCount-3);
                if (restOfFields != null) {
                    for (int i = 0; i < restOfFields.length; i++) {
                        if (restOfFields[i] != null && restOfFields[i].trim().length() > 0) {
                            bedline_sym.setProperty("optional field " + (i + 1), restOfFields[i]);
                        }
                    }
                }
                symList.add(bedline_sym);
            }
        } catch (Exception ex) {
            Logger.getLogger(BigBedSymLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return symList;
    }

    @Override
    public List<String> getFormatPrefList() {
        return BigBedHandler.getFormatPrefList();
    }

    /**
     * copied from BPTree.findAllChromosomeNames()
     *
     * @param thisNode BPTree root node
     * @param chromosomeMap passed in map
     */
    public void findAllChromosomeNamesAndSizes(BPTreeNode thisNode, Map<String, Integer> chromosomeMap) {

        // search down the tree recursively starting with the root node
        if (thisNode.isLeaf()) {
            // add all leaf names
            int nLeaves = thisNode.getItemCount();
            for (int index = 0; index < nLeaves; ++index) {

                BPTreeLeafNodeItem leaf = (BPTreeLeafNodeItem) thisNode.getItem(index);
                chromosomeMap.put(leaf.getChromKey(), leaf.getChromSize());
            }
        } else {
            // get all child nodes
            int nNodes = thisNode.getItemCount();
            for (int index = 0; index < nNodes; ++index) {

                BPTreeChildNodeItem childItem = (BPTreeChildNodeItem) thisNode.getItem(index);
                BPTreeNode childNode = childItem.getChildNode();

                // keep going until leaf items are extracted
                findAllChromosomeNamesAndSizes(childNode, chromosomeMap);
            }
        }
    }

    /**
     * Returns "text/bb".
     */
    public String getMimeType() {
        return "text/bb";
    }
}
