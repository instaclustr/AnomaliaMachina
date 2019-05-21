import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingProducerInterceptor;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

// multi threaded Kafka producer, produces stream of identical values for each Id, with occasional anomaly.
public class AnomaliaProducerThreaded implements Runnable {
	
	
	KafkaProducer<Long, Long> producer;

	boolean debug = false;
	
	// OpenTracing
	static Tracer tracer = initTracer("AnomaliaMachina");
	
	// Prometheus metric
	static final Counter pipelineCounter = Counter.build()
		     .name("AnomaliaMachina" + "_requests_total").help("Count of executions of pipeline stages")
		     .labelNames("stage")
		     .register();
	
	public AnomaliaProducerThreaded() {	
		 Properties props; 
		 props = new Properties();    
		 
		 String IPs = KafkaProperties.KAFKA_BOOTSTRAP_SERVERS;
		 System.out.println("Properties: IPs = " + IPs);
	     GlobalProperties.load();
	        
	     // try and find 3 Ips from Provisioning API
	     String IPs2 = "";
	     for (int i=1; i <= 3; i++)
	     {
	        if (GlobalProperties.properties.containsKey("Kafka_IP_" + i))
	        {
	            	String s = (String) GlobalProperties.properties.get("Kafka_IP_" + i);
	            	IPs2 += s + ":9092" + (i < 3 ? ", " : "");
	            	System.out.println("API Kafka_IP_" + i + "=" + s);
	        }
	     }
	        
	     if (IPs2.length() > 0)
	     {
	    	 	IPs = IPs2;
	    	 	System.out.println("Kafka IPs from Provisioning API: " + IPs);
	     }
		 
	     // set properties
		 props.put("bootstrap.servers", IPs);
		 props.put("security.protocol", KafkaProperties.KAFKA_SECURITY_PROTOCOL);
		 props.put("sasl.mechanism", KafkaProperties.KAFKA_SASL_MECHANISM);
		 props.put("sasl.jaas.config", KafkaProperties.KAFKA_SASL_JAAS_CONFIG);
		 props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
		 props.put("value.serializer", "org.apache.kafka.common.serialization.LongSerializer"); 
		 
		 GlobalTracer.register(tracer);
		 
		 if (AnomaliaProperties.openTracing)
			 props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());

		 producer = new KafkaProducer<Long, Long>(props);
	}
	
	public void send(String topic, Long k, Long v)
	{
		ProducerRecord<Long, Long> r = new ProducerRecord<Long, Long>(topic, k, v);
		
		if (debug) System.out.println("ProducerRecord = " + r.toString());
		
		boolean async = AnomaliaProperties.asyncProducer; 
		
		try {	
			if (async) producer.send(r);
			else producer.send(r).get();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	
	// async send with metrics collection
	public void futureSend(String topic, Long k, Long v)
	{	
		ProducerRecord<Long, Long> r = new ProducerRecord<Long, Long>(topic, k, v);
		
		if (debug) System.out.println("ProducerRecord = " + r.toString());
		
		boolean async = AnomaliaProperties.asyncProducer; 
		
		if (async)
			producer.send(r,
					new Callback() {
		                   public void onCompletion(RecordMetadata metadata, Exception e) {
		                       if(e != null) {
		                          e.printStackTrace();
		                       }
		                       else {
		                    	   		pipelineCounter.labels("producer").inc();
		                       }
		                   }
		               }
					);
		else
			try {
					producer.send(r).get();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					e1.printStackTrace();
			}
	}
	
	public void run()
	{
		long randId; 
		boolean async = AnomaliaProperties.asyncProducer; 
		
		boolean go = true;
		
		while (go)
		{	
			long now = System.currentTimeMillis();			
			if (now > AnomaliaMainProducer.loadStopTime)
			{
				System.out.println("loadStopTime reached, going home bye");
				break;
			}
			
			randId = ThreadLocalRandom.current().nextLong(0, AnomaliaProperties.idMax);
			long v = randId;
			if (ThreadLocalRandom.current().nextDouble() <= AnomaliaProperties.anomalyProbability)
				v *= 1000; 	// anomalous event
			if (async)
			{
				futureSend(AnomaliaProperties.topicName, randId, v);	
			}
			else 
			{
				send(AnomaliaProperties.topicName, randId, v);
	    			pipelineCounter.labels("producer").inc();
			}
			
			try {
			Thread.sleep(AnomaliaProperties.producerDelay);
			} catch (InterruptedException e) {
			e.printStackTrace();
			}
		}
				
	}
	
	// magic stuff to create a Jaeger tracer
	public static JaegerTracer initTracer(String service) {
		SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv().withType("const").withParam(1);
		ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true);
		Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);
		return config.getTracer();
	}
	
}