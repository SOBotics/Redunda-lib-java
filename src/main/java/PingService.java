import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

public class PingService {
	/**
	 * The API-key for the current instance. You can get the key in the Instances-overview on Redunda
	 * */
	public String apiKey = "";
	
	private ScheduledExecutorService executorService;
	
	/**
	 * The time between two pings in seconds
	 * */
	private int interval = 30;
	
	/**
	 * Stores if the bot should be on standby; true by default
	 * */
	private boolean standby = true;
	
	/**
	 * Initialize with default values
	 * */
	public PingService() {}
	
	/**
	 * Initializes the object with the api key
	 * */
	public PingService(String key) {
		this.apiKey = key;
	}
	
	/**
	 * Initializes the object with a `pingInterval` in seconds and the api key
	 * */
	public PingService(String key, int pingInterval) {
		this.apiKey = key;
		this.interval = pingInterval;
	}
	
	/**
	 * Resets the executor services and starts pinging the server
	 * */
	public void start() {
		this.executorService = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		
		executorService.scheduleAtFixedRate(()->secureExecute(), 0, this.interval, TimeUnit.SECONDS);
	}
	
	/**
	 * Executes execute() and catches all errors
	 * */
	private void secureExecute() {
		try {
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Pings the server
	 * 
	 * @source https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
	 * */
	private void execute() throws Throwable {
		String url = "https://redunda.erwaysoftware.com/status.json";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Redunda Library");
		
		String parameters = "key="+this.apiKey;

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(parameters);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + parameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		String responseString = response.toString();
		
		//http://stackoverflow.com/a/25948078/4687348
		JsonReader jsonReader = Json.createReader(new StringReader(responseString));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		
		try {
			boolean standbyResponse = object.getBoolean("should_standby");
			this.standby = standbyResponse;
		} catch (Throwable e) {
			//server might be offline; don't change status!
			e.printStackTrace();
		}
	}
}
