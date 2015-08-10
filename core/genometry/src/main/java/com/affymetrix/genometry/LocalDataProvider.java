package com.affymetrix.genometry;

import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.LoadUtils;
import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class LocalDataProvider implements DataProvider {

    private static final Logger logger = LoggerFactory.getLogger(LocalDataProvider.class);
    private static final String PROVIDER_NAME = "Local Files";
    private boolean containsReferenceSequenceData;

    public LocalDataProvider() {
        containsReferenceSequenceData = false;
    }

    @Override
    public LinkedHashSet<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        return Sets.newLinkedHashSet();
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public void initialize() {
    }

    @Override
    public int getLoadPriority() {
        return -1;
    }

    @Override
    public void setLoadPriority(int loadPriority) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setMirrorUrl(String mirrorUrl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean useMirrorUrl() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LoadUtils.ResourceStatus getStatus() {
        return LoadUtils.ResourceStatus.Initialized;
    }

    @Override
    public void setStatus(LoadUtils.ResourceStatus serverStatus) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Optional<String> getLogin() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLogin(String login) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Optional<String> getPassword() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPassword(String password) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean isContainsReferenceSequenceData() {
        return containsReferenceSequenceData;
    }

    public void setContainsReferenceSequenceData(boolean containsReferenceSequenceData) {
        this.containsReferenceSequenceData = containsReferenceSequenceData;
    }

}
