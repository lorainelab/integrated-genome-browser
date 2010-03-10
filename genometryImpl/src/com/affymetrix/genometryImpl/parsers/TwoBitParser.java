package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.TwoBitIterator;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sgblanch
 * @author hiralv
 * @version $Id$
 */
public final class TwoBitParser {
	/** Magic Number of 2bit files */
    private static final int MAGIC_NUMBER = 0x1A412743;

	/** Size of integer, in bytes */
	private static final int INT_SIZE = 4;

	/** Number of residues in each byte */
	private static final int RESIDUES_PER_BYTE = 4;

	/** Use a 4KB buffer, as that is the block size of most filesystems */
	private static  int BUFFER_SIZE = 4096;

    /** Byte mask for translating unsigned bytes into Java integers */
    private static final int BYTE_MASK = 0xff;

    /** Byte mask for translating unsigned ints into Java longs */
    private static final long INT_MASK = 0xffffffff;

	/** Character mask for translating binary into Java chars */
	private static final int CHAR_MASK = 0x03;

	/** Character set used to decode strings.  Currently ASCII */
    private static final Charset charset = Charset.forName("ASCII");

	private File file;

	private static final char[] BASES = { 'T', 'C', 'A', 'G', 't', 'c', 'a', 'g'};

    public BioSeq parse(File file, AnnotatedSeqGroup seq_group) throws FileNotFoundException, IOException {
		this.file = file;
        FileChannel channel = new RandomAccessFile(file, "r").getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		loadBuffer(channel, buffer);
        int seq_count = readFileHeader(buffer);
        BioSeq seq = readSequenceIndex(channel, buffer, seq_count, seq_group);
		channel.close();
		return seq;
    }

    private static String getString(ByteBuffer buffer, int length) {
        byte[] string = new byte[length];
        buffer.get(string);
        return new String(string, charset);
    }

	/**
	 * Load data from the channel into the buffer.  This convenience method is
	 * used to ensure that the buffer has the correct endian and is rewound.
	 */
	private void loadBuffer(FileChannel channel, ByteBuffer buffer) throws IOException {
		buffer.rewind();
		channel.read(buffer);
		//buffer.order(byteOrder);
		buffer.rewind();
	}

    private int readFileHeader(ByteBuffer buffer) throws IOException {
        /* Java is big endian so try that first */
        int magic = buffer.getInt();

        /* Try little endian if big endian did not work */
        if (magic != MAGIC_NUMBER) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.rewind();
			magic = buffer.getInt();
        }

        /* Fail if we have no magic */
        if (magic != MAGIC_NUMBER) {
            throw new IOException("File is not in 2bit format:  Bad magic (0x" + Integer.toHexString(magic) + " actual, 0x" + Integer.toHexString(MAGIC_NUMBER) + " expected)");
        }

        /* Grab the rest of the header fields */
        int version = buffer.getInt();
        int seq_count = buffer.getInt();
        int reserved = buffer.getInt();

        /* Currently version and 'reserved' should be zero */
        if (version != 0 || reserved != 0) {
            throw new IOException("Unsupported 2bit format: version(" + version + ") and reserved(" + reserved + ") must equal 0");
        }

