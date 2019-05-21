import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

// Kafka
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

// Prometheus
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

// Opentracing
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.HeadersMapExtractAdapter;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import io.opentracing.propagation.Format;

// Jaeger is a specific OpenTracing tracer, can easily change to something else
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;

// single Kafka consumer, continues forever. Runs a consumer in one thread pool, and Cassandra client and detector in another.
// assumes the topic has been created or auto topic creation turned on. Ensure sufficient (but not too many) partitions.
public class AnomaliaConsumer extends Thread {
	
	  boolean debug = false;
	  boolean firstTime = true;
	  // if true then assume kakfa records have a span inserted by producer and use that as context.
	  boolean opentracingKafka = AnomaliaProperties.openTracing; 

	  // Kafka message is just <Long, Long> pair of id and value
	  private final KafkaConsumer<Long, Long> consumer;
	  
	  // Kafka topic
	  private final String topic;
	  
	  boolean subscribed = false; // true after subscribe() returns
	 
	  // thread pool for detector threads (including Cassandra client)
	  static ExecutorService executor = Executors.newFixedThreadPool(AnomaliaProperties.detectorThreads);
	  
	  static Tracer tracer = initTracer("AnomaliaMachina");
	  
	  // Prometheus metrics - use one counter for all stages, use a label for different stages.
	  static final Counter pipelineCounter = Counter.build()
			     .name("AnomaliaMachina" + "_requests_total").help("Count of executions of pipeline stages")
			     .labelNames("stage")
			     .register();
	  
	  // use another counter for results
	  static final Counter anomalyCounter = Counter.build()
			     .name("AnomaliaMachina" + "_anomalies_total").help("Count of anomalies detected")
			     .register();
	  
	  // use a Gauge for records read as well
	  static final Gauge recordsReadGauge = Gauge.build()
			     .name("AnomaliaMachina" + "_records_total").help("Gauge of records read per read")
			     .labelNames("stage")
			     .register();
	  
	  // use a Gauge for latencies
	  static final Gauge pipelineGauge = Gauge.build()
			     .name("AnomaliaMachina" + "_duration_seconds").help("Gauge of stage durations in seconds")
			     .labelNames("stage")
			     .register();
	     
	  // Create Kafka consumer subscribed to given topic
	  public AnomaliaConsumer(String topic)
	  {
	    this.topic = topic;
	    Properties props = new Properties();  
	 	
	 	// use Instaclustr provisioning API to get Kafka cluster Ips
        GlobalProperties.load();
        
        // try and find 3 Kafka Ips from Provisioning API
        String IPs = "";
        for (int i=1; i <= 3; i++)
        {
        		if (GlobalProperties.properties.containsKey("Kafka_IP_" + i))
            {
            		String s = (String) GlobalProperties.properties.get("Kafka_IP_" + i);
            		IPs += s + ":9092" + (i < 3 ? ", " : "");
            }
        }
        
        if (IPs.length() > 0)
        		System.out.println("Kafka IPs from Provisioning API: " + IPs);
        
	 	
        // setup properties
	 	props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, IPs);
	 	props.put("security.protocol", KafkaProperties.KAFKA_SECURITY_PROTOCOL);
	 	props.put("sasl.mechanism", KafkaProperties.KAFKA_SASL_MECHANISM);
	 	props.put("sasl.jaas.config", KafkaProperties.KAFKA_SASL_JAAS_CONFIG);
	    props.put(ConsumerConfig.GROUP_ID_CONFIG, "AnomaliaConsumer");
	    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
	    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
	    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
	    
	    // use default serializers for <Long, Long> <key, value> pairs
	    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongDeserializer");
	    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongDeserializer");
	    
