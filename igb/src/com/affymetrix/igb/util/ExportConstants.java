package com.affymetrix.igb.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author nick
 */
public interface ExportConstants {

	static final String PREF_FILE = "File";
	static final String PREF_DIR = "Dir";
	static final String PREF_EXT = "Ext";
	static final String PREF_X = "X"; // Horizontal Resolution
	static final String PREF_Y = "Y"; // Vertical Resolution
	static final String[] EXTENSION = {".jpeg", ".png"};
	static final String[] DESCRIPTION = {
		"Joint Photographic Experts Group (*.jpeg)",
		"Portable Network Graphics (*.png)"
	};
	static final String DEFAULT_FILE = "export.jpeg";
}

class ImageInfo {

	private int width;
	private int height;
	private int xResolution = 300;
	private int yResolution = 300;

	ImageInfo(int w, int h) {
		width = w;
		height = h;
	}

	ImageInfo(int w, int h, int x, int y) {
		width = w;
		height = h;
		xResolution = x;
		yResolution = y;
	}
	
	public void reset(int w, int h, int x, int y)
	{
		width = w;
		height = h;
		xResolution = x;
		yResolution = y;
	}

	public void setWidth(int w) {
		width = w;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int h) {
		height = h;
	}

	public int getHeight() {
		return height;
	}

	public void setXResolution(int x) {
		xResolution = x;
	}

	public int getXResolution() {
		return xResolution;
	}

	public void setYResolution(int y) {
		yResolution = y;
	}

	public int getYResolution() {
		return yResolution;
	}
}

class ExportFileType {

	private String fileExtension;
	private String fileDescription;

	ExportFileType(String extension, String description) {
		fileExtension = extension;
		fileDescription = description;
	}

	public String getExtension() {
		return fileExtension;
	}

	public String getDescription() {
		return fileDescription;
	}

	@Override
	public String toString() {
		return getDescription();
	}
}

class ExportFileFilter extends FileFilter {

	public ExportFileType type;

	public ExportFileFilter(ExportFileType type) {
		this.type = type;
	}

	public boolean accept(File file) {

		if (file.isDirectory()) {
			return true;
		}

		return file.getName().toLowerCase().endsWith(type.getExtension());
	}

	public String getDescription() {
		return type.getDescription();
	}

	public String getExtension() {
		return type.getExtension();
	}

	public boolean accept(File file, String name) {
		return name.toLowerCase().endsWith(type.getExtension());
	}
}