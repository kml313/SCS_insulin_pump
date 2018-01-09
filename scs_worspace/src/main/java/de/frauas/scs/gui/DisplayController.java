package de.frauas.scs.gui;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;
import de.frauas.scs.BloodStream;
import de.frauas.scs.simulation.GlucoseLevelSimulator;
import de.frauas.scs.simulation.PancreasSimulator;

/**
 * The MainController contains all elements of the GUI and hence is capable of
 * reacting to action events (e.g. mouse clicks) and is as well the contact
 * point regarding changes on it. The structure of the GUI is specified in the
 * FXML document "main.fxml"
 *
 * @author younes.el.ouarti
 *
 */
public class DisplayController {

	@FXML
	private Label messageBox;

	/**
	 * Number of points on the x-axis. Each point on this axis will be assigned
	 * to a value on the y-axis. The JavaFX engine then connects those dots.
	 */
	private final static int NUM_OF_X_AXIS_SLICES = 300;

	public void clearErrorMessage()
	{
		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				messageBox.setText("");
				messageBox.setVisible(false);
			}
		});
	}

	public void printConversionErrorMessage()
	{

		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				messageBox.setText(MessageBoxText.NOT_A_NUMBER_ERROR.getText());
				messageBox.setVisible(true);
			}
		});
	}

	public void printError(final String text)
	{
		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				messageBox.setText(text);
				messageBox.setVisible(true);
			}
		});

	}

	public void printExceptionMessage()
	{

		Platform.runLater(new Runnable() {
			@Override
			public void run()
			{
				messageBox.setTextFill(Color.RED);
				messageBox.setText(MessageBoxText.EXCEPTION_MESSAGE.getText());
				messageBox.setVisible(true);
			}
		});
	}


	@FXML
	private TextField balancingRangeMaximum;

	@FXML
	private TextField balancingRangeMinimum;

	private BloodStream bloodStream = BloodStream.getInstance();

	@FXML private Text actiontarget;

	@FXML
	private Button consumeButton;

	@FXML
	private Button StartButton;


	@FXML
	private ProgressBar consumptionBar;

	@FXML
	private TextField consumptionField;

	private SimpleDateFormat dateFormatter;

	@FXML
	private Label elapsedTime;

	private GlucoseLevelSimulator glucoseLevelSimulator = new GlucoseLevelSimulator();

	@FXML
	private LineChart<Number, Number> lineChart;

	private PancreasSimulator pancreasSimulator = new PancreasSimulator();

	@FXML
	private TextField readjustmentNegativeInjectionBoundary;

	@FXML
	private TextField readjustmentPositiveInjectionBoundary;

	private Series<Number, Number> series;

	@FXML
	private CheckBox simulationCheckBox;

	@FXML
	private Label statusLabel;

	@FXML
	private Circle statusLed;

	@FXML
	private CheckBox throwError;

	@FXML
	private NumberAxis xAxis;

	private double xSeriesData = 0;

	@FXML
	private NumberAxis yAxis;

	@FXML
	private GridPane doctorpane;
	@FXML private TextField user;

	@FXML private TextField password;

	@FXML private Button loginButton;
	@FXML private VBox Plane;

	public DisplayController() {
		dateFormatter = new SimpleDateFormat("mm ss");
		dateFormatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Initiates the consumption process of the entered calories
	 *
	 * @param e
	 */
	@FXML
	public void login(ActionEvent e) throws IOException {

		if(user.getText().equals("doctor")&&password.getText().equals("password")&&loginButton.getText().equals("Sign In")){
			doctorpane.setVisible(true);
			loginButton.setText("Sign Out");
			messageBox.setTextFill(Color.GREEN);
			messageBox.setText("Doctors's Panel Opened");
			messageBox.setVisible(true);


		}
		else if(loginButton.getText().equals("Sign Out")){
			doctorpane.setVisible(false);
			loginButton.setText("Sign In");
			messageBox.setTextFill(Color.GREEN);
			messageBox.setText("Signed Out Successfully");
			messageBox.setVisible(true);
		}
		else{
			messageBox.setTextFill(Color.RED);
			messageBox.setText("Wrong Credentials.");
			messageBox.setVisible(true);
		}

	}
	@FXML
	public void consume(ActionEvent e)
	{
		try
		{
			double amountToConsume = 0;
			amountToConsume = getAmountToConsume();
			if (amountToConsume != 0)
			{
				messageBox.setVisible(false);
				glucoseLevelSimulator.consume(amountToConsume);
			}
		} catch (NumberFormatException ex)
		{
			messageBox.setText(MessageBoxText.NOT_A_NUMBER_ERROR.getText());
			messageBox.setVisible(true);
		}
		consumptionField.setText("");
	}
	@FXML
	public void start(ActionEvent e){
		Plane.setVisible(true);
		StartButton.setVisible(false);

	}

	@SuppressWarnings("unchecked")
	public void initialize()
	{
		bindProperties();
		messageBox.setVisible(false);
		series = new XYChart.Series<Number, Number>();
		lineChart.getData().addAll(series);
		glucoseLevelSimulator.start();
		pancreasSimulator.start();
		doctorpane.setVisible(false);
		Plane.setVisible(false);
	}

	public void updateGui()
	{
		ConcurrentLinkedQueue<Number> glucoseLevelQueue = BloodStream.getInstance().getGlucoseLevelGuiQueue();

		Double currentGlucoseLevelValue = (Double) glucoseLevelQueue.peek();
		if (currentGlucoseLevelValue != null)
		{
			evaluateCurrentStatus(currentGlucoseLevelValue);
		}

		/*
		 * "series" contains the actual values which are plotted. Those values
		 * are retrieved from the glucose level of BloodStream. The BloodStream
		 * on the other hand gets its values from the GlucoseLevelSimulator.
		 */
		int numOfPendingValues = glucoseLevelQueue.size();
		for (int i = 0; i < numOfPendingValues; i++)
		{
			series.getData().add(new Data<Number, Number>(xSeriesData++, glucoseLevelQueue.remove()));
		}

		/*
		 * If data is only added to series and not removed at some point, then
		 * the graph will be squeezed by the time since the graph has a limited
		 * window size. Therefore it is important to remove all data which
		 * becomes "out dated" (the graph is intended to only to show the latest
		 * x seconds (e.g. 30s). Any data older then that, will be removed).
		 */
		if (series.getData().size() > NUM_OF_X_AXIS_SLICES)
		{
			series.getData().remove(0, series.getData().size()
					- NUM_OF_X_AXIS_SLICES);
		}

		updateTimeStamp();

		/*
		 * The setting of the lower and upper bound of the x axis results in a
		 * moving graph. This enables the view of a specific time frame.
		 */
		xAxis.setLowerBound(xSeriesData - NUM_OF_X_AXIS_SLICES);
		xAxis.setUpperBound(xSeriesData - 1);
	}

	private void bindProperties()
	{
		consumptionBar.progressProperty().bindBidirectional(glucoseLevelSimulator.getConsumptionProgress());
		consumeButton.disableProperty().bindBidirectional(glucoseLevelSimulator.getIsConsumingProperty());
		consumptionField.disableProperty().bindBidirectional(glucoseLevelSimulator.getIsConsumingProperty());
		simulationCheckBox.disableProperty().bindBidirectional(glucoseLevelSimulator.getIsConsumingProperty());
		simulationCheckBox.selectedProperty().bindBidirectional(glucoseLevelSimulator.getIsBalancedProperty());
		throwError.selectedProperty().bindBidirectional(glucoseLevelSimulator.getShouldThrowErrorProperty());
		balancingRangeMaximum.textProperty().bindBidirectional(pancreasSimulator.getBalancingRangeMaximumProperty());
		balancingRangeMinimum.textProperty().bindBidirectional(pancreasSimulator.getBalancingRangeMinimumProperty());
		readjustmentNegativeInjectionBoundary.textProperty().bindBidirectional(pancreasSimulator.getReadjustmentNegativeInjectionBoundaryProperty());
		readjustmentPositiveInjectionBoundary.textProperty().bindBidirectional(pancreasSimulator.getReadjustmentPositiveInjectionBoundaryProperty());
	}

	private void evaluateCurrentStatus(double currentGlucoseLevelValue)
	{
		if (currentGlucoseLevelValue > 17 || currentGlucoseLevelValue < 3)
		{
			setStatus(GlucoseLevelStatus.CRITICAL);
		} else if (currentGlucoseLevelValue > 4 && currentGlucoseLevelValue < 8)
		{
			setStatus(GlucoseLevelStatus.GOOD);
		} else
		{
			setStatus(GlucoseLevelStatus.ALERTING);
		}
	}

	private double getAmountToConsume()
	{
		double amountToConsume = 0;
		String fieldText = consumptionField.getText();
		if (!fieldText.isEmpty())
		{
			amountToConsume = Double.parseDouble(fieldText);
		}
		return amountToConsume;
	}

	private void setStatus(GlucoseLevelStatus status)
	{
		statusLabel.setText(status.getValue());
		switch (status)
		{
			case ALERTING:
				statusLed.setFill(Color.ORANGE);
				statusLed.setStroke(Color.ORANGE);
				break;
			case CRITICAL:
				statusLed.setFill(Color.CRIMSON);
				statusLed.setStroke(Color.CRIMSON);
				break;
			case GOOD:
				statusLed.setFill(Color.GREEN);
				statusLed.setStroke(Color.GREEN);
				break;
			default:
				break;
		}
	}

	/**
	 * Updates the time stamp on the GUI
	 */
	private void updateTimeStamp()
	{
		String rawTimeStamp = dateFormatter.format(bloodStream.getElapsedTime());
		String[] timeStamp = rawTimeStamp.split(" ");
		String updatedTime = timeStamp[0] + "h " + timeStamp[1] + "min";
		elapsedTime.setText(updatedTime);
	}

}
