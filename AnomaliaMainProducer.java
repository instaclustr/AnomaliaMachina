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

// Top level class for Kafka producer (load generator).

public class AnomaliaMainProducer {

	public AnomaliaMainProducer() {
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
		
		AnomaliaProperties.getAndSetProps();
	
		String pThreads = null;
	    pThreads = System.getProperty("producerThreads");
	     
	    String rampTime = null;
	    rampTime = System.getProperty("rampTime");
	     
	    String loadTime = null;
	    loadTime = System.getProperty("loadTime");
	     
	    String maxID = null;
	    maxID = System.getProperty("maxID");
		
		int port = 0;
		port = AnomaliaProperties.producerPort;
		
		if (pThreads != null)
		{
			AnomaliaProperties.producerThreads = Integer.parseInt(pThreads);
			System.out.println("-DproducerThreads=" + AnomaliaProperties.producerThreads);
		}
		
		if (rampTime != null)
		{
			AnomaliaProperties.rampUpTimeS = Integer.parseInt(rampTime);
			System.out.println("-DrampTime=" + AnomaliaProperties.rampUpTimeS);
		}
		
		if (loadTime != null)
		{
			AnomaliaProperties.loadTimeS = Integer.parseInt(loadTime);
			System.out.println("-DloadTime=" + AnomaliaProperties.loadTimeS);
		}
		
		if (maxID != null)
		{
			AnomaliaProperties.idMax = Integer.parseInt(maxID);
			System.out.println("-DmaxID=" + AnomaliaProperties.idMax);
		}
		
		try {
			server = new HTTPServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// default is multithreaded
		if (multiThreadProducer)
		{   
		    int threads = AnomaliaProperties.producerThreads;
			
			System.out.println("Starting producer threads = " + threads);
			long rampUpTime = AnomaliaProperties.rampUpTimeS;
			// compute sleep time as time/threads
			long sleepTime = (rampUpTime * 1000)/threads;
			
			loadStopTime = Long.MAX_VALUE;

			for (int t=0; t < threads; t++)
			{
				Thread aThread = new Thread (new AnomaliaProducerThreaded());
				aThread.start();
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
			// remember now so we can compute load time
			loadStartTime = System.currentTimeMillis();
			// compute stop time
			loadStopTime = loadStartTime + (AnomaliaProperties.loadTimeS * 1000); 
			System.out.println("All threads started, running load for seconds = " + AnomaliaProperties.loadTimeS);
		}
		 
		// run forever, threads will stop when time up
		while (true)
		{
			try { 
						System.out.print(".");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
			}
		}		
	}
}
