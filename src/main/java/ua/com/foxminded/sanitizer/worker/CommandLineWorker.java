package ua.com.foxminded.sanitizer.worker;

import javafx.application.Application.Parameters;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ua.com.foxminded.sanitizer.ISanitizerEnvironment;

@RequiredArgsConstructor
public class CommandLineWorker implements ISanitizerEnvironment {
    @NonNull
    private Parameters parameters;

    public String getMasterProjectFile() {
        return parameters.getRaw().isEmpty() ? null
                : parameters.getRaw().stream().anyMatch(p -> p.endsWith(MASTER_EXT))
                        ? parameters.getRaw().stream().filter(p -> p.endsWith(MASTER_EXT)).findFirst().get()
                        : null;
    }
}
