package com.affymetrix.genometryImpl.quickload;

import com.affymetrix.genometryImpl.util.ServerTypeI;

public class QuickloadServerType implements ServerTypeI {
	private static final String name = "Quickload";
	public static final int ordinal = 30;
	private static final QuickloadServerType instance = new QuickloadServerType();
	public static QuickloadServerType getInstance() {
		return instance;
	}

	private QuickloadServerType() {
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
