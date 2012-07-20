package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.ViewI;

public class CodonGlyph extends AbstractAlignedTextGlyph {
	private static final double STAGGER_COLOR_PCT = 0.8;
	private int offset;
	private CodonGlyph drawCodonGlyph;
//	private static SeqSpan waitingSpan = null;
//	private static boolean loading = false;
	
	public static final String CODON_GLYPH_CODE_SIZE = "Codon glyph code size";
	public static final int default_codon_glyph_code_size = 3;

	public CodonGlyph() {
	super();
	//*	
		PreferenceUtils.getTopNode().putInt(CODON_GLYPH_CODE_SIZE, PreferenceUtils.getIntParam(CODON_GLYPH_CODE_SIZE, default_codon_glyph_code_size));
	}

	@Override
	public void drawTraversal(ViewI view)  {
		if (drawCodonGlyph != null) {
			drawCodonGlyph.drawTraversal(view);
		}
		else {
			setCoordBox(getParent().getCoordBox());
			if (chariter == null) {
				String residues = getResidue(view);
				if (residues != null) {
					setResidues(residues);
				}
			}
			if (chariter != null) {
				super.drawTraversal(view);
			}
		}
	}

	public void setDrawCodonGlyph(CodonGlyph drawCodonGlyph) {
		this.drawCodonGlyph = drawCodonGlyph;
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
	public void draw(ViewI view) {
		if (drawCodonGlyph != null) {
			drawCodonGlyph.drawTraversal(view);
		}
		else {
			super.draw(view);
		}
	}

	@Override
	protected void drawResidueRectangles(
			Graphics g, double pixelsPerBase, char[] charArray, int seqBegIndex, BitSet residueMask, int x, int y, int height, boolean show_mask) {
		if (PreferenceUtils.getIntParam(CODON_GLYPH_CODE_SIZE, default_codon_glyph_code_size) == 0) {
			return;
		}

		Color bgColor = getParent().getBackgroundColor();
		Color altColor = new Color((int)(bgColor.getRed() * STAGGER_COLOR_PCT), (int)(bgColor.getGreen() * STAGGER_COLOR_PCT), (int)(bgColor.getBlue() * STAGGER_COLOR_PCT));
		g.setColor(altColor);
		int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
		int totalOffset = offset + seqBegIndex;
		int mod = totalOffset % 6;
		int startOffset;
		if (mod < 4) {
			startOffset = 3 - mod;
		}
		else {
			g.fillRect(x, y, intPixelsPerBase * (6 - mod), height);
			startOffset = 9 - mod;
		}
		for (int j = startOffset; j < charArray.length; j += 6) {
			g.fillRect(x + (int) (j * pixelsPerBase), y, intPixelsPerBase * Math.min(charArray.length - j, 3), height);
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
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
		SeqSymmetry parentSym = (SeqSymmetry)getParent().getParent().getInfo();
		SeqSymmetry exonSym = (SeqSymmetry)getParent().getInfo();
		SeqSpan exonSpan = exonSym.getSpan(seq);
		if (((SeqSpan)parentSym).isForward() && exonSpan.getStart() > exonSpan.getEnd()) {
			exonSpan = new SimpleSeqSpan(exonSpan.getEnd(), exonSpan.getStart(), seq);
		}
		else if (!((SeqSpan)parentSym).isForward() && exonSpan.getStart() < exonSpan.getEnd()) {
			exonSpan = new SimpleSeqSpan(exonSpan.getEnd(), exonSpan.getStart(), seq);
		}
		StringBuffer codesSB = new StringBuffer("");
		SeqSpan cdsSpan = ((SupportsCdsSpan)parentSym).getCdsSpan();
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
				}
				else {
					continue;
				}
			}
			residue = seq.substring(span.getStart(), span.getEnd());
			if (((SeqSpan)parentSym).isForward()) {
				if (exonSpan.getStart() == span.getStart()) {
					startPos = codesSB.length();
				}
				codesSB.append(residue);
				if (exonSpan.getEnd() == span.getEnd()) {
					endPos = codesSB.length();
				}
			}
			else {
				if (exonSpan.getEnd() == span.getEnd()) {
					startPos = codesSB.length();
				}
				codesSB.insert(0, residue);
				if (exonSpan.getStart() == span.getStart()) {
					endPos = codesSB.length();
				}
			}
		}
		return new ResidueRange(startPos, endPos, codesSB.toString());
	}

	private String getResidue(ViewI view) {
		String errorMessage = null;
		SeqSymmetry parentSym = (SeqSymmetry)getParent().getParent().getInfo();
//		SeqSpan exonSpan = ((SeqSymmetry)getParent().getInfo()).getSpan(GenometryModel.getGenometryModel().getSelectedSeq());
		if (PreferenceUtils.getIntParam(CODON_GLYPH_CODE_SIZE, default_codon_glyph_code_size) == 0 || drawCodonGlyph != null) {
			return null;
		}
		ResidueRange residueRange = getResidueRange();
		String residue = residueRange.getResidue();
		if (residue.indexOf("-") > -1) { // load residues
//			if (view != null) {
//				loadInBackground(view, exonSpan);
//			}
			return null;
		}
		if (residue.length() < 6 || residue.length() % 3 != 0) {
			errorMessage = "invalid length " + residue.length();
//			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, errorMessage + " for " + parentSym.getGeneName() + ":" + parentSym.getID());
		}
//		else if (!residue.substring(0, 3).equals(AminoAcid.START_CODON)) {
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
			}
			else {
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
		StringBuffer aminoAcidsSB = new StringBuffer("");
		String nextCodon = null;
		for (int pos = 0; pos < residue.length(); pos += 3) {
			nextCodon = residue.substring(pos, pos + 3).toUpperCase();
			AminoAcid aminoAcid = AminoAcid.CODON_TO_AMINO_ACID.get(nextCodon);
			if (aminoAcid == null) { // should never happen
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "invalid sequence " + nextCodon + " for " + parentSym.toString() + ":" + parentSym.getID());
				return null;
			}
			String aaCode = "";
			int codeSize = PreferenceUtils.getIntParam(CODON_GLYPH_CODE_SIZE, default_codon_glyph_code_size);
			if (codeSize == 3) {
				aaCode = aminoAcid.getCode();
			}
			else if (codeSize == 1)  {
				aaCode = " " + aminoAcid.getLetter() + " ";
			}
			if (((SeqSpan)parentSym).isForward()) {
				aminoAcidsSB.append(aaCode);
			}
			else {
				aminoAcidsSB.insert(0, aaCode);
			}
		}
		offset = residueRange.getStartPos();
		return aminoAcidsSB.substring(residueRange.getStartPos(), residueRange.getEndPos());
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
