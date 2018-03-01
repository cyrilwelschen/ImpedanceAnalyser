package sample;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.Dialog;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Double.max;
import static java.lang.Double.min;

public class Controller {
    @FXML
    TextField workingDirectoryTextField;
    @FXML
    TreeView<String> directoryTreeView;
    @FXML
    StackPane chartContainer;
    @FXML
    LineChart<Number, Number> impedanceLineChart;
    @FXML
    NumberAxis xAxis;
    @FXML
    NumberAxis yAxis;
    @FXML
    StackPane chartContainerF;
    @FXML
    LineChart<Number, Number> impedanceLineChartF;
    @FXML
    NumberAxis xAxisF;
    @FXML
    NumberAxis yAxisF;
    private String currentChartTitle = "";
    private final Double DEFAULT_X_MIN_DIST = 0.0;
    private final Double DEFAULT_X_MAX_DIST = 2500.0;
    private final Double DEFAULT_Y_MIN_DIST = -30.0;
    private final Double DEFAULT_Y_MAX_DIST = 120.0;
    private final Double DEFAULT_X_MIN_FREQ = 0.0;
    private final Double DEFAULT_X_MAX_FREQ = 1000000.0;
    private final Double DEFAULT_Y_MIN_FREQ = 0.0;
    private final Double DEFAULT_Y_MAX_FREQ = 300.0;

    public void initialize() {
        workingDirectoryTextField.setOnMousePressed(event -> setWorkingDirectory());

        setLineChartProperties(impedanceLineChart);
        Rectangle zoomRect = rectangleFactory();
        chartContainer.getChildren().add(zoomRect);
        setUpZooming(zoomRect, impedanceLineChart, impedanceLineChart);

        setLineChartProperties(impedanceLineChartF);
        Rectangle zoomFreq = rectangleFactory();
        chartContainerF.getChildren().add(zoomFreq);
        setUpZooming(zoomFreq, impedanceLineChartF, impedanceLineChartF);

        // Axis initial boundaries
        resetAxis(xAxis, DEFAULT_X_MIN_DIST, DEFAULT_X_MAX_DIST);
        resetAxis(yAxis, DEFAULT_Y_MIN_DIST, DEFAULT_Y_MAX_DIST);
        xAxis.setOnMouseClicked(this::handleAxisClick);
        yAxis.setOnMouseClicked(this::handleAxisClick);
        //todo: set frequency x defaults different to dist x defaults (auto-recognize in resetAxis method
        resetAxis(xAxisF, DEFAULT_X_MIN_FREQ, DEFAULT_X_MAX_FREQ);
        resetAxis(yAxisF, DEFAULT_Y_MIN_FREQ, DEFAULT_Y_MAX_FREQ);
        xAxisF.setOnMouseClicked(this::handleAxisClick);
        yAxisF.setOnMouseClicked(this::handleAxisClick);
    }

