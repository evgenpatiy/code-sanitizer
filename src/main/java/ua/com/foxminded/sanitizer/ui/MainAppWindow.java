package ua.com.foxminded.sanitizer.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Application.Parameters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.Config;
import ua.com.foxminded.sanitizer.data.MasterProject;
import ua.com.foxminded.sanitizer.project.AbstractProject;
import ua.com.foxminded.sanitizer.project.AngularProject;
import ua.com.foxminded.sanitizer.project.MavenProject;
import ua.com.foxminded.sanitizer.ui.elements.TextAreaAppender;
import ua.com.foxminded.sanitizer.worker.CommandLineWorker;
import ua.com.foxminded.sanitizer.worker.FileWorker;
import ua.com.foxminded.sanitizer.worker.MasterProjectWorker;
import ua.com.foxminded.sanitizer.worker.OSWorker.OS;
import ua.com.foxminded.sanitizer.worker.commandshell.AbstractCommandShell;
import ua.com.foxminded.sanitizer.worker.commandshell.UnixCommandShell;
import ua.com.foxminded.sanitizer.worker.commandshell.WindowsCommandShell;
import ua.com.foxminded.sanitizer.worker.config.IConfigWorker;
import ua.com.foxminded.sanitizer.worker.config.XMLConfigWorker;

public final class MainAppWindow implements ISanitizerWindow, ISanitizerEnvironment {
    private FileWorker fileWorker;
    @Setter
    private Config config;
    private IConfigWorker configWorker = new XMLConfigWorker();
    private MasterProject masterProject = new MasterProject();
    private CheckBox stzFileAssiciationCheckBox = new CheckBox();
    private Button openMasterProjectButton = new Button();
    private Button saveMasterProjectButton = new Button();
    private Button selectOriginalFolderButton = new Button();
    private Button selectConfigFileButton = new Button();
    private Button selectOutputFolderButton = new Button();
    private Label originalFolderStatusLabel = new Label();
    @Getter
    private Label configFileStatusLabel = new Label();
    private Label outputFolderStatusLabel = new Label();
    private Label originalInfoLabel = new Label();
    private Button editOrNewConfigButton = new Button();
    private Label outputInfoLabel = new Label();
    private File originalFolder;
    @Setter
    private File configFile;
    private File outputPreparedFolder;
    private File baseFolder;
    private File masterProjectFile;
    private Button saveLogButton = new Button();
    private Button exploreOriginalProjectFilesButton = new Button();
    private Button prepareOutputFolderButton = new Button();
    private Button stripOriginalProjectFilesButton = new Button();
    private Button undoStrippedProjectFilesButton = new Button();
    private Button stripUnstripButton = new Button();
    private boolean isOriginalFolderSelected;
    @Setter
    private boolean isProperConfigFileSelected;
    @Setter
    private boolean isOutputFolderPrepared;
    private boolean isOutputFolderSelected;
    private boolean isMasterProjectFileUsed;
    private String title;
    private long size;
    private int files;
    private Parameters parameters;
    private AbstractCommandShell commandShell;
    private final TextArea loggingView = new TextArea();
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public MainAppWindow(Parameters parameters) {
        super();
        this.parameters = parameters;
        title = "sanitizer";
        TextAreaAppender.setTextArea(loggingView);

        if (ENV == OS.WINDOWS) {
            commandShell = new WindowsCommandShell();
        } else if (ENV == OS.UNIX) {
            commandShell = new UnixCommandShell();
        } else {
            commandShell = null;
        } // determine operating system
        originalFolderStatusLabel
                .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
        configFileStatusLabel
                .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
        outputFolderStatusLabel
                .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
    }

    protected void checkAndToggleButtons() {
        selectOriginalFolderButton.setDisable(isMasterProjectFileUsed);
        selectConfigFileButton.setDisable(isMasterProjectFileUsed);
        selectOutputFolderButton.setDisable(isMasterProjectFileUsed);

        exploreOriginalProjectFilesButton.setDisable(!(isOriginalFolderSelected) | isMasterProjectFileUsed);
        prepareOutputFolderButton
                .setDisable(!(isOriginalFolderSelected && isOutputFolderSelected && isProperConfigFileSelected)
                        | isMasterProjectFileUsed);
        stripOriginalProjectFilesButton.setDisable(!isProperConfigFileSelected | isMasterProjectFileUsed);
        undoStrippedProjectFilesButton.setDisable(!isProperConfigFileSelected | isMasterProjectFileUsed);
        editOrNewConfigButton
                .setDisable(!(isOriginalFolderSelected && isOutputFolderSelected) | isMasterProjectFileUsed);
        saveMasterProjectButton
                .setDisable(!(isOriginalFolderSelected && isProperConfigFileSelected && isOutputFolderSelected));
        stripUnstripButton.setDisable(!isMasterProjectFileUsed);
    }

