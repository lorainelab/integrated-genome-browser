package com.lorainelab.das;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import static com.lorainelab.das.utils.DasServerUtils.toExternalForm;
import com.lorainelab.synonymlookup.services.DefaultSynonymLookup;

/**
 *
 * @author dcnorris
 */
@Component(name = DasDataProviderFactory.COMPONENT_NAME, immediate = true)
public class DasDataProviderFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "DasDataProviderFactory";
    public static final String FACTORY_NAME = "DAS";
    private static final int WEIGHT = 2;
    private DefaultSynonymLookup defSynonymLookup;

    @Reference
    public void setDefSynonymLookup(DefaultSynonymLookup defSynonymLookup) {
        this.defSynonymLookup = defSynonymLookup;
    }

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority) {
        url = toExternalForm(url.trim());
        DasDataProvider dasDataProvider = new DasDataProvider(url, name, loadPriority, defSynonymLookup);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        url = toExternalForm(url.trim());
        DasDataProvider dasDataProvider = new DasDataProvider(url, name, mirrorUrl, loadPriority, defSynonymLookup);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public int getWeight() {
        return WEIGHT;
    }

}
