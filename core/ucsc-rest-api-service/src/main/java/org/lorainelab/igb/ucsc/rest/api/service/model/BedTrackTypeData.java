package org.lorainelab.igb.ucsc.rest.api.service.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BedTrackTypeData {

    String chrom;
    int chromStart;
    int chromEnd;
    String name;
    int score;
    String strand;
    int thickStart;
    int thickEnd;
    String reserved;
    int blockCount;
    String blockSizes;
    String chromStarts;
    Map<String, Object> props;

    private List<String> properties = new ArrayList<>(Arrays.asList("chrom", "chromStart", "chromEnd", "name", "score", "strand", "thickStart",
            "thickEnd", "reserved", "blockCount", "blockSizes", "chromStarts"));

    public int[] getBlockSizesArray(){
        return Objects.nonNull(blockSizes) && !blockSizes.isEmpty()
                ? Arrays.stream(blockSizes.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

    public int[] getChromStartsArray(){
        return Objects.nonNull(chromStarts) && !chromStarts.isEmpty()
                ? Arrays.stream(chromStarts.split(",")).mapToInt(Integer::parseInt).toArray() : null;
    }

    public void setProps(String jsonString) {
        Gson gson = new Gson();
        Map<String, Object> jsonMap = gson.fromJson(jsonString, new TypeToken<Map<String, Object>>() {
        }.getType());
        this.props = new Hashtable<>();
        jsonMap.keySet().forEach(key -> {
            if(!properties.contains(key))
                props.put(key, jsonMap.get(key));
        });
    }
}
