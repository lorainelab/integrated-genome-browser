package com.lorainelab.das2;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import static com.lorainelab.das2.utils.Das2ServerUtils.toExternalForm;
import com.lorainelab.igb.synonymlookup.services.DefaultSynonymLookup;

/**
 *
 * @author dcnorris
 */
@Component(name = Das2DataProviderFactory.COMPONENT_NAME, immediate = true)
public class Das2DataProviderFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "Das2DataProviderFactory";
    public static final String FACTORY_NAME = "DAS2";
    private static final int WEIGHT = 3;
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
        Das2DataProvider dasDataProvider = new Das2DataProvider(url, name, loadPriority, defSynonymLookup);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        url = toExternalForm(url.trim());
        Das2DataProvider dasDataProvider = new Das2DataProvider(url, name, mirrorUrl, loadPriority, defSynonymLookup);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return dasDataProvider;
    }

    @Override
    public int getWeight() {
        return WEIGHT;
    }
}
