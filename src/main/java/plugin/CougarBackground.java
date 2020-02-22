package plugin;

import edu.wpi.first.shuffleboard.api.widget.Description;
import edu.wpi.first.shuffleboard.app.components.AdderTab;
import edu.wpi.first.shuffleboard.app.components.DashboardTab;
import edu.wpi.first.shuffleboard.app.components.DashboardTabPane;
import edu.wpi.first.shuffleboard.app.components.ProcedurallyDefinedTab;
import edu.wpi.first.shuffleboard.app.components.WidgetGallery;
import edu.wpi.first.shuffleboard.app.prefs.AppPreferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

@Description(dataTypes = { CougarBackground.None.class }, name = "CougarWidget")
public class CougarBackground extends IterativeWidget<CougarBackground.None> {

    private Image cougar = new Image(getClass().getResourceAsStream("cougar.png"));
    private AnchorPane mainPane;

    public CougarBackground() {
        mainPane = new AnchorPane();
        launch(1000);
    }

    @Override
    public Pane getView() {
        return mainPane;
    }

    Background originalBackground;
    Background cougarBackground;
    ObjectProperty<Background> activeBackground;

    @Override
    public void init() {

        BackgroundImage cougarImage = new BackgroundImage(cougar, BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT, new BackgroundSize(144, 144, false, false, false, false));

        Parent p = mainPane.getParent().getParent().getParent().getParent().getParent().getParent().getParent()
                .getParent().getParent().getParent().getParent();

        DashboardTabPane d = (DashboardTabPane) (((StackPane) p.getChildrenUnmodifiable().get(1)).getChildren().get(0));

        BackgroundImage originalImage = ((DashboardTab) d.getTabs().get(0)).getWidgetPane().getBackground().getImages()
                .get(0);

        originalBackground = new Background(originalImage);
        cougarBackground = new Background(originalImage, cougarImage);

        activeBackground = new SimpleObjectProperty<>(originalBackground);

        d.getTabs().forEach(t -> {
            if (t.getClass() == DashboardTab.class || t.getClass() == ProcedurallyDefinedTab.class) {
                var pane = ((DashboardTab) t).getWidgetPane();
                pane.backgroundProperty().unbind();
                pane.backgroundProperty().bind(activeBackground);
            } else if(t.getClass() == AdderTab.class) {
                var pane = new DashboardTab(((AdderTab)t).getText()).getWidgetPane();
                pane.backgroundProperty().unbind();
                pane.backgroundProperty().bind(activeBackground);
            }
        });
        d.getTabs().addListener((ListChangeListener<Tab>) l -> {
            d.getTabs().forEach(t -> {
                if (t.getClass() == DashboardTab.class || t.getClass() == ProcedurallyDefinedTab.class) {
                    var pane = ((DashboardTab) t).getWidgetPane();
                    pane.backgroundProperty().unbind();
                    pane.backgroundProperty().bind(activeBackground);
                } else if(t.getClass() == AdderTab.class) {
                    var pane = new DashboardTab(((AdderTab)t).getText()).getWidgetPane();
                    pane.backgroundProperty().unbind();
                    pane.backgroundProperty().bind(activeBackground);
                }
            });
        });
        if (AppPreferences.getInstance().themeProperty().getValue().getName() == App.darkBlueTheme.getName()) {
            activeBackground.set(cougarBackground);
        } else {
            activeBackground.set(originalBackground);
        }
        AppPreferences.getInstance().themeProperty().addListener((__, oldTheme, newTheme) -> {
            if (newTheme.getName() == App.darkBlueTheme.getName()) {
                activeBackground.set(cougarBackground);
            } else {
                activeBackground.set(originalBackground);
            }
        });
        ((WidgetGallery) mainPane.getParent().getParent().getParent()).getChildren()
                .remove(mainPane.getParent().getParent());
    }
    class None {

    }
    @Override
    public void update() {
        // TODO Auto-generated method stub

    }
}