package com.affymetrix.igb.shared;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.BitSet;

import com.affymetrix.genometryImpl.AminoAcid;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymSpanWithCds;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.util.ColorUtils;

public class CodonGlyph extends AbstractAlignedTextGlyph {

    private static final double STAGGER_COLOR_PCT = 0.8;
    private int offset;
//	private static SeqSpan waitingSpan = null;
//	private static boolean loading = false;

    public static final String CODON_GLYPH_CODE_SIZE = "Codon glyph code size";
    public static final int default_codon_glyph_code_size = 1;

    private int codeSize;
    private boolean tried = false;
    private Color altColor = Color.darkGray;

    public CodonGlyph(int codeSize) {
        super();
        this.codeSize = codeSize;
    }

    @Override
    public void drawTraversal(ViewI view) {
        try {
            if (chariter == null && !tried) {
                String residues = getResidue(view);
                if (residues != null) {
                    setResidues(residues);
                }
                tried = true;
            }
            if (chariter != null) {
                super.drawTraversal(view);
            }
        } catch (Exception ex) {
            System.out.println("Exception in CodonGlyph :" + ex);
        }
    }

    private String repeatString(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return String.valueOf(chars);
    }

    @Override
    protected boolean getShowMask() {
        return false;
    }

    @Override
    public void setBackgroundColor(Color bgColor) {
        super.setBackgroundColor(bgColor);
        if (bgColor == null) {
            altColor = Color.DARK_GRAY;
            return;
        }
        //altColor = new Color((int)(bgColor.getRed() * STAGGER_COLOR_PCT), (int)(bgColor.getGreen() * STAGGER_COLOR_PCT), (int)(bgColor.getBlue() * STAGGER_COLOR_PCT));
        altColor = ColorUtils.getAlternateColor(bgColor);
    }

    @Override
    protected void drawResidueRectangles(ViewI view, double pixelsPerBase,
            char[] charArray, int seqBegIndex, int seqEndIndex, BitSet residueMask) {

        if (codeSize == 0 || (view.getTransform()).getScaleX() < 2) {
            return;
        }

        Graphics g = view.getGraphics();
        g.setColor(altColor);

        int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
        int totalOffset = offset + seqBegIndex;
        int mod = totalOffset % 6;
        int startOffset;
        if (mod < 4) {
            startOffset = 3 - mod;
        } else {
            g.fillRect(getPixelBox().x, getPixelBox().y, intPixelsPerBase * (6 - mod), getPixelBox().height);
            startOffset = 9 - mod;
        }
        for (int j = startOffset; j < charArray.length; j += 6) {
            g.fillRect(getPixelBox().x + (int) (j * pixelsPerBase), getPixelBox().y, intPixelsPerBase * Math.min(charArray.length - j, 3), getPixelBox().height);
        }
    }

    private class ResidueRange {

        private final int startPos;
        private final int endPos;
        private final String residue;

        private ResidueRange(int startPos, int endPos, String residue) {
            super();
            this.startPos = startPos;
            this.endPos = endPos;
            this.residue = residue;
        }

        public int getStartPos() {
            return startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public String getResidue() {
            return residue;
        }
    }

    private ResidueRange getResidueRange() {
        SymSpanWithCds parentSym = (SymSpanWithCds) getParent().getParent().getInfo();
//		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
        BioSeq seq = parentSym.getBioSeq();
        SeqSymmetry exonSym = (SeqSymmetry) getInfo();
//		SeqSpan exonSpan = exonSym.getSpan(seq);
        SeqSpan exonSpan = exonSym.getSpan(0);

        if (parentSym.isForward() && exonSpan.getStart() > exonSpan.getEnd()) {
            exonSpan = new SimpleSeqSpan(exonSpan.getEnd(), exonSpan.getStart(), seq);
        } else if (!parentSym.isForward() && exonSpan.getStart() < exonSpan.getEnd()) {
            exonSpan = new SimpleSeqSpan(exonSpan.getEnd(), exonSpan.getStart(), seq);
        }
        StringBuilder codesSB = new StringBuilder("");
        SeqSpan cdsSpan = parentSym.getCdsSpan();
        int startPos = 0;
        int endPos = 0;
        for (int index = 0; index < parentSym.getChildCount(); index++) {
            SeqSymmetry exonLoopSym = parentSym.getChild(index);
            String residue;
            SeqSpan span = exonLoopSym.getSpan(seq);
            MutableSeqSpan dstSpan = new SimpleMutableSeqSpan();
            if (cdsSpan != null) {
                if (SeqUtils.intersection(span, cdsSpan, dstSpan)) {
                    span = dstSpan;
                } else {
                    continue;
                }
            }
            residue = seq.substring(span.getStart(), span.getEnd());
            if (parentSym.isForward()) {
                if (exonSpan.getStart() == span.getStart()) {
                    startPos = codesSB.length();
                }
                codesSB.append(residue);
                if (exonSpan.getEnd() == span.getEnd()) {
                    endPos = codesSB.length();
                }
            } else {
                if (exonSpan.getEnd() == span.getEnd()) {
                    startPos = codesSB.length();
                }
                codesSB.insert(0, residue);
                if (exonSpan.getStart() == span.getStart()) {
                    endPos = codesSB.length();
                }
            }
        }
        if (codesSB.length() % 3 > 0) {
            codesSB.append(repeatString('*', 3 - codesSB.length() % 3));
        }
        return new ResidueRange(startPos, endPos, codesSB.toString());
    }

