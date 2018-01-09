package de.frauas.scs.gui;

public enum MessageBoxText {
	NOT_A_NUMBER_ERROR("Only numbers are allowed. Please reenter a value."), EXCEPTION_MESSAGE(
			"An internal error occured!\nPlease restart the device. If error persists, contact your physician immediately!"), CONFLICTING_BALANCING_RANGE(
			"Upper bound for non-balancing must be greater than lowerbound!"), CONFLICTING_READJUSTMENT_RANGE(
			"Readjustment injections of glucagon must be at a lower level than insulin's!"), IN_CRITICAL_AREA(
			"Control input fields are not allowed to be in a critical area. Please readjust entry.");

	MessageBoxText(String text) {
		this.text = text;
	}

	private final String text;

	public String getText()
	{
		return text;
	}
}
