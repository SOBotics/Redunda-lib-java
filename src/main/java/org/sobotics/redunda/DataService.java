package org.sobotics.redunda;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronizes files across bot-instances via Redunda
 * */
public class DataService {
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
	 * */
	public List<String> getTrackedFiles() {
		return this.trackedFiles;
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
