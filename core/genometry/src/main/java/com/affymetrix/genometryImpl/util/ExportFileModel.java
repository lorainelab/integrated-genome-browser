package com.affymetrix.genometry.util;

import com.affymetrix.genometry.parsers.*;
import com.affymetrix.genometry.symloader.BedGraph;
import com.affymetrix.genometry.symloader.Fasta;
import com.affymetrix.genometry.symloader.Gr;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class ExportFileModel {
	
	private static final String MIME_TYPE_PREFIX = "text/";
	private final Map<FileTypeCategory, List<Class<? extends AnnotationWriter>>> annotationWriters;

	public ExportFileModel(){
		annotationWriters = new EnumMap<>(FileTypeCategory.class);
		init();
	}
	
	private void init(){
		List<Class<? extends AnnotationWriter>> annotationList = new ArrayList<>();
		annotationList.add(BedParser.class);
		annotationList.add(BedDetailWriter.class);
		annotationWriters.put(FileTypeCategory.Annotation, annotationList);
		
		List<Class<? extends AnnotationWriter>> alignmentList = new ArrayList<>();
		alignmentList.add(BedParser.class);
		alignmentList.add(BedDetailWriter.class);
//		alignmentList.add(SAMWriter.class); // Disable SAMWriter as it is not completed
		annotationWriters.put(FileTypeCategory.Alignment, alignmentList);
				
		List<Class<? extends AnnotationWriter>> graphList = new ArrayList<>();
		graphList.add(BedGraph.class);
		graphList.add(Gr.class);
		annotationWriters.put(FileTypeCategory.Graph, graphList);
		
		List<Class<? extends AnnotationWriter>> sequenceList = new ArrayList<>();
		sequenceList.add(Fasta.class);
		annotationWriters.put(FileTypeCategory.Sequence, sequenceList);
	}
	
	public Optional<Map<UniFileFilter, AnnotationWriter>> getFilterToWriters(FileTypeCategory fileTypeCategory){
		List<Class<? extends AnnotationWriter>> availableWriters = annotationWriters.get(fileTypeCategory);
		if (availableWriters == null || availableWriters.isEmpty()) {		
			return Optional.empty();
		}
		
		Map<UniFileFilter, AnnotationWriter> filter2writers = new HashMap<>();
		for (Class<? extends AnnotationWriter> clazz : availableWriters) {
			try {
				AnnotationWriter writer = clazz.newInstance();
				String mimeType = writer.getMimeType();
				if (mimeType.startsWith(MIME_TYPE_PREFIX)) {
					String extension = mimeType.substring(MIME_TYPE_PREFIX.length());
					FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(extension);
					
					// Hack to display as bed detail
					String name = clazz == BedDetailWriter.class ? "BED Detail" : fth.getName();
					
					UniFileFilter filter = new UniFileFilter(extension, name);
					filter2writers.put(filter, writer);
				}else{
					Logger.getLogger(ExportFileModel.class.getName()).log(
						Level.WARNING, "Export file can't handle mime type {0}"
							 ,mimeType);
				}
			} catch (Exception ex) {
				Logger.getLogger(ExportFileModel.class.getName()).log(
						Level.WARNING, "Couldn't not initiate class for {0} due to exception {1}",
						new Object[]{clazz, ex.getMessage()});
			}
		}
		
		return Optional.ofNullable(filter2writers);
	}
	
	/**
	 * Collects syms at a given desired leaf level.
	 * @param sym A sym to evaluate. It cannot be null.
	 * @param syms Output list of syms. It cannot be null.
	 * @param desired_leaf_depth Desired depth. It cannot be negative.
	 */
	public static void collectSyms(SeqSymmetry sym, List<SeqSymmetry> syms, int desired_leaf_depth){
		if(sym == null || syms == null){
			throw new IllegalArgumentException("Neither of argument sym or syms can be null");
		}
		if(desired_leaf_depth < 0){
			throw new IllegalArgumentException("Desired leaf depth cannot be negative");
		}
		
		int depth = SeqUtils.getDepthFor(sym);
		if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				collectSyms(sym.getChild(i), syms, desired_leaf_depth);
			}
		} else {  // depth == desired_leaf_depth
			syms.add(sym);
		}
	}
}
