package com.affymetrix.genometryImpl.symloader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.thread.PositionCalculator;
import com.affymetrix.genometryImpl.thread.ProgressUpdater;
import com.affymetrix.genometryImpl.util.BlockCompressedStreamPosition;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.broad.tribble.readers.LineReader;
import org.broad.tribble.source.tabix.TabixLineReader;
import org.broad.tribble.util.BlockCompressedInputStream;

/**
 * This SymLoader is intended to be used for data sources that
 * are indexed with a tabix file. This SymLoader uses the TabixReader
 * from the Broad Institute
 */
public class SymLoaderTabix extends SymLoader {

	protected final Map<BioSeq, String> seqs = new HashMap<BioSeq, String>();
	private TabixLineReader tabixLineReader;
	private final LineProcessor lineProcessor;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	public SymLoaderTabix(URI uri, String featureName, AnnotatedSeqGroup group, LineProcessor lineProcessor){
		super(uri, featureName, group);
		this.lineProcessor = lineProcessor;
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	/**
	 * @return if this SymLoader is valid, there is a readable
	 * tabix file for the data source
	 */
	public boolean isValid() {
		return tabixLineReader != null;
	}

	@Override
	public void init() throws Exception  {
		if (this.isInitialized){
			return;
		}
		try {
			String uriString = uri.toString();
			if (uriString.startsWith(FILE_PREFIX)) {
				uriString = uri.getPath();
			}
			this.tabixLineReader = new TabixLineReader(uriString);
		}
		catch (Exception x) {
			this.tabixLineReader = null;
			Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.SEVERE,
						"Could not initialize tabix line reader for {0}.",
						new Object[]{featureName});
			return;
		}
		
		if (!isValid()) {
			throw new IllegalStateException("tabix file does not exist or was not read");
		}
		
		lineProcessor.init(uri);
		for (String seqID : tabixLineReader.getSequenceNames()) {
			BioSeq seq = group.getSeq(seqID);
			if (seq == null) {
				int length = 1000000000;
				seq = group.addSeq(seqID, length);
				Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.INFO,
						"Sequence not found. Adding {0} with default length {1}",
						new Object[]{seqID,length});
			}
			seqs.put(seq, seqID);
		}
		this.isInitialized = true;
	}

	public LineProcessor getLineProcessor() {
		return lineProcessor;
	}

	@Override
	public List<String> getFormatPrefList() {
		return lineProcessor.getFormatPrefList();
	}


	/**
	 * @return current CompressedStreamPosition for TabixLineReader
	 * @throws Exception
	 */
	private BlockCompressedStreamPosition getCompressedInputStreamPosition(LineReader reader) throws Exception {
		Field privateReaderField = reader.getClass().getDeclaredField("reader");
		privateReaderField.setAccessible(true);
		Object mReaderValue = privateReaderField.get(reader);
		Field privateCompressedInputStreamField = mReaderValue.getClass().getDeclaredField("mFp");
		privateCompressedInputStreamField.setAccessible(true);
		Object compressedInputStreamValue = privateCompressedInputStreamField.get(mReaderValue);
		Field privateBlockAddressField = compressedInputStreamValue.getClass().getDeclaredField("mBlockAddress");
		privateBlockAddressField.setAccessible(true);
		long blockAddressValue = ((Long)privateBlockAddressField.get(compressedInputStreamValue)).longValue();
		Field privateCurrentOffsetField = compressedInputStreamValue.getClass().getDeclaredField("mCurrentOffset");
		privateCurrentOffsetField.setAccessible(true);
		int currentOffsetValue =  ((Integer)privateCurrentOffsetField.get(compressedInputStreamValue)).intValue();;
		return new BlockCompressedStreamPosition(blockAddressValue, currentOffsetValue);
	}

	private long getEndPosition(String seqID, int max) throws Exception {
		String uriString = uri.toString();
		if (uriString.startsWith(FILE_PREFIX)) {
			uriString = uri.getPath();
		}
		TabixLineReader tabixLineReaderTemp = new TabixLineReader(uriString);
		LineReader lineReaderTemp = tabixLineReaderTemp.query(seqID, max - 1, max);
		String line;
		try {
			do {
				line = lineReaderTemp.readLine();
			}
			while (line != null);
		}
		catch (IOException x) {}
		return getCompressedInputStreamPosition(lineReaderTemp).getApproximatePosition();
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
		init();
		String seqID = seqs.get(seq);
		return lineProcessor.processLines(seq, tabixLineReader.query(seqID, 1, Integer.MAX_VALUE / 2));
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception  {
		init();
		String seqID = seqs.get(overlapSpan.getBioSeq());
		final LineReader lineReader = tabixLineReader.query(seqID, (overlapSpan.getStart() + 1), + overlapSpan.getEnd());
		if (lineReader == null) {
			return new ArrayList<SeqSymmetry>();
		}
		final long endPosition = getEndPosition(seqID, overlapSpan.getMax());
		final long startPosition = getCompressedInputStreamPosition(lineReader).getApproximatePosition();
/*
		ProgressUpdater progressUpdater = new ProgressUpdater(startPosition, endPosition,
			new PositionCalculator() {
				@Override
				public long getCurrentPosition() {
					long currentPosition = startPosition;
					try {
						currentPosition = getCompressedInputStreamPosition(lineReader).getApproximatePosition();
					}
					catch (Exception x) {
						// log error here
					}
					return currentPosition;
				}
			}
		);
*/
		List<? extends SeqSymmetry> region = lineProcessor.processLines(overlapSpan.getBioSeq(), lineReader);
//		progressUpdater.kill();
		return region;
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
                is = new BlockCompressedInputStream(new URL(path + ".tbi"));
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
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
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
					return new SymLoaderTabix(sym.uri, sym.featureName, sym.group, (LineProcessor)sym);
				}
			}
		} catch (URISyntaxException ex) {
			Logger.getLogger(SymLoaderTabix.class.getName()).log(Level.SEVERE, null, ex);
		}
		return sym;
	}
}

