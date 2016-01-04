package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTPS_PROTOCOL;
import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL;
import static com.affymetrix.genometry.symloader.SymLoader.remoteFileCacheService;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.BlockCompressedStreamPosition;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.lorainelab.igb.cache.api.CacheStatus;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.seekablestream.SeekableStream;
import net.sf.samtools.seekablestream.SeekableStreamFactory;
import net.sf.samtools.util.BlockCompressedInputStream;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.readers.TabixIteratorLineReader;
import org.broad.tribble.readers.TabixReader;
import org.slf4j.LoggerFactory;

/**
 * This SymLoader is intended to be used for data sources that are indexed with a tabix file. This SymLoader uses the
 * TabixReader from the Broad Institute
 */
public class SymLoaderTabix extends SymLoader {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SymLoaderTabix.class);

    private static final int MAX_ACTIVE_POOL_OBJECTS = Runtime.getRuntime().availableProcessors() + 1;
    protected final Map<BioSeq, String> seqs = Maps.newConcurrentMap();
    private final LineProcessor lineProcessor;
    private final GenericObjectPool<TabixReaderCached> pool;
    private static final List<LoadStrategy> strategyList = new ArrayList<>();

    static {
        strategyList.add(LoadStrategy.NO_LOAD);
        strategyList.add(LoadStrategy.AUTOLOAD);
        strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
        strategyList.add(LoadStrategy.GENOME);
    }

    public SymLoaderTabix(final URI uri, final Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion, LineProcessor lineProcessor) throws Exception {
        super(uri, indexUri, featureName, genomeVersion);
        this.lineProcessor = lineProcessor;
        PoolableObjectFactory<TabixReaderCached> poolFactory = new TabixReaderPoolableObjectFactory();
        this.pool = new GenericObjectPool<>(poolFactory);
        //Do not leave open connections
        this.pool.setMinIdle(0);
        // Set maximum number of object to be created
        this.pool.setMaxActive(MAX_ACTIVE_POOL_OBJECTS);

        TabixReaderCached test = pool.borrowObject();
        if (!poolFactory.validateObject(test)) {
            throw new IllegalStateException("tabix file does not exist or was not read");
        }
        pool.returnObject(test);

        // Make sure object is not null
        this.pool.setTestOnBorrow(true);
        this.pool.setTestOnReturn(true);
        this.pool.setTestWhileIdle(true);
        this.pool.setMinEvictableIdleTimeMillis(30000);
        this.pool.setTimeBetweenEvictionRunsMillis(5000);
    }

    public final class TabixReaderCached extends TabixReader {

        private String indexFile;

        public TabixReaderCached(String fn, String indexFile) throws IOException {
            super(fn);
            this.indexFile = indexFile;
            readIndex();
        }

        public TabixReaderCached(String fn) throws IOException {
            super(fn);
        }

        public TabixReaderCached(String fn, String idxFn, SeekableStream stream) throws IOException {
            super(fn, idxFn, stream);
        }

        @Override
        public void readIndex() throws IOException {
            if (!Strings.isNullOrEmpty(indexFile)) {
                readIndex(SeekableStreamFactory.getInstance().getStreamFor(indexFile));
            }
        }

    }

    @Override
    public List<LoadStrategy> getLoadChoices() {
        return strategyList;
    }

    @Override
    public void init() throws Exception {
        if (this.isInitialized) {
            return;
        }

        lineProcessor.init(uri);
        TabixReaderCached tabixReader = pool.borrowObject();
        try {
            for (String seqID : tabixReader.mChr2tid.keySet()) {
                BioSeq seq = genomeVersion.getSeq(seqID);
                if (seq == null) {
                    //int length = 1000000000;
                    int length = 200000000;
                    seq = genomeVersion.addSeq(seqID, length);
//				Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.INFO,
//						"Sequence not found. Adding {0} with default length {1}",
//						new Object[]{seqID,length});
                }
                seqs.put(seq, seqID);
            }
            this.isInitialized = true;
        } catch (Exception ex) {
            throw ex;
        } finally {
            pool.returnObject(tabixReader);

        }
    }

    public LineProcessor getLineProcessor() {
        return lineProcessor;
    }

    @Override
    public List<String> getFormatPrefList() {
        return lineProcessor.getFormatPrefList();
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        init();
        return ImmutableList.copyOf(seqs.keySet());
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        init();
        List<BioSeq> allSeq = getChromosomeList();
        List<SeqSymmetry> retList = new ArrayList<>();
        for (BioSeq seq : allSeq) {
            retList.addAll(getChromosome(seq));
        }
        return retList;
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        return getRegion(new SimpleSeqSpan(0, Integer.MAX_VALUE / 2, seq)); // end faked
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        init();
        String seqID = seqs.get(overlapSpan.getBioSeq());
        TabixReaderCached tabixReader = pool.borrowObject();
        try {
            if (!tabixReader.mChr2tid.containsKey(seqID)) {
                return new ArrayList<>();
            }
//			System.out.println("Total :" + (pool.getNumActive() + pool.getNumIdle()));
            final LineReader lineReader = new TabixIteratorLineReader(tabixReader.query(tabixReader.mChr2tid.get(seqID), overlapSpan.getStart(), overlapSpan.getEnd()));
            long[] startEnd = getStartEnd(lineReader);
            if (startEnd == null) {
                return new ArrayList<>();
            }
            return lineProcessor.processLines(overlapSpan.getBioSeq(), lineReader);
        } catch (Exception ex) {
            throw ex;
        } finally {
            pool.returnObject(tabixReader);

        }
    }

    private long[] getStartEnd(LineReader lineReader) {
        long[] startEnd = new long[2];
        try {
            Field field = lineReader.getClass().getDeclaredField("iterator");
            field.setAccessible(true);
            Object it = field.get(lineReader);
            // Probably no data in the region
            if (it == null) {
                return null;
            }
            field = it.getClass().getDeclaredField("off");
            field.setAccessible(true);
            Object[] off = (Object[]) field.get(it);
            field = off[0].getClass().getDeclaredField("u");
            field.setAccessible(true);
            long startPos = (Long) field.get(off[0]);
            startEnd[0] = new BlockCompressedStreamPosition(startPos).getApproximatePosition();
            field = off[off.length - 1].getClass().getDeclaredField("v");
            field.setAccessible(true);
            long endPos = (Long) field.get(off[0]);
            startEnd[1] = new BlockCompressedStreamPosition(endPos).getApproximatePosition();
        } catch (IllegalAccessException x) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "unable to display progress for " + uri, x);
        } catch (NoSuchFieldException x) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "unable to display progress for " + uri, x);
        }
        return startEnd;
    }

    /**
     * copied from the igv 1.5.64 source
     *
     * @param path path of data source
     * @return if the data source has a valid tabix index
     */
    public static boolean isTabix(String path) {
        if (!path.endsWith("gz")) {
            return false;
        }

        BlockCompressedInputStream is = null;
        try {
            if (path.startsWith(FTP_PROTOCOL)) {
                return false; // ftp not supported by BlockCompressedInputStream
            } else if (path.startsWith(HTTP_PROTOCOL) || path.startsWith(HTTPS_PROTOCOL)) {
                final URL url = new URL(path + ".tbi");
                if (remoteFileCacheService != null && remoteFileCacheService.cacheExists(url)) {
                    Optional<InputStream> inputStream = remoteFileCacheService.getFilebyUrl(url, true);
                    if (inputStream.isPresent()) {
                        is = new BlockCompressedInputStream(inputStream.get());
                    } else {
                        is = new BlockCompressedInputStream(LocalUrlCacher.getInputStream(url));
                    }
                } else {
                    is = new BlockCompressedInputStream(LocalUrlCacher.getInputStream(url));
                }
            } else {
                is = new BlockCompressedInputStream(new File(URLDecoder.decode(path, GeneralUtils.UTF8) + ".tbi"));
            }

            byte[] bytes = new byte[4];
            is.read(bytes);
            return (char) bytes[0] == 'T' && (char) bytes[1] == 'B';
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        } finally {
            GeneralUtils.safeClose(is);
        }
    }

    public static SymLoader getSymLoader(SymLoader sym) {
        try {
            URI uri = new URI(sym.uri.toString() + ".tbi");
            if (sym instanceof LineProcessor && LocalUrlCacher.isValidRequest(uri)) {
                return new SymLoaderTabix(sym.uri, Optional.ofNullable(sym.getIndexUri()), sym.featureName, sym.genomeVersion, (LineProcessor) sym);
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception x) {
            Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.SEVERE,
                    "Could not initialize tabix line reader for {0}.",
                    new Object[]{sym.featureName});
        }
        return sym;
    }

    @Override
    public boolean isMultiThreadOK() {
        return lineProcessor.isMultiThreadOK();
    }

    private class TabixReaderPoolableObjectFactory extends BasePoolableObjectFactory<TabixReaderCached> {

        @Override
        public void destroyObject(TabixReaderCached obj) throws Exception {
            obj.close();
        }

        @Override
        public TabixReaderCached makeObject() throws Exception {
            String uriString = uri.toString();
            if (uriString.startsWith(FILE_PROTOCOL)) {
                uriString = uri.getPath();
            } else {
                URL fileUrl = uri.toURL();
                if (BedUtils.isRemoteBedFile(fileUrl)
                        && remoteFileCacheService.cacheExists(fileUrl)) {
                    Optional<InputStream> fileIs = Optional.empty();
                    Optional<InputStream> indexFileIs = Optional.empty();
                    try {
                        fileIs = remoteFileCacheService.getFilebyUrl(fileUrl, true);
                        if (indexUri != null) {
                            
                        } else {
                            indexUri = new URI(fileUrl.toString() + ".tbi");
                        }
                        indexFileIs = remoteFileCacheService.getFilebyUrl(indexUri.toURL(), true);
                        if (fileIs.isPresent() && indexFileIs.isPresent()) {
                            CacheStatus cacheStatus = remoteFileCacheService.getCacheStatus(fileUrl);
                            CacheStatus indexFileCacheStatus = remoteFileCacheService.getCacheStatus(indexUri.toURL());
                            if (cacheStatus.isDataExists() && indexFileCacheStatus.isDataExists()) {
                                return new TabixReaderCached(cacheStatus.getData().getAbsolutePath(), indexFileCacheStatus.getData().getAbsolutePath());
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    } finally {
                        if (fileIs.isPresent()) {
                            fileIs.get().close();
                        }
                        if (indexFileIs.isPresent()) {
                            indexFileIs.get().close();
                        }
                    }
                }
            }
            return new TabixReaderCached(uriString, uriString + ".tbi");
        }

        @Override
        public boolean validateObject(TabixReaderCached tabixReader) {
            return tabixReader != null;
        }
    }
}
