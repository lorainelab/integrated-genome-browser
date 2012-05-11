package net.sf.samtools;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.util.SeekableStream;

public class StubBAMFileIndex extends AbstractBAMFileIndex_ {
	public StubBAMFileIndex(final SeekableStream ss, URI uri, SAMSequenceDictionary dictionary) {
		super(dictionary, new SeekableStreamFileBuffer(ss, uri));
    }

	@Override
	public BAMFileSpan getSpanOverlapping(int referenceIndex, int startPos, int endPos) {
		return null; // not implemented
	}

	@Override
	protected BAMIndexContent getQueryResults(int reference) {
		return null; // not implemented
	}

	public BAMIndexContent query(final int referenceSequence) {
		return query(referenceSequence, 1, BIN_GENOMIC_SPAN-1);
	}

	public Iterator<Map<Integer, List<List<Long>>>> getBinIter(int refno) {
		BAMIndexContent bic = query(refno);
		return getBinIterator(bic.getBins().iterator());
	}

	public List<List<Long>> getChunkList(final Bin bin) {
		final List<Chunk> chunks = bin.getChunkList();
		return new AbstractList<List<Long>>() {
			@Override
			public List<Long> get(int index) {
				Chunk chunk = chunks.get(index);
				List<Long> chunkList = new ArrayList<Long>();
				try {
					Field mChunkStart = Chunk.class.getDeclaredField("mChunkStart");
					mChunkStart.setAccessible(true);
					chunkList.add(mChunkStart.getLong(chunk));
					Field mChunkEnd = Chunk.class.getDeclaredField("mChunkEnd");
					mChunkEnd.setAccessible(true);
					chunkList.add(mChunkEnd.getLong(chunk));
				}
				catch (Exception x) {
					Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "failed to get chunk values", x);
				}
				return chunkList;
			}
			@Override
			public int size() {
				return chunks.size();
			}
		};
	}

	private Iterator<Map<Integer, List<List<Long>>>> getBinIterator(final Iterator<Bin> blIter) {
		return new Iterator<Map<Integer, List<List<Long>>>>() {
			@Override
			public boolean hasNext() {
				return blIter.hasNext();
			}
			@Override
			public Map<Integer, List<List<Long>>> next() {
				final Bin bin = blIter.next();
				return new AbstractMap<Integer, List<List<Long>>>() {
					@Override
					public Set<java.util.Map.Entry<Integer, List<List<Long>>>> entrySet() {
						Set<java.util.Map.Entry<Integer, List<List<Long>>>> entrySet =
							new HashSet<java.util.Map.Entry<Integer, List<List<Long>>>>();
						entrySet.add(new SimpleEntry<Integer, List<List<Long>>>(bin.getBinNumber(), StubBAMFileIndex.this.getChunkList(bin)));
						return entrySet;
					}
				};
			}
			@Override
			public void remove() {}
		};
	}
}
