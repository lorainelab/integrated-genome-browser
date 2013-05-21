package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;
import java.util.Map;

/**
 * An abstraction for performing an operation on
 * one or more SeqSymmetries to create a new SeqSymetry.
 * input type (and quantity) and output are specified by
 * FileTypeCategory. Parameters for the Operator are optional,
 * as well as ordering (for the UI).
 */
public interface Operator  {

	/**
	 * Unique identifier for the Operator, MUST be unique,
	 * and should not be displayed to the users, use getDisplay().
	 * This should help you keep track of different operators.
	 * Note that this should be different for each instance.
	 * @return a name suitable for identifying this operator.
	 */
	public String getName();

	/**
	 * user display
	 * @return a string suitable for showing a user.
	 */
	public String getDisplay();

	/**
	 * Carry out the operation, only on the specified BioSeq.
	 * @param aseq - BioSeq - necessary because some SeqSymmetries
	 *   can refer to multiple BioSeqs.
	 * @param symList symmetries used as input.
	 * @return output, a new symmetry.
	 */
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList);

	/**
	 * specify the minimum number of SeqSymmetries of the specified
	 * FileTypeCategory, can be zero
	 * @return the smallest size list allowed.
	 */
	public int getOperandCountMin(FileTypeCategory category);

	/**
	 * specify the maximum number of SeqSymmetries of the specified
	 * FileTypeCategory, can be zero
	 * @return the largest size list allowed.
	 */
	public int getOperandCountMax(FileTypeCategory category);

	/**
	 * Some operators can take parameters for different
	 * instances of the same Operator class.
	 * @return parameters for this Operator
	 */
	public Map<String, Class<?>> getParameters();

	/**
	 * Used for properties specific to a type of operator.
	 * Why does it take a different type Map
	 * from that returned by {@link #getParameters}?
	 */
	public boolean setParameters( Map<String, Object> parms);

	/**
	 * Indicates whether or not forward and reverse tracks are supported.
	 * @return true if for/rev supported, false otherwise
	 */
	public boolean supportsTwoTrack();

	/**
	 * specify the FileTypeCategory of the result SeqSymmetry
	 * @return the file type category appropriate for the BioSeq
	 * returned by {@link #operate}.
	 */
	public FileTypeCategory getOutputCategory();

	/**
	 * optional interface for Operators, used to order
	 * the list of Operators in the UI
	 */
	public static interface Order{
		public int getOrder();
	}
	
	public static interface Style{
		public Map<String,Object> getStyleProperties();
	}
}
