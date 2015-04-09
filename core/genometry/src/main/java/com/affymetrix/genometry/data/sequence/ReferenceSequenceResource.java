package com.affymetrix.genometry.data.sequence;

/**
 *
 * @author dcnorris
 */
public interface ReferenceSequenceResource {

    /**
     * @return the name of the ReferenceSequenceResource
     */
    public String getName();

    /**
     * @param name which will be displayed to users to identify this ReferenceSequenceResource
     */
    public void setName(String name);

    /**
     * Load priority will be used to determine the order
     * in which to query ReferenceSequenceResource instances.
     * Users will be able to override the value returned here.
     *
     * @return Returns the default load priority
     */
    public int getLoadPriority();

    /**
     * sets the load priority
     *
     * @param loadPriority
     */
    public void setLoadPriority(int loadPriority);
}
