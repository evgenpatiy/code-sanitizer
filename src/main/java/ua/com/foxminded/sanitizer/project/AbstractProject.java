package ua.com.foxminded.sanitizer.project;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;

@RequiredArgsConstructor
public abstract class AbstractProject implements ISanitizerEnvironment {
    @NonNull
    @Getter
    private File dir;
    protected static final Logger logger = LogManager.getLogger("sanitizer");

    public abstract boolean isProperProject();

    public abstract ImageView getProjectLabelIcon();
}
