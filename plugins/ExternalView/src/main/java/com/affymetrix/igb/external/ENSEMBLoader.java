package com.affymetrix.igb.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.image.BufferedImage;
import java.rmi.server.ExportException;

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
            return MessageFormat.format(ExternalViewer.BUNDLE.getString("transposeError"), loc.db);
        }
        if (loc.length() >= 100000) {
            return ExternalViewer.BUNDLE.getString("regionTooLargeError");
        }
        String chr = loc.chr.replaceAll("chr", "");
        String url = urlMap.get(loc.db).url + "/Location/View?r=" + chr + ":" + (loc.start + 1) + "-" + loc.end; //ensembl = 1 based
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
        String url = "";
        try {
            url = getUrlForView(loc, pixWidth);
        } catch (Exception e) {
            url = MessageFormat.format(ExternalViewer.BUNDLE.getString("translateUCSCEnsembleError"), loc);
        }
        if (url.startsWith("http")) {
            String cookie = EnsemblView.ENSEMBLWIDTH + "=" + cookies.get(EnsemblView.ENSEMBLWIDTH);
            String session = cookies.get(EnsemblView.ENSEMBLSESSION);
            if (session != null && session.length() != 0) {
                cookie += ";" + EnsemblView.ENSEMBLSESSION + "=" + cookies.get(EnsemblView.ENSEMBLSESSION);
            }
            url = getImageUrl(url, cookie, new ENSEMBLURLFinder());
            if (url.startsWith("http")) {
                try {
                    return ImageIO.read(new URL(url));
                } catch (IOException e) {
                    Logger.getLogger(BrowserView.class.getName()).log(Level.FINE, "url was : " + url, e);
                }
            }
        }
        throw new ImageUnavailableException();
    }
}

/**
 * Extracts the image url from the returned html page. ENSEMBL likes to change
 * the ids of the elments quite often e.g. sep2009 id = "BottomViewPanel" ->
 * may2010 id ="contigviewbottom"
 *
 * the panelPattern could be part of the ensemblurl and passed into the
 * constructor to be more flexible and allow other ensembl versions
 *
 */
class ENSEMBLURLFinder implements URLFinder {

    private final static Pattern panelPattern = Pattern.compile("id=\"contigviewbottom\"");
    private final static Pattern imagePattern = Pattern.compile("img-tmp(.*png)");

    @Override
    public String findUrl(BufferedReader reader, URL redirectedURL) throws IOException, ImageUnavailableException {
        String inputLine = "";
        boolean panel = false;
        while ((inputLine = reader.readLine()) != null) {
            if (!panel) {
                Matcher m = panelPattern.matcher(inputLine);
                panel = m.find();
            } else {
                Matcher m = imagePattern.matcher(inputLine);
                if (m.find()) {
                    Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName {0}", inputLine);
                    String fileName = m.group(1);
                    return "http://" + redirectedURL.getHost() + "/img-tmp" + fileName;
                }
            }
        }
        throw new ImageUnavailableException();
    }
}

class EnsemblURL {

    final String url;

    EnsemblURL(String url) {
        this.url = url;
    }
}
