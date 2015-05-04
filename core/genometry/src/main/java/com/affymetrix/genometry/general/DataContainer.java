package com.affymetrix.genometry.general;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.comparator.StringVersionDateComparator;
import com.affymetrix.genometry.data.DataProvider;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class DataContainer implements Comparable<DataContainer> {

    private final GenomeVersion genomeVersion;
    private final DataProvider dataProvider;
    private final Set<DataSet> dataSets;
    private boolean isInitialized = false;	// is this version initialized?

    /**
     * @param versionID
     * @param name
     * @param gServer -- not null
     * @param versionSourceObj
     */
    public DataContainer(GenomeVersion genomeVersion, DataProvider dataProvider) {
        this.genomeVersion = checkNotNull(genomeVersion);
        this.dataProvider = dataProvider;
        dataSets = Sets.newHashSet();
    }

    public void addDataSet(DataSet f) {
        dataSets.add(f);
    }

    public boolean removeDataSet(DataSet f) {
        dataSets.remove(f);
        return getGenomeVersion().removeSeqsForUri(f.getSymL().uri.toString());
    }

    public void setInitialized() {
        this.isInitialized = true;
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public void setIsInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * Return versions, but don't allow them to be modified.
     */
    public Set<DataSet> getDataSets() {
        return Collections.unmodifiableSet(dataSets);
    }

    @Override
    public String toString() {
        return this.genomeVersion.getName();
    }

    @Override
    public int compareTo(DataContainer other) {
        return new StringVersionDateComparator().compare(this.getName(), other.getName());
    }

    public void clear() {
        dataSets.clear();
    }

    /**
     * @return the genomeVersion
     */
    public GenomeVersion getGenomeVersion() {
        return genomeVersion;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.genomeVersion.getName();
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(genomeVersion).append(dataProvider).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataContainer) == false) {
            return false;
        }
        DataContainer rhs = ((DataContainer) other);
        return new EqualsBuilder()
                .append(getName(), rhs.getName())
                .append(dataProvider, rhs.getDataProvider())
                .append(genomeVersion, rhs.getGenomeVersion())
                .isEquals();
    }

}
