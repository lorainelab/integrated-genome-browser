package org.lorainelab.igb.quickload.utils;

import com.affymetrix.genometry.GenomeVersion;
import com.google.common.base.Strings;
import org.lorainelab.igb.quickload.QuickloadDataProvider;
import org.lorainelab.igb.synonymlookup.services.SpeciesInfo;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;


/**
 *
 * @author dcnorris
 */
public class QuickloadDataProviderTest {

    private static QuickloadDataProvider dataProvider;
    private static GenomeVersion version;

    @BeforeAll
    public static void setup() throws InterruptedException, MalformedURLException {
        GenomeVersion genomeVersion = new GenomeVersion("Quickload sample");
        ClassLoader classLoader = QuickloadDataProviderTest.class.getClassLoader();
        File file = new File(classLoader.getResource("quickload").getFile());
        dataProvider = new QuickloadDataProvider(file.toURI().toURL().toString(),"", 1);
        dataProvider.setMirrorUrl(file.toURI().toURL().toString());
        version = new GenomeVersion("A_thaliana_Jun_2009");
        dataProvider.initialize();
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
        //JDaly - Changed from "chr5" in IGBF-1142 to match output from AssemblyInfo
        assertTrue(assemblyInfo.containsKey("Chr5"));
        assertTrue(assemblyInfo.containsKey("ChrM"));
    }

    @Test
    public void checkMirrorUrl() {
        assertTrue(dataProvider.getMirrorUrl().isPresent());
    }

    @Test
    public void validateQuickload(){
        final Set<String> supportedGenomeVersionNames = dataProvider.getSupportedGenomeVersionNames();
        ClassLoader classLoader = QuickloadDataProviderTest.class.getClassLoader();
        File file = new File(classLoader.getResource("quickload").getFile());
        File[] f = file.listFiles();
        List<String> folders =new ArrayList<>();
        for (File dir: f) {
            if(dir.isDirectory()){
                folders.add(dir.getName());
            }
        }
        assertEquals(supportedGenomeVersionNames.size(),folders.size());
        for (String genomeVersionName: supportedGenomeVersionNames) {
            assertTrue(folders.contains(genomeVersionName));
        }
    }

}
