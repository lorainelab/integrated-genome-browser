package com.gene.tallyhandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.LineProcessor;
import com.affymetrix.genometryImpl.symmetry.MisMatchPileupGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.GraphGlyphUtils;

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
        List<Integer> xList = new ArrayList<Integer>();
        List<Integer> wList = new ArrayList<Integer>();
        List<Float> yList = new ArrayList<Float>();
        List<Character> refList = new ArrayList<Character>();
        Map<Character, List<Integer>> baseTotals = new HashMap<Character, List<Integer>>();
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
                Map<Character, Integer> baseCounts = new HashMap<Character, Integer>();
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
        List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
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
        BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeqGroup().getSeq(parts[0]);
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
