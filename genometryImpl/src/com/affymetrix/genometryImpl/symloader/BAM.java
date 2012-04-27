package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.SeekableFTPStream;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sf.picard.sam.BuildBamIndex;
import net.sf.samtools.SAMException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.SeekableBufferedStream;

/**
 * @author jnicol
 */
public final class BAM extends XAM {

	public final static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bam");
	}

	private static final Pattern CLEAN = Pattern.compile("[/\\s+]");
	
	protected SAMFileHeader header;
	
	private File indexFile = null;
	
	public BAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
		strategyList.add(LoadStrategy.AUTOLOAD);
	}

	@Override
	public void init() throws Exception  {
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
			} else if(scheme.startsWith("ftp")){
				String baiUriStr = findIndexFile(uri.toString());
				// Guess at the location of the .bai URL as BAM URL + ".bai"	
				if (baiUriStr == null) {
					ErrorHandler.errorPanel("No BAM index file",
							"Could not find URL of BAM index at " + uri.toString() + ". Please be sure this is in the same directory as the BAM file.");
					this.isInitialized = false;
					return;
				}
				indexFile = LocalUrlCacher.convertURIToFile(URI.create(baiUriStr));
				reader = new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(uri.toURL())), indexFile, false);
				reader.setValidationStringency(ValidationStringency.SILENT);
			}else {
				Logger.getLogger(BAM.class.getName()).log(
						Level.SEVERE, "URL scheme: {0} not recognized", scheme);
				return;
			}
			//set header
			header = reader.getFileHeader();
			
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

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) throws Exception  {
		init();
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		List<Throwable> errList = new ArrayList<Throwable>(10);
		CloseableIterator<SAMRecord> iter = null;
		CThreadWorker ctw = CThreadWorker.getCurrentCThreadWorker();
		try {
			if (reader != null) {
				iter = reader.query(seqs.get(seq), min, max, contained);
				if (iter != null && iter.hasNext()) {
					SAMRecord sr = null;
					while(iter.hasNext() && (!Thread.currentThread().isInterrupted())){
						try{
							sr = iter.next();
							if (skipUnmapped && sr.getReadUnmappedFlag()) continue;
							symList.add(convertSAMRecordToSymWithProps(sr, seq, uri.toString()));
						}catch(SAMException e){
							errList.add(e);
						}
					}
				}
			}
		} catch (Exception ex){
			throw ex;
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
	public List<SeqSymmetry> parseAll(BioSeq seq, String method) {		
		reader = new SAMFileReader(new File(uri));
		reader.setValidationStringency(ValidationStringency.SILENT);
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		if (reader != null) {
			for (final SAMRecord sr: reader){
				if (skipUnmapped && sr.getReadUnmappedFlag()) continue;
				symList.add(convertSAMRecordToSymWithProps(sr, seq, method));
			}
		}
		return symList;
	}

	/**
	 * Write annotations from min-max on the given chromosome to stream.
	 * @param seq -- chromosome
	 * @param min -- min coordinate
	 * @param max -- max coordinate
	 * @param dos -- output stream
	 * @param BAMWriter -- write as BAM or as SAM
	 */
	public void writeAnnotations(BioSeq seq, int min, int max, DataOutputStream dos, boolean BAMWriter) throws Exception  {
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
						System.err.println("Cannot create temporary BAM file! \n"+ex.getStackTrace());
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
					"See console and the tomcat catalina.out for the details of the exception.\n", ex);
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
		}else if(scheme.startsWith("http") || scheme.startsWith("ftp")) {
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

	public static List<? extends SeqSymmetry> parse(URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span) throws Exception{
		File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
		bamfile.deleteOnExit();
		BAM bam = new BAM(bamfile.toURI(),featureName, group);
		//for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
		if (uri.getScheme().equals("http")) return bam.parseAll(overlap_span.getBioSeq(), uri.toString());
		return bam.getRegion(overlap_span);
	}

	public List<? extends SeqSymmetry> parse(InputStream is, boolean annotate_seq)
	throws Exception  {
		throw new IllegalStateException(); // should not happen
	}
}
