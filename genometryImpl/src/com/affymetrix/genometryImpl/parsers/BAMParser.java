package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.RandomAccessSym;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.util.CloseableIterator;

/**
 * @author jnicol
 */
public final class BAMParser {
	private SAMFileReader reader;
    private SAMFileHeader header;
	private File f;
	private AnnotatedSeqGroup group;

	public BAMParser(File f, AnnotatedSeqGroup seq_group) {
		this.group = seq_group;
		this.f = f;
		try {
			reader = new SAMFileReader(f);
			header = reader.getFileHeader();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void parse() {
		for (BioSeq seq : group.getSeqList()) {
			parse(seq, seq.getMin(), seq.getMax(), true, true);
		}
	}
	
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		CloseableIterator<SAMRecord> iter = null;
		try {
			iter = reader.query(seq.getID(), min, max, contained);
			for (SAMRecord sr = iter.next(); iter.hasNext(); sr = iter.next()) {
				if (containerSym) {
					// positive
					RandomAccessSym sym = new RandomAccessSym(this);
					sym.setID(sr.getReadName());
					for (SAMTagAndValue tv : sr.getAttributes()) {
						sym.setProperty(tv.tag, tv.value);
					}
					sym.addSpan(new SimpleSeqSpan(min, max, seq));
					sym.setProperty("method", f.getName());
					seq.addAnnotation(sym);
					symList.add(sym);

					// negative
					sym = new RandomAccessSym(this);
					sym.setID(sr.getReadName());
					for (SAMTagAndValue tv : sr.getAttributes()) {
						sym.setProperty(tv.tag, tv.value);
					}
					sym.addSpan(new SimpleSeqSpan(max, min, seq));
					sym.setProperty("method", f.getName());
					seq.addAnnotation(sym);
					symList.add(sym);
					return symList;
				}
				SimpleSymWithProps sym = convertSAMRecordToSymWithProps(sr, seq, f.getName());
				symList.add(sym);
				//seq.addAnnotation(sym);
			}
		} finally {
			iter.close();
		}

		return symList;
	}

	/**
	 * Return intervals for the given chromosome range
	 */
	public void parseLoRes(BioSeq seq, int min, int max, float scaleFactor, int[]x, float[]y) {
		CloseableIterator<SAMRecord> iter = null;
		try {
			iter = reader.query(seq.getID(), min, max, true);
			for (SAMRecord sr = iter.next(); iter.hasNext(); sr = iter.next()) {
				SimpleSymWithProps sym = convertSAMRecordToSymWithProps(sr, seq, f.getName().intern());
				int start = (int) ((sr.getAlignmentStart() - 1) * scaleFactor); // convert to interbase
				int end = (int) (sr.getAlignmentEnd() * scaleFactor);
				if (!sr.getReadNegativeStrandFlag()) {
					// swap
					int temp = start;
					start = end;
					end = temp;
				}
			}
		} finally {
			iter.close();
		}
	}

	/**
	 * Convert SAMRecord to SymWithProps.
	 * @param sr - SAMRecord
	 * @param seq - chromosome
	 * @param meth - method name
	 * @return SimpleSymWithProps
	 */
	public static SimpleSymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String meth) {
		SimpleSymWithProps sym = new SimpleSymWithProps();
		sym.setID(sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		int start = sr.getAlignmentStart() - 1; // convert to interbase
		int end = sr.getAlignmentEnd();
		if (!sr.getReadNegativeStrandFlag()) {
			sym.addSpan(new SimpleSeqSpan(start, end, seq));
		} else {
			sym.addSpan(new SimpleSeqSpan(end, start, seq));
		}
		sym.setProperty("residues", sr.getReadString().intern());
		sym.setProperty("method", meth);
		return sym;
	}

	public String getMimeType() {
		return "binary/BAM";
	}
}
