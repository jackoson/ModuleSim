package simulator;

import gui.GUI;
import gui.Ticker;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.swing.*;
import java.net.URL;

/**
 * Just does initialisation for the program
 * @author aw12700
 *
 */
public class App extends Application {

	public static GUI ui = null;
	public static Sim sim = null;

	/**
	 * Program starting point
	 * @param args System arguments
	 */
	public static void main(String[] args) {
		launch(args);

		// Set up GUI thread
		/*SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// (In the new thread)
				ui = new GUI();
				ui.generateUI();
				ui.showUI(true);

				// Start render ticking
				Thread t = new Thread(new Ticker());
				t.start();

				// Start sim ticking - sim is initialized below *before* this is called
				sim.start();
			}
		});

		// Set up simulator
		sim = new Sim();*/
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Hello");

		URL fxml_location = GUI.class.getResource("mainUI.fxml");
		Parent root = FXMLLoader.load(fxml_location);
		primaryStage.setScene(new Scene(root, 300, 250));
		primaryStage.show();
	}
}
