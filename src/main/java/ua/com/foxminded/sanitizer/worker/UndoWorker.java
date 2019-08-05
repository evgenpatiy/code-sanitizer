package ua.com.foxminded.sanitizer.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.Config;

@RequiredArgsConstructor
public class UndoWorker {
    @NonNull
    private File baseFolder;
    @NonNull
    private Config config;
    private FileWorker fileWorker = new FileWorker();
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public Path getPatchFile() {
        try (Stream<Path> walk = Files.walk(Paths.get(fileWorker.getProperOriginalFolderName(baseFolder)))) {
            return walk.filter(p -> p.toString().endsWith(ISanitizerEnvironment.PATCH_EXT)).findFirst().get();
        } catch (IOException e) {
            logger.error("error during process " + baseFolder);
            e.printStackTrace();
        }
        return null;
    }
}
