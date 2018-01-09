package de.frauas.scs.simulation;

import static de.frauas.scs.gui.DisplayControllerHolder.getController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import de.frauas.scs.BloodStream;

public class GlucoseLevelSimulator extends AbstractSimulator {

	private final static int SIMULATION_STEPS_IN_MILLIS = 100;

	private double consumptionInProgress_mmol_L = 0;

	private DoubleProperty consumptionProgress = new SimpleDoubleProperty(0);

	/**
	 * This value represents the offset which will be added to the regular value
	 * (which has the value between 5.5 - 5.833) <i><b>in the next step of the
	 * simulation</b></i>.
	 */
	private double currentGlucoseLevelOffset = 0;

	private final ConcurrentLinkedQueue<Double> glucoseLevelOffsetSeries = new ConcurrentLinkedQueue<>();

	private BooleanProperty isBalanced = new SimpleBooleanProperty(true);

	private BooleanProperty isBalancing = new SimpleBooleanProperty(false);

	private BooleanProperty shouldThrowError = new SimpleBooleanProperty(false);

	private BooleanProperty isConsuming = new SimpleBooleanProperty();

	/**
	 * Specifies the maximum amount of kcal which can be consumed at once.
	 */
	private final int MAX_AMOUNT_KCAL_IN_2H = 2000;

	private final double MIN_AMOUNT_KCAL_IN_30MIN = -500d;

	private final int MAX_NEGATIVE_INTAKE_DURATION = 300;

	/**
	 * When consuming an amount of energy, then a maximal limit must be
	 * specified where all the energy is consumed. Here all the energy that is
	 * taken in, will be consumed in 2 hours ([MAX_INTAKE_DURATION] = 10
	 * seconds). An upper limit of how many kcal can be consumed at once is
	 * specified {@link #MAX_AMOUNT_KCAL_IN_2H}.
	 */
	private final int MAX_POSITIVE_INTAKE_DURATION = 1200;

	private SimulatorThread simulatorThread;

	private DoubleProperty totalAmountInConsumption_mmol_L = new SimpleDoubleProperty(0);

