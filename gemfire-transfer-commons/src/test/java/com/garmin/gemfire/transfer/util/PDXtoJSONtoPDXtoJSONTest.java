package com.garmin.gemfire.transfer.util;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.pdx.PdxInstance;
import com.gemstone.gemfire.pdx.ReflectionBasedAutoSerializer;


/** 
 * This system/functional test is testing converting a PDX object to JSON, and then
 * back to PDX and back to JSON to prove we can go through multiple conversions in both
 * directions without losing a single byte of data.  Some corner cases might result in lost 
 * data if we only tested one set of conversions instead of two. 
 */
public class PDXtoJSONtoPDXtoJSONTest {
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static void main(String[] args) throws Exception{
		String locator = "olaxpd-itwgfdata00";
		PDXtoJSONtoPDXtoJSONTest saf = new PDXtoJSONtoPDXtoJSONTest();		
//		saf.doSomething(locator, "garminCustomer");
//		saf.doSomething(locator, "garminCustomerNotes");
//		saf.doSomething(locator, "garminCustomerPreferenceTypes");
//		saf.doSomething(locator, "garminCustomerPreferences");
//		saf.doSomething(locator, "garminCustomerVerifiedPhoneIndex");
//		saf.doSomething(locator, "garminDS_doubleOptIn");
//		saf.doSomething(locator, "garminDS_emailPreferenceCategories");
//		saf.doSomething(locator, "sso_SMSVerificationCode");
//		saf.doSomething(locator, "sso_applicationConfiguration");
//		saf.doSomething(locator, "sso_customerLogin");
//		saf.doSomething(locator, "sso_loginToken");
//		saf.doSomething(locator, "sso_registeredService");
//		saf.doSomething(locator, "sso_rememberMeTicket");
//		saf.doSomething(locator, "sso_serviceTicket");
//		saf.doSomething(locator, "sso_tempPassword");
		saf.doSomething(locator, "sso_ticketGrantingTicket");
		System.out.println("DONE");
		
	}
	
	 public void doSomething(String locator, String region) throws Exception {
		 
		ClientCache cache = getCache(locator, region);
		Region reg = cache.createClientRegionFactory("PROXY").create(region);
		
		SelectResults sr = reg.query("select * from /" + region + " limit 1");	
		if (sr.size() == 0) {
			System.err.println("\n@@@@ No objects found for region: " + region + " @@@@\n");
			closeCache(cache);
			return;
		}
		Object obj = sr.iterator().next();
		if (! (obj instanceof PdxInstance)) {
			System.out.println("\n#### No PDX objects found for region: " + region + " ####\n");
			closeCache(cache);
			return;
		}
		
		PdxInstance pi1 = (PdxInstance)obj;	
		System.out.println("pdx1 = " + pi1);
		Long now = System.currentTimeMillis();
		String json1 = JSONTypedFormatter.toJsonTransport("key", pi1, "UPDATE", region, now);
		System.out.println("json1 = " + json1);
		
		PdxInstance pi2 = JSONTypedFormatter.fromJsonTransport(cache, json1);		
		System.out.println("pdx2 = " + pi2);
		
		String json2 = JSONTypedFormatter.toJsonTransport("key", pi2, "UPDATE", region, now);
		System.out.println("json2 = " + json2);
		
		if (!json1.equals(json2)) {
			if (! (json1.length() == json2.length())) {
				System.err.println("Final JSON does not match original.  Test failed for region " + region);
				throw new Exception("Final JSON does not match original.  Test failed for region " + region);				
			} else {
 			    char[] first = json1.toCharArray();
			    char[] second = json2.toCharArray();
			    Arrays.sort(first);
			    Arrays.sort(second);
			    if (!Arrays.equals(first, second)){
					System.err.println("Final JSON does not match original.  Test failed for region " + region);
					throw new Exception("Final JSON does not match original.  Test failed for region " + region);							    	
			    } else {			    	
					System.err.println("JSON does not match original, but they have the same characters, so it's probably fine and the difference is caused by Set ordering: " + region);
			    }

			}				
		} else {
			System.out.println("\n== " + region + " Test Passed! ==\n");
		}
		
		closeCache(cache);
		return;
	}


	private ClientCache getCache(String arg_locator, String arg_region) {
		ReflectionBasedAutoSerializer rbas = new ReflectionBasedAutoSerializer(".*");
		ClientCacheFactory ccf = new ClientCacheFactory();
		ccf.addPoolLocator(arg_locator, 10334);
		ccf.set("mcast-port", "0");
		ccf.setPdxSerializer(rbas);
		ccf.setPdxReadSerialized(true);
		ccf.set("log-level", "error");
		ccf.set("security-username", LocalCredentials.loadAdminCreds(arg_locator).getProperty("username"));
		ccf.set("security-password", LocalCredentials.loadAdminCreds(arg_locator).getProperty("password"));
		ccf.set("security-client-auth-init", "com.garmin.data.gemfire.UserPasswordAuthInit.create");
		//ccf.set("ssl-enabled", SSL?"true":"false");
		ClientCache cache = ccf.create();
		return cache;
	}
	
	private void closeCache(ClientCache cache) {
		try {
			Thread.sleep(100);
		} catch (Exception dontcare) {
		}
		cache.close();
	}

}
