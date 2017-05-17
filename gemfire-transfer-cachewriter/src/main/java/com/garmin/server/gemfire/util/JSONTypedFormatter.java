package com.garmin.server.gemfire.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.query.internal.StructImpl;
import com.gemstone.gemfire.pdx.PdxInstance;
import com.gemstone.gemfire.pdx.PdxInstanceFactory;
import com.gemstone.gemfire.pdx.internal.EnumInfo.PdxInstanceEnumInfo;

public class JSONTypedFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JSONTypedFormatter.class);
	private static ObjectMapper mapper = new ObjectMapper();

	//Convert an Object or PDX Object into formatted JSON
	public static String toPrettyJSON(Object obj) throws JsonProcessingException {
		String rtn = toFlatJSON(obj);
		try {
			JsonNode node = mapper.readTree(rtn);
			rtn = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return rtn;
	}

	//Convert an Object or PDX Object into UN-formatted (flat single line) JSON
	public static String toFlatJSON(Object obj){
		if (obj == null)
			return "null";
		String value = "";
		if (!(obj instanceof PdxInstance)) {
			if (obj instanceof Iterable) { // Sets, Collections
				value += "[";
				Iterable iterable = (Iterable) obj;
				int index = 0;
				for (Object o : iterable) 
					value += toFlatJSON(o) + ",";
				value = commaTrim(value);
				value += "]";
			} else if (obj instanceof Object[]) { // Arrays (Object [])
				value += "[";
				Object[] array = (Object[]) obj;
				for (Object o : array)
					value += toFlatJSON(o) + ",";
				value = commaTrim(value);
				value += "]";
			} else if (obj instanceof Map) { // Maps (HashMap, Hashtable, Properties)
				value += "{";
				Map map = (Map) obj;
				for (Object o : map.keySet()) {
					
					// Json requires that keys in a Map be strings. Java does not.
					if (o instanceof Number)
						value += "\"";
					value += toFlatJSON(o);
					if (o instanceof Number)
						value += "\"";
					value += ": ";
					
					Object valueObject = map.get(o);
					
					value += toFlatJSON(valueObject) + ",";					
					value += toFlatJSON(o.toString() + "__type") + ": ";
					String type = null;
					if (valueObject != null) {
						type = valueObject.getClass().getName();
						value += toFlatJSON(type);
					} else {
						value += "null";
					}
					value += ",";
					
					if (valueObject != null) {
						if (valueObject instanceof PdxInstance) {
							value += toFlatJSON(o.toString() + "__class") + ": ";
							String pdxType = ((PdxInstance)valueObject).getClassName();
							value += toFlatJSON(pdxType);
							value += ",";
						} else if (valueObject instanceof Iterable) {
							 Iterable iterableObject = (Iterable) valueObject;
							 String firstObjectClassName = "null";					
							 if (iterableObject.iterator().hasNext()) {
							   	   Object firstObject = iterableObject.iterator().next();
								   firstObjectClassName = firstObject.getClass().getName();									
							 }
							 value += toFlatJSON(o.toString() + "__class") + ": ";
							 value += toFlatJSON(firstObjectClassName);
							 value += ",";							 
						} else if (valueObject instanceof Object[]) {
							 Object[] objectArray = (Object[]) valueObject;
							 String firstObjectClassName = "null";					
							 if (objectArray.length > 0) {
						 		 Object firstObject = objectArray[0];
								 firstObjectClassName = firstObject.getClass().getName();
								 if (firstObject instanceof PdxInstance) firstObjectClassName = ((PdxInstance) firstObject).getClassName();
							 }
							 value += toFlatJSON(o.toString() + "__class") + ": ";
							 value += toFlatJSON(firstObjectClassName);
  							 value += ",";							 
						}
						
					}						
				}
				value = commaTrim(value);
				value += "}";
			} else if (obj instanceof StructImpl) {
				StructImpl struct = (StructImpl) obj;
				for (String name : struct.getFieldNames()) {
					value += toFlatJSON(name) + ":";
					value += toFlatJSON(struct.get(name));
					value += ", ";
				}
				value = commaTrim(value);
			} else {
				// Ultimately, everything ends up here once we recurse down to the point that our field is 
				// some simple data type like String or Long or Date, and we can't recurse further.
				// The mapper will handle things like escaping values if a string has quotes in it.
				try {
					value += mapper.writeValueAsString(obj);
				} catch (JsonProcessingException jpe) {
					value += "UNABLE TO CONVERT OBJECT INTO JSON: " + obj.getClass().getName();
					LOGGER.error("JSONTypedFormatter: Unable to convert object into JSON: " + obj.getClass().getName());
				}
			}
		} else {
			if (obj instanceof PdxInstanceEnumInfo) {
				PdxInstanceEnumInfo pie = (PdxInstanceEnumInfo) obj;
				value += pdxToFlatJson(pie);
			} else {
				PdxInstance pi = (PdxInstance) obj;
				value += pdxToFlatJson(pi);
			}
		}
		return value;
	}

	//If this is a PDX object, then we need to add "type" and "class" information to the JSON
	//For example, an ArrayList<Customer>() would have a type of "ArrayList" and a 
	//class of "Customer".  
	private static String pdxToFlatJson(PdxInstance pdi)  {
		String rtn = "{";
		List<String> fieldNames = pdi.getFieldNames();
		for (String field : fieldNames) {

			rtn += toFlatJSON(field) + ": ";
			rtn += toFlatJSON(pdi.getField(field));
			rtn += ",";
			rtn += toFlatJSON(field + "__type") + ": ";
			Object object = pdi.getField(field);
			String type = "null";
			if (object != null) {
				type = object.getClass().getName();
				rtn += toFlatJSON(type);
			} else { 			
				rtn += "null";
			}
			
			rtn += ",";
					
			if (object != null) {
				String firstObjectClassName = object.getClass().getName();
				if (object instanceof Iterable) {
					Iterable iterableObject = (Iterable) object;
					if (iterableObject.iterator().hasNext()) {
						Object firstObject = iterableObject.iterator().next();
						firstObjectClassName = ((PdxInstance) firstObject).getClassName();
					}
				} else if (object instanceof Object[]) {
					Object[] objectArray = (Object[]) object;
					if (objectArray.length > 0) {
						Object firstObject = objectArray[0];
						firstObjectClassName = firstObject.getClass().getName();
						if (firstObject instanceof PdxInstance) firstObjectClassName = ((PdxInstance) firstObject).getClassName();
					}
				} else if (object instanceof PdxInstance) {
					firstObjectClassName = ((PdxInstance) object).getClassName();
				} else {
					firstObjectClassName = null;
				}
				
				if (firstObjectClassName != null) {
					rtn += toFlatJSON(field + "__class") + ": ";
					rtn += toFlatJSON(firstObjectClassName);
					rtn += ",";
				}				
			}
		}
		rtn = commaTrim(rtn);
		rtn += "}";
		return rtn;
	}

	
	public static String toJsonTransport(String key, Object obj, String operation, String regionName)  {
		String clazz = "N/A";			
		String json = "{}";
		//for a destroy operation, the obj will probably be null
		if (obj != null) {
			json = toFlatJSON(obj);
			clazz = obj.getClass().getName();
			if (obj instanceof PdxInstance) {
				clazz = ((PdxInstance)obj).getClassName();
			}
		}
		
		String jsonTransport = "{\"operation\":\"" + operation + "\", \"timestamp\":\"" + String.valueOf(System.currentTimeMillis()) 
		       + "\", \"region\"" + ":" + "\"" + regionName + "\"" + ", \"class\":\"" + clazz + "\", \"object\" : " + json 
		       + ", \"key\":\"" + key + "\"}";
		return jsonTransport;			
	}
	

	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/* "JsonTransport" is a wrapper around the JSON object.  It contains the object, plus all the transport
	 * 	information like the region, the region operation, a timestamp, the key, etc.  
	 */
	public static PdxInstance jsonTransportToPdx(ClientCache cache, String json) throws JsonProcessingException, IOException {
		JsonNode root = mapper.readTree(json);
		String clazz = root.get("class").asText();
		PdxInstanceFactory writeablePdx = cache.createPdxInstanceFactory(clazz);
		JsonNode objectNode = root.get("object");
		jsonNodeToPdx(cache, objectNode, writeablePdx);
		PdxInstance pi = writeablePdx.create();
		return pi;
	}

	
	
	private static void jsonNodeToPdx(ClientCache cache, JsonNode objectNode, PdxInstanceFactory writeablePdx) {
		Iterator<String> it_fields = objectNode.fieldNames();
		while (it_fields.hasNext()) {
			String fieldName = it_fields.next();
			if (fieldName.endsWith("__type")) continue;
			if (fieldName.endsWith("__class")) continue;
			
			JsonNode typeNode = objectNode.get(fieldName + "__type");
			String type = typeNode.asText();
		
			if (type.equals("java.lang.String")) {
				String value = objectNode.get(fieldName).asText();
				writeablePdx.writeString(fieldName, value);
			} else if (type.equals("java.lang.Long")) {
				String value = objectNode.get(fieldName).asText();
				writeablePdx.writeLong(fieldName, Long.parseLong(value));
			} else if (type.equals("java.lang.Integer")) {
				String value = objectNode.get(fieldName).asText();
				writeablePdx.writeInt(fieldName, Integer.parseInt(value));
			} else if (type.equals("java.util.Date")) {
				String value = objectNode.get(fieldName).asText();
				Long lvalue = Long.parseLong(value);
				writeablePdx.writeDate(fieldName, new Date(lvalue));
			} else if (type.equals("java.lang.Boolean")) {
				String value = objectNode.get(fieldName).asText(); 
				writeablePdx.writeBoolean(fieldName, Boolean.parseBoolean(value));
			} else if (type.equals("java.util.Locale")) {
				String value = objectNode.get(fieldName).asText();
				Locale loc = null;
				String[] locParts = value.split("_");
				String language = locParts[0];
				if (locParts.length > 1) {
					String country = value.split("_")[1];
					loc = new Locale(language, country);
				} else {
					loc = new Locale(language);
				}
				writeablePdx.writeObject(fieldName, loc);
			} else if (type.equals("null")) {
				writeablePdx.writeObject(fieldName, null);
			} else if (type.equals("java.util.ArrayList")) {
				JsonNode clazzNode = objectNode.get(fieldName + "__class");
				String clazz = null;
				if (clazzNode != null)
					clazz = clazzNode.asText();
				ArrayList list = new ArrayList();

				ArrayNode arrayNode = (ArrayNode) objectNode.get(fieldName);
				for (JsonNode node : arrayNode) {
					PdxInstanceFactory writeablePdx2 = cache.createPdxInstanceFactory(clazz);
					jsonNodeToPdx(cache, node, writeablePdx2);
					PdxInstance subPi = writeablePdx2.create();
					list.add(subPi);
				}
				writeablePdx.writeObject(fieldName, list);
			} else if (type.equals("[Ljava.lang.Object;")) {
				JsonNode clazzNode = objectNode.get(fieldName + "__class");
				String clazz = null;
				if (clazzNode != null)
					clazz = clazzNode.asText();
				ArrayNode arrayNode = (ArrayNode) objectNode.get(fieldName);
				Object[] objArr = new Object[arrayNode.size()];

				int index = 0;
				for (JsonNode node : arrayNode) {
					PdxInstanceFactory writeablePdx2 = cache.createPdxInstanceFactory(clazz);
					jsonNodeToPdx(cache, node, writeablePdx2);
					PdxInstance subPi = writeablePdx2.create();
					objArr[index] = subPi;
					index++;
				}
				writeablePdx.writeObject(fieldName, objArr);
			} else if (type.equals("[Ljava.lang.Integer;")) {
					ArrayNode arrayNode = (ArrayNode) objectNode.get(fieldName);
					Integer[] intArr = new Integer[arrayNode.size()];
					int index = 0;
					for (JsonNode node : arrayNode) {
						Integer value = node.asInt();
						intArr[index] = value;
						index++;
					}
					writeablePdx.writeObject(fieldName, intArr);

			} else if (type.equals("[I")) {
				ArrayNode arrayNode = (ArrayNode) objectNode.get(fieldName);
				int[] intArr = new int[arrayNode.size()];
				int index = 0;
				for (JsonNode node : arrayNode) {
					int value = node.asInt();
					intArr[index] = value;
					index++;
				}
				writeablePdx.writeIntArray(fieldName, intArr);
			} else if (type.contains("PdxInstanceEnumInfo")) {
				JsonNode clazzNode = objectNode.get(fieldName + "__class");
				String clazz = clazzNode.asText();
				JsonNode enumNode = objectNode.get(fieldName);
				String name = enumNode.get("name").asText();
				Integer ordinal = enumNode.get("ordinal").asInt();
				PdxInstance pdxEnum = cache.createPdxEnum(clazz, name, ordinal);
				writeablePdx.writeObject(fieldName, pdxEnum);

			} else if (type.equals("java.util.HashMap") || type.contains("UnmodifiableMap")) {
				HashMap map = new HashMap();
				JsonNode mapNode = objectNode.get(fieldName);
				Iterator<String> mapKeys = mapNode.fieldNames();
				while (mapKeys.hasNext()) {
					String mapKey = mapKeys.next();
					if (mapKey.endsWith("__class")) continue;
					if (mapKey.endsWith("__type")) continue;
					
					JsonNode mapValue = mapNode.get(mapKey);					
					JsonNode valueTypeNode = mapNode.get(mapKey + "__type");
					JsonNode clazzNode = mapNode.get(mapKey + "__class");

					String valueType = "null";
					if (valueTypeNode != null) {
						valueType = valueTypeNode.asText();
					}
					
					if (valueType.equals("java.lang.Long")) {
						map.put(mapKey, mapValue.asLong());
					} else if (valueType.equals("java.lang.String")) {
						map.put(mapKey, mapValue.asText());
					} else if (valueType.equals("java.lang.Boolean")) {
						map.put(mapKey, mapValue.asBoolean());
					} else if (valueType.equals("null")) {
						map.put(mapKey, null);
					} else if (valueType.contains("PdxInstance")) {
						if (clazzNode == null) {
							System.out.println(mapKey + ":" + mapValue);
						}
						String clazz = clazzNode.asText();
						PdxInstanceFactory writeablePdx2 = cache.createPdxInstanceFactory(clazz);
						jsonNodeToPdx(cache, mapValue, writeablePdx2);
						PdxInstance subPi = writeablePdx2.create();
						map.put(mapKey,  subPi);
						
					} else {
						System.out.println("@@@@@@@@@@@ Unhandled map object type " + valueType);
					}
					
				}
				if (type.contains("UnmodifiableMap")) {
					Map umap = Collections.unmodifiableMap(map);
					writeablePdx.writeObject(fieldName, umap);					
				} else {
					writeablePdx.writeObject(fieldName, map);
				}
			} else if (type.contains("PdxInstance")) {
				JsonNode clazzNode = objectNode.get(fieldName + "__class");
				String clazz = clazzNode.asText();
				PdxInstanceFactory writeablePdx2 = cache.createPdxInstanceFactory(clazz);
				JsonNode oNode = objectNode.get(fieldName);
				jsonNodeToPdx(cache, oNode, writeablePdx2);
				PdxInstance subPi = writeablePdx2.create();
				writeablePdx.writeObject(fieldName, subPi);
			} else {
				System.out.println("Defaulting type = " + type);
				PdxInstanceFactory writeablePdx2 = cache.createPdxInstanceFactory(type);
				JsonNode oNode = objectNode.get(fieldName);
				jsonNodeToPdx(cache, oNode, writeablePdx2);
				PdxInstance subPi = writeablePdx2.create();
				writeablePdx.writeObject(fieldName, subPi);
			}
		}
	}

	public static String commaTrim(String arg) {
		while (arg.endsWith(",") || arg.endsWith(" "))
			arg = arg.substring(0, arg.length() - 1);
		return arg;
	}

}