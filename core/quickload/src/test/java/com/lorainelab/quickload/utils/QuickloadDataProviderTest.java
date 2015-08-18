package com.lorainelab.quickload.utils;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.data.SpeciesInfo;
import com.google.common.base.Strings;
import com.lorainelab.quickload.QuickloadDataProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class QuickloadDataProviderTest {

    private static QuickloadDataProvider dataProvider;
    private static GenomeVersion version;

    @BeforeClass
    public static void setup() throws InterruptedException {
//        dataProvider = new QuickloadDataProvider("http://igbquickload.org/", "igbquickload", 1);
//        Thread.sleep(1000);
//        dataProvider.setMirrorUrl("http://bioviz.org/quickload/");
//        version = new GenomeVersion("A_thaliana_Jun_2009");
//        dataProvider.initialize();
    }

    @Test
    public void validateSpeciesInfo() {
        final Optional<Set<SpeciesInfo>> speciesInfo = dataProvider.getSpeciesInfo();
        assertTrue(speciesInfo.isPresent());
        speciesInfo.ifPresent(infoSet -> {
            infoSet.stream().forEach(info -> {
                assertFalse(Strings.isNullOrEmpty(info.getName()));
                assertFalse(Strings.isNullOrEmpty(info.getCommonName()));
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

}
