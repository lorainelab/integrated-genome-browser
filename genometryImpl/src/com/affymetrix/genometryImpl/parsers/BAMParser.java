package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

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
			parse(seq, seq.getMin(), seq.getMax(), true);
		}
	}
	
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	private List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym) {
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		CloseableIterator<SAMRecord> iter = null;
		try {
			iter = reader.query(seq.getID(), min, max, true);
			for (SAMRecord sr = iter.next(); iter.hasNext(); sr = iter.next()) {
				/*if (containerSym) {
					RandomAccessSym sym = new RandomAccessSym();
					sym.addParser(this);
					seq.addAnnotation(sym,f.getName());
					symList.add(sym);
					return symList;
				}*/

				// Actually parsing the information
				SimpleSymWithProps sym = new SimpleSymWithProps();
				sym.setID(sr.getReadName());
				for (SAMTagAndValue tv : sr.getAttributes()) {
					sym.setProperty(tv.tag, tv.value);
				}
				if (!sr.getReadNegativeStrandFlag()) {
					sym.addSpan(new SimpleSeqSpan(sr.getAlignmentStart(), sr.getAlignmentEnd(), seq));
				} else {
					sym.addSpan(new SimpleSeqSpan(sr.getAlignmentEnd(), sr.getAlignmentStart(), seq));
				}
				sym.setProperty("residues", new String(sr.getReadBases()));
				sym.setProperty("meth", f.getName());
				symList.add(sym);
				seq.addAnnotation(sym);
			}
		} finally {
			iter.close();
		}

		return symList;
	}

	public String getMimeType() {
		return "binary/BAM";
	}
}
