package com.lorainelab.quickload.util;

import com.affymetrix.genometry.data.SpeciesInfo;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.lorainelab.quickload.QuickloadConstants;
import static com.lorainelab.quickload.QuickloadConstants.ANNOTS_XML;
import com.lorainelab.quickload.model.annots.QuickloadFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
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
            URL url = new URL(urlString);
            parseGenomeVersionSynonyms(getInputStream(url), genomeVersionSynonyms);
        } catch (MalformedURLException ex) {
            logger.warn("Optional quickload synonyms.txt file could not be loaded from {}", urlString);
        } catch (IOException ex) {
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
            URL url = new URL(urlString);
            parseSpeciesInfo(getInputStream(url), speciesInfo);
        } catch (IOException ex) {
            logger.warn("Optional species.txt could not be loaded from: {}", urlString, ex);
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
                SpeciesInfo info = new SpeciesInfo(speciesName, Optional.ofNullable(commonName), genomeVersionPrefix);
                speciesInfo.add(info);
            });
        }
    }

    public static void loadSupportedGenomeVersionInfo(String urlString, Map<String, Optional<String>> supportedGenomeVersionInfo) throws IOException {
        try {
            urlString = toExternalForm(urlString);
            urlString += QuickloadConstants.CONTENTS_TXT;
            URL url = new URL(urlString);
            processContentsTextFile(getInputStream(url), supportedGenomeVersionInfo);
        } catch (IOException ex) {
            logger.error("Could not read contents.txt from: {}", urlString, ex);
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

    public static Optional<Set<QuickloadFile>> getGenomeVersionData(String quickloadUrl, String genomeVersionName, Map<String, Optional<String>> supportedGenomeVersionInfo) {
        String genomeVersionBaseUrl = getGenomeVersionBaseUrl(quickloadUrl, genomeVersionName);
        String annotsXmlUrl = genomeVersionBaseUrl + QuickloadConstants.ANNOTS_XML;
        try {
            URL url = new URL(annotsXmlUrl);
            try (Reader reader = new InputStreamReader(getInputStream(url))) {
                List<QuickloadFile> annotsFiles = ANNOTS_PARSER.getQuickloadFileList(reader);
                if (!annotsFiles.isEmpty()) {
                    return Optional.of(Sets.newHashSet(annotsFiles));
                } else {
                    logger.error("Could not read annots.xml or this file was empty. Skipping this genome version for quickload site {}", genomeVersionBaseUrl);
                    supportedGenomeVersionInfo.remove(genomeVersionName);
                }
            }
        } catch (MalformedURLException ex) {
            logger.error("Missing required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersionName, genomeVersionBaseUrl, ex);
        } catch (IOException ex) {
            logger.error("Could not read required {} file for genome version {}, skipping this genome version for quickload site {}", ANNOTS_XML, genomeVersionName, genomeVersionBaseUrl, ex);
        }
        return Optional.empty();
    }

    public static Optional<Map<String, Integer>> getAssemblyInfo(String quickloadUrl, String genomeVersionName) throws MalformedURLException, IOException {
        String genomeVersionBaseUrl = getGenomeVersionBaseUrl(quickloadUrl, genomeVersionName);
        String genomeTxtUrl = genomeVersionBaseUrl + QuickloadConstants.GENOME_TXT;
        Map<String, Integer> assemblyInfo = Maps.newHashMap();
        URL url = new URL(genomeTxtUrl);
        try (Reader reader = new InputStreamReader(getInputStream(url))) {
            getCSVRecordStreamFromTabDelimitedResource(reader).filter(record -> record.size() == 2).forEach(record -> {
                assemblyInfo.put(record.get(0), Integer.parseInt(record.get(1)));
            });
        }
        if (!assemblyInfo.isEmpty()) {
            return Optional.of(assemblyInfo);
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
        return urlString;
    }

    private static Stream<CSVRecord> getCSVRecordStreamFromTabDelimitedResource(final Reader reader) throws IOException {
        Iterable<CSVRecord> records = CSVFormat.TDF
                .withCommentMarker('#')
                .withIgnoreSurroundingSpaces(true)
                .withIgnoreEmptyLines(true)
                .parse(reader);
        return StreamSupport.stream(records.spliterator(), false);
    }

    //TODO could easily be modified to use cache, but this is not required for now since cacheing needs to be updated before it will be useful
    public static InputStream getInputStream(URL url) {
        return HttpRequest.get(url)
                .acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true).buffer();
    }

}
