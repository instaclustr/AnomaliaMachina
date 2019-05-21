// CUSUM Change detector, detects changes in both positive and negative directions.
// Based on https://faithfull.me/change-detection-for-software-engineers-part-i-introduction-and-cusum/
// Background theory: https://en.wikipedia.org/wiki/CUSUM

public class CUSUMChangeDetector implements ChangeDetector<Double> {

	    private final static double DEFAULT_MAGNITUDE = 0.05;
	    private final static double DEFAULT_THRESHOLD = 3;
	    private final static long DEFAULT_READY_AFTER = 50; // now set from props

	    private double cusumPrev = 0;
	    private double cusum;
	    private double cusumMin;
	    private double cusumMinPrev = 0;

	    private double magnitude;
	    private double threshold;
	    private double magnitudeMultiplier;
	    private double thresholdMultiplier;
	    private long   readyAfter;

	    private long   observationCount 	= 0;
	    private double runningMean      	= 0.0;
	    private double runningVariance 	= 0.0;

	    private boolean change = false;
	    private boolean changeNeg = false;


	    /**
	     * Create a CUSUM detector
	     * @param magnitudeMultiplier Magnitude of acceptable change in stddevs
	     * @param thresholdMultiplier Threshold in stddevs
	     * @param readyAfter Number of observations before allowing change to be signaled
	     */
	    public CUSUMChangeDetector(double magnitudeMultiplier, 
	                               double thresholdMultiplier, 
	                               long readyAfter) {
	        this.magnitudeMultiplier = magnitudeMultiplier;
	        this.thresholdMultiplier = thresholdMultiplier;
	        this.readyAfter = readyAfter;
	    }

	    public CUSUMChangeDetector() {
	        this(DEFAULT_MAGNITUDE, DEFAULT_THRESHOLD, AnomaliaProperties.readyAfter);
	    }

	    public void update(Double xi) {
	        ++observationCount;

	        // Instead of providing the target mean as a parameter as 
	        // we would in an offline test, we calculate it as we go to 
	        // create a target of normality.
	        double newMean = runningMean + (xi - runningMean) / observationCount;
	        runningVariance += (xi - runningMean)*(xi - newMean);
	        runningMean = newMean;
	        double std = Math.sqrt(runningVariance);

	        magnitude = magnitudeMultiplier * std;
	        threshold = thresholdMultiplier * std;
	        cusum = Math.max(0, cusumPrev + (xi - runningMean - magnitude));
	        cusumMin = Math.min(0, cusumMinPrev + (xi - runningMean - magnitude));

	        if(isReady()) {
	            this.change = cusum > threshold;
	    			this.changeNeg = cusumMin < -threshold;
	        }

	        cusumPrev = cusum;
	        cusumMinPrev = cusumMin;
	    }

	    public boolean isChange() {
	        return change;
	    }
	    
	    public boolean isChangePos()
	    {
	    		return change;
	    	}
	    
	    public boolean isChangeNeg()
	    {
	    		return changeNeg;
	    	}

	    public boolean isReady() {
	        return this.observationCount >= readyAfter;
	    }

	    public void reset() {
	        this.cusum = 0;
	        this.cusumMin = 0;
	        this.cusumPrev = 0;
	        this.cusumMinPrev = 0;
	        this.runningMean = 0;
	        this.observationCount = 0;
	    }
	    
	    public long points()
	    {
	    		return this.readyAfter;
	    }
	    
	    public long count()
	    {
	    		return this.observationCount;
	    }

	}