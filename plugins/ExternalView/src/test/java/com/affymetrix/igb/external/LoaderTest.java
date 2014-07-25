package com.affymetrix.igb.external;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import static junit.framework.Assert.assertNotNull;
import org.junit.Ignore;
import org.junit.Test;

public class LoaderTest {

    @Ignore
    @Test
    public void checkDownLoadEnsembl() throws ImageUnavailableException {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put(EnsemblView.ENSEMBLSESSION, "");
        cookies.put(EnsemblView.ENSEMBLWIDTH, "800");
        Loc loc = new Loc("hg19", "chr1", 6203693, 6206373);
        BufferedImage image = new ENSEMBLoader().getImage(loc, 800, cookies);
        assertNotNull(image);
    }

    
    @Test
    public void checkDownLoadUCSC() throws ImageUnavailableException {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put(UCSCView.UCSCUSERID, "");
        Loc loc = new Loc("hg19", "chr1", 6203693, 6206373);
        BufferedImage image = new UCSCLoader().getImage(loc, 800, cookies);
        assertNotNull(image);
    }

}
