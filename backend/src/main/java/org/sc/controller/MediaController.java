package org.sc.controller;

import org.apache.commons.lang3.StringUtils;
import org.sc.common.rest.*;
import org.sc.common.rest.response.MediaResponse;
import org.sc.configuration.AppProperties;
import org.sc.data.validator.FileNameValidator;
import org.sc.data.validator.MediaFileValidator;
import org.sc.manager.MediaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(org.sc.controller.MediaController.PREFIX)
public class MediaController {

    public final static String PREFIX = "/media";
    public static final String EMPTY_ID_ERROR = "Empty Id";

    public File uploadDir;

    private final MediaFileValidator mediaFileValidator;
    private final FileNameValidator fileNameValidator;
    private final MediaManager mediaManager;
    private final AppProperties appProperties;

    @Autowired
    public MediaController(final MediaFileValidator mediaFileValidator,
                           final FileNameValidator fileNameValidator,
                           final MediaManager mediaManager,
                           final AppProperties appProperties) {
        this.mediaFileValidator = mediaFileValidator;
        this.fileNameValidator = fileNameValidator;
        this.mediaManager = mediaManager;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        uploadDir = new File(appProperties.getTempStorage());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaResponse upload(@RequestAttribute("file") MultipartFile file) throws IOException {
        final Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
        final String originalFileName = file.getOriginalFilename();
        final Set<String> validationErrors =
                fileNameValidator.validate(originalFileName);
        try (final InputStream input = file.getInputStream()) {
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        validationErrors.addAll(mediaFileValidator.validate(tempFile.toFile()));

        if (validationErrors.isEmpty()) {
            assert originalFileName != null;
            final List<MediaDto> saveResult = mediaManager.save(originalFileName, tempFile);
            return new MediaResponse(Status.OK, Collections.emptySet(), saveResult);
        }
        return new MediaResponse(Status.ERROR, validationErrors, Collections.emptyList());
    }

    @GetMapping("/{id}")
    public MediaResponse getById(@PathVariable String id) {
        if(StringUtils.isEmpty(id)){
            return new MediaResponse(Status.ERROR, Collections.singleton(EMPTY_ID_ERROR), Collections.emptyList());
        }
        List<MediaDto> medias = mediaManager.getById(id);
        return new MediaResponse(Status.OK, Collections.emptySet(), medias);
    }

    @DeleteMapping("/{id}")
    public MediaResponse deleteById(@PathVariable String id) {
        if(StringUtils.isEmpty(id)){
            return new MediaResponse(Status.ERROR, Collections.singleton(EMPTY_ID_ERROR), Collections.emptyList());
        }
        List<MediaDto> medias = mediaManager.deleteById(id);
        return new MediaResponse(Status.OK, Collections.emptySet(), medias);
    }

}
