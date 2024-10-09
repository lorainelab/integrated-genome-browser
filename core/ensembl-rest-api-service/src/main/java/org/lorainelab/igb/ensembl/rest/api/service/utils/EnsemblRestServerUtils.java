package org.lorainelab.igb.ensembl.rest.api.service.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lorainelab.igb.ensembl.rest.api.service.model.EnsemblGenomeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnsemblRestServerUtils {
    private static final Logger logger = LoggerFactory.getLogger(EnsemblRestServerUtils.class);
    public static final String INFO = "info";
    public static final String DIVISIONS = "divisions";
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String SPECIES = "species";
    public static final String DIVISION = "division";

    public static Map<String, EnsemblGenomeData> retrieveEnsemblGenomeResponse(String contextRoot) throws IOException {
        ConcurrentHashMap<String, EnsemblGenomeData> ensemblGenomeDataMap = new ConcurrentHashMap<>();
        List<String> ensemblDivisions = retrieveEnsemblDivisions(contextRoot);
        Pattern pattern = Pattern.compile("^(([a-zA-Z]+_)+[a-zA-Z]+)(?=(_[^\\d_]*\\d))");
        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            List<CompletableFuture<Void>> futures = ensemblDivisions.stream().filter(ensemblDivision -> ensemblDivision.equalsIgnoreCase("EnsemblVertebrates") ||
                            ensemblDivision.equalsIgnoreCase("EnsemblPlants") ||
                            ensemblDivision.equalsIgnoreCase("EnsemblMetazoa"))
                    .map(ensemblDivision -> CompletableFuture.supplyAsync(() -> retrieveEnsemblSpeciesInfo(contextRoot, ensemblDivision), executorService)
                            .thenAccept(ensemblGenomeDataList -> {
                                for (EnsemblGenomeData ensemblGenomeData : ensemblGenomeDataList) {
                                    String name = ensemblGenomeData.getName();
                                    Matcher m = pattern.matcher(name);
                                    if (m.find())
                                        ensemblGenomeData.setName(m.group(1));
                                    ensemblGenomeDataMap.put(ensemblGenomeData.getAssembly(), ensemblGenomeData);
                                }
                            }))
                    .toList();
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                allOf.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                executorService.shutdown();
            }
        }
        return ensemblGenomeDataMap;
    }

    private static List<String> retrieveEnsemblDivisions(String contextRoot) {
        List<String> ensemblDivisions = new ArrayList<>();
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + INFO) + DIVISIONS;
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(CONTENT_TYPE, APPLICATION_JSON);
            HttpGet httpget = new HttpGet(uriBuilder.toString());
            String responseBody = httpClient.execute(httpget, new ApiResponseHandler());
            ensemblDivisions = new Gson().fromJson(
                    responseBody, new TypeToken<List<String>>(){}.getType()
            );
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return ensemblDivisions;
    }

    private static List<EnsemblGenomeData> retrieveEnsemblSpeciesInfo(String contextRoot, String ensemblDivision) {
        Map<String, List<EnsemblGenomeData>> ensemblGenomeDataList = new HashMap<>();
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + INFO) + SPECIES;
        try(CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(CONTENT_TYPE, APPLICATION_JSON);
            uriBuilder.addParameter(DIVISION, ensemblDivision);
            HttpGet httpget = new HttpGet(uriBuilder.toString());
            String responseBody = httpClient.execute(httpget, new ApiResponseHandler());
            ensemblGenomeDataList = new Gson().fromJson(
                    responseBody, new TypeToken<Map<String, List<EnsemblGenomeData>>>(){}.getType()
            );
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return ensemblGenomeDataList.containsKey(SPECIES) ? ensemblGenomeDataList.get(SPECIES) : new ArrayList<>();
    }

    public static String toExternalForm(String urlString) {
        urlString = urlString.trim();
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }
}
