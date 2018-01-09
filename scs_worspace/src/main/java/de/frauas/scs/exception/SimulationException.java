package de.frauas.scs.exception;

import de.frauas.scs.gui.MessageBoxText;

public class SimulationException extends Exception {

	private static final long serialVersionUID = 1L;

	public SimulationException(MessageBoxText message) {
		super(message.getText());
	}

	public SimulationException(String message, Throwable cause) {
		super(message, cause);
	}

}
