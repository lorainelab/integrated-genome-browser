package org.lorainelab.igb.ucsc.rest.api.service.model;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UCSCRestTracks {
    private String downloadTime;
    private long downloadTimeStamp;
    private String dataTime;
    private long dataTimeStamp;
    private Map<String, TrackDetails> tracks;
    private static List<String> AVAILABLE_TYPES = Arrays.asList("genePred", "psl", "bed", "bigBed", "bedDetail", "wig", "bigGenePred", "barChart", "bigBarChart");

    public void setTracks(String jsonString, String genome) {
        JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
        if (jsonObject.has(genome) && jsonObject.get(genome).isJsonObject()) {
            JsonObject genomeData = jsonObject.getAsJsonObject(genome);
            tracks = new HashMap<>();
            traverseTracksResponseJson(genome, genomeData, null, tracks);
        }
    }

    private void traverseTracksResponseJson(String jsonObjectKey, JsonObject jsonObject, String group, Map<String, TrackDetails> tracks) {
        boolean isParent = false;
        Set<String> keySet = jsonObject.keySet();
        for (String key: keySet) {
            if(jsonObject.get(key).isJsonObject()) {
                isParent = true;
                group = jsonObject.has("group") ? jsonObject.get("group").getAsString(): group;
                traverseTracksResponseJson(key, jsonObject.get(key).getAsJsonObject(), group, tracks);
            }
        }
        if(!isParent && jsonObject.has("type") && AVAILABLE_TYPES.contains(jsonObject.get("type").getAsString().split(" ")[0])) {
            TrackDetails trackDetails = new Gson().fromJson(jsonObject, TrackDetails.class);
            if (Strings.isNullOrEmpty(trackDetails.getGroup()) && group != null) {
                trackDetails.setGroup(group);
            }
            tracks.put(jsonObjectKey, trackDetails);
        }
    }
}
