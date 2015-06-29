/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class InterProscanServiceRest implements InterProscanService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(InterProscanServiceRest.class);

    private static final String INTERPROSCAN_BASE_URL = "http://www.ebi.ac.uk/Tools/services/rest/iprscan5";

    @Override
    public Status status(String jobId) {
        URL url = null;
        try {
            url = new URL(INTERPROSCAN_BASE_URL + "/status/" + jobId);
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {

            String response = IOUtils.toString(bis, "UTF-8");
            try {
                return InterProscanService.Status.valueOf(response);
            } catch (IllegalArgumentException ex) {
                LOG.error(ex.getMessage(), ex);
            }

        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return InterProscanService.Status.ERROR;
    }

    @Override
    public Optional<String> run(JobRequest jobRequest) {
        URL url = null;
        StringBuilder request = new StringBuilder(INTERPROSCAN_BASE_URL);
        request.append("/run?")
                .append("email=").append(jobRequest.getEmail());
        if (jobRequest.getTitle().isPresent()) {
            request.append("&title=").append(jobRequest.getTitle().get());
        }
        if (jobRequest.getGoterms().isPresent()) {
            request.append("&goterms=").append(jobRequest.getGoterms().get());
        }
        if (jobRequest.getPathways().isPresent()) {
            request.append("&pathways=").append(jobRequest.getPathways().get());
        }
        if (jobRequest.getSequence().isPresent()) {
            request.append("&sequence=").append(jobRequest.getSequence().get());
        }
        if (jobRequest.getSignatureMethods().isPresent()) {
            for (JobRequest.SignatureMethods sm : jobRequest.getSignatureMethods().get()) {
                request.append("&appl=").append(sm.toString());
            }
        }
        try {
            url = new URL(request.toString());
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {

            String response = IOUtils.toString(bis, "UTF-8");
            try {
                return Optional.ofNullable(response);
            } catch (IllegalArgumentException ex) {
                LOG.error(ex.getMessage(), ex);
            }

        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return Optional.empty();

    }

}
