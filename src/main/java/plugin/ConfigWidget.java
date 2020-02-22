package plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import edu.wpi.first.shuffleboard.api.data.MapData;
import edu.wpi.first.shuffleboard.api.widget.Description;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane;

@Description(dataTypes = { MapData.class }, name = "ConfigWidget")
public class ConfigWidget extends IterativeWidget<MapData> {

	private GridPane variableGrid;
	private Map<String, ConfigEntry> variables;

	public ConfigWidget() {
		mainPane = new AnchorPane();
		mainPane.setMinWidth(256);
		mainPane.setMinHeight(100);
		launch(500);
	}

	public void init() {
		int i = 0;
		variableGrid = new GridPane();
		TilePane variableBox = new TilePane(Orientation.VERTICAL);

		variables = new HashMap<String, ConfigEntry>();

		variableGrid.setPadding(new Insets(10, 10, 10, 10));
		variableGrid.setHgap(10);

		mainPane.getChildren().add(variableBox);
		//mainPane.getChildren().add(variableGrid);

		if (dataProperty().get().asMap().entrySet().size() > 0) {
			for(var entry : dataProperty().get().asMap().entrySet()) {
				String variable = entry.getKey();
				int slashIndex = variable.indexOf("/");
				if(slashIndex != -1) {
					logger.log(Level.INFO, "Added variable " + entry.getKey());
					String varName = variable.substring(0, slashIndex);
					String varType = variable.substring(slashIndex + 1, variable.length() - 1);

					if(!variables.containsKey(varName)) {
						var ce = new ConfigEntry(varName);
						variables.put(varName, ce);

						GridPane horizontalBox = new GridPane();
						horizontalBox.setHgap(10);
						horizontalBox.setPadding(new Insets(10, 10, 10, 10));
						horizontalBox.prefWidthProperty().bind(mainPane.widthProperty());
						//horizontalBox.minWidthProperty().bind(horizontalBox.widthProperty());
						ce.title.minWidthProperty().bind(ce.title.widthProperty());
						//ce.title.prefWidthProperty().bind(ce.title.widthProperty());

						ce.text.prefWidthProperty().bind(mainPane.widthProperty().subtract(ce.title.widthProperty().add(50)));
						ce.text.minWidthProperty().bind(mainPane.widthProperty().subtract(ce.title.widthProperty().add(50)));
						ce.text.maxWidthProperty().bind(mainPane.widthProperty().subtract(ce.title.widthProperty().add(50)));


						horizontalBox.addRow(0, ce.title, ce.text);
						
						variableBox.getChildren().add(horizontalBox);
						i++;
					}
					switch(varType) {
						case "Default":
							variables.get(varName).defaultValue = dataProperty().get().asMap().get(varName);
							break;
					}
				}
			}
		}
		new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> {
					
                });
            } catch (InterruptedException e) {}
        }).start();
	}
	public void update() {
		for(var entry : variables.values()) {
			entry.update();
		}
	}
	
	private Function<String, Object> getConversion(Object type) {
		if(type.getClass() == Double.class) {
			return str -> Double.parseDouble(str);
		}
		if(type.getClass() == Boolean.class) {
			return str -> Boolean.parseBoolean(str);
		}
		return str -> str;
	}
	
	private TextFormatter<String> getTextFormatter(Object type) {
		if(type.getClass() == Double.class) {
			return new TextFormatter<>(change -> {
				if (!change.isContentChange()) {
					return change;
				}
				String text = change.getControlNewText();
	
				String replaced = text.replaceAll("[^\\d.-]", "");
				if(!text.equals(replaced)) {
					return null;
				}
				return change;
			});
		}
		if(type.getClass() == Boolean.class) {
			return new TextFormatter<>(change -> {
				if (!change.isContentChange()) {
					return change;
				}
				String text = change.getControlNewText();
				String replaced = text.replaceAll("[^falsetru]", "");
				if(!text.equals(replaced)) {
					return null;
				}
				return change;
			});
		}
		return new TextFormatter<>(change -> change);
	}
	public class ConfigEntry {
		public Object previousValue;
		public Object defaultValue;
		public Function<String, Object> conversion;
		public TextField text;
		public String name;
		public Label title;
		private double lastUpdated;
		public ConfigEntry(String name) {
			this.title = new Label(name);
			this.name = name;
			this.previousValue = dataProperty().get().asMap().get(name + "/Value");
			this.text = new TextField(previousValue.toString());
			this.text.setTextFormatter(getTextFormatter(previousValue));
			this.conversion = getConversion(previousValue);
			this.text.textProperty().addListener((__, oldValue, newValue) -> {
				lastUpdated = System.currentTimeMillis();
			});
		}
		public void update() {
			var netVal = dataProperty().get().asMap().get(name + "/Value");
			var textVal = conversion.apply(text.getText());
			
			if(!netVal.equals(textVal)) {
				if(!previousValue.equals(textVal)) {
					//textVal was updated
					if(lastUpdated + 500 < System.currentTimeMillis()) {
						Map<String, Object> map = new HashMap<String, Object>(dataProperty().get().asMap());
						map.put(name + "/Value", textVal);
						dataProperty().setValue(new MapData(map));
						previousValue = textVal;
					}
				}
				else {
					//networkVal was updated
					text.setText(netVal.toString());
					previousValue = netVal;
				}
			}
		}
	}
}