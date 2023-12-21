package org.lorainelab.igb.bam;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symloader.Das2SliceSupport;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.BlockCompressedStreamPosition;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LocalUrlCacher;
import htsjdk.samtools.util.FileExtensions;
import org.lorainelab.igb.Exception.IndexFileNotFoundException;
import org.lorainelab.igb.cache.api.CacheStatus;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.SAMFileWriterFactory;
import org.apache.commons.lang3.StringUtils;
import org.lorainelab.igb.util.IndexFileUtil;
import org.slf4j.LoggerFactory;

/**
 * @author jnicol
 */
public final class BAM extends XAM implements Das2SliceSupport {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BAM.class);
    public final static List<String> pref_list = new ArrayList<>();
    /*SAMFileReader was changed to SamReader to accommodate the changes in the new library.*/
    private SamReader mateReader;

    private static final Integer BAM_EXTENSION_LENGTH = 4;
    
    //FYI: the variable reader is inherited from the parent class XAM.java

    static {
        pref_list.add("bam");
    }

    private static final Pattern CLEAN = Pattern.compile("[/\\s+]");

    protected SAMFileHeader header;

    public BAM(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion seq_group) {
        super(uri, indexUri, featureName, seq_group);
        strategyList.add(LoadStrategy.AUTOLOAD);
    }

    private SamReader getSAMFileReader() throws IOException, IndexFileNotFoundException {

        final SamReaderFactory factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        SamInputResource resource = null;
        File indexFile = null;
        SamReader samFileReader = null;
        String scheme = uri.getScheme().toLowerCase();
        if (StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)) {
            // BAM is file.
            //indexFile = new File(uri.)
            File f = new File(uri);
            indexFile = IndexFileUtil.findIndexFile(f, FileExtensions.BAI_INDEX, BAM_EXTENSION_LENGTH);
            resource = SamInputResource.of(f).index(indexFile);
            samFileReader = factory.open(resource);
        } else if (StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME)) {
            String reachable_url = LocalUrlCacher.getReachableUrl(uri.toASCIIString());

            if (reachable_url == null) {
                ErrorHandler.errorPanel("Url cannot be reached");
                this.isInitialized = false;
                return null;
            }
            URI baiUri = indexUri;
            if (indexUri == null) {
                baiUri = URI.create(getBamIndexUriStr(uri));
            }
            CacheStatus indexCacheStatus = null;
            boolean localFile = LocalUrlCacher.isLocalFile(baiUri);
            if (!localFile) {
                Optional<InputStream> indexStream = remoteFileCacheService.getFilebyUrl(baiUri.toURL(), false);
                if (indexStream.isPresent()) {
                    indexStream.get().close();
                }
                indexCacheStatus = remoteFileCacheService.getCacheStatus(baiUri.toURL());

            }

            SeekableBufferedStream seekableStream = new SeekableBufferedStream(new SeekableHTTPStream(new URL(reachable_url)));
            if (!localFile && indexCacheStatus != null && indexCacheStatus.isDataExists() && !indexCacheStatus.isCorrupt()) {
                resource = SamInputResource.of(seekableStream).index(indexCacheStatus.getData());
                samFileReader = factory.open(resource);
            } else {
                indexFile = LocalUrlCacher.convertURIToFile(baiUri);
                resource = SamInputResource.of(seekableStream).index(indexFile);
                samFileReader = factory.open(resource);
            }
        } else if (scheme.startsWith(FTP_PROTOCOL_SCHEME)) {
            URI baiUri = indexUri;
            if (indexUri == null) {
                baiUri = URI.create(getBamIndexUriStr(uri));
            }
            indexFile = LocalUrlCacher.convertURIToFile(baiUri);

            resource = SamInputResource.of(uri.toURL()).index(indexFile);
            samFileReader = factory.open(resource);
        } else {
            Logger.getLogger(BAM.class.getName()).log(
                    Level.SEVERE, "URL scheme: {0} not recognized", scheme);
            return null;
        }
        return samFileReader;
    }

    private String getBamIndexUriStr(URI uri) throws IndexFileNotFoundException {
        // BAM is URL.  Get the indexed .bai file, and query only the needed portion of the BAM file.
        String baiUriStr = IndexFileUtil.findIndexFile(uri.toString(), FileExtensions.BAI_INDEX, BAM_EXTENSION_LENGTH);
        // Guess at the location of the .bai URL as BAM URL + ".bai"
        if (StringUtils.isBlank(baiUriStr)) {
            ErrorHandler.errorPanel("No BAM index file",
                    "Could not find URL of BAM index at " + uri.toString() + ". Please be sure this is in the same directory as the BAM file.", Level.SEVERE);
            this.isInitialized = false;
            throw new IndexFileNotFoundException();
        }
        return baiUriStr;
    }

    @Override
    public void init() throws IOException, IndexFileNotFoundException {
        if (this.isInitialized) {
            return;
        }

        try {
            reader = getSAMFileReader();
            mateReader = getSAMFileReader();
            //set header
            header = reader.getFileHeader();

            if (initTheSeqs()) {
                super.init();
            }
        } catch (SAMFormatException ex) {
            ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your BAM files (see http://broadinstitute.github.io/picard/command-line-overview.html#ValidateSamFile). "
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
    private BlockCompressedStreamPosition getCompressedInputStreamPosition(SamReader sfr) throws Exception {
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
        int currentOffsetValue = ((Integer) privateCurrentOffsetField.get(compressedInputStreamValue));
        return new BlockCompressedStreamPosition(blockAddressValue, currentOffsetValue);
    }

    /**
     * Return a list of symmetries for the given chromosome range.
     */
    public synchronized List<SeqSymmetry> parse(SeqSpan span) throws Exception {
        init();
        BioSeq seq = span.getBioSeq();
        int min = span.getMin();
        int max = span.getMax();
        List<SeqSymmetry> symList = new ArrayList<>(1000);
        List<Throwable> errList = new ArrayList<>(10);
        boolean contained = false;
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

                            BAMSym bamSym = (BAMSym) convertSAMRecordToSymWithProps(sr, seq, uri.toString());
                            symList.add(bamSym);

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

    private Optional<BAMSym> getReadMate(SAMRecord sr, BioSeq seq) {
        SAMRecord mateSAMRecord = mateReader.queryMate(sr);
        //check to be sure the mate is in the same BioSeq else ignore mate
        if (mateSAMRecord.getAlignmentStart() > seq.getMax()) {
            return Optional.empty();
        } else {
            BAMSym mateBamSym = (BAMSym) convertSAMRecordToSymWithProps(mateSAMRecord, seq, uri.toString());
            return Optional.ofNullable(mateBamSym);
        }
    }

    /**
     * Returns a list of symmetries for the entire file. Good for loading DAS/2 derived data slices, skips building an
     * index.
     */
    @Override
    public List<SeqSymmetry> parseAll(BioSeq seq, String method) {

        final SamReaderFactory factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        SamInputResource resource = null;
        reader = factory.open(new File(uri));
        List<SeqSymmetry> symList = new ArrayList<>(1000);
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
            iter = reader.query(seq.getId(), min, max, false);
            //check for any records
            if (!iter.hasNext()) {
                Logger.getLogger(BAM.class.getName()).log(Level.INFO, "No overlapping bam alignments.", "Min-Max: " + min + "-" + max);
                return;
            }
            //write out records to file
            //TODO: is this hack necessary with updated picard.jar?
            reader.getFileHeader().setSortOrder(SAMFileHeader.SortOrder.coordinate); // A hack to prevent error caused by picard tool.

            SAMFileWriterFactory sfwf = new SAMFileWriterFactory();
            if (BAMWriter) {
                // BAM files cannot be written to the stream one line at a time.
                // Rather, a tempfile is created, and later read into the stream.
                try {
                    tempBAMFile = File.createTempFile(CLEAN.matcher(featureName).replaceAll("_"), ".bam");
                    tempBAMFile.deleteOnExit();
                } catch (IOException ex) {
                    logger.error("Cannot create temporary BAM file", ex);
                    return;
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
            logger.error("SAM exception A SAMFormatException has been thrown by the Picard tools.\n"
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

    public String getMimeType() {
        return "binary/BAM";
    }

    public static List<? extends SeqSymmetry> parse(URI uri, Optional<URI> indexUri, InputStream istr, GenomeVersion genomeVersion, String featureName, SeqSpan overlap_span) throws Exception {
        File bamfile = GeneralUtils.convertStreamToFile(istr, featureName);
        bamfile.deleteOnExit();
        BAM bam = new BAM(bamfile.toURI(), indexUri, featureName, genomeVersion);
        //for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
        if (uri.getScheme().equals(HTTP_PROTOCOL_SCHEME)) {
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
}
