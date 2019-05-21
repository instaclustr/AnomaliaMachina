import java.io.IOException;
import java.lang.management.ManagementFactory;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// Top level class to run the Consumer, starts multiple Kafka consumers

public class AnomaliaMainConsumer {

	public AnomaliaMainConsumer() {
	}
	
	// Prometheus metrics server
	static HTTPServer server = null;
	
	static long loadStartTime = 0;
	static long loadStopTime = 0;

	public static void main(String[] args)
	{
		boolean multiThreadProducer = true;
		
		// Prometheus set up
		// Allow default JVM metrics to be exported 
	    DefaultExports.initialize();

	    // Metrics are pulled by Prometheus, create an HTTP server as the endpoint
	    // Note if there are multiple processes running on the same server need to change port number
	    // e.g. consumer and producer need different port numbers if on same server.
	    // get all of the run time properties, currently hard coded (could be from properties file, jar, args, etc).
		AnomaliaProperties.getAndSetProps();
		
		// pass key=value pairs like this as args: -Dport=1236
		String aPort = System.getProperty("port");
        
        String cThreads = null;
        String dThreads = null;
        cThreads = System.getProperty("consumerThreads");
        dThreads = System.getProperty("detectorThreads");
		
		int port = AnomaliaProperties.consumerPort;
		if (aPort != null)
			port = Integer.parseInt(aPort);
		System.out.println("Prometheus PORT=" + port);
		
		if (cThreads != null)
		{
			AnomaliaProperties.numThreads = Integer.parseInt(cThreads);
			System.out.println("-DconsumerThreads=" + AnomaliaProperties.numThreads);
		}
		
		if (dThreads != null)
		{
			AnomaliaProperties.detectorThreads = Integer.parseInt(dThreads);
			System.out.println("-DdetectorThreads=" + AnomaliaProperties.detectorThreads);
		}
		
				
		try {
			server = new HTTPServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// start background summary() thread (10s by default). No longer used in this version.
		// new MSet().start();
		
		System.out.println("Starting consumers...");
		startConsumers(AnomaliaProperties.numThreads);
		try {
				Thread.sleep(1000);
		} catch (InterruptedException e) {
				e.printStackTrace();
		}
		
		while (true)
		{
				try { 
						System.out.print(".");
						Thread.sleep(AnomaliaProperties.reportEvery * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
			}
		}
	}
	
	// start multiple consumer threads
	static public void startConsumers(int n)
	{
    	 	String topicName = AnomaliaProperties.topicName;
    	 	
    	 	System.out.println("Starting " + n + " Consumers on topic " + topicName);
    	 	
    	 	for (int i=0; i < n; i++)
    	 	{
    	 		System.out.println("Starting  Consumer " + i);
    	 		
    	 		AnomaliaConsumer consumer = new AnomaliaConsumer(topicName);
    	 		consumer.start();
    	 		// wait until consumer is subscribed to prevent rebalancing storm delays
    	 		try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    	 		
    	 		while (!consumer.subscribed)
    	 		{
    	 			try {
						Thread.sleep(10);
					} catch (InterruptedException e) 
    	 			{
						e.printStackTrace();
						System.exit(0);
    	 			}
    	 		}
    	 		System.out.println("AnomaliaConsumer " + i + " SUBSCRIBED");
    	 	}
     
	}

}
