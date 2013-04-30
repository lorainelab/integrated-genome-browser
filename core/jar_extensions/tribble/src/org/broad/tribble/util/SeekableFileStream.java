package org.broad.tribble.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class SeekableFileStream extends SeekableStream {

    net.sf.samtools.seekablestream.SeekableFileStream innerStream;

    public SeekableFileStream(File file) throws FileNotFoundException {
		innerStream = new net.sf.samtools.seekablestream.SeekableFileStream(file);
    }


    public boolean eof() throws IOException {
        return innerStream.eof();
    }

    public long length() {
        return innerStream.length();
    }


    public void seek(long position) throws IOException {
       innerStream.seek(position);
    }

    public long position() throws IOException {
        return innerStream.position();
    }

    @Override
    public long skip(long n) throws IOException {
        return innerStream.skip(n);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return innerStream.read(buffer, offset, length);
    }

    @Override
    public int read() throws IOException {
        return innerStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return innerStream.read(b);    
    }

    @Override
    public int available() throws IOException {
        return innerStream.available();
    }

    @Override
    public void mark(int readlimit) {
        innerStream.mark(readlimit); 
    }

    @Override
    public boolean markSupported() {
        return innerStream.markSupported();
    }

    @Override
    public void reset() throws IOException {
        innerStream.reset();
    }

    @Override
    public void close() throws IOException {
        innerStream.close();
    }

}
