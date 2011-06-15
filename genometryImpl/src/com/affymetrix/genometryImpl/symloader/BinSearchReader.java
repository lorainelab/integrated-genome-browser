package com.affymetrix.genometryImpl.symloader;

import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableHTTPStream;
//import net.sf.samtools.util.SeekableBufferedStream;
import com.affymetrix.genometryImpl.util.SeekableBufferedStream;
// note - samtools SeekableBufferedStream version does not support SeekableBufferedStream.position()

import org.broad.tribble.readers.QueryReader;
import org.broad.tribble.util.LineReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the QueryReader that uses binary search as
 * a fast way to find regions in a genome data file. The data
 * file must be in the form of one item per line (seq, start position,
 * end position) and be sorted by sequence, then position.
 * This can be used for large unindexed data files.
 */
public class BinSearchReader implements QueryReader, LineReader {
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
    private Map<String, FileDataRange> sequenceRanges; // all the chromosomes and their file data ranges, this is the "index"

    /**
     * The constructor
     *
     * @param fn File name/ URL of the data file
     * @param sequenceSpanReader
     * @throws IOException
     */
    public BinSearchReader(final String fn, final SequenceSpanReader sequenceSpanReader) throws IOException {
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
        	currentOffset--;
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
    		int centerLineLocation = (int)(fileOffset - startFileOffset);
    		// find the previous CR/LF for the beginning of the line
    		while (startFileOffset > 0 && s.lastIndexOf('\r', centerLineLocation) == -1 && s.lastIndexOf('\n', centerLineLocation) == -1) {
    			long saveStartFileOffset = startFileOffset;
    			startFileOffset = Math.max(saveStartFileOffset - BUFF_SIZE, 0);
        		seekableBufferedStream.seek(startFileOffset);
        		count = seekableBufferedStream.read(byteArray, 0, (int)(saveStartFileOffset - startFileOffset));
        		subArray = Arrays.copyOfRange(byteArray, 0, count);
        		s = new String(subArray) + s;
        		centerLineLocation = (int)(fileOffset - startFileOffset);
    		}
    		int beginLineLocation;
    		int prevLineLocation = Math.max(s.lastIndexOf('\n', centerLineLocation), s.lastIndexOf('\r', centerLineLocation));
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
    		return new FileDataRange(fileOffset + (beginLineLocation - centerLineLocation), fileOffset + (endLineLocation - centerLineLocation), line, sequenceDataSpan);
     	}
    	catch (Exception x) {
    		x.printStackTrace(System.out);
    		return null;
    	}
    }

    /**
     * add a FileDataRange to the sequenceRange (sequence index)
     * @param fileDataRange the sequence and the start file offset
     * or end file offset or both
     */
    private void addSeqRange(FileDataRange fileDataRange) {
    	String seq = fileDataRange.getSequenceDataSpan().getSeq();
		FileDataRange oldFileDataRange = sequenceRanges.get(seq);
		if (oldFileDataRange == null) {
			sequenceRanges.put(seq, fileDataRange);
		}
		else {
			if (oldFileDataRange.getStartOffset() == -1 && fileDataRange.getStartOffset() == -1) {
				throw new RuntimeException("error at addSeqRange, old start and new start are both -1");
			}
			if (oldFileDataRange.getStartOffset() != -1 && fileDataRange.getStartOffset() != -1) {
				throw new RuntimeException("error at addSeqRange, old start and new start are both not -1");
			}
			long startOffset = (fileDataRange.getStartOffset() == -1) ? oldFileDataRange.getStartOffset() : fileDataRange.getStartOffset();
			if (oldFileDataRange.getEndOffset() == -1 && fileDataRange.getEndOffset() == -1) {
				throw new RuntimeException("error at addSeqRange, old end and new end are both -1");
			}
			if (oldFileDataRange.getEndOffset() != -1 && fileDataRange.getEndOffset() != -1) {
				throw new RuntimeException("error at addSeqRange, old end and new end are both not -1");
			}
			long endOffset = (fileDataRange.getEndOffset() == -1) ? oldFileDataRange.getEndOffset() : fileDataRange.getEndOffset();
			SequenceDataSpan oldSequenceDataSpan = oldFileDataRange.getSequenceDataSpan();
			SequenceDataSpan sequenceDataSpan = fileDataRange.getSequenceDataSpan();
			if (oldSequenceDataSpan.getStartPos() == -1 && sequenceDataSpan.getStartPos() == -1) {
				throw new RuntimeException("error at addSeqRange, old startPos and new startPos are both -1");
			}
			if (oldSequenceDataSpan.getStartPos() != -1 && sequenceDataSpan.getStartPos() != -1) {
				throw new RuntimeException("error at addSeqRange, old startPos and new startPos are both not -1");
			}
			long startPos = (sequenceDataSpan.getStartPos() == -1) ? oldSequenceDataSpan.getStartPos() : sequenceDataSpan.getStartPos();
			if (oldSequenceDataSpan.getEndPos() == -1 && sequenceDataSpan.getEndPos() == -1) {
				throw new RuntimeException("error at addSeqRange, old endPos and new endPos are both -1");
			}
			if (oldSequenceDataSpan.getEndPos() != -1 && sequenceDataSpan.getEndPos() != -1) {
				throw new RuntimeException("error at addSeqRange, old endPos and new endPos are both not -1");
			}
			long endPos = (sequenceDataSpan.getEndPos() == -1) ? oldSequenceDataSpan.getEndPos() : sequenceDataSpan.getEndPos();
			FileDataRange newFileDataRange = 
				new FileDataRange(startOffset, endOffset, null, new SequenceDataSpan(seq, startPos, endPos));
			sequenceRanges.put(seq, newFileDataRange);
		}
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
    		addSeqRange(new FileDataRange(-1, lowRange.getEndOffset(), null, new SequenceDataSpan(lowSeq, -1, lowRange.getSequenceDataSpan().getEndPos())));
    		addSeqRange(new FileDataRange(highRange.getStartOffset(), -1, null, new SequenceDataSpan(highSeq, highRange.getSequenceDataSpan().getStartPos(), -1)));
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
    	sequenceRanges = new HashMap<String, FileDataRange>();
    	FileDataRange lowRange = readDataLineAt(0);
    	String lowSeq = lowRange.getSequenceDataSpan().getSeq();
    	SequenceDataSpan lowSequenceSpan = new SequenceDataSpan(lowSeq, lowRange.getSequenceDataSpan().getStartPos(), -1);
    	FileDataRange lowSequenceRange = new FileDataRange(lowRange.getStartOffset(), -1, null, lowSequenceSpan);
    	addSeqRange(lowSequenceRange);
    	FileDataRange highRange = readDataLineAt(seekableBufferedStream.length() - 1);
    	String highSeq = highRange.getSequenceDataSpan().getSeq();
    	SequenceDataSpan highSequenceSpan = new SequenceDataSpan(highSeq, -1, highRange.getSequenceDataSpan().getEndPos());
    	FileDataRange highSequenceRange = new FileDataRange(-1, highRange.getStartOffset(), null, highSequenceSpan);
    	addSeqRange(highSequenceRange);
    	if (!lowSeq.equals(highSeq)) {
    		loadSequenceRanges(lowRange, highRange);
    	}
    }

    @Override
    public Set<String> getSequenceNames() {
    	return new HashSet<String>(sequenceRanges.keySet());
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
        String chr = colon >= 0 ? reg.substring(0, colon) : reg;
        long startPos = colon >= 0 ? Long.parseLong(reg.substring(colon + 1, hyphen)) - 1 : 0;
        long endPos = hyphen >= 0 ? Long.parseLong(reg.substring(hyphen + 1)) : 0x7fffffff;
        return new SequenceDataSpan(chr, startPos, endPos);
    }

    @Override
    public LineReader query(final String seq, int startPos, int endPos) {
    	FileDataRange seqFileDataRange = sequenceRanges.get(seq);
    	long startFileRange = seqFileDataRange.getStartOffset();
    	long startSeqPos = seqFileDataRange.getSequenceDataSpan().getStartPos();
    	FileDataRange lowRange = new FileDataRange(startFileRange, startFileRange, null, new SequenceDataSpan(seq, startSeqPos, startSeqPos));
    	long endFileRange = seqFileDataRange.getEndOffset();
    	long endSeqPos = seqFileDataRange.getSequenceDataSpan().getEndPos();
    	FileDataRange highRange = new FileDataRange(endFileRange, endFileRange, null, new SequenceDataSpan(seq, endSeqPos, endSeqPos));
    	final FileDataRange fileDataRangeLow = getDataRange(seq, startPos, lowRange, highRange, true);
    	final FileDataRange fileDataRangeHigh = getDataRange(seq, endPos, lowRange, highRange, false);
    	try {
    		seekableBufferedStream.seek(fileDataRangeLow.getStartOffset());
    	}
    	catch (IOException x) {
    		System.out.println("Error in query(seq, startPos, endPos)");
    		x.printStackTrace(System.out);
    		return null;
    	}
        return new LineReader() {
			@Override
			public String readLine() throws IOException {
				if (seekableBufferedStream.position() >= fileDataRangeHigh.getEndOffset()) {
					return null;
				}
				return readNextLine(seekableBufferedStream);
			}
			@Override
			public void close() throws IOException {
			}
        };
    }

    public LineReader query(final String reg) {
    	SequenceDataSpan sequenceDataSpan;
    	if (reg.indexOf(':') > 0) {
    		sequenceDataSpan = parseReg(reg);
    	}
    	else {
    		sequenceDataSpan = sequenceRanges.get(reg).getSequenceDataSpan();
    	}
		return query(sequenceDataSpan.getSeq(), (int)sequenceDataSpan.getStartPos(), (int)sequenceDataSpan.getEndPos());
    }

    @Override
    public void close() throws IOException {
    	seekableBufferedStream.close();
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
            BinSearchReader tr = new BinSearchReader(args[0],
            	new SequenceSpanReader() {
					@Override
					public SequenceDataSpan readSequenceSpan(String line) {
						if (line.startsWith("#")) {
							return null;
						}
						String[] parts = line.split("\t");
						long start = Long.parseLong(parts[1]);
						int length = 0;
						if (!".".equals(parts[3])) {
							length = parts[3].length();
						}
						return new SequenceDataSpan(parts[0], start, start + length);
					}
             	}
            );


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
}
