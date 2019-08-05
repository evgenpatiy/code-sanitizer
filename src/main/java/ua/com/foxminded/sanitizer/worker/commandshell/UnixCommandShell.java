package ua.com.foxminded.sanitizer.worker.commandshell;

public class UnixCommandShell extends AbstractCommandShell {

    public UnixCommandShell() {
        super();
        setJavaExecutable("java");
    }

    @Override
    public void associateSTZFileInOS() {
        System.out.println("associate stz");

    }

    @Override
    public void deAssociateSTZFileInOS() {
        System.out.println("deassociate stz");

    }

    @Override
    public boolean isSTZFileAssociated() {
        String responce = runCommand("mimetype " + MASTER_EXT);
        if (responce.isEmpty()) {
            logger.info("STZ files not associated with application");
            return false;
        } else if (!responce.contains(STZ_MIME_DESCRIPTION)) {
            logger.info("STZ files not associated with application");
            return false;
        } else {
            // action
            return true;
        }
    }
}
