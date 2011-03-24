package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BAMSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sf.picard.sam.BuildBamIndex;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMException;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.CloseableIterator;

/**
 * @author jnicol
 */
public final class BAM extends SymLoader {

	public final static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bam");
	}

	private static final boolean DEBUG = false;
	private boolean skipUnmapped = true;
	private SAMFileReader reader;
    private SAMFileHeader header;
	private final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();
	private File indexFile = null;
	private static final Pattern CLEAN = Pattern.compile("[/\\s+]");

	private final static List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();

	public static final String CIGARPROP = "cigar";
	public static final String RESIDUESPROP = "residues";
	public static final String BASEQUALITYPROP = "baseQuality";
	public static final String SHOWMASK = "showMask";
	public static final String INSRESIDUESPROP = "insResidues";

	static {
		// BAM files are generally large, so only allow loading visible data.
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.AUTOLOAD);
		strategyList.add(LoadStrategy.VISIBLE);
	}

	public BAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		
		try {
			String scheme = uri.getScheme().toLowerCase();
			if (scheme.length() == 0 || scheme.equals("file")) {
				// BAM is file.
				//indexFile = new File(uri.)
				File f = new File(uri);
				indexFile = findIndexFile(f);
				reader = new SAMFileReader(f, indexFile, false);
				reader.setValidationStringency(ValidationStringency.SILENT);
			} else if (scheme.startsWith("http")) {
				// BAM is URL.  Get the indexed .bai file, and query only the needed portion of the BAM file.
				String baiUriStr = findIndexFile(uri.toString());
				// Guess at the location of the .bai URL as BAM URL + ".bai"	
				if (baiUriStr == null) {
					ErrorHandler.errorPanel("No BAM index file",
							"Could not find URL of BAM index at " + uri.toString() + ". Please be sure this is in the same directory as the BAM file.");
					this.isInitialized = false;
					return;
				}
				indexFile = LocalUrlCacher.convertURIToFile(URI.create(baiUriStr));
				reader = new SAMFileReader(uri.toURL(), indexFile, false);
				reader.setValidationStringency(ValidationStringency.SILENT);
			} else {
				Logger.getLogger(BAM.class.getName()).log(
						Level.SEVERE, "URL scheme: {0} not recognized", scheme);
				return;
			}

			if(initTheSeqs()){
				super.init();
			}
		} catch (SAMFormatException ex) {
			ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n" +
					"Please validate your BAM files (see http://picard.sourceforge.net/command-line-overview.shtml#ValidateSamFile). " +
					"See console for the details of the exception.\n");
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}



	private boolean initTheSeqs() {
		try {
			header = reader.getFileHeader();
			if (header == null || header.getSequenceDictionary() == null || header.getSequenceDictionary().getSequences() == null) {
				Logger.getLogger(BAM.class.getName()).log(Level.WARNING, "Couldn't find sequences in file");
				return false;
			}
			Thread thread = Thread.currentThread();
			for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
				try {
					if (thread.isInterrupted()) {
						break;
					}
					String seqID = ssr.getSequenceName();
					int seqLength = ssr.getSequenceLength();
					BioSeq seq = group.addSeq(seqID, seqLength);
					if(seq.getVersion() != null){
						seq.setVersion(group.getID());
					}
					seqs.put(seq,seqID);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			return !thread.isInterrupted();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public List<BioSeq> getChromosomeList() {
		init();
		return new ArrayList<BioSeq>(seqs.keySet());
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
		return parse(seq, seq.getMin(), seq.getMax(), true, false);
	}


	@Override
	public List<SeqSymmetry> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(), span.getMin(), span.getMax(), true, false);
	}

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		init();
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		List<Throwable> errList = new ArrayList<Throwable>(10);
		CloseableIterator<SAMRecord> iter = null;
		try {
			if (reader != null) {
				iter = reader.query(seqs.get(seq), min, max, contained);
				if (iter != null && iter.hasNext()) {
					SAMRecord sr = null;
					while(iter.hasNext() && (!Thread.currentThread().isInterrupted())){
						try{
							sr = iter.next();
							if (skipUnmapped && sr.getReadUnmappedFlag()) continue;
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
	
	/**
	 * Returns a list of symmetries for the entire file, good for loading DAS/2 derived data slices, skips building an index
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> parseAll(BioSeq seq) {		
		reader = new SAMFileReader(new File(uri));
		reader.setValidationStringency(ValidationStringency.SILENT);
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		if (reader != null) {
			for (final SAMRecord sr: reader){
				if (skipUnmapped && sr.getReadUnmappedFlag()) continue;
				symList.add(convertSAMRecordToSymWithProps(sr, seq, featureName, featureName));
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
	private static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String featureName, String meth) {
		SimpleSeqSpan span = null;
		int start = sr.getAlignmentStart() - 1; // convert to interbase
		int end = sr.getAlignmentEnd();
		if (!sr.getReadNegativeStrandFlag()) {
			span = new SimpleSeqSpan(start, end, seq);
		} else {
			span = new SimpleSeqSpan(end, start, seq);
		}

		List<SimpleSymWithProps> insertChilds = new ArrayList<SimpleSymWithProps>();
		List<SimpleSymWithProps> childs = getChildren(sr, seq, sr.getCigar(), insertChilds);

		int blockMins[] = new int[childs.size()];
		int blockMaxs[] = new int[childs.size()];
		for (int i = 0; i < childs.size(); i++) {
			SymWithProps child = childs.get(i);
			blockMins[i] = child.getSpan(0).getMin() + span.getMin();
			blockMaxs[i] = blockMins[i] + child.getSpan(0).getLength();
		}

		int iblockMins[] = new int[insertChilds.size()];
		int iblockMaxs[] = new int[insertChilds.size()];
		for (int i = 0; i < insertChilds.size(); i++) {
			SymWithProps child = insertChilds.get(i);
			iblockMins[i] = child.getSpan(0).getMin() + span.getMin();
			iblockMaxs[i] = iblockMins[i] + child.getSpan(0).getLength();
		}

		if (childs.isEmpty()) {
			blockMins = new int[1];
			blockMins[0] = span.getStart();
			blockMaxs = new int[1];
			blockMaxs[0] = span.getEnd();
		}

		StringBuffer insResidue = new StringBuffer(200);
		BAMSym sym = new BAMSym(featureName, seq, start, end, sr.getReadName(),
				0.0f, span.isForward(), 0, 0, blockMins, blockMaxs, iblockMins, iblockMaxs);
		sym.setProperty(BASEQUALITYPROP, sr.getBaseQualityString());
		sym.setProperty("id", sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		sym.setProperty(CIGARPROP, sr.getCigar());
		sym.setProperty(SHOWMASK, true);

		sym.setResidues(interpretCigar(sr.getCigar(), sr.getReadString(), end - start, insResidue));
		sym.setInsResidues(insResidue.toString());

//		Not using "SEQ" anywhere. So commenting out for now.
//		if (sr.getCigar() == null || sym.getProperty("MD") == null) {
//			//sym.setProperty("residues", sr.getReadString());
//		} else {
//			// If both the MD and Cigar properties are set, don't need to specify residues.
//			byte[] SEQ = SequenceUtil.makeReferenceFromAlignment(sr, false);
//			sym.setProperty("SEQ", SEQ);
//		}
		
		sym.setProperty("method", meth);

		getFileHeaderProperties(sr.getHeader(), sym);

		return sym;
	}

	private static List<SimpleSymWithProps> getChildren(SAMRecord sr, BioSeq seq, Cigar cigar, List<SimpleSymWithProps> insertChilds) {
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
					currentChildStart = currentChildEnd;
					currentChildEnd = currentChildStart  + celLength;
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					// TODO -- allow possibility that INSERTION is terminator, not M
					// print insertion
					currentChildStart = currentChildEnd;
					currentChildEnd = currentChildStart;
					SimpleSymWithProps ss = new SimpleSymWithProps();
					if (!sr.getReadNegativeStrandFlag()) {
						ss.addSpan(new SimpleSeqSpan(currentChildStart, currentChildStart + celLength, seq));
					}
					else {
						ss.addSpan(new SimpleSeqSpan(currentChildStart + celLength, currentChildStart, seq));
					}
					insertChilds.add(ss);
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

	private static void getFileHeaderProperties(SAMFileHeader hr, SymWithProps sym) {
		if (hr == null) {
			return;
		}
		//Sequence Dictionary
		SAMSequenceDictionary ssd = hr.getSequenceDictionary();
		for (SAMSequenceRecord ssr : ssd.getSequences()) {
			if (ssr.getAssembly() != null) {
				sym.setProperty("genomeAssembly", ssr.getAssembly());
			}
			if (ssr.getSpecies() != null) {
				sym.setProperty("species  ", ssr.getSpecies());
			}
		}
		//Read Group
		for (SAMReadGroupRecord srgr : hr.getReadGroups()) {
			for (Entry<String, String> en : srgr.getAttributes()) {
				sym.setProperty(en.getKey(), en.getValue());
			}
		}

		//Program
		for (SAMProgramRecord spr : hr.getProgramRecords()) {
			for (Entry<String, String> en : spr.getAttributes()) {
				sym.setProperty(en.getKey(), en.getValue());
			}
		}
	}

	/**
	 * Rewrite the residue string, based upon cigar information
	 * @param cigarObj
	 * @param residues
	 * @param spanLength
	 * @param insResidues
	 * @return
	 */
	private static String interpretCigar(Cigar cigar, String residues, int spanLength, StringBuffer insResidues) {
		if (cigar == null || cigar.numCigarElements() == 0) {
			return residues;
		}
		StringBuilder sb = new StringBuilder(spanLength);
		int currentPos = 0;
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				int celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
						char[] tempArr = new char[celLength];
						Arrays.fill(tempArr, '_');		// print deletion as '_'
						sb.append(tempArr);
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					insResidues.append(residues.substring(currentPos, currentPos + celLength));
					currentPos += celLength;	// print insertion
				} else if (cel.getOperator() == CigarOperator.M) {
						sb.append(residues.substring(currentPos, currentPos + celLength));
					currentPos += celLength;	// print matches
				} else if (cel.getOperator() == CigarOperator.N) {
						char[] tempArr = new char[celLength];
						Arrays.fill(tempArr, '-');
						sb.append(tempArr);
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
			} catch (Exception ex) {
				ex.printStackTrace();
				if (spanLength - currentPos > 0) {
					char[] tempArr = new char[spanLength - currentPos];
					Arrays.fill(tempArr, '.');
					sb.append(tempArr);
				}
			}
		}

		return sb.toString().intern();
	}

	/**
	 * Write annotations from min-max on the given chromosome to stream.
	 * @param seq -- chromosome
	 * @param min -- min coordinate
	 * @param max -- max coordinate
	 * @param dos -- output stream
	 * @param BAMWriter -- write as BAM or as SAM
	 */
	public void writeAnnotations(BioSeq seq, int min, int max, DataOutputStream dos, boolean BAMWriter) {
		init();
		if (reader == null) {
			return;
		}
		CloseableIterator<SAMRecord> iter = null;
		SAMFileWriter sfw = null;
		File tempBAMFile = null;
		try {
			iter = reader.query(seq.getID(), min, max, false);
			//check for any records
			if (iter.hasNext() == false) {
				Logger.getLogger(BAM.class.getName()).log(Level.INFO, "No overlapping bam alignments.", "Min-Max: "+min+"-"+max);
				return;
			}
			//write out records to file
			//TODO: is this hack necessary with updated picard.jar?
			reader.getFileHeader().setSortOrder(net.sf.samtools.SAMFileHeader.SortOrder.coordinate); // A hack to prevent error caused by picard tool.
			if (iter != null) {
				net.sf.samtools.SAMFileWriterFactory sfwf = new net.sf.samtools.SAMFileWriterFactory();
				if (BAMWriter) {
					// BAM files cannot be written to the stream one line at a time.
					// Rather, a tempfile is created, and later read into the stream.
					try {
						tempBAMFile = File.createTempFile(CLEAN.matcher(featureName).replaceAll("_"), ".bam");						
						tempBAMFile.deleteOnExit();
					} catch (IOException ex) {
						Logger.getLogger(BAM.class.getName()).log(Level.SEVERE, null, ex);
						return; // Can't create the temporary file!
					}
					sfw = sfwf.makeBAMWriter(header, true, tempBAMFile);
				} else {
					sfw = sfwf.makeSAMWriter(header, true, dos);
				}
				
				// read each record, and add to the SAMFileWriter
				for (SAMRecord sr = iter.next(); iter.hasNext() && (!Thread.currentThread().isInterrupted()); sr = iter.next()) {
					sfw.addAlignment(sr);
				}
			}
		} catch(Exception ex){
			Logger.getLogger(BAM.class.getName()).log(Level.SEVERE,"SAM exception A SAMFormatException has been thrown by the Picard tools.\n" +
					"Please validate your BAM files and contact the Picard project at http://picard.sourceforge.net." +
					"See console for the details of the exception.\n", ex);
		} finally {
			if (iter != null) {
				try {
					iter.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (sfw != null) {
				try {
					sfw.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			if (tempBAMFile != null && tempBAMFile.exists()) {
				GeneralUtils.writeFileToStream(tempBAMFile, dos);
				// delete tempfile if possible.
				if (!tempBAMFile.delete()) {
					Logger.getLogger(BAM.class.getName()).log(
							Level.WARNING, "Couldn''t delete file {0}", tempBAMFile.getName());
				}
			}
		}
	}

	/**Modified to look for both xxx.bai and xxx.bam.bai files in parent directory.*/
	static public File findIndexFile(File bamfile) throws IOException {
		//look for xxx.bam.bai
		String path = bamfile.getPath();
		File f = new File(path+".bai");
		if (f.exists())
			return f;

		//look for xxx.bai
		path = path.substring(0, path.length()-3)+"bai";
		f = new File(path);
		if (f.exists())
				return f;

		return null;
	}

	static public String findIndexFile(String bamfile) {
		// Guess at the location of the .bai URL as BAM URL + ".bai"
		String baiUriStr = bamfile + ".bai";

		if (LocalUrlCacher.isValidURL(baiUriStr)) {
			return baiUriStr;
		}

		baiUriStr = bamfile.substring(0, bamfile.length() - 3) + "bai";

		//look for xxx.bai
		if(LocalUrlCacher.isValidURL(baiUriStr)){
			return baiUriStr;
		}
		
		return null;
	}

	public static boolean hasIndex(URI uri) throws IOException{
		String scheme = uri.getScheme().toLowerCase();
		if (scheme.length() == 0 || scheme.equals("file")) {
			File f = findIndexFile(new File(uri));
			return f != null;
		}else if(scheme.startsWith("http")) {
			String uriStr = findIndexFile(uri.toString());
			return uriStr != null;
		}

		return false;
	}

	//Can be used later. Do not remove.
	@SuppressWarnings("unused")
	static private File createIndexFile(File bamfile) throws IOException{
		File indexfile = File.createTempFile(bamfile.getName(), ".bai");

		if(!indexfile.exists()){
			ErrorHandler.errorPanel("Unable to create file.");
			return null;
		}

		if (DEBUG)
			System.out.println("Creating new bam index file -> "+indexfile);

		String input = "INPUT=" + bamfile.getAbsolutePath();
		String output = "OUTPUT=" + indexfile.getAbsolutePath();
		String quiet = "QUIET="+!DEBUG;
		BuildBamIndex buildIndex = new BuildBamIndex();
		buildIndex.instanceMain(new String[]{input, output, quiet});

		return indexfile;
	}
	
	public String getMimeType() {
		return "binary/BAM";
	}

}
