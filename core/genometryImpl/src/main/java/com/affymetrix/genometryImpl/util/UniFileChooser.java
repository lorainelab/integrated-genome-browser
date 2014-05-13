/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.util;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


/**
 *  For reading files, you probably need to set-up your own JFileChooser, but
 *  the UniFileFilter class can be helpful in that case.
 *
 */
public class UniFileChooser extends GFileChooser {
	static final long serialVersionUID = 1L;

	/**
	 *  A singleton UniFileChooser, re-used when possible.
	 */
	private static UniFileChooser static_file_chooser;

	private String description = "Any file (*.*)";
	private String extension = "";

	private FileFilter current_file_filter;

	protected UniFileChooser() {
		// bare constructor.  Allows for subclassing.
		super();
	}

	/**
	 *  Creates and returns a JFileChooser which accepts only filenames
	 *  ending in period+extension when creating or writing to a file.
	 *
	 *  <p>Example: new UniFileChooser("AXML file", "axml");
	 */
	public UniFileChooser(String description, String extension) {
		this();
		reinitialize(description, extension);
	}

	public static UniFileChooser getFileChooser(String description, String extension) {
		if (static_file_chooser == null) {
			static_file_chooser = new UniFileChooser(description, extension);
		}
		else {
			static_file_chooser.reinitialize(description, extension);
		}

		return static_file_chooser;
	}

	/**
	 *  Reinitializes a singleton JFileChooser to accept only an ".axml" filename.
	 */
	public static UniFileChooser getAXMLFileChooser() {
		return getFileChooser("AXML file", "axml");
	}

	/**
	 *  Reinitializes a singleton JFileChooser to accept only an ".xml" filename.
	 */
	public static UniFileChooser getXMLFileChooser() {
		return getFileChooser("XML file", "xml");
	}

	/**
	 *  Resets such that it will accept only filenames
	 *  ending in period+extension when creating or writing to a file.
	 *
	 *  <p>Example: reinitialize("AXML file", "axml");
	 */
	public void reinitialize(final String description, final String extension) {
		if (description==null || extension==null || "".equals(extension)) {
			throw new IllegalArgumentException("description and extension cannot be null");
		}

		if (extension.indexOf('.') != -1) {
			throw new IllegalArgumentException("extension should not contain \'.\'");
		}

		if (this.description != description || this.extension != extension) {
			this.description = description;
			this.extension = extension;

			FileFilter[] filters = getChoosableFileFilters();
			for (int i=0; i<filters.length; i++) {
				removeChoosableFileFilter(filters[i]);
			}
			current_file_filter = new UniFileFilter(extension, description);

			addChoosableFileFilter(current_file_filter);
		}

		//addChoosableFileFilter(getAcceptAllFileFilter());
		setFileFilter(current_file_filter);
		setMultiSelectionEnabled(false);
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		rescanCurrentDirectory();
		setSelectedFile(null);
	}
}
