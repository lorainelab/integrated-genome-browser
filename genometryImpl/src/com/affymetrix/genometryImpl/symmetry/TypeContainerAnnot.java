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

package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;

/**
 *  Top-level annots attached to a BioSeq.
 */
public final class TypeContainerAnnot extends RootSeqSymmetry implements TypedSym   {
	private static final FileTypeCategory DEFAULT_CATEGORY = FileTypeCategory.Annotation;
	private final String ext;
	String type;

	public TypeContainerAnnot(String type) {
		this(type,"");
	}

	public TypeContainerAnnot(String type, String ext) {
		super();
		this.type = type;
		this.ext = ext;
		this.setProperty("method", type);
		this.setProperty(CONTAINER_PROP, Boolean.TRUE);
	}

	public String getType()  { return type; }

	@Override
	public FileTypeCategory getCategory() {
		FileTypeCategory category = null;
		FileTypeHandler handler = FileTypeHolder.getInstance().getFileTypeHandler(ext);
		if (handler != null) {
			category = handler.getFileTypeCategory();
		}
		if (category == null) {
			category = DEFAULT_CATEGORY;
		}
		return category;
	}
}
