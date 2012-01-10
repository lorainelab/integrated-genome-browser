package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.ServerTypeI;

public class LocalFilesServerType implements ServerTypeI {
	private static final String name = "Local Files";
	public static final int ordinal = 40;
	private static final LocalFilesServerType instance = new LocalFilesServerType();
	public static LocalFilesServerType getInstance() {
		return instance;
	}

	private LocalFilesServerType() {
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

	@Override
	public boolean processServer(GenericServer gServer, String path) {
		return false;
	}

	@Override
	public String formatURL(String url) {
		return url;
	}

	@Override
	public Object getServerInfo(String url, String name) {
		return null;
	}
}
