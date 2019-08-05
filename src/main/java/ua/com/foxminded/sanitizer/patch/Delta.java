package ua.com.foxminded.sanitizer.patch;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Delta {
    private String description = "default description";
    private List<SanitizerFilePatch> deltas = new ArrayList<SanitizerFilePatch>();

    @Override
    public String toString() {
        String result = "Delta description=" + description + System.lineSeparator();
        for (SanitizerFilePatch SFP : deltas) {
            result += SFP.toString() + System.lineSeparator();
        }
        return result;
    }

}
