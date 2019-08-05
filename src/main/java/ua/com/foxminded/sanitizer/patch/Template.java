package ua.com.foxminded.sanitizer.patch;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@XmlRootElement(name = "diff-data")
@XmlAccessorType(XmlAccessType.FIELD)
public class Template {
    @XmlElement(name = "original-checksum")
    private long originalCRC32; // original file checksum
    @XmlElement(name = "modified-checksum")
    private long modifiedCRC32; // modified file checksum
    private Map<Long, Delta> patches;
}
