package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

/**
 *
 * @author fwang4
 * 
 * This writer is not completed.
 * 
 * We try to compose a SAMRecord from SeqSymmetry - reverse engineering on XAM.convertSAMRecordToSymWithProps()
 * 
 * However seems the existing SeqSymmetry attributes are not enough:
 * redidues, VN, score, type, baseQuality, id, cigar, NH, name, NM, method, showMask, seq id, CL
 * 
 * The following materials are refereed:
 * 
 * http://samtools.sourceforge.net/
 * http://genome.sph.umich.edu/wiki/SAM
 * http://samtools.sourceforge.net/SAM1.pdf
 * 
 */
public class SAMWriter implements AnnotationWriter{

	@Override
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq, String type, OutputStream outstream) throws IOException {
		
		if(syms == null || syms.isEmpty()) {
			return false;
		}
		
		SAMFileHeader header = new SAMFileHeader();
		SAMFileWriter outputSam = new SAMFileWriterFactory().makeSAMWriter(header, true, outstream);
		for(SeqSymmetry sym : syms) {
			SAMRecord samRecord = new SAMRecord(header);
			
			SymWithProps bamSym = (BAMSym)sym;
			for(Map.Entry<String, Object> map : bamSym.getProperties().entrySet()) {
				// Save attributes for SAMRecord
//				samRecord.setAttribute(map.getKey(), map.getValue());
//				System.out.println(map.getKey() + " => " + map.getValue());
			}
//			outputSam.addAlignment(samRecord);
//			outstream.flush();
		}
		
		return true;
	}

	@Override
	public String getMimeType() {
		return "text/sam";
	}
}