	    // create consumer
	    this.consumer = new KafkaConsumer<Long, Long>(props);
	    System.out.println("Created new AnomaliaConsumer, topic " + this.topic + " partitions=" + this.consumer.partitionsFor(this.topic).size());
	  }
	 
	  @Override
	  public void run()
	  {
		 long threadId = -1;
		  
		 List l = consumer.partitionsFor(this.topic);
		 int numParts = l.size();
		 System.out.println("Number of Partitions for " + this.topic + "=" + numParts);
				  
	    try
	    {
	    		threadId = Thread.currentThread().getId();
		    	consumer.subscribe(Collections.singletonList(this.topic));
		    	subscribed = true;
		    System.out.println(threadId + " ********* AnomaliaConsumer subscribed to topic " + this.topic);
		    boolean go = true;
		    ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
		    
		    long t0 = 0;
		    
		    	while (go)
		    {
		        ConsumerRecords<Long, Long> records = consumer.poll(100);

		        if (records.count() > 0)
		        	 	t0 = System.nanoTime();
		        
		        for (ConsumerRecord<Long, Long> record : records)
		        {
		        		Span span = null;
		        		// process each record, if opentracingKafka then extract span context
		        		if (opentracingKafka)
		        		{
		        			Headers headers = record.headers();
		        			SpanContext spanContext =  tracer.extract(Format.Builtin.TEXT_MAP, new MyHeadersMapExtractAdapter(headers, false));
		        			
		        			if (spanContext == null)
		        				// couldn't find a span context so build a new start span
		        				span = tracer.buildSpan("consumer").start();		
		        			else // else build a new span which references it
		        				span = tracer.buildSpan("consumer").addReference(References.FOLLOWS_FROM, spanContext).start();
		        			span.setTag("span.kind", "consumer");
		        		}
		        	
		    			pipelineCounter.labels("consumer").inc();
		    			long producerTime = record.timestamp();
	        	  		
		    			// for testing can change probability of each event being checked
	        			boolean checkIt = ThreadLocalRandom.current().nextDouble(0, 1.0) <= AnomaliaProperties.probOfCheck;
	        			// get ready to run the detector in a separate thread pool
	        			final long finalKey = record.key();
	        			final double finalValue = (double) record.value();
	        			final boolean finalCheck = checkIt;
	        			final long startTime = t0;
	        			final long pt = producerTime;
	        			
	        			// OpenTracing: new thread can't get access to scope, only span
	        			final Span parentSpan = span;
	        			span.finish();
	        				 
	        				 Runnable runMe = new Runnable()
	        				 {
	        					 public void run()
	        					 {
	        						 Span span = null;
	        						 // start a new linked span for detector
        							 span = tracer.buildSpan("detector").addReference(References.FOLLOWS_FROM, parentSpan.context()).start();
        							 Result r = new Result();  
	        						 r = CheckEvent.checkEventWithResult(finalKey, finalValue, finalCheck);
	        		   
	        						 if (finalCheck)
	        						 {
	        							 long t1 = System.nanoTime();
	        							 long latencyMs = (t1 - startTime)/1000000;
	        							 pipelineGauge.labels("detector").set(latencyMs);
	        							 long now = System.currentTimeMillis();
	        							 long endToEnd = now - pt;
	        							 double endToEndS = endToEnd/1000.0;
	        							 pipelineGauge.labels("endToEnd").set(endToEndS);
	        							 recordsReadGauge.labels("detector").set(r.count);
	        		        			 	 if (r.count >= 50)
	        		        			 		 pipelineCounter.labels("detector_Run").inc();
	        		        			 	 else
	        		        			 		 pipelineCounter.labels("detector_notRun").inc();
	        							 if (r.anomaly) 
	        								 anomalyCounter.inc();
	        						 }
	        						 
	        		        			 pipelineCounter.labels("detector").inc();
	        		        			 if (span != null) 
	        							 span.finish();
	        					 }
	        				 }; // Runnable    		
	        				 
	        				// Submit new event to thread pool
	        				long t = 1;
	        				while (true)
	        				{
	        					// if queue empty, then submit job, else wait
	        					if (tpe.getQueue().size() == 0)
	        					{
        						  {
        							  executor.submit(runMe);
        							  break;
        						  }
	        					}
	        					else Thread.sleep(t);
	        					t++;
	        				}
	        				
	        		     	// Need to wait a bit until Cassandra connection is created first time around.
	        		     	if (firstTime)
	        					Thread.sleep(2000);
	        		     	firstTime = false;
				}
		    }
	    }
	  
	    catch (Exception e) {
	    } finally {
	      consumer.close();
	      System.out.println("AnomaliaConsumer " + threadId + " closing");
	    }
	  }
	  
	  public void shutdown() {
	    consumer.wakeup();
	  }
	  
	  // magic code to create a Jaeger Tracer
	  public static JaegerTracer initTracer(String service) {
	        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv().withType("const").withParam(1);
	        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true);
	        Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
	        return config.getTracer();
	    }
	}
