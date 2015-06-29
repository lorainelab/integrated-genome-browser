package com.lorainelab.das2;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.data.DataProvider;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class Das2DataProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(Das2DataProviderTest.class);
    public static String DAS2_URL = "http://bioserver.hci.utah.edu:8080/DAS2DB";

    @Test
    @Ignore
    public void typeRequestTest() {
        DataProvider dataProvider = new Das2DataProvider(DAS2_URL, "HCI_UTAH", 1);
        GenomeVersion genomeVersion = new GenomeVersion("H_sapiens_Feb_2009");
        DataContainer container = new DataContainer(genomeVersion, dataProvider);
        Set<DataSet> availableDataSets = dataProvider.getAvailableDataSets(container);
        availableDataSets.stream().forEach(ds -> {
            logger.info(ds.getURI().toString());
        });
    }
}
