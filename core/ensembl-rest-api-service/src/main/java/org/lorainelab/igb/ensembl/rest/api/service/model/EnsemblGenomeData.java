package org.lorainelab.igb.ensembl.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnsemblGenomeData {
    String name;
    String display_name;
    String assembly;
}
