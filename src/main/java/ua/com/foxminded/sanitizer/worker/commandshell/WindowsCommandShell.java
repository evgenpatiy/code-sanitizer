package ua.com.foxminded.sanitizer.worker.commandshell;

public class WindowsCommandShell extends AbstractCommandShell {

    public WindowsCommandShell() {
        super();
        setJavaExecutable("java.exe");
    }

    private String runAsAdministrator(String command) {
        return runCommand("runas /profile /user:Administrator \"" + command + "\"");
    }

    @Override
    public void associateSTZFileInOS() {
        String commandLine = "cmd.exe /c assoc " + MASTER_EXT + "=" + STZ_MIME_DESCRIPTION;
        String responce = runAsAdministrator(commandLine);
    }

    @Override
    public void deAssociateSTZFileInOS() {
        String commandLine = "cmd /c assoc " + MASTER_EXT + "= ";
        String responce = runAsAdministrator(commandLine);
    }

    @Override
    public boolean isSTZFileAssociated() {
        String responce = runCommand("cmd /c assoc " + MASTER_EXT);
        if (responce.isEmpty()) {
            logger.info("STZ files not associated with sanitizer");
            return false;
        } else if (!responce.contains(STZ_MIME_DESCRIPTION)) {
            logger.info("STZ files not associated with sanitizer");
            return false;
        } else {
            // action
            return true;
        }
    }
}
