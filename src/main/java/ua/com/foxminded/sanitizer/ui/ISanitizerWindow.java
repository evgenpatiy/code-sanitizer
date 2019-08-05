package ua.com.foxminded.sanitizer.ui;

import javafx.stage.Stage;

public interface ISanitizerWindow {
    public static final int INSET = 10;
    public static final int MAIN_W = 800;
    public static final int MAIN_H = 600;
    public static final int EXPLORE_W = 800;
    public static final int EXPLORE_H = 600;
    public static final int VIEWER_W = 800;
    public static final int VIEWER_H = 600;
    public static final int CONFIGEDITOR_W = 950;
    public static final int CONFIGEDITOR_H = 600;
    public static final int PROCESS_W = 320;
    public static final int PROCESS_H = 100;
    public static final int UNDO_W = 640;
    public static final int UNDO_H = 600;

    public void setMessages();

    public void setButtonsActions(Stage stage);

    public void show();
}
