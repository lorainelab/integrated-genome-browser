package net.sf.samtools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.sf.samtools.util.BlockCompressedFilePointerUtil;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SynonymLookup;

public class BaiZoomSymLoader extends SymLoader {
	private static final int BIN_COUNT = 32768; // smallest bin
	private static final int BIN_LENGTH = 16384; // smallest bin
	private BioSeq saveSeq;
	private GraphSym saveSym;

	public BaiZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri, featureName, group);
	}

	private int getRefNo(String igbSeq, SAMSequenceDictionary ssd) {
		List<SAMSequenceRecord> sList = ssd.getSequences();
		for (int i = 0; i < sList.size(); i++) {
			String bamSeq = SynonymLookup.getChromosomeLookup().getPreferredName(sList.get(i).getSequenceName());
			if (igbSeq.equals(bamSeq)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		BioSeq seq = overlapSpan.getBioSeq();
		if (!seq.equals(saveSeq) || saveSym == null) {
			File bamFile = new File(uri.toString().substring(0, uri.toString().length() - ".bai".length()));
			File bamIndexFile = new File(uri.toString());
			SAMFileReader sfr = new SAMFileReader(bamFile);
			SAMSequenceDictionary ssd = sfr.getFileHeader().getSequenceDictionary();
			int refno = getRefNo(seq.toString(), ssd);
			if (refno == -1) {
				saveSym = new GraphSym(new int[]{}, new int[]{}, new float[]{}, featureName, seq);
			}
			else {
				StubBAMFileIndex dbfi = new StubBAMFileIndex(bamIndexFile, ssd);
				BAMIndexContent bic = dbfi.query(refno);
				Iterator<Bin> blIter = bic.getBins().iterator();
				final String symId = UUID.randomUUID().toString();
				int[] xList = new int[BIN_COUNT];
				for (int i = 0; i < BIN_COUNT; i++) {
					xList[i] = i * BIN_LENGTH;
				}
				int[] wList = new int[BIN_COUNT];
				Arrays.fill(wList, BIN_LENGTH);
				float[] yList = new float[BIN_COUNT];
				Arrays.fill(yList,  0.0f);
				while (blIter.hasNext()) {
					Bin bin = blIter.next();
					if (bin.containsChunks()) {
						int[] region = getRegion(bin.getBinNumber());
						int yValue = 0;
						for (Chunk chunk : bin.getChunkList()) {
							if (chunk != null) {
								yValue += getUncompressedLength(chunk);
							}
						}
						for (int i = region[0] / BIN_LENGTH; i < region[1] / BIN_LENGTH; i++) {
							yList[i] += yValue;
						}
					}
				}
				saveSym = new GraphSym(xList, wList, yList, symId, seq);
			}
			saveSeq = seq;
		}
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		symList.add(saveSym);
		return symList;
	}

	private static final int CHUNK_SIZE = 2 >> 16;
	private static final int COMPRESS_RATIO = 64 / 20; // 64K to about 19.5K
	private static long getUncompressedLength(Chunk chunk) {
		long blockAddressStart = BlockCompressedFilePointerUtil.getBlockAddress(chunk.getChunkStart());
		long blockAddressEnd = BlockCompressedFilePointerUtil.getBlockAddress(chunk.getChunkEnd());
		long blockOffsetStart = BlockCompressedFilePointerUtil.getBlockOffset(chunk.getChunkStart());
		long blockOffsetEnd = BlockCompressedFilePointerUtil.getBlockOffset(chunk.getChunkEnd());
		if (blockAddressStart == blockAddressEnd) {
			return blockOffsetEnd - blockOffsetStart;
		}
		else {
			return (blockAddressEnd - blockAddressStart) * COMPRESS_RATIO + (CHUNK_SIZE - blockOffsetStart) + blockOffsetEnd;
		}
	}

	private static int[] getRegion(int binno) {
		int counter = 0;
		int idx = -3;
		int [] span = null;
		while (span == null) {
			idx += 3;
			int base = (int)Math.pow(2, idx);
			if (counter + base > binno) {
				int mod = binno - counter;
				int lvl = (int)Math.pow(2, 29 - idx);
				span = new int[]{lvl * mod, lvl * (mod + 1) - 1};
			}
			else {
				counter += base;
			}
		}
		return span;
	}
}
