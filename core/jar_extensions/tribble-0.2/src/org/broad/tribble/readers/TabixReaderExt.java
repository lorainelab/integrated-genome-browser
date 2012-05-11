package org.broad.tribble.readers;

import java.io.IOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.broad.tribble.readers.TabixReader;
import org.broad.tribble.util.SeekableStream;

public class TabixReaderExt extends TabixReader {
	public TabixReaderExt(final String fn) throws IOException {
		super(fn);
    }
	public TabixReaderExt(String fn, SeekableStream stream) throws IOException {
		super(fn, stream);
	}

	private List<List<Long>> getChunkList(final TPair64[] chunks) {
		return new AbstractList<List<Long>>() {
			@Override
			public List<Long> get(int index) {
				TPair64 chunk = chunks[index];
				List<Long> chunkList = new ArrayList<Long>();
				chunkList.add(chunk.u);
				chunkList.add(chunk.v);
				return chunkList;
			}
			@Override
			public int size() {
				return chunks.length;
			}
		};
	}

	private java.util.Iterator<Map<Integer, List<List<Long>>>> getBinIterator(final HashMap<Integer, TPair64[]> bins) {
		return new java.util.Iterator<Map<Integer, List<List<Long>>>>() {
			java.util.Iterator<Integer> binIter = bins.keySet().iterator();
			@Override
			public boolean hasNext() {
				return binIter.hasNext();
			}
			@Override
			public Map<Integer, List<List<Long>>> next() {
				final Integer binNo = binIter.next();
				return new AbstractMap<Integer, List<List<Long>>>() {
					@Override
					public Set<java.util.Map.Entry<Integer, List<List<Long>>>> entrySet() {
						Set<java.util.Map.Entry<Integer, List<List<Long>>>> entrySet =
							new HashSet<java.util.Map.Entry<Integer, List<List<Long>>>>();
						final TPair64[] chunks = bins.get(binNo);
						entrySet.add(new SimpleEntry<Integer, List<List<Long>>>(binNo, TabixReaderExt.this.getChunkList(chunks)));
						return entrySet;
					}
				};
			}
			@Override
			public void remove() {}
		};
	}

	private int getRefNo(Map<String, String> synonymMap, String igbSeq) {
		for (String chr : mChr2tid.keySet()) {
			String bamSeq = synonymMap.get(chr);
			if (igbSeq.equals(bamSeq)) {
				return mChr2tid.get(chr);
			}
		}
		return -1;
	}

	public java.util.Iterator<Map<Integer, List<List<Long>>>> getBinIter(Map<String, String> synonymMap, String seq) {
		int refno = getRefNo(synonymMap, seq.toString());
		if (refno == -1) {
			return null;
		}
		else {
			final HashMap<Integer, TPair64[]> bins = mIndex[refno].b;
			return getBinIterator(bins);
		}
	}
}

