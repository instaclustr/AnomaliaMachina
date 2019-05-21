import java.util.Properties;

public class GlobalProperties {
	
	static Properties properties = new Properties();

	public static void load() {
		
		if (properties.isEmpty())
		{
			ProvisionAPI.getClusterIPs();
			System.out.println("Got IPs from Instaclustr Provisioning API! " + properties.toString());
		}
	}

}
