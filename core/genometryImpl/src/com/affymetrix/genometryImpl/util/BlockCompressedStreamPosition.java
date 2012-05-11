package com.affymetrix.genometryImpl.util;

public class BlockCompressedStreamPosition {
	private static final double COMPRESS_RATIO = 64.0 / 22.0; // 64K to about 19.5K
	private static final double BLOCK_FACTOR = (2 << 15) * COMPRESS_RATIO;
	private final long blockAddress;
	private final int currentOffset;
	public BlockCompressedStreamPosition(long blockAddress, int currentOffset) {
		super();
		this.blockAddress = blockAddress;
		this.currentOffset = currentOffset;
	}
	public long getApproximatePosition() {
		return (long)(blockAddress * BLOCK_FACTOR + currentOffset);
	}
	@Override
	public String toString() {
		return "" + blockAddress + ":" + currentOffset;
	}
}
