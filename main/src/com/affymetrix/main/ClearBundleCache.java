package com.affymetrix.main;

public class ClearBundleCache {
	public static void main(String[] args) {
		OSGiHandler.getInstance().clearCache();
	}
}
