import org.apache.http.auth.UsernamePasswordCredentials;

// Default properties, change to tune/test.

public class AnomaliaProperties {
	
	 // default values
	static long idMax = 1000000;				// generate ids from 0 to idMax - 1
	static long eventsMax = 1000000;			// generate this many new values, not used for multi-threaded producer
	static double probOfCheck = 1.0;			// probability of each new event having a check done. if 0 then write only (no reads) default is 1.0
	static long readyAfter=50;					// min events needed for detector to make a decision
	static int progress = Integer.MAX_VALUE;	// report progress every this many events
	static boolean runDetector = true; 			// run detector or not (for testing)
	static int numThreads = 2;					// number of Kafka consumer threads, default 2
	static boolean asyncCheck = false;
	static String topicName = "anomaliaTest";	// Kafka topic name
	static long producerDelay = 0;				// delay between producer events for testing
	static long reportEvery = Integer.MAX_VALUE;				// report metrics every period was 60
	static long measureEvery = Integer.MAX_VALUE;			// compute metrics every period was 10
	static int producerThreads = 2;				// default producer threads 2
	static int detectorThreads = 40;				// detector threads (everything after the consumer thread). default 40
	static boolean readOnly = false;				// if true then no writes, for testing
	static boolean consistencyLevelQUORUM = false;	// ONE or QUORUM, default ONE
	static boolean asyncProducer = true;			// false is slow, true is fast
	static long rampUpTimeS = 10;				// ramp up time to full load for producers
	static long loadTimeS = 10;					// how long to run full load for (if -1 then use eventsMax instead)
	static int producerPort = 1235;				// version for K8 uses same port as consumer as in different pods so don't care
	static int consumerPort = 1235;				// different consumer Port number, for testing
	static boolean kafkaOnly = false;			// if true no Cassandra functionality used, for testing only, fakes detector results.
	static String tableName = "test1";   		// Cassandra table name
	static boolean cassandraPublicIPS = true; 	// if false use publicIPs, else privateIPs which needs VPC peering set up.
	static boolean kafkaPublicIPS = true; 		// if false use publicIPs, else privateIPs which needs VPC peering set up.
	static boolean openTracing = true; 			// if true inject/extract opentracing spans, else don't. 
	static String instaclustrUser = "user"; // Instaclustr Provisioning API Username
	static String instaclustrPassword = "key"; // Instaclustr Provisioning API Key
	static double anomalyProbability = 0.0001;	// probability of anomaly
	
	public static void getAndSetProps()
	{
			// use defaults and print out values
			System.out.println("Using default property values:");
			System.out.println("idMax=" + idMax);
			System.out.println("probOfCheck=" + probOfCheck);
			System.out.println("readyAfter=" + readyAfter);
			System.out.println("topicName=" + topicName);
			System.out.println("numConsumerThreads=" + numThreads);
			System.out.println("detectorThreads=" + detectorThreads);
			System.out.println("producerThreads=" + producerThreads);
			System.out.println("consistencyLevelQUORUM=" + consistencyLevelQUORUM);
			System.out.println("asyncProducer=" + asyncProducer);
			System.out.println("rampUpTimeS=" + rampUpTimeS);
			System.out.println("loadTimeS=" + loadTimeS);
			System.out.println("producerPort=" + producerPort);
			System.out.println("consumerPort=" + consumerPort);
			System.out.println("cassandraPublicIPS=" + cassandraPublicIPS);
			System.out.println("kafkaPublicIPS=" + kafkaPublicIPS);
			System.out.println("tableName=" + tableName);
			System.out.println("openTracing=" + openTracing);
	}
}
