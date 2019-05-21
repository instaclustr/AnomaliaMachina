import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
	
public class ProvisionAPI {
	
	    public static void main(String[] args) {
	    		// test
	    		getClusterIPs();
	    		System.out.println(GlobalProperties.properties.toString());
	    }
	    
	    // called to get some of the Instaclustr Cassandra and Kafka cluster IP addresses
	    // looks for prod_cassandra and prod_kafka names
	    public static void getClusterIPs()
	    {
	    	
	    	String s = http("https://api.dev.instaclustr.com/provisioning/v1", "");
	    	System.out.println("API Result=" + s);
	    	
	    	// Now get list of clusters, find cassandra cluster, and do call to get IPs for cluster.
	    	
	    	String cassandraID = null;
	    	String cID = null;
	    	String kafkaID = null;
	    	
	    	try {
                JSONParser parser = new JSONParser();
                Object resultObject = parser.parse(s);

                if (resultObject instanceof JSONArray) {
                		System.out.println("Array");
                    JSONArray array=(JSONArray)resultObject;
                    for (Object object : array) {
                    		System.out.println("Obj");

                        JSONObject obj =(JSONObject)object;
                        System.out.println(obj.toString());
                        
                        String name = obj.get("name").toString();
                        System.out.println("NAME = " + name);
                        String cassandraVersion;
                        
                        try {
                        		cassandraVersion = obj.get("cassandraVersion").toString();
                        }
                        catch (Exception e) 
                        {
                        		cassandraVersion = "";
                        }
                        
                        System.out.println("version = " + cassandraVersion);

                        if (name.contains("prod_cassandra"))
                        	  cassandraID = obj.get("id").toString();
                        // get backup test cluster
                        else if (cassandraVersion.contains("cassandra"))
                        		cID = obj.get("id").toString();
                        else if (name.contains("prod_kafka"))
                      	  kafkaID = obj.get("id").toString();
                    }
                }
                else if (resultObject instanceof JSONObject) {
                	System.out.println("Object");

                    JSONObject obj =(JSONObject)resultObject;
                    System.out.println(obj.toString());
                }

            } catch (Exception e) {            	
            }
	    	
	    	// now get IPs for each Cluster Id
	    	// this version gets public and private IPs
	    	if (cassandraID == null)
	    		cassandraID = cID;
	    	System.out.println("Cass ID=" + cassandraID);
	    	System.out.println("kafka ID=" + kafkaID);

	    String t = http("https://api.dev.instaclustr.com/provisioning/v1/" + cassandraID, "");
	    	System.out.println("API Result=" + t);
	    	
	    ArrayList<String> ips;
	    
	    ips = getIPs(t, AnomaliaProperties.cassandraPublicIPS);

	    int i = 1;
	    for (String ip: ips)
	    {
	    		GlobalProperties.properties.setProperty("Cassandra_IP_" + i++, ip);
	    		System.out.println("Cassandra IP =" + ip);
	    }
	    	
	    String u = http("https://api.dev.instaclustr.com/provisioning/v1/" + kafkaID, "");
	    	System.out.println("Result=" + u);
	    	
		ips = getIPs(u, AnomaliaProperties.kafkaPublicIPS);

	    i = 1;
	    for (String ip: ips)
	    {
    			GlobalProperties.properties.setProperty("Kafka_IP_" + i++, ip);
    			System.out.println("Kafka IP =" + ip);
	    }   
	    	
	    }
	    
	    public static ArrayList<String> getIPs(String t, boolean publicIPS)
	    {
		    ArrayList<String> ips = new ArrayList<String>();
		    	
		 	try {
	            JSONParser parser = new JSONParser();
	            Object resultObject = parser.parse(t);
	
	            if (resultObject instanceof JSONArray) {
	            		System.out.println("Array");
	                JSONArray array=(JSONArray)resultObject;
	                for (Object object : array) {
	                		System.out.println("Obj");
	
	                    JSONObject obj =(JSONObject)object;
	                    System.out.println(obj.toString());
	                }
	            }
	            else if (resultObject instanceof JSONObject) {
	            		System.out.println("Object");           
	                JSONObject obj = (JSONObject)resultObject;
	                System.out.println(obj.toString());
	
	                
	                JSONArray array = (JSONArray) obj.get("dataCentres");
	                
	                for (Object object : array) {
	            			System.out.println("Obj");
	
	            			JSONObject x =(JSONObject)object;
	            			System.out.println(x.toString());
	                    JSONArray nodes = (JSONArray) x.get("nodes");
	                    
	                    for (Object node : nodes) {
	            			System.out.println("Node");
	
	            			JSONObject n =(JSONObject)node;
	            			System.out.println(n.toString());
	            			
	            			if (publicIPS)
	            			{
	            				String ip = n.get("publicAddress").toString();
	            				System.out.println("PUBLIC IP = " + ip);
	            				ips.add(ip);
	            			}	
	            			else
	            			{
	            				String ip = n.get("privateAddress").toString();
	                			System.out.println("PRIVATE IP = " + ip);
	                			ips.add(ip);
	            			}
	            			}	
	            }
	            }
	        } catch (Exception e) {
	        }
		 	
			return ips;
	    }

public static String http(String url, String body) {
	    	
	    	CredentialsProvider provider = new BasicCredentialsProvider();
	    	
	    	UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(AnomaliaProperties.instaclustrUser, AnomaliaProperties.instaclustrPassword);

	    	provider.setCredentials(AuthScope.ANY, credentials);
	
	    try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build()) {    	
	    		HttpResponse response = httpClient.execute(new HttpGet(url));
		    	int statusCode = response.getStatusLine().getStatusCode();
		    	String json = EntityUtils.toString(response.getEntity(), "UTF-8");
	            	return json;	            	
	         } catch (Exception e) {
	    }
		return null;
}

}
