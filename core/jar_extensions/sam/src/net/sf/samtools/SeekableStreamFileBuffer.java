package net.sf.samtools;

import java.io.IOException;
import java.net.URI;

import net.sf.samtools.AbstractBAMFileIndex_.IndexFileBuffer;
import net.sf.samtools.seekablestream.SeekableStream;
import net.sf.samtools.util.RuntimeIOException;

public class SeekableStreamFileBuffer extends IndexFileBuffer {
    private static final int PAGE_SIZE = 4 * 1024;
    private static final int PAGE_OFFSET_MASK = PAGE_SIZE-1;
    private static final int PAGE_MASK = ~PAGE_OFFSET_MASK;
    private static final int INVALID_PAGE = 1;
    private URI mUri;
    private SeekableStream mSeekableStream;
    private int mFileLength;
    private int mFilePointer = 0;
    private int mCurrentPage = INVALID_PAGE;
    private final byte[] mBuffer = new byte[PAGE_SIZE];

    SeekableStreamFileBuffer(SeekableStream seekableStream, URI uri) {
    	mSeekableStream = seekableStream;
    	mUri = uri;
        long fileLength = seekableStream.length();
        if (fileLength > Integer.MAX_VALUE) {
            throw new RuntimeException("BAM index file " + mUri + " is too large: " + fileLength);
        }
        mFileLength = (int) fileLength;
    }

    public URI getURI() {
    	return mUri;
    }

    void readBytes(final byte[] bytes) {
        int resultOffset = 0;
        int resultLength = bytes.length;
        if (mFilePointer + resultLength > mFileLength) {
            throw new RuntimeException("Attempt to read past end of BAM index file (file is truncated?): " + mUri);
        }
        while (resultLength > 0) {
            loadPage(mFilePointer);
            final int pageOffset = mFilePointer & PAGE_OFFSET_MASK;
            final int copyLength = Math.min(resultLength, PAGE_SIZE - pageOffset);
            System.arraycopy(mBuffer, pageOffset, bytes, resultOffset, copyLength);
            mFilePointer += copyLength;
            resultOffset += copyLength;
            resultLength -= copyLength;
        }
    }

    int readInteger() {
        // This takes advantage of the fact that integers in BAM index files are always 4-byte aligned.
        loadPage(mFilePointer);
        final int pageOffset = mFilePointer & PAGE_OFFSET_MASK;
        mFilePointer += 4;
        return((mBuffer[pageOffset + 0] & 0xFF) |
               ((mBuffer[pageOffset + 1] & 0xFF) << 8) | 
               ((mBuffer[pageOffset + 2] & 0xFF) << 16) |
               ((mBuffer[pageOffset + 3] & 0xFF) << 24));
    }

    long readLong() {
        // BAM index files are always 4-byte aligned, but not necessrily 8-byte aligned.
        // So, rather than fooling with complex page logic we simply read the long in two 4-byte chunks.
        long lower = readInteger();
        long upper = readInteger();
        return ((upper << 32) | (lower & 0xFFFFFFFFL));
    }

    void skipBytes(final int count) {
        mFilePointer += count;
    }
    
    void seek(final int position) {
        mFilePointer = position;
    }

    void close() {
        mFilePointer = 0;
        mCurrentPage = INVALID_PAGE;
        if (mSeekableStream != null) {
            try {
            	mSeekableStream.close();
            } catch (IOException exc) {
                throw new RuntimeIOException(exc.getMessage(), exc);
            }
            mSeekableStream = null;
        }
    }

    private void loadPage(int filePosition) {
        final int page = filePosition & PAGE_MASK;
        if (page == mCurrentPage) {
            return;
        }
        try {
        	mSeekableStream.seek(page);
            final int readLength = Math.min(mFileLength - page, PAGE_SIZE);
            mSeekableStream.read(mBuffer, 0, readLength);
            mCurrentPage = page;
        } catch (IOException exc) {
            throw new RuntimeIOException("Exception reading BAM index file " + mUri + ": " + exc.getMessage(), exc);
        }
    }
}
