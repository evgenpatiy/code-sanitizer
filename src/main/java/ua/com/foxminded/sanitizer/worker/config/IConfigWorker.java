package ua.com.foxminded.sanitizer.worker.config;

import java.io.File;

import ua.com.foxminded.sanitizer.data.Config;

public interface IConfigWorker {
    public Config readConfigData(File file, Class<?> c);

    public boolean writeConfigData(File file, Config config);
}
