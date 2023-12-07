package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genome {
    public String description;
    public String nibPath;
    public String organism;
    public String defaultPos;
    public int active;
    public int orderKey;
    public String genome;
    public String scientificName;
    public String htmlPath;
    public int hgNearOk;
    public int hgPbOk;
    public String sourceName;
    public int taxId;
}