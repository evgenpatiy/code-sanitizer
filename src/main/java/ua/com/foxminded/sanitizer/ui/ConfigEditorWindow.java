package ua.com.foxminded.sanitizer.ui;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.data.Config;
import ua.com.foxminded.sanitizer.data.RefactorReplacement;
import ua.com.foxminded.sanitizer.ui.elements.FilesSelectorHBox;
import ua.com.foxminded.sanitizer.ui.elements.RefactorReplacementPane;
import ua.com.foxminded.sanitizer.worker.config.IConfigWorker;
import ua.com.foxminded.sanitizer.worker.config.XMLConfigWorker;

@RequiredArgsConstructor
public class ConfigEditorWindow implements ISanitizerWindow, ISanitizerEnvironment {
    @Getter
    @NonNull
    private Config config;
    private IConfigWorker configWorker = new XMLConfigWorker();
    @NonNull
    private File configFile;
    @NonNull
    private File originalProject;
    @NonNull
    private File outputProject;
    private Status operationStatus;
    private Label projectFoldersLabel = new Label();
    private Button newConfigButton = new Button();
    private Button saveConfigButton = new Button();
    private Button cancelButton = new Button();
    private Button addContentReplacementButton = new Button();
    private Button addFileSystemReplacementButton = new Button();
    private RefactorReplacementPane contentReplacementPane = new RefactorReplacementPane();
    private RefactorReplacementPane filesystemReplacementPane = new RefactorReplacementPane();
    private CheckBox removeCommentsCheckBox = new CheckBox();
    private CheckBox ifCommentContainCheckBox = new CheckBox();
    private TextField ifCommentContainTextField = new TextField();
    private HBox removeCommentsFileSettingsBox = new HBox();
    private FilesSelectorHBox removeCommentsFilePatternSelectorBox = new FilesSelectorHBox();
    @Setter
    private MainAppWindow mainAppWindow;
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public ConfigEditorWindow(File originalProject, File outputProject) {
        super();
        this.originalProject = originalProject;
        this.outputProject = outputProject;
    }

