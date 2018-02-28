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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

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

    public void initialize() {
        workingDirectoryTextField.setOnMousePressed(event -> {
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
        });
        impedanceLineChart.setAlternativeColumnFillVisible(true);
        impedanceLineChart.setCreateSymbols(false);
        impedanceLineChart.setLegendVisible(false);
        final Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.5));
        zoomRect.setAccessibleText("Rectangle");
        chartContainer.getChildren().add(zoomRect);
        setUpZooming(zoomRect, impedanceLineChart);
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(5000);
        xAxis.setTickUnit(200);
        yAxis.setLowerBound(-20);
        yAxis.setUpperBound(80);
        yAxis.setTickUnit(10);
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
                        }
                        readCsv(currentPath);
                    } else { // todo: make this counter through business nicer
                        int indexToDelete = 0;
                        int counter = 0;
                        for (XYChart.Series<Number, Number> cr : impedanceLineChart.getData()) {
                            if (cr.getName().equals(currentPath.getFileName().toString())) {
                                indexToDelete = counter;
                            } else {
                                counter++;
                            }
                        }
                        impedanceLineChart.getData().remove(indexToDelete);
                    }
                });
            }
        }
        directoryTreeView.setRoot(rootItem);
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
        node.setOnMouseExited(event -> chart.setTitle("Impedance vs Distance"));
        node.setOnMouseEntered(event -> chart.setTitle(label));
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

    private void readCsv(Path pathToCsvFile) {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ",";

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(pathToCsvFile.getFileName().toString());
        String csvHiddenFile = Paths.get(pathToCsvFile.getParent().toString(), "HIDDEN_CALCULATED_IMPDIST_" + pathToCsvFile.getFileName().toString()).toString();
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
        impedanceLineChart.getData().add(series);
        setOnMouseEventOnSeries(series.getNode(), impedanceLineChart, pathToCsvFile.getFileName().toString());
    }

    private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
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
            final NumberAxis xAxisZ = (NumberAxis) impedanceLineChart.getXAxis();
            double AXxS = xAxisZ.localToScene(0, 0).getX();
            final NumberAxis yAxisZ = (NumberAxis) impedanceLineChart.getYAxis();
            double AYyS = yAxisZ.localToScene(0, 0).getY();
            double newXMin = (xS - AXxS) / xAxisZ.getScale();
            double newYMax = (yS - AYyS) / yAxisZ.getScale() + 80.0;
            axisController.setNewXMinYMax(newXMin, newYMax);
        });
        zoomingNode.setOnMouseReleased(event -> {
            double xS = event.getSceneX();
            double yS = event.getSceneY();
            final NumberAxis xAxisZ = (NumberAxis) impedanceLineChart.getXAxis();
            double AXxS = xAxisZ.localToScene(0, 0).getX();
            final NumberAxis yAxisZ = (NumberAxis) impedanceLineChart.getYAxis();
            double AYyS = yAxisZ.localToScene(0, 0).getY();
            double newXMax = (xS - AXxS) / xAxisZ.getScale();
            double newYMin = (yS - AYyS) / yAxisZ.getScale() + 80.0;
            double newXMin = axisController.getNewXMin();
            double newYMax = axisController.getNewYMax();
            xAxisZ.setLowerBound(min(newXMin, newXMax));
            xAxisZ.setUpperBound(max(newXMin, newXMax));
            yAxisZ.setLowerBound(min(newYMin, newYMax));
            yAxisZ.setUpperBound(max(newYMin, newYMax));
            xAxisZ.setTickUnit((max(newXMin, newXMax)- min(newXMin, newXMax)) / 12);
            yAxisZ.setTickUnit((max(newYMin, newYMax) - min(newYMin, newYMax)) / 10);
            rect.setWidth(0);
            rect.setHeight(0);
        });
        zoomingNode.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                xAxis.setLowerBound(0);
                xAxis.setUpperBound(5000);
                xAxis.setTickUnit(200);
                yAxis.setLowerBound(-20);
                yAxis.setUpperBound(80);
                yAxis.setTickUnit(10);
            }
        });
    }
}
