package net.sf.samtools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.util.BlockCompressedFilePointerUtil;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.DepthOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symmetry.GraphIntervalSym;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
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

	private URI getBamURI(URI baiUri) throws Exception {
		String bamUriString = baiUri.toString().substring(0, baiUri.toString().length() - ".bai".length());
		if (!bamUriString.startsWith("file:") && !bamUriString.startsWith("http:") && !bamUriString.startsWith("https:") && !bamUriString.startsWith("ftp:")) {
			bamUriString = GeneralUtils.getFileScheme() + bamUriString;
		}
		return new URI(bamUriString);
	}

	private float getRealAvg(SimpleSeqSpan span) throws Exception {
		SymLoader symL = FileTypeHolder.getInstance().getFileTypeHandler("bam").createSymLoader(getBamURI(uri), featureName, group);
		@SuppressWarnings("unchecked")
		List<SeqSymmetry> symList = (List<SeqSymmetry>)symL.getRegion(span);
		if (symList.size() == 0) {
			return 0.0f;
		}
		Operator depthOperator = new DepthOperator(FileTypeCategory.Alignment);
		GraphIntervalSym sym = (GraphIntervalSym)depthOperator.operate(span.getBioSeq(), symList);
		float total = 0.0f;
		for (int i = 0; i < sym.getPointCount(); i++) {
			total += sym.getGraphYCoord(i) * (float)sym.getGraphWidthCoord(i);
		}
		return total / (float)span.getLength();
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
		BioSeq seq = overlapSpan.getBioSeq();
		if (!seq.equals(saveSeq) || saveSym == null) {
			File bamFile = new File(GeneralUtils.fixFileName(uri.toString().substring(0, uri.toString().length() - ".bai".length())));
			File bamIndexFile = new File(GeneralUtils.fixFileName(uri.toString()));
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
				int[] xList = new int[BIN_COUNT];
				for (int i = 0; i < BIN_COUNT; i++) {
					xList[i] = i * BIN_LENGTH;
				}
				int[] wList = new int[BIN_COUNT];
				Arrays.fill(wList, BIN_LENGTH);
				float[] yList = new float[BIN_COUNT];
				Arrays.fill(yList,  0.0f);
				float largestY = Float.MIN_VALUE;
				int indexLargest = -1;
				while (blIter.hasNext()) {
					Bin bin = blIter.next();
					if (bin.containsChunks()) {
						int[] region = getRegion(bin.getBinNumber());
						int yValue = 0;
						for (Chunk chunk : bin.getChunkList()) {
							if (chunk != null) {
								yValue += (double)(getUncompressedLength(chunk.getChunkStart(), chunk.getChunkEnd()) * BIN_LENGTH) / (double)(region[1] - region[0]);
							}
						}
						if (1 + region[1] - region[0] == BIN_LENGTH && yValue > 0.0f) { // smallest bin
							if (yValue > largestY || indexLargest == -1) {
								indexLargest = region[0] / BIN_LENGTH;
								largestY = yValue;
							}
						}
						for (int i = region[0] / BIN_LENGTH; i < (region[1] + 1) / BIN_LENGTH; i++) {
							yList[i] += yValue;
						}
					}
				}
				if (indexLargest != -1) {
					try {
						float realAvg = getRealAvg(new SimpleSeqSpan(indexLargest * BIN_LENGTH, (indexLargest + 1) * BIN_LENGTH, seq));
						if (realAvg > 0) {
							float ratio = realAvg / yList[indexLargest];
							for (int i = 0; i < yList.length; i++) {
								yList[i] *= ratio;
							}
						}
					}
					catch (Exception x) {
						Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "fail loading BAM segment " + uri, x);
					}
				}
				saveSym = new GraphSym(xList, wList, yList, featureName, seq);
			}
			saveSeq = seq;
		}
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>();
		symList.add(saveSym);
		return symList;
	}

	private static final int CHUNK_SIZE = 2 >> 16;
	private static final double COMPRESS_RATIO = 64.0 / 22.0; // 64K to about 19.5K
	private static long getUncompressedLength(long chunkStart, long chunkEnd) {
		long blockAddressStart = BlockCompressedFilePointerUtil.getBlockAddress(chunkStart);
		long blockAddressEnd = BlockCompressedFilePointerUtil.getBlockAddress(chunkEnd);
		long blockOffsetStart = BlockCompressedFilePointerUtil.getBlockOffset(chunkStart);
		long blockOffsetEnd = BlockCompressedFilePointerUtil.getBlockOffset(chunkEnd);
		if (blockAddressStart == blockAddressEnd) {
			return blockOffsetEnd - blockOffsetStart;
		}
		else {
			return Math.round((blockAddressEnd - blockAddressStart) * COMPRESS_RATIO + (CHUNK_SIZE - blockOffsetStart) + blockOffsetEnd);
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

	public static void main(String[] args) {
		for (int i = 0; i < 37449; i++) {
			int[] r = getRegion(i);
			System.out.println("" + i + ":" + r[0] + "-" + r[1]);
		}
/*
		int i = 0;
		boolean found = false;
		while (!found) {
			i++;
			int[] r = getRegion(i);
			if (r[0] == 22511616) {
				System.out.println("region = " + i);
				found = true;
			}
		}
*/	
	}
}
