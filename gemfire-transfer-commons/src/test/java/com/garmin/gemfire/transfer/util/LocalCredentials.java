package com.garmin.gemfire.transfer.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * This is just a test class for getting usernames and passwords from your local file system so that we don't
 * check those things into source control.  Ultimately, prompting you to manually input the username and password
 * from the console for every test is impractical, so we have to get the password from somewhere.  Long-term
 * it will be from Thycotic or a proper credentials store, but for now we just store passwords in properties files.
 */

public class LocalCredentials {

	public static boolean initialLoad = true;	    
	public static int BATCH_SIZE = 10000;
	public static final String FIELD_SEPARATOR = "⚉";			
	private static Logger LOG = LoggerFactory.getLogger(LocalCredentials.class);
	public static Properties loadAdminCreds(String locator) {	
				
		if (locator.endsWith(".garmin.com")) locator = locator.replace(".garmin.com","");
		Properties props = new Properties();
		try {
			String credFile = "/web/secure-config/gemfire/" + locator + ".properties";
			File f = new File(credFile);
			if (!f.exists()) {
				LOG.error("\n\n********Can't find required file: " + credFile + "\nExiting application.\n");
				return props;
			}
			FileReader fr = new FileReader(new File(credFile));
			props.load(fr);
			fr.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}	
		return props;
	}
	
	public static Properties loadRegionCreds(String locator, String region) {	
		
		Properties props = new Properties();
		try {
			String credFile = "/web/secure-config/gemfire/" + locator + "." + region + ".properties";
			File f = new File(credFile);
			if (!f.exists()) {
				LOG.error("\n\n********Can't find required file: " + credFile + "\nExiting application.\n");
				return props;
			}
			FileReader fr = new FileReader(new File(credFile));
			props.load(fr);
			fr.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}	
		return props;
	}	

	
	public static Properties loadJMXCreds(String locator) {	
		
		Properties props = new Properties();
		try {
			String credFile = "/web/secure-config/gemfire/" + locator + ".jmx.properties";
			File f = new File(credFile);
			if (!f.exists()) {
				LOG.error("\n\n********Can't find required file: " + credFile + "\nExiting application.\n");
				System.exit(-1);
			}
			FileReader fr = new FileReader(new File(credFile));
			props.load(fr);
			fr.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}	
		return props;
	}
}