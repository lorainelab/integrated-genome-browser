package org.lorainelab.igb.quickload.util;

import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.util.ModalUtils;
import static com.affymetrix.genometry.util.UriUtils.getInputStream;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import org.lorainelab.igb.quickload.QuickloadConstants;
import static org.lorainelab.igb.quickload.QuickloadConstants.ANNOTS_XML;
import org.lorainelab.igb.quickload.model.annots.QuickloadFile;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.synonymlookup.services.SpeciesInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
/**
 *
 * @author dcnorris
 */
public class QuickloadUtils {

    private static final Logger logger = LoggerFactory.getLogger(QuickloadUtils.class);
    private static final AnnotsParser ANNOTS_PARSER = new AnnotsParser();

    public static void loadGenomeVersionSynonyms(String urlString, Multimap<String, String> genomeVersionSynonyms) {
        try {
            urlString = toExternalForm(urlString);
            urlString += QuickloadConstants.SYNONYMS_TXT;
            URI uri = new URI(urlString);
            parseGenomeVersionSynonyms(getInputStream(uri), genomeVersionSynonyms);
        } catch (URISyntaxException | IOException ex) {
            logger.warn("Optional quickload synonyms.txt file could not be loaded from {}", urlString);
        }

    }

    private static void parseGenomeVersionSynonyms(InputStream istream, Multimap<String, String> genomeVersionSynonyms) throws IOException {
        try (Reader reader = new InputStreamReader(istream)) {
            Stream<CSVRecord> recordStream = getCSVRecordStreamFromTabDelimitedResource(reader);
            recordStream.filter(record -> record.size() >= 2).forEach(record -> {
                genomeVersionSynonyms.putAll(record.get(0), record);
            });
        }
    }

    public static void loadSpeciesInfo(String urlString, Set<SpeciesInfo> speciesInfo) {
        try {
            urlString = toExternalForm(urlString);
            urlString += QuickloadConstants.SPECIES_TXT;
            URI uri = new URI(urlString);
            parseSpeciesInfo(getInputStream(uri), speciesInfo);
        } catch (IOException | URISyntaxException ex) {
            logger.warn("Optional species.txt could not be loaded from: {}", urlString);
        }
    }

    private static void parseSpeciesInfo(InputStream istream, Set<SpeciesInfo> speciesInfo) throws IOException {
        try (Reader reader = new InputStreamReader(istream)) {
            Stream<CSVRecord> recordStream = getCSVRecordStreamFromTabDelimitedResource(reader);
            recordStream.forEach(record -> {
                String speciesName = record.get(0);
                String commonName = null;
                String genomeVersionPrefix = null;
                if (record.size() >= 2) {
                    commonName = record.get(1);
                }
                if (record.size() >= 3) {
                    genomeVersionPrefix = record.get(2);
                }
                SpeciesInfo info = new SpeciesInfo(speciesName, commonName, genomeVersionPrefix);
                speciesInfo.add(info);
            });
        }
    }

    public static void loadSupportedGenomeVersionInfo(String urlString, Map<String, Optional<String>> supportedGenomeVersionInfo) throws IOException, URISyntaxException {
        try {
            urlString = toExternalForm(urlString);
            urlString += QuickloadConstants.CONTENTS_TXT;
            URI uri = new URI(urlString);
            processContentsTextFile(getInputStream(uri), supportedGenomeVersionInfo);
        } catch (IOException | URISyntaxException ex) {
            logger.warn("Could not read contents.txt from: {}", urlString);
            throw ex;
        }
    }

    private static void processContentsTextFile(InputStream istream, Map<String, Optional<String>> supportedGenomeVersionInfo) throws IOException {
        try (Reader reader = new InputStreamReader(istream)) {
            Stream<CSVRecord> recordStream = getCSVRecordStreamFromTabDelimitedResource(reader);
            recordStream.forEach(record -> {
                if (record.size() >= 1) {
                    String genomeName = record.get(0);
                    if (record.size() >= 2) {
                        String description = record.get(1);
                        supportedGenomeVersionInfo.put(genomeName, Optional.of(description));
                    } else {
                        supportedGenomeVersionInfo.put(genomeName, Optional.empty());
                    }
                }
            });
        }
    }
    
    private static boolean isValidRepositoryUrl(String strAnnotsURL){
        try {
            URL AnnotsURL = new URL(strAnnotsURL);
            if(AnnotsURL.toString().contains("https"))
            {
                if(((HttpsURLConnection) AnnotsURL.openConnection()).getResponseCode() != HttpsURLConnection.HTTP_OK)
                    throw new MalformedURLException("Invalid URL or annots.xml not found");
            } else if(strAnnotsURL.contains("http")){
                if(((HttpURLConnection) AnnotsURL.openConnection()).getResponseCode() != HttpURLConnection.HTTP_OK)
                    throw new MalformedURLException("Invalid URL or annots.xml not found");
            } else if(strAnnotsURL.contains("file")){
                if(!(new File(java.net.URLDecoder.decode(strAnnotsURL.replace("file:",""), "UTF-8"))).exists()){
                    throw new FileNotFoundException("Invalid file path or annots.xml not found.");
                }
            }
           
        } catch (MalformedURLException | FileNotFoundException ex) {
            logger.warn(ex.getMessage());
            return false;
        } catch (IOException ex) { 
            logger.warn(ex.getMessage());
            return false;
        }
        return true;
    }