    @Override
    public void setMessages() {
        openMasterProjectButton.setText("Open master project");
        saveMasterProjectButton.setText("Save master project");
        selectOriginalFolderButton.setText("Original project folder");
        selectConfigFileButton.setText("Select config file");
        selectOutputFolderButton.setText("Output project folder");
        originalFolderStatusLabel.setText("not selected");
        configFileStatusLabel.setText("not selected");
        outputFolderStatusLabel.setText("not selected");
        saveLogButton.setText("Save log");
        exploreOriginalProjectFilesButton.setText("Explore original project");
        prepareOutputFolderButton.setText("Prepare output folder");
        stripOriginalProjectFilesButton.setText("Strip original project");
        undoStrippedProjectFilesButton.setText("Undo strip steps");
        editOrNewConfigButton.setText("Edit or new config");
        stripUnstripButton.setText("Strip");
    }

    private void fillOriginalFolderLabelsLine(Stage stage) {
        // выясняем, что за проект
        AbstractProject project = new MavenProject(originalFolder).isProperProject() ? new MavenProject(originalFolder)
                : (new AngularProject(originalFolder).isProperProject() ? new AngularProject(originalFolder) : null);

        if (project != null) {
            processDirectory(originalFolder.toPath());
            originalInfoLabel.setText("Size: " + fileWorker.turnFileSizeToString(size) + " / Files: " + files);
            logger.info("original project root folder: " + originalFolder.getAbsolutePath());
            originalFolderStatusLabel.setText("project at " + originalFolder.getName() + " " + Status.OK.getStatus());
            originalFolderStatusLabel.setGraphic(project.getProjectLabelIcon());
            stage.setTitle(stage.getTitle() + " " + originalFolder.getAbsolutePath());
            isOriginalFolderSelected = true;
        } else {
            originalFolderStatusLabel.setText("ordinary directory");
            originalFolderStatusLabel
                    .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
            logger.warn("no proper projects here: " + originalFolder.toString());
            isOriginalFolderSelected = false;
            stage.setTitle(title);
        }
    }

