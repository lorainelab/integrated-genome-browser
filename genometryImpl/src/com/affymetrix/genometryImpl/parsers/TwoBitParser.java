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
import java.util.Arrays;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class TwoBitParser {
	/** Magic Number of 2bit files */
    private static final int MAGIC_NUMBER = 0x1A412743;

	/** Size of integer, in bytes */
	private static final int INT_SIZE = 4;

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

		long oldPosition = channel.position();
        channel.position(offset);
        loadBuffer(channel, buffer);

		//dnaSize
        long size = buffer.getInt() & INT_MASK;
		System.out.println("size is " + size + " bases");

		if (size > Integer.MAX_VALUE) {
			throw new IOException("IGB can not handle sequences larger than " + Integer.MAX_VALUE + ".  Offending sequence length: " + size);
		}

		BioSeq seq = seq_group.addSeq(name, (int) size);

		//nBlockCount, nBlockStart, nBlockSize
        readBlocks(channel, buffer, seq, nBlocks);

		//maskBlockCount, maskBlockStart, maskBlockSize
		readBlocks(channel, buffer, seq, maskBlocks);

		//reserved
        if (buffer.getInt() != 0) {
            throw new IOException("Unknown 2bit format: sequence's reserved field is non zero");
        }

		long residueOffset = offset + 4*4 + nBlocks.getSpanCount()*4*2 + maskBlocks.getSpanCount()*4*2;
		channel.position(residueOffset);
		loadBuffer(channel,buffer);

		//packedDNA
		SeqSpan nBlock = null,maskBlock = null;
		byte valueBuffer[] = new byte[BUFFER_SIZE];
		long length = size/4 + size%4;
		long residueCounter = 0;
		char temp[] = new char[4];
		for (int i = 0; i < length; i+=BUFFER_SIZE) {
			buffer.get(valueBuffer);
			for (int k = 0; k < BUFFER_SIZE && k < length; k++) {

				temp = parseByte(valueBuffer[k]);
				
				for(int j = 0; j < 4; j++){

					if(nBlock == null){
						nBlock = GetBlock(residueCounter,nBlocks);
					}

					if(nBlock != null){
						if(nBlock.getEnd() == residueCounter)
							nBlock = null;
						else
							temp[j] = 'N';
					}

					if(maskBlock == null){
						maskBlock = GetBlock(residueCounter,maskBlocks);
					}

					if(maskBlock != null){
						if(maskBlock.getEnd() == residueCounter)
							maskBlock = null;
						else
							temp[j] = Character.toLowerCase(temp[j]);
					}

					residueCounter++;
				}

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

	private char[] parseByte(byte valueBuffer){
		char temp[] = new char[4];
		int dna, value = valueBuffer & BYTE_MASK;

		for (int j = 3; j >= 0; j--) {
			dna = value & CHAR_MASK;
			value = value >> 2;
			temp[j] = BASES[dna];
		}
		return temp;
	}

	private SeqSpan GetBlock(long start, MutableSeqSymmetry blocks){
		for(int i=0; i<blocks.getSpanCount(); i++){
			SeqSpan block = blocks.getSpan(i);
			if(block.getStart() == start)
				return block;
		}
		return null;
	}

	private long updateBuffer(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
		channel.position(position - buffer.remaining());
		loadBuffer(channel, buffer);
		return channel.position();
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
		//File f = new File("/Users/aloraine/Downloads/tests/output/testMask.2bit");
		File f = new File("genometryImpl/test/data/2bit/at.2bit");
		TwoBitParser instance = new TwoBitParser();
		try {
			BioSeq seq = instance.parse(f, new AnnotatedSeqGroup("foo"));
			//seq.getResidues();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
