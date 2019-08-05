package ua.com.foxminded.sanitizer.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MavenProject extends AbstractProject {
    public MavenProject(File dir) {
        super(dir);
    }

    private boolean isValidPomXml(File mavenPomFile) {
        try {
            if (mavenPomFile != null) {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory
                        .newSchema(new StreamSource(getClass().getResourceAsStream("/schema/maven-4.0.0.xsd")));
                // проверяем через JAXB
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(mavenPomFile));
                logger.info("validate " + mavenPomFile + " " + Status.OK.getStatus());
                return true;
            }
        } catch (SAXException e) {
            logger.error("SAX error in " + mavenPomFile);
            e.printStackTrace();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            logger.error("IO error " + mavenPomFile);
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isProperProject() {
        File mavenPomFile = new File(getDir().getAbsoluteFile() + "/pom.xml");
        boolean hasPomXml = mavenPomFile.exists();
        logger.info(
                hasPomXml ? mavenPomFile + " " + Status.OK.getStatus() : mavenPomFile + " " + Status.FAIL.getStatus());
        boolean isProperPomXml = isValidPomXml(mavenPomFile);
        File srcFolder = new File(getDir().getAbsoluteFile() + "/src");
        boolean hasSrcFolder = srcFolder.exists() && (srcFolder.isDirectory());

        boolean resultOK = getDir().isDirectory() && hasSrcFolder && hasPomXml && isProperPomXml;
        if (resultOK) {
            logger.info(hasSrcFolder ? "src folder: " + srcFolder + " " + Status.OK.getStatus()
                    : "src folder: " + Status.FAIL.getStatus());
            logger.info("+++ maven project found at " + getDir());
        }
        return resultOK;
    }

    @Override
    public ImageView getProjectLabelIcon() {
        return new ImageView(new Image(getClass().getResourceAsStream("/img/project/maven.png")));
    }

}
