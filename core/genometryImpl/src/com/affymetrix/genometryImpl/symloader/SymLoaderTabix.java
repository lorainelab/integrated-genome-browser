package com.affymetrix.genometryImpl.symloader;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.BlockCompressedStreamPosition;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URL;

import net.sf.samtools.util.BlockCompressedInputStream;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import org.broad.tribble.readers.LineReader;
import org.broad.tribble.readers.TabixIteratorLineReader;
import org.broad.tribble.readers.TabixReader;

/**
 * This SymLoader is intended to be used for data sources that
 * are indexed with a tabix file. This SymLoader uses the TabixReader
 * from the Broad Institute
 */
public class SymLoaderTabix extends SymLoader {
	private static final int MAX_ACTIVE_POOL_OBJECTS = 8;
	protected final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();
	private final LineProcessor lineProcessor;
	private final GenericObjectPool<TabixReader> pool;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.AUTOLOAD);
		strategyList.add(LoadStrategy.VISIBLE);
//		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	public SymLoaderTabix(final URI uri, String featureName, AnnotatedSeqGroup group, LineProcessor lineProcessor) throws Exception {
		super(uri, featureName, group);
		this.lineProcessor = lineProcessor;
		this.pool = new GenericObjectPool<TabixReader>(new TabixReaderPoolableObjectFactory());
		// Always have minimum one reader in pool
		this.pool.setMinIdle(1);
		// Set maximum number of object to be created
		this.pool.setMaxActive(MAX_ACTIVE_POOL_OBJECTS);
		// Make sure object is not null
		this.pool.setTestOnBorrow(true);
		this.pool.setTestOnReturn(true);
		this.pool.setTestWhileIdle(true);
		
		// Test if it's working
		if (pool.borrowObject() == null) {
			throw new IllegalStateException("tabix file does not exist or was not read");
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
		TabixReader tabixReader = pool.borrowObject();
		try {
			for (String seqID : tabixReader.mChr2tid.keySet()) {
				BioSeq seq = group.getSeq(seqID);
				if (seq == null) {
					//int length = 1000000000;
					int length = 200000000;
					seq = group.addSeq(seqID, length);
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
	public List<BioSeq> getChromosomeList() throws Exception  {		
		init();
		return new ArrayList<BioSeq>(seqs.keySet());
	}

	@Override
	 public List<? extends SeqSymmetry> getGenome() throws Exception  {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<SeqSymmetry> retList = new ArrayList<SeqSymmetry>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	 }

	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception  {
		return getRegion(new SimpleSeqSpan(0, Integer.MAX_VALUE / 2, seq)); // end faked
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		init();
		String seqID = seqs.get(overlapSpan.getBioSeq());
		TabixReader tabixReader = pool.borrowObject();
		try {
			if (!tabixReader.mChr2tid.containsKey(seqID)) {
				return new ArrayList<SeqSymmetry>();
			}
//			System.out.println("Total :" + (pool.getNumActive() + pool.getNumIdle()));
			final LineReader lineReader = new TabixIteratorLineReader(tabixReader.query(tabixReader.mChr2tid.get(seqID), overlapSpan.getStart(), overlapSpan.getEnd()));
			long[] startEnd = getStartEnd(lineReader);
			if (startEnd == null) {
				return new ArrayList<SeqSymmetry>();
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
			if(it == null){
				return null;
			}
			field = it.getClass().getDeclaredField("off");
			field.setAccessible(true);
			Object[] off = (Object[])field.get(it);
			field = off[0].getClass().getDeclaredField("u");
			field.setAccessible(true);
			long startPos = (Long)field.get(off[0]);
			startEnd[0] = new BlockCompressedStreamPosition(startPos).getApproximatePosition();
			field = off[off.length - 1].getClass().getDeclaredField("v");
			field.setAccessible(true);
			long endPos = (Long)field.get(off[0]);
			startEnd[1] = new BlockCompressedStreamPosition(endPos).getApproximatePosition();
		}
		catch(IllegalAccessException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "unable to display progress for " + uri, x);
		}
		catch(NoSuchFieldException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "unable to display progress for " + uri, x);
		}
		return startEnd;
	}

    /**
     * copied from the igv 1.5.64 source
     * @param path path of data source
     * @return if the data source has a valid tabix index
     */
    public static boolean isTabix(String path) {
        if (!path.endsWith("gz")) {
            return false;
        }

        BlockCompressedInputStream is = null;
        try {
            if (path.startsWith("ftp:")) {
            	return false; // ftp not supported by BlockCompressedInputStream
            }
            else if (path.startsWith("http:") || path.startsWith("https:")) {
                is = new BlockCompressedInputStream(LocalUrlCacher.getInputStream(new URL(path + ".tbi")));
            }
            else {
                is = new BlockCompressedInputStream(new File(URLDecoder.decode(path, GeneralUtils.UTF8) + ".tbi"));
            }

            byte[] bytes = new byte[4];
            is.read(bytes);
            return (char) bytes[0] == 'T' && (char) bytes[1] == 'B';
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return false;
        }
        finally {
			GeneralUtils.safeClose(is);
        }
    }

    public static SymLoader getSymLoader(SymLoader sym){
		try {
			URI uri = new URI(sym.uri.toString() + ".tbi");
			if(LocalUrlCacher.isValidURI(uri)){
				String uriString = sym.uri.toString();
				if (uriString.startsWith(FILE_PREFIX)) {
					uriString = sym.uri.getPath();
				}
				if (isTabix(uriString) && sym instanceof LineProcessor) {
					return new SymLoaderTabix(sym.uri, sym.featureName, sym.group, (LineProcessor) sym);
				}
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
	public boolean isMultiThreadOK(){
		return lineProcessor.isMultiThreadOK();
	}
	
	private class TabixReaderPoolableObjectFactory extends BasePoolableObjectFactory<TabixReader> {
		@Override
		public TabixReader makeObject() throws Exception {
			String uriString = uri.toString();
			if (uriString.startsWith(FILE_PREFIX)) {
				uriString = uri.getPath();
			}
			return new TabixReader(uriString);			
		}
		
		@Override
		public boolean validateObject(TabixReader tabixReader){
			return tabixReader != null;
		}
	}
}

