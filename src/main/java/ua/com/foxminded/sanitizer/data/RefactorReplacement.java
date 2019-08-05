package ua.com.foxminded.sanitizer.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class RefactorReplacement {
    @NonNull
    private String source;
    @NonNull
    private String target;
    @XmlElement(name = "refactor-replace-filemask")
    private ProjectFileMask fileMask = new ProjectFileMask();
}
