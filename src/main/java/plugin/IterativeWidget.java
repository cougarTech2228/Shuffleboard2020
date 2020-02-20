package plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.wpi.first.shuffleboard.api.util.ShutdownHooks;
import edu.wpi.first.shuffleboard.api.widget.SimpleAnnotatedWidget;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;

public abstract class IterativeWidget<T> extends SimpleAnnotatedWidget<T> {
    protected Logger logger = App.logger;

    public Pane mainPane;
    public boolean isActive;
    public boolean isInitialized;

    public IterativeWidget() {

    }
    public final void launch(long msWait) {
        if(mainPane == null) {
            logger.log(Level.SEVERE, "You must initialize the pane before launching");
        }
        new Thread(() -> {
			try {
				Thread.sleep(msWait);
                Platform.runLater(this::launch);
			} catch (InterruptedException e) {}
        }).start();
        
    }
    private void launch() {
        isActive = true;
        init();
        isInitialized = true;
            if(isActive) {
            var updater = new Thread(() -> {
                while (isActive && mainPane.parentProperty().isNotNull().getValue()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {  }
                    Platform.runLater(this::update);
                }
                logger.log(Level.INFO, "Shutdown thread for " + this.getClass().getSimpleName());
            },
            "Updater"
            );
            ShutdownHooks.addHook(() -> isActive = false);
            updater.start();
        }
    }
    public abstract void init();
    public abstract void update();

    @Override
    public Pane getView() {
        return mainPane;
    }
}