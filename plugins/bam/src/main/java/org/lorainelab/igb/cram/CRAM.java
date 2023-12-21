package org.lorainelab.igb.cram;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symloader.Das2SliceSupport;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.LocalUrlCacher;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.CRAMException;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.FileExtensions;
import org.apache.commons.lang3.StringUtils;
import org.lorainelab.igb.Exception.IndexFileNotFoundException;
import org.lorainelab.igb.bam.XAM;
import org.lorainelab.igb.cache.api.CacheStatus;
import org.lorainelab.igb.util.IndexFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL_SCHEME;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;

public class CRAM extends XAM implements Das2SliceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CRAM.class);
    private static final Integer CRAM_EXTENSION_LENGTH = 5;
    public final static List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("cram");
    }

    public CRAM(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion seq_group) {
        super(uri, indexUri, featureName, seq_group);
    }
    private SamReader getSAMReader() throws IOException, IndexFileNotFoundException {
        SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault().referenceSource(new IGBReferenceSource()).validationStringency(ValidationStringency.SILENT);
        SamInputResource samInputResource=null;
        File indexFile = null;
        SamReader samReader = null;
        String scheme = uri.getScheme().toLowerCase();
        if(StringUtils.equals(scheme, FILE_PROTOCOL_SCHEME)){
            File f = new File(uri);
            indexFile = IndexFileUtil.findIndexFile(f, FileExtensions.CRAM_INDEX, CRAM_EXTENSION_LENGTH);
            samInputResource = SamInputResource.of(f).index(indexFile);
            samReader = samReaderFactory.open(samInputResource);
        }else if(StringUtils.equals(scheme, HTTP_PROTOCOL_SCHEME) || StringUtils.equals(scheme, HTTPS_PROTOCOL_SCHEME)){
            String reachableURL = LocalUrlCacher.getReachableUrl(uri.toString());
            if(reachableURL == null){
                ErrorHandler.errorPanel("URL cannot be reached");
                this.isInitialized=false;
                return null;
            }
            URI CRAIUri = indexUri;
            if(indexUri == null){
                CRAIUri = URI.create(getCRAMIndexUri(uri));
            }
            CacheStatus indexCacheStatus = null;
            boolean localFile = LocalUrlCacher.isLocalFile(CRAIUri);
            if(!localFile){
                Optional<InputStream> indexStream = remoteFileCacheService.getFilebyUrl(CRAIUri.toURL(), false);
                if(indexStream.isPresent()){
                    indexStream.get().close();
                }
                indexCacheStatus = remoteFileCacheService.getCacheStatus(CRAIUri.toURL());
            }
            SeekableBufferedStream seekableStream = new SeekableBufferedStream(new SeekableHTTPStream(new URL(reachableURL)));
            if(!localFile && indexCacheStatus != null && indexCacheStatus.isDataExists() && !indexCacheStatus.isCorrupt()) {
                samInputResource = SamInputResource.of(seekableStream).index(indexCacheStatus.getData());
            } else {
                indexFile = LocalUrlCacher.convertURIToFile(CRAIUri);
                samInputResource = SamInputResource.of(seekableStream).index(indexFile);
            }
            samReader = samReaderFactory.open(samInputResource);

        }else if(scheme.startsWith(FTP_PROTOCOL_SCHEME)){
            URI CRAIUri = indexUri;
            if(CRAIUri==null){
                CRAIUri = URI.create(getCRAMIndexUri(CRAIUri));
            }
            indexFile = LocalUrlCacher.convertURIToFile(CRAIUri);
            samInputResource = SamInputResource.of(uri.toURL()).index(indexFile);
            samReader = samReaderFactory.open(samInputResource);
        }else{
            LOG.info("URL scheme: {0} not recognized", scheme);
            return null;
        }
        return samReader;
    }

    private String getCRAMIndexUri(URI uri) throws IndexFileNotFoundException {
        String CRAIUri = IndexFileUtil.findIndexFile(uri.toString(), FileExtensions.CRAM_INDEX, CRAM_EXTENSION_LENGTH);
        if(StringUtils.isBlank(CRAIUri)){
            ErrorHandler.errorPanel("CRAM index file not found", "Could not find URL of CRAM index file at "+ uri +". Please be sure this is in the same directory as the BAM file", Level.SEVERE);
            this.isInitialized=false;
            throw new IndexFileNotFoundException("could not find CRAM index file");
        }
        return CRAIUri;
    }
    @Override
    public List<SeqSymmetry> parse(SeqSpan span) throws Exception {
        init();
        BioSeq seq = span.getBioSeq();
        int min = span.getStart();
        int max = span.getEnd();
        List<SeqSymmetry> symList = new ArrayList<>(1000);
        List<Throwable> errors = new ArrayList<>(10);
        CloseableIterator<SAMRecord> iterator = null;
        try {
            if(reader!=null){
               iterator = reader.query(seqs.get(seq), min, max, false);
               if(iterator!=null){
                   SAMRecord sr = null;
                   while (iterator.hasNext() && !Thread.currentThread().isInterrupted()){
                       try{
                           sr = iterator.next();
                           if(skipUnmapped && sr.getReadUnmappedFlag()){
                               continue;
                           }
                           symList.add(convertSAMRecordToSymWithProps(sr, seq, uri.toString()));
                       }catch (SAMException ex){
                           errors.add(ex);
                       }
                   }
               }
            }
        }catch (Throwable ex){
            if(ex instanceof CRAMException){
                ErrorHandler.errorPanel("CRAM Exception: possible sequence mismatch.", ex.getMessage(), Level.WARNING);
            }else{
                throw new Exception(ex);
            }
        }finally {
            if (iterator!=null){
                iterator.close();
            }
            if(!errors.isEmpty()){
                ErrorHandler.errorPanel("SAM exception", "Ignoring " + errors.size() + " records", errors, Level.WARNING);
            }
        }
        return symList;
    }
    @Override
    public void init(){
        if(this.isInitialized) return;
        LOG.info("Initialising CRAM parser");
        try {
            reader = getSAMReader();
            header = reader.getFileHeader();
            if (initTheSeqs()) {
                super.init();
            }
        }catch (SAMFormatException ex){
            ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n"
                    + "Please validate your CRAM files (see http://broadinstitute.github.io/picard/command-line-overview.html#ValidateSamFile). "
                    + "See console for the details of the exception.\n", Level.SEVERE);
            ex.printStackTrace();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public List<SeqSymmetry> parseAll(BioSeq seq, String method) {
        return new ArrayList<>();
    }
}
