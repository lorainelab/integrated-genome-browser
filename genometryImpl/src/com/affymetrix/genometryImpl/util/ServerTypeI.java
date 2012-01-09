package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.das.DasServerType;
import com.affymetrix.genometryImpl.das2.Das2ServerType;
import com.affymetrix.genometryImpl.quickload.QuickloadServerType;

public interface ServerTypeI extends Comparable<ServerTypeI> {
	ServerTypeI QuickLoad = QuickloadServerType.getInstance();
	ServerTypeI DAS = DasServerType.getInstance();
	ServerTypeI DAS2 = Das2ServerType.getInstance();
	ServerTypeI LocalFiles = LocalFilesServerType.getInstance();

	public String getName();
	public int getOrdinal();
}