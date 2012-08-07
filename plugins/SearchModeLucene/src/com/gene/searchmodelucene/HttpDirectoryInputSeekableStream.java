package com.gene.searchmodelucene;

import java.io.IOException;
import java.net.URL;

import org.apache.lucene.store.BufferedIndexInput;
import org.broad.tribble.util.SeekableHTTPStream;
import org.broad.tribble.util.SeekableStream;

public class HttpDirectoryInputSeekableStream extends BufferedIndexInput {
	private SeekableStream stream;
	private final String fileUrl;

    public HttpDirectoryInputSeekableStream(String httpURL, String filename) throws IOException {
		super(filename);
		String url = httpURL;
		if (!url.endsWith("/")) {
			url += "/";
		}
		url += filename;
		fileUrl = url;
		this.stream = new SeekableHTTPStream(new URL(fileUrl));
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public long length() {
		long length = stream.length();
		return length;
	}

	@Override
	protected void readInternal(byte[] b, int offset, int length)
			throws IOException {
		seekInternal(getFilePointer());
		stream.read(b, offset, length);
	}

	@Override
	protected void seekInternal(long pos) throws IOException {
		stream.seek(pos);
	}
}
