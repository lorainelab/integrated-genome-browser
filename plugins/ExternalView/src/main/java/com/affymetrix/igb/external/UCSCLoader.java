package com.affymetrix.igb.external;

import static com.affymetrix.genometry.symloader.ProtocolConstants.HTTP_PROTOCOL_SCHEME;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Returns location image string from the UCSC genome browser.
 *
 *
 * @author Ido M. Tamir
 */
public class UCSCLoader extends BrowserLoader {

    private static final Pattern fileNamePattern = Pattern.compile("(hgt_genome.*png)");
    private static final Pattern sideNamePattern = Pattern.compile("(side_genome.*png)");

    private static final String fileNameBaseUrl = "http://genome.ucsc.edu/trash/hgt/";
    private static final String sideNameBaseUrl = "http://genome.ucsc.edu/trash/hgtSide/";

    public String getUrlForView(Loc loc, int pixWidth) {
        String width = "pix=" + pixWidth + "&";
        return "http://genome.ucsc.edu/cgi-bin/hgTracks?" + width + "db=" + loc.db + "&position=" + loc.chr + ":" + loc.start + "-" + loc.end;
    }

    public BufferedImage getImage(Loc loc, int pixWidth, Map<String, String> cookies) throws ImageUnavailableException {
        String base_url = getUrlForView(loc, pixWidth);
        String userId = cookies.get(UCSCView.UCSCUSERID).equals("") ? null : UCSCView.UCSCUSERID + "=" + cookies.get(UCSCView.UCSCUSERID);
        String file_name_url = getImageUrl(base_url, userId, new UCSCURLFinder(fileNameBaseUrl, fileNamePattern));
        if (file_name_url.startsWith(HTTP_PROTOCOL_SCHEME)) {
            BufferedImage finalImage = null;
            BufferedImage fileImage = null;
            try {
                fileImage = ImageIO.read(new URL(file_name_url));
                finalImage = fileImage;
            } catch (IOException e) {
                Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "url was : " + file_name_url, e);
            }
            String side_name_url = getImageUrl(base_url,userId, new UCSCURLFinder(sideNameBaseUrl, sideNamePattern));
            if (fileImage != null && side_name_url.startsWith(HTTP_PROTOCOL_SCHEME)) {
                BufferedImage sideImage = null;
                try {
                    sideImage = ImageIO.read(new URL(side_name_url));
                } catch (IOException e) {
                    Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "url was : " + side_name_url, e);
                }
                if (sideImage != null) {
                    BufferedImage tempImage = new BufferedImage(fileImage.getWidth(), fileImage.getHeight(), fileImage.getType());
                    Graphics graphics = tempImage.getGraphics();
                    graphics.drawImage(sideImage, 0, 0, sideImage.getWidth(), sideImage.getHeight(), null);
                    graphics.drawImage(fileImage, 0, 0, fileImage.getWidth(), fileImage.getHeight(), null);
                    finalImage = tempImage;
                }
            }
            return finalImage;
        }

        throw new ImageUnavailableException();
    }

    class UCSCURLFinder implements URLFinder {

        final String base_url;
        final Pattern pattern;

        UCSCURLFinder(String base_url, Pattern pattern) {
            this.base_url = base_url;
            this.pattern = pattern;
        }

        public String findUrl(BufferedReader reader, URL redirectedUrl) throws IOException, ImageUnavailableException {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                Matcher m = pattern.matcher(inputLine);
                if (m.find() && m.groupCount() == 1) {
                    Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName {0}", inputLine);
                    String fileName = m.group(1);
                    return base_url + fileName;
                }
            }
            throw new ImageUnavailableException();
        }
    }
}
