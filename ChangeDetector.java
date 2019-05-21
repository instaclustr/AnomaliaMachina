
public interface ChangeDetector<T> {

	    /**
	     * Provide the next item in the stream
	     * @param observation the next item in the stream
	     */
	    void update(T observation);

	    /**
	     * Did the detector signal change at the last item?
	     * @return true, if it did.
	     */
	    boolean isChange();
	    
	    boolean isChangePos();
	    
	    boolean isChangeNeg();

	    /**
	     * Has the detector seen enough items to detect change?
	     * @return true, if it has.
	     */
	    boolean isReady();
	    
	    long count();
	    
	    long points();

	    /**
	     * Reset the detector, wiping any memory component it retains.
	     */
	    void reset();

	}
	