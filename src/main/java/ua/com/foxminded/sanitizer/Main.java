package ua.com.foxminded.sanitizer;

import java.io.IOException;

import com.github.difflib.algorithm.DiffException;
import com.sun.javafx.css.StyleManager;

import javafx.application.Application;
import javafx.stage.Stage;
import ua.com.foxminded.sanitizer.ui.MainAppWindow;

@SuppressWarnings("restriction")
public class Main extends Application {
    public static void main(String[] args) throws DiffException, IOException {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        StyleManager.getInstance().addUserAgentStylesheet("/css/sanitizer.css");
        new MainAppWindow(getParameters()).show();
    }
}
