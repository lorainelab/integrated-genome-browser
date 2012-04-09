package net.sf.samtools;

import java.io.File;

public class StubBAMFileIndex extends AbstractBAMFileIndex {
	public StubBAMFileIndex(final File file, SAMSequenceDictionary dictionary) {
        super(file, dictionary);
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
}
