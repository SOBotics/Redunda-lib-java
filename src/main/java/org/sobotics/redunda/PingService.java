package org.sobotics.redunda;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Manages the connection to Redunda
 * */
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
	 * The object that will be notified about status-changes.
	 * */
	public PingServiceDelegate delegate = null;
	
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
	 * The version of the bot as string
	 * */
	private String version = null;
	
	/**
	 * Initialize with default values
	 * */
	public PingService() {}
	
	/**
	 * Initializes the object with the api key
	 * 
	 * @param key The API key for Redunda. You can get it form the instances overview
	 * */
	public PingService(String key) {
		this.apiKey = key;
	}
	
	/**
	 * Initializes the object with the api key and bot-version
	 * 
	 * @param key The API key for Redunda. You can get it form the instances overview
	 * @param botVersion The version of your bot as string
	 * */
	public PingService(String key, String botVersion) {
		this.apiKey = key;
		this.version = botVersion;
	}
	
	/**
	 * Initializes the object with a `pingInterval` in seconds, the bot's verision and the api key
	 * 
	 * @param key The API key for Redunda. You can get it form the instances overview
	 * @param botVersion The version of your bot as string
	 * @param pingInterval The time in seconds between two pings. (30 by default)
	 * */
	public PingService(String key, String botVersion, int pingInterval) {
		this.apiKey = key;
		this.version = botVersion;
		this.interval = pingInterval;
	}
	
	/**
	 * Returns a `DataService` with the same API-key as `PingService`
	 * */
	public DataService buildDataService() {
		return new DataService(this.apiKey);
	}
	
	/**
	 * Enables or disables the debugging mode.
	 * 
	 * If the debugging mode is activated, the bot will never be on standby.
	 * 
	 * @param debug The new value
	 * */
	public void setDebugging(boolean debug) {
		this.debugging = debug;
		if (this.debugging == true) {
			PingService.standby = new AtomicBoolean(false);
		}
	}
	
	/**
	 * Returns is PingService is in debugging mode.
	 * 
	 * @return true, if debugging is enabled
	 * */
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
	 * Fetches the current standby status synchronously from the server
	 * 
	 * If an exception occurs, the status will be `true`.
	 * 
	 * The value is affected by the debug-mode.
	 * 
	 * @return The standby-status
	 * */
	public boolean checkStandbyStatus() {
		try {
			this.execute();
			return PingService.standby.get();
		} catch (Throwable e) {
			e.printStackTrace();
			return true;
		}
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
		
		String url = "https://redunda.sobotics.org/status.json";
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Redunda Library");
		
		String parameters = "key="+this.apiKey;
		
		//add version parameter if available
		if (this.version != null)
			parameters = parameters+"&version="+this.version;

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(parameters);
		wr.flush();
		wr.close();

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
			boolean oldValue = PingService.standby.get();
			PingService.standby.set(standbyResponse);
			if (standbyResponse != oldValue) {
				if (this.delegate != null)this.delegate.standbyStatusChanged(standbyResponse);
			}
		} catch (Throwable e) {
			//no apikey or server might be offline; don't change status!
			e.printStackTrace();
		}
	}
}
