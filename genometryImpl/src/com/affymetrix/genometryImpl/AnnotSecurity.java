package com.affymetrix.genometryImpl;

import java.util.Map;

/*
 * An interface to determining if user has access to an
 * annotation.
 */
public interface AnnotSecurity {

	/**
	 * Indicates if user is allowed to see this annotation.
	 * 
	 * @param genomeVersionName The name of the name version
	 * @param annotationId The id of the annotation
	 * @return true if user is authorized to access this annotation; false
	 *          if user is not authorized.
	 */
	public boolean isAuthorized(String genomeVersionName, String annotationName, Object annotationId);
	
	
	/**
	 * Get the properties (tag/value pairs) for an annotation.
	 * 
	 * @param genomeVersionName The name of the name version
	 * @param annotationId The id of the annotation
	 * @return Property map of annotation.  (Only returns property map if
	 *          user is authorized to see this annotation.
	 */
	public Map<String, Object> getProperties(String genomeVersionName, String annotationName, Object annotationId);
	
	/**
	 * Determine if an annotation has a certain file extension
	 * 
	 * @param data_root
	 * @param genomeVersionName The name of the name version
	 * @param annotationId The id of the annotation
	 * @param extension The file extension
	 * @return true if this annotation has the given file extension
	 */
	public boolean hasFileExtension(String data_root, String genomeVersionName, String annotationName, Object annotationId, String extension);
	
	
	/**
	 * Returns the sequence directory for a given genome version
	 * 
	 * @param data_root
	 * @param genome
	 * @return
	 * @throws Exception
	 */
	
	public String getSequenceDirectory(String data_root, AnnotatedSeqGroup genome) throws Exception;
	
}
