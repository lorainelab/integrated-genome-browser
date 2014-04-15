package com.affymetrix.igb.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReplaceInputStream extends InputStream {
	private final InputStream is;
	public ReplaceInputStream(InputStream in,
			String from, String to) throws IOException {
		if (from == null || to == null) {
			is = in;
		}
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		    StringBuilder sb = new StringBuilder();
		    String line = null;
		    while ((line = reader.readLine()) != null) {
		      	sb.append(line).append("\n");
		    }
		    in.close();
		    String orig = sb.toString();
		    String result = orig.replaceAll(from, to);
		    byte[] bytes = result.getBytes("UTF-8");
		    is = new ByteArrayInputStream(bytes);
		}
	}

	@Override
	public int read() throws IOException {
		return is.read();
	}
	@Override
	public void close() throws IOException {
		is.close();
	}
	@Override
	public int available() throws IOException {
		return is.available();
	}
	@Override
	public void mark(int readlimit) {
		is.mark(readlimit);
	}
	@Override
	public boolean markSupported() {
		return is.markSupported();
	}
	@Override
	public int read(byte[] b) throws IOException {
		return is.read(b);
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return is.read(b, off, len);
	}
	@Override
	public void reset() throws IOException {
		is.reset();
	}
	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
}
