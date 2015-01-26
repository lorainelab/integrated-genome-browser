package com.affymetrix.genometry.general;

/**
 *
 * @author hiralv
 */
public interface ID {

    /**
     * Unique identifier, MUST be unique, and should not be displayed to the
     * users, use getDisplay(). This should help you keep track of different
     * operators. Note that this should be different for each instance.
     *
     * @return a name suitable for identifying this operator.
     */
    public String getName();

    /**
     * user display
     *
     * @return a string suitable for showing a user.
     */
    public String getDisplay();

    /**
     * optional interface for Operators, used to order the list of Operators in
     * the UI
     */
    public static interface Order {

        public int getOrder();
    }
}
