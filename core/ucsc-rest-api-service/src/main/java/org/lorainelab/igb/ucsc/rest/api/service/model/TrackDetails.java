package org.lorainelab.igb.ucsc.rest.api.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackDetails {
        private String shortLabel;
        private String type;
        private String longLabel;
        private String visibility;
        private String group;
        private String barChartColors;
        private String barChartBars;

        public String[] getBarChartBarCategories(){
                return Objects.nonNull(barChartBars) && !barChartBars.isEmpty()
                        ? barChartBars.split(" ")
                        : null;
        }

        public String[] getBarChartColors(){
                return Objects.nonNull(barChartColors) && !barChartColors.isEmpty()
                        ? barChartColors.split(" ")
                        : null;
        }
    }
