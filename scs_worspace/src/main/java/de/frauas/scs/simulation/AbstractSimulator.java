package de.frauas.scs.simulation;

import java.util.concurrent.ExecutorService;

//import org.apache.log4j.Logger;

import de.frauas.scs.BloodStream;

public abstract class AbstractSimulator {

	//protected final static Logger LOGGER = Logger.getLogger(GlucoseLevelSimulator.class);

	protected BloodStream bloodStream = BloodStream.getInstance();

	protected ExecutorService executor;

	public abstract void start();

}
