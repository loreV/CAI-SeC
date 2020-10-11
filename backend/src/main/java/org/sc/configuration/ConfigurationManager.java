package org.sc.configuration;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.sc.controller.AccessibilityNotificationController;
import org.sc.controller.TrailController;
import org.sc.controller.MaintenanceController;
import spark.Spark;

import javax.inject.Named;
import java.io.File;

import static java.lang.String.format;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.sc.configuration.ConfigurationProperties.LOCAL_IP_ADDRESS;
import static spark.Spark.*;

public class ConfigurationManager {

    public static final String TMP_FOLDER = "tmp";

    private final Logger LOG = getLogger(ConfigurationManager.class.getName());

    private final DataSource dataSource;

    private static final String PORT_PROPERTY = "web-port";
    public static final File UPLOAD_DIR = new File(TMP_FOLDER);

    /**
     * Controllers
     */
    private final TrailController trailController;
    private final MaintenanceController maintenanceController;
    private final AccessibilityNotificationController accessibilityNotificationController;

    @Inject
    public ConfigurationManager(final @Named(PORT_PROPERTY) String port,
                                final TrailController trailController,
                                final MaintenanceController maintenanceController,
                                final AccessibilityNotificationController accessibilityNotificationController,
                                final DataSource dataSource) {
        this.trailController = trailController;
        this.maintenanceController = maintenanceController;
        this.accessibilityNotificationController = accessibilityNotificationController;
        this.dataSource = dataSource;
        webServerSetup(port);
        UPLOAD_DIR.mkdir();

    }

    private void webServerSetup(final String port) {
        Spark.ipAddress(LOCAL_IP_ADDRESS);
        Spark.staticFiles.location("/public"); // Static files
        Spark.staticFiles.externalLocation(TMP_FOLDER); // Static files
        port(Integer.parseInt(port));
    }


    public void init() {
        startControllers();
        testConnectionWithDB();
        LOG.info(format("Configuration completed. Listening on port %s", port()));
    }

    private void testConnectionWithDB() {
        dataSource.getClient().listDatabases();
    }

    private void startControllers() {
        after((request, response) -> response.type("application/json"));
        trailController.init();
        maintenanceController.init();
        accessibilityNotificationController.init();
    }



}
