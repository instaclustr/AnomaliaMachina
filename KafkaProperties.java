public class KafkaProperties {
	public static final Boolean DEBUG = false;
	public static final Boolean DISPLAY = true;
	// for testing only
    public static final String KAFKA_SERVER_URL = "localhost";
    public static final int KAFKA_SERVER_PORT = 9092;
    public static final String KAFKA_LOCAL = "localhost:9092";
   
    // IPs now obtained using provisioning API
    public static final String KAFKA_BOOTSTRAP_SERVERS = "";

    public static final String KAFKA_SECURITY_PROTOCOL = "SASL_PLAINTEXT";
    public static final String KAFKA_SASL_MECHANISM = "SCRAM-SHA-256";
    
    // Instaclustr Kafka cluster username and password need to be set here
    // see https://www.instaclustr.com/support/documentation/kafka/using-your-kafka-cluster/connecting-to-a-kafka-cluster/
    public static final String KAFKA_SASL_JAAS_CONFIG_Anomalia = "org.apache.kafka.common.security.scram.ScramLoginModule required " + 
			"username=\"user\"" + 
			"password=\"key\";";
    
    public static final String KAFKA_SASL_JAAS_CONFIG = KAFKA_SASL_JAAS_CONFIG_Anomalia;
    
    public static final int KAFKA_PRODUCER_BUFFER_SIZE = 64 * 1024;
    public static final int CONNECTION_TIMEOUT = 100000;  
    public static final int DELAY = 100;
    public static final String TOPIC = "";
    public static final String CLIENT_ID = "";

    private KafkaProperties() {}
}