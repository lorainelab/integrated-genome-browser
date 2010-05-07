package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
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
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.CloseableIterator;

/**
 * @author jnicol
 */
public final class BAM extends SymLoader {
	private SAMFileReader reader;
    private SAMFileHeader header;
	private AnnotatedSeqGroup group;
	private final String featureName;
	private final List<BioSeq> seqs = new ArrayList<BioSeq>();

	public BAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri);
		this.group = seq_group;
		this.featureName = featureName;
	}

	@Override
	public LoadStrategy[] getLoadChoices() {
		// BAM files are generally large, so only allow loading visible data.
		LoadStrategy[] choices = {LoadStrategy.NO_LOAD, LoadStrategy.VISIBLE};
		return choices;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();

		try {
			String scheme = uri.getScheme().toLowerCase();
			if (scheme.length() == 0 || scheme.equals("file")) {
				// BAM is file.
				File f = new File(uri);
				reader = new SAMFileReader(f);
				reader.setValidationStringency(ValidationStringency.SILENT);
			} else if (scheme.startsWith("http")) {
				// BAM is URL.  Get the indexed .bai file, and query only the needed portion of the BAM file.

				String uriStr = uri.toString();
				// Guess at the location of the .bai URL as BAM URL + ".bai"
				String baiUriStr = uriStr + ".bai";
				File indexFile = LocalUrlCacher.convertURIToFile(URI.create(baiUriStr));
				if (indexFile == null) {
					Logger.getLogger(BAM.class.getName()).log(Level.SEVERE,
							"Could not find URL of BAM index at " + baiUriStr + ". Please be sure this is in the same directory as the BAM file.");
				}
				reader = new SAMFileReader(uri.toURL(), indexFile, false);
				reader.setValidationStringency(ValidationStringency.SILENT);
			} else {
				Logger.getLogger(BAM.class.getName()).log(Level.SEVERE,
						"URL scheme: " + scheme + " not recognized");
				return;
			}

			initTheSeqs();
		} catch (SAMFormatException ex) {
			ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n" +
					"Please validate your BAM files and contact the Picard project at http://picard.sourceforge.net." +
					"See console for the details of the exception.\n");
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}



	private void initTheSeqs() {
		try {
			header = reader.getFileHeader();
			if (header == null || header.getSequenceDictionary() == null || header.getSequenceDictionary().getSequences() == null) {
				Logger.getLogger(BAM.class.getName()).log(Level.WARNING, "Couldn't find sequences in file");
			}
			for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
				try {
					if (Thread.currentThread().isInterrupted()) {
						break;
					}
					String seqID = ssr.getSequenceName();
					if (group.getSeq(seqID) == null) {
						int seqLength = ssr.getSequenceLength();
						BioSeq seq = new BioSeq(seqID, group.getID(), seqLength);
						seqs.add(seq);
						Logger.getLogger(BAM.class.getName()).log(Level.INFO, "Adding chromosome " + seqID + " to group " + group.getID());
						group.addSeq(seq);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public List<BioSeq> getChromosomeList() {
		init();
		return seqs;
	}

	@Override
	public List<SeqSymmetry> getGenome() {
		init();
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (BioSeq seq : group.getSeqList()) {
			results.addAll(getChromosome(seq));
		}
		return results;
	}

	@Override
	public List<SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return parse(seq, seq.getMin(), seq.getMax(), true, true);
	}


	@Override
	public List<SeqSymmetry> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(), span.getMin(), span.getMax(), true, true);
	}
	
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		init();
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		CloseableIterator<SAMRecord> iter = null;
		try {
			if (reader != null) {
				iter = reader.query(seq.getID(), min, max, contained);
				if (iter != null) {
					for (SAMRecord sr = iter.next(); iter.hasNext() && (!Thread.currentThread().isInterrupted()); sr = iter.next()) {
						symList.add(convertSAMRecordToSymWithProps(sr, seq, featureName, featureName));
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
	private static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String featureName, String meth){
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
		for (int i=0;i<childs.size();i++) {
			SymWithProps child = childs.get(i);
			blockMins[i] =  child.getSpan(0).getMin() + span.getMin();
			blockMaxs[i] =  blockMins[i] + child.getSpan(0).getLength();
		}

		if(childs.size() == 0){
			blockMins = new int[1];
			blockMins[0] = span.getStart();
			blockMaxs = new int[1];
			blockMaxs[0] = span.getEnd();
		}

		SymWithProps sym = new UcscBedSym(featureName, seq, start, end, sr.getReadName(), 0.0f, span.isForward(), 0, 0, blockMins, blockMaxs);
		sym.setProperty("baseQuality", sr.getBaseQualityString());
		sym.setProperty("id",sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		sym.setProperty("residues", sr.getReadString());
		sym.setProperty("cigar", sr.getCigar());
		sym.setProperty("method", meth);

		return sym;
	}

	private static List<SimpleSymWithProps> getChildren(SAMRecord sr, BioSeq seq, Cigar cigar, String residues, int spanLength) {
		List<SimpleSymWithProps> results = new ArrayList<SimpleSymWithProps>();
		if (cigar == null || cigar.numCigarElements() == 0) {
			return results;
		}
		int currentChildStart = 0;
		int currentChildEnd = 0;
		int celLength = 0;
		
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					// skip over deletion
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					// TODO -- allow possibility that INSERTION is terminator, not M
					// print insertion
					currentChildEnd += celLength;
				} else if (cel.getOperator() == CigarOperator.M) {
					// print matches
					currentChildEnd += celLength;
					SimpleSymWithProps ss = new SimpleSymWithProps();
					if (!sr.getReadNegativeStrandFlag()) {
						ss.addSpan(new SimpleSeqSpan(currentChildStart, currentChildEnd, seq));
					}
					else {
						ss.addSpan(new SimpleSeqSpan(currentChildEnd, currentChildStart, seq));
					}
					results.add(ss);
				} else if (cel.getOperator() == CigarOperator.N) {
					currentChildStart = currentChildEnd + celLength;
					currentChildEnd = currentChildStart;
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					// TODO -- allow possibility that PADDING is terminator, not M
					// print matches
					currentChildEnd += celLength;
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					// hard clip can be ignored
				}
			} catch (Exception ex) {
				ex.printStackTrace();
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
	public static String interpretCigar(Object cigarObj, String residues, int startPos, int spanLength) {
		Cigar cigar = (Cigar)cigarObj;
		if (cigar == null || cigar.numCigarElements() == 0) {
			return residues;
		}
		StringBuilder sb = new StringBuilder(spanLength);
		int currentPos = 0;
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				int celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					currentPos += celLength;	// skip over deletion
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					if (currentPos >= startPos) {
						sb.append(residues.substring(currentPos, currentPos + celLength));
					}
					currentPos += celLength;	// print insertion
				} else if (cel.getOperator() == CigarOperator.M) {
					if (currentPos >= startPos) {
						sb.append(residues.substring(currentPos, currentPos + celLength));
					}
					currentPos += celLength;	// print matches
				} else if (cel.getOperator() == CigarOperator.N) {
					// ignore skips
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					char[] tempArr = new char[celLength];
					Arrays.fill(tempArr, '*');		// print padding as '*'
					sb.append(tempArr);
					currentPos += celLength;
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					currentPos += celLength;	// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					continue;				// hard clip can be ignored
				}
				if (currentPos - startPos >= spanLength) {
					break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				if (spanLength - currentPos - startPos > 0) {
					char[] tempArr = new char[spanLength - currentPos - startPos];
					Arrays.fill(tempArr, '.');
					sb.append(tempArr);
				}
			}
		}

		return sb.toString().intern();
	}

	public String getMimeType() {
		return "binary/BAM";
	}
}
