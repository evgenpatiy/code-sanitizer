package ua.com.foxminded.sanitizer.ui;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;
import ua.com.foxminded.sanitizer.worker.FileWorker;
import ua.com.foxminded.sanitizer.worker.OSWorker.OS;

@RequiredArgsConstructor
public class FileViewWindow implements ISanitizerWindow, ISanitizerEnvironment {
    @NonNull
    private String fileName;
    private String modifiedFileString;
    private String ownerFileString;
    private String permissionsFileString;
    private FileWorker fileWorker = new FileWorker();
    private static final Logger logger = LogManager.getLogger("sanitizer");
    private static final String[] KEYWORD = new String[] { "abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while" };
    private static final String[] HIGHLIGHT = new String[] { String.valueOf('\u0009') };
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORD) + ")\\b";
    private static final String HIGHLIGHT_PATTERN = "(" + String.join("|", HIGHLIGHT) + ")\\b";
    private static final Pattern PATTERN = Pattern
            .compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<HIGHLIGHT>" + HIGHLIGHT_PATTERN + ")");

    private static StyleSpans<Collection<String>> highlight(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass = matcher.group("KEYWORD") != null ? "keyword"
                    : matcher.group("HIGHLIGHT") != null ? "highlight" : null;
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    @Override
    public void setMessages() {
        modifiedFileString = "Modified: ";
        ownerFileString = "Owner: ";
        permissionsFileString = "Permissions: ";
    }

    @Override
    public void setButtonsActions(Stage stage) {
        // TODO Auto-generated method stub
    }

    @Override
    public void show() {
        setMessages();
        Path file = Paths.get(fileName);
        Stage stage = new Stage();
        CodeArea textArea = new CodeArea();
        FlowPane bottomPane = new FlowPane();
        bottomPane.setAlignment(Pos.CENTER);
        bottomPane.setId("bottomPane");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(INSET));

        textArea.setCache(true);
        textArea.setEditable(false);
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));
        textArea.textProperty().addListener((obs, oldText, newText) -> textArea.setStyleSpans(0, highlight(newText)));

        try (InputStream in = new FileInputStream(file.toFile());
                Reader reader = new InputStreamReader(in, "UTF-8");
                BufferedReader buffer = new BufferedReader(reader)) {
            String line;
            while ((line = buffer.readLine()) != null) {
                textArea.appendText(line + System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("count content lines for " + file + ": " + Status.FAIL);
        }
        root.setCenter(new StackPane(new VirtualizedScrollPane<CodeArea>(textArea)));

        try {
            bottomPane.getChildren().add(new Label(modifiedFileString + fileWorker.getFileTime(file.toFile())));
        } catch (IOException e) {
            logger.error("read file modification time fail");
        }

        if ((ENV == OS.MAC) || (ENV == OS.UNIX) || (ENV == OS.SOLARIS)) {
            try {
                bottomPane.getChildren()
                        .add(new Label(ownerFileString + Files.getOwner(file, LinkOption.NOFOLLOW_LINKS)));
                bottomPane.getChildren().add(new Label(permissionsFileString
                        + fileWorker.getPermissions(Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS))));
            } catch (IOException e) {
                logger.error("read file owner and permissions info for " + file + ": " + Status.FAIL);
            }
        }
        bottomPane.getChildren().add(new Label("Size: " + file.toFile().length() + " bytes"));
        bottomPane.getChildren().forEach(node -> FlowPane.setMargin(node, new Insets(INSET)));
        root.setBottom(bottomPane);

        try {
            stage.setTitle(file.getFileName().toString() + " | " + fileWorker.getFileContentType(file.toFile()));
        } catch (IOException e) {
            stage.setTitle(file.getFileName().toString());
        }
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/code.png")));
        stage.setScene(new Scene(root, VIEWER_W, VIEWER_H));
        stage.show();
    }
}
