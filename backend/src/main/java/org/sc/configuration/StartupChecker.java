package org.sc.configuration;

import org.apache.logging.log4j.Logger;
import org.sc.data.repository.TrailDatasetVersionDao;
import org.sc.util.FileManagementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.File;

import static org.apache.logging.log4j.LogManager.getLogger;

@Component
public class StartupChecker {

    private static final Logger LOGGER = getLogger(StartupChecker.class);

    @Autowired
    TrailDatasetVersionDao trailDatasetVersionDao;
    @Autowired
    AppProperties appProperties;
    @Autowired
    FileManagementUtil fileManagementUtil;

    @PostConstruct
    public void init() {
        configureDir(appProperties.getTempStorage(), "Could not create temp folder");
        configureDir(appProperties.getTrailStorage(), "Could not create storage folder");
        configureDir(fileManagementUtil.getMediaStoragePath(), "Could not create media folder");

        try {
            trailDatasetVersionDao.getLast();
        } catch (Exception mongoSocketOpenException) {
            LOGGER.error("Could not establish a correct configuration. Is the database available and running?");
        }
    }

    private void configureDir(final String path,
                              final String errorPath) {
        final File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new IllegalStateException(errorPath);
            }
        }
    }
}
