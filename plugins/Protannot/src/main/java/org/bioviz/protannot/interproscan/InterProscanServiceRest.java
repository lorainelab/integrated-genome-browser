/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.resulttype.model.ResultTypes;
import org.bioviz.protannot.interproscan.resulttype.model.TypeType;
import org.bioviz.protannot.interproscan.resulttype.model.TypesType;
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
                request.append("&appl=").append(sm.getValue());
            }
        }
        HttpURLConnection con = null;
        try {
            url = new URL(request.toString());
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        try (BufferedInputStream bis = new BufferedInputStream(con.getInputStream())) {
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
    
    public List<ResultTypes> resultTypes(String jobId) {
        StringBuilder requestUrl = new StringBuilder(INTERPROSCAN_BASE_URL);
        requestUrl.append("/resulttypes/")
                .append(jobId);
        URL url = null;
        try {
            url = new URL(requestUrl.toString());
        } catch (MalformedURLException ex) {
            Logger.getLogger(InterProscanServiceRest.class.getName()).log(Level.SEVERE, null, ex);
        }
        TypesType resultTypesJaxb = null;
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {
            JAXBContext jaxbContext = JAXBContext.newInstance(TypesType.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            resultTypesJaxb = (TypesType) jaxbUnmarshaller.unmarshal(bis);
            
        } catch (IOException ex) {
            
        } catch (JAXBException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        
        List<ResultTypes> resultTypes = new ArrayList<>();
        for(TypeType resultType : resultTypesJaxb.getType()) {
            resultTypes.add(new ResultTypes(resultType.getDescription(), resultType.getIdentifier()));
        }
        return resultTypes;
    }

}
