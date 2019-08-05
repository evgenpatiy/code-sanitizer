package ua.com.foxminded.sanitizer.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectFileMask {
    @XmlAttribute(name = "mask")
    private String filenameFilterRegexp;
    @XmlElementWrapper(name = "filters")
    @XmlElement(name = "filter")
    private List<String> filenameFilters = new ArrayList<String>();

    public boolean isMatchFilePatterns(File file) {
        boolean isMatchFileExtension = filenameFilters.stream().anyMatch(e -> file.getAbsolutePath().endsWith(e));
        boolean isMatchPattern = (filenameFilterRegexp == null) ? false
                : new WildcardFileFilter(filenameFilterRegexp).accept(file);
        return isMatchFileExtension | isMatchPattern;
    }
}
