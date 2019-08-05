package ua.com.foxminded.sanitizer.ui;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.data.Config;
import ua.com.foxminded.sanitizer.worker.PreparationWorker;

@RequiredArgsConstructor
public class PrepareWindow implements ISanitizerWindow {
    @NonNull
    private File originalFolder;
    @NonNull
    private File outputFolder;
    @NonNull
    private Config config;
    @NonNull
    private MainAppWindow mainAppWindow;
    private String title;
    private PreparationWorker preparationWorker;
    private ProgressBar preparationProgressBar = new ProgressBar(0);
    private Button cancelPreparationButton = new Button();
    private Button startPreparationButton = new Button();
    private Button closePreparationWindowButton = new Button();
    private static final Logger logger = LogManager.getLogger("sanitizer");

    @Override
    public void setMessages() {
        title = "Ready to prepare output";
        cancelPreparationButton.setText("Cancel");
        startPreparationButton.setText("Start");
        closePreparationWindowButton.setText("Close");
    }

    @Override
    public void setButtonsActions(Stage stage) {
        closePreparationWindowButton.setOnAction(event -> stage.close());
        startPreparationButton.setOnAction(event -> {
            startPreparationButton.setDisable(true);
            closePreparationWindowButton.setDisable(true);
            cancelPreparationButton.setDisable(false);
            preparationProgressBar.setProgress(0);

            preparationWorker = new PreparationWorker(originalFolder.toPath(), outputFolder.toPath());
            preparationProgressBar.progressProperty().unbind();
            preparationProgressBar.progressProperty().bind(preparationWorker.progressProperty());
            stage.titleProperty().unbind();
            stage.titleProperty().bind(preparationWorker.messageProperty());

            preparationWorker.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent t) {
                            closePreparationWindowButton.setDisable(false);
                            cancelPreparationButton.setDisable(true);
                            startPreparationButton.setDisable(true);
                            logger.info("*** complete peparation process");
                            stage.titleProperty().unbind();
                            stage.setTitle("Job successfully completed");
                            mainAppWindow.setOutputFolderPrepared(true);
                            mainAppWindow.checkAndToggleButtons();
                        }
                    });
            logger.info("*** start preparation process");
            new Thread(preparationWorker).start();
        });
        cancelPreparationButton.setOnAction(event -> {
            startPreparationButton.setDisable(false);
            closePreparationWindowButton.setDisable(false);
            cancelPreparationButton.setDisable(true);
            preparationWorker.cancel(true);
            logger.info("!!! user interrupt preparation process");
            preparationProgressBar.progressProperty().unbind();
            stage.titleProperty().unbind();
            preparationProgressBar.setProgress(0);
            mainAppWindow.setOutputFolderPrepared(false);
            mainAppWindow.checkAndToggleButtons();
        });
    }

    @Override
    public void show() {
        setMessages();
        cancelPreparationButton.setDisable(true);
        FlowPane topPane = new FlowPane();
        FlowPane bottomPane = new FlowPane();
        BorderPane root = new BorderPane();

        preparationProgressBar.setMinWidth(0.8 * ISanitizerWindow.PROCESS_W);
        topPane.setAlignment(Pos.BASELINE_CENTER);
        topPane.getChildren().add(preparationProgressBar);
        topPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(ISanitizerWindow.INSET)));
        root.setTop(topPane);

        bottomPane.setAlignment(Pos.CENTER);
        bottomPane.getChildren().addAll(startPreparationButton, cancelPreparationButton, closePreparationWindowButton);
        bottomPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(ISanitizerWindow.INSET)));
        root.setBottom(bottomPane);

        Stage stage = new Stage();
        setButtonsActions(stage);
        stage.setOnCloseRequest(event -> {
            if (preparationWorker.isRunning()) {
                preparationWorker.cancel(true);
                logger.info("!!! user interrupt preparation process");
                mainAppWindow.setOutputFolderPrepared(false);
                mainAppWindow.checkAndToggleButtons();
            }
            stage.close();
        });
        setButtonsActions(stage);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));

        stage.setScene(new Scene(root, ISanitizerWindow.PROCESS_W, ISanitizerWindow.PROCESS_H));
        stage.setTitle(title);
        stage.show();
    }

}
