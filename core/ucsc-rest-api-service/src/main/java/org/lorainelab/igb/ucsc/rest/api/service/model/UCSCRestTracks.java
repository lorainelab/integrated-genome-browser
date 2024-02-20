package org.lorainelab.igb.ucsc.rest.api.service.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UCSCRestTracks {
    private String downloadTime;
    private long downloadTimeStamp;
    private String dataTime;
    private long dataTimeStamp;
    private Map<String, TrackDetails> tracks;
    private static List<String> AVAILABLE_TYPES = Arrays.asList("genePred", "psl", "bed", "bigBed", "bedDetail", "bigWig");

    public void setTracks(String jsonString, String genome) {
        JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);
        if (jsonObject.has(genome) && jsonObject.get(genome).isJsonObject()) {
            JsonObject genomeData = jsonObject.getAsJsonObject(genome);
            Type type = new TypeToken<Map<String, TrackDetails>>() {}.getType();
            tracks = new Gson().fromJson(genomeData, type);
            tracks.values().removeIf(trackDetails -> !AVAILABLE_TYPES.contains(trackDetails.getType().split(" ")[0]));
        }
    }
}
