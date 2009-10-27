package com.affymetrix.genoviz.bioviews;

/**
 *  A transform used internally by NeoSeq, should not be used directly.
 */
public final class ConstrainLinearTrnsfm extends LinearTransform {

	private double constrain_value;

	public ConstrainLinearTrnsfm() {
		constrain_value = 1;
	}

	public void setConstrainValue(double cv) {
		constrain_value = cv;
	}

	public double getConstrainValue() {
		return constrain_value;
	}

	@Override
	public double transform(int orientation, double in) {
		double out = 0;
		if (orientation == X) {
			out = in * xscale;
		} else if (orientation == Y) {
			out = in * yscale;
		}

		out = out - (out % constrain_value);
	
		if (orientation == X) {
			out += xoffset;
		} else if (orientation == Y) {
			out += yoffset;
		}

		return out;
	}

	public boolean equals(TransformI Tx) {
		return (Tx instanceof ConstrainLinearTrnsfm) &&
				super.equals(Tx) &&
				(constrain_value == ((ConstrainLinearTrnsfm)Tx).getConstrainValue());
	}
}
