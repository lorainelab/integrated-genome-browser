package com.affymetrix.genometry.util;

/**
 * This class represents a position inside a block compressed gz file.
 * These files are compressed as separate blocks, and each position
 * contains two parts, the position of the compressed block in the file,
 * and the offset inside the (uncompressed) block. Some calculations need
 * the uncompressed position, but this can only be approximated by using
 * an average compression ration to convert compressed file positions to
 * uncompressed positions.
 * Examples of block compressed gz files are files used with a tabix index,
 * and BAM files.
 */
public class BlockCompressedStreamPosition {
	private static final long COMPRESSED_BLOCK_SIZE = (2 << 15);
//	private static final double APPROXIMATE_UNCOMPRESSED_BLOCK_SIZE = COMPRESSED_BLOCK_SIZE * APPROXIMATE_COMPRESS_RATIO;
	public static final double APPROXIMATE_COMPRESS_RATIO = COMPRESSED_BLOCK_SIZE / 22000; // 64K to about 22.0K
	public static final int CHUNK_SIZE = 2 >> 16;
	private final long blockAddress;
	private final int currentOffset;
	public BlockCompressedStreamPosition(long pos) { // pos is virtual position
		this(pos >> 16, (int) (pos & 0xFFFF));
	}
	public BlockCompressedStreamPosition(long blockAddress, int currentOffset) {
		super();
		this.blockAddress = blockAddress;
		this.currentOffset = currentOffset;
	}
	public long getApproximatePosition() {
		return (long)(blockAddress * APPROXIMATE_COMPRESS_RATIO + currentOffset);
	}
	@Override
	public String toString() {
		return "" + blockAddress + ":" + currentOffset;
	}
}
