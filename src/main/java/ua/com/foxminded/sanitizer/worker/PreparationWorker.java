package ua.com.foxminded.sanitizer.worker;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;

@RequiredArgsConstructor
public class PreparationWorker extends Task<List<Path>> {
    @NonNull
    private Path originalFolder;
    @NonNull
    private Path outputFolder;
    private static final Logger logger = LogManager.getLogger("sanitizer");

    @Override
    protected List<Path> call() throws Exception {
        try (Stream<Path> walk = Files.walk(originalFolder)) {
            List<Path> paths = walk.collect(Collectors.toList());
            int filesQuantity = paths.size();
            int filesCounter = 0;
            String snapshotTimeString = new FileWorker().getCurrentDateTimeString();
            String projectName = Paths.get(outputFolder.toString(), originalFolder.getFileName().toString()).toString();
            String projectNameWithSnapshot = projectName + "-v" + snapshotTimeString + "-";

            logger.info("### current time snapshot: " + snapshotTimeString);
            for (Path path : paths) {
                String basePathString = outputFolder.resolve(originalFolder.getParent().relativize(path)).toString();
                Path origPath = Paths.get(basePathString.replaceFirst(projectName,
                        projectNameWithSnapshot + ISanitizerEnvironment.ORIG_SUFFIX));

                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createDirectory(origPath);
                    } catch (FileAlreadyExistsException e) { // пропускаем
                    }
                } else {
                    Files.copy(path, origPath, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.info("prepared " + path);
                filesCounter++;
                this.updateProgress(filesCounter, filesQuantity);
                this.updateMessage("prepare: " + filesCounter + "/" + filesQuantity + " files");
            }
            return paths;
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("!!! error during file strip process");
            return null;
        }
    }
}
