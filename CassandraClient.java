import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;

/*

CassandraClient handles creating connections to Cassandra and writing/reading data before running the anomaly detector.
 

CQL for Cassandra key space and table creation. Compression improves performance for small reads.
 
 	CREATE KEYSPACE anomalia WITH replication = {'class': 'NetworkTopologyStrategy', 'AWS_VPC_US_EAST_1': '3'}  AND durable_writes = true;

	CREATE TABLE anomalia.test1 (
            ...     id bigint,
            ...     time timestamp,
            ...     value double,
            ...     PRIMARY KEY (id, time)
            ... ) WITH CLUSTERING ORDER BY (time DESC) AND compression = {'sstable_compression': 'LZ4Compressor',
            ...                     'chunk_length_kb': 1};
    
 */

public class CassandraClient {
	
	static Cluster cluster = null;
	static Session session = null;
	static PreparedStatement preparedStatement = null;
	static PreparedStatement preparedStatementInsert = null;
	
	static Session.State state;
	static LoadBalancingPolicy loadBalancingPolicy;
	static PoolingOptions poolingOptions;
	
	static boolean debug = false;
	
	// connect to Cluster and return Session
	public static Session CassandraClient() {	
		
		String keyspace = "anomalia";
		
		// Get Cassandra IP addresses to connect to cluster using the Instaclustr provisioning API.
        String ip1 = "";
        String ip2 = "";
        String ip3 = "";
        
        GlobalProperties.load();
        
        if (GlobalProperties.properties.containsKey("Cassandra_IP_1"))
        {
        		ip1 = (String) GlobalProperties.properties.get("Cassandra_IP_1");
        		System.out.println("Cassandra_IP_1 = " + ip1);
        }
        
        if (GlobalProperties.properties.containsKey("Cassandra_IP_2"))
        {
        		ip2 = (String) GlobalProperties.properties.get("Cassandra_IP_2");
        		System.out.println("Cassandra_IP_2 = " + ip2);
        }
        
        if (GlobalProperties.properties.containsKey("Cassandra_IP_3"))
        {
        		ip3 = (String) GlobalProperties.properties.get("Cassandra_IP_3");
        		System.out.println("Cassandra_IP_3 = " + ip3);
        } 
        
        QueryOptions qo = null;
       
        // Set ConsistencyLevel: ONE is the default and the value used for the published results.
        if (AnomaliaProperties.consistencyLevelQUORUM)
        		qo = new QueryOptions().setConsistencyLevel(ConsistencyLevel.QUORUM);
        else qo = new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE);
        
        // Connect to Cassandra
        try
        {
        	
        		if (cluster == null)
        			cluster = Cluster.builder()   
        				.addContactPoint(ip1)
        				.addContactPoint(ip2)
        				.addContactPoint(ip3)
        				.withQueryOptions(qo)
        				.withLoadBalancingPolicy(
        		                LatencyAwarePolicy.builder
        		                (
        		                		new TokenAwarePolicy
        		                		(
        		                				DCAwareRoundRobinPolicy.builder().build()
        		                		)
        		                	).build()
        		        )
        		        .build();
        				
        		poolingOptions = cluster.getConfiguration().getPoolingOptions();
        		// start with 1 connection, will increase automatically
        		poolingOptions
        	    		.setCoreConnectionsPerHost(HostDistance.LOCAL,  1)
        	    		.setMaxConnectionsPerHost( HostDistance.LOCAL, 100)
        	    		.setCoreConnectionsPerHost(HostDistance.REMOTE, 1)
        	    		.setMaxConnectionsPerHost(HostDistance.REMOTE, 50);
        		
        		session = cluster.connect(keyspace);        		
        }
        catch (Exception e)
        {
        		System.out.println(e);
        }
        
		return session;
	}
	
	// populateDetector(): given new event <id, val>, get or create Cassandra session, create new detector, try and read sufficient rows with key = id, write the latest event
	// and if sufficient events read, fill the detector with the read and latest event.
    // note that for testing, it's optional to check, run the detector, and write events.
	static ChangeDetector populateDetector(long id, double val, boolean check)
    {
		ChangeDetector detector = null;
		
		// if 1st time then create a Cassandra session
		if (session == null)
			session = CassandraClient();
		
		Date now = new Date();

		// if check true then run the detector (the probability of checking events can be set to be < 1.0 so check can be false)
    		if (check)
    		{
    			// create new detector
    			detector = new CUSUMChangeDetector();
    			int limit = (int) detector.points();
    		
    			if (preparedStatement == null)
    				preparedStatement = session.prepare("SELECT value FROM " + AnomaliaProperties.tableName + " WHERE id = ? limit ?");
    		 
    			// read up to limit rows for id from Cassandra
    			Statement statement = preparedStatement.bind(id, limit);
    			ResultSet rows = session.execute(statement);
  
    			List<Row> descending = rows.all();     
    			int got = descending.size();
    			if (debug) System.out.println("requested " + limit + " rows, got " + got);
        
    			// If less rows than requested give up (detector will have 0 points), but still write the event and count the rows reads.
    			// Else populate detector with events
    			if (got >= (limit-1))
    			{
    				ListIterator<Row> reverseIterator = descending.listIterator(descending.size());
    				// Iterate in reverse.
	        		while(reverseIterator.hasPrevious())
	        		{
	        			Row row = reverseIterator.previous();
	        			Double v =  row.getDouble(0);
	        			if (AnomaliaProperties.runDetector) detector.update(v);
	        		}
        
	        		// add latest event to detector
	        		if (AnomaliaProperties.runDetector) detector.update(val);
    			}
    		}
        
        // write latest event to Cassandra
    		if (!AnomaliaProperties.readOnly)
    		{
    			 if (preparedStatementInsert == null)
    				 preparedStatementInsert = session.prepare("INSERT into " + AnomaliaProperties.tableName + " (id, time, value) values (?,?,?)");
    			Statement s2 = preparedStatementInsert.bind(id, now, val);
    				
    			// now write async don't bother waiting for result
    			session.executeAsync(s2);
    		}
    		
    		return detector;
    }
}
