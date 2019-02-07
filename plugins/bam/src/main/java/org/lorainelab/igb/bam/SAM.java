package org.lorainelab.igb.bam;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symloader.LineProcessor;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.util.BufferedLineReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.readers.LineReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import java.util.Iterator;

/**
 *
 * @author hiralv
 */
public class SAM extends XAM implements LineProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SAM.class);

    public SAM(URI uri, String featureName, GenomeVersion seq_group) {
        super(uri, Optional.empty(), featureName, seq_group);
    }

    @Override
    public void init() throws Exception {
        try {
            final SamReaderFactory factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
            SamInputResource resource = null;
            String scheme = uri.getScheme().toLowerCase();
            if (StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)) {
                File f = new File(uri);
                resource = SamInputResource.of(f);
                reader = factory.open(resource);
            } else if (StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME)) {
                String reachable_url = LocalUrlCacher.getReachableUrl(uri.toASCIIString());

                if (reachable_url == null) {
                    ErrorHandler.errorPanel("The data set you requested could not be loaded as the URL cannot be reached. \n"
                           + "Select Help > Show Console for more information");
                    this.isInitialized = false;
                    return ;
                }
                SeekableBufferedStream seekableStream = new SeekableBufferedStream(new SeekableHTTPStream(new URL(reachable_url)));
                resource = SamInputResource.of(seekableStream);
                reader = factory.open(resource);
            }else if (scheme.startsWith(FTP_PROTOCOL_SCHEME)) {
                resource = SamInputResource.of(uri.toURL());
                reader = factory.open(resource);
            } else {
                logger.error( "URL scheme: {0} not recognized", scheme);
                ErrorHandler.errorPanel("The data set you requested could not be loaded as the URL cannot be reached. \n"
                        + "Select Help > Show Console for more information");
                this.isInitialized = false;
                return ;
            }
            if (this.isInitialized) {
                return;
            }

            if (initTheSeqs()) {
                super.init();
            }

        } catch (SAMFormatException ex) {
            ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your BAM files (see http://broadinstitute.github.io/picard/command-line-overview.html#ValidateSamFile). "
                    + "See console for the details of the exception.\n", Level.SEVERE);
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    protected boolean initTheSeqs() {
        boolean ret = super.initTheSeqs();

        if (ret) {
            if (header.getSortOrder() != SAMFileHeader.SortOrder.coordinate) {
                Logger.getLogger(SAM.class.getName()).log(Level.SEVERE, "Sam file must be sorted by coordinate.");
                return false;
            }
        }
        return ret;
    }

    @Override
    public List<SeqSymmetry> parse(SeqSpan span) throws Exception {
        init();
        if (reader != null) {
            CloseableIterator<SAMRecord> iter = reader.iterator();
            if (iter != null && iter.hasNext()) {
                return parse(iter, span, true);
            }
        }

        return Collections.<SeqSymmetry>emptyList();
    }

    public List<SeqSymmetry> parse(CloseableIterator<SAMRecord> iter, SeqSpan span, boolean check) throws Exception {
        BioSeq seq = span.getBioSeq();
        int min = span.getMin();
        int max = span.getMax();
        List<SeqSymmetry> symList = new ArrayList<>(1000);
        List<Throwable> errList = new ArrayList<>();
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
                            if (maximum > max) {
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
            return symList;
        } finally {
            if (iter != null) {
                iter.close();
            }
            if (!errList.isEmpty()) {
                Iterator<Throwable> error = errList.iterator();
                String errMessage = "";
                while(error.hasNext()) {
                    errMessage += error.next() +"<br>";
                }
                ErrorHandler.errorPanel("SAM exception", errMessage +"<br>Ignoring " + errList.size() + " records <br>", errList, Level.WARNING);
            }
        }
    }

    private static boolean checkRange(int start, int end, int min, int max) {

        //getChromosome && getRegion
        if (end < min || start > max) {
            return false;
        }

        return true;
    }

    @Override
    public List<? extends SeqSymmetry> processLines(BioSeq seq, LineReader lineReader) throws Exception {
        throw new UnsupportedOperationException("Cannot query SAM text files");
    }


    public void init(URI uri) {

        final SamReaderFactory factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        SamInputResource resource = SamInputResource.of(LocalUrlCacher.convertURIToBufferedStream(uri));
        reader = factory.open(resource);
        header = reader.getFileHeader();
    }

    private class AsciiTabixLineReader extends BufferedLineReader {

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
            } finally {
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
        public int peek() {
            return -1;
        }

    }

    @Override
    public SeqSpan getSpan(String line) {
        return null; // not used yet
    }

    public boolean processInfoLine(String line, List<String> infoLines) {
        return false; // not used yet
    }
}
