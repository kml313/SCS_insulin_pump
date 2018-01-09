package de.frauas.scs.gui;

public class DisplayControllerHolder {

	private static DisplayController controller;

	public static DisplayController getController() {
		return controller;
	}

	public static void setController(DisplayController controller) {
		DisplayControllerHolder.controller = controller;
	}
}
