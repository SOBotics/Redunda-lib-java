package org.sobotics.redunda;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Synchronizes files across bot-instances via Redunda
 * */
public class DataService {
	/**
	 * The API-key for the current instance. You can get the key in the Instances-overview on Redunda
	 * */
	public String apiKey = "";
	
	/**
	 * Stores if files should be uploaded automatically
	 * */
	private boolean autoUpload = false;
	
	/**
	 * If `true`, `standby` will always return `false` for debugging purposes!
	 * */
	private boolean debugging = false;
	
	/**
	 * The executor service to ping the server
	 * */
	private ScheduledExecutorService executorService;
	
	/**
	 * The time between two checks in seconds
	 * */
	private int interval = 180;
	
	/**
	 * The list of files to track and synchronize.
	 * */
	private List<String> trackedFiles = new ArrayList<String>();
	
	/**
	 * Initialize with default values
	 * */
	public DataService() {}
	
	/**
	 * Initializes the object with the api key
	 * 
	 * @param key The API key for Redunda. You can get it form the instances overview
	 * */
	public DataService(String key) {
		this.apiKey = key;
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
	}
	
	/**
	 * Returns if `DataService` is in debugging mode.
	 * 
	 * @return true, if debugging is enabled
	 * */
	public boolean getDebugging() {
		return this.debugging;
	}
	
	/**
	 * Adds a file to the list of tracked files
	 * 
	 * @param path The path to the file to track
	 * */
	public void trackFile(String path) {
		if (this.trackedFiles.contains(path)) {
			System.out.println("Already tracking "+path);
		} else {
			this.trackedFiles.add(path);
		}
	}
	
	/**
	 * Returns the list of tracked files
	 * 
	 * @warning A tracked file may NOT contain the string `_slash_`!
	 * */
	public List<String> getTrackedFiles() {
		return this.trackedFiles;
	}
	
	private String encodeFilename(String filename) {
		return filename.replace("/", "_slash_");
	}
	
	private String decodeFilename(String filename) {
		return filename.replace("_slash_", "/");
	}
	
	/**
	 * Uploads all tracked files to Redunda.
	 * 
	 * @note This will ALWAYS overwrite the files on the server
	 * */
	public void pushFiles() throws IOException {
		for (String file : this.trackedFiles) {
			this.pushFile(file);
		}
	}
	
	/**
	 * Uploads a file to Redunda
	 * 
	 * @note This will ALWAYS overwrite the files on the server
	 * 
	 * @param filename The name of the file to upload
	 * @throws IOException if the file couldn't be read
	 * */
	public void pushFile(String filename) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(filename)));
		String encodedFilename;
		try {
			encodedFilename = URLEncoder.encode(this.encodeFilename(filename), "UTF-8");
		} catch (Throwable e) {
			e.printStackTrace();
			return;
		}
		
		
		String url = "https://redunda.sobotics.org/bots/data/"+encodedFilename+"?key="+this.apiKey;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Redunda Library");
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(content);
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
	}
	
	public List<String> getRemoteFileList() throws Throwable {
		String url = "https://redunda.sobotics.org/bots/data.json?key="+this.apiKey;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Redunda Library");
		
		int responseCode = con.getResponseCode();
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		String responseString = response.toString();
		//System.out.println(responseString);
		
		//http://stackoverflow.com/a/15116323/4687348
		JsonParser jsonParser = new JsonParser();
		JsonArray array = (JsonArray)jsonParser.parse(responseString);
		
		List<String> filenames = new ArrayList<String>();
		
		for (JsonElement element : array) {
			JsonObject elementObject = element.getAsJsonObject();
			String key = elementObject.get("key").getAsString();
			if (key != null) {
				String decodedKey = this.decodeFilename(key);
				filenames.add(decodedKey);
			}
		}
		
		return filenames;
	}
	
	/**
	 * Downloads the contents of a file from Redunda
	 * 
	 * @param filename The name of the file to download
	 * 
	 * @return The content of the file or `null` if an error occurs. (for example status not 200)
	 * */
	public String getContentOfRemoteFile(String filename) throws Throwable {
		String encodedFilename;
		try {
			encodedFilename = URLEncoder.encode(this.encodeFilename(filename), "UTF-8");
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
		
		String url = "https://redunda.sobotics.org/bots/data/"+encodedFilename+"?key="+this.apiKey;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Redunda Library");
		
		int responseCode = con.getResponseCode();
		if (responseCode != 200)
			return null;
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		String responseString = response.toString();
		return responseString;
	}
	
	/**
	 * Writes an input string to a local file
	 * 
	 * @param input The string to write
	 * @param filename The name of the file
	 * @throws Throwable If an error occurs while writing
	 * */
	private void writeStringToFile(String input, String filename) throws Throwable {
		Files.write(Paths.get(filename), input.getBytes());
	}
	
	/**
	 * Resets the executor services and starts checking the files
	 * */
	public final void start() {
		this.executorService = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		
		//executorService.scheduleAtFixedRate(()->secureExecute(), 0, this.interval, TimeUnit.SECONDS);
	}
}
