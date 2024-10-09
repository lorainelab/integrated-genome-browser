package org.lorainelab.igb.ensembl.rest.api.service;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.data.DataProviderFactory;
import com.affymetrix.genometry.general.DataProviderPrefKeys;
import org.osgi.service.component.annotations.Component;

import static org.lorainelab.igb.ensembl.rest.api.service.utils.EnsemblRestServerUtils.toExternalForm;


@Component(name = EnsemblRestDataProviderFactory.COMPONENT_NAME, immediate = true)
public class EnsemblRestDataProviderFactory implements DataProviderFactory {

    public static final String COMPONENT_NAME = "EnsemblRestApiDataProviderFactory";
    public static final String FACTORY_NAME = "Ensembl REST";
    private static final int WEIGHT = 4;

    @Override
    public String getFactoryName() {
        return FACTORY_NAME;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority) {
        url = toExternalForm(url.trim());
        EnsemblRestApiDataProvider ensemblRestApiDataProvider = new EnsemblRestApiDataProvider(url, name, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return ensemblRestApiDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority) {
        url = toExternalForm(url.trim());
        EnsemblRestApiDataProvider ensemblRestApiDataProvider = new EnsemblRestApiDataProvider(url, name, mirrorUrl, loadPriority);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return ensemblRestApiDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, int loadPriority, String id) {
        url = toExternalForm(url.trim());
        EnsemblRestApiDataProvider ensemblRestApiDataProvider = new EnsemblRestApiDataProvider(url, name, loadPriority, id);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return ensemblRestApiDataProvider;
    }

    @Override
    public DataProvider createDataProvider(String url, String name, String mirrorUrl, int loadPriority, String id) {
        url = toExternalForm(url.trim());
        EnsemblRestApiDataProvider ensemblRestApiDataProvider = new EnsemblRestApiDataProvider(url, name, mirrorUrl, loadPriority, id);
        PreferenceUtils.getDataProviderNode(url).put(DataProviderPrefKeys.FACTORY_NAME, FACTORY_NAME);
        return ensemblRestApiDataProvider;
    }
    
    @Override
    public int getWeight() {
        return WEIGHT;
    }
}

