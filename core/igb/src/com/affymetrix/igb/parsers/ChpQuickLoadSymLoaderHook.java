package com.affymetrix.igb.parsers;

import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoader;
import com.affymetrix.genometryImpl.quickload.QuickLoadSymLoaderHook;
import com.affymetrix.genometryImpl.quickload.QuickloadServerType;

public class ChpQuickLoadSymLoaderHook implements QuickLoadSymLoaderHook {
	static {
		QuickloadServerType.addQuickLoadSymLoaderHook(new ChpQuickLoadSymLoaderHook());
	}

	@Override
	public QuickLoadSymLoader processQuickLoadSymLoader(QuickLoadSymLoader quickLoadSymLoader) {
		if (quickLoadSymLoader.getSymLoader().extension.endsWith("chp")) {
			return new QuickLoadSymLoaderChp(quickLoadSymLoader.uri, quickLoadSymLoader.featureName, quickLoadSymLoader.getAnnotatedSeqGroup());
		}
		return quickLoadSymLoader;
	}
}
