package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

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
					Level.WARNING, "Couldn't find sequence dictionary -- no sequences loaded from BAM");
		} else {
			// add sequences that aren't in the original group.  Especially useful for "unknown groups"
			for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
				try {
					if(Thread.currentThread().isInterrupted())
						break;

					String seqID = ssr.getSequenceName();
					if (group.getSeq(seqID) == null) {
						int seqLength = ssr.getSequenceLength();
						group.addSeq(new BioSeq(seqID, group.getID(), seqLength));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		getGenome();
	}

	public List<SeqSymmetry> getGenome() {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (BioSeq seq : group.getSeqList()) {
			results.addAll(getChromosome(seq));
		}
		return results;
	}

	public List<SeqSymmetry> getChromosome(BioSeq seq) {
		return parse(seq, seq.getMin(), seq.getMax(), true, true);
	}


	public List<SeqSymmetry> getRegion(SeqSpan span) {
		return parse(span.getBioSeq(), span.getMin(), span.getMax(), true, true);
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
					for (SAMRecord sr = iter.next(); iter.hasNext() && (!Thread.currentThread().isInterrupted()); sr = iter.next()) {
						symList.add(convertSAMRecordToSymWithProps(sr, seq, f.getName()));
					}
				}
			}
		} finally {
			if (iter != null) {
				iter.close();
			}
		}

		return symList;
	}

	/**
	 * Convert SAMRecord to SymWithProps.
	 * @param sr - SAMRecord
	 * @param seq - chromosome
	 * @param meth - method name
	 * @return SimpleSymWithProps
	 */
	private static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String meth){
		SimpleSeqSpan span = null;
		int start = sr.getAlignmentStart() - 1; // convert to interbase
		int end = sr.getAlignmentEnd();
		if (!sr.getReadNegativeStrandFlag()) {
			span = new SimpleSeqSpan(start, end, seq);
		} else {
			span = new SimpleSeqSpan(end, start, seq);
		}

		List<SimpleSymWithProps> childs = getChildren(sr, seq, sr.getCigar(), sr.getReadString(), span.getLength());

		int blockMins[] = new int[childs.size()];
		int blockMaxs[] = new int[childs.size()];
		int i = 0;
		for (SimpleSymWithProps child : childs) {
			blockMins[i] =  child.getSpan(0).getMin() + span.getMin();
			blockMaxs[i] =  blockMins[i] + child.getSpan(0).getLength();
			i++;
		}

		if(childs.size() == 0){
			blockMins = new int[1];
			blockMins[0] = span.getStart();
			blockMaxs = new int[1];
			blockMaxs[0] = span.getEnd();
		}

		SymWithProps sym = new UcscBedSym(seq.getID(), seq, start, end, sr.getReadName(), 0.0f, span.isForward(), 0, 0, blockMins, blockMaxs);


		sym.setProperty("id",sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		sym.setProperty("cigar", sr.getCigar());
		sym.setProperty("residues", sr.getReadString().intern());
		sym.setProperty("method", meth);
		seq.addAnnotation(sym);

		return sym;
	}

	private static List<SimpleSymWithProps> getChildren(SAMRecord sr, BioSeq seq, Cigar cigar, String residues, int spanLength) {
		List<SimpleSymWithProps> results = new ArrayList<SimpleSymWithProps>();
		if (cigar == null || cigar.numCigarElements() == 0) {
			return results;
		}

		StringBuilder sb = new StringBuilder();
		int currentPosition = 0;
		int currentChildStart = 0;
		int currentChildEnd = 0;
		int celLength = 0;
		
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					currentPosition += celLength;	// skip over deletion
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					sb.append(residues.substring(currentPosition, currentPosition + celLength));
					currentPosition += celLength;	// print insertion
					currentChildEnd += celLength;
				} else if (cel.getOperator() == CigarOperator.M) {
					sb.append(residues.substring(currentPosition, currentPosition + celLength));
					currentPosition += celLength;	// print matches
					currentChildEnd += celLength;
					String childResidues = sb.toString().intern();
					sb = new StringBuilder();
					SimpleSymWithProps ss = new SimpleSymWithProps();
					if (!sr.getReadNegativeStrandFlag()) {
						ss.addSpan(new SimpleSeqSpan(currentChildStart, currentChildEnd, seq));
					}
					else {
						ss.addSpan(new SimpleSeqSpan(currentChildEnd, currentChildStart, seq));
					}
					ss.setProperty("residues", childResidues);
					results.add(ss);
				} else if (cel.getOperator() == CigarOperator.N) {
					currentChildStart = currentChildEnd + celLength;
					currentChildEnd = currentChildStart;
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					char[] tempArr = new char[celLength];
					Arrays.fill(tempArr, '*');		// print padding as '*'
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					currentPosition += celLength;	// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					continue;						// hard clip can be ignored
				}
				if (currentPosition > spanLength) {
					Logger.getLogger(BAMParser.class.getName()).log(Level.FINE,
							"currentPosition > spanLength: " + currentPosition + " > " + spanLength);
					break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return results;
			}
		}

		return results;
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
