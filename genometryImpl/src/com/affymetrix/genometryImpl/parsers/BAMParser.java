package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.RandomAccessSym;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceRecord;
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
			//header = reader.getFileHeader();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void parse() {
		header = reader.getFileHeader();
		if (header == null || header.getSequenceDictionary() == null) {
			Logger.getLogger(BAMParser.class.getName()).log(
					Level.WARNING, "Couldn't find sequence dictionary -- no sequences loaded");
			return;
		}
		// add sequences that aren't in the original group.  Especially useful for "unknown groups"
		for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
			try {
				String seqID = ssr.getSequenceName();
				if (group.getSeq(seqID) == null) {
					int seqLength = ssr.getSequenceLength();
					group.addSeq(new BioSeq(seqID, group.getID(), seqLength));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
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
			if (reader != null) {
				iter = reader.query(seq.getID(), min, max, contained);
				if (iter != null) {
					for (SAMRecord sr = iter.next(); iter.hasNext(); sr = iter.next()) {
						if (containerSym) {
							// positive track
							symList.add(createRandomAccessSym(sr, min, max, seq));

							// negative track
							symList.add(createRandomAccessSym(sr, max, min, seq));

							return symList;
						}
						symList.add(convertSAMRecordToSymWithProps(sr, seq, f.getName()));
					}
				}
			}
		} finally {
			iter.close();
		}

		return symList;
	}

	private RandomAccessSym createRandomAccessSym(SAMRecord sr, int min, int max, BioSeq seq) {
		RandomAccessSym sym = new RandomAccessSym(this);
		sym.setID(sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		sym.addSpan(new SimpleSeqSpan(min, max, seq));
		sym.setProperty("method", f.getName());
		seq.addAnnotation(sym);
		return sym;
	}

	/**
	 * Return intervals for the given chromosome range
	 */
	public void parseLoRes(BioSeq seq, int min, int max, float scaleFactor, int[]x, float[]y) {
		CloseableIterator<SAMRecord> iter = null;
		try {
			if (reader != null) {
				iter = reader.query(seq.getID(), min, max, true);
				if (iter != null) {
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
		sym.setProperty("cigar", sr.getCigar());	// interpreted later
		sym.setProperty("residues", sr.getReadString().intern());
		sym.setProperty("method", meth);
		return sym;
	}

	/**
	 * Rewrite the residue string, based upon cigar information
	 * @param cigarObj
	 * @param residues
	 * @param spanLength
	 * @return
	 */
	public static String interpretCigar(Object cigarObj, String residues, int spanLength) {
		Cigar cigar = (Cigar)cigarObj;
		if (cigar == null || cigar.numCigarElements() == 0) {
			return residues;
		}
		StringBuilder sb = new StringBuilder(spanLength);
		int currentPosition = 0;
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				int celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					currentPosition += celLength;	// skip over deletion
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					sb.append(residues.substring(currentPosition, currentPosition + celLength));
					currentPosition += celLength;	// print insertion
				} else if (cel.getOperator() == CigarOperator.M) {
					sb.append(residues.substring(currentPosition, currentPosition + celLength));
					currentPosition += celLength;	// print matches
				} else if (cel.getOperator() == CigarOperator.N) {
					char[] tempArr = new char[celLength];
					Arrays.fill(tempArr, '.');
					sb.append(tempArr);				// print N as '.'
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					char[] tempArr = new char[celLength];
					Arrays.fill(tempArr, '*');		// print padding as '*'
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					currentPosition += celLength;	// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					continue;						// hard clip can be ignored
				}
				if (currentPosition > spanLength) {
					Logger.getLogger(BAMParser.class.getName()).log(Level.FINE, "currentPosition > spanLength: " + currentPosition + " > " + spanLength);
					break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				char[] tempArr = new char[spanLength-currentPosition];
				Arrays.fill(tempArr, '-');
				sb.append(tempArr);
				return sb.toString().intern();
			}
		}

		return sb.toString().intern();
	}

	public String getMimeType() {
		return "binary/BAM";
	}
}
