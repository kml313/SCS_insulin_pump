package de.frauas.scs.simulation;

import static de.frauas.scs.gui.DisplayControllerHolder.getController;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import de.frauas.scs.BloodStream;
import de.frauas.scs.gui.MessageBoxText;

/**
 * @author sanjeevthapa
 * 
 */
public class PancreasSimulator extends AbstractSimulator {

	private boolean isErrorMessageShown = false;

	protected class SimulatorThread implements Runnable {

		public void run()
		{
			try
			{
				if (isDoctorsPaneValid())
				{
					analyzebloodstream = bloodStream.getGlucoseLevelControlQueue();
					if (!analyzebloodstream.isEmpty())
						sliceTotalValue = (Double) analyzebloodstream.peek();
					analyzebloodstream.clear();
					if (!isInNonBalancingRange(sliceTotalValue))
						if (initial_value == 0)
						{
							initial_value = sliceTotalValue;
							meanDataSet.add(initial_value);
						} else
						{
							meanDataSet.add(sliceTotalValue);
							analyzeSetsOfDatas(sliceTotalValue);
						}
				}
				try
				{
					Thread.sleep(SIMULATION_STEPS_IN_MILLIS);
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

		protected void analyzeSetsOfDatas(double valueForCheck)
		{
			try
			{
				if (!isInjected)
				{
					final_value = meanDataSet.get(meanDataSet.size() - 1);
					if (meanDataSet.get(0) == 0.0)
						initial_value = meanDataSet.get(1);
					else
						initial_value = meanDataSet.get(0);

					double difference = final_value - initial_value;
					if (difference > 0)
						total_fluctuation = difference * 30 * 10;
					else
						// System.out.println(meanDataSet);
						total_fluctuation = difference * 30;

					genericIncrease = total_fluctuation;
					if (difference > 0)
					{
						System.out.println("series rising make it donw");
						glucoseLevelOffsetSeries.addAll(generateSeriesBalancingOffsets(genericIncrease, 1200d, "down"));
					} else
					{
						System.out.println("sugal level going down make it up");
						System.out.println(valueForCheck);
						glucoseLevelOffsetSeries.addAll(generateSeriesBalancingOffsets(10, 50d, "up"));
					}

					injectedInsulin = injectedInsulin + total_fluctuation;
					ArrayList<ConcurrentLinkedQueue<Double>> hormonalInjections = BloodStream.getInstance().getHormoneInjections();
					hormonalInjections.add(glucoseLevelOffsetSeries);
					isInjected = true;
					System.out.println("total increase/decrease caluculated at a glance is"
							+ total_fluctuation);

				} else
				{
					// Default values
					double readjustmentNegativeInjectionBoundary = 8;
					double readjustmentPositiveInjectionBoundary = 5.5;
					try
					{
						readjustmentNegativeInjectionBoundary = Double.valueOf(readjustmentNegativeInjectionBoundaryProperty.getValue());
						readjustmentPositiveInjectionBoundary = Double.valueOf(readjustmentPositiveInjectionBoundaryProperty.getValue());
					} catch (NumberFormatException e)
					{
						getController().printConversionErrorMessage();
					}
					// Default = 8
					if (valueForCheck > readjustmentNegativeInjectionBoundary)
					{
						ConcurrentLinkedQueue<Double> newInjection = new ConcurrentLinkedQueue<>();
						if (valueForCheck > 10)
							if (genericIncrease > 60)
								newInjection.addAll(generateSeriesBalancingOffsets(genericIncrease / 3, 100d, "down"));
							else
								newInjection.addAll(generateSeriesBalancingOffsets(genericIncrease / 5, 100d, "down"));
						else if (valueForCheck > 8 && valueForCheck < 10)
							newInjection.addAll(generateSeriesBalancingOffsets(5, 50d, "down"));
						else
							newInjection.addAll(generateSeriesBalancingOffsets(3, 50d, "down"));

						ArrayList<ConcurrentLinkedQueue<Double>> hormonalInjections = BloodStream.getInstance().getHormoneInjections();
						injectedInsulin = injectedInsulin
								+ (genericIncrease / (injectedTimes + 2));
						hormonalInjections.add(newInjection);
						injectedTimes++;
						// Default = 5.5
					} else if (valueForCheck < readjustmentPositiveInjectionBoundary)
					{
						ConcurrentLinkedQueue<Double> newInjectionNegative = new ConcurrentLinkedQueue<>();

						if (genericIncrease < 0)
						{
							newInjectionNegative.addAll(generateSeriesBalancingOffsets(3, 50d, "up"));
						}

						else
						{
							newInjectionNegative.addAll(generateSeriesBalancingOffsets(0.8, 50d, "up"));
						}

						ArrayList<ConcurrentLinkedQueue<Double>> hormonalInjections = BloodStream.getInstance().getHormoneInjections();
						injectedInsulin = injectedInsulin
								+ (genericIncrease / (injectedTimes + 2));
						hormonalInjections.add(newInjectionNegative);
						injectedTimes++;
					}// end else if valueforcheck less than 6

				}
			} catch (Exception e)
			{
				getController().printExceptionMessage();
			}

		}

		/*
		 * Former isInBetweenFiveAndSix. Name is changed because it doesn't look
		 * if it is ONLY between 5 and 6
		 */

		protected ArrayList<Double> generateSeriesBalancingOffsets(
				double glucoseLevelIncrease_mmol_L, double slots, String mode)
		{
			if (mode == "up")
			{
				glucoseLevelIncrease_mmol_L = Math.abs(glucoseLevelIncrease_mmol_L);
			}
			ArrayList<Double> result = new ArrayList<>(MAX_INTAKE_DURATION);
			int iterator = 0;
			double insuilinAmountInInit = glucoseLevelIncrease_mmol_L * 0.2;
			int timeSlicesInInit = (int) (slots * 0.2);
			result.addAll(iterator, getOffsetSerie(insuilinAmountInInit, timeSlicesInInit, mode));

			double insulinAmountInRest_1 = glucoseLevelIncrease_mmol_L * 0.4;
			int timeSlicesInRest_1 = (int) (MAX_INTAKE_DURATION * 0.4);
			iterator += timeSlicesInInit;
			result.addAll(iterator, getOffsetSerie(insulinAmountInRest_1, timeSlicesInRest_1, mode));

			double insulinAmountInRest_2 = glucoseLevelIncrease_mmol_L * 0.2;
			int timeSlicesInRest_2 = (int) (MAX_INTAKE_DURATION * 0.20);
			iterator += timeSlicesInRest_1;
			result.addAll(iterator, getOffsetSerie(insulinAmountInRest_2, timeSlicesInRest_2, mode));

			double insulinAmountInRest_3 = glucoseLevelIncrease_mmol_L * 0.2;
			int timeSlicesInRest_3 = (int) (MAX_INTAKE_DURATION * 0.2);
			iterator += timeSlicesInRest_2;
			result.addAll(iterator, getOffsetSerie(insulinAmountInRest_3, timeSlicesInRest_3, mode));

			return result;
		}

		protected ArrayList<Double> getOffsetSerie(double amount,
				int timeSlices, String modeUpDown)
		{
			// System.out.println(modeUpDown);
			double distiributedAmount;
			if (modeUpDown == "down")
				distiributedAmount = -(amount / timeSlices);
			else
				distiributedAmount = (amount / timeSlices);

			ArrayList<Double> result = new ArrayList<>(timeSlices);
			for (int i = 0; i < timeSlices; i++)
			{
				result.add(i, distiributedAmount);
			}
			return result;
		}

		protected boolean isInNonBalancingRange(double val)
		{
			// default
			double max = 6;
			double min = 5;
			try
			{
				max = Double.valueOf(balancingRangeMaximumProperty.getValue());
				min = Double.valueOf(balancingRangeMinimumProperty.getValue());
			} catch (NumberFormatException e)
			{
				getController().printConversionErrorMessage();
			}
			// default:
			// maximum = 6
			// minimum = 5
			if ((val < max) && (val >= min))
			{
				return true;
			} else
			{
				return false;
			}

		}

	}

	private ConcurrentLinkedQueue<Number> analyzebloodstream = new ConcurrentLinkedQueue<>();

	private StringProperty balancingRangeMaximumProperty = new SimpleStringProperty("6");

	private StringProperty balancingRangeMinimumProperty = new SimpleStringProperty("5");

	private double final_value = 0;

	private double genericIncrease = 0;

	private final ConcurrentLinkedQueue<Double> glucoseLevelOffsetSeries = new ConcurrentLinkedQueue<>();

	private double initial_value = 0;

	private Double injectedInsulin = 0d;

	private int injectedTimes = 0;

	private boolean isInjected = false;

	private final int MAX_INTAKE_DURATION = 1200;

	private ArrayList<Double> meanDataSet = new ArrayList<Double>();

	private StringProperty readjustmentNegativeInjectionBoundaryProperty = new SimpleStringProperty("5.5");

	private StringProperty readjustmentPositiveInjectionBoundaryProperty = new SimpleStringProperty("8");

	private long SIMULATION_STEPS_IN_MILLIS = 100 * 10;

	private SimulatorThread simulatorThread;

	private Double sliceTotalValue = 0d;

	private double total_fluctuation = 0;

	public PancreasSimulator() {
		super();
		executor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r)
			{
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	public StringProperty getBalancingRangeMaximumProperty()
	{
		return balancingRangeMaximumProperty;
	}

	public StringProperty getBalancingRangeMinimumProperty()
	{
		return balancingRangeMinimumProperty;
	}

	public StringProperty getReadjustmentNegativeInjectionBoundaryProperty()
	{
		return readjustmentNegativeInjectionBoundaryProperty;
	}

	public StringProperty getReadjustmentPositiveInjectionBoundaryProperty()
	{
		return readjustmentPositiveInjectionBoundaryProperty;
	}

	/**
	 * Starts the simulation
	 */
	public void start()
	{
		try
		{
			simulatorThread = new SimulatorThread();
			executor.execute(simulatorThread);
		} catch (Exception e)
		{
			getController().printExceptionMessage();
		}
	}

	private boolean isDoctorsPaneValid()
	{
		try
		{
			double balancingRangeMaxValue = Double.valueOf(balancingRangeMaximumProperty.getValue());
			double balancingRangeMinValue = Double.valueOf(balancingRangeMinimumProperty.getValue());
			double readjustmentNegativeInjectionBoundaryValue = Double.valueOf(readjustmentNegativeInjectionBoundaryProperty.getValue());
			double readjustmentPositiveInjectionBoundaryValue = Double.valueOf(readjustmentPositiveInjectionBoundaryProperty.getValue());

			if (balancingRangeMaxValue < balancingRangeMinValue)
			{
				getController().printError(MessageBoxText.CONFLICTING_BALANCING_RANGE.getText());
				isErrorMessageShown = true;
				return false;
			}

			if (balancingRangeMaxValue > 17 || balancingRangeMinValue < 4
					|| readjustmentNegativeInjectionBoundaryValue > 17
					|| readjustmentPositiveInjectionBoundaryValue < 4)
			{
				getController().printError(MessageBoxText.IN_CRITICAL_AREA.getText());
				isErrorMessageShown = true;
				return false;
			}

			if (readjustmentPositiveInjectionBoundaryValue < readjustmentNegativeInjectionBoundaryValue)
			{
				getController().printError(MessageBoxText.CONFLICTING_READJUSTMENT_RANGE.getText());
				isErrorMessageShown = true;
				return false;
			}
			if (isErrorMessageShown)
			{
				getController().clearErrorMessage();
				isErrorMessageShown = false;
			}
			return true;
		} catch (NumberFormatException e)
		{
			getController().printConversionErrorMessage();
			return false;
		}
	}

}
