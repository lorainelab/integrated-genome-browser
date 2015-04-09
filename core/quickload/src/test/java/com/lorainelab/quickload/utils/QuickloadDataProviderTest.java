package com.lorainelab.quickload.utils;

import com.affymetrix.genometry.data.SpeciesInfo;
import com.affymetrix.genometry.general.GenomeVersion;
import com.google.common.base.Strings;
import com.lorainelab.quickload.QuickloadDataProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class QuickloadDataProviderTest {

    private QuickloadDataProvider dataProvider;
    private GenomeVersion version;

    @Before
    public void setup() {
        dataProvider = new QuickloadDataProvider("http://igbquickload.org/", "igbquickload");
        dataProvider.setMirrorUrl("http://bioviz.org/quickload/");
        version = new GenomeVersion(null, null, "A_thaliana_Jun_2009", null, null);
    }

    @Test
    public void validateSpeciesInfo() {
        final Optional<Set<SpeciesInfo>> speciesInfo = dataProvider.getSpeciesInfo();
        assertTrue(speciesInfo.isPresent());
        speciesInfo.ifPresent(infoSet -> {
            infoSet.stream().forEach(info -> {
                assertFalse(Strings.isNullOrEmpty(info.getName()));
                assertFalse(Strings.isNullOrEmpty(info.getCommonName().get()));
                assertFalse(Strings.isNullOrEmpty(info.getGenomeVersionNamePrefix()));
            });
        });
    }

    @Test
    public void validateSupportedGenomeVersionNames() {
        final Set<String> supportedGenomeVersionNames = dataProvider.getSupportedGenomeVersionNames();
        assertTrue(supportedGenomeVersionNames.contains("A_thaliana_Jun_2009"));
        assertTrue(supportedGenomeVersionNames.contains("H_sapiens_Dec_2013"));
    }

    @Test
    public void validateGenomeVersionDataUrls() {
        final Set<String> availableDataSetUrls = dataProvider.getAvailableDataSetUrls(version);
        assertTrue(availableDataSetUrls.contains("http://igbquickload.org/A_thaliana_Jun_2009/TAIR10.bed.gz"));
        assertTrue(availableDataSetUrls.contains("http://igbquickload.org/A_thaliana_Jun_2009/cold_stress/cold_treatment.mm.bam"));
    }

    @Test
    public void testAssemblyInfo() {
        Map<String, Integer> assemblyInfo = dataProvider.getAssemblyInfo(version);
        assertTrue(assemblyInfo.containsKey("chr5"));
        assertTrue(assemblyInfo.containsKey("chrM"));
    }

    @Test
    public void checkMirrorUrl() {
        assertTrue(dataProvider.getMirrorUrl().isPresent());
        assertTrue(dataProvider.getMirrorUrl().get().equals("http://bioviz.org/quickload/"));
    }

    @Test
    public void checkResidueFileUrl() {
        Assert.assertEquals("http://igbquickload.org/A_thaliana_Jun_2009/A_thaliana_Jun_2009.2bit", dataProvider.getSequenceFileUrl(version));
    }
}
