package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

import java.io.*;
import java.util.*;


public interface AnnotationWriter {

	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) throws IOException;

	public String getMimeType();
}
