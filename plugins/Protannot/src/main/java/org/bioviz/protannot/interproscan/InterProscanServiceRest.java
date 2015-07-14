/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.Job;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.api.JobSequence;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author jeckstei
 */
@Component
public class InterProscanServiceRest implements InterProscanService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(InterProscanServiceRest.class);

    private static final String STRING_ENCODING = "UTF-8";

    private static final String INTERPROSCAN_BASE_URL = "http://www.ebi.ac.uk/Tools/services/rest/iprscan5";

    private JAXBContext jaxbc;
    
    private ParameterType cachedParameterType;

    @Activate
    public void activate() throws JAXBException {
        jaxbc = JAXBContext.newInstance("org.bioviz.protannot.interproscan.appl.model", this.getClass().getClassLoader());
    }

    @Override
    public Optional<String> getApplicationLabel(String key) {
        if(cachedParameterType == null) {
            cachedParameterType = getApplications();
        }
        return cachedParameterType.getValues().getValue().stream()
                .filter(vt -> vt.getValue().toLowerCase().contains(key.toLowerCase()))
                .map(vt -> vt.getLabel())
                .findFirst();

    }
    
    

    @Override
    public ParameterType getApplications() {
        URL url = null;

        try {
            url = new URL(INTERPROSCAN_BASE_URL + "/parameterdetails/appl");
        } catch (MalformedURLException ex) {
            Logger.getLogger(InterProscanServiceRest.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {
            Object obj = ((JAXBElement<ParameterType>) jaxbc.createUnmarshaller().unmarshal(bis)).getValue();
            cachedParameterType = (ParameterType) obj;
            return (ParameterType) obj;
        } catch (JAXBException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return null;
    }

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

    private String buildBodyOfRequest(JobRequest jobRequest, JobSequence jobSequence) {
        StringBuilder body = new StringBuilder();

        body.append("email=").append(jobRequest.getEmail());
        if (jobRequest.getTitle().isPresent()) {
            body.append("&title=").append(jobRequest.getTitle().get());
        }
        if (jobRequest.getGoterms().isPresent()) {
            body.append("&goterms=").append(jobRequest.getGoterms().get());
        }
        if (jobRequest.getPathways().isPresent()) {
            body.append("&pathways=").append(jobRequest.getPathways().get());
        }
        if (jobRequest.getSignatureMethods().isPresent()) {
            for (String sm : jobRequest.getSignatureMethods().get()) {
                body.append("&appl=").append(sm);
            }
        }
        body.append("&sequence=%3Eigb protannot%0A").append(jobSequence.getProteinSequence());
        return body.toString();
    }

    private URL buildUrlOfRequest() throws MalformedURLException {
        StringBuilder request = new StringBuilder(INTERPROSCAN_BASE_URL);
        request.append("/run");
        URL url = new URL(request.toString());
        return url;
    }

    private HttpURLConnection createHttpConnection() throws MalformedURLException, ProtocolException, IOException {
        URL url = buildUrlOfRequest();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        return con;
    }

    private void postJobToUrl(String body, HttpURLConnection con) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(con.getOutputStream())) {
            StringReader sr = new StringReader(body);
            IOUtils.copy(sr, bos);
        }
    }

    private void readResponseFromPost(HttpURLConnection con, List<Job> results, JobSequence jobSequence) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(con.getInputStream())) {
            String response = IOUtils.toString(bis, STRING_ENCODING);
            LOG.info(response);
            results.add(new Job(jobSequence.getSequenceName(), response));
        }
    }

    @Override
    public List<Job> run(JobRequest jobRequest) {
        List<Job> results = new ArrayList<>();
        for (JobSequence jobSequence : jobRequest.getJobSequences()) {
            try {
                String body = buildBodyOfRequest(jobRequest, jobSequence);
                HttpURLConnection con = createHttpConnection();
                postJobToUrl(body, con);
                readResponseFromPost(con, results, jobSequence);
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            } 
        }
        return results;
    }

    @Override
    public Optional<Document> result(String jobId) {
        StringBuilder requestUrl = new StringBuilder(INTERPROSCAN_BASE_URL);
        requestUrl.append("/result/")
                .append(jobId).append("/xml");
        URL url = null;
        try {
            url = new URL(requestUrl.toString());
        } catch (MalformedURLException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        Document document = null;
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(bis);
        } catch (IOException | SAXException | ParserConfigurationException ex) {
            LOG.error(ex.getMessage());
        }
        return Optional.ofNullable(document);
    }

}
