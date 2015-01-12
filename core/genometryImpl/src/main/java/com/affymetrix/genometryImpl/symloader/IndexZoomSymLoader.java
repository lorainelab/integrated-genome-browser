package com.affymetrix.genometryImpl.symloader;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.util.BlockCompressedFilePointerUtil;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.DepthOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.impl.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.BlockCompressedStreamPosition;
import com.affymetrix.genometryImpl.util.SynonymLookup;

public abstract class IndexZoomSymLoader extends SymLoader {

    private static final int BIN_COUNT = 32768; // smallest bin
    private static final int BIN_LENGTH = 16384; // smallest bin
    private BioSeq saveSeq;
    private GraphSym saveSym;

    public IndexZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, featureName, group);
    }

    protected abstract SymLoader getDataFileSymLoader() throws Exception;

    protected abstract Iterator<Map<Integer, List<List<Long>>>> getBinIter(String seq);

    protected float getRealAvg(SimpleSeqSpan span) throws Exception {
        SymLoader symL = getDataFileSymLoader();
        @SuppressWarnings("unchecked")
        List<SeqSymmetry> symList = (List<SeqSymmetry>) symL.getRegion(span);
        if (symList.isEmpty()) {
            return 0.0f;
        }
        Operator depthOperator = new DepthOperator(FileTypeCategory.Alignment);
        GraphIntervalSym sym = (GraphIntervalSym) depthOperator.operate(span.getBioSeq(), symList);
        float total = 0.0f;
        for (int i = 0; i < sym.getPointCount(); i++) {
            total += sym.getGraphYCoord(i) * sym.getGraphWidthCoord(i);
        }
        return total / span.getLength();
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        init();
        BioSeq seq = overlapSpan.getBioSeq();
        if (!seq.equals(saveSeq) || saveSym == null) {
            Iterator<Map<Integer, List<List<Long>>>> binIter = getBinIter(seq.toString());
            if (binIter == null) {
                saveSym = new GraphSym(new int[]{}, new int[]{}, new float[]{}, featureName, seq);
            } else {
                int[] xList = new int[BIN_COUNT];
                for (int i = 0; i < BIN_COUNT; i++) {
                    xList[i] = i * BIN_LENGTH;
                }
                int[] wList = new int[BIN_COUNT];
                Arrays.fill(wList, BIN_LENGTH);
                float[] yList = new float[BIN_COUNT];
                Arrays.fill(yList, 0.0f);
                float largestY = Float.MIN_VALUE;
                int indexLargest = -1;
                while (binIter.hasNext()) {
                    Map<Integer, List<List<Long>>> binWrapper = binIter.next();
                    int binNo = binWrapper.keySet().iterator().next();
                    int[] region = getRegion(binNo);
                    int yValue = 0;
                    for (List<Long> chunkWrapper : binWrapper.get(binNo)) {
                        if (chunkWrapper != null) {
                            yValue += (double) (getUncompressedLength(chunkWrapper.get(0), chunkWrapper.get(1)) * BIN_LENGTH) / (double) (region[1] - region[0]);
                        }
                    }
                    if (1 + region[1] - region[0] == BIN_LENGTH && yValue > 0.0f) { // smallest bin
                        if (yValue > largestY || indexLargest == -1) {
                            indexLargest = region[0] / BIN_LENGTH;
                            largestY = yValue;
                        }
                    }
                    for (int i = region[0] / BIN_LENGTH; i < (region[1] + 1) / BIN_LENGTH; i++) {
                        yList[i] += yValue;
                    }
                }
                indexLargest = -1;//skip for now
                if (indexLargest != -1) {
                    try {
                        float realAvg = getRealAvg(new SimpleSeqSpan(indexLargest * BIN_LENGTH, (indexLargest + 1) * BIN_LENGTH, seq));
                        if (realAvg > 0) {
                            float ratio = realAvg / yList[indexLargest];
                            for (int i = 0; i < yList.length; i++) {
                                yList[i] *= ratio;
                            }
                        }
                    } catch (Exception x) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "fail loading BAM segment " + uri, x);
                    }
                }
                saveSym = new GraphSym(xList, wList, yList, featureName, seq);
            }
            saveSeq = seq;
        }
        List<SeqSymmetry> symList = new ArrayList<>();
        symList.add(saveSym);
        return symList;
    }

    private static long getUncompressedLength(long chunkStart, long chunkEnd) {
        BlockCompressedStreamPosition start = new BlockCompressedStreamPosition(BlockCompressedFilePointerUtil.getBlockAddress(chunkStart), BlockCompressedFilePointerUtil.getBlockOffset(chunkStart));
        BlockCompressedStreamPosition end = new BlockCompressedStreamPosition(BlockCompressedFilePointerUtil.getBlockAddress(chunkEnd), BlockCompressedFilePointerUtil.getBlockOffset(chunkEnd));
        return Math.max(0, end.getApproximatePosition() - start.getApproximatePosition());
    }

    private static int[] getRegion(int binno) {
        int counter = 0;
        int idx = -3;
        int[] span = null;
        while (span == null) {
            idx += 3;
            int base = (int) Math.pow(2, idx);
            if (counter + base > binno) {
                int mod = binno - counter;
                int lvl = (int) Math.pow(2, 29 - idx);
                span = new int[]{lvl * mod, lvl * (mod + 1) - 1};
            } else {
                counter += base;
            }
        }
        return span;
    }

    protected Map<String, String> getSynonymMap() {
        final SynonymLookup chromosomeLookup = SynonymLookup.getChromosomeLookup();
        return new AbstractMap<String, String>() {
            @Override
            public Set<java.util.Map.Entry<String, String>> entrySet() {
                return null;
            }

            @Override
            public String get(Object key) {
                return chromosomeLookup.getPreferredName((String) key);
            }
        };
    }

    public static void main(String[] args) {
        for (int i = 0; i < 37449; i++) {
            int[] r = getRegion(i);
            System.out.println("" + i + ":" + r[0] + "-" + r[1]);
        }
        /*
         int i = 0;
         boolean found = false;
         while (!found) {
         i++;
         int[] r = getRegion(i);
         if (r[0] == 22511616) {
         System.out.println("region = " + i);
         found = true;
         }
         }
         */
    }
}