    private String getResidue(ViewI view) {
        String errorMessage = null;
        SymSpanWithCds parentSym = (SymSpanWithCds) getParent().getParent().getInfo();
//		SeqSpan exonSpan = ((SeqSymmetry)getParent().getInfo()).getSpan(GenometryModel.getGenometryModel().getSelectedSeq());
        ResidueRange residueRange = getResidueRange();
        String residue = residueRange.getResidue();
        if (residue.indexOf('-') > -1) { // load residues
//			if (view != null) {
//				loadInBackground(view, exonSpan);
//			}
            return null;
        }
        if (residue.length() < 6 || residue.length() % 3 != 0) {
            errorMessage = "invalid length " + residue.length();
//			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, errorMessage + " for " + parentSym.getGeneName() + ":" + parentSym.getID());
        } //		else if (!residue.substring(0, 3).equals(AminoAcid.START_CODON)) {
        //			errorMessage = "invalid start " + residue.substring(0, 3);
        //			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, errorMessage + " for " + parentSym.getGeneName() + ":" + parentSym.getID());
        //		}
        else {
//		String endCodon = residue.substring(residue.length() - 3, residue.length());
//			if (!(endCodon.equals(AminoAcid.STOP_CODONS[0]) || endCodon.equals(AminoAcid.STOP_CODONS[1]) || endCodon.equals(AminoAcid.STOP_CODONS[2]))) {
//				errorMessage = "invalid stop " + endCodon;
//				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, errorMessage + " for " + parentSym.getGeneName() + ":" + parentSym.getID());
//			}
        }
        if (errorMessage != null) {
            int spanLength = residueRange.getEndPos() - residueRange.getStartPos();
            if (errorMessage.length() >= spanLength) {
                return errorMessage.substring(0, spanLength);
            } else {
                errorMessage = " " + errorMessage;
                if (spanLength > errorMessage.length()) {
                    errorMessage += " ";
                }
                int extraLength = (spanLength - errorMessage.length()) / 2;
                errorMessage = repeatString('?', extraLength) + errorMessage + repeatString('?', extraLength);
                if (spanLength > errorMessage.length()) {
                    errorMessage += '?';
                }
            }
            return errorMessage;
        }
        StringBuilder aminoAcidsSB = AminoAcid.getAminoAcid(residue, codeSize, parentSym.isForward(), "  ");
        if (aminoAcidsSB.length() == 0) {
            return null;
        }
        offset = residueRange.getStartPos();
//		return aminoAcidsSB.substring(Math.min(residueRange.getStartPos(), residueRange.getEndPos()), Math.max(residueRange.getStartPos(), residueRange.getEndPos()));
        return aminoAcidsSB.substring(residueRange.getStartPos(), residueRange.getEndPos());
    }

    @Override
    protected Color getResidueStringsColor() {
        return getEffectiveContrastColor(getParent().getBackgroundColor());
    }

    /*
     * Calculate the effective contrast color
     * Credit: http://24ways.org/2010/calculating-color-contrast
     */
    protected Color getEffectiveContrastColor(Color color) {
        Color constractColor = default_bg_color;
        if (null != color) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            int yiq = ((red * 299) + (green * 587) + (blue * 114)) / 1000;
            constractColor = (yiq >= 128) ? Color.BLACK : Color.WHITE;
        }
        return constractColor;
    }
    /*
     private synchronized void loadInBackground(final ViewI view, final SeqSpan span) {
     final SeqSpan useSpan = new SimpleSeqSpan(Math.min(span.getStart(), span.getEnd()), Math.max(span.getStart(), span.getEnd()), span.getBioSeq());
     if (loading) {
     if (waitingSpan == null) {
     waitingSpan = useSpan;
     }
     else {
     waitingSpan = new SimpleSeqSpan(Math.min(span.getStart(), waitingSpan.getStart()), Math.max(span.getEnd(), waitingSpan.getEnd()), span.getBioSeq());
     }
     }
     else {
     loading = true;
     SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
     public Boolean doInBackground() {
     return GeneralLoadView.getLoadView().loadResidues(useSpan, true);
     }
	
     @Override
     public synchronized void done() {
     String residues = getResidue(null);
     if (residues != null) {
     setResidues(residues);
     }
     if (chariter != null) {
     CodonGlyph.super.drawTraversal(view);
     }
     loading = false;
     if (waitingSpan != null) {
     SeqSpan tempSpan = waitingSpan;
     waitingSpan = null;
     loadInBackground(view, tempSpan);
     }
     }
     };
     // Use a SwingWorker to avoid locking up the GUI.
     ThreadUtils.getPrimaryExecutor(CodonGlyph.this).execute(worker);
     }
     }
     */
}
