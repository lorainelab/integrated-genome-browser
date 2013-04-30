package org.broad.tribble.util;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author hiralv
 */
public class SeekableStream extends InputStream {
	net.sf.samtools.seekablestream.SeekableStream innerStream;
	
	SeekableStream(){
		innerStream = null;
	}
	
	SeekableStream(net.sf.samtools.seekablestream.SeekableStream is){
		innerStream = is;
	}
	
    public long length(){
		return innerStream.length();
	}

    public long position() throws IOException {
		return innerStream.position();
	}

    public void seek(long position) throws IOException{
		innerStream.seek(position);
	}

    public int read(byte[] buffer, int offset, int length) throws IOException {
		return innerStream.read(buffer, offset, length);
	}

    public void close() throws IOException {
		innerStream.close();
	}

    public boolean eof() throws IOException {
		return innerStream.eof();
	}

    public String getSource() {
		return innerStream.getSource();
	}

    public void readFully(byte b[]) throws IOException {
        innerStream.readFully(b);
    }

	public int read() throws IOException {
		return innerStream.read();
	}

}
