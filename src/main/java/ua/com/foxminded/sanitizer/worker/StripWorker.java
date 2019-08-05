package ua.com.foxminded.sanitizer.worker;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.Config;
import ua.com.foxminded.sanitizer.data.ProjectFileMask;
import ua.com.foxminded.sanitizer.data.RefactorReplacement;

@RequiredArgsConstructor
public class StripWorker extends Task<List<Path>> implements ISanitizerEnvironment {
    @NonNull
    private Path originalFolder;
    @NonNull
    private Path outputFolder;
    @NonNull
    private Config config;
    private static final Logger logger = LogManager.getLogger("sanitizer");

    @Override
    protected List<Path> call() throws Exception {
        try (Stream<Path> walk = Files.walk(originalFolder)) {
            List<Path> paths = walk.collect(Collectors.toList());
            int filesQuantity = paths.size();
            int filesCounter = 0;
            FileWorker fileWorker = new FileWorker();
            ProjectFileMask projectFileMask = new ProjectFileMask();
            String timeDumpString = fileWorker.getCurrentDateTimeString();

            for (Path fileInOriginalFolder : paths) {
                if (Files.isDirectory(fileInOriginalFolder, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.createDirectory(outputFolder.resolve(originalFolder.relativize(fileInOriginalFolder)));
                    } catch (FileAlreadyExistsException e) { // пропускаем
                    }
                } else { // пофайловый перебор
                    Path fileInStripFolder = outputFolder.resolve(originalFolder.relativize(fileInOriginalFolder));
                    Path copyOfFileInStripFolder = Paths.get(fileInStripFolder.toString() + ORIGINAL_EXT);
                    Path patchInOriginalFolder = Paths.get(fileInOriginalFolder.toString() + PATCH_EXT);
                    String originalCode = null;
                    String modifiedCode = null;
                    Files.copy(fileInOriginalFolder, fileInStripFolder, StandardCopyOption.REPLACE_EXISTING);

                    projectFileMask = config.getRemoveComment().getFileMask(); // вначале - нужно ли в файле удалять коменты
                    if (config.getRemoveComment().isToRemove()
                            && projectFileMask.isMatchFilePatterns(fileInOriginalFolder.toFile())) {

                        Files.copy(fileInStripFolder, copyOfFileInStripFolder, StandardCopyOption.REPLACE_EXISTING);
                        fileWorker.setOriginalFile(copyOfFileInStripFolder); // копия оригинала
                        fileWorker.setModifiedFile(fileInStripFolder); // обработанный файл
                        fileWorker.setPatchFile(patchInOriginalFolder); // патч в оригинальной папке

                        originalCode = fileWorker // исправляем табы
                                .fixTabsInCodeString(fileWorker.fileToString(fileInStripFolder));
                        modifiedCode = originalCode;

                        // вырезание коментов
                        if (fileInStripFolder.toString().toLowerCase().endsWith(".java")) {
                            modifiedCode = fileWorker.removeCommentsFromJava(modifiedCode);
                        } else if (fileInStripFolder.toString().toLowerCase().endsWith(".xml")) {
                            modifiedCode = fileWorker.removeCommentsFromXml(modifiedCode);
                        }

                        logger.info("strip comments in " + fileInOriginalFolder);
                        // перезаписываем исходный файл с изменениями
                        fileWorker.stringToFile(modifiedCode, fileInStripFolder);
                        // записываем или перезаписываем патч
                        fileWorker.updateTotalPatch("remove comments: " + timeDumpString, originalCode, modifiedCode);
                    }

                    // проверяем на замены внутри кода по файловым маскам
                    if (config.getReplacementInFileContent() != null
                            && config.getReplacementInFileContent().size() > 0) {
                        for (Map.Entry<String, RefactorReplacement> entry : config.getReplacementInFileContent()
                                .entrySet()) {
                            projectFileMask = entry.getValue().getFileMask();

                            if (projectFileMask.isMatchFilePatterns(fileInOriginalFolder.toFile())) {
                                // System.out.println(fileInStripFolder + " " + entry.getKey() + " " + entry.getValue()
                                //        + " " + projectFileMask);
                                originalCode = fileWorker.fileToString(fileInStripFolder);
                                modifiedCode = fileWorker.replaceInCodeString(originalCode,
                                        entry.getValue().getSource(), entry.getValue().getTarget());

                                logger.info("strip code replacements in " + fileInOriginalFolder);
                                // перезаписываем исходный файл с изменениями
                                fileWorker.stringToFile(modifiedCode, fileInStripFolder);
                                // записываем или перезаписываем патч
                                fileWorker.updateTotalPatch(
                                        "replace " + entry.getValue().getSource() + " with "
                                                + entry.getValue().getTarget() + " " + timeDumpString,
                                        originalCode, modifiedCode);
                            }
                        }
                    }
                    Files.deleteIfExists(copyOfFileInStripFolder);
                }
                filesCounter++;
                this.updateProgress(filesCounter, filesQuantity);
                this.updateMessage("strip: " + filesCounter + "/" + filesQuantity + " files");
            }
            return paths;
        } catch (

        IOException e) {
            e.printStackTrace();
            logger.error("!!! error during file strip process");
            return null;
        }
    }
}
