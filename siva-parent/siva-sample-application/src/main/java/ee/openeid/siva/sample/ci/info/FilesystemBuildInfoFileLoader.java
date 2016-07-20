package ee.openeid.siva.sample.ci.info;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ee.openeid.siva.sample.configuration.BuildInfoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Load CI build number and commit hash and commiter name 
 */
@Component
public class FilesystemBuildInfoFileLoader implements BuildInfoFileLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemBuildInfoFileLoader.class);

    private static final byte[] EMPTY_CONTENT = new byte[0];
    private static final String UNIX_PATH_START = "/";
    private static final String WINDOWS_DRIVE_LETTER_POSTFIX = ":";

    private BuildInfoProperties properties;

    @Override
    public Observable<BuildInfo> loadBuildInfo() throws IOException {
        final byte[] yamlFile = loadYamlFile();
        return Observable.just(mapToBuildInfo(yamlFile)) ;
    }

    private static BuildInfo mapToBuildInfo(byte[] yamlFile) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            return mapper.readValue(yamlFile, BuildInfo.class);
        } catch (final JsonMappingException ex) {
            LOGGER.warn("Failed to parse JSON with with message: {}", ex.getMessage(), ex);
        }

        return new BuildInfo();
    }

    private byte[] loadYamlFile() throws IOException {
        final Path yamlFilePath = getBuildInfoFilePath();
        if (!Files.exists(yamlFilePath)) {
            LOGGER.warn("No such file exists: {}", yamlFilePath);
            return EMPTY_CONTENT;
        }

        LOGGER.info("Start loading in build info YAML file: {}", yamlFilePath);
        return Files.readAllBytes(yamlFilePath);
    }

    private Path getBuildInfoFilePath() {
        final String defaultPath = Paths.get("").toAbsolutePath() + File.separator;
        final String infoFile = properties.getInfoFile();
        final String infoFilePath = infoFile.startsWith(UNIX_PATH_START) || infoFile.contains(WINDOWS_DRIVE_LETTER_POSTFIX) ?
                infoFile :
                defaultPath + infoFile;

        return Paths.get(infoFilePath);
    }

    @Autowired
    public void setProperties(final BuildInfoProperties properties) {
        this.properties = properties;
    }
}