package com.affymetrix.genometryImpl.das2;

import com.affymetrix.genometryImpl.util.ServerTypeI;

public class Das2ServerType implements ServerTypeI {
	private static final String name = "DAS2";
	public static final int ordinal = 10;
	private static final Das2ServerType instance = new Das2ServerType();
	public static Das2ServerType getInstance() {
		return instance;
	}

	private Das2ServerType() {
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
