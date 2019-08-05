package ua.com.foxminded.sanitizer.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileData {
    private boolean tabs;
    private boolean comments;
    private String fileName;
    private long lines;
}
