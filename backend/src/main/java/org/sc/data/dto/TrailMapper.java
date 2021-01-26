package org.sc.data.dto;

import org.mapstruct.Mapper;
import org.sc.common.rest.TrailDto;
import org.sc.data.entity.Trail;

// TODO: ensure mapping works
@Mapper(componentModel = "spring")
public interface TrailMapper {
    TrailDto trailToTrailDto(Trail trail);
    Trail trailDtoToTrail(TrailDto trail);
}
