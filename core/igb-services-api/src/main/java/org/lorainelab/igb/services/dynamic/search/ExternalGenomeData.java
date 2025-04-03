package org.lorainelab.igb.services.dynamic.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalGenomeData {
    private String id;
    private String commonName;
    private String scientificName;
    private String assemblyVersion;
    private String infoLinkUrl;
}
