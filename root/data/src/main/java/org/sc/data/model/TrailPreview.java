package org.sc.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
public class TrailPreview {
    private String id;
    private String code;
    private TrailClassification classification;
    private PlaceRef startPos;
    private PlaceRef finalPos;
    private FileDetails fileDetails;
    private boolean bikeData;
    private TrailStatus trailStatus;
}