package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.sf.samtools.SAMException;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;

/**
 *
 * @author hiralv
 */
public class SAM extends XAM{
	
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
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		init();
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		List<Throwable> errList = new ArrayList<Throwable>(10);
		CloseableIterator<SAMRecord> iter = null;
		int maximum;
		String seqId = seqs.get(seq);
		try {
			if (reader != null) {
				iter = reader.iterator();
				if (iter != null && iter.hasNext()) {
					SAMRecord sr = null;
					while(iter.hasNext() && (!Thread.currentThread().isInterrupted())){
						try{
							sr = iter.next();
							if(!seqId.equals(sr.getReferenceName()))
								continue;
							
							maximum = sr.getAlignmentEnd();
							if(!(checkRange(sr.getAlignmentStart(),maximum,min,max))){ 
								if(max > maximum) 
									break;
							}
							
							if (skipUnmapped && sr.getReadUnmappedFlag()) {
								continue;
							}
							symList.add(convertSAMRecordToSymWithProps(sr, seq, featureName, featureName));
						}catch(SAMException e){
							errList.add(e);
						}
					}
				}
			}
		} finally {
			if (iter != null) {
				iter.close();
			}

			if(!errList.isEmpty()){
				ErrorHandler.errorPanel("SAM exception", "Ignoring "+errList.size()+" records",  errList);
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
}
