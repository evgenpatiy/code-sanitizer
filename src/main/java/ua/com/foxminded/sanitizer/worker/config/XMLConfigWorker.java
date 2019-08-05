package ua.com.foxminded.sanitizer.worker.config;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.Config;

public class XMLConfigWorker implements IConfigWorker, ISanitizerEnvironment {
    private static final Logger logger = LogManager.getLogger("sanitizer");

    @Override
    public Config readConfigData(File file, Class<?> c) {
        Config config = null;
        try {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            config = (Config) unmarshaller.unmarshal(file);
            logger.info("read config " + file.getAbsolutePath() + " " + Status.OK.getStatus());
            return config;
        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error("failure at JAXB in " + file.getAbsolutePath() + ", read config: " + Status.FAIL.getStatus());
            logger.info("--- " + file.getAbsolutePath() + " doesn't looks like config file");
            return null;
        }
    }

    @Override
    public boolean writeConfigData(File file, Config config) {
        try {
            JAXBContext context = JAXBContext.newInstance(config.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(config, System.out);
            marshaller.marshal(config, file);
            logger.info("write config " + file.getAbsolutePath() + " " + Status.OK.getStatus());
            return true;
        } catch (JAXBException e) {
            e.printStackTrace();
            logger.error("failure at JAXB in " + file.getAbsolutePath() + ", read config: " + Status.FAIL.getStatus());
            logger.info("--- " + file.getAbsolutePath() + " doesn't looks like config file");
            return false;
        }
    }
}