	public GlucoseLevelSimulator() {
		super();
		try
		{
			isConsuming.setValue(false);
			executor = Executors.newCachedThreadPool(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r)
				{
					Thread thread = new Thread(r);
					thread.setDaemon(true);
					return thread;
				}
			});
		} catch (Exception e)
		{
			getController().printExceptionMessage();
		}
	}

	public void consume(double amountToConsume)
	{
		isConsuming.setValue(true);
		double amount_mmol_L = calculateGlucoseLevelIncrease_Mmol_L(amountToConsume);
		totalAmountInConsumption_mmol_L.setValue(amount_mmol_L);
		if (amountToConsume > 0)
		{
			glucoseLevelOffsetSeries.addAll(generateSeriesOfPositivGlucoseLevelOffsets(amount_mmol_L));
		} else
		{
			glucoseLevelOffsetSeries.addAll(generateSeriesOfNegativeGlucoseLevelOffsets(amount_mmol_L));
		}
	}

	public Property<Number> getConsumptionProgress()
	{
		return consumptionProgress;
	}

	public Property<Boolean> getIsBalancedProperty()
	{
		return isBalanced;
	}

	public Property<Boolean> getIsBalancingProperty()
	{
		return isBalancing;
	}

	public Property<Boolean> getIsConsumingProperty()
	{
		return isConsuming;
	}

	public DoubleProperty getTotalAmountInConsumption_mmol_L()
	{
		return totalAmountInConsumption_mmol_L;
	}

	/**
	 * Starts the simulation
	 */
	public void start()
	{
		simulatorThread = new SimulatorThread();
		executor.execute(simulatorThread);
	}

	protected double calculateGlucoseLevelIncrease_Mmol_L(Double kcal)
	{
		kcal = verifyKcalWithinBoundary(kcal);

		if (kcal > 0)
		{
			// Half of the calories are assumed to be fat and protein
			kcal /= 2;
		}

		// Equivalent amount of carbs (=glucose) in mg
		// 1 g carbs <=> 4 kcal energy
		double amountOfCarbs_mg = (kcal / 4) * 1000;

		// amount of blood in body in dl
		double amountOfBlood_dL = BloodStream.getAmountOfBloodInLiter() * 10;

		// total amount of glucose in mg/dl which will be consumed
		double glucoseLevelIncrease_mg_dL = amountOfCarbs_mg / amountOfBlood_dL;

		double glucoseLevelIncrease_mmol_L = glucoseLevelIncrease_mg_dL / 18;

		return glucoseLevelIncrease_mmol_L;

	}

	private Double verifyKcalWithinBoundary(Double kcal)
	{
		if (kcal > MAX_AMOUNT_KCAL_IN_2H)
		{
			kcal = (double) MAX_AMOUNT_KCAL_IN_2H;
		}

		if (kcal < -500)
		{
			kcal = MIN_AMOUNT_KCAL_IN_30MIN;
		}
		return kcal;
	}

	protected Collection<? extends Double> generateSeriesOfNegativeGlucoseLevelOffsets(
			double glucoseLevelDecrease_mmol_L)
	{
		ArrayList<Double> result = new ArrayList<>(MAX_NEGATIVE_INTAKE_DURATION);
		result.addAll(0, getOffsetSerie(glucoseLevelDecrease_mmol_L, MAX_POSITIVE_INTAKE_DURATION));
		return result;
	}

	protected ArrayList<Double> generateSeriesOfPositivGlucoseLevelOffsets(
			double glucoseLevelIncrease_mmol_L)
	{
		ArrayList<Double> result = new ArrayList<>(MAX_POSITIVE_INTAKE_DURATION);

		// The rise will be in 5 phases:
		// init: 10% of glucose in 25.0% of the total time
		// peak: 45% of glucose in 12.5% of the total time
		// rest_1: 20% of glucose in 12.5% of the total time
		// rest_2: 15% of glucose in 25.0% of the total time
		// rest_3: 10% of glucose in 25.0% of the total time

		int iterator = 0;
		double glucoseAmountInInit = glucoseLevelIncrease_mmol_L * 0.1;
		int timeSlicesInInit = (int) (MAX_POSITIVE_INTAKE_DURATION * 0.25);
		result.addAll(iterator, getOffsetSerie(glucoseAmountInInit, timeSlicesInInit));

		double glucoseAmountInPeak = glucoseLevelIncrease_mmol_L * 0.45;
		int timeSlicesInPeak = (int) (MAX_POSITIVE_INTAKE_DURATION * 0.125);
		iterator = timeSlicesInInit - 1;
		result.addAll(iterator, getOffsetSerie(glucoseAmountInPeak, timeSlicesInPeak));

		double glucoseAmountInRest_1 = glucoseLevelIncrease_mmol_L * 0.20;
		int timeSlicesInRest_1 = (int) (MAX_POSITIVE_INTAKE_DURATION * 0.125);
		iterator += timeSlicesInPeak;
		result.addAll(iterator, getOffsetSerie(glucoseAmountInRest_1, timeSlicesInRest_1));

		double glucoseAmountInRest_2 = glucoseLevelIncrease_mmol_L * 0.15;
		int timeSlicesInRest_2 = (int) (MAX_POSITIVE_INTAKE_DURATION * 0.25);
		iterator += timeSlicesInRest_1;
		result.addAll(iterator, getOffsetSerie(glucoseAmountInRest_2, timeSlicesInRest_2));

		double glucoseAmountInRest_3 = glucoseLevelIncrease_mmol_L * 0.10;
		int timeSlicesInRest_3 = (int) (MAX_POSITIVE_INTAKE_DURATION * 0.25);
		iterator += timeSlicesInRest_2;
		result.addAll(iterator, getOffsetSerie(glucoseAmountInRest_3, timeSlicesInRest_3));

		return result;
	}

	protected ArrayList<Double> getOffsetSerie(double amount, int timeSlices)
	{
		double distiributedAmount = amount / timeSlices;
		ArrayList<Double> result = new ArrayList<>(timeSlices);
		for (int i = 0; i < timeSlices; i++)
		{
			result.add(i, distiributedAmount);
		}
		return result;
	}

	/**
	 * Generates a value between 5.5 and 5.833.<br>
	 * This value represents the natural blood glucose level.
	 * 
	 * @return generated value
	 */
	protected double getRegularValue()
	{
		double value = 5.5;
		/*
		 * Since Math.random() always generates values between 0.0 and 1.0, the
		 * generated naturalOffset is always between 5.5 and 5.833
		 */
		double naturalOffset = Math.random() / 5;
		return value + naturalOffset;
	}

	private class SimulatorThread implements Runnable {
		public void run()
		{
			try
			{
				/*
				 * This exception is only thrown for demonstration/simulation
				 * purposes!!!
				 */
				if (shouldThrowError.getValue() == true)
				{
					throw new RuntimeException();
				}
				ConcurrentLinkedQueue<Number> glucoseLevelGui = bloodStream.getGlucoseLevelGuiQueue();

				ConcurrentLinkedQueue<Number> glucoseLevelControl = bloodStream.getGlucoseLevelControlQueue();
				double regularValue = getRegularValue();
				if (!glucoseLevelOffsetSeries.isEmpty())
				{
					double nextOffset = glucoseLevelOffsetSeries.remove();
					currentGlucoseLevelOffset += nextOffset;
					updateProgress(nextOffset);
				} else
				{
					isConsuming.setValue(false);
					consumptionInProgress_mmol_L = 0;
					consumptionProgress.setValue(0);
				}

				if (isBalanced.getValue())
				{
					final double hormonalEffect = bloodStream.getHormonalEffect();
					currentGlucoseLevelOffset += hormonalEffect;
					setBalancingIcon(hormonalEffect);
				}
				double valueToAdd = regularValue + currentGlucoseLevelOffset;
				glucoseLevelGui.add(valueToAdd);
				glucoseLevelControl.add(valueToAdd);
				try
				{
					Thread.sleep(SIMULATION_STEPS_IN_MILLIS);
					bloodStream.updateElapsedTime();
					executor.execute(this);
				} catch (InterruptedException ex)
				{
					getController().printExceptionMessage();
				}
			} catch (RuntimeException e)
			{
				getController().printExceptionMessage();
			}
		}

		private void setBalancingIcon(final double hormonalEffect)
		{
			Platform.runLater(new Runnable() {
				@Override
				public void run()
				{
					if (hormonalEffect == 0)
					{
						isBalancing.setValue(false);
					} else
					{
						isBalancing.setValue(true);
					}
				}
			});
		}

		private void updateProgress(double currentAmount)
		{
			consumptionInProgress_mmol_L += currentAmount;
			double progress = consumptionInProgress_mmol_L
					/ totalAmountInConsumption_mmol_L.doubleValue();
			consumptionProgress.setValue(progress);
		}

	}

	public Property<Boolean> getShouldThrowErrorProperty()
	{
		return shouldThrowError;
	}

}
