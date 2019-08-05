package ua.com.foxminded.sanitizer.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class RemoveComment {
    @XmlAttribute(name = "remove")
    private boolean isToRemove;
    private String contain;
    @XmlElement(name = "remove-comment-filemask")
    private ProjectFileMask fileMask = new ProjectFileMask();
}
