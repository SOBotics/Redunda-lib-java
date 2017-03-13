package org.sobotics;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PingService {
	/**
	 * The API-key for the current instance. You can get the key in the Instances-overview on Redunda
	 * */
	public String apiKey = "";
	
	/**
	 * If `true`, `standby` will always return `false` for debugging purposes!
	 * */
	private boolean debugging = false;
	
	/**
	 * The executor service to ping the server
	 * */
	private ScheduledExecutorService executorService;
	
	/**
	 * The time between two pings in seconds
	 * */
	private int interval = 30;
	
	/**
	 * Stores if the bot should be on standby; true by default
	 * */
	public static AtomicBoolean standby = new AtomicBoolean(true);
	
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
	
	public void setDebugging(boolean debug) {
		this.debugging = debug;
		if (this.debugging == true) {
			PingService.standby = new AtomicBoolean(false);
		}
	}
	
	public boolean getDebugging() {
		return this.debugging;
	}
	
	/**
	 * Resets the executor services and starts pinging the server
	 * */
	public final void start() {
		this.executorService = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		
		executorService.scheduleAtFixedRate(()->secureExecute(), 0, this.interval, TimeUnit.SECONDS);
	}
	
	/**
	 * Executes execute() and catches all errors
	 * */
	private void secureExecute() {
		try {
			this.execute();
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
		//don't execute on debug-mode
		if(this.debugging == true)
			return;
		
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
		
		//int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + url);
		//System.out.println("Post parameters : " + parameters);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		String responseString = response.toString();
		
		//http://stackoverflow.com/a/15116323/4687348
		JsonParser jsonParser = new JsonParser();
		JsonObject object = (JsonObject)jsonParser.parse(responseString);
		
		try {
			boolean standbyResponse = object.get("should_standby").getAsBoolean();
			PingService.standby.set(standbyResponse);
		} catch (Throwable e) {
			//no apikey or server might be offline; don't change status!
			e.printStackTrace();
		}
	}
}
