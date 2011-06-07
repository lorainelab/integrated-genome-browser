package com.affymetrix.genometryImpl.symloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.samtools.SAMException;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMValidationError;
import net.sf.samtools.TextTagCodec;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.StringUtil;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symloader.SymLoaderTabix.LineProcessor;
import com.affymetrix.genometryImpl.util.ErrorHandler;

import org.broad.tribble.readers.TabixReader.TabixLineReader;

/**
 *
 * @author hiralv
 */
public class SAM extends XAM implements LineProcessor{
	
	public SAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
	}
	
	@Override
	public void init() {
		try {
			File f = new File(uri);
			reader = new SAMFileReader(f);
			reader.setValidationStringency(ValidationStringency.SILENT);

			if (this.isInitialized) {
				return;
			}

			if (initTheSeqs()) {
				super.init();
			}
			
		} catch (SAMFormatException ex) {
			ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
					+ "Please validate your BAM files (see http://picard.sourceforge.net/command-line-overview.shtml#ValidateSamFile). "
					+ "See console for the details of the exception.\n");
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	protected boolean initTheSeqs(){
		boolean ret = super.initTheSeqs();
		
		if(ret){
			if(header.getSortOrder() != SAMFileHeader.SortOrder.coordinate){
				Logger.getLogger(SAM.class.getName()).log(Level.SEVERE,"Sam file must be sorted by coordinate.");
				return false;
			}
		}
		return ret;
	}
	
	@Override
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		init();
		if (reader != null) {
			CloseableIterator<SAMRecord> iter = reader.iterator();
			if (iter != null && iter.hasNext()) {
				return parse(iter, seq, min, max, containerSym, contained, true);
			}
		}

		return Collections.<SeqSymmetry>emptyList();
	}
	
	public List<SeqSymmetry> parse(CloseableIterator<SAMRecord> iter, BioSeq seq, int min, int max, boolean containerSym, boolean contained, boolean check) {
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		List<Throwable> errList = new ArrayList<Throwable>(10);
		int maximum;
		String seqId = seqs.get(seq);
		SAMRecord sr = null;
		try {
			while (iter.hasNext() && (!Thread.currentThread().isInterrupted())) {
				try {
					sr = iter.next();
					maximum = sr.getAlignmentEnd();

					if (check) {
						if (!seqId.equals(sr.getReferenceName())) {
							continue;
						}

						if (!(checkRange(sr.getAlignmentStart(), maximum, min, max))) {
							if (max > maximum) {
								break;
							}
							continue;
						}
					}

					if (skipUnmapped && sr.getReadUnmappedFlag()) {
						continue;
					}
					symList.add(convertSAMRecordToSymWithProps(sr, seq, uri.toString()));
				} catch (SAMException e) {
					errList.add(e);
				}
			}
		} finally {
			if (iter != null) {
				iter.close();
			}

			if (!errList.isEmpty()) {
				ErrorHandler.errorPanel("SAM exception", "Ignoring " + errList.size() + " records", errList);
			}
		}
		return symList;
	}
	
	private static boolean checkRange(int start, int end, int min, int max){

		//getChromosome && getRegion
		if(end < min || start > max){
			return false;
		}

		return true;
	}

	public List<? extends SeqSymmetry> processLines(BioSeq seq, TabixLineReader lineReader) {
		RecordIterator iter = new RecordIterator(lineReader);
		return parse(iter,seq,seq.getMin(),seq.getMax(),true,false,false);
	}

	public void init(URI uri) {
		File f = new File(uri);
		reader = new SAMFileReader(f);
		reader.setValidationStringency(ValidationStringency.SILENT);
		header = reader.getFileHeader();
	}
	
	/**
     * SAMRecord iterator for SAMTextReader
	 * Derived from picard.
     */
    private class RecordIterator implements CloseableIterator<SAMRecord>{

		 // From SAM specification
		private static final int QNAME_COL = 0;
		private static final int FLAG_COL = 1;
		private static final int RNAME_COL = 2;
		private static final int POS_COL = 3;
		private static final int MAPQ_COL = 4;
		private static final int CIGAR_COL = 5;
		private static final int MRNM_COL = 6;
		private static final int MPOS_COL = 7;
		private static final int ISIZE_COL = 8;
		private static final int SEQ_COL = 9;
		private static final int QUAL_COL = 10;

	    private static final int NUM_REQUIRED_FIELDS = 11;

		// Read string must contain only these characters
		private final Pattern VALID_BASES = Pattern.compile("^[acgtnACGTN.=]+$");
		private final TextTagCodec tagCodec = new TextTagCodec();
	
        /**
         * Allocate this once rather than for every line as a performance optimization.
         * The size is arbitrary -- merely large enough to handle the maximum number
         * of fields we might expect from a reasonable SAM file.
         */
        private final String[] mFields = new String[10000];
		private String mCurrentLine = null;
		
		private final TabixLineReader lineReader;
		
		RecordIterator(TabixLineReader lineReader){
			this.lineReader = lineReader;
			advanceLine();
		}
		
		public boolean hasNext() {
			return mCurrentLine != null;
		}

		public SAMRecord next() {
			if(!hasNext()){
				return null;
			}
			try{
				return parseLine();
			}finally{
				advanceLine();
			}
		}

		private String advanceLine() {
			try {
				mCurrentLine = lineReader.readLine();
				return mCurrentLine;
			} catch (IOException ex) {
				Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}
		
        private SAMRecord parseLine() {
			
            final int numFields = StringUtil.split(mCurrentLine, mFields, '\t');
            if (numFields < NUM_REQUIRED_FIELDS) {
                throw reportFatalErrorParsingLine("Not enough fields");
            }
            if (numFields == mFields.length) {
                reportErrorParsingLine("Too many fields in SAM text record.");
            }
            for (int i = 0; i < numFields; ++i) {
                if (mFields[i].length() == 0) {
                    reportErrorParsingLine("Empty field at position " + i + " (zero-based)");
                }
            }
            final SAMRecord samRecord = new SAMRecord(header);
            samRecord.setValidationStringency(ValidationStringency.SILENT);
//            if(reader != null)
//                samRecord.setFileSource(new SAMFileSource(reader,null));
            samRecord.setHeader(header);
            samRecord.setReadName(mFields[QNAME_COL]);

            final int flags = parseInt(mFields[FLAG_COL], "FLAG");
            samRecord.setFlags(flags);

            String rname = mFields[RNAME_COL];
            if (!rname.equals("*")) {
                rname = SAMSequenceRecord.truncateSequenceName(rname);
                validateReferenceName(rname, "RNAME");
                samRecord.setReferenceName(rname);
            } else if (!samRecord.getReadUnmappedFlag()) {
                    reportErrorParsingLine("RNAME is not specified but flags indicate mapped");
                }

            final int pos = parseInt(mFields[POS_COL], "POS");
            final int mapq = parseInt(mFields[MAPQ_COL], "MAPQ");
            final String cigar = mFields[CIGAR_COL];
            if (!SAMRecord.NO_ALIGNMENT_REFERENCE_NAME.equals(samRecord.getReferenceName())) {
                if (pos == 0) {
                    reportErrorParsingLine("POS must be non-zero if RNAME is specified");
                }
                if (!samRecord.getReadUnmappedFlag() && cigar.equals("*")) {
                    reportErrorParsingLine("CIGAR must not be '*' if RNAME is specified");
                }
            } else {
                if (pos != 0) {
                    reportErrorParsingLine("POS must be zero if RNAME is not specified");
                }
                if (mapq != 0) {
                    reportErrorParsingLine("MAPQ must be zero if RNAME is not specified");
                }
                if (!cigar.equals("*")) {
                    reportErrorParsingLine("CIGAR must be '*' if RNAME is not specified");
                }
            }
            samRecord.setAlignmentStart(pos);
            samRecord.setMappingQuality(mapq);
            samRecord.setCigarString(cigar);

            String mateRName = mFields[MRNM_COL];
            if (mateRName.equals("*")) {
                if (samRecord.getReadPairedFlag() && !samRecord.getMateUnmappedFlag()) {
                    reportErrorParsingLine("MRNM not specified but flags indicate mate mapped");
                }
            }
            else {
                if (!samRecord.getReadPairedFlag()) {
                    reportErrorParsingLine("MRNM specified but flags indicate unpaired");
                }
                if (!"=".equals(mateRName)) {
                    mateRName = SAMSequenceRecord.truncateSequenceName(mateRName);
                }
                validateReferenceName(mateRName, "MRNM");
                if (mateRName.equals("=")) {
                    if (samRecord.getReferenceName() == null) {
                        reportErrorParsingLine("MRNM is '=', but RNAME is not set");
                    }
                    samRecord.setMateReferenceName(samRecord.getReferenceName());
                } else {
                    samRecord.setMateReferenceName(mateRName);
                }
            }

            final int matePos = parseInt(mFields[MPOS_COL], "MPOS");
            final int isize = parseInt(mFields[ISIZE_COL], "ISIZE");
            if (!samRecord.getMateReferenceName().equals(SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)) {
                if (matePos == 0) {
                    reportErrorParsingLine("MPOS must be non-zero if MRNM is specified");
                }
            } else {
                if (matePos != 0) {
                    reportErrorParsingLine("MPOS must be zero if MRNM is not specified");
                }
                if (isize != 0) {
                    reportErrorParsingLine("ISIZE must be zero if MRNM is not specified");
                }
            }
            samRecord.setMateAlignmentStart(matePos);
            samRecord.setInferredInsertSize(isize);
            if (!mFields[SEQ_COL].equals("*")) {
                validateReadBases(mFields[SEQ_COL]);
                samRecord.setReadString(mFields[SEQ_COL]);
            } else {
                samRecord.setReadBases(SAMRecord.NULL_SEQUENCE);
            }
            if (!mFields[QUAL_COL].equals("*")) {
                if (samRecord.getReadBases() == SAMRecord.NULL_SEQUENCE) {
                    reportErrorParsingLine("QUAL should not be specified if SEQ is not specified");
                }
                if (samRecord.getReadString().length() != mFields[QUAL_COL].length()) {
                    reportErrorParsingLine("length(QUAL) != length(SEQ)");
                }
                samRecord.setBaseQualityString(mFields[QUAL_COL]);
            } else {
                samRecord.setBaseQualities(SAMRecord.NULL_QUALS);
            }

            for (int i = NUM_REQUIRED_FIELDS; i < numFields; ++i) {
                parseTag(samRecord, mFields[i]);
            }

            final List<SAMValidationError> validationErrors = samRecord.isValid();
            if (validationErrors != null) {
                for (final SAMValidationError errorMessage : validationErrors) {
                    reportErrorParsingLine(errorMessage.getMessage());
                }
            }
            return samRecord;
        }
		
        private void validateReadBases(final String bases) {
            if (!VALID_BASES.matcher(bases).matches()) {
                reportErrorParsingLine("Invalid character in read bases");
            }
        }

        private void parseTag(final SAMRecord samRecord, final String tag) {
            Map.Entry<String, Object> entry = null;
            try {
                entry = tagCodec.decode(tag);
            } catch (SAMFormatException e) {
                reportErrorParsingLine(e);
            }
            if (entry != null) {
                samRecord.setAttribute(entry.getKey(), entry.getValue());
            }
        }
		
		int parseInt(final String s, final String fieldName) {
            final int ret;
            try {
                ret = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw reportFatalErrorParsingLine("Non-numeric value in " + fieldName + " column");
            }
            return ret;
        }

        void validateReferenceName(final String rname, final String fieldName) {
            if (rname.equals("=")) {
                if (fieldName.equals("MRNM")) {
                    return;
                }
                reportErrorParsingLine("= is not a valid value for " + fieldName + " field.");
            }
            if (header.getSequenceDictionary().size() != 0) {
                if (header.getSequence(rname) == null) {
                    reportErrorParsingLine(fieldName + " '" + rname + "' not found in any SQ record");
                }
            }
        }
		
		private void reportErrorParsingLine(final String reason) {
			final String errorMessage = reason;
			System.err.println("Ignoring SAM validation error due to lenient parsing:");
			System.err.println(errorMessage);			
		}

		private void reportErrorParsingLine(final Exception e) {
			final String errorMessage = e.getMessage();
			System.err.println("Ignoring SAM validation error due to lenient parsing:");
			System.err.println(errorMessage);
		}

		private RuntimeException reportFatalErrorParsingLine(final String reason) {
			return new SAMFormatException(reason);
		}

		public void close() {
			try {
				lineReader.close();
			} catch (IOException ex) {
				Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		public void remove() {
			throw new UnsupportedOperationException("Not supported yet.");
		}
    }
}
