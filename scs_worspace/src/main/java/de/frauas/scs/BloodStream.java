package de.frauas.scs;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.frauas.scs.simulation.GlucoseLevelSimulator;

/**
 * BloodStream is a singleton and serves as data storage which should be
 * accessible by any object of this program. It mimics the blood stream of the
 * patient and therefore contains the current value of the blood glucose level
 * and other meaningful data which would be ubiquitous in real life for any
 * component of the pump.
 * 
 * @author younes.el.ouarti
 *
 */
public class BloodStream {

	private static final double AMOUNT_OF_BLOOD_IN_LITER = 5;

	private static BloodStream instance;

	private static long simulationStart;

	/**
	 * @return Amount of blood inside the blood stream (body)
	 */
	public static double getAmountOfBloodInLiter()
	{
		return AMOUNT_OF_BLOOD_IN_LITER;
	}

	/**
	 * Gets the singleton object
	 * 
	 * @return The singleton BloodStream object
	 */
	public static BloodStream getInstance()
	{
		if (BloodStream.instance == null)
		{
			BloodStream.instance = new BloodStream();
			Date buffer = new Date();
			simulationStart = buffer.getTime();
		}
		return BloodStream.instance;
	}

	/**
	 * Time in milliseconds since the simulation started.
	 */
	private Date elapsedTimeInMillis;
	private ConcurrentLinkedQueue<Number> glucoseLevelControlQueue = new ConcurrentLinkedQueue<Number>();

	/**
	 * Contains the values calculated by {@link GlucoseLevelSimulator}. The
	 * simulator adds new values in predefined time frames (e.g. each 100ms).
	 */
	private ConcurrentLinkedQueue<Number> glucoseLevelGuiQueue = new ConcurrentLinkedQueue<Number>();

	private ArrayList<ConcurrentLinkedQueue<Double>> hormoneInjections = new ArrayList<>();

	private BloodStream() {
		super();
	}

	/**
	 * Gets the current elapsed time in milliseconds
	 * 
	 * @return elapsed time in ms.
	 */
	public Date getElapsedTime()
	{
		return elapsedTimeInMillis;
	}

	public ConcurrentLinkedQueue<Number> getGlucoseLevelControlQueue()
	{
		return glucoseLevelControlQueue;
	}

	/**
	 * Gets {@link #glucoseLevelGuiQueue}
	 * 
	 * @return {@link #glucoseLevelGuiQueue}
	 */
	public ConcurrentLinkedQueue<Number> getGlucoseLevelGuiQueue()
	{
		return glucoseLevelGuiQueue;
	}

	public double getHormonalEffect()
	{
		double effect = 0;
		if (!hormoneInjections.isEmpty())
		{
			effect = calculateEffect();
			// clean up empty injections from arraylist
			while (cleanupQueue())
				;
		}
		return effect;
	}

	public synchronized ArrayList<ConcurrentLinkedQueue<Double>> getHormoneInjections()
	{
		return hormoneInjections;
	}

	/**
	 * Sets the current elapsed time in milliseconds
	 * 
	 * @param difference
	 *            Is the difference in milliseconds of the start time and the
	 *            current time
	 */
	public void updateElapsedTime()
	{
		elapsedTimeInMillis = new Date(System.currentTimeMillis()
				- simulationStart);
	}

	private double calculateEffect()
	{
		double effect = 0;
		for (ConcurrentLinkedQueue<Double> injection : hormoneInjections)
		{
			if (!injection.isEmpty())
			{
				effect = injection.remove();
			}
		}
		return effect;
	}

	private boolean cleanupQueue()
	{
		int size = hormoneInjections.size();
		boolean isCleanNecessary = false;
		for (int i = 0; i < size; i++)
		{
			ConcurrentLinkedQueue<Double> queue = hormoneInjections.get(i);
			if (queue.isEmpty())
			{
				hormoneInjections.remove(i);
				isCleanNecessary = true;
				return isCleanNecessary;
			}
		}
		return isCleanNecessary;
	}

}
