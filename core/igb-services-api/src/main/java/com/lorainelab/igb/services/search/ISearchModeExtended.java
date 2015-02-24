package com.lorainelab.igb.services.search;

/**
 * Interface for search with option box
 *
 * @author hiralv
 */
public interface ISearchModeExtended extends ISearchMode {

    /**
     * @return the name of the option at the specified index
     */
    public String getOptionName();

    /**
     * @return the tooltip of the option at the specified index
     */
    public String getOptionTooltip();

    /**
     * @return if the option at the specified index is enabled
     */
    public boolean getOptionEnable();

    /**
     * Set option state
     */
    public void setOptionState(boolean selected);

    /**
     * @return if option state
     */
    public boolean getOptionState();

    /**
     * @return Action to be associated with button
     */
    public javax.swing.Action getCustomAction();
}
