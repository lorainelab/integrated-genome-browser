package com.lorainelab.das2;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import com.affymetrix.genometry.util.PreferenceUtils;
import static com.lorainelab.das2.utils.Das2ServerUtils.toExternalForm;

/**
 *
 * @author dcnorris
 */
@Component(name = Das2DataProviderFactory.COMPONENT_NAME, immediate = true)
public class Das2DataProviderFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "Das2DataProviderFactory";
    public static final String FACTORY_NAME = "DAS2";

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority, boolean editable) {
        url = toExternalForm(url.trim());
        Das2DataProvider dasDataProvider = new Das2DataProvider(url, name, loadPriority, editable);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority, boolean editable) {
        url = toExternalForm(url.trim());
        Das2DataProvider dasDataProvider = new Das2DataProvider(url, name, mirrorUrl, loadPriority, editable);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }
}
