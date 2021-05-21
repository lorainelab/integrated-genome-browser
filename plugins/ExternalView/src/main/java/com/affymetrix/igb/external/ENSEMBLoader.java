package com.affymetrix.igb.external;

import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import com.google.common.io.Closer;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for getting genomic images from ENSEMBL. The mappings for ensembl
 * are defined in ensemblURLs tab delimited text file
 *
 * @author Ido M. Tamir
 */
class ENSEMBLoader extends BrowserLoader {

    private Map<String, EnsemblURL> urlMap = null;
    private static final Logger LOG = LoggerFactory.getLogger(ENSEMBLoader.class);

    public ENSEMBLoader() {
        try {
            urlMap = loadMap();
        } catch (IOException ex) {
              LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * Read a space-delimited file that maps UCSC genome version names onto their
     * corresponding Ensembl genome browser URLs.
     * 
     * @return a Map object that maps UCSC genome names onto their corresponding
     * base URLs for accessing the same genome in the Ensembl genome browser.
     * 
     * @throws IOException 
     */
    public static Map<String, EnsemblURL> loadMap() throws IOException {
        Map<String, EnsemblURL> urlMap = new HashMap<>();
        String urlfile = "/ensemblURLs";
        InputStream file_input_str
                = ExternalViewer.class.getResourceAsStream(urlfile);
        BufferedReader d = new BufferedReader(new InputStreamReader(file_input_str));
        StringTokenizer string_toks;
        String ucscName, ensemblURL, line;
        while ((line=d.readLine())!=null) {
            // skip commented lines or lines with only whitespace
            if (line.startsWith("#") || line.trim().length() == 0) {
                continue;
            }
            string_toks = new StringTokenizer(line);
            ucscName = string_toks.nextToken();
            ensemblURL = string_toks.nextToken();
            urlMap.put(ucscName, new EnsemblURL(ensemblURL));
        }
        return urlMap;
    }

    /**
     * Utility method that obtains the base URL for a genome. If the genome
     * is not represented in the genome mapping file, returns empty String.
     * 
     * @param loc - a Location in the genome being viewed by the user
     * @return url - String with URL prefix for the given Location's genome or empty String.
     */
    public String url(Loc loc) {
        EnsemblURL url = urlMap.get(loc.db);
        if (url == null) {
            return "";
        } else {
            return url.url;
        }
    }
    
    /**
     * Create a URL String required to access an image showing the given Location
     * from the Ensembl genome browser. 
     * 
     * @param loc - Location in the genome being viewed in IGB right now
     * @param pixWidth - The desired width in pixels of the image that will be displayed
     * @return 
     */
    public String getUrlForView(Loc loc, int pixWidth) {
        if (!urlMap.containsKey(loc.db)) {
            return MessageFormat.format(ExternalViewer.BUNDLE.getString("ensemblTransposeError"), loc.db);
        }
        if (loc.length() >= 100000) {
            return ExternalViewer.BUNDLE.getString("regionTooLargeError");
        }
        String chr = loc.chr.replaceAll("chr", "");
        String url = urlMap.get(loc.db).url + "/Component/Location/Web/ViewBottom?r=" + chr + ":" + (loc.start + 1) + "-" + loc.end; //ensembl = 1 based
        url = url + ";image_width=" + pixWidth + ";export=png";
        LOG.info("Made URL string: {0}", url);
        return url;
    }

    /**
     * Obtain a BufferedImage that IGB will display in the External View tabbed panel
     * interface.
     * 
     * @param loc Location the user would like to view. 
     * @param pixWidth Width of the image to be obtained from Ensembl.
     * @param cookies 
     * @return
     */
    @Override
    public BufferedImage getImage(Loc loc, int pixWidth, Map<String, String> cookies) throws ImageUnavailableException {
        String url;
        Closer closer = Closer.create();
        url = getUrlForView(loc, pixWidth);
        if (url.startsWith(HTTP_PROTOCOL_SCHEME)) {
            try {
                String cookie = EnsemblView.ENSEMBLWIDTH + "=" + cookies.get(EnsemblView.ENSEMBLWIDTH);
                String session = cookies.get(EnsemblView.ENSEMBLSESSION);
                if (session != null && session.length() != 0) {
                    cookie += ";" + EnsemblView.ENSEMBLSESSION + "=" + cookies.get(EnsemblView.ENSEMBLSESSION);
                }
                InputStream in = getConnection(url, cookie).getInputStream();
                closer.register(in);
                return ImageIO.read(in);
            } catch (IOException e) {
                LOG.error("IOException on opening: "+url, e);
                throw new ImageUnavailableException("IOException on opening: "+url);
            }
        }
        throw new ImageUnavailableException(url);
    }
}

class EnsemblURL {

    final String url;

    EnsemblURL(String url) {
        this.url = url;
    }
}
