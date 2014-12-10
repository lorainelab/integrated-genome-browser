package com.affymetrix.genometryImpl.util;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.swing.ImageIcon;

import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOImage;
import net.sf.samtools.util.BlockCompressedInputStream;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import static com.google.common.io.Closeables.close;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.net.ServerSocket;
import java.util.ArrayList;
import net.sf.samtools.seekablestream.SeekableStream;
import net.sf.samtools.seekablestream.SeekableStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.LoggerFactory;

public final class GeneralUtils {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GeneralUtils.class);
    public static final String UTF8 = "UTF-8";
    private static final Pattern CLEAN = Pattern.compile("[/\\s+]");

    /**
     * Safely close a Closeable object. If it doesn't exist, return.
     */
    public static <S extends Closeable> void safeClose(S s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch (Exception ex) {
            logger.error(null, ex);
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                close(closeable, true);
            } catch (IOException ex) {
                logger.error(null, ex);
            }
        }
    }

    public static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.error(null, ex);
            }
        }
    }

    /**
     * A list of all the compression-type file endings that this object knows
     * how to decompress. This list is all lower-case, but should be treated as
     * case-insensitive.
     */
    public static final String[] compression_endings
            = {".z", ".gzip", ".gz", ".zip", ".bz2", ".tar"};

    /**
     * Returns the file name with all {@link #compression_endings} stripped-off.
     */
    public static String stripEndings(String name) {
        for (int i = 0; i < compression_endings.length; i++) {
            String ending = compression_endings[i].toLowerCase();
            if (name.toLowerCase().endsWith(ending)) {
                String stripped_name = name.substring(0, name.lastIndexOf('.'));
                return stripEndings(stripped_name);
            }
        }
        return name;
    }

    /**
     * Returns a BufferedInputStream, possibly wrapped by a GZIPInputStream, or
     * ZipInputStream, as appropriate based on the name of the given file.
     *
     * @param f a file
     * @param sb a StringBuffer used to pass back the name of the file with the
     * compression endings (like ".zip") removed, and converted to lower case.
     */
    public static InputStream getInputStream(File f, StringBuffer sb) throws
            FileNotFoundException, IOException {

        String infile_name = "file:" + f.getAbsolutePath();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        InputStream isr = unzipStream(bis, infile_name, sb);
        return isr;
    }

    /**
     * Takes a named input stream and returns another one which is an instance
     * of GZIPInputStream or ZipInputStream if the given name ends with one of
     * the {@link #compression_endings} (case insensitive). (If the stream name
     * does not have one of those endings, the original InputStream is simply
     * returned unchanged.) The name with the compression ending stripped off
     * (and converted to lower case) is returned in the value of stripped_name.
     */
    public static InputStream unzipStream(InputStream istr, String stream_name,
            StringBuffer stripped_name)
            throws IOException {
        String lc_stream_name = stream_name.toLowerCase();
        if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip")
                || lc_stream_name.endsWith(".z")) {
            InputStream gzstr = getGZipInputStream(stream_name, istr);
            String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
            return unzipStream(gzstr, new_name, stripped_name);
        } else if (lc_stream_name.endsWith(".zip")) {
            ZipInputStream zstr = new ZipInputStream(istr);
            zstr.getNextEntry();
            String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
            return unzipStream(zstr, new_name, stripped_name);
        } else if (lc_stream_name.endsWith(".bz2")) {
            BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(istr);
            String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
            return unzipStream(bz2, new_name, stripped_name);
        } else if (lc_stream_name.endsWith(".tar")) {
            TarArchiveInputStream tarInput = new TarArchiveInputStream(istr);
            tarInput.getNextTarEntry();
            String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
            return unzipStream(tarInput, new_name, stripped_name);
        }
        stripped_name.append(stream_name);
        return istr;
    }

    public static String getUnzippedName(String stream_name) {
        String lc_stream_name = stream_name.toLowerCase();
        if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip")
                || lc_stream_name.endsWith(".z")) {
            return stream_name.substring(0, stream_name.lastIndexOf('.'));
        } else if (stream_name.endsWith(".zip")) {
            return stream_name.substring(0, stream_name.lastIndexOf('.'));
        } else if (stream_name.endsWith(".bz2")) {
            return stream_name.substring(0, stream_name.lastIndexOf('.'));
        } else if (stream_name.endsWith(".tar")) {
            return stream_name.substring(0, stream_name.lastIndexOf('.'));
        }
        return stream_name;
    }

    /**
     * Fix several potential problems in URL names.
     */
    public static String convertStreamNameToValidURLName(String streamName) {
        int httpIndex = streamName.indexOf("http:");
        if (httpIndex > -1) {
            streamName = streamName.substring(httpIndex + 5);	// strip off initial characters including http:
        }

        // strip off initial "/" characters.  There may be one, or multiple.
        int streamNameLen = streamName.length();
        for (int i = 0; i < streamNameLen; i++) {
            if (streamName.startsWith("/")) {
                streamName = streamName.substring(1);
            }
        }

        // strip off final "/" character, if it exists.
        if (streamName.endsWith("/")) {
            streamName = streamName.substring(0, streamName.length() - 1);
        }
        return "http://" + streamName;

    }

    /**
     * @param istr	- input stream
     * @param streamName
     * @return File with data from stream.
     */
    public static File convertStreamToFile(InputStream istr, String streamName) {
        // Output the InputStream to a temporary file, and read that as a FileInputStream.
        OutputStream out = null;
        try {
            String unzippedStreamName = stripEndings(streamName);
            String extension = getExtension(unzippedStreamName);
            //watch out for spaces and / in the stream name
            File f = File.createTempFile(CLEAN.matcher(unzippedStreamName).replaceAll("_"), extension);
            f.deleteOnExit();	// This is only a temporary file!  Delete when the app exits.
            out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = istr.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            return f;
        } catch (IOException ex) {
            logger.error(null, ex);
            return null;
        } finally {
            GeneralUtils.safeClose(out);
        }
    }

    /**
     * Get a favicon from the URL.
     *
     * @param iconString
     * @return null
     */
    public static ImageIcon determineFriendlyIcon(String iconString) {
        // Step 1. getting IconURL
        URL iconURL = null;
        try {
            iconURL = new URL(iconString);
        } catch (Exception ex) {
            // Ignore an exception here, since this is only for making a pretty UI.
        }
        if (iconURL == null) {
            return null;
        }

        // Step 2. loading the icon and find a proper icon
        BufferedImage icon = null;
        URLConnection conn = null;
        List<ICOImage> icoImages = null;
        try {
            conn = iconURL.openConnection();
            conn.setConnectTimeout(5000);	// only wait a few seconds, since this isn't critical
            conn.setReadTimeout(5000);		// only wait a few seconds, since this isn't critical
            conn.connect();
            if (conn.getInputStream() == null) {
                return null;
            }
            icoImages = ICODecoder.readExt(conn.getInputStream());
        } catch (Exception ex) {
            return null;
        }

        if (icoImages == null) {
            return null;
        }
        int maxColorDepth = 0;
        for (ICOImage icoImage : icoImages) {
            int colorDepth = icoImage.getColourDepth();
            int width = icoImage.getWidth();
            if (width == 16 && maxColorDepth < colorDepth) {
                icon = icoImage.getImage();
                maxColorDepth = colorDepth;
            }
        }
        if (icon == null && !icoImages.isEmpty()) {
            icon = icoImages.get(0).getImage();
        }

        // step 3. create the imageIcon instance
        ImageIcon friendlyIcon = null;
        try {
            if (icon != null) {
                friendlyIcon = new ImageIcon(icon);
            }
        } catch (Exception ex) {
            // Ignore an exception here, since this is only for making a pretty UI.
        }
        return friendlyIcon;
    }

    public static String URLEncode(String s) {
        try {
            return URLEncoder.encode(s, UTF8);
        } catch (UnsupportedEncodingException ex) {
            logger.error( null, ex);
            throw new IllegalArgumentException(ex);
        }
    }

    public static String URLDecode(String s) {
        try {
            return URLDecoder.decode(s, UTF8);
        } catch (UnsupportedEncodingException ex) {
            logger.error( null, ex);
            throw new IllegalArgumentException(ex);
        }
    }

    public static void browse(String s) {
        try {
            URI u = new URI(s);

            if ("file".equalsIgnoreCase(u.getScheme())) {
                Desktop.getDesktop().open(new File(u));
                return;
            }

            Desktop.getDesktop().browse(u);
        } catch (IOException ex) {
            logger.error( null, ex);
        } catch (URISyntaxException ex) {
            logger.error( null, ex);
        } catch (IllegalArgumentException ex) {
            ErrorHandler.errorPanel("Invalid address or path for information page:\n\n" + s);
        }
    }

    /**
     * Write contents of file to output stream.
     *
     * @param f
     * @param dos
     */
    public static void writeFileToStream(File f, OutputStream dos) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(f);
            byte[] buffer = new byte[4096]; // tweaking this number may increase performance
            int len;
            while ((len = is.read(buffer)) != -1) {
                dos.write(buffer, 0, len);
            }
        } catch (Exception ex) {
            logger.error( null, ex);
        } finally {
            GeneralUtils.safeClose(is);

        }
    }

    public static GenericFeature findFeatureWithURI(Collection<GenericFeature> features, URI uri) {
        if (uri == null || features.isEmpty()) {
            return null;
        }

        for (GenericFeature feature : features) {
            if (uri.equals(feature.getURI())) {
                return feature;
            }
        }

        return null;	// couldn't find it
    }

    /**
     * Moves mapping to the given path and renames it to filename.
     *
     * @param file	File to be moved.
     * @param fileName	File name to be given to moved mapping.
     * @param path	Path to where mapping is moved.
     */
    public static boolean moveFileTo(File file, String fileName, String path) {
        File newLocation = new File(path + "/" + fileName);
        boolean sucess = file.renameTo(newLocation);

        if (!sucess) {
            logger.error( "Could not find move file {0} to {1} !!!", new Object[]{fileName, path});
        }

        return sucess;
    }

    /**
     * Copies mapping to the given path.
     *
     * @param file	File to be copied
     * @param fileName	File name to be given to copied mapping.
     * @param path	Path to where mapping is copied.
     */
    public static boolean copyFileTo(File file, String fileName, String path) {
        try {
            File newLocation = new File(path + "/" + fileName);
            if (!newLocation.createNewFile()) {
                Logger.getLogger(GeneralUtils.class.getName()).log(
                        Level.SEVERE, "Could not find copy file from {0} to {1} !!!",
                        new Object[]{fileName, path});
                return false;
            }

            GeneralUtils.unzipFile(file, newLocation);

            return true;
        } catch (IOException ex) {
            logger.error( null, ex);
        }
        return false;
    }

    /**
     * Creates directory for the given path.
     *
     * @param path	Path where directory is to be created.
     */
    public static File makeDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    /**
     * Returns mapping for give path.
     *
     * @param path	File path.
     * @param fileMayNotExist
     */
    public static File getFile(String path, boolean fileMayNotExist) {
        File file = null;
        try {
            file = LocalUrlCacher.convertURIToFile(URI.create(path), fileMayNotExist);
        } catch (Exception ex) {
            logger.error(null, ex);
        }

        if (file == null && !fileMayNotExist) {
            logger.error( "Invalid path : {0} !!!", path);
        }

        return file;
    }

    /**
     * this method will return an Input for a gzip, trying to determine if this
     * is a regular or blocked gzip file. If it cannot determine the default is
     * blocked gzip.
     *
     * @param url the url (should be a url for a gzip file)
     * @param istr the raw (uncompressed) InputStream to wrap
     * @return BlockCompressedInputStream if this is a blocked gzip file,
     * GZIPInputStream otherwise
     */
    public static InputStream getGZipInputStream(String url, InputStream istr) throws IOException {
        InputStream gzstr = null;
        boolean blockedGZip = false;
        GZIPInputStream gis = null;
        try {
            URLConnection conn = LocalUrlCacher.connectToUrl(url, null, -1);
            gis = new GZIPInputStream(conn.getInputStream());
            gis.read();
        } catch (ZipException x) {
            blockedGZip = true;
        } catch (Exception x) {
            blockedGZip = true; // default
        } finally {
            try {
                gis.close();
            } catch (Exception e) {
            }
        }
        if (blockedGZip) {
            gzstr = new BlockCompressedInputStream(istr);
        } else {
            URLConnection conn = LocalUrlCacher.connectToUrl(url, null, -1);
            gzstr = new GZIPInputStream(conn.getInputStream());
        }

        return gzstr;
    }

    public static void unzipFile(File f, File f2) throws IOException {
        // File must be unzipped!
        InputStream is = null;
        OutputStream out = null;
        try {
            // This will also unzip the stream if necessary
            is = GeneralUtils.getInputStream(f, new StringBuffer());
            out = new FileOutputStream(f2);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            GeneralUtils.safeClose(is);
            GeneralUtils.safeClose(out);
        }
    }


    /* from http://stackoverflow.com/questions/4596447/java-check-if-file-exists-on-remote-server-using-its-url */
    private static boolean httpExists(String URLName) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con
                    = (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFileScheme() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("windows")) {
            return "file:/";
        } else {
            return "file:";
        }
    }

    public static String fixFileName(String fileName) {
        if (fileName.startsWith("http:/") || fileName.startsWith("https:/")) {
            return fileName;
        }
        String fixedFileName = fileName;
        if (fileName.startsWith("file:/")) {
            fixedFileName = fileName.substring(getFileScheme().length());
        } else if (fileName.startsWith("file:")) {
            fixedFileName = fileName.substring("file:".length());
        }
        return URLDecode(fixedFileName);
    }

    private static final double COMPRESSION_RATIO = 3.5;

    public static long getUriLength(URI uri) {
        long uriLength = -1;
        try {
            SeekableStream seekableStream = SeekableStreamFactory.getInstance().getStreamFor(GeneralUtils.fixFileName(uri.toString()));
            uriLength = seekableStream.length();
            seekableStream.close();
            // very, very gross approximation
            if (uri.toString().toLowerCase().endsWith(".gz") || uri.toString().toLowerCase().endsWith(".zip")) {
                uriLength = (long) (uriLength * COMPRESSION_RATIO);
            }
        } catch (IOException x) {
            logger.error( "can''t get length of uri {0}", uri);
        }
        return uriLength;
    }

    public static boolean urlExists(String url) {
        if (url == null) {
            return false;
        } else if (url.startsWith("http:")) {
            return httpExists(url);
        } else {
            File f = new File(fixFileName(url));
            return f.exists();
        }
    }

    /**
     * Private copy of the default Synonym lookup
     *
     * @see SynonymLookup#getDefaultLookup()
     */
    private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();

    public static String getPreferredVersionName(Set<GenericVersion> gVersions) {
        return LOOKUP.getPreferredName(gVersions.iterator().next().versionName);
    }

    public static String preferencesDisplay(Preferences prefs) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            prefs.exportSubtree(baos);
        } catch (Exception x) {
            return x.getClass().getSimpleName() + " " + x.getMessage();
        }
        return baos.toString();
    }

    /**
     * http://stackoverflow.com/questions/740299/how-do-i-sort-a-set-to-a-list-in-java
     *
     * @param <T>
     * @param collection
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> collection) {
        List<T> list = new ArrayList<T>(collection);
        java.util.Collections.sort(list);
        return list;
    }

    public static <T> List<T> asSortedList(Collection<T> collection, java.util.Comparator<T> comparator) {
        List<T> list = new ArrayList<T>(collection);
        java.util.Collections.sort(list, comparator);
        return list;
    }

    // Can be improved to get any data flavor
    public static String getClipboard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                return text.trim();
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static void copyToClipboard(String content) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection data = new StringSelection(content);
        clipboard.setContents(data, null);
    }

    /**
     * Determine extension.
     *
     * @param stream_name
     * @return the file extension
     */
    public static String getExtension(String stream_name) {
        if (stream_name.endsWith(".link.psl")) {
            return stream_name.substring(stream_name.lastIndexOf(".link.psl"), stream_name.length());
        } else if (stream_name.lastIndexOf('.') >= 0) {
            return stream_name.substring(stream_name.lastIndexOf('.'), stream_name.length());
        } else {
            return "";
        }
    }
}