    private Rectangle rectangleFactory() {
        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.5));
        zoomRect.setAccessibleText("Rectangle");
        return zoomRect;
    }

    private void setLineChartProperties(LineChart<Number, Number> lineChart){
        lineChart.setAlternativeColumnFillVisible(true);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
    }

    private void resetAxis(NumberAxis axis, Double bound1, Double bound2) {
        Double lowerBound = min(bound1, bound2);
        Double upperBound = max(bound1, bound2);
        axis.setAutoRanging(false);
        axis.setLowerBound(lowerBound);
        axis.setUpperBound(upperBound);
        axis.setTickUnit((upperBound - lowerBound)/10);
    }

    private void setFileTreeView(File workingDirectory) throws IOException {
        Path workingDirectoryPath = workingDirectory.toPath();

        List<Path> returnPathList = new ArrayList<>();
        PrintFiles pf = new PrintFiles(returnPathList, workingDirectoryPath);
        Files.walkFileTree(workingDirectoryPath, pf);
        returnPathList = pf.pathListToReturn;
        Collections.sort(returnPathList);

        returnPathList.remove(0);

        CheckBoxTreeItem<String> rootItem = new CheckBoxTreeItem<>(workingDirectoryPath.getFileName().toString());
        rootItem.setExpanded(true);
        directoryTreeView.setEditable(true);

        List<CheckBoxTreeItem<String>> rootItemList = new ArrayList<>();
        rootItemList.add(rootItem);

        directoryTreeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        for (Path currentPath : returnPathList) {
            final CheckBoxTreeItem<String> checkBoxTreeItem = new CheckBoxTreeItem<>(currentPath.getFileName().toString());
            String parentPathString = currentPath.getParent().getFileName().toString();
            CheckBoxTreeItem<String> currentRoot = findCurrentRootItem(rootItemList, parentPathString, rootItem);
            currentRoot.getChildren().add(checkBoxTreeItem);
            if (Files.isDirectory(currentPath)) {
                rootItemList.add(checkBoxTreeItem);
            } else {
                checkBoxTreeItem.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), (CheckBoxTreeItem.TreeModificationEvent<Object> event) -> {
                    CheckBoxTreeItem checkBoxWhoSelectionChanged = event.getTreeItem();
                    if (checkBoxWhoSelectionChanged.isSelected()) {
                        String csvHiddenFileString = Paths.get(currentPath.getParent().toString(), "HIDDEN_CALCULATED_IMPDIST_" + currentPath.getFileName().toString()).toString();
                        File csvHiddenFile = new File(csvHiddenFileString);
                        if (!(csvHiddenFile.exists())) {
                            try {
                                calculateInverseFourierTransform(currentPath);
                                Thread.sleep(1000);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Hidden Calculated file already exists. Not calculating it again!");
                        }
                        readCsv(currentPath, "HIDDEN_CALCULATED_IMPDIST_", impedanceLineChart);
                        readCsv(currentPath, "HIDDEN_CALCULATED_IMPFREQ_", impedanceLineChartF);
                    } else {
                        removeSeriesFromChart(currentPath, impedanceLineChart);
                        removeSeriesFromChart(currentPath, impedanceLineChartF);
                    }
                });
            }
        }
        directoryTreeView.setRoot(rootItem);
    }

    private void removeSeriesFromChart(Path pathToCsv, LineChart<Number, Number> lineChart) {
        int indexToDelete = 0;
        int counter = 0;
        for (XYChart.Series<Number, Number> cr : lineChart.getData()) {
            if (cr.getName().equals(pathToCsv.getFileName().toString())) {
                indexToDelete = counter;
            } else {
                counter++;
            }
        }
        lineChart.getData().remove(indexToDelete);
    }

    private CheckBoxTreeItem<String> findCurrentRootItem(List<CheckBoxTreeItem<String>> rootItemList, String parentPathString, CheckBoxTreeItem<String> rootItem) {
        CheckBoxTreeItem<String> currentRootItem = rootItem;
        for (CheckBoxTreeItem<String> rI : rootItemList) {
            if (rI.getValue().equals(parentPathString)) {
                currentRootItem = rI;
            }
        }
        return currentRootItem;
    }

    private void setOnMouseEventOnSeries(Node node, final LineChart chart, final String label) {
        node.setOnMouseExited(event -> chart.setTitle(currentChartTitle));
        node.setOnMouseEntered(event -> {
            currentChartTitle = chart.getTitle();
            chart.setTitle(label);
        });
    }

    private void calculateInverseFourierTransform(Path pathToFile) throws IOException {
        System.out.println("Python version is:");
        String pythonVersionCommand = "python --version";
        Process pythonVersionProcess = Runtime.getRuntime().exec(pythonVersionCommand);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(pythonVersionProcess.getInputStream()));
        System.out.println(stdInput.readLine());
        String command = "python ./src/pythonScripts/loadAndSave.py " + pathToFile;
        Runtime.getRuntime().exec(command);
    }

    private void readCsv(Path pathToCsvFile, String hiddenPrefix, LineChart<Number, Number> lineChart) {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ",";

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(pathToCsvFile.getFileName().toString());
        String csvHiddenFile = Paths.get(pathToCsvFile.getParent().toString(), hiddenPrefix  + pathToCsvFile.getFileName().toString()).toString();
        try {
            br = new BufferedReader(new FileReader(csvHiddenFile));
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] xyPair = line.split(cvsSplitBy);
                Float x = Float.parseFloat(xyPair[0]);
                Float y = Float.parseFloat(xyPair[1]);
                series.getData().add(new XYChart.Data<>(x, y));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        lineChart.getData().add(series);
        setOnMouseEventOnSeries(series.getNode(), lineChart, pathToCsvFile.getFileName().toString());
    }

    private void setUpZooming(final Rectangle rect, final Node zoomingNode, LineChart<Number, Number> lineChart) {
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
        AxisController axisController = new AxisController();
        zoomingNode.setOnMouseDragged(dragEvent -> {
            double x = dragEvent.getX();
            double y = dragEvent.getY();
            rect.setX(Math.min(x, mouseAnchor.get().getX()));
            rect.setY(Math.min(y, mouseAnchor.get().getY()));
            rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
            rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
        });
        zoomingNode.setOnMousePressed(event -> {
            mouseAnchor.set(new Point2D(event.getX(), event.getY()));
            rect.setWidth(0);
            rect.setHeight(0);
            double xS = event.getSceneX();
            double yS = event.getSceneY();
            final NumberAxis xAxisZ = (NumberAxis) lineChart.getXAxis();
            double AXxS = xAxisZ.localToScene(0, 0).getX();
            final NumberAxis yAxisZ = (NumberAxis) lineChart.getYAxis();
            double AYyS = yAxisZ.localToScene(0, 0).getY();
            double newXMin = (xS - AXxS) / xAxisZ.getScale();
            double newYMax = (yS - AYyS) / yAxisZ.getScale() + yAxisZ.getUpperBound();
            axisController.setNewXMinYMax(newXMin, newYMax);
        });
        zoomingNode.setOnMouseReleased(event -> {
            if (rect.getWidth() > 10 && rect.getHeight() > 10) {
                double xS = event.getSceneX();
                double yS = event.getSceneY();
                final NumberAxis xAxisZ = (NumberAxis) lineChart.getXAxis();
                double AXxS = xAxisZ.localToScene(0, 0).getX();
                final NumberAxis yAxisZ = (NumberAxis) lineChart.getYAxis();
                double AYyS = yAxisZ.localToScene(0, 0).getY();
                double newXMax = (xS - AXxS) / xAxisZ.getScale();
                double newYMin = (yS - AYyS) / yAxisZ.getScale() + yAxisZ.getUpperBound();
                double newXMin = axisController.getNewXMin();
                double newYMax = axisController.getNewYMax();
                xAxisZ.setLowerBound(min(newXMin, newXMax));
                xAxisZ.setUpperBound(max(newXMin, newXMax));
                yAxisZ.setLowerBound(min(newYMin, newYMax));
                yAxisZ.setUpperBound(max(newYMin, newYMax));
                xAxisZ.setTickUnit((max(newXMin, newXMax) - min(newXMin, newXMax)) / 12);
                yAxisZ.setTickUnit((max(newYMin, newYMax) - min(newYMin, newYMax)) / 10);
                rect.setWidth(0);
                rect.setHeight(0);
            }
        });
        zoomingNode.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                final NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
                final NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();
                // todo: implement to go back to last settings (not all the way back to default)
                if (lineChart.getId().equals("impedanceLineChart")) {
                    resetAxis(xAxis, DEFAULT_X_MIN_DIST, DEFAULT_X_MAX_DIST);
                    resetAxis(yAxis, DEFAULT_Y_MIN_DIST, DEFAULT_Y_MAX_DIST);
                } else if (lineChart.getId().equals("impedanceLineChartF")) {
                    resetAxis(xAxis, DEFAULT_X_MIN_FREQ, DEFAULT_X_MAX_FREQ);
                    resetAxis(yAxis, DEFAULT_Y_MIN_FREQ, DEFAULT_Y_MAX_FREQ);
                }
            }
        });
    }

    private void handleAxisClick(MouseEvent event) {
        Dialog<String> setXDialog = new Dialog<>();
        setXDialog.setTitle("Reset Axis Range");
        Label minXLabel = new Label("Min: ");
        Label maxXLabel = new Label("Max: ");
        TextField minXInput = new TextField();
        TextField maxXInput = new TextField();
        GridPane setXGrid = new GridPane();
        setXGrid.add(minXLabel, 1, 1);
        setXGrid.add(minXInput, 2, 1);
        setXGrid.add(maxXLabel, 3, 1);
        setXGrid.add(maxXInput, 4, 1);
        setXDialog.getDialogPane().setContent(setXGrid);
        ButtonType buttonOk = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        setXDialog.getDialogPane().getButtonTypes().add(buttonOk);
        setXDialog.showAndWait();
        try {
            Double minInput = Double.parseDouble(minXInput.getText());
            Double maxInput = Double.parseDouble(maxXInput.getText());
            if (event.getSource().equals(yAxis)) {
                resetAxis(yAxis, minInput, maxInput);
            } else if (event.getSource().equals(xAxis)) {
                resetAxis(xAxis, minInput, maxInput);
            } else if (event.getSource().equals(xAxisF)) {
                resetAxis(xAxisF, minInput, maxInput);
            } else if (event.getSource().equals(yAxisF)) {
                resetAxis(yAxisF, minInput, maxInput);
            }
        } catch (NumberFormatException e) {
            Alert notConvertibleToDouble = new Alert(Alert.AlertType.ERROR);
            notConvertibleToDouble.setTitle("Input Type Error");
            notConvertibleToDouble.setHeaderText("Couldn't convert input!");
            notConvertibleToDouble.setContentText("Please provide a format like '12.553', '-20', ...");
            notConvertibleToDouble.showAndWait();
        }
    }

    private void setWorkingDirectory() {
        Stage secondaryStage = new Stage();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(secondaryStage);

        if (selectedDirectory == null) {
            workingDirectoryTextField.setPromptText("Invalid directory selected, please try again.");
        } else {
            workingDirectoryTextField.setText(selectedDirectory.getAbsolutePath());
            try {
                setFileTreeView(selectedDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