    @Override
    public void show() {
        setMessages();
        GridPane topPane = new GridPane();
        topPane.setId("topPane");
        ColumnConstraints mainColumn = new ColumnConstraints();
        mainColumn.setPercentWidth(100);
        topPane.getColumnConstraints().add(mainColumn);

        HBox ifContainHBox = new HBox();
        ifContainHBox.setAlignment(Pos.BASELINE_CENTER);
        ifCommentContainTextField.setEditable(false);
        ifContainHBox.getChildren().addAll(ifCommentContainCheckBox, ifCommentContainTextField);

        FlowPane removeCommentsPane = new FlowPane();
        removeCommentsFileSettingsBox.getChildren().addAll(ifContainHBox, removeCommentsFilePatternSelectorBox);
        removeCommentsPane.getChildren().addAll(removeCommentsCheckBox, removeCommentsFileSettingsBox);
        removeCommentsPane.setAlignment(Pos.CENTER);
        removeCommentsPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(0, INSET, 0, INSET)));

        HBox projectFolderBox = new HBox();
        projectFolderBox.setAlignment(Pos.BASELINE_CENTER);
        projectFolderBox.getChildren().add(projectFoldersLabel);
        topPane.add(projectFolderBox, 0, 0);
        topPane.add(removeCommentsPane, 0, 1);
        topPane.getChildren().forEach(node -> GridPane.setMargin(node, new Insets(INSET / 2, 0, INSET / 2, 0)));

        BorderPane centerPane = new BorderPane();
        FlowPane centerTopButtonsPane = new FlowPane();
        centerTopButtonsPane.setAlignment(Pos.BASELINE_CENTER);
        centerTopButtonsPane.getChildren().addAll(addContentReplacementButton, addFileSystemReplacementButton);
        centerTopButtonsPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(INSET)));
        centerPane.setTop(centerTopButtonsPane);
        SplitPane splitCenterPane = new SplitPane();
        splitCenterPane.setOrientation(Orientation.VERTICAL);
        splitCenterPane.getItems().addAll(contentReplacementPane, filesystemReplacementPane);
        centerPane.setCenter(splitCenterPane);

        FlowPane bottomButtonsPane = new FlowPane();
        bottomButtonsPane.setAlignment(Pos.CENTER);
        bottomButtonsPane.setId("bottomPane");
        bottomButtonsPane.getChildren().addAll(newConfigButton, saveConfigButton, cancelButton);
        bottomButtonsPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(INSET)));

        BorderPane root = new BorderPane();
        root.setTop(topPane);
        root.setCenter(centerPane);
        root.setBottom(bottomButtonsPane);
        Stage stage = new Stage();
        setButtonsActions(stage);
        stage.setOnCloseRequest(event -> {
            logger.info("cancel config");
        });

        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));
        stage.setScene(new Scene(root, CONFIGEDITOR_W, CONFIGEDITOR_H));

        removeCommentsFileSettingsBox.setDisable(true);
        if (config != null && configFile != null) {
            loadConfigData();
            stage.setTitle("Edit config " + configFile.getAbsolutePath());
            logger.info("edit config " + configFile.getAbsolutePath());
        } else {
            stage.setTitle("New config file");
            logger.info("new config file started");
        }
        stage.show();
    }

    public void loadConfigData() {
        removeCommentsFilePatternSelectorBox.setFileMaskData(config.getRemoveComment().getFileMask());
        removeCommentsCheckBox.setSelected(config.getRemoveComment().isToRemove());
        removeCommentsFileSettingsBox.setDisable(!config.getRemoveComment().isToRemove());
        logger.info("...load remove comments feature: " + Status.OK.getStatus());

        if (config.getRemoveComment().getContain() != null
                && !config.getRemoveComment().getContain().equalsIgnoreCase("")) {
            ifCommentContainCheckBox.setSelected(true);
            ifCommentContainTextField.setText(config.getRemoveComment().getContain());
            logger.info("...load remove comments if contain feature: " + Status.OK.getStatus());
        }

        if (config.getReplacementInFileContent() != null
                && config.getReplacementInFileContent().entrySet().size() > 0) {
            config.getReplacementInFileContent().entrySet().stream().forEach(entry -> {
                RefactorReplacement refactorReplacement = new RefactorReplacement();
                refactorReplacement.setSource(entry.getValue().getSource());
                refactorReplacement.setTarget(entry.getValue().getTarget());
                refactorReplacement.setFileMask(entry.getValue().getFileMask());
                contentReplacementPane.addReplacementItem(contentReplacementPane.new RefactorReplacementItem(
                        entry.getKey(), refactorReplacement, contentReplacementPane));
            });
            operationStatus = Status.OK;
        } else {
            operationStatus = Status.FAIL;
        }
        logger.info("...load per-file replacements: " + operationStatus.getStatus());

        if (config.getReplacementInProjectStructure() != null
                && config.getReplacementInProjectStructure().entrySet().size() > 0) {
            config.getReplacementInProjectStructure().entrySet().stream().forEach(entry -> {
                RefactorReplacement refactorReplacement = new RefactorReplacement();
                refactorReplacement.setSource(entry.getValue().getSource());
                refactorReplacement.setTarget(entry.getValue().getTarget());
                refactorReplacement.setFileMask(entry.getValue().getFileMask());

                filesystemReplacementPane.addReplacementItem(filesystemReplacementPane.new RefactorReplacementItem(
                        entry.getKey(), refactorReplacement, filesystemReplacementPane));
            });
            operationStatus = Status.OK;
        } else {
            operationStatus = Status.FAIL;
        }
        logger.info("...load project structure replacements: " + operationStatus.getStatus());

        operationStatus = (config.getOriginalProject() != null && config.getOutputProject() != null) ? Status.OK
                : Status.FAIL;
        logger.info("...load original and output project folders: " + operationStatus.getStatus());
    }

    public void clearConfig() {
        removeCommentsCheckBox.setSelected(false);
        ifCommentContainCheckBox.setSelected(false);
        ifCommentContainTextField.setEditable(false);
        ifCommentContainTextField.setText("if contain");
        contentReplacementPane.clear();
        filesystemReplacementPane.clear();
        removeCommentsFilePatternSelectorBox.setFileMaskData(null);
        removeCommentsFileSettingsBox.setDisable(true);
    }

    @Override
    public void setMessages() {
        newConfigButton.setText("New config");
        saveConfigButton.setText("Save config");
        cancelButton.setText("Cancel");
        addContentReplacementButton.setText("Add per-file replacement");
        addFileSystemReplacementButton.setText("Add project structure replacement");
        removeCommentsCheckBox.setText("Remove comments");
        contentReplacementPane.setText("Per-file replacements");
        filesystemReplacementPane.setText("Project structure replacements");
        ifCommentContainTextField.setText("if contain");
        projectFoldersLabel
                .setText("Original project folder: " + originalProject + " | output project folder: " + outputProject);
    }

    @Override
    public void setButtonsActions(Stage stage) {
        removeCommentsCheckBox.setOnAction(event -> {
            if (removeCommentsCheckBox.isSelected()) {
                removeCommentsFileSettingsBox.setDisable(false);
            } else {
                removeCommentsFileSettingsBox.setDisable(true);
            }
        });
        ifCommentContainCheckBox.setOnAction(event -> {
            if (ifCommentContainCheckBox.isSelected()) {
                ifCommentContainTextField.setText("");
                ifCommentContainTextField.setEditable(true);
            } else {
                ifCommentContainTextField.setText("if contain");
                ifCommentContainTextField.setEditable(false);
            }
        });
        newConfigButton.setOnAction(event -> {
            logger.info("start new config");
            clearConfig();
        });
        saveConfigButton.setOnAction(event -> {
            Alert alert = new Alert(AlertType.ERROR);
            if (contentReplacementPane.isWrongDescriptionInReplacementItems()
                    || filesystemReplacementPane.isWrongDescriptionInReplacementItems()) {
                alert.setTitle("Description error");
                alert.setContentText("Empty descriptions are prohibited");
                alert.showAndWait();
            } else if (contentReplacementPane.isWrongSourceInReplacementItems()
                    || filesystemReplacementPane.isWrongSourceInReplacementItems()) {
                alert.setTitle("Source error");
                alert.setContentText("Empty sources are prohibited");
                alert.showAndWait();
            } else if (contentReplacementPane.isWrongTargetInReplacementItems()
                    || filesystemReplacementPane.isWrongTargetInReplacementItems()) {
                alert.setTitle("Target error");
                alert.setContentText("Empty targets are prohibited");
                alert.showAndWait();
            } else if (contentReplacementPane.isDuplicateDescriptionsInReplacementItems()
                    || filesystemReplacementPane.isDuplicateDescriptionsInReplacementItems()) {
                alert.setTitle("Description error");
                alert.setContentText("Similar descriptions are prohibited");
                alert.showAndWait();
            } else {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().clear();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(XML_DIALOG_NAME, XML_PATTERN));
                configFile = fc.showSaveDialog(stage);
                if (configFile != null) {
                    if (config == null) {
                        config = new Config();
                    }
                    logger.info("save current config to " + configFile.getAbsolutePath());

                    // работаем с удалением каментов
                    if (removeCommentsCheckBox.isSelected()) {
                        config.getRemoveComment().setToRemove(true);
                        logger.info("...save remove comments feature: " + Status.OK.getStatus());

                        // if contain
                        if (ifCommentContainCheckBox.isSelected() && !ifCommentContainTextField.getText().equals(null)
                                && !ifCommentContainTextField.getText().equalsIgnoreCase("")) {
                            config.getRemoveComment().setContain(ifCommentContainTextField.getText());
                            logger.info("...save remove comments contain text feature: " + Status.OK.getStatus());
                        } else {
                            logger.info("...save remove comments contain text feature: " + Status.FAIL.getStatus());
                        }

                        config.getRemoveComment().setFileMask(removeCommentsFilePatternSelectorBox.getFileMaskData());
                    } else {
                        logger.info("...save remove comments feature: " + Status.FAIL.getStatus() + ", nothing to do");
                    }

                    // работаем с рефактором внутри файлов
                    if (contentReplacementPane.getReplacementsMap() != null
                            && contentReplacementPane.getReplacementsMap().size() > 0) {
                        config.setReplacementInFileContent(contentReplacementPane.getReplacementsMap());
                        operationStatus = Status.OK;
                    } else {
                        operationStatus = Status.FAIL;
                    }
                    logger.info("...save per-file replacements: " + operationStatus.getStatus());

                    // работаем с ребилдом структуры проекта
                    if (filesystemReplacementPane.getReplacementsMap() != null
                            && filesystemReplacementPane.getReplacementsMap().size() > 0) {
                        config.setReplacementInProjectStructure(filesystemReplacementPane.getReplacementsMap());
                        operationStatus = Status.OK;
                    } else {
                        operationStatus = Status.FAIL;
                    }
                    logger.info("...save project structure replacements: " + operationStatus.getStatus());

                    if (originalProject != null && outputProject != null) {
                        config.setOriginalProject(originalProject);
                        config.setOutputProject(outputProject);
                        operationStatus = Status.OK;
                    } else {
                        operationStatus = Status.FAIL;
                    }
                    logger.info("...save original and output folder: " + operationStatus.getStatus());

                    if (configWorker.writeConfigData(configFile, config)) { // записали, обновили
                        // статус, проверили кнопки снизу
                        mainAppWindow.setConfigFile(configFile);
                        mainAppWindow.setProperConfigFileSelected(true);
                        mainAppWindow.getConfigFileStatusLabel().setText(configFile.getAbsolutePath());
                        mainAppWindow.getConfigFileStatusLabel().setGraphic(
                                new ImageView(new Image(getClass().getResourceAsStream("/img/sign/ok.png"))));
                        mainAppWindow.checkAndToggleButtons();
                        mainAppWindow.setConfig(config);
                    } else {
                        mainAppWindow.getConfigFileStatusLabel().setText("cancel select");
                        mainAppWindow.getConfigFileStatusLabel().setGraphic(
                                new ImageView(new Image(getClass().getResourceAsStream("/img/sign/disable.png"))));
                    }
                } else {
                    logger.info("cancel config save");
                }
                stage.close();
            }
        });
        cancelButton.setOnAction(event -> {
            logger.info("cancel config save");
            stage.close();
        });
        addContentReplacementButton.setOnAction(event -> {
            contentReplacementPane.addReplacementItem(null);
        });
        addFileSystemReplacementButton.setOnAction(event -> {
            filesystemReplacementPane.addReplacementItem(null);
        });
    }

}
