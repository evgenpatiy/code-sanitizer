package ua.com.foxminded.sanitizer.worker;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.MasterProject;

public class MasterProjectWorker implements ISanitizerEnvironment {
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public MasterProject readMasterProject(File file, Class<?> c) {
        MasterProject masterProject = null;
        try {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            masterProject = (MasterProject) unmarshaller.unmarshal(file);
            logger.info("read master project " + file.getAbsolutePath() + " " + Status.OK.getStatus());
            return masterProject;
        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error("failure at JAXB in " + file.getAbsolutePath() + ", read master project: "
                    + Status.FAIL.getStatus());
            logger.info("--- " + file.getAbsolutePath() + " doesn't looks like master project meta-file");
            return null;
        }
    }

    public boolean writeMasterProject(File file, MasterProject masterProject) {
        try {
            JAXBContext context = JAXBContext.newInstance(masterProject.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(masterProject, System.out);
            marshaller.marshal(masterProject, file);
            logger.info("write master project " + file.getAbsolutePath() + " " + Status.OK.getStatus());
            return true;
        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error("failure at JAXB in " + file.getAbsolutePath() + ", read master project: "
                    + Status.FAIL.getStatus());
            logger.info("--- " + file.getAbsolutePath() + " doesn't looks like master project meta-file");
            return false;
        }
    }
}
