package net.sf.samtools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.util.BlockCompressedFilePointerUtil;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.SumOperator;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SynonymLookup;

public class BaiZoomSymLoader extends SymLoader {
	private BioSeq saveSeq;
	private GraphSym saveSym;

	public BaiZoomSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri, featureName, group);
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		BioSeq seq = overlapSpan.getBioSeq();
		if (!seq.equals(saveSeq) || saveSym == null) {
			File bamFile = new File(uri.toString().substring(0, uri.toString().length() - ".bai".length()));
			File bamIndexFile = new File(uri.toString());
			SAMFileReader sfr = new SAMFileReader(bamFile);
			SAMSequenceDictionary ssd = sfr.getFileHeader().getSequenceDictionary();
			List<SAMSequenceRecord> sList = ssd.getSequences();
			int refno = -1;
			String igbSeq = seq.toString();
			for (int i = 0; i < sList.size() && refno == -1; i++) {
				String bamSeq = SynonymLookup.getChromosomeLookup().getPreferredName(sList.get(i).getSequenceName());
				if (igbSeq.equals(bamSeq)) {
					refno = i;
				}
			}
			if (refno == -1) {
				saveSym = new GraphSym(new int[]{}, new int[]{}, new float[]{}, featureName, seq);
			}
			else {
				StubBAMFileIndex dbfi = new StubBAMFileIndex(bamIndexFile, ssd);
				BAMIndexContent bic = dbfi.query(refno);
				Iterator<Bin> blIter = bic.getBins().iterator();
				List<SeqSymmetry> graphs = new ArrayList<SeqSymmetry>();
				System.out.println(">>>>>>>>>> before collect");
				Date start = new Date();
				while (blIter.hasNext()) {
					Bin bin = blIter.next();
					int[] region = getRegion(bin.getBinNumber());
					int yValue = 0;
					for (Chunk chunk : bin.getChunkList()) {
						yValue += getUncompressedLength(chunk);
					}
					graphs.add(new GraphSym(new int[]{region[0]}, new int[]{region[1] - region[0]}, new float[]{yValue}, featureName, seq));
				}
				Date mid = new Date();
				System.out.println(">>>>>>>>>> collect took " + ((double)(mid.getTime() - start.getTime()) / 1000.0) + " seconds");
				saveSym = (GraphSym)new SumOperator().operate(seq, graphs);
				Date end = new Date();
				System.out.println(">>>>>>>>>> operator took " + ((double)(end.getTime() - mid.getTime()) / 1000.0) + " seconds");
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
				span = new int[]{base * mod, base * (mod + 1) - 1};
			}
			else {
				counter += base;
			}
		}
		return span;
	}
}
