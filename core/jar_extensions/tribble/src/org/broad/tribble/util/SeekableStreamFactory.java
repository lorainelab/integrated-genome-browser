
package org.broad.tribble.util;

import java.io.IOException;

/**
 *
 * @author hiralv
 */
public class SeekableStreamFactory {
	
	public static SeekableStream getStreamFor(String uriString) throws IOException {
		return new SeekableStream(net.sf.samtools.seekablestream.SeekableStreamFactory.getInstance().getStreamFor(uriString));
	}
}
