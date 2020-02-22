package plugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import edu.wpi.first.shuffleboard.api.data.MapData;
import edu.wpi.first.shuffleboard.api.prefs.Group;
import edu.wpi.first.shuffleboard.api.prefs.Setting;
import edu.wpi.first.shuffleboard.api.widget.Description;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

@Description(dataTypes = { MapData.class }, name = "ConsoleWidget")
public class ConsoleWidget extends IterativeWidget<MapData> {

    TextFlow console;
    VBox box;
    ScrollPane consoleScroller;
    ArrayList<CheckedClass> allClassElements;
    CheckedClass allButton;
    ArrayList<String> activeClassPaths;
    ArrayList<String> activeClassNames;

    List<String> allClasses;
    ComboBox<String> severity;

    private String oldPrint = "";
    private String oldLog = "";

    private static Logger robotLogger = Logger.getLogger("Robot");

    String[] severities = new String[] { "Info", "Warning", "Severe" };

    StringProperty allClassesProperty, startupMessageProperty;
    IntegerProperty maxPrintsProperty;

    public ConsoleWidget() {
        mainPane = new AnchorPane();
        mainPane.setMinWidth(200);
        mainPane.setMinHeight(200);

        allClasses = new ArrayList<String>();
        allClassesProperty = new SimpleStringProperty();
        allClassesProperty.addListener((a, b, c) -> {
            if (!isInitialized) {
                allClasses = Arrays.asList(c.replaceAll("\\s+", "").split(","));
            }
        });

        maxPrintsProperty = new SimpleIntegerProperty(200);
        startupMessageProperty = new SimpleStringProperty();

        launch(500);
    }

