package com.gene.tallyhandler;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symloader.LineProcessor;
import com.affymetrix.genometry.symmetry.impl.MisMatchPileupGraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.lorainelab.igb.igb.genoviz.extensions.GraphGlyphUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.broad.tribble.readers.LineReader;

public class TallyLineProcessor implements LineProcessor {

    private static final char[] nucleotides = {'A', 'C', 'G', 'T', 'N'};
    private String featureName;

    public TallyLineProcessor(String featureName) {
        super();
        this.featureName = featureName;
    }

    @Override
    public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) throws Exception {
        List<Integer> xList = new ArrayList<>();
        List<Integer> wList = new ArrayList<>();
        List<Float> yList = new ArrayList<>();
        List<Character> refList = new ArrayList<>();
        Map<Character, List<Integer>> baseTotals = new HashMap<>();
        for (char nucleotide : nucleotides) {
            baseTotals.put(nucleotide, new ArrayList<Integer>());
        }
        String line = null;
        try {
            line = lineReader.readLine();
            while (line != null) {
                String[] parts = line.split("\t");
//				String seq = parts[0];
                int location = Integer.parseInt(parts[1]) - 1;
                String[] countStrings = parts[2].split(" ");
                Map<Character, Integer> baseCounts = new HashMap<>();
                for (char nucleotide : nucleotides) {
                    baseCounts.put(nucleotide, 0);
                }
                for (String countString : countStrings) {
                    char nucleotide = countString.charAt(0);
                    int pos = countString.indexOf('(');
                    if (pos == -1) {
                        pos = countString.length();
                    }
                    int count = Integer.parseInt(countString.substring(1, pos));
                    baseCounts.put(nucleotide, baseCounts.get(nucleotide) + count);
                }
                xList.add(location);
                refList.add(countStrings[0].charAt(0));
                wList.add(1);
                float y = 0f;
                for (char nucleotide : nucleotides) {
                    baseTotals.get(nucleotide).add(baseCounts.get(nucleotide));
                    y += baseCounts.get(nucleotide);
                }
                yList.add(y);
                line = lineReader.readLine();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(
                    Level.SEVERE, "error reading tally file with linereader for line " + line, ex);
            line = null;
        }
        MisMatchPileupGraphSym sym = new MisMatchPileupGraphSym(
                GraphGlyphUtils.intListToArray(xList),
                GraphGlyphUtils.intListToArray(wList),
                GraphGlyphUtils.floatListToArray(yList),
                GraphGlyphUtils.intListToArray(baseTotals.get('A')),
                GraphGlyphUtils.intListToArray(baseTotals.get('T')),
                GraphGlyphUtils.intListToArray(baseTotals.get('G')),
                GraphGlyphUtils.intListToArray(baseTotals.get('C')),
                GraphGlyphUtils.intListToArray(baseTotals.get('N')),
                featureName,
                seq,
                charListToArray(refList)
        );
        List<SeqSymmetry> syms = new ArrayList<>();
        syms.add(sym);
        return syms;
    }

    private char[] charListToArray(List<Character> list) {
        char[] array = new char[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    @Override
    public void init(URI uri) throws Exception {
    }

    @Override
    public List<String> getFormatPrefList() {
        return TallyHandler.getFormatPrefList();
    }

    @Override
    public SeqSpan getSpan(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\t");
        BioSeq seq = GenometryModel.getInstance().getSelectedGenomeVersion().getSeq(parts[0]);
        int location = Integer.parseInt(parts[1]);
        return new SimpleSeqSpan(location, location + 1, seq);
    }

    @Override
    public boolean processInfoLine(String line, List<String> infoLines) {
        return false;
    }

    @Override
    public boolean isMultiThreadOK() {
        return true;
    }
}
