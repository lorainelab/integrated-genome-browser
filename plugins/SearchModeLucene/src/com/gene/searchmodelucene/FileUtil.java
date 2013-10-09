package com.gene.searchmodelucene;

public class FileUtil {
	private static final FileUtil instance = new FileUtil();
	private FileUtil() {
		super();
	}
	public static final FileUtil getInstance() {
		return instance;
	}

	private int getNamePosition(String uri) {
	//	String separator = (uri.toLowerCase().startsWith(HTTP_PREFIX) || uri.toLowerCase().startsWith(HTTPS_PREFIX)) ? HTTP_SEPARATOR : FILE_SEPARATOR;
		int pos = Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('\\')) + 1;
		return pos;
	}

	public String getIndexName(String uri) {
		int pos = getNamePosition(uri);
		if (pos >= uri.length()) {
			return "";
		}
		return uri.substring(0, pos) + "." + uri.substring(pos) + ".index";
	}

	public boolean isIndexName(String uri) {
		int pos = getNamePosition(uri);
		return pos < uri.length() && uri.charAt(pos) == '.' && uri.endsWith(".index");
	}

	public boolean isDirName(String uri) {
		int pos = getNamePosition(uri);
		return pos < uri.length() && uri.charAt(pos) == '.' && uri.endsWith(".index.dir");
	}
}