        return seq_count;
    }

    private void readBlocks(FileChannel channel, ByteBuffer buffer, BioSeq seq, MutableSeqSymmetry sym) throws IOException {
		//xBlockCount, where x = n OR mask
		int block_count = buffer.getInt();
		System.out.println("I want " + block_count + " blocks");
        int[] blockStarts = new int[block_count];
        //ByteBuffer buffer = ByteBuffer.allocate(2 * block_count * INT_SIZE + INT_SIZE);
        for (int i = 0; i < block_count; i++) {
			//xBlockStart, where x = n OR mask
            blockStarts[i] = buffer.getInt();
        }

        for (int i = 0; i < block_count; i++) {
			//xBlockSize, where x = n OR mask
			sym.addSpan(new SimpleSeqSpan(blockStarts[i], blockStarts[i] + buffer.getInt(), seq));
        }

    }


    private BioSeq readSequenceIndex(FileChannel channel, ByteBuffer buffer, int seq_count, AnnotatedSeqGroup seq_group) throws IOException {
        String name;
        int name_length;
		long offset, position;

		position = channel.position();
		//for (int i = 0; i < seq_count; i++) {
		if (buffer.remaining() < INT_SIZE) {
			position = updateBuffer(channel, buffer, position);
		}

		name_length = buffer.get() & BYTE_MASK;

		if (buffer.remaining() < name_length + INT_SIZE) {
			position = updateBuffer(channel, buffer, position);
		}

		name = getString(buffer, name_length);
		offset = buffer.getInt() & INT_MASK;

		System.out.println("Sequence '" + name + "', offset " + offset);
		return readSequenceHeader(channel, buffer.order(), offset, seq_group, name);
		//}
    }

    private BioSeq readSequenceHeader(FileChannel channel, ByteOrder order, long offset, AnnotatedSeqGroup seq_group, String name) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.order(order);
		MutableSeqSymmetry nBlocks    = new SimpleMutableSeqSymmetry();
		MutableSeqSymmetry maskBlocks = new SimpleMutableSeqSymmetry();
		long residueOffset = offset;

		long oldPosition = channel.position();
        channel.position(offset);
        loadBuffer(channel, buffer);

		//dnaSize
        long size = buffer.getInt() & INT_MASK;
		System.out.println("size is " + size + " bases");
		residueOffset += INT_SIZE;

		if (size > Integer.MAX_VALUE) {
			throw new IOException("IGB can not handle sequences larger than " + Integer.MAX_VALUE + ".  Offending sequence length: " + size);
		}

		BioSeq seq = seq_group.addSeq(name, (int) size);

		//nBlockCount, nBlockStart, nBlockSize
        readBlocks(channel, buffer, seq, nBlocks);
		residueOffset += INT_SIZE + nBlocks.getSpanCount() * INT_SIZE * 2;

		//maskBlockCount, maskBlockStart, maskBlockSize
		readBlocks(channel, buffer, seq, maskBlocks);
		residueOffset += INT_SIZE + maskBlocks.getSpanCount() * INT_SIZE * 2;

		//reserved
        if (buffer.getInt() != 0) {
            throw new IOException("Unknown 2bit format: sequence's reserved field is non zero");
        }
		residueOffset += INT_SIZE;

		long start = 3, end = 6;
		long residuePosition = start;
		long residueCounter = 0;
		long startOffset = start / RESIDUES_PER_BYTE;

		long bytesToRead = calculateBytesToRead(start,end);
		int beginLength = RESIDUES_PER_BYTE - (int)start % 4;
		int endLength = (int)end % RESIDUES_PER_BYTE;

		if(bytesToRead == 1){
			if(start % RESIDUES_PER_BYTE == 0)
				beginLength = 0;
			else
				endLength = 0;
		}
		
		channel.position(residueOffset + startOffset);
		loadBuffer(channel,buffer);
		updateBlocks(start,nBlocks);
		updateBlocks(start,maskBlocks);

		//packedDNA
		SeqSpan nBlock = null,maskBlock = null;
		byte valueBuffer[] = new byte[BUFFER_SIZE];
		char temp[] = null;

		for (int i = 0; i < bytesToRead; i+=BUFFER_SIZE) {
			buffer.get(valueBuffer);
			for (int k = 0; k < BUFFER_SIZE && k < bytesToRead; k++) {

				if(k == 0 && beginLength != 0){
					temp = parseByte(valueBuffer[k], beginLength,true);
				}else if(k == bytesToRead - 1 && endLength != 0){
					temp = parseByte(valueBuffer[k], endLength,false);
				}else{
					temp = parseByte(valueBuffer[k]);
				}

				for (int j = 0; j < temp.length; j++) {
					nBlock = processResidue(residuePosition, temp, j, nBlock, nBlocks, false);
					maskBlock = processResidue(residuePosition, temp, j, maskBlock, maskBlocks, true);
					residuePosition++;
				}
				residueCounter += temp.length;
				System.out.print(temp);
			}
			channel.position(channel.position() + BUFFER_SIZE);
			loadBuffer(channel, buffer);
		}
		System.out.println();
		System.out.println(residueCounter);

//		seq.setResiduesProvider(new TwoBitIterator(file,size,residueOffset,buffer.order(),nBlocks,maskBlocks));
		channel.position(oldPosition);
		return seq;
    }


	private long updateBuffer(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
		channel.position(position - buffer.remaining());
		loadBuffer(channel, buffer);
		return channel.position();
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

	public static String getMimeType() {
		return "binary/2bit";
	}

	public static boolean writeAnnotations(BioSeq seq, OutputStream outstream)
	{
		DataOutputStream dos = null;
		try
		{
			dos = new DataOutputStream(outstream);
			dos.writeBytes(seq.getResidues());
			dos.flush();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public static void main(String[] args){
		File f = new File("/afs/transvar.org/home/hvora1/Desktop/test1.2bit");
		//File f = new File("genometryImpl/test/data/2bit/at.2bit");
		TwoBitParser instance = new TwoBitParser();
		try {
			BioSeq seq = instance.parse(f, new AnnotatedSeqGroup("foo"));
			//seq.getResidues();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
