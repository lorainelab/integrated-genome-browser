package com.affymetrix.genometryImpl.symloader;

import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
//import org.broad.tribble.util.SeekableBufferedStream;
import com.affymetrix.genometryImpl.util.SeekableBufferedStream;

import org.broad.tribble.TribbleException;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.source.query.QuerySource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// !!! NOTE !!! - this class only supports one query at a time
/**
 * Implementation of the QueryReader that uses binary search as
 * a fast way to find regions in a genome data file. The data
 * file must be in the form of one item per line (seq, start position,
 * end position) and be sorted by sequence, then position.
 * This can be used for large unindexed data files.
 */
public class BinSearchReader implements LineReader, QuerySource {
	private static final int BUFF_SIZE = 256;
	/**
	 * supplied interface to convert a data line into a SequenceDataSpan
	 * this will be unique to each file type (VCF, SAM, PSL, ...)
	 */
	public static interface SequenceSpanReader {
		/**
		 * @param line the data line
		 * @return the SequenceDataSpan from the data line
		 * can be null if the line is not data, e.g. comment or blank line
		 */
		public SequenceDataSpan readSequenceSpan(String line);
	}

	/**
	 * data structure to hold a data span / region in a genome
	 * seq is the chromosome/sequence, also start and
	 * end positions in the sequence
	 */
	public static class SequenceDataSpan {
		private final String seq;
		private final long seqStartPos;
		private final long seqEndPos;
		public SequenceDataSpan(String seq, long seqStartPos, long seqEndPos) {
			super();
			this.seq = seq;
			this.seqStartPos = seqStartPos;
			this.seqEndPos = seqEndPos;
		}
		public String getSeq() {
			return seq;
		}
		public long getStartPos() {
			return seqStartPos;
		}
		public long getEndPos() {
			return seqEndPos;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((seq == null) ? 0 : seq.hashCode());
			result = prime * result + (int) (seqEndPos ^ (seqEndPos >>> 32));
			result = prime * result
					+ (int) (seqStartPos ^ (seqStartPos >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SequenceDataSpan other = (SequenceDataSpan) obj;
			if (seq == null) {
				if (other.seq != null)
					return false;
			} else if (!seq.equals(other.seq))
				return false;
			if (seqEndPos != other.seqEndPos)
				return false;
			if (seqStartPos != other.seqStartPos)
				return false;
			return true;
		}
		@Override
		public String toString() {
	    	return getSeq() + ":" + getStartPos() + "-" + getEndPos();
		}
	}

	/**
	 * data structure to hold a file range in a data file
	 * start and end offsets in the file, the data line
	 * between the offsets (separated by cr/fl) and the
	 * SequenceDataSpan from the data line
	 */
	public static class FileDataRange {
		private final long startOffset;
		private final long endOffset;
		private final String line;
		private final SequenceDataSpan sequenceDataSpan;
		private FileDataRange(long startOffset, long endOffset, String line, SequenceDataSpan sequenceDataSpan) {
			super();
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.line = line;
			this.sequenceDataSpan = sequenceDataSpan;
		}
		public long getStartOffset() {
			return startOffset;
		}
		public long getEndOffset() {
			return endOffset;
		}
		public String getLine() {
			return line;
		}
		public SequenceDataSpan getSequenceDataSpan() {
			return sequenceDataSpan;
		}
		public boolean isNoData() {
			return (getLine().length() == 0 || getSequenceDataSpan() == null);
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (endOffset ^ (endOffset >>> 32));
			result = prime * result + ((line == null) ? 0 : line.hashCode());
			result = prime
					* result
					+ ((sequenceDataSpan == null) ? 0 : sequenceDataSpan
							.hashCode());
			result = prime * result + (int) (startOffset ^ (startOffset >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileDataRange other = (FileDataRange) obj;
			if (endOffset != other.endOffset)
				return false;
			if (line == null) {
				if (other.line != null)
					return false;
			} else if (!line.equals(other.line))
				return false;
			if (sequenceDataSpan == null) {
				if (other.sequenceDataSpan != null)
					return false;
			} else if (!sequenceDataSpan.equals(other.sequenceDataSpan))
				return false;
			if (startOffset != other.startOffset)
				return false;
			return true;
		}
		@Override
		public String toString() {
	    	return "" + getStartOffset() + "-" + getEndOffset() + "=\"" + getLine() + "\"" + "->" + getSequenceDataSpan();
		}
	}

    private SequenceSpanReader sequenceSpanReader; // supplied reader to get the SequenceDataSpan from a data line
    private SeekableBufferedStream seekableBufferedStream; // input stream for data file
    private Map<String, List<FileDataRange>> sequenceRanges; // all the chromosomes and their file data ranges, this is the "index"
    														 // key is seq/chromosome, value is List with two values, low FileDataRaneg and high FileDataRange
    private String source;
    
    /**
     * The constructor
     *
     * @param fn File name/ URL of the data file
     * @param sequenceSpanReader
     * @throws IOException
     */
    public BinSearchReader(final String fn, final SequenceSpanReader sequenceSpanReader) throws IOException {
    	this.source = fn;
        this.sequenceSpanReader = sequenceSpanReader;
        if (fn.startsWith("http:") || fn.startsWith("https:")) {
        	seekableBufferedStream = new SeekableBufferedStream(new SeekableHTTPStream(new URL(fn)));
        } else {
        	seekableBufferedStream = new SeekableBufferedStream(new SeekableFileStream(new File(fn)));
        }
        createSequenceRanges();
    }

    /**
     * read a FileDataRange from the data file at a specified
     * file position, handle non data lines by going backwards
     * or forwards until it gets a data line
     * @param fileOffset offset within file
     * @return FileDataRange at the file offset
     */
    private FileDataRange readDataLineAt(long fileOffset) {
    	long currentOffset = fileOffset;
    	FileDataRange dataSpan = readLineAt(currentOffset);
    	while (dataSpan.isNoData() && currentOffset > 0) {
        	currentOffset = dataSpan.getStartOffset() - 1;
        	dataSpan = readLineAt(currentOffset);
    	}
    	if (currentOffset == 0 && dataSpan.isNoData()) {
    		currentOffset = dataSpan.getEndOffset() + 1;
        	while (dataSpan.isNoData() && currentOffset < seekableBufferedStream.length()) {
            	currentOffset++;
            	dataSpan = readLineAt(currentOffset);
        	}
    	}
    	return dataSpan;
    }

    /**
     * read a FileDataRange from the data file at a specified
     * file position, may return non data lines (comments or blank line)
     * @param fileOffset offset within file
     * @return FileDataRange at the file offset
     */
    private FileDataRange readLineAt(long fileOffset) {
    	byte[] byteArray = new byte[BUFF_SIZE];
    	try {
    		boolean EOF = false;
    		long startFileOffset = Math.max(fileOffset - BUFF_SIZE / 2, 0);
    		seekableBufferedStream.seek(startFileOffset);
    		int count = seekableBufferedStream.read(byteArray);
    		EOF = count < BUFF_SIZE;
    		byte [] subArray = Arrays.copyOfRange(byteArray, 0, count);
    		String s = new String(subArray);
    		int centerLineLocation = count / 2;//(int)(fileOffset - startFileOffset);
    		// find the previous CR/LF for the beginning of the line
    		while (startFileOffset > 0 && s.lastIndexOf('\r', centerLineLocation - 1) == -1 && s.lastIndexOf('\n', centerLineLocation - 1) == -1) {
    			long saveStartFileOffset = startFileOffset;
    			startFileOffset = Math.max(saveStartFileOffset - BUFF_SIZE, 0);
        		seekableBufferedStream.seek(startFileOffset);
        		count = seekableBufferedStream.read(byteArray, 0, (int)(saveStartFileOffset - startFileOffset));
        		subArray = Arrays.copyOfRange(byteArray, 0, count);
        		s = new String(subArray) + s;
        		centerLineLocation = (int)(fileOffset - startFileOffset);
    		}
    		int beginLineLocation;
    		int prevLineLocation = Math.max(s.lastIndexOf('\n', centerLineLocation - 1), s.lastIndexOf('\r', centerLineLocation - 1));
    		if (prevLineLocation > -1) {
    			beginLineLocation = prevLineLocation + 1;
    		}
    		else if (startFileOffset == 0) {
    			beginLineLocation = 0;
    		}
    		else {
    			throw new Exception("should not happen");
    		}
    		// find the next CR/LF for the end of the line
    		while (!EOF && s.indexOf('\r', centerLineLocation) == -1 && s.indexOf('\n', centerLineLocation) == -1) {
    			long endFileOffset = startFileOffset + s.length();
        		seekableBufferedStream.seek(endFileOffset);
        		count = seekableBufferedStream.read(byteArray);
        		EOF = count < BUFF_SIZE;
        		if (count > 0) {
        			subArray = Arrays.copyOfRange(byteArray, 0, count);
        			s = s + new String(subArray);
        		}
    		}
    		int endLineLocation;
    		int nextLineLocation = s.indexOf('\n', beginLineLocation);
    		int crLocation = s.indexOf('\r', beginLineLocation);
    		if (nextLineLocation == -1 || (crLocation != -1 && crLocation < nextLineLocation)) {
    			nextLineLocation = crLocation;
    		}

    		if (nextLineLocation > -1) {
    			endLineLocation = nextLineLocation;
    		}
    		else {
    			endLineLocation = s.length();
    		}
    		String line = s.substring(beginLineLocation, endLineLocation);
    		SequenceDataSpan sequenceDataSpan = sequenceSpanReader.readSequenceSpan(line);
    		return new FileDataRange(startFileOffset + beginLineLocation, startFileOffset + endLineLocation, line, sequenceDataSpan);
     	}
    	catch (Exception x) {
    		x.printStackTrace(System.out);
    		return null;
    	}
    }

    /**
     * add a FileDataRange to the sequenceRange (sequence index)
     * @param fileDataRange the sequence and the file offset
     * @param high true if this is the high end of the sequence, false if the low end
     */
    private void addSeqIndex(FileDataRange fileDataRange, boolean high) {
    	String seq = fileDataRange.getSequenceDataSpan().getSeq();
		List<FileDataRange> fileDataRanges = sequenceRanges.get(seq);
		if (fileDataRanges == null) {
			fileDataRanges = new ArrayList<FileDataRange>(2);
			fileDataRanges.add(null);
			fileDataRanges.add(null);
			sequenceRanges.put(seq, fileDataRanges);
		}
		else if (!high && fileDataRanges.get(0) != null) {
			throw new RuntimeException("error at addSeqIndex, duplicate low range for seq=" + seq);
		}
		else if (high && fileDataRanges.get(1) != null) {
			throw new RuntimeException("error at addSeqIndex, duplicate high range for seq=" + seq);
		}
		fileDataRanges.set(high ? 1 : 0, fileDataRange);
    }

    /**
     * Used to create the sequenceRanges
     * Recursive method to find the start and end file offsets for
     * each sequence, so that the sequenceRanges (sequence index) can
     * be filled out. This is called recursively by taking a low and high
     * file data range, and aplitting the difference (binary search) when
     * the two sequences are not equal. Finally, it will get to ranges
     * that are consecutive in the file, so that the low range is the end
     * of one sequence, and the high range is the start of the next sequence
     * @param lowRange the lower range
     * @param highRange the higher range
     */
    private void loadSequenceRanges(FileDataRange lowRange, FileDataRange highRange) {
    	long fileOffset = lowRange.getEndOffset() + (highRange.getStartOffset() - lowRange.getEndOffset()) / 2;
    	FileDataRange middleRange = readDataLineAt(fileOffset);
    	if (lowRange.equals(middleRange) || middleRange.equals(highRange)) {
    		String lowSeq = lowRange.getSequenceDataSpan().getSeq();
    		String highSeq = highRange.getSequenceDataSpan().getSeq();
    		if (lowSeq.equals(highSeq)) {
    			throw new RuntimeException("internal error"); // should never happen
    		}
    		addSeqIndex(lowRange, true);
    		addSeqIndex(highRange, false);
      	}
    	else {
    		if (!lowRange.getSequenceDataSpan().getSeq().equals(middleRange.getSequenceDataSpan().getSeq())) {
    			loadSequenceRanges(lowRange, middleRange);
    		}
    		if (!middleRange.getSequenceDataSpan().getSeq().equals(highRange.getSequenceDataSpan().getSeq())) {
    			loadSequenceRanges(middleRange, highRange);
    		}
    	}
    }

    /**
     * create the sequenceRanges (sequence index)
     * called at the beginning, only once per file
     */
    private void createSequenceRanges() {
    	sequenceRanges = new HashMap<String, List<FileDataRange>>();
    	FileDataRange lowRange = readDataLineAt(0);
    	String lowSeq = lowRange.getSequenceDataSpan().getSeq();
    	addSeqIndex(lowRange, false);
    	FileDataRange highRange = readDataLineAt(seekableBufferedStream.length() - 1);
    	String highSeq = highRange.getSequenceDataSpan().getSeq();
    	addSeqIndex(highRange, true);
    	if (!lowSeq.equals(highSeq)) {
    		loadSequenceRanges(lowRange, highRange);
    	}
    }

    @Override
    public List<String> getSequenceNames() {
    	return new ArrayList<String>(sequenceRanges.keySet());
    }

    /**
     * read the next line of the input stream
     * @param is input stream
     * @return the next line
     * @throws IOException
     */
    private String readNextLine(final InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();
        int c;
        while ((c = is.read()) >= 0 && c != '\n')
            buf.append((char) c);
        if (c < 0) return null;
        return buf.toString();
    }


    @Override
    public String readLine() throws IOException {
        return readNextLine(seekableBufferedStream);
    }

    @Override
    public LineReader iterate() {
        return this;
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Used to get a LineReader to read a sequence span/region, called
     * for both the start and end of the requested span.
     * Recursive method to find the start and end file offsets for
     * a requested sequence span/region. This is called recursively by taking
     * a low and high file data range, and splitting the difference (binary search)
     * until finally, it either finds the sequence position or the ranges before
     * and after the sequence position.
     * @param lowRange the lower range
     * @param highRange the higher range
     */
    private FileDataRange getDataRange(String chr, long seqPos, FileDataRange lowRange, FileDataRange highRange, boolean takeHigher) {
    	SequenceDataSpan lowSequenceDataSpan = lowRange.getSequenceDataSpan();
    	SequenceDataSpan highSequenceDataSpan = highRange.getSequenceDataSpan();
    	if (seqPos >= lowSequenceDataSpan.getStartPos() && seqPos <= lowSequenceDataSpan.getEndPos()) {
    		return lowRange;
    	}
    	if (seqPos >= highSequenceDataSpan.getStartPos() && seqPos <= highSequenceDataSpan.getEndPos()) {
    		return highRange;
    	}
    	long middleFileOffset = lowRange.getEndOffset() + (highRange.getStartOffset() - lowRange.getEndOffset()) / 2;
    	FileDataRange middleRange = readDataLineAt(middleFileOffset);
    	if (seqPos >= middleRange.getSequenceDataSpan().getStartPos() && seqPos <= middleRange.getSequenceDataSpan().getEndPos()) {
    		return middleRange;
    	}
    	if (middleRange.equals(lowRange) || middleRange.equals(highRange)) {
    		return takeHigher ? highRange : lowRange;
    	}
    	if (seqPos < middleRange.getSequenceDataSpan().getStartPos()) {
    		return getDataRange(chr, seqPos, lowRange, middleRange, takeHigher);
    	}
    	else {
       		return getDataRange(chr, seqPos, middleRange, highRange, takeHigher);
    	}
    }

    /**
     * parse a region into a SequenceDataSpan
     * @param reg the region in the form seq:nnn-nnn
     * @return the SequenceDataSpan
     */
    private SequenceDataSpan parseReg(final String reg) { // TODO FIXME: NOT working when the sequence name contains : or -.
        int colon, hyphen;
        colon = reg.lastIndexOf(':');
        hyphen = reg.lastIndexOf('-');
        String seq = colon >= 0 ? reg.substring(0, colon) : reg;
        long startPos = colon >= 0 ? Long.parseLong(reg.substring(colon + 1, hyphen)): 0;
        long endPos = hyphen >= 0 ? Long.parseLong(reg.substring(hyphen + 1)) : 0x7fffffff;
        return new SequenceDataSpan(seq, startPos, endPos);
    }

    public class BinSearchLineReader implements LineReader {
    	private final long endOffset;
		private BinSearchLineReader(long startOffset, long endOffset) {
			super();
			long useEndOffset = endOffset;
			try  {
				seekableBufferedStream.seek(startOffset);
			}
			catch (IOException x) {
	    		System.out.println("Error in BinSearchLineReader() constructor");
	    		x.printStackTrace(System.out);
	    		useEndOffset = -1;
			}
 			this.endOffset = useEndOffset;
		}
		@Override
		public String readLine() throws IOException {
			if (seekableBufferedStream.position() >= endOffset) {
				return null;
			}
			return readNextLine(seekableBufferedStream);
		}
		@Override
		public void close() {
	        try {
				seekableBufferedStream.close();
	        } catch (IOException e) {
	            System.out.println("Unable to close file source " + source);
	        }
		}
    }

    @Override
    public LineReader query(final String seq, int startPos, int endPos) {
    	List<FileDataRange> seqFileDataRanges = sequenceRanges.get(seq);
    	FileDataRange fileDataRangeHigh;
    	if (endPos >= seqFileDataRanges.get(1).getSequenceDataSpan().getStartPos()) {
    		fileDataRangeHigh = seqFileDataRanges.get(1);
    	}
    	else {
    		fileDataRangeHigh = getDataRange(seq, endPos, seqFileDataRanges.get(0), seqFileDataRanges.get(1), false);
    	}
    	try {
    		seekableBufferedStream.seek(seqFileDataRanges.get(0).getStartOffset());
    		// must read through file start at seq start until we get a span that extends past the requested startPos
    		long endOfSeq = fileDataRangeHigh.getEndOffset();
    		long saveStartPosition;
    		SequenceDataSpan sds;
    		do {
        		saveStartPosition = seekableBufferedStream.position();
    			String line = readNextLine(seekableBufferedStream);
    			sds = sequenceSpanReader.readSequenceSpan(line);
    		}
    		while (seekableBufferedStream.position() < endOfSeq && (sds == null || sds.getEndPos() <= startPos));
    		if (seekableBufferedStream.position() >= endOfSeq) { // no data
    			return null;
    		}
            return new BinSearchLineReader(saveStartPosition, fileDataRangeHigh.getEndOffset());
    	}
    	catch (IOException x) {
    		System.out.println("Error in query(seq, startPos, endPos)");
    		x.printStackTrace(System.out);
    		return null;
    	}
    }

    public LineReader query(final String reg) {
    	SequenceDataSpan sequenceDataSpan;
    	if (reg.indexOf(':') > 0) {
    		sequenceDataSpan = parseReg(reg);
    	}
    	else {
        	List<FileDataRange> seqFileDataRanges = sequenceRanges.get(reg);
        	if (seqFileDataRanges == null) {
        		System.out.println("Error in query(reg)");
    			return null;
        	}
        	sequenceDataSpan = new SequenceDataSpan(reg, seqFileDataRanges.get(0).getSequenceDataSpan().getStartPos(), seqFileDataRanges.get(1).getSequenceDataSpan().getEndPos());
    	}
		return query(sequenceDataSpan.getSeq(), (int)sequenceDataSpan.getStartPos(), (int)sequenceDataSpan.getEndPos());
    }

    @Override
    public void close() {
        try {
			seekableBufferedStream.close();
        } catch (IOException e) {
            System.out.println("Unable to close file source " + source);
        }
    }

    /**
     * main method - used only for test
     * @param args test args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -cp .:sam.jar BinSearchReader <infile> [region]");
            System.exit(1);
        }
        try {
            BinSearchReader tr = null;
            if (args[0].endsWith(".vcf")) {
	            tr = new BinSearchReader(args[0],
	            	new SequenceSpanReader() {
						@Override
						public SequenceDataSpan readSequenceSpan(String line) {
							if (line.trim().length() == 0 || line.startsWith("#")) {
								return null;
							}
							String[] parts = line.split("\t");
							String seq = parts[0];
							long start = Long.parseLong(parts[1]);
							int length = 0;
							if (!".".equals(parts[3])) {
								length = parts[3].length();
							}
							long end = start + length;
							return new SequenceDataSpan(seq, start, end);
						}
	             	}
	            );
            }
            if (args[0].endsWith(".sam")) {
	            tr = new BinSearchReader(args[0],
	            	new SequenceSpanReader() {
						@Override
						public SequenceDataSpan readSequenceSpan(String line) {
							if (line.trim().length() == 0 || line.startsWith("@")) {
								return null;
							}
							String[] parts = line.split("\t");
							String seq = parts[2];
							if (seq.equals("*")) {
								return null;
							}
							long start = Long.parseLong(parts[3]);
							int length = 0;
							if (!parts[8].equals("*")) {
								length = Integer.parseInt(parts[8]);
							}
							if (length == 0) {
								length = 1;
							}
							if (length < 0) {
								return null;
							}
							long end = start + length;
							return new SequenceDataSpan(seq, start, end);
						}
	             	}
	            );
            }

            String s;
            if (args.length == 1) { // no region is specified; print the whole file
                while ((s = tr.readLine()) != null)
                    System.out.println(s);
            } else { // a region is specified; random access
            	LineReader iter = tr.query(args[1]); // get the iterator
                while ((s = iter.readLine()) != null)
                    System.out.println(s);
            }
        } catch (IOException e) {
        }
    }

	@Override
	public void mark() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean markSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() throws IOException {
		// TODO Auto-generated method stub
		
	}
}
