/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api.test;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import junit.framework.Assert;
import org.bioviz.protannot.interproscan.InterProscanServiceRest;
import org.bioviz.protannot.interproscan.api.InterProscanService;
import org.bioviz.protannot.interproscan.api.JobRequest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tarun
 */
public class InterProscanServiceRestTest {
    
    private static final Logger logger = LoggerFactory.getLogger(InterProscanServiceRestTest.class);

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
        
        Optional<Set<JobRequest.SignatureMethods>> set = Optional.of(Sets.newHashSet(JobRequest.SignatureMethods.BlastProDom, JobRequest.SignatureMethods.FPrintScan, JobRequest.SignatureMethods.HMMPIR, JobRequest.SignatureMethods.HMMPfam,
                JobRequest.SignatureMethods.HMMSmart, JobRequest.SignatureMethods.HMMTigr, JobRequest.SignatureMethods.ProfileScan,
                JobRequest.SignatureMethods.HAMAP, JobRequest.SignatureMethods.Coils, JobRequest.SignatureMethods.Phobius, JobRequest.SignatureMethods.Gene3D,
                JobRequest.SignatureMethods.HMMPanther, JobRequest.SignatureMethods.TMHMM, JobRequest.SignatureMethods.SignalPHMM, JobRequest.SignatureMethods.SuperFamily,
                JobRequest.SignatureMethods.PatternScan));

        
        request.setSignatureMethods(set);
        
        Optional<String> id = service.run(request);
        Assert.assertTrue(id.isPresent());
        
    }

}
