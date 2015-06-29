/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {

            String response = IOUtils.toString(bis, "UTF-8");
            try {
                return InterProscanService.Status.valueOf(response);
            } catch (IllegalArgumentException ex) {
                LOG.error(ex.getMessage(), ex);
            }

        } catch (IOException ex) {
            LOG.error(jobId);
        }
        return InterProscanService.Status.ERROR;
    }

    @Override
    public String run(JobRequest jobRequest) {
        return "";
    }

}
