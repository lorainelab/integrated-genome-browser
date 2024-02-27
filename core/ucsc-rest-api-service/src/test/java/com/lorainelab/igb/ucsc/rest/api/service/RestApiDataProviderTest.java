package com.lorainelab.igb.ucsc.rest.api.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lorainelab.igb.ucsc.rest.api.service.RestApiDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestApiDataProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(RestApiDataProviderTest.class);
    private final static String UCSC_REST_URL = "https://api.genome.ucsc.edu/";
    private static final String HUMAN_GENOME_ID = "hg38";
    private static RestApiDataProvider restApiDataProvider;

    @BeforeAll
    public static void setup() {
        restApiDataProvider = new RestApiDataProvider(UCSC_REST_URL, "UCSC", 1);
    }

    @Test
    public void retrieveSupportedGenomeVersions() {
        Assertions.assertTrue(restApiDataProvider.getSupportedGenomeVersionNames().contains(HUMAN_GENOME_ID));
        restApiDataProvider.getSupportedGenomeVersionNames().forEach(logger::info);
    }
}

