package org.sobotics.redunda;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Represents a file that is synced with Redunda
 * */
public class SyncedFile {
	private String path;
	private byte[] data;
	
	/**
	 * Initializes `SyncedFile` with byte-content.
	 * */
	public SyncedFile(String path, byte[] data) {
		this.path = path;
		this.data = data;
	}
	
	/**
	 * Loads a file from the given path
	 * 
	 * https://stackoverflow.com/a/8895205/4687348
	 * 
	 * @throws IOException 
	 * */
	public static SyncedFile loadFrom(String path) throws IOException {
		File inputFile = new File(path);
		byte[] newData = new byte[(int) inputFile.length()];
		FileInputStream fis = new FileInputStream(inputFile);
		fis.read(newData, 0, newData.length);
		fis.close();
		
		SyncedFile newFile = new SyncedFile(path, newData);
		
		return newFile;
	}
	
	/**
	 * Writes the file to the disk.
	 * @throws IOException 
	 * */
	public void writeToDisk() throws IOException {
		//FileOutputStream fos = new FileOutputStream(this.path);
		Files.write(Paths.get(this.path), this.data, StandardOpenOption.CREATE);
	}
	
	public String getPath() {
		return this.path;
	}
	
	public byte[] getData() {
		return this.data;
	}
}
