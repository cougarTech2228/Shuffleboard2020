package plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import edu.wpi.first.shuffleboard.api.data.MapData;
import edu.wpi.first.shuffleboard.api.prefs.Group;
import edu.wpi.first.shuffleboard.api.prefs.Setting;
import edu.wpi.first.shuffleboard.api.widget.Description;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

@Description(dataTypes = { MapData.class }, name = "DrumWidget")
public class DrumWidget extends IterativeWidget<MapData> {

	private double rotation = 0;

	private BooleanProperty animate = new SimpleBooleanProperty(this, "animate", true);
	private DoubleProperty spinSpeed = new SimpleDoubleProperty(this, "spinSpeed", 72);

	protected VBox _box;
	private GraphicsContext gc;
	private boolean isShooting;

	private BallArray balls = new BallArray();
	private BallArray robotArray = new BallArray();

	private Image drumBack = new Image(getClass().getResourceAsStream("drumBack.png"));
	private Image drumHole = new Image(getClass().getResourceAsStream("drumHole.png"));
	private Image drumBall = new Image(getClass().getResourceAsStream("drumBall.png"));

	public DrumWidget() {
		mainPane = new AnchorPane();
		mainPane.setMinWidth(256);
		mainPane.setMinHeight(256);
		launch(100);
	}
	public void init() {
		Canvas c = new Canvas();
		VBox v = new VBox();
		v.setAlignment(Pos.CENTER);
		v.setMinSize(200, 200);
		VBox.setVgrow(mainPane, Priority.ALWAYS);
		v.prefWidthProperty().bind(mainPane.widthProperty());
		v.prefHeightProperty().bind(mainPane.heightProperty());

		c.setWidth(200);
		c.setHeight(200);

		gc = c.getGraphicsContext2D();

		gc.drawImage(drumBack, 0, 0);

		v.getChildren().add(c);
		
		mainPane.getChildren().add(v);
		try {
			balls = new BallArray();
		} catch(Exception e) {

		}
		this.dataProperty().addListener(new ChangeListener<MapData>() {
			@Override
			public void changed(ObservableValue<? extends MapData> arg0, MapData oldValue, MapData newValue) {
				if(oldValue == null) oldValue = newValue;
				BallArray oldArray = new BallArray(oldValue.asMap());
				BallArray newArray = new BallArray(newValue.asMap());
				if(oldArray.clearIndex(2).data == newArray.data) {
					logger.log(Level.INFO, "Rotated");
					if(animate.get()) {
						balls = balls.rotate();
						rotation = -72;
						isShooting = true;
					} else {
						rotation = 0;
						balls = newArray;
					}
				}
				else if(oldArray.clearIndex(2).rotate().data == newArray.data) {
					logger.log(Level.INFO, "Rotated");
					if(animate.get()) {
						balls = balls.rotate();
						rotation = -72;
						isShooting = true;
					} else {
						rotation = 0;
						balls = newArray;
					}
				}
				else if(new BallArray(oldArray.data ^ 1).data == newArray.data) {
					logger.log(Level.INFO, "Acquired");
					rotation = animate.get() ? -72 : 0;
					balls = newArray;
				}
				else if(oldArray.acquire().data == newArray.data) {
					logger.log(Level.INFO, "Acquired");
					rotation = animate.get() ? -72 : 0;
					balls = newArray;
				}
				else {
					balls = newArray;
				}
				robotArray = newArray;
			}
		});
	}
	public void update() {
		gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
		
		if(animate.get() && rotation < 0) {
			rotation += spinSpeed.get() * .02;
		}
		if(isShooting && rotation > -36) {
			balls = robotArray;
			isShooting = false;
		}
		gc.drawImage(drumBack, 0, 0);

		for (int i = 0; i < 5; i++) {
			
			gc.save();
			gc.translate(100, 100);
			gc.rotate(i * 72 + rotation);
			gc.translate(-100, -100);
			gc.drawImage(drumHole, 0, 0);
			if (balls.getAtIndex(i))
				gc.drawImage(drumBall, 0, 0);
			gc.restore();
		}
	}
	
	@Override
	public List<Group> getSettings() {
		LinkedList<Group> propertyList = new LinkedList<Group>();
		propertyList.add(Group.of("Properties",
			Setting.of("Animation Enabled", animate, Boolean.class),
			Setting.of("Animation Speed (Deg / Sec)", spinSpeed, Double.class)
		));

		return propertyList;
	}
	public class BallArray {
		public int data;
		public BallArray() {
			this.data = 0;
		}
		public BallArray(Map<String, Object> map) {
			data = ((Number)map.getOrDefault("DrumData", 0)).intValue();
		}
		public BallArray(int data) {
			this.data = data;
		}
		public BallArray acquire() {
			return new BallArray(data | 1).rotate();
		}
		public BallArray rotate() {
			return new BallArray(((data << 1) | (data >> 4)) & 31);
		}
		public BallArray shoot() {
			return new BallArray(rotate().data & 23);
		}
		public boolean getAtIndex(int i) {
			return (data | (1 << i)) == data;
		}
		public BallArray clearIndex(int i) {
			return new BallArray(data & (31 ^ (1 << i)));
		}
		public boolean isFull() {
			return data > 30;
		}
	}
}
