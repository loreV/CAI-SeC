package org.sc.data.repository;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.sc.common.rest.Position;
import org.sc.common.rest.Trail;
import org.sc.common.rest.TrailPreview;
import org.sc.configuration.DataSource;
import org.sc.data.entity.mapper.Mapper;
import org.sc.data.entity.mapper.TrailLightMapper;
import org.sc.data.entity.mapper.TrailMapper;
import org.sc.data.entity.mapper.TrailPreviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.sc.data.repository.MongoConstants.*;

@Repository
public class TrailDAO {


    private static final String RESOLVED_START_POS_COORDINATE = Trail.START_POS + "." + Position.COORDINATES;

    // Max number of documents output per request
    // TODO: shall return this amount only if requested amount is greater than it.
    public static final int RESULT_LIMIT = 150;

    private final MongoCollection<Document> collection;

    private final Mapper<Trail> trailMapper;
    private final Mapper<Trail> trailLightMapper;
    private final Mapper<TrailPreview> trailPreviewMapper;

    @Autowired
    public TrailDAO(final DataSource dataSource,
                    final TrailMapper trailMapper,
                    final TrailLightMapper trailLightMapper,
                    final TrailPreviewMapper trailPreviewMapper) {
        this.collection = dataSource.getDB().getCollection(Trail.COLLECTION_NAME);
        this.trailMapper = trailMapper;
        this.trailLightMapper = trailLightMapper;
        this.trailPreviewMapper = trailPreviewMapper;
    }

    public List<Trail> getTrailsByStartPosMetricDistance(final double longitude,
                                                         final double latitude,
                                                         final double meters,
                                                         final int limit) {
        final FindIterable<Document> documents = collection.find(
                new Document(RESOLVED_START_POS_COORDINATE,
                        new Document($_NEAR_OPERATOR,
                                new Document(RESOLVED_COORDINATES, Arrays.asList(longitude, latitude)
                                )
                        ).append($_MIN_DISTANCE_FILTER, 0).append($_MAX_M_DISTANCE_FILTER, meters))).limit(limit);
        return toTrailsList(documents);
    }

    @NotNull
    public List<Trail> trailsByPointDistance(double longitude, double latitude, double meters, int limit) {
        final AggregateIterable<Document> aggregate = collection.aggregate(Arrays.asList(new Document($_GEO_NEAR_OPERATOR,
                        new Document(NEAR_OPERATOR, new Document("type", "Point").append("coordinates", Arrays.asList(longitude, latitude)))
                                .append(DISTANCE_FIELD, "distanceToIt")
                                .append(KEY_FIELD, "geoPoints.coordinates")
                                .append(INCLUDE_LOCS_FIELD, "closestLocation")
                                .append(MAX_DISTANCE_M, meters)
                                .append(SPHERICAL_FIELD, "true")
                                .append(UNIQUE_DOCS_FIELD, "true")),
                new Document(LIMIT, limit)
        ));
        return toTrailsList(aggregate);
    }

    @NotNull
    public List<Trail> getTrails(boolean isLight, int page, int count) {
        if (isLight) {
            return toTrailsLightList(collection.find(new Document()).limit(RESULT_LIMIT));
        }
        return toTrailsList(collection.find(new Document()).limit(RESULT_LIMIT));
    }

    @NotNull
    public List<Trail> getTrailByCode(@NotNull String code, boolean isLight) {
        if (isLight) {
            return toTrailsLightList(collection.find(new Document(Trail.CODE, code)));
        }
        return toTrailsList(collection.find(new Document(Trail.CODE, code)));
    }

    public boolean delete(final String code) {
        return collection.deleteOne(new Document(Trail.CODE, code)).getDeletedCount() > 0;
    }

    public void upsert(final Trail trailRequest) {
        final Document trail = trailMapper.mapToDocument(trailRequest);
        collection.replaceOne(new Document(Trail.CODE, trailRequest.getCode()),
                trail, new ReplaceOptions().upsert(true));
    }

    @NotNull
    public List<TrailPreview> getTrailPreviews(final int page, final int count) {
        return toTrailsPreviewList(collection.find()
                .projection(projectPreview())
                .skip(page)
                .limit(count));
    }

    @NotNull
    public List<TrailPreview> trailPreviewByCode(final @NotNull String code) {
        return toTrailsPreviewList(collection.find(new Document(Trail.CODE, code))
                .projection(projectPreview()));
    }

    private List<TrailPreview> toTrailsPreviewList(final FindIterable<Document> documents) {
        return StreamSupport.stream(documents.spliterator(), false)
                .map(trailPreviewMapper::mapToObject).collect(toList());
    }

    private Document projectPreview() {
        return new Document(Trail.CODE, 1).append(Trail.START_POS, 1).append(Trail.FINAL_POS, 1).append(Trail.CLASSIFICATION, 1)
                .append(Trail.LAST_UPDATE_DATE, 1);
    }

    @NotNull
    private List<Trail> toTrailsLightList(Iterable<Document> documents) {
        return StreamSupport.stream(documents.spliterator(), false).map(trailLightMapper::mapToObject).collect(toList());
    }

    @NotNull
    private List<Trail> toTrailsList(Iterable<Document> documents) {
        return StreamSupport.stream(documents.spliterator(), false).map(trailMapper::mapToObject).collect(toList());
    }

}