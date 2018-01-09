package de.frauas.scs.gui;

public enum GlucoseLevelStatus {

	GOOD("Good"), ALERTING("Alerting"), CRITICAL("Critical");

	GlucoseLevelStatus(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	private final String propertyValue;

	public String getValue()
	{
		return propertyValue;
	}
}