    private void fillConfigFileLabelsLine(Stage stage) {
        if (config != null) {
            configFileStatusLabel
                    .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/ok.png"))));
            configFileStatusLabel.setText(configFile.getAbsolutePath());
            isProperConfigFileSelected = true;
            logger.info("+++ " + configFile.getName() + " is proper sanitizer config");

            // проверяем пути исходной и выходной папок
            if (originalFolder == null || outputPreparedFolder == null) {
                originalFolder = config.getOriginalProject();
                outputPreparedFolder = config.getOutputProject();
                fillOriginalFolderLabelsLine(stage);
                fillOutputFolderLabelsLine();
            } else if (!config.getOriginalProject().getAbsolutePath().equalsIgnoreCase(originalFolder.getAbsolutePath())
                    || !config.getOutputProject().getAbsolutePath()
                            .equalsIgnoreCase(outputPreparedFolder.getAbsolutePath())) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Folders error");
                alert.setHeaderText("Original or output folders doesn't match saved properties");
                alert.setContentText("Folders you've chose will be replaced ones from config file!");
                alert.showAndWait();
                originalFolder = config.getOriginalProject();
                outputPreparedFolder = config.getOutputProject();
                fillOriginalFolderLabelsLine(stage);
                fillOutputFolderLabelsLine();
            }
        } else {
            configFileStatusLabel
                    .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
            isProperConfigFileSelected = false;
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Config error");
            alert.setHeaderText(configFile.getName() + " doesn't looks like proper sanitizer config");
            alert.setContentText("Use editor");
            alert.showAndWait();
        }
    }

    private void fillOutputFolderLabelsLine() {
        // обрабатываем выходную папку
        if (originalFolder != null && outputPreparedFolder.getAbsolutePath().equals(originalFolder.getAbsolutePath())) {
            logger.error("wrong output project folder selected!");
            isOutputFolderSelected = false;
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Wrong folder selected!");
            alert.setHeaderText("Original and output folder couldn't be the same");
            alert.setContentText("Choose another output or original project folder");
            alert.showAndWait();
        } else {
            if (outputPreparedFolder.getFreeSpace() > size) {
                outputFolderStatusLabel.setText(outputPreparedFolder.getAbsolutePath());
                outputFolderStatusLabel
                        .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/ok.png"))));
                logger.info("select output project folder " + outputPreparedFolder.getAbsolutePath());
                outputInfoLabel
                        .setText("Free space: " + fileWorker.turnFileSizeToString(outputPreparedFolder.getFreeSpace()));
                isOutputFolderSelected = true;
            } else {
                outputFolderStatusLabel.setText(outputPreparedFolder.getAbsolutePath());
                outputFolderStatusLabel
                        .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
                logger.error("!!! not enough space in " + outputPreparedFolder.getAbsolutePath());
                outputInfoLabel.setText("Not enough space!");
                isOutputFolderSelected = false;
            }
        }
    }

    public void loadMasterProject(Stage stage) {
        if (masterProjectFile != null) {
            masterProject = new MasterProjectWorker().readMasterProject(masterProjectFile, MasterProject.class);
            originalFolder = masterProject.getOriginalProjectFolder();
            configFile = masterProject.getConfigFile();
            outputPreparedFolder = masterProject.getOutputPreparedFolder();
            config = configWorker.readConfigData(configFile, Config.class);

            fillOriginalFolderLabelsLine(stage);
            fillConfigFileLabelsLine(stage);
            fillOutputFolderLabelsLine();
            logger.info("*** opened master project meta-file " + masterProjectFile);
            isMasterProjectFileUsed = true;
        } else {
            logger.info("!!! open master project meta-file cancelled");
            isMasterProjectFileUsed = isMasterProjectFileUsed ? true : false;
        }
    }

    @Override
    public void setButtonsActions(Stage stage) {
        fileWorker = new FileWorker();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        FileChooser fileChooser = new FileChooser();

        stzFileAssiciationCheckBox.setOnAction(event -> {
            if (stzFileAssiciationCheckBox.isSelected()) {
                stzFileAssiciationCheckBox.setText("STZ master project files associated with sanitizer");
                commandShell.associateSTZFileInOS();
            } else {
                stzFileAssiciationCheckBox.setText("STZ master project files not associated with sanitizer");
                commandShell.deAssociateSTZFileInOS();
            }
        });

        saveLogButton.setOnAction(event -> {
            fileChooser.setTitle("Select .log file to save log");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LOG_DIALOG_NAME, LOG_PATTERN));
            fileChooser.setInitialFileName("sanitizer-" + fileWorker.getCurrentDateTimeString() + LOG_EXT);
            File logFile = fileChooser.showSaveDialog(stage);
            if (logFile != null) {
                fileWorker.stringToFile(loggingView.getText(), logFile.toPath());
            }
        });

        stripUnstripButton.setOnAction(event -> {
            System.out.println("click strip unstrip");
        });

        openMasterProjectButton.setOnAction(event -> {
            logger.info("trying open master project meta-file...");
            fileChooser.setTitle("Select master project meta-file");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(MASTER_DIALOG_NAME, MASTER_PATTERN));
            masterProjectFile = fileChooser.showOpenDialog(stage);
            loadMasterProject(stage);
            checkAndToggleButtons();
        });
        saveMasterProjectButton.setOnAction(event -> {
            logger.info("trying save master project meta-file...");
            fileChooser.setTitle("Select master project meta-file");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(MASTER_DIALOG_NAME, MASTER_PATTERN));
            fileChooser.setInitialFileName(
                    originalFolder.getName() + "-" + fileWorker.getCurrentDateTimeString() + MASTER_EXT);
            masterProjectFile = fileChooser.showSaveDialog(stage);
            if (masterProjectFile != null) {
                masterProject.setOriginalProjectFolder(originalFolder);
                masterProject.setOutputPreparedFolder(outputPreparedFolder);
                masterProject.setConfigFile(configFile);
                new MasterProjectWorker().writeMasterProject(masterProjectFile, masterProject);
                logger.info("*** master project meta-file saved to " + masterProjectFile);
            } else {
                logger.info("!!! open master project meta-file cancelled");
            }
        });
        selectOriginalFolderButton.setOnAction(event -> {
            logger.info("trying select original project root folder...");
            directoryChooser.setTitle("Select original project root folder");
            originalFolder = directoryChooser.showDialog(stage);
            if (originalFolder != null) {
                if (outputPreparedFolder != null
                        && outputPreparedFolder.getAbsolutePath().equals(originalFolder.getAbsolutePath())) {
                    logger.info("wrong original project folder selected!");
                    isOriginalFolderSelected = false;
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Wrong folder selected!");
                    alert.setHeaderText("Original and output folder couldn't be the same");
                    alert.setContentText("Choose another output or original project folder");
                    alert.showAndWait();
                } else {
                    logger.info("select original project root folder");
                    fillOriginalFolderLabelsLine(stage);
                }
            } else {
                originalFolderStatusLabel.setText("cancel select");
                originalFolderStatusLabel
                        .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
                logger.info("cancel select original project root folder");
                if (!isOriginalFolderSelected) {
                    isOriginalFolderSelected = false;
                }
            }
            checkAndToggleButtons();
        });
        selectConfigFileButton.setOnAction(event -> {
            logger.info("trying select config file...");
            fileChooser.setTitle("Select project config file");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(XML_DIALOG_NAME, XML_PATTERN));
            configFile = fileChooser.showOpenDialog(stage);
            if (configFile != null) {
                config = configWorker.readConfigData(configFile, Config.class);
                fillConfigFileLabelsLine(stage);
            } else {
                configFileStatusLabel.setText("cancel select");
                configFileStatusLabel
                        .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
                logger.info("cancel select config file");
                if (!isProperConfigFileSelected) {
                    isProperConfigFileSelected = false;
                }
            }
            checkAndToggleButtons();
        });
        selectOutputFolderButton.setOnAction(event -> {
            logger.info("trying select output project folder...");
            directoryChooser.setTitle("Select output project root folder");
            outputPreparedFolder = directoryChooser.showDialog(stage);
            if (outputPreparedFolder != null) {
                fillOutputFolderLabelsLine();
            } else {
                outputFolderStatusLabel.setText("cancel select");
                outputFolderStatusLabel
                        .setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
                logger.info("cancel select output project folder");
                if (!isOutputFolderSelected) {
                    isOutputFolderSelected = false;
                }
            }
            checkAndToggleButtons();
        });
        exploreOriginalProjectFilesButton.setOnAction(event -> new ExploreProjectWindow(originalFolder).show());
        prepareOutputFolderButton.setOnAction(event -> {
            if (!outputPreparedFolder.getAbsolutePath().equalsIgnoreCase(config.getOutputProject().getAbsolutePath())) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Folders mismatch!");
                alert.setHeaderText("Chosen output folder doesn't match saved to config");
                alert.setContentText("Fix before continue...");
                alert.showAndWait();
            } else if (!originalFolder.getAbsolutePath()
                    .equalsIgnoreCase(config.getOriginalProject().getAbsolutePath())) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Folders mismatch!");
                alert.setHeaderText("Chosen original folder doesn't match saved to config");
                alert.setContentText("Fix before continue...");
                alert.showAndWait();
            } else if (!fileWorker.isContainProperOriginalFolder(outputPreparedFolder)) {
                logger.info("prepare output folder " + outputPreparedFolder);
                new PrepareWindow(originalFolder, outputPreparedFolder, config, this).show();
            } else {
                logger.warn("!!! " + outputPreparedFolder
                        + " already contains proper original project folder, choose another one!");
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Wrong folder!");
                alert.setHeaderText(outputPreparedFolder.toString());
                alert.setContentText("already contains proper original project folder, " + System.lineSeparator()
                        + "choose another one");
                alert.showAndWait();
            }
        });
        stripOriginalProjectFilesButton.setOnAction(event -> {
            directoryChooser.setTitle("Select base work folder");
            baseFolder = directoryChooser.showDialog(stage);
            if (baseFolder != null) {
                if (fileWorker.isContainProperOriginalFolder(baseFolder)) {
                    logger.info("+++ strip files according to config " + configFile.getAbsolutePath());
                    new StripWindow(
                            new File(fileWorker.getProperOriginalFolderName(baseFolder)), new File(fileWorker
                                    .getProperOriginalFolderName(baseFolder).replaceAll(ORIG_SUFFIX, STRIP_SUFFIX)),
                            config).show();
                } else {
                    logger.info("--- no proper original project folder found at " + baseFolder
                            + ". prepare project in advance");
                }
            } else {
                logger.info("--- strip files cancelled");
            }
        });
        undoStrippedProjectFilesButton.setOnAction(event -> {
            directoryChooser.setTitle("Select base work folder");
            baseFolder = directoryChooser.showDialog(stage);
            if (baseFolder != null) {
                if (fileWorker.isContainProperOriginalFolder(baseFolder)
                        && fileWorker.isContainProperStripFolder(baseFolder)) {
                    logger.info("*** start undo operations in " + baseFolder + " using config " + configFile);
                    new UndoSelectWindow(baseFolder, config).show();
                } else {
                    logger.error("--- no proper original and strip project folder found at " + baseFolder);
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Wrong folder!");
                    alert.setHeaderText(baseFolder.toString());
                    alert.setContentText("no proper original and strip project here " + System.lineSeparator()
                            + "choose another one");
                    alert.showAndWait();
                }
            } else {
                logger.info("--- undo cancelled");
            }
        });
        editOrNewConfigButton.setOnAction(event -> {
            ConfigEditorWindow configEditor;
            if (configFile != null) {
                logger.info("load " + configFile.getAbsolutePath() + " to config editor");
                config = configWorker.readConfigData(configFile, Config.class);
                if (config != null) {
                    configEditor = new ConfigEditorWindow(config, configFile, originalFolder, outputPreparedFolder);
                    configEditor.setMainAppWindow(this);
                    configEditor.show();
                } else {
                    Alert alert = new Alert(AlertType.WARNING, configFile.getName() + " not a config. Run new config?",
                            ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> option = alert.showAndWait();
                    if (option.get() == ButtonType.YES) {
                        configEditor = new ConfigEditorWindow(originalFolder, outputPreparedFolder);
                        configEditor.setMainAppWindow(this);
                        configEditor.show();
                    } else {
                        logger.info("cancel new config");
                    }
                }
            } else {
                configEditor = new ConfigEditorWindow(originalFolder, outputPreparedFolder);
                configEditor.setMainAppWindow(this);
                configEditor.show();
            }
        });
    }

    @Override
    public void show() {
        FlowPane stzCheckBoxPane = new FlowPane();
        stzCheckBoxPane.setAlignment(Pos.CENTER_LEFT);
        stzCheckBoxPane.getChildren().add(stzFileAssiciationCheckBox);
        stzCheckBoxPane.getChildren()
                .forEach(element -> FlowPane.setMargin(element, new Insets(ISanitizerWindow.INSET)));

        FlowPane topMasterProjectButtonsPane = new FlowPane();
        topMasterProjectButtonsPane.setAlignment(Pos.BASELINE_RIGHT);
        openMasterProjectButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/code.png"))));
        topMasterProjectButtonsPane.getChildren().add(openMasterProjectButton);
        saveMasterProjectButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/img/code.png"))));
        topMasterProjectButtonsPane.getChildren().add(saveMasterProjectButton);
        topMasterProjectButtonsPane.getChildren()
                .forEach(element -> FlowPane.setMargin(element, new Insets(ISanitizerWindow.INSET)));

        GridPane topMasterProjectPane = new GridPane();
        topMasterProjectPane.add(stzCheckBoxPane, 0, 0);
        topMasterProjectPane.add(topMasterProjectButtonsPane, 1, 0);
        topMasterProjectPane.setId("topPane");

        GridPane topProjectConfigPane = new GridPane();
        ColumnConstraints buttonsLeftColumn = new ColumnConstraints();
        buttonsLeftColumn.setPercentWidth(25);
        topProjectConfigPane.getColumnConstraints().add(buttonsLeftColumn);
        ColumnConstraints statusLabelColumn = new ColumnConstraints();
        statusLabelColumn.setPercentWidth(50);
        topProjectConfigPane.getColumnConstraints().add(statusLabelColumn);
        ColumnConstraints buttonsRightColumn = new ColumnConstraints();
        buttonsRightColumn.setPercentWidth(25);
        topProjectConfigPane.getColumnConstraints().add(buttonsRightColumn);

        topProjectConfigPane.setGridLinesVisible(false);
        topProjectConfigPane.add(selectOriginalFolderButton, 0, 0);
        topProjectConfigPane.add(selectConfigFileButton, 0, 1);
        topProjectConfigPane.add(selectOutputFolderButton, 0, 2);
        topProjectConfigPane.add(originalFolderStatusLabel, 1, 0);
        topProjectConfigPane.add(configFileStatusLabel, 1, 1);
        topProjectConfigPane.add(outputFolderStatusLabel, 1, 2);
        topProjectConfigPane.add(originalInfoLabel, 2, 0);
        topProjectConfigPane.add(editOrNewConfigButton, 2, 1);
        topProjectConfigPane.add(outputInfoLabel, 2, 2);

        topProjectConfigPane.getChildren().forEach(element -> {
            GridPane.setMargin(element, new Insets(INSET));
            if (element instanceof Button) {
                ((Button) element).setMaxWidth(220);
            }
        });

        GridPane topPane = new GridPane();
        ColumnConstraints mainColumn = new ColumnConstraints();
        mainColumn.setPercentWidth(100);
        topPane.getColumnConstraints().add(mainColumn);
        topPane.add(topMasterProjectPane, 0, 0);
        topPane.add(topProjectConfigPane, 0, 1);

        StackPane logPane = new StackPane();
        //logPane.getChildren().add(getLogTextArea());
        logPane.getChildren().add(loggingView);

        exploreOriginalProjectFilesButton.setDisable(true);
        prepareOutputFolderButton.setDisable(true);
        stripOriginalProjectFilesButton.setDisable(true);
        undoStrippedProjectFilesButton.setDisable(true);
        editOrNewConfigButton.setDisable(true);
        saveMasterProjectButton.setDisable(true);
        stripUnstripButton.setDisable(true);

        FlowPane bottomPane = new FlowPane();
        bottomPane.setAlignment(Pos.CENTER);
        bottomPane.setId("bottomPane");
        bottomPane.getChildren().addAll(saveLogButton, exploreOriginalProjectFilesButton, prepareOutputFolderButton,
                stripOriginalProjectFilesButton);
        bottomPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(INSET)));

        BorderPane root = new BorderPane();
        root.setTop(topPane);
        root.setCenter(logPane);
        root.setBottom(bottomPane);

        if (commandShell != null & commandShell.isSystemEnvironmentOK()) { // ассоциации файлов в поддерживаемых ОС
            if (commandShell.isSTZFileAssociated()) {
                stzFileAssiciationCheckBox.setSelected(true);
                stzFileAssiciationCheckBox.setText("STZ master project files associated with sanitizer");
            } else {
                stzFileAssiciationCheckBox.setSelected(false);
                stzFileAssiciationCheckBox.setText("STZ master project files not associated with sanitizer");
            }
        } else {
            stzFileAssiciationCheckBox.setDisable(true);
            stzFileAssiciationCheckBox.setSelected(false);
            stzFileAssiciationCheckBox.setText("check file associations environment failure!");
        }

        setMessages();
        Stage stage = new Stage();
        stage.setOnCloseRequest(event -> logger.info("finish all jobs, exit"));
        setButtonsActions(stage);

        String masterProjectFromCommandLine = new CommandLineWorker(parameters).getMasterProjectFile();
        if (masterProjectFromCommandLine != null) {
            masterProjectFile = new File(masterProjectFromCommandLine);
            if (masterProjectFile.exists()) {
                logger.info("open master project " + masterProjectFile.getAbsolutePath());
                loadMasterProject(stage);
            } else {
                logger.error("fail load master project " + masterProjectFile.getAbsolutePath());
            }
            checkAndToggleButtons();
        }
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));
        stage.setScene(new Scene(root, MAIN_W, MAIN_H));
        stage.setTitle(title);
        stage.show();
    }

    private void processDirectory(Path dir) {
        size = 0;
        files = 0;

        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> result = walk.collect(Collectors.toList());
            for (Path path : result) {
                if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    files++;
                    size += path.toFile().length();
                }
            }
        } catch (IOException e) {
            logger.error("IO error during process file tree in " + dir);
        }
    }
}
