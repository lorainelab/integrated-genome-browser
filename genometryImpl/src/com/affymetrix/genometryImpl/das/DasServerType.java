package com.affymetrix.genometryImpl.das;

import com.affymetrix.genometryImpl.util.ServerTypeI;

public class DasServerType implements ServerTypeI {
	private static final String name = "DAS";
	public static final int ordinal = 20;
	private static final DasServerType instance = new DasServerType();
	public static DasServerType getInstance() {
		return instance;
	}

	private DasServerType() {
		super();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int compareTo(ServerTypeI o) {
		return ordinal - o.getOrdinal();
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String toString() {
		return name;
	}
}
