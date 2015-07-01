/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api.test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import junit.framework.Assert;
import org.bioviz.protannot.interproscan.InterProscanServiceRest;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.bioviz.protannot.interproscan.appl.model.ValueType;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author Tarun
 */
public class InterProscanServiceRestTest {

    private static final Logger logger = LoggerFactory.getLogger(InterProscanServiceRestTest.class);

    @Ignore
    @Test
    public void testStatus() {
        InterProscanService service = new InterProscanServiceRest();
        InterProscanService.Status status = service.status("iprscan5-R20150629-154703-0868-18931209-oy");
        Assert.assertEquals(status, InterProscanService.Status.FINISHED);
    }


    @Test
    public void testRun() {
        InterProscanService service = new InterProscanServiceRest();
        JobRequest request = new JobRequest();
        request.setEmail("tmall@uncc.edu");
        ParameterType applParameters = service.getApplications();
        List<ValueType> applValues = applParameters.getValues().getValue();
        Set<String> inputApplSet = new HashSet<>();
        for(ValueType valueType : applValues) {
            inputApplSet.add(valueType.getValue());
        }
        
        request.setSignatureMethods(Optional.of(inputApplSet));
        request.setTitle(Optional.empty());
        request.setGoterms(Optional.empty());
        request.setPathways(Optional.empty());
        request.setSequence(Optional.of("MSKLPRELTRDLERSLPAVASLGSSLSHSQSLSSHLLPPPEKRRAISDVRRTFCLFVTFDLLFISLLWIIELNTNTGIRKNLEQEIIQYNFKTSFFDIFVLAFFRFSGLLLGYAVLRLRHWWVIALLSKGAFGYLLPIVSFVLAWLETWFLDFKVLPQEAEEERWYLAAQVAVARGPLLFSGALSEGQFYSPPESFAGSDNESDEEVAGKKSFSAQEREYIRQGKEATAVVDQILAQEENWKFEKNNEYGDTVYTIEVPFHGKTFILKTFLPCPAELVYQEVILQPERMVLWNKTVTACQILQRVEDNTLISYDVSAGAAGGVVSPRDFVNVRRIERRRDRYLSSGIATSHSAKPPTHKYVRGENGPGGFIVLKSASNPRVCTFVWILNTDLKGRLPRYLIHQSLAATMFEFAFHLRQRISELGARA"));
        Optional<String> id = service.run(request);
        Assert.assertTrue(id.isPresent());

    }
    
    @Ignore
    @Test
    public void testResult() {
        InterProscanService service = new InterProscanServiceRest();
        Optional<Document> result = service.result("iprscan5-R20150629-154703-0868-18931209-oy");
        Assert.assertTrue(result.isPresent());
        Document document = result.get();
    }

    @Ignore
    @Test
    public void testGetApplications() {
        InterProscanService service = new InterProscanServiceRest();
        Assert.assertNotNull(service.getApplications());
        Assert.assertFalse(service.getApplications().getValues().getValue().isEmpty());
    }
}
