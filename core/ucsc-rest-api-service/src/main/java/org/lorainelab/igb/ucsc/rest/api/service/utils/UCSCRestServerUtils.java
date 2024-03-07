package org.lorainelab.igb.ucsc.rest.api.service.utils;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.ChromosomeData;
import org.lorainelab.igb.ucsc.rest.api.service.model.DnaInfo;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.lorainelab.igb.ucsc.rest.api.service.RestApiDataProvider.READ_TIMEOUT;

public class UCSCRestServerUtils {
    private static final Logger logger = LoggerFactory.getLogger(UCSCRestServerUtils.class);
    private static final String LIST = "list";
    private static final String GET_DATA = "getData";
    private static final String UCSC_GENOMES = "ucscGenomes";
    private static final String TRACKS = "tracks";
    private static final String GENOME = "genome";
    private static final String TRACK_PARAM = "trackLeavesOnly";
    public static final String TRACK_PARAM_VALUE = "1";
    private static final String CHROMOSOMES = "chromosomes";
    public static final String SEQUENCE = "sequence";
    public static final String CHROM = "chrom";
    public static final String START = "start";
    public static final String END = "end";
    public static final int HTTP_RESPONSE_OKAY = 200;
    private static final CloseableHttpClient httpClient;
    private static final ResponseHandler<String> responseHandler;

    static {
        httpClient = HttpClients.createDefault();
        responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == HTTP_RESPONSE_OKAY) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
    }

    public static Optional<GenomesData> retrieveDsnResponse(String rootUrl) throws IOException {
        String url = toExternalForm(toExternalForm(rootUrl.trim()) + LIST) + UCSC_GENOMES;
        url = checkValidAndSetUrl(url);
        HttpGet httpget = new HttpGet(url);
        String responseBody = httpClient.execute(httpget, responseHandler);
        GenomesData genomesData = new Gson().fromJson(
                responseBody, GenomesData.class
        );
        return Optional.ofNullable(genomesData);
    }

    public static Optional<UCSCRestTracks> retrieveTracksResponse(String contextRoot, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + LIST) + TRACKS;
        UCSCRestTracks ucscRestTracks = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            uriBuilder.addParameter(TRACK_PARAM, TRACK_PARAM_VALUE);
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            HttpGet httpget = new HttpGet(validUrl);
            String responseBody = httpClient.execute(httpget, responseHandler);
            ucscRestTracks = new Gson().fromJson(
                    responseBody, UCSCRestTracks.class
            );
            if(Objects.nonNull(ucscRestTracks))
                ucscRestTracks.setTracks(responseBody, genomeVersionName);
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.ofNullable(ucscRestTracks);
    }

    public static Map<String, Integer> getAssemblyInfo(String url, GenomeVersion genomeVersion, Set<String> genomeContextRootMap) {
        final String genomeVersionName = genomeVersion.getName();
        Optional<String> contextRootkey = getContextRootKey(genomeVersionName, genomeContextRootMap, genomeVersion.getGenomeVersionSynonymLookup());
        if (contextRootkey.isPresent()) {
            String contextRoot = contextRootkey.get();
            return retrieveAssemblyInfoByContextRoot(url, contextRoot);
        }
        return Maps.newLinkedHashMap();
    }

    public static Map<String, Integer> retrieveAssemblyInfoByContextRoot(String contextRoot, String genomeVersionName) {
        Optional<ChromosomeData> chromosomeResponse = retrieveChromosomeResponse(contextRoot, genomeVersionName);
        if (chromosomeResponse.isPresent()) {
            ChromosomeData chromosomeData = chromosomeResponse.get();
            return chromosomeData.getChromosomes();
        }
        return Maps.newLinkedHashMap();
    }

    private static Optional<ChromosomeData> retrieveChromosomeResponse(String contextRoot, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + LIST) + CHROMOSOMES;
        ChromosomeData chromosomeDate = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            HttpGet httpget = new HttpGet(validUrl);
            String responseBody = httpClient.execute(httpget, responseHandler);
            chromosomeDate = new Gson().fromJson(
                    responseBody, ChromosomeData.class
            );
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.ofNullable(chromosomeDate);
    }

    public static String retrieveDna(String contextRoot, SeqSpan span, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + GET_DATA) + SEQUENCE;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            uriBuilder.addParameter(CHROM, span.getBioSeq().getId());
            uriBuilder.addParameter(START, String.valueOf(span.getMin()));
            uriBuilder.addParameter(END, String.valueOf(span.getMax()));
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            HttpGet httpget = new HttpGet(validUrl);
            String responseBody = httpClient.execute(httpget, responseHandler);
            DnaInfo dnaInfo = new Gson().fromJson(
                    responseBody, DnaInfo.class
            );
            return dnaInfo.getDna();
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public static Optional<String> getContextRootKey(final String genomeVersionName, Set<String> availableGenomesSet, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        if (availableGenomesSet.contains(genomeVersionName)) {
            return Optional.of(genomeVersionName);
        } else {
            Set<String> genomeVersionSynonyms = genomeVersionSynonymLookup.getSynonyms(genomeVersionName);
            Optional<String> matchingSynonym = genomeVersionSynonyms.stream().filter(availableGenomesSet::contains).findFirst();
            if (matchingSynonym.isPresent()) {
                return matchingSynonym;
            }
        }
        return Optional.empty();
    }

    public static String toExternalForm(String urlString) {
        urlString = urlString.trim();
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }

    public static String checkValidAndSetUrl(String uriString) {
        try {
            URL obj = new URI(uriString).toURL();
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(READ_TIMEOUT);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER) {
                    return conn.getHeaderField("Location");
                }
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return uriString;
    }
}
