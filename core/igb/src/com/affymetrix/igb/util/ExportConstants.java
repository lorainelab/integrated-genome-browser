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
	static final String PREF_DIR = "Dir";
	static final String PREF_RESOLUTION = "Resolution"; // same resolution for horizontal and vertical 
	static final String PREF_UNIT = "Unit";
	static final String[] EXTENSION = {".svg", ".png", ".jpeg", ".jpg"};
	static final String[] DESCRIPTION = {
		"Scalable Vector Graphics (*.svg)",
		"Portable Network Graphics (*.png)",
		"Joint Photographic Experts Group (*.jpeg)",
	};
	static final String DEFAULT_FILE = "export.png";
	
	static final Object[] RESOLUTION = {72, 200, 300, 400, 500, 600, 800, 1000};
	static final Object[] UNIT = { "pixels", "inches"};
}

class ImageInfo {

	private double width;
	private double height;
	private int resolution = 300;

	ImageInfo(double w, double h) {
		width = w;
		height = h;
	}

	ImageInfo(double w, double h, int r) {
		width = w;
		height = h;
		resolution = r;
	}

	public void reset(int w, int h, int r) {
		width = w;
		height = h;
		resolution = r;
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

	public void setResolution(int r) {
		resolution = r;
	}

	public int getResolution() {
		return resolution;
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