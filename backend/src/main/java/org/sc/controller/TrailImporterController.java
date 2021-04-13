package org.sc.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.sc.common.rest.Status;
import org.sc.common.rest.TrailRawDto;
import org.sc.common.rest.response.TrailRawResponse;
import org.sc.manager.TrailFileManager;
import org.sc.manager.TrailManagementManager;
import org.sc.processor.GpxFileHandlerHelper;
import org.sc.util.FileProbeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RestController
@RequestMapping(TrailImporterController.PREFIX)
public class TrailImporterController {

    public final static String PREFIX = "/import";
    public static final String REQUEST_CONTAINS_MISSING_NAMES_ERROR = "File is empty";

    private final TrailFileManager trailFileManager;
    private final TrailManagementManager trailManagementManager;
    private final ControllerPagination controllerPagination;
    private final FileProbeUtil fileProbeUtil;
    private final GpxFileHandlerHelper gpxFileHandlerHelper;


    @Autowired
    public TrailImporterController(final TrailFileManager trailFileManager,
                                   final TrailManagementManager trailManagementManager,
                                   final ControllerPagination controllerPagination,
                                   final FileProbeUtil fileProbeUtil,
                                   final GpxFileHandlerHelper gpxFileHandlerHelper) {
        this.trailFileManager = trailFileManager;
        this.trailManagementManager = trailManagementManager;
        this.controllerPagination = controllerPagination;
        this.fileProbeUtil = fileProbeUtil;
        this.gpxFileHandlerHelper = gpxFileHandlerHelper;
    }

    @Operation(summary = "Read and import one GPX trail file")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TrailRawResponse readGpxFile(@RequestParam("file") MultipartFile gpxFile) {
        return processUploadedFiles(Collections.singletonList(gpxFile));
    }

    @Operation(summary = "Read and import multiple GPX trail files")
    @PostMapping(path = "/bulk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TrailRawResponse readBulkGpxFile(@RequestParam("files") MultipartFile[] files) {
        return processUploadedFiles(Arrays.asList(files));
    }

    private TrailRawResponse processUploadedFiles(final List<MultipartFile> files) {
        final Map<String, Optional<Path>> originalFileNamesToTempPaths
                = trailFileManager.getGPXFilesTempPathList(files);

        if (originalFileNamesToTempPaths.isEmpty()) {
            return constructResponse(singleton(REQUEST_CONTAINS_MISSING_NAMES_ERROR), emptyList(),
                    trailManagementManager.countTrailRaw(),
                    Constants.ZERO, Constants.ONE);
        }

        final Map<String, Path> originalFileNamesToExistingPaths = originalFileNamesToTempPaths
                .entrySet().stream()
                .filter(path -> path.getValue().isPresent())
                .collect(toMap(Map.Entry::getKey,
                        path -> originalFileNamesToTempPaths
                                .get(path.getKey())
                                .orElseThrow(IllegalStateException::new)));

        final Map<String, Path> gpxValidFiles = originalFileNamesToExistingPaths
                .entrySet().stream()
                .filter(nameToPath -> fileProbeUtil.getFileMimeType(nameToPath.getValue().toFile(),
                        nameToPath.getKey()).equals("application/xml"))
                // TODO: verify that this works
                .filter(nameToPath -> gpxFileHandlerHelper.canRead(nameToPath.getValue()))
                .collect(toMap(Map.Entry::getKey,
                        path -> originalFileNamesToExistingPaths
                                .get(path.getKey())));


        final List<TrailRawDto> trailRawDtos = gpxValidFiles
                .keySet()
                .parallelStream()
                .map(originalFilename -> {
                    final String uniqueFileName = trailFileManager.makeUniqueFileName(originalFilename);
                    final Path rawGpxPath = trailFileManager.saveRawGpx(uniqueFileName, originalFileNamesToExistingPaths.get(originalFilename));
                    return trailFileManager
                            .getTrailRawModel(uniqueFileName, originalFilename, rawGpxPath);
                }).collect(toList());

        final List<TrailRawDto> savedTrails = trailRawDtos.stream().map(
                trailManagementManager::saveRaw
        ).collect(toList());

        final int size = savedTrails.size();


        if(size != originalFileNamesToExistingPaths.size()) {
            final Set<String> notProcessedFiles = findNotProcessedFiles(originalFileNamesToExistingPaths.keySet(),
                    savedTrails.stream().map(a -> a.getFileDetails().getOriginalFilename()).collect(Collectors.toSet()));
            return constructResponse(notProcessedFiles, savedTrails, size,
                    Constants.ZERO, size);
        }

        return constructResponse(emptySet(), savedTrails, size,
                Constants.ZERO, size);
    }

    private Set<String> findNotProcessedFiles(Set<String> initialFilenames, Set<String> savedFilenames) {
        return initialFilenames.stream().filter(a-> !savedFilenames.contains(a)).collect(Collectors.toSet());
    }

    private TrailRawResponse constructResponse(Set<String> errors,
                                               List<TrailRawDto> dtos,
                                               long totalCount,
                                               int skip,
                                               int limit) {
        if (!errors.isEmpty()) {
            return new TrailRawResponse(Status.ERROR, errors, dtos, 1L,
                    Constants.ONE, limit, totalCount);
        }
        return new TrailRawResponse(Status.OK, errors, dtos,
                controllerPagination.getCurrentPage(skip, limit),
                controllerPagination.getTotalPages(totalCount, limit), limit, totalCount);
    }
}