    @Override
    public void init() {

        if (dataProperty().get() == null) {
            isActive = false;
            return;
        }
        var map = dataProperty().get().asMap();
        if (map == null || map.get("Print Stream") == null) {
            isActive = false;
            return;
        }

        String user = System.getProperty("user.name");
        try {
            String date = new SimpleDateFormat("MM_dd_yy__hh_mm_ss").format(new Date());
            String path = "C:/Users/" + user + "/Desktop/Robotics/Logs/" + date + "_robotLog.log";
            File targetFile = new File(path);
            targetFile.createNewFile();
            FileHandler fh = new FileHandler(path);
            robotLogger.addHandler(fh);

            SimpleFormatter logFormat = new SimpleFormatter() {
                SimpleDateFormat timeFormatter = new SimpleDateFormat("[hh:mm:ss]");

                @Override
                public synchronized String format(LogRecord lr) {
                    return timeFormatter.format(new Date(lr.getMillis())) + " [" + lr.getLevel().getLocalizedName()
                            + "] " + lr.getMessage() + "\n";
                }
            };

            fh.setFormatter(logFormat);
            robotLogger.setUseParentHandlers(false);
        } catch (Exception e) {
            StringWriter strStream = new StringWriter();
            e.printStackTrace(new PrintWriter(strStream));
            logger.log(Level.SEVERE, strStream.toString());
        }

        box = new VBox();
        activeClassPaths = new ArrayList<String>();
        activeClassNames = new ArrayList<String>();
        allClassElements = new ArrayList<CheckedClass>();

        box.setPadding(new Insets(10, 10, 10, 10));
        console = new TextFlow();
        console.setStyle("-fx-background-color: BLACK");
        severity = new ComboBox<>(FXCollections.observableArrayList(severities));
        severity.minWidth(100);
        severity.prefWidthProperty().set(150);
        severity.getSelectionModel().selectFirst();

        Text t = new Text();
        t.setStyle("-fx-fill: YELLOW;");
        t.setText(startupMessageProperty.get());
        t.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, FontPosture.REGULAR, 14));
        console.getChildren().add(t);

        consoleScroller = new ScrollPane(console);
        consoleScroller.setPadding(new Insets(0, 0, 0, 10));
        consoleScroller.setStyle("-fx-background-color: BLACK");

        console.getChildren().addListener((ListChangeListener<Node>) l -> {
            if (!console.isFocused())
                consoleScroller.vvalueProperty().bind(console.heightProperty());
        });
        box.prefWidthProperty().bind(mainPane.widthProperty());
        box.prefHeightProperty().bind(mainPane.heightProperty());

        consoleScroller.prefWidthProperty().bind(box.widthProperty());
        consoleScroller.prefHeightProperty().bind(box.heightProperty());

        var classLabel = new ComboBox<CheckedClass>();
        classLabel.promptTextProperty().setValue("Classes");

        classLabel.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (classLabel.isShowing()) {
                String text = classLabel.promptTextProperty().get();
                String finalText = text;
                if (event.getCode() == KeyCode.BACK_SPACE && text.length() > 0) {
                    finalText = text.substring(0, text.length() - 1);
                } else if (Character.isLetter(event.getText().charAt(0))) {
                    finalText = text + event.getText();
                }
                final String finalFinalText = finalText;

                Comparator<CheckedClass> nameComparator = new Comparator<CheckedClass>() {
                    Comparator<String> nameComparator = Comparator
                            .<String, Integer>comparing(s -> numberOfMatchingChars(s, finalFinalText)).reversed()
                            .thenComparing(Comparator.naturalOrder());

                    @Override
                    public int compare(CheckedClass a, CheckedClass b) {
                        return nameComparator.compare(a.className, b.className);
                    }
                };

                classLabel.promptTextProperty().setValue(finalText);
                classLabel.getItems().sort(nameComparator);
                classLabel.getSelectionModel().selectFirst();
            }
        });
        classLabel.showingProperty().addListener((a, b, c) -> {
            if (c)
                classLabel.promptTextProperty().setValue("");
            else
                classLabel.promptTextProperty().setValue("Classes");
        });
        classLabel.converterProperty().setValue(new StringConverter<CheckedClass>() {
            @Override
            public CheckedClass fromString(String str) {
                return null;
            }

            @Override
            public String toString(CheckedClass c) {
                return classLabel.promptTextProperty().get();
            }
        });
        var skin = new ComboBoxListViewSkin<CheckedClass>(classLabel);
        skin.setHideOnClick(false);
        classLabel.setSkin(skin);

        classLabel.setCellFactory(c -> {
            ListCell<CheckedClass> cell = new ListCell<CheckedClass>() {
                @Override
                protected void updateItem(CheckedClass item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) {
                        final CheckBox cb = new CheckBox(item.className);
                        cb.selectedProperty().bind(item.isActive);
                        if (item.className == "All") {
                            allButton = item;
                            item.isActive.addListener((__, oldVal, newVal) -> {
                                if (newVal && !oldVal) {
                                    classLabel.getItems().forEach(c -> c.isActive.setValue(true));
                                } else if (oldVal && !newVal) {
                                    classLabel.getItems().forEach(c -> c.isActive.setValue(false));
                                }
                            });
                        }
                        setGraphic(cb);
                    }
                }
            };
            cell.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                cell.getItem().isActive.setValue(!cell.getItem().isActive.get());
                classLabel.getSelectionModel().select(cell.getItem());
            });
            return cell;
        });

        List<CheckedClass> allClassesAsBoxes = new ArrayList<CheckedClass>();
        allClassesAsBoxes.add(new CheckedClass("All"));

        for (String s : (String[]) dataProperty().get().asMap().get("Printing Classes")) {
            activeClassPaths.add(s);
        }

        for (String s : allClasses) {
            var checkedClass = new CheckedClass(s);

            if (activeClassPaths.contains(s)) {
                checkedClass.isActive.setValue(true);
            }
            checkedClass.isActive.addListener((__, oldVal, newVal) -> {
                if (newVal) {
                    if (!activeClassPaths.contains(s)) {
                        activeClassPaths.add(s);
                    }
                } else {
                    activeClassPaths.remove(s);
                }
            });
            allClassesAsBoxes.add(checkedClass);
        }
        classLabel.setItems(FXCollections.observableArrayList(allClassesAsBoxes));

        var options = new HBox(severity, classLabel);
        box.getChildren().addAll(consoleScroller, options);
        mainPane.getChildren().addAll(box);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    if(activeClassPaths.size() == 0) {
                        allButton.isActive.setValue(true);
                    }
                });
            } catch (InterruptedException e) {}
        }).start();
    }
    private class CheckedClass {
        public BooleanProperty isActive = new SimpleBooleanProperty();
        public String classPath, className;
        public CheckedClass(String path) {
            className = path.substring(path.lastIndexOf('.') + 1);
            classPath = path;
        }
    }
    private int numberOfMatchingChars(String className, String incomingText) {
        className = className.substring(className.lastIndexOf(".") + 1).toLowerCase();
        for(String textPiece = incomingText.toLowerCase(); textPiece.length() > 1; textPiece = textPiece.substring(0, textPiece.length() - 2)) {
            int index = className.indexOf(textPiece);
            if(index != -1) {
                return 100 * textPiece.length() - index;
            }
        }
        int index = className.indexOf(incomingText);
        return index != -1 ? 100 - index : 0;
    }
    private String findBetween(String s, String b1, String b2) {
        int b1i = s.indexOf(b1), b2i = s.indexOf(b2);
        if(b1i + b2i < 0) {
            return "null";
        } else if(b2i < b1i) {
            return "null";
        }
        return s.substring(b1i + b1.length(), b2i);
    }
    @Override
    public void update() {
        
        Map<String, Object> data = dataProperty().get().asMap();
        if(data != null) {
            var printObj = data.get("Print Stream");
            var logObj = data.get("Log Stream");

            if(printObj == null && logObj == null) {
                //isActive = false;
                return;
            }

            String logMsg = logObj.toString();
            String printMsg = printObj.toString();

            if(!printMsg.equals(oldPrint)) {
                oldPrint = printMsg;
                while(printMsg.length() > 1) {
                    String printedClass = findBetween(printMsg, "#class=", ",");
                    printMsg = printMsg.substring(printMsg.indexOf(",") + 1);

                    String printedSeverity = findBetween(printMsg, "severity=", ",");
                    printMsg = printMsg.substring(printMsg.indexOf(",") + 1);

                    String printedTimestamp = findBetween(printMsg, "time=", ",");
                    printMsg = printMsg.substring(printMsg.indexOf(",") + 1);

                    String printedColor = findBetween(printMsg, "color=", "#");
                    printMsg = printMsg.substring(printMsg.indexOf("#") + 1);

                    int index = printMsg.indexOf("#");
                    String printedMessage = printMsg;
                    if(index != -1) {
                        printedMessage = printMsg.substring(0, index);
                        printMsg = printMsg.substring(index);
                    }

                    switch(printedSeverity) {
                        case "INFO": addText("(INFO) ", "LIMEGREEN"); break;
                        case "WARNING": addText("(WARNING) ", "YELLOW"); break;
                        case "SEVERE": addText("(SEVERE) ", "RED"); break;
                    }
                    addText(printedClass + " " + printedMessage + "\n", printedColor);

                    if(index == -1) {
                        break;
                    }
                }
            }
            if(!logMsg.equals(oldLog)) {
                oldLog = logMsg;
                while(logMsg.length() > 1) {
                    String loggedClass = findBetween(logMsg, "#class=", ",");
                    logMsg = logMsg.substring(logMsg.indexOf(",") + 1);

                    String loggedSeverity = findBetween(logMsg, "severity=", ",");
                    logMsg = logMsg.substring(logMsg.indexOf(",") + 1);

                    String loggedTimestamp = findBetween(logMsg, "time=", "#");
                    logMsg = logMsg.substring(logMsg.indexOf("#") + 1);

                    int index = logMsg.indexOf("#");
                    String loggedMessage = logMsg;
                    if(index != -1) {
                        loggedMessage = logMsg.substring(0, index);
                        logMsg = logMsg.substring(index);
                    }
                    Level severity = Level.OFF;
                    switch(loggedSeverity) {
                        case "INFO": severity = Level.INFO; break;
                        case "WARNING": severity = Level.WARNING; break;
                        case "SEVERE": severity = Level.SEVERE; break;
                    }
                    robotLogger.log(severity, "(" + loggedClass + ") " + loggedMessage);

                    if(index == -1) {
                        break;
                    }

                }
            }
            var editedMap = new HashMap<String, Object>(data);
            editedMap.put("Printing Classes", activeClassPaths.toArray(new String[0]));
            editedMap.put("Print Severity", severity.getSelectionModel().getSelectedItem().toUpperCase());
            dataProperty().setValue(new MapData(editedMap));
        }
    }
    public void addText(String text, String color) {
        if(text.length() > 0) {
            Text t = new Text();
            t.setStyle("-fx-fill: " + color + ";");
            t.setText(text);
            console.getChildren().add(t);
        }
        for(int i = console.getChildren().size(); i > maxPrintsProperty.intValue() * 2; i--) {
            console.getChildren().remove(0);
        }
    }
    @Override
    public String toString() {
        return "Classes";
    }

    @Override
	public List<Group> getSettings() {
		LinkedList<Group> propertyList = new LinkedList<Group>();
		propertyList.add(Group.of("Properties",
            Setting.of("All Classes", allClassesProperty, String.class),
            Setting.of("Startup Message", startupMessageProperty, String.class),
            Setting.of("Max Prints", maxPrintsProperty, Integer.class)
		));
		return propertyList;
	}
}