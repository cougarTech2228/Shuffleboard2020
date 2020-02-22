package plugin;

import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import edu.wpi.first.shuffleboard.api.plugin.Description;
import edu.wpi.first.shuffleboard.api.plugin.Plugin;
import edu.wpi.first.shuffleboard.api.theme.Theme;
import edu.wpi.first.shuffleboard.api.widget.ComponentType;
import edu.wpi.first.shuffleboard.api.widget.WidgetType;

@Description(group = "Cougartech Plugin", name = "plugin", summary = "Drum & Config Widgets", version = "1.1.0")
public class App extends Plugin {

	public static final Theme darkBlueTheme = new Theme(App.class, "Dank Theme", "DarkTheme.css");

	@Override
	@SuppressWarnings("all")
	public List<ComponentType> getComponents() {

		return ImmutableList.of(
			WidgetType.forAnnotatedWidget(DrumWidget.class),
			WidgetType.forAnnotatedWidget(ConfigWidget.class),
			WidgetType.forAnnotatedWidget(ConsoleWidget.class),
			WidgetType.forAnnotatedWidget(CougarBackground.class)
		);
	}
	@Override
	public List<Theme> getThemes() {
		return List.of(darkBlueTheme);
	}
	/*
	@Override
	public Map<DataType, ComponentType> getDefaultComponents() {
		return Map.of(
			BallsType.Instance, WidgetType.forAnnotatedWidget(DrumWidget.class)
		);
	}*/
	public static Logger logger = Logger.getLogger(edu.wpi.first.shuffleboard.app.Main.class.getName());
}
