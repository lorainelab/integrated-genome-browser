package com.lorainelab.das;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.PreferenceUtils;
import static com.lorainelab.das.utils.DasServerUtils.toExternalForm;

/**
 *
 * @author dcnorris
 */
@Component(name = DasDataProviderFactory.COMPONENT_NAME, immediate = true)
public class DasDataProviderFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "DasDataProviderFactory";
    public static final String FACTORY_NAME = "DAS";

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority) {
        url = toExternalForm(url.trim());
        DasDataProvider dasDataProvider = new DasDataProvider(url, name, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        url = toExternalForm(url.trim());
        DasDataProvider dasDataProvider = new DasDataProvider(url, name, mirrorUrl, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public int getWeight() {
        return 2;
    }

}
