package org.lorainelab.igb.quickload;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import static org.lorainelab.igb.quickload.util.QuickloadUtils.toExternalForm;

/**
 *
 * @author dcnorris
 */
@Component(name = QuickloadFactory.COMPONENT_NAME, immediate = true, provide = DataProviderFactory.class)
public class QuickloadFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "QuickloadFactory";
    private static final String FACTORY_NAME = "Quickload";
    private static final int WEIGHT = 1;

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }

    @Override
    public int getWeight() {
        return WEIGHT;
    }

    @Override
    public boolean supportsLocalFileInstances() {
        return true;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority) {
        url = toExternalForm(url);
        QuickloadDataProvider quickloadDataProvider = new QuickloadDataProvider(url, name, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return quickloadDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        url = toExternalForm(url);

        QuickloadDataProvider quickloadDataProvider = new QuickloadDataProvider(url, name, mirrorUrl, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return quickloadDataProvider;
    }

}
