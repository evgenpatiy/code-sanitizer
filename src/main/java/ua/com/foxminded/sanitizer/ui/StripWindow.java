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
import ua.com.foxminded.sanitizer.worker.StripWorker;

@RequiredArgsConstructor
public class StripWindow implements ISanitizerWindow {
    @NonNull
    private File originalFolder;
    @NonNull
    private File outputFolder;
    @NonNull
    private Config config;
    private String title;
    private StripWorker stripWorker;
    private ProgressBar stripProgressBar = new ProgressBar(0);
    private Button cancelStripButton = new Button();
    private Button startStripButton = new Button();
    private Button closeStripWindowButton = new Button();
    private Button exploreStripFolderButton = new Button();
    private static final Logger logger = LogManager.getLogger("sanitizer");

    @Override
    public void setMessages() {
        title = "Ready to strip";
        cancelStripButton.setText("Cancel");
        startStripButton.setText("Start");
        closeStripWindowButton.setText("Close");
        exploreStripFolderButton.setText("Explore");
    }

    @Override
    public void setButtonsActions(Stage stage) {
        closeStripWindowButton.setOnAction(event -> stage.close());
        exploreStripFolderButton.setOnAction(event -> {
            stage.close();
            new ExploreProjectWindow(outputFolder).show();
        });
        startStripButton.setOnAction(event -> {
            startStripButton.setDisable(true);
            closeStripWindowButton.setDisable(true);
            cancelStripButton.setDisable(false);
            stripProgressBar.setProgress(0);

            stripWorker = new StripWorker(originalFolder.toPath(), outputFolder.toPath(), config);
            stripProgressBar.progressProperty().unbind();
            stripProgressBar.progressProperty().bind(stripWorker.progressProperty());
            stage.titleProperty().unbind();
            stage.titleProperty().bind(stripWorker.messageProperty());

            stripWorker.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {

                @Override
                public void handle(WorkerStateEvent t) {
                    exploreStripFolderButton.setDisable(false);
                    closeStripWindowButton.setDisable(false);
                    cancelStripButton.setDisable(true);
                    startStripButton.setDisable(true);
                    logger.info("*** complete strip process");
                    stage.titleProperty().unbind();
                    stage.setTitle("Job successfully completed");
                }
            });
            logger.info("*** start file strip");
            new Thread(stripWorker).start();
        });
        cancelStripButton.setOnAction(event -> {
            startStripButton.setDisable(false);
            closeStripWindowButton.setDisable(false);
            cancelStripButton.setDisable(true);
            stripWorker.cancel(true);
            logger.info("!!! user interrupt project files strip");
            stripProgressBar.progressProperty().unbind();
            stage.titleProperty().unbind();
            stripProgressBar.setProgress(0);
        });
    }

    @Override
    public void show() {
        setMessages();
        cancelStripButton.setDisable(true);
        exploreStripFolderButton.setDisable(true);

        FlowPane topPane = new FlowPane();
        FlowPane bottomPane = new FlowPane();
        BorderPane root = new BorderPane();

        stripProgressBar.setMinWidth(0.8 * ISanitizerWindow.PROCESS_W);
        topPane.setAlignment(Pos.BASELINE_CENTER);
        topPane.getChildren().add(stripProgressBar);
        topPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(ISanitizerWindow.INSET)));
        root.setTop(topPane);

        bottomPane.setAlignment(Pos.CENTER);
        bottomPane.getChildren().addAll(startStripButton, cancelStripButton, exploreStripFolderButton,
                closeStripWindowButton);
        bottomPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(ISanitizerWindow.INSET)));
        root.setBottom(bottomPane);

        Stage stage = new Stage();
        setButtonsActions(stage);
        stage.setOnCloseRequest(event -> {
            if (stripWorker.isRunning()) {
                stripWorker.cancel(true);
                logger.info("!!! user interrupt project files strip process");
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
