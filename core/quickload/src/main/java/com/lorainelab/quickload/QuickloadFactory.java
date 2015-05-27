package com.lorainelab.quickload;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.PreferenceUtils;
import static com.lorainelab.quickload.util.QuickloadUtils.toExternalForm;

/**
 *
 * @author dcnorris
 */
@Component(name = QuickloadFactory.COMPONENT_NAME, immediate = true, provide = DataProviderFactory.class)
public class QuickloadFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "QuickloadFactory";
    private static final String FACTORY_NAME = "Quickload";

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
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
