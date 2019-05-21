// top level checking class: takes event, gets historical data, writes latest event to C*, and checks for anomaly

public class CheckEvent {
	
	static boolean debug = false;

	public CheckEvent() {
	}
	
	// checkEvent() takes a new event <id, v>, if check true then run the check, else just write the event.
	// returns true if anomaly found else false (if no anomaly or insufficient data or check false)
	static boolean checkEvent(long id, double v, boolean check)
	{
		if (check && debug) System.out.println("check Event " + id + ", " + v);
		
		// get historic data, write latest <id, v> to C*, run the detector
		ChangeDetector detector = CassandraClient.populateDetector(id, v, check);
		
		if (check && detector.isReady() && detector.isChange())
			return true;
		else return false;
	}
	
	// checkEventWithResult() same as checkEvent() but returns Result object with true/false and count of number of rows obtained for id.
	static Result checkEventWithResult(long id, double v, boolean check)
	{
		Result r = new Result();
		if (check && debug) System.out.println("check Event with Result " + id + ", " + v);
		
		// get historic data, write latest <id, v> to C*, run the detector
		ChangeDetector detector = CassandraClient.populateDetector(id, v, check);
		
		if (check && detector.isReady() && detector.isChange())
			r.anomaly = true;
		else r.anomaly = false;
		r.count = detector.count();

		return r;
	}
}
