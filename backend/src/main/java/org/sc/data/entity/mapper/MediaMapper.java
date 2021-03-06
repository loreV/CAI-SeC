package org.sc.data.entity.mapper;

import org.bson.Document;
import org.sc.data.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper implements Mapper<Media> {

    private final FileDetailsMapper fileDetailsMapper;

    @Autowired
    public MediaMapper(FileDetailsMapper fileDetailsMapper) {
        this.fileDetailsMapper = fileDetailsMapper;
    }

    @Override
    public Media mapToObject(final Document document) {
        return new Media(
                document.getString(Media.OBJECT_ID),
                document.getDate(Media.CREATION_DATE),
                document.getString(Media.NAME),
                document.getString(Media.FILENAME),
                document.getString(Media.FILE_URL),
                document.getString(Media.MIME),
                document.getLong(Media.FILE_SIZE),
                fileDetailsMapper.mapToObject(document.get(Media.FILE_DETAILS, Document.class)));
    }

    @Override
    public Document mapToDocument(final Media object) {
        return new Document()
                .append(Media.CREATION_DATE, object.getCreationDate())
                .append(Media.NAME, object.getName())
                .append(Media.FILENAME, object.getFileName())
                .append(Media.FILE_URL, object.getFileUrl())
                .append(Media.MIME, object.getMime())
                .append(Media.FILE_SIZE, object.getFileSize())
                .append(Media.FILE_DETAILS,
                        fileDetailsMapper.mapToDocument(object.getFileDetails()));
    }
}
