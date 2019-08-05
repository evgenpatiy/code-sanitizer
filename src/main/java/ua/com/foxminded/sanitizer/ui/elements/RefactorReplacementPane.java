package ua.com.foxminded.sanitizer.ui.elements;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import ua.com.foxminded.sanitizer.data.RefactorReplacement;
import ua.com.foxminded.sanitizer.ui.ISanitizerWindow;

public class RefactorReplacementPane extends TitledPane {
    // строки, добавляемые в панель
    public class RefactorReplacementItem extends GridPane {
        private HBox refactorReplaceHBox = new HBox();
        private FilesSelectorHBox filesSelectorBox = new FilesSelectorHBox();
        private Label descriptionLabel = new Label();
        private Label sourceLabel = new Label();
        private Label targetLabel = new Label();
        private TextField descriptionTextField = new TextField();
        private TextField sourceTextField = new TextField();
        private TextField targetTextField = new TextField();
        private Button deleteReplacementItemButton = new Button();
        private RefactorReplacementPane replacementPane;

        public RefactorReplacementItem(String description, RefactorReplacement refactorReplacement,
                RefactorReplacementPane replacementPane) {
            super();
            setMessages();
            setButtonActions();
            setId("topPane");
            this.replacementPane = replacementPane;
            descriptionTextField.setText(description);

            refactorReplaceHBox.getChildren().addAll(descriptionLabel, descriptionTextField, sourceLabel,
                    sourceTextField, targetLabel, targetTextField, deleteReplacementItemButton);
            refactorReplaceHBox.setAlignment(Pos.BASELINE_CENTER);
            refactorReplaceHBox.getChildren().forEach(
                    node -> HBox.setMargin(node, new Insets(0, ISanitizerWindow.INSET, 0, ISanitizerWindow.INSET)));
            filesSelectorBox.setAlignment(Pos.BASELINE_LEFT);
            add(refactorReplaceHBox, 0, 0);
            add(filesSelectorBox, 0, 1);
            getChildren().forEach(node -> GridPane.setMargin(node,
                    new Insets(ISanitizerWindow.INSET / 2, 0, ISanitizerWindow.INSET / 2, 0)));
            setRefactorReplacement(refactorReplacement);
        }

        private void setMessages() {
            descriptionLabel.setText("Description: ");
            sourceLabel.setText("Source: ");
            targetLabel.setText("Target: ");
            deleteReplacementItemButton.setText("Delete");
        }

        private void setButtonActions() {
            deleteReplacementItemButton.setOnAction(event -> replacementPane.removeReplacementItem(this));
        }

        private void setRefactorReplacement(RefactorReplacement refactorReplacement) {
            sourceTextField.setText(refactorReplacement.getSource());
            targetTextField.setText(refactorReplacement.getTarget());
            filesSelectorBox.setFileMaskData(refactorReplacement.getFileMask());
        }

        public RefactorReplacement getRefactorReplacement() {
            RefactorReplacement refactorReplacement = new RefactorReplacement();
            refactorReplacement.setFileMask(filesSelectorBox.getFileMaskData());
            refactorReplacement.setSource(sourceTextField.getText());
            refactorReplacement.setTarget(targetTextField.getText());
            return refactorReplacement;
        }
    }

    @Getter
    private GridPane mainPane = new GridPane();

    public RefactorReplacementPane() {
        ColumnConstraints mainColumn = new ColumnConstraints();
        mainColumn.setPercentWidth(100);
        mainPane.getColumnConstraints().add(mainColumn);
        setContent(new ScrollPane(mainPane));
    }

    public Map<String, RefactorReplacement> getReplacementsMap() {
        Map<String, RefactorReplacement> result = new HashMap<String, RefactorReplacement>();
        mainPane.getChildren().stream().forEach(node -> {
            RefactorReplacementItem item = (RefactorReplacementItem) node;
            RefactorReplacement replacement = new RefactorReplacement();
            replacement.setSource(item.sourceTextField.getText());
            replacement.setTarget(item.targetTextField.getText());
            replacement.setFileMask(item.filesSelectorBox.getFileMaskData());
            result.put(item.descriptionTextField.getText(), replacement);
        });
        return result;
    }

    public void clear() {
        mainPane.getChildren().clear();
    }

    public boolean isWrongSourceInReplacementItems() {
        return mainPane.getChildren().stream()
                .anyMatch(node -> ((RefactorReplacementItem) node).sourceTextField.getText().equals("")
                        || ((RefactorReplacementItem) node).sourceTextField.getText().equals(null));
    }

    public boolean isWrongTargetInReplacementItems() {
        return mainPane.getChildren().stream()
                .anyMatch(node -> ((RefactorReplacementItem) node).targetTextField.getText().equals("")
                        || ((RefactorReplacementItem) node).targetTextField.getText().equals(null));
    }

    public boolean isWrongDescriptionInReplacementItems() {
        return mainPane.getChildren().stream()
                .anyMatch(node -> ((RefactorReplacementItem) node).descriptionTextField.getText().equals("")
                        || ((RefactorReplacementItem) node).descriptionTextField.getText().equals(null));
    }

    public boolean isDuplicateDescriptionsInReplacementItems() {
        return mainPane.getChildren().stream()
                .map(node -> ((RefactorReplacementItem) node).descriptionTextField.getText())
                .collect(Collectors.toSet()).size() < mainPane.getChildren().size();
    }

    public void addReplacementItem(RefactorReplacementItem replacementItem) {
        mainPane.add((replacementItem == null) ? new RefactorReplacementItem("", new RefactorReplacement("", ""), this)
                : replacementItem, 0, mainPane.getChildren().size());
    }

    public void removeReplacementItem(RefactorReplacementItem replacementItem) {
        mainPane.getChildren().remove(replacementItem);
    }
}
