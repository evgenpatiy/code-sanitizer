package ua.com.foxminded.sanitizer.ui.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment.Status;
import ua.com.foxminded.sanitizer.data.ProjectFileMask;
import ua.com.foxminded.sanitizer.ui.ISanitizerWindow;

public class FilesSelectorHBox extends HBox {
    private List<CheckBox> extensions = Arrays.asList(new CheckBox(".java"), new CheckBox(".xml"), new CheckBox(".ts"));
    private CheckBox filePatternCheckBox = new CheckBox();
    private TextField filePatternTextField = new TextField();
    private Status operationStatus;
    private static final Logger logger = LogManager.getLogger("sanitizer");

    public FilesSelectorHBox() {
        setAlignment(Pos.BASELINE_CENTER);
        getChildren().add(new Label("Files pattern:"));
        extensions.forEach(extension -> extension.setSelected(
                (extension.getText().equalsIgnoreCase(".java")) || (extension.getText().equalsIgnoreCase(".xml"))));
        getChildren().addAll(extensions);

        HBox filePatternHBox = new HBox();
        filePatternHBox.setAlignment(Pos.BASELINE_CENTER);
        filePatternTextField.setEditable(false);
        filePatternTextField.setText("custom pattern");
        filePatternCheckBox.setOnAction(event -> {
            if (filePatternCheckBox.isSelected()) {
                filePatternTextField.setText("");
                filePatternTextField.setEditable(true);
            } else {
                filePatternTextField.setText("custom pattern");
                filePatternTextField.setEditable(false);
            }
        });
        filePatternHBox.getChildren().addAll(filePatternCheckBox, filePatternTextField);
        getChildren().add(filePatternHBox);
        getChildren().forEach(
                node -> HBox.setMargin(node, new Insets(0, ISanitizerWindow.INSET, 0, ISanitizerWindow.INSET)));
    }

    public ProjectFileMask getFileMaskData() {
        ProjectFileMask result = new ProjectFileMask();
        List<String> patterns = new ArrayList<String>();
        extensions.forEach(extension -> {
            if (extension.isSelected()) {
                patterns.add(extension.getText());
            }
        });
        logger.info("...save file extensions: " + Status.OK.getStatus());
        result.setFilenameFilters(patterns);

        if (filePatternCheckBox.isSelected() && (!filePatternTextField.getText().equals(""))
                && (!filePatternTextField.getText().equals(null))) {
            result.setFilenameFilterRegexp(filePatternTextField.getText());
            operationStatus = Status.OK;
        } else {
            result.setFilenameFilterRegexp(null);
            operationStatus = Status.FAIL;
        }
        logger.info("...save custom file regexp for comments removal: " + operationStatus.getStatus());
        return result;
    }

    public void setFileMaskData(ProjectFileMask projectFileMask) {
        if (projectFileMask != null) {
            if (projectFileMask.getFilenameFilters() != null) {
                extensions.stream().forEach(extension -> {
                    extension.setSelected(projectFileMask.getFilenameFilters().stream()
                            .anyMatch(config -> config.equalsIgnoreCase(extension.getText())) ? true : false);
                });
                operationStatus = Status.OK;
            } else {
                operationStatus = Status.FAIL;
            }
            logger.info("...load file extensions: " + operationStatus.getStatus());
            if (projectFileMask.getFilenameFilterRegexp() != null) {
                filePatternCheckBox.setSelected(true);
                filePatternTextField.setEditable(true);
                filePatternTextField.setText(projectFileMask.getFilenameFilterRegexp());
                operationStatus = Status.OK;
            } else {
                operationStatus = Status.FAIL;
            }
            logger.info("...load custom file pattern: " + operationStatus.getStatus());
        } else {
            extensions.stream().forEach(extension -> extension.setSelected(
                    (extension.getText().equalsIgnoreCase(".java")) || (extension.getText().equalsIgnoreCase(".xml"))));
            filePatternCheckBox.setSelected(false);
            filePatternTextField.setEditable(false);
            filePatternTextField.setText("custom pattern");
        }
    }
}
