package ua.com.foxminded.sanitizer.worker.patch;

import java.nio.file.Path;

import ua.com.foxminded.sanitizer.patch.Template;

public interface IPatchWorker {
    public Template readPatchData(Path path, Class<?> c);

    public void writePatchData(Path path, Template patchData);
}
