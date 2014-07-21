package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import static com.affymetrix.genometryImpl.symloader.UriProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometryImpl.symloader.UriProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometryImpl.symloader.UriProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometryImpl.symloader.UriProtocolConstants.HTTP_PROTOCOL;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.BlockCompressedStreamPosition;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SeekableFTPStream;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.sf.picard.sam.BuildBamIndex;
import net.sf.samtools.SAMException;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.seekablestream.SeekableBufferedStream;
import net.sf.samtools.seekablestream.SeekableHTTPStream;
import net.sf.samtools.util.CloseableIterator;
import org.apache.commons.lang3.StringUtils;

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

    public BAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
        super(uri, featureName, seq_group);
        strategyList.add(LoadStrategy.AUTOLOAD);
    }

    private SAMFileReader getSAMFileReader() throws IOException, BamIndexNotFoundException {
        File indexFile = null;
        SAMFileReader samFileReader = null;
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL)) {
			// BAM is file.
            //indexFile = new File(uri.)
            File f = new File(uri);
            indexFile = findIndexFile(f);
            samFileReader = new SAMFileReader(f, indexFile, false);
            samFileReader.setValidationStringency(ValidationStringency.SILENT);
        } else if (StringUtils.equals(scheme, HTTP_PROTOCOL) || StringUtils.equals(scheme, HTTPS_PROTOCOL)) {
            String reachable_url = LocalUrlCacher.getReachableUrl(uri.toASCIIString());

            if (reachable_url == null) {
                ErrorHandler.errorPanel("Url cannot be reached");
                this.isInitialized = false;
                return null;
            }
            String baiUriStr = getBamIndexUriStr(uri);
            indexFile = LocalUrlCacher.convertURIToFile(URI.create(baiUriStr));
            SeekableBufferedStream seekableStream = new SeekableBufferedStream(new SeekableHTTPStream(new URL(reachable_url)));
            samFileReader = new SAMFileReader(seekableStream, indexFile, false);
            samFileReader.setValidationStringency(ValidationStringency.SILENT);
        } else if (scheme.startsWith("ftp")) {
            String baiUriStr = getBamIndexUriStr(uri);
            indexFile = LocalUrlCacher.convertURIToFile(URI.create(baiUriStr));
            samFileReader = new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(uri.toURL())), indexFile, false);
            samFileReader.setValidationStringency(ValidationStringency.SILENT);
        } else {
            Logger.getLogger(BAM.class.getName()).log(
                    Level.SEVERE, "URL scheme: {0} not recognized", scheme);
            return null;
        }
        return samFileReader;
    }

    private String getBamIndexUriStr(URI uri) throws BamIndexNotFoundException {
        // BAM is URL.  Get the indexed .bai file, and query only the needed portion of the BAM file.
        String baiUriStr = findIndexFile(uri.toString());
        // Guess at the location of the .bai URL as BAM URL + ".bai"	
        if (StringUtils.isBlank(baiUriStr)) {
            ErrorHandler.errorPanel("No BAM index file",
                    "Could not find URL of BAM index at " + uri.toString() + ". Please be sure this is in the same directory as the BAM file.", Level.SEVERE);
            this.isInitialized = false;
            return null;
        }
        return baiUriStr;
    }

    @Override
    public void init() throws IOException, BamIndexNotFoundException {
        if (this.isInitialized) {
            return;
        }

        try {
            reader = getSAMFileReader();
            //set header
            header = reader.getFileHeader();

            if (initTheSeqs()) {
                super.init();
            }
        } catch (SAMFormatException ex) {
            ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your BAM files (see http://picard.sourceforge.net/command-line-overview.shtml#ValidateSamFile). "
                    + "See console for the details of the exception.\n", Level.SEVERE);
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
     * @return current CompressedStreamPosition for SAMFileReader
     * @throws Exception
     */
    private BlockCompressedStreamPosition getCompressedInputStreamPosition(SAMFileReader sfr) throws Exception {
        Field privateReaderField = sfr.getClass().getDeclaredField("mReader");
        privateReaderField.setAccessible(true);
        Object mReaderValue = privateReaderField.get(sfr);
        Field privateCompressedInputStreamField = mReaderValue.getClass().getDeclaredField("mCompressedInputStream");
        privateCompressedInputStreamField.setAccessible(true);
        Object compressedInputStreamValue = privateCompressedInputStreamField.get(mReaderValue);
        Field privateBlockAddressField = compressedInputStreamValue.getClass().getDeclaredField("mBlockAddress");
        privateBlockAddressField.setAccessible(true);
        long blockAddressValue = ((Long) privateBlockAddressField.get(compressedInputStreamValue));
        Field privateCurrentOffsetField = compressedInputStreamValue.getClass().getDeclaredField("mCurrentOffset");
        privateCurrentOffsetField.setAccessible(true);
        int currentOffsetValue = ((Integer) privateCurrentOffsetField.get(compressedInputStreamValue));;
        return new BlockCompressedStreamPosition(blockAddressValue, currentOffsetValue);
    }

    /**
     * Return a list of symmetries for the given chromosome range.
     */
    public synchronized List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) throws Exception {
        init();
        List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
        List<Throwable> errList = new ArrayList<Throwable>(10);
        CloseableIterator<SAMRecord> iter = null;
        try {
            if (reader != null) {
                iter = reader.query(seqs.get(seq), max - 1, max, contained);
                while (iter.hasNext()) {
                    iter.next();
                }
                iter.close();
                iter = reader.query(seqs.get(seq), min, max, contained);
                if (iter != null && iter.hasNext()) {
                    SAMRecord sr = null;
                    while (iter.hasNext() && (!Thread.currentThread().isInterrupted())) {
                        try {
                            sr = iter.next();
                            if (skipUnmapped && sr.getReadUnmappedFlag()) {
                                continue;
                            }
                            symList.add(convertSAMRecordToSymWithProps(sr, seq, uri.toString()));
                        } catch (SAMException e) {
                            errList.add(e);
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            throw new Exception(ex);
        } finally {
            if (iter != null) {
                iter.close();
            }

            if (!errList.isEmpty()) {
                ErrorHandler.errorPanel("SAM exception", "Ignoring " + errList.size() + " records", errList, Level.WARNING);
            }
        }

        return symList;
    }

    /**
     * Returns a list of symmetries for the entire file. Good for loading DAS/2
     * derived data slices, skips building an index.
     */
    public List<SeqSymmetry> parseAll(BioSeq seq, String method) {
        reader = new SAMFileReader(new File(uri));
        reader.setValidationStringency(ValidationStringency.SILENT);
        List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
        if (reader != null) {
            for (final SAMRecord sr : reader) {
                if (skipUnmapped && sr.getReadUnmappedFlag()) {
                    continue;
                }
                symList.add(convertSAMRecordToSymWithProps(sr, seq, method));
            }
        }
        return symList;
    }

    public SeqSymmetryIterator getIterator(final BioSeq seq, final int min, final int max, final boolean contained) throws Exception {
        init();
        if (reader != null) {
            CloseableIterator<SAMRecord> iter = reader.query(seqs.get(seq), max - 1, max, contained);
            while (iter.hasNext()) {
                iter.next();
            }
            iter.close();
            long endPosition = getCompressedInputStreamPosition(reader).getApproximatePosition();
            iter = reader.query(seqs.get(seq), min, max, contained);
            long startPosition = getCompressedInputStreamPosition(reader).getApproximatePosition();
            return new SeqSymmetryIterator(seq, iter, startPosition, endPosition);
        }

        return null;
    }

    /**
     * Write annotations from min-max on the given chromosome to stream.
     *
     * @param seq -- chromosome
     * @param min -- min coordinate
     * @param max -- max coordinate
     * @param dos -- output stream
     * @param BAMWriter -- write as BAM or as SAM
     */
    public void writeAnnotations(BioSeq seq, int min, int max, DataOutputStream dos, boolean BAMWriter) throws Exception {
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
                Logger.getLogger(BAM.class.getName()).log(Level.INFO, "No overlapping bam alignments.", "Min-Max: " + min + "-" + max);
                return;
            }
			//write out records to file
            //TODO: is this hack necessary with updated picard.jar?
            reader.getFileHeader().setSortOrder(net.sf.samtools.SAMFileHeader.SortOrder.coordinate); // A hack to prevent error caused by picard tool.

            net.sf.samtools.SAMFileWriterFactory sfwf = new net.sf.samtools.SAMFileWriterFactory();
            if (BAMWriter) {
					// BAM files cannot be written to the stream one line at a time.
                // Rather, a tempfile is created, and later read into the stream.
                try {
                    tempBAMFile = File.createTempFile(CLEAN.matcher(featureName).replaceAll("_"), ".bam");
                    tempBAMFile.deleteOnExit();
                } catch (IOException ex) {
                    Logger.getLogger(BAM.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Cannot create temporary BAM file! \n" + ex.getStackTrace());
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
        } catch (Exception ex) {
            Logger.getLogger(BAM.class.getName()).log(Level.SEVERE, "SAM exception A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your BAM files and contact the Picard project at http://picard.sourceforge.net."
                    + "See console and the tomcat catalina.out for the details of the exception.\n", ex);
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

    /**
     * Modified to look for both xxx.bai and xxx.bam.bai files in parent directory.
     * @param bamfile
     * @return file
     * @throws com.affymetrix.genometryImpl.symloader.BAM.BamIndexNotFoundException 
     */
    public static File findIndexFile(File bamfile) throws BamIndexNotFoundException {
        //look for xxx.bam.bai
        try {
            String path = bamfile.getPath();
            File f = new File(path + ".bai");
            if (f.exists()) {
                return f;
            }

            //look for xxx.bai
            path = path.substring(0, path.length() - 3) + "bai";
            f = new File(path);
            if (f.exists()) {
                return f;
            }
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                Logger.getLogger(BAM.class.getName()).log(
                        Level.WARNING, null, e);
            }

        }
        throw new BamIndexNotFoundException();
    }

    public static String findIndexFile(String bamfile) throws BamIndexNotFoundException {
        // Guess at the location of the .bai URL as BAM URL + ".bai"        
        try {
            String baiUriStr = bamfile + ".bai";
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }

            baiUriStr = bamfile.substring(0, bamfile.length() - 3) + "bai";

            //look for xxx.bai
            if (LocalUrlCacher.isValidURL(baiUriStr)) {
                return baiUriStr;
            }
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                Logger.getLogger(BAM.class.getName()).log(
                        Level.WARNING, null, e);
            }
        }
        throw new BamIndexNotFoundException();
    }

    public static boolean hasIndex(URI uri) throws BamIndexNotFoundException {
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL)) {
            File f = findIndexFile(new File(uri));
            return f != null;
        } else if (StringUtils.equals(scheme, HTTP_PROTOCOL) || StringUtils.equals(scheme, HTTPS_PROTOCOL) || StringUtils.equals(scheme, FTP_PROTOCOL)) {
            String uriStr = findIndexFile(uri.toString());
            return uriStr != null;
        }

        return false;
    }

	//Can be used later. Do not remove.
    //Moving to a new plugin project BAMIndexer -KTS
    @SuppressWarnings("unused")
    static private File createIndexFile(File bamfile) throws IOException {
        File indexfile = File.createTempFile(bamfile.getName(), ".bai");

        if (!indexfile.exists()) {
            ErrorHandler.errorPanel("Unable to create file.");
            return null;
        }

        if (DEBUG) {
            System.out.println("Creating new bam index file -> " + indexfile);
        }

        String input = "INPUT=" + bamfile.getAbsolutePath();
        String output = "OUTPUT=" + indexfile.getAbsolutePath();
        String quiet = "QUIET=" + !DEBUG;
        BuildBamIndex buildIndex = new BuildBamIndex();
        buildIndex.instanceMain(new String[]{input, output, quiet});

        return indexfile;
    }

    public String getMimeType() {
        return "binary/BAM";
    }

    public static List<? extends SeqSymmetry> parse(URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName, SeqSpan overlap_span) throws Exception {
        File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
        bamfile.deleteOnExit();
        BAM bam = new BAM(bamfile.toURI(), featureName, group);
        //for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
        if (uri.getScheme().equals("http")) {
            return bam.parseAll(overlap_span.getBioSeq(), uri.toString());
        }
        return bam.getRegion(overlap_span);
    }

    public List<? extends SeqSymmetry> parse(InputStream is, boolean annotate_seq)
            throws Exception {
        throw new IllegalStateException(); // should not happen
    }

    public class SeqSymmetryIterator implements CloseableIterator<SeqSymmetry> {

        final BioSeq seq;
        final CloseableIterator<SAMRecord> iter;
        private SeqSymmetry next = null;

        SeqSymmetryIterator(BioSeq seq, CloseableIterator<SAMRecord> iter, long startPosition, long endPosition) {
            super();
            this.seq = seq;
            this.iter = iter;
            next = getNext();
        }

        @Override
        public final boolean hasNext() {
            return next != null;
        }

        @Override
        public final SeqSymmetry next() {
            SeqSymmetry sym = next;
            next = getNext();
            return sym;
        }

        @Override
        public final void remove() {
            iter.remove();
        }

        @Override
        public final void close() {
            iter.close();
        }

        private SeqSymmetry getNext() {
            while (iter.hasNext()) {
                try {
                    SAMRecord sr = iter.next();

                    if (skipUnmapped && sr.getReadUnmappedFlag()) {
                        continue;
                    }

                    return convertSAMRecordToSymWithProps(sr, seq, uri.toString(), false);
                } catch (SAMException ex) {
                    System.err.print("!!! SAM Record Error:" + ex.getMessage());
                } catch (NoSuchElementException ex) {
                    //FIXME: This exception occurs at the end of query
                } catch (Exception ex) {
                    System.err.print("!!! Error:" + ex.getMessage());
                }
            }
            return null;
        }

    }

    public static class BamIndexNotFoundException extends Exception {

        private static final long serialVersionUID = -3711705910840303497L;

        public BamIndexNotFoundException() {
            super("Could not find Bam Index File.");
        }

        public BamIndexNotFoundException(String message) {
            super(message);
        }
    }
}
