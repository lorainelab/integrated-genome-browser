package com.affymetrix.igb.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author nick
 */
public interface ExportConstants {

	static final String PREF_FILE = "File";
	static final String PREF_EXT = "Ext";
	static final String PREF_X = "X"; // Horizontal Resolution
	static final String PREF_Y = "Y"; // Vertical Resolution
	static final String[] EXTENSION = {".jpeg", ".png"};
	static final String[] DESCRIPTION = {
		"Joint Photographic Experts Group (*.jpeg)",
		"Portable Network Graphics (*.png)"
	};
	static final String DEFAULT_FILE = "export.png";
}

class ImageInfo {

	private double width;
	private double height;
	private int xResolution = 300;
	private int yResolution = 300;

	ImageInfo(double w, double h) {
		width = w;
		height = h;
	}

	ImageInfo(double w, double h, int x, int y) {
		width = w;
		height = h;
		xResolution = x;
		yResolution = y;
	}

	public void reset(int w, int h, int x, int y) {
		width = w;
		height = h;
		xResolution = x;
		yResolution = y;
	}

	public void setWidth(double w) {
		width = w;
	}

	public double getWidth() {
		return width;
	}

	public void setHeight(double h) {
		height = h;
	}

	public double getHeight() {
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

	public double getWidthHeightRate() {
		return width / height;
	}

	public double getHeightWidthRate() {
		return height / width;
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