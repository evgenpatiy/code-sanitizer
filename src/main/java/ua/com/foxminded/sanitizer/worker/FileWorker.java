package ua.com.foxminded.sanitizer.worker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;

import lombok.Setter;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.patch.Delta;
import ua.com.foxminded.sanitizer.patch.SanitizerFilePatch;
import ua.com.foxminded.sanitizer.patch.Template;
import ua.com.foxminded.sanitizer.worker.patch.XMLPatchWorker;

public class FileWorker implements ISanitizerEnvironment {
    @Setter
    private Path originalFile;
    @Setter
    private Path modifiedFile;
    @Setter
    private Path patchFile;
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public String getCurrentDateTimeString() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
    }

    public String getFileContentType(File file) throws IOException {
        return new Tika().detect(file);
    }

    public String getFileTime(File file) throws IOException {
        FileTime time = Files.getLastModifiedTime(Paths.get(file.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
        return DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss").format(time.toInstant().atZone(ZoneId.of("UTC")));
    }

    public String getPermissions(Set<PosixFilePermission> perm) {
        String s = "-";

        if (perm.contains(PosixFilePermission.OWNER_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OWNER_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OWNER_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        s += "/";
        if (perm.contains(PosixFilePermission.GROUP_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.GROUP_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.GROUP_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        s += "/";

        if (perm.contains(PosixFilePermission.OTHERS_READ)) {
            s += "r";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OTHERS_WRITE)) {
            s += "w";
        } else {
            s += "-";
        }
        if (perm.contains(PosixFilePermission.OTHERS_EXECUTE)) {
            s += "x";
        } else {
            s += "-";
        }
        return s;
    }

    public String getProperOriginalFolderName(File dir) {
        return Arrays.stream(dir.listFiles()).filter(d -> d.getName().endsWith(ORIG_SUFFIX)).findFirst().get()
                .getAbsolutePath();
    }

    public boolean isContainProperOriginalFolder(File dir) {
        return Arrays.stream(dir.listFiles()).anyMatch(d -> (d.isDirectory() && d.getName().endsWith(ORIG_SUFFIX)));
    }

    public boolean isContainProperStripFolder(File dir) {
        return Arrays.stream(dir.listFiles()).anyMatch(d -> (d.isDirectory() && d.getName().endsWith(STRIP_SUFFIX)));
    }

    public String turnFileSizeToString(final long value) {
        final int BYTES = 1024;
        long[] dividers = new long[] { (long) Math.pow(BYTES, 3), (long) Math.pow(BYTES, 2), (long) Math.pow(BYTES, 1),
                (long) Math.pow(BYTES, 0) };
        String[] units = new String[] { "Gb", "Mb", "Kb", "b" };
        String result = "";
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private String format(long value, long divider, String unit) {
        double result = divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }

    public String fileToString(Path path) { // fast file reader
        String code = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toString()))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                code += line + System.lineSeparator();
            }
        } catch (IOException e) {
            logger.error("!!! file read error at " + path.toString());
        }
        return code;
    }

    private List<String> codeToStringsList(String code) {
        return Arrays.asList(code.split(System.lineSeparator()));
    }

    public void stringToFile(String code, Path path) { // fast file writer
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toString(), false))) {
            writer.write(code);
        } catch (IOException e) {
            logger.error("!!! file write error at " + path.toString());
        }
    }

    public String fixTabsInCodeString(String code) {
        final String tabReplacer = "    ";
        final char tab = '\u0009';
        return code.replaceAll(String.valueOf(tab), tabReplacer);
    }

    public String replaceInCodeString(String code, String original, String target) {
        return code.replaceAll(original, target);
    }

    private long getCheckSum(Path path) {
        long result = 0;

        try (CheckedInputStream check = new CheckedInputStream(new FileInputStream(path.toFile()), new CRC32());
                BufferedInputStream in = new BufferedInputStream(check)) {
            while (in.read() != -1) {
            }
            result = check.getChecksum().getValue();
        } catch (IOException e) {
            logger.error("error in get checksum method in " + path);
            e.printStackTrace();
        }
        return result;
    }

    public String removeCommentsFromProperties(String code) {
        return code.replaceAll("([\\t]*#.*)|(=.*)", "$1 ");
    }

    public String removeCommentsFromCss(String code) {
        return code.replaceAll(
                "(/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/)|\"(\\\\.|[^\\\\\"])*\"|'(\\\\[\\s\\S]|[^'])*'",
                "$1 ");
    }

    public String removeCommentsFromTs(String code) {
        return code.replaceAll("<!--(?!\\\\s*(?:\\\\[if [^\\\\]]+]|<!|>))(?:(?!-->)(.|\\\\n))*-->", "$1 ");
    }

    public String removeCommentsFromXml(String code) {
        return code.replaceAll("<!--(?!\\\\s*(?:\\\\[if [^\\\\]]+]|<!|>))(?:(?!-->)(.|\\\\n))*-->", "$1 ");
    }

    public String removeCommentsFromJava(String code) {
        return code.replaceAll("//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1 ");
    }

    public Template getTotalPatchFromDiff(String currentPatchDescription, String originalCode, String modifiedCode)
            throws DiffException { // текущие изменения по сравнению с
        // оригиналом
        List<String> original = codeToStringsList(originalCode);
        List<String> revised = codeToStringsList(modifiedCode);
        Patch<String> diff = DiffUtils.diff(original, revised);
        Template totalFilePatch = new Template(); // весь патч со всеми изменениями за все время
        Map<Long, Delta> patches = new LinkedHashMap<Long, Delta>(); // мапа всех отдельных дельт (сеансов)
        Delta delta = new Delta(); // каждая дельта - список изменений в файле до сохранения

        diff.getDeltas().forEach(d -> { // маппим дифф файла на свой класс
            SanitizerFilePatch sfp = new SanitizerFilePatch(); // для marshal-unmarshal
            sfp.setType(d.getType());
            sfp.getSource().setLines(d.getSource().getLines());
            sfp.getSource().setPosition(d.getSource().getPosition());
            sfp.getTarget().setLines(d.getTarget().getLines());
            sfp.getTarget().setPosition(d.getTarget().getPosition());
            delta.getDeltas().add(sfp);
        });
        delta.setDescription(currentPatchDescription);
        totalFilePatch.setOriginalCRC32(getCheckSum(originalFile));
        totalFilePatch.setModifiedCRC32(getCheckSum(modifiedFile));
        patches.put(getCheckSum(modifiedFile), delta);
        totalFilePatch.setPatches(patches);
        return totalFilePatch;
    }

    public void updateTotalPatch(String currentPatchDescription, String originalCode, String modifiedCode)
            throws DiffException { // берем предыдущие изменения и добавляем текущий
        // snapshot
        Template totalFilePatch;
        long modifiedFileCRC32;
        Map<Long, Delta> newPatches;

        if (Files.exists(patchFile, LinkOption.NOFOLLOW_LINKS)) { // берем предыдущий патч целиком
            totalFilePatch = new XMLPatchWorker().readPatchData(patchFile, Template.class);
            modifiedFileCRC32 = getCheckSum(modifiedFile);

            if ((totalFilePatch != null) && (totalFilePatch.getModifiedCRC32() != modifiedFileCRC32)) {
                Template previousPatchData = totalFilePatch;
                totalFilePatch = getTotalPatchFromDiff(currentPatchDescription, originalCode, modifiedCode);
                newPatches = totalFilePatch.getPatches();
                newPatches.putAll(previousPatchData.getPatches()); // объединяем предыдущие патчи с текущим
                totalFilePatch.setPatches(newPatches);
            }
        } else {
            totalFilePatch = getTotalPatchFromDiff(currentPatchDescription, originalCode, modifiedCode);
            newPatches = totalFilePatch.getPatches();
            totalFilePatch.setPatches(newPatches);
        }
        new XMLPatchWorker().writePatchData(patchFile, totalFilePatch);
    }
}
