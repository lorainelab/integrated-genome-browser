package com.affymetrix.genometryImpl.symloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.SAMException;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symloader.LineProcessor;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import net.sf.samtools.SAMTextReader;
import net.sf.samtools.util.AsciiLineReader;

import org.broad.tribble.readers.LineReader;

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

	public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) {
		SAMTextReader str = new SAMTextReader(new AsciiTabixLineReader(lineReader), header, ValidationStringency.SILENT);
		return parse(str.getIterator(), seq,seq.getMin(), seq.getMax(), true, false, false);
	}

	public void init(URI uri) {
		File f = new File(uri);
		reader = new SAMFileReader(f);
		reader.setValidationStringency(ValidationStringency.SILENT);
		header = reader.getFileHeader();
	}
	
	private class AsciiTabixLineReader extends AsciiLineReader {

		private final LineReader readerImpl;
		private int lineNumber;
		
		AsciiTabixLineReader(LineReader readerImpl) {
			super(null);
			this.readerImpl = readerImpl;
			lineNumber = 0;
		}

		@Override
		public String readLine() {
			try {
				return readerImpl.readLine();
			} catch (IOException ex) {
				Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, null, ex);
			}finally{
				lineNumber++;
			}
			return null;
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		@Override
		public void close() {
			readerImpl.close();
		}
		
		@Override
		public int peek() { return -1; }

	}
}
