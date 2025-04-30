package org.lorainelab.igb.services.dynamic.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalGenomeData {
    private String id;
    private Map<String, String> columnValueMap;
    private String infoLinkUrl;
}
