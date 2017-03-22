package org.sobotics.redunda;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A helper to get an user agent for the library
 * */
class UserAgent {
	/**
	 * Generates an user agent used by the library
	 * 
	 * @return The user agent
	 * */
	static String getUserAgent() {
		Properties prop = new Properties();
        
        try{
            InputStream is = UserAgent.class.getResourceAsStream("/redunda-lib.properties");
            prop.load(is);
        }
        catch (IOException e){
            e.printStackTrace();
            return "redunda-lib-java/1.8";
        }
		
		
		return "redunda-lib-java/"+prop.getProperty("version", "1.8");
	}
}
