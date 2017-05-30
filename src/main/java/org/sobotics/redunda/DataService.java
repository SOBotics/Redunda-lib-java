package org.sobotics.redunda;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	 * Sets the time between two syncs. (if your bot syncs automatically)
	 * 
	 * @param seconds The new value
	 * */
	public void setSyncInterval(int seconds) {
		this.interval = seconds > 0 ? seconds : 180;
	}
	
	/**
	 * Returns the time in seconds between two syncs.
	 * 
	 * @return The value in seconds
	 * */
	public int getSyncInterval() {
		return this.interval;
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
	 * A tracked file may NOT contain the string `_slash_`!
	 * 
	 * @return The list of tracked files
	 * */
	public List<String> getTrackedFiles() {
		return this.trackedFiles;
	}
	
	/**
	 * Encodes a filename
	 * 
	 * @param filename The filename to encode
	 * 
	 * @return The encoded filename
	 * */
	private String encodeFilename(String filename) {
		return filename.replace("/", "_slash_");
	}
	
	/**
	 * Decodes a filename
	 * 
	 * @param filename The filename to decode
	 * 
	 * @return The decoded filename
	 * */
	private String decodeFilename(String filename) {
		return filename.replace("_slash_", "/");
	}
	
	/**
	 * Uploads all tracked files to Redunda.
	 * 
	 * This will ALWAYS overwrite the files on the server
	 * 
	 * @throws IOException If the upload of a file fails
	 * */
	public void pushFiles() throws IOException {
		for (String file : this.trackedFiles) {
			this.pushFile(file);
		}
	}
	
	/**
	 * Uploads a file to Redunda
	 * 
	 * This will ALWAYS overwrite the files on the server
	 * 
	 * @param filename The name of the file to upload
	 * @throws IOException if the file couldn't be read
	 * */
	public void pushFile(String filename) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(filename)));
		System.out.println(content);
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
		con.setRequestProperty("User-Agent", UserAgent.getUserAgent());
		
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
	
	/**
	 * Returns the list of files on the server as `JsonArray`
	 * 
	 * @throws Throwable If the download/parsing fails
	 * 
	 * @return An array of objects with information about the stored files
	 * */
	public JsonArray getRemoteFileList() throws Throwable {
		String url = "https://redunda.sobotics.org/bots/data.json?key="+this.apiKey;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", UserAgent.getUserAgent());
		
		con.getResponseCode();
		
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
		return array;
		/*
		List<String> filenames = new ArrayList<String>();
		
		for (JsonElement element : array) {
			JsonObject elementObject = element.getAsJsonObject();
			String key = elementObject.get("key").getAsString();
			if (key != null) {
				String decodedKey = this.decodeFilename(key);
				filenames.add(decodedKey);
			}
		}
		
		return filenames;*/
	}
	
	/**
	 * Downloads the contents of a file from Redunda.
	 * 
	 * If the file is not tracked, it will be added to `this.trackedFiles`. You can change this behavior by using `this.getContentOfRemoteFile(String, boolean)`.
	 * 
	 * @param filename The name of the file to download
	 * 
	 * @throws Throwable If an error occurs while downloading the file
	 * 
	 * @return The content of the file or `null` if an error occurs. (for example status not 200)
	 * */
	public String getContentOfRemoteFile(String filename) throws Throwable {
		return this.getContentOfRemoteFile(filename, true);
	}
	
	/**
	 * Downloads the contents of a file from Redunda
	 * 
	 * @param filename The name of the file to download
	 * @param trackFile If `true`, the file will be added to the list of tracked files
	 * 
	 * @throws Throwable If an error occurs while downloading the file
	 * 
	 * @return The content of the file or `null` if an error occurs. (for example status not 200)
	 * */
	public String getContentOfRemoteFile(String filename, boolean trackFile) throws Throwable {
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
		con.setRequestProperty("User-Agent", UserAgent.getUserAgent());
		
		int responseCode = con.getResponseCode();
		if (responseCode != 200)
			return null;
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine+"\n"); //http://stackoverflow.com/a/4825297/4687348
		}
		in.close();
		
		String responseString = response.toString();
		
		//add to tracked files?
		if (trackFile == true && this.trackedFiles.contains(filename))
			this.trackedFiles.add(filename);
		
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
	 * Synchronizes the files for the bot via Redunda.
	 * 
	 * First, the list of files on Redunda will be downloaded. This list will be compared with `this.trackedFiles`.
	 * Files that exist on the server but not on the client, will be downloaded.
	 * Tracked files that are not on the server will be uploaded.
	 * 
	 * Downloaded files will overwrite the local files, if the are newer.
	 * Uploaded files will *always* overwrite the files on the server. (see `pushFile(String)`)
	 * 
	 * */
	public void syncFiles() {
		List<String> filesToDownload = new ArrayList<String>();
		List<String> filesToUpload = new ArrayList<String>();
		List<String> remoteFileNames = new ArrayList<String>();
		JsonArray remoteFilesInfo;
		
		//Create the lists of files to up- and download
		
		try {
			remoteFilesInfo = this.getRemoteFileList();
		} catch (Throwable e) {
			e.printStackTrace();
			return;
		}
		
		//loop through remote files
		for (JsonElement remoteFileInfo : remoteFilesInfo) {
			JsonObject remoteFileInfoObject = remoteFileInfo.getAsJsonObject();
			String remoteFileName = this.decodeFilename(remoteFileInfoObject.get("key").getAsString());
			
			//add to list for later usage (see next for-loop)
			remoteFileNames.add(remoteFileName);
			
			//check if file is tracked
			if (this.trackedFiles.contains(remoteFileName)) {
				//file is tracked
				//Compare last changed dates; if remote is newer, add to DL list. If not, add to UL list
				try {
					long remoteChangedTimestamp = remoteFileInfoObject.get("updated_at").getAsLong() * 1000; //redunds sends seconds. I need milliseconds
					//String remoteChangedDateString = remoteFileInfoObject.get("updated_at").getAsString();
					//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
					
					LocalDateTime remoteChangedDate = Instant.ofEpochMilli(remoteChangedTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
					//LocalDateTime remoteChangedDate = LocalDateTime.parse(remoteChangedDateString, formatter);
					
					File localFile = new File(remoteFileName);
					long localChangedTimestamp = localFile.lastModified();
					LocalDateTime localChangedDate = Instant.ofEpochMilli(localChangedTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
					
					if (remoteChangedDate.isAfter(localChangedDate)) {
						//remote file is newer
						filesToDownload.add(remoteFileName);
					}
					
				} catch (DateTimeParseException e) {
					e.printStackTrace();
				}
			} else {
				//file is not tracked -> download it
				filesToDownload.add(remoteFileName);
			}
		}
		
		//loop through tracked files
		for (String trackedFileName : this.trackedFiles) {
			if (remoteFileNames.contains(trackedFileName)) {
				//file exists on server -> Do nothing. Will be handled in the previous for-loop
			} else {
				//file doesn't exist on server -> upload it
				filesToUpload.add(trackedFileName);
			}
		}
		
		
		//Upload the files
		for (String fileToUpload : filesToUpload) {
			try {
				this.pushFile(fileToUpload);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//Download the files
		for (String fileToDownload : filesToDownload) {
			try {
				String fileContent = this.getContentOfRemoteFile(fileToDownload);
				this.writeStringToFile(fileContent, fileToDownload);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Synchronizes the files synchronously and then starts the background thread.
	 * 
	 * It's recommended to use this right after launching the bot.
	 * */
	public final void syncAndStart() {
		this.syncFiles();
		this.start();
	}
	
	/**
	 * Resets the executor services and starts checking the files after 30 seconds
	 * 
	 * If the bot has just started, it is better to call `syncAndStart()`.
	 * */
	public final void start() {
		this.executorService = null;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		
		executorService.scheduleAtFixedRate(()->syncFiles(), 30, this.interval, TimeUnit.SECONDS);
	}
}