    public static Optional<Set<QuickloadFile>> getGenomeVersionData(String quickloadUrl, String genomeVersionName, Map<String, Optional<String>> supportedGenomeVersionInfo, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        genomeVersionName = getContextRootKey(genomeVersionName, supportedGenomeVersionInfo.keySet(), genomeVersionSynonymLookup).orElse(genomeVersionName);
        String genomeVersionBaseUrl = getGenomeVersionBaseUrl(quickloadUrl, genomeVersionName);
        String annotsXmlUrl = genomeVersionBaseUrl + QuickloadConstants.ANNOTS_XML;
        try {
            URI uri = new URI(annotsXmlUrl);
            if(isValidRepositoryUrl(annotsXmlUrl)){
              try (InputStream inputStream = getInputStream(uri)) {
                List<QuickloadFile> annotsFiles = ANNOTS_PARSER.getQuickloadFileList(inputStream);
                if (!annotsFiles.isEmpty()) {
                    return Optional.of(Sets.newLinkedHashSet(annotsFiles));
                } else {
                    logger.warn("Could not read annots.xml or this file was empty. Skipping this genome version for quickload site {}", genomeVersionBaseUrl);
                    supportedGenomeVersionInfo.remove(genomeVersionName);
                }
                }  
            }
            
        } catch (URISyntaxException | IOException ex) {
            logger.warn("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersionName, genomeVersionBaseUrl, ex);
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static Optional<Map<String, Integer>> getAssemblyInfo(String quickloadUrl, String genomeVersionName) throws IOException, URISyntaxException {
        String genomeVersionBaseUrl = getGenomeVersionBaseUrl(quickloadUrl, genomeVersionName);
        String genomeTxtUrl = genomeVersionBaseUrl + QuickloadConstants.GENOME_TXT;
        Map<String, Integer> assemblyInfo = Maps.newLinkedHashMap();
        URI uri = new URI(genomeTxtUrl);
        try (InputStream stream = getInputStream(uri);
                Reader reader = new InputStreamReader(stream);) {
            getCSVRecordStreamFromTabDelimitedResource(reader).filter(record -> record.size() == 2).forEach(record -> {
                assemblyInfo.put(record.get(0), Integer.parseInt(record.get(1)));
            });
        }
        if (!assemblyInfo.isEmpty()) {
            return Optional.of(assemblyInfo);
        }
        return Optional.empty();
    }

    public static Optional<String> getContextRootKey(final String genomeVersionName, Set<String> supportedGenomeVersionNames, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        if (supportedGenomeVersionNames.contains(genomeVersionName)) {
            return Optional.of(genomeVersionName);
        } else {
            Set<String> genomeVersionSynonyms = genomeVersionSynonymLookup.getSynonyms(genomeVersionName);
            Optional<String> matchingSynonym = genomeVersionSynonyms.stream().filter(syn -> supportedGenomeVersionNames.contains(syn)).findFirst();
            if (matchingSynonym.isPresent()) {
                return Optional.of(matchingSynonym.get());
            }
        }
        return Optional.empty();
    }

    public static String getGenomeVersionBaseUrl(String quickloadUrl, String genomeVersionName) {
        final String externalQuickloadUrl = toExternalForm(quickloadUrl);
        final Function<String, String> toGenomeVersionBaseUrl = name -> externalQuickloadUrl + name;
        return toExternalForm(toGenomeVersionBaseUrl.apply(genomeVersionName));
    }

    public static String toExternalForm(String urlString) {
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString.replaceAll(" ", "%20");
    }

    private static Stream<CSVRecord> getCSVRecordStreamFromTabDelimitedResource(final Reader reader) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.TDF
                .withCommentMarker('#')
                .withIgnoreSurroundingSpaces(true)
                .withIgnoreEmptyLines(true)
                .parse(reader);
        return StreamSupport.stream(records.spliterator(), false);
    }
    
    /**
    * Returns a URI of the file location.
    * If file location is online and absolute, path is not encoded.
    * If file location is local and absolute, path is encoded.
    * If file location is online and relative, spaces are encoded in the relative path.
    * If file location is local and relative, path is encoded.
    * 
    * @param fileName the value of the name attribute from the annots.xml
    * @param url the url of the quickload
    * @param genomeVersionName the IGB-friendly genome version name
    * @return the encoded uri of the file location
    */
    public static URI getUri(String fileName, String url, String genomeVersionName){
        URI uri = null;
        try {
                if (fileName.startsWith("http") || fileName.startsWith("ftp")){
                    uri = new URI(fileName);
                } else {
                    if(new File(fileName).isAbsolute()) {
                        uri = new File(fileName).toURI();
                    } else {
                        if(url.startsWith("http" ) || url.startsWith("ftp")){
                            uri = new URI((url + genomeVersionName + "/" + fileName).replace(" ", "%20"));
                        } else {
                            uri = new File(java.net.URLDecoder.decode(url.replace("file:",""), "UTF-8") + genomeVersionName + "/" + fileName).toURI();
                        }
                    }
                }
            } catch (URISyntaxException | UnsupportedEncodingException ex) {
                logger.error(ex.getMessage(), ex);
            }
        return uri;
    }
}
