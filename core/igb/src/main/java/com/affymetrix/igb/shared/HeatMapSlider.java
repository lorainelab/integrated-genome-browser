package com.affymetrix.igb.shared;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JSlider;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 * The thumb controls are used to select the lower and upper value of a range
 * with predetermined minimum and maximum values.
 *
 * <p>
 * Note that HeatMapSlider makes use of the default BoundedRangeModel, which
 * supports an inner range defined by a value and an extent. The upper value
 * returned by HeatMapSlider is simply the lower value plus the extent.</p>
 */
public class HeatMapSlider extends JSlider {

    /**
     * Constructs a HeatMapSlider with the specified default minimum and maximum
     * values.
     */
    public HeatMapSlider(int noOfThumbs, int min, int max) {
        super(new MultiValuedBoundedRangeModel(noOfThumbs, min, max));
        setMinimum(min);
        setMaximum(max);
        setOrientation(HORIZONTAL);
    }

    /**
     * Overrides the superclass method to install the UI delegate to draw two
     * thumbs.
     */
    @Override
    public void updateUI() {
        setUI(new HeatMapSliderUI(this, ((MultiValuedBoundedRangeModel) getModel()).totalValues()));
        // Update UI for slider labels.  This must be called after updating the
        // UI of the slider.  Refer to JSlider.updateUI().
        updateLabelUIs();
    }

    /**
     * Returns the upper value in the range.
     */
    public int getValue(int thumbNumber) {
        return ((MultiValuedBoundedRangeModel) getModel()).getValue(thumbNumber);
    }

    /**
     * Sets the upper value in the range.
     */
    public void setValue(int thumbNumber, int value) {

        // Compute new extent.
        int lowerValue = thumbNumber - 1 < 0 ? getModel().getMinimum()
                : ((MultiValuedBoundedRangeModel) getModel()).getValue(thumbNumber - 1);
        int higherValue = thumbNumber + 1 >= ((MultiValuedBoundedRangeModel) getModel()).totalValues()
                ? getModel().getMaximum() : ((MultiValuedBoundedRangeModel) getModel()).getValue(thumbNumber + 1);

        int newValue = Math.min(Math.max(lowerValue, value), higherValue);

        ((MultiValuedBoundedRangeModel) getModel()).setValue(thumbNumber, newValue);
    }

    private static class MultiValuedBoundedRangeModel extends DefaultBoundedRangeModel {

        private static final long serialVersionUID = 1L;

        private final int[] values;

        private MultiValuedBoundedRangeModel(int noOfValues, int min, int max) {
            super(min, 0, min, max);
            values = new int[noOfValues];
            int prev = 0;
            for (int i = 0; i < values.length; i++) {
                values[i] = (max - min) / values.length + prev;
                prev = values[i];
            }
        }

        public int getValue(int thumbNumber) {
            return values[thumbNumber];
        }

        public void setValue(int thumbNumber, int value) {
            values[thumbNumber] = value;
            fireStateChanged();
        }

        public int totalValues() {
            return values.length;
        }
    }
}
