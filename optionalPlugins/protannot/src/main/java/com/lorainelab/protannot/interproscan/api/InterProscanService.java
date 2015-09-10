/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.interproscan.api;

import com.lorainelab.protannot.interproscan.appl.model.ParameterType;
import java.util.List;
import java.util.Optional;
import org.w3c.dom.Document;

/**
 *
 * @author jeckstei
 */
public interface InterProscanService {

    public Status status(String jobId);

    public List<Job> run(JobRequest jobRequest);

    public ParameterType getApplications();
    
    public Optional<String> getApplicationLabel(String key);

    public Optional<Document> result(String jobId);

    public static enum Status {

        RUNNING("Running"), FINISHED("Finished"), ERROR("Error"),
        FAILURE("Failure"), NOT_FOUND("Not found"), CANCELLED("Cancelled"),
        INVALID_INPUT_STOP_CODONS_IN_SEQUENCE("Invalid Protein Sequence: Translation contains multiple stop codons"),
        INVALID_NO_TRANSLATED_REGION("Invalid Protein Sequence: No translation information for this gene model");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }


    }
}
