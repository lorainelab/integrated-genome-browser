package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackDetails {
        private String shortLabel;
        private String type;
        private String longLabel;
        private String visibility;
        private String group;
    }
