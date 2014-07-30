package com.affymetrix.igb.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.google.common.io.Closer;
import java.awt.image.BufferedImage;

/**
 * Helper class for getting genomic images from ENSEMBL The mappings for ensembl
 * are defined in ensemblURLs tab delimited text file
 *
 * @author Ido M. Tamir
 */
class ENSEMBLoader extends BrowserLoader {

    private final Map<String, EnsemblURL> urlMap;

    public ENSEMBLoader() {
        urlMap = loadMap();
    }

    public Map<String, EnsemblURL> loadMap() {
        Map<String, EnsemblURL> urlMap = new HashMap<String, EnsemblURL>();
        String urlfile = "/ensemblURLs";
        InputStream file_input_str
                = ExternalViewer.class.getResourceAsStream(urlfile);

        if (file_input_str == null) {
            ErrorHandler.errorPanel(ExternalViewer.BUNDLE.getString("emsembleFileErrorTitle"),
                    MessageFormat.format(ExternalViewer.BUNDLE.getString("emsembleFileError"), urlfile));
        }
        BufferedReader d = null;

        if (file_input_str != null) {
            try {

                d = new BufferedReader(new InputStreamReader(file_input_str));
                StringTokenizer string_toks;
                String ucscName, ensemblURL;
                String line = "";
                while ((line = d.readLine()) != null) {
                    if (!line.startsWith("#") && line.trim().length() > 0) {
                        string_toks = new StringTokenizer(line);
                        ucscName = string_toks.nextToken();
                        ensemblURL = string_toks.nextToken();
                        urlMap.put(ucscName, new EnsemblURL(ensemblURL));
                    }
                }
            } catch (Exception ex) {
                //log error
            } finally {
                GeneralUtils.safeClose(d);
                GeneralUtils.safeClose(file_input_str);
            }
        }
        return urlMap;
    }

    public String url(Loc loc) {
        EnsemblURL url = urlMap.get(loc.db);
        if (url == null) {
            return "";
        } else {
            return url.url;
        }
    }

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
        Logger.getLogger(ENSEMBLoader.class.getName()).log(Level.FINE, "url was : {0}", url);
        return url;
    }

    /**
     *
     * @param query comes from the IGB as UCSC query string
     * @param pixWidth
     * @param cookies
     * @return
     */
    @Override
    public BufferedImage getImage(Loc loc, int pixWidth, Map<String, String> cookies) throws ImageUnavailableException {
        String url;
        Closer closer = Closer.create();
        url = getUrlForView(loc, pixWidth);
        if (url.startsWith("http")) {
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
                logger.log(Level.WARNING, "IOException.", e);  
                throw new ImageUnavailableException();
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
