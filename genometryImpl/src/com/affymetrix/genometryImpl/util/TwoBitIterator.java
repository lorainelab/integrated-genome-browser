package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class TwoBitIterator implements SearchableCharIterator {

	/** Use a 4KB buffer, as that is the block size of most filesystems */
	private static final int BUFFER_SIZE = 4096;
	
	/** Number of residues in each byte */
	private static final int RESIDUES_PER_BYTE = 4;

	/** Byte mask for translating unsigned bytes into Java integers */
    private static final int BYTE_MASK = 0xff;

	/** Character mask for translating binary into Java chars */
	private static final int CHAR_MASK = 0x03;

	private static final char[] BASES = { 'T', 'C', 'A', 'G', 't', 'c', 'a', 'g'};

	private final File file;
	private final long length, offset;
	private final MutableSeqSymmetry nBlocks, maskBlocks;
	private final List<Segment> segments = new ArrayList<Segment>();
	private final ByteOrder byteOrder;

	public TwoBitIterator(File file, long length, long offset, ByteOrder byteOrder, MutableSeqSymmetry nBlocks, MutableSeqSymmetry maskBlocks) {
		this.file       = file;
		this.length     = length;
		this.offset     = offset;
		this.nBlocks    = nBlocks;
		this.maskBlocks = maskBlocks;
		this.byteOrder  = byteOrder;

		if (this.length > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("IGB can not handle sequences larger than " + Integer.MAX_VALUE + ".  Offending sequence length: " + length);
		}

//		int  current_start = 0;
//		long current_offset = offset;
//		SeqSpan block;
//		for (int i=0; i < nBlocks.getSpanCount(); i++) {
//			block = nBlocks.getSpan(i);
//			if (block.getMin() > current_start) {
//				segments.add(new Segment(current_start, block.getMin(), current_offset));
//				current_offset += block.getMin() - current_start;
//				current_start   = block.getMax();
//			}
//		}

	}

	private void loadBuffer(FileChannel channel, ByteBuffer buffer) throws IOException {
		buffer.rewind();
		channel.read(buffer);
		buffer.rewind();
	}

	public String substring(int offset, int length) {
		try {
			//		char bases[] = new char[length];
			//
			//		for (Segment segment : segments) {
			//			if (offset < segment.start) {
			//				continue;
			//			}
			//
			//			if (offset + length < segment.end) {
			//				break;
			//			}
			//		}
			long start = offset;
			long end = start + length;
			long residuePosition = start;
			long residueCounter = 0;
			long startOffset = start / RESIDUES_PER_BYTE;
			long bytesToRead = calculateBytesToRead(start, end);
			int beginLength = RESIDUES_PER_BYTE - (int) start % 4;
			int endLength = (int) end % RESIDUES_PER_BYTE;
			if (bytesToRead == 1) {
				if (start % RESIDUES_PER_BYTE == 0) {
					beginLength = 0;
				} else {
					endLength = 0;
				}
			}
			FileChannel channel = new RandomAccessFile(file, "r").getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			buffer.order(this.byteOrder);
			channel.position(this.offset + startOffset);
			loadBuffer(channel, buffer);
			updateBlocks(start, nBlocks);
			updateBlocks(start, maskBlocks);
			//packedDNA
			SeqSpan nBlock = null;
			SeqSpan maskBlock = null;
			
			byte[] valueBuffer = new byte[BUFFER_SIZE];
			char[] temp = null;
			for (int i = 0; i < bytesToRead; i += BUFFER_SIZE) {
				buffer.get(valueBuffer);
				for (int k = 0; k < BUFFER_SIZE && k < bytesToRead; k++) {
					if (k == 0 && beginLength != 0) {
						temp = parseByte(valueBuffer[k], beginLength, true);
					} else if (k == bytesToRead - 1 && endLength != 0) {
						temp = parseByte(valueBuffer[k], endLength, false);
					} else {
						temp = parseByte(valueBuffer[k]);
					}
					for (int j = 0; j < temp.length; j++) {
						nBlock = processResidue(residuePosition, temp, j, nBlock, nBlocks, false);
						maskBlock = processResidue(residuePosition, temp, j, maskBlock, maskBlocks, true);
						residuePosition++;
					}
					residueCounter += temp.length;
					//System.out.print(temp);
				
				}
				channel.position(channel.position() + BUFFER_SIZE);
				loadBuffer(channel, buffer);
			}
			System.out.println();
			System.out.println(residueCounter);
			channel.close();
			//channel.position(oldPosition);
			//throw new UnsupportedOperationException("Not supported yet.");
			System.gc();
			return new String("");
		} catch (Exception ex) {
			Logger.getLogger(TwoBitIterator.class.getName()).log(Level.SEVERE, null, ex);
		}
		return new String("");
	}

	private long calculateBytesToRead(long start, long end) {

		if(start/RESIDUES_PER_BYTE == end/RESIDUES_PER_BYTE)
			return 1;

		long length = end - start;
		long bytesToRead = length/RESIDUES_PER_BYTE;

		int endLength = (int)end % RESIDUES_PER_BYTE;
		int lengthExtra = length % RESIDUES_PER_BYTE == 0 ? 0 : 1;
		int endExtra = endLength == 0 ? 0 : 1;

		if(length <= RESIDUES_PER_BYTE)
			bytesToRead = bytesToRead + endExtra + lengthExtra;
		else
			bytesToRead = bytesToRead + Math.max(endExtra,lengthExtra);

		return bytesToRead;
	}

	private void updateBlocks(long start, MutableSeqSymmetry blocks){
		List<SeqSpan> removeSpans = new ArrayList<SeqSpan>();

		for(int i=0; i<blocks.getSpanCount(); i++){
			SeqSpan span = blocks.getSpan(i);
			if(start > span.getStart() && start > span.getEnd()){
					removeSpans.add(span);
			}

		}

		for(SeqSpan span: removeSpans){
			blocks.removeSpan(span);
		}
	}
	private SeqSpan processResidue(long residuePosition, char temp[], int pos, SeqSpan block, MutableSeqSymmetry blocks, boolean isMask){
		if (block == null) {
			block = GetNextBlock(blocks);
		}

		if (block != null) {
			if (residuePosition == block.getEnd()) {
				blocks.removeSpan( block);
				block = null;
			} else if (residuePosition >= block.getStart()) {
				if(isMask)
					temp[pos] = Character.toLowerCase(temp[pos]);
				else
					temp[pos] = 'N';
			}
		}
		return block;
	}

	private SeqSpan GetNextBlock(MutableSeqSymmetry Blocks){
		if(Blocks.getSpanCount() > 0)
			return Blocks.getSpan(0);
		else
			return null;
	}

	private char[] parseByte(byte valueBuffer, int size, boolean isFirst){
		char temp[] = parseByte(valueBuffer);
		char newTemp[] = new char[size];

		if(isFirst){
			int skip = temp.length - size;
			for(int i=0; i<size; i++){
				newTemp[i] = temp[skip+i];
			}
		}else{
			for(int i=0; i<size; i++){
				newTemp[i] = temp[i];
			}
		}
		return newTemp;
	}

	private char[] parseByte(byte valueBuffer){
		char temp[] = new char[RESIDUES_PER_BYTE];
		int dna, value = valueBuffer & BYTE_MASK;

		for (int j = RESIDUES_PER_BYTE; j > 0; j--) {
			dna = value & CHAR_MASK;
			value = value >> 2;
			temp[j-1] = BASES[dna];
		}

		return temp;
	}
	
	public int indexOf(String needle, int offset) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getLength() {
		return (int) length;
	}

	private static final class Segment {
		protected final int start, end;
		protected final long offset;

		protected Segment(int start, int end, long offset) {
			this.start  = start;
			this.end    = end;
			this.offset = offset;
		}
	}
}
