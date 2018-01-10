package de.frauas.scs.gui;

public enum MessageBoxText {
	NOT_A_NUMBER_ERROR("Not a number. Please re-enter a value."), EXCEPTION_MESSAGE(
			"Error occured!\nPlease restart the device. Contact your physician immediately!"), CONFLICTING_BALANCING_RANGE(
			"Upper bound for non-balancing must be greater than lower bound value!"), CONFLICTING_READJUSTMENT_RANGE(
			"Readjustment injections of Glucagon must be at a lower level than insulin's!"), IN_CRITICAL_AREA(
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
