package de.frauas.scs;

import java.io.IOException;
import java.io.InputStream;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

//import org.apache.log4j.Logger;

import de.frauas.scs.exception.ThrowableConverter;
import de.frauas.scs.gui.DisplayController;
import de.frauas.scs.gui.DisplayControllerHolder;

/**
 * Main class which starts the application.
 *
 */
public class Main extends Application {

	//private final static Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(String[] args)
	{
		launch(args);
	}

	private DisplayController mainController;

	@Override
	public void start(Stage stage)
	{
		try
		{
			stage.setTitle("Insuline/Glucagon Pump");
			Pane mainPane = loadMainPane();
			Scene scene = new Scene(mainPane);
			stage.setScene(scene);
			stage.show();

			prepareTimeline();
		//	LOGGER.info("GUI successfully loaded");
		} catch (Exception e)
		{
			final String message = "Unexpected Exception in main function \n"
					+ ThrowableConverter.convertStackTrace(e);
		//	LOGGER.error(message);
		}
	}

	/**
	 * Loads main.fxml which represents the view
	 */
	private Pane loadMainPane()
	{
		Pane mainPane = null;
		try
		{
			FXMLLoader loader = new FXMLLoader();
			InputStream i = getClass().getClassLoader().getResourceAsStream("main.fxml");
			mainPane = (Pane) loader.load(i);
			mainController = (DisplayController) loader.getController();
			DisplayControllerHolder.setController(mainController);

		} catch (IOException e)
		{
		//	LOGGER.error("IOException while loading main.fxml", e);
		} catch (RuntimeException e1)
		{
		//	LOGGER.error("RuntimeException while loading main.fxml", e1);
		}
		return mainPane;
	}

	/**
	 * This function i.e. the Animationtimer will run each time the gui is
	 * updated (according to Oracle up to 60 fps). This function is used to
	 * initiate the update of the graph with new values.
	 */
	private void prepareTimeline()
	{
		new AnimationTimer() {
			@Override
			public void handle(long now)
			{
				mainController.updateGui();
			}
		}.start();
	}
}
