package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;
import java.util.Map;

/**
 * An abstraction for generating a SeqSymetry from a list of others.
 */
public interface Operator  {

	/**
	 * This should help you keep track of different operators.
	 * Note that this should be different for each instance.
	 * @return a name suitable for identifying this operator.
	 */
	public String getName();

	/**
	 * @return a string suitable for showing a user.
	 */
	public String getDisplay();

	/**
	 * Carry out the operation.
	 * @param aseq What is this for?
	 * @param symList symmetries used as input.
	 * @return a new symmetry.
	 */
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList);

	/** @return the smallest size list allowed. */
	public int getOperandCountMin(FileTypeCategory category);

	/** @return the largest size list allowed. */
	public int getOperandCountMax(FileTypeCategory category);

	public Map<String, Class<?>> getParameters();

	/**
	 * Used for properties specific to a type of operator.
	 * Why does it take a different type Map
	 * from that returned by {@link #getParameters}?
	 */
	public boolean setParameters( Map<String, Object> parms);

	/** Indicates whether or not forward and reverse tracks are supported. */
	public boolean supportsTwoTrack();

	/**
	 * @return the file type category appropriate for the BioSeq
	 * returned by {@link #operate(BioSeq, List<SeqSymetry>)).
	 */
	public FileTypeCategory getOutputCategory();

	/** What is this for? */
	public static interface Order{
		public int getOrder();
	}

}
