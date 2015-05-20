package com.lorainelab.das;

import com.affymetrix.genometry.data.BaseDataProvider;
import com.affymetrix.genometry.data.DataSetProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import static com.affymetrix.genometry.util.LoadUtils.ResourceStatus.Disabled;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DasDataProvider extends BaseDataProvider implements DataSetProvider {

    private static final Logger logger = LoggerFactory.getLogger(DasDataProvider.class);
    DasServerInfo dasServerInfo;

    public DasDataProvider(String dasUrl, String name, int loadPriority) {
        super(dasUrl, name, loadPriority);
        try {
            URL url = new URL(dasUrl);
            dasServerInfo = new DasServerInfo(url);
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage(), ex);
            setStatus(Disabled);
        }
        if (status != Disabled) {
            initialize();
        }
    }

    @Override
    protected void disable() {

    }

    @Override
    public Set<String> getSupportedGenomeVersionNames() {
        return dasServerInfo.getDataSources().keySet();
    }

    @Override
    public void initialize() {

    }

    @Override
    public LinkedHashSet<DataSet> getAvailableDataSets(DataContainer dataContainer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
