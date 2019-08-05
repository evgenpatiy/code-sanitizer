package ua.com.foxminded.sanitizer.patch;

import com.github.difflib.patch.DeltaType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SanitizerFilePatch {
    private DeltaType type;
    private Source source = new Source();
    private Target target = new Target();
}
