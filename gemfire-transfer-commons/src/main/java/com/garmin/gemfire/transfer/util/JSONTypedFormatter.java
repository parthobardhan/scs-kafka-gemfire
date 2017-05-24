package com.garmin.gemfire.transfer.util;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import com.garmin.gemfire.transfer.model.TransportRecord;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.pdx.PdxInstance;
import com.gemstone.gemfire.pdx.PdxInstanceFactory;

public class JSONTypedFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JSONTypedFormatter.class);
	private static ObjectMapper mapper = new ObjectMapper();
	private static final String TYPE_SUFFIX = "__type";
	
	/**
	 * @param key - The key for the object 
	 * @param obj - The object (This really should be a PDX object, but there are no guarantees.)
	 * @param operation - The GemFire operation
	 * @param regionName - The GemFire regionName
	 * @param timeStamp - Let the client pass this in rather than generate it ourselves.  
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String toJsonTransport(String key, Object obj, String operation, String regionName, Long timeStamp) throws JsonProcessingException  {
		String json = "{}";
		//for a destroy operation, the obj will probably be null
		if (obj != null) {
			json = "{" + JSONTypedFormatter.objectToJsonTuple("root", obj) + "}";
		}
		
		String jsonTransport = "{" + formatTuple(TransportRecord.FIELD_OPERATION, operation) + ","
								   + formatTuple(TransportRecord.FIELD_KEY, key) + ","
								   + formatTuple(TransportRecord.FIELD_REGION, regionName) + ","
								   + formatTupleNum(TransportRecord.FIELD_TIMESTAMP, timeStamp) + ","
								   + formatSingle(TransportRecord.FIELD_OBJECT) + ":" + json 
								   + "}";
		return jsonTransport;			
	}
	
	public static TransportRecord transportRecordFromJson(ClientCache cache, String json) throws JsonProcessingException, IOException {
		JsonNode root = mapper.readTree(json);
		
		JsonNode object = root.get(TransportRecord.FIELD_OBJECT);	
		PdxInstance pi = (PdxInstance)jsonNodeToObject(cache, object, "root");
		
		String region = root.get(TransportRecord.FIELD_REGION).asText();
		String key = root.get(TransportRecord.FIELD_REGION).asText();
		String operation = root.get(TransportRecord.FIELD_REGION).asText();
		Long timestamp = root.get(TransportRecord.FIELD_REGION).asLong();
		
		TransportRecord jt = new TransportRecord(operation, key, region, timestamp, pi);
		
		return jt;
		
	}
	

	
	/**
	 * This will generate a JSON tuple, like "name":"bert" or "customer":{ some big object }
	 * The trick is that it will also record all the type information in the JSON.  Every field 
	 * will have a sibling "<field>__type" field that tells us the data type.  
	 * 
	 * @param fieldName - The JSON field name
	 * @param field - The JSON field value (object/string/whatever) 
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String objectToJsonTuple(String fieldName, Object field) throws JsonProcessingException {
		String fieldNameType = fieldName + TYPE_SUFFIX;
		
		StringBuilder json = new StringBuilder();	
		if (field == null) {
			return formatTuple(fieldName, null);
		}

		if (field instanceof PdxInstance) {
			PdxInstance pi = (PdxInstance)field;
			json.append(formatTuple(fieldNameType, field.getClass().getName() + ":" + pi.getClassName()));
			json.append(",");
		} else {	
			json.append(formatTuple(fieldNameType, field.getClass().getName()));			
			json.append(",");
		}

		
		if (field instanceof PdxInstance) {
			PdxInstance pi = (PdxInstance)field;
			json.append(formatSingle(fieldName));
			json.append(":");
			json.append("{");
			List<String> childFieldNames = pi.getFieldNames();
			for (Iterator<String> it = childFieldNames.iterator(); it.hasNext();) {
				String childFieldName = it.next();
				json.append(objectToJsonTuple(childFieldName, pi.getField(childFieldName)));
				if (it.hasNext()) json.append(",");				
			}
			json.append("}");
		} else if (field instanceof Iterable) {
			json.append(formatSingle(fieldName));
			json.append(":");
			json.append("[");
			Iterable iterable = (Iterable) field;
			int index = 0;
			for (Iterator it = iterable.iterator(); it.hasNext();) {
				json.append("{");
				json.append(objectToJsonTuple("" + index, it.next()));
				json.append("}");
				if (it.hasNext()) json.append(",");	
				index++;
			}
			json.append("]");
		} else if (field instanceof Object[]) {
			json.append(formatSingle(fieldName));
			json.append(":");
			json.append("[");
			Object[] iterable = (Object[]) field;
			int index = 0;
			for (Object o : iterable) {
				json.append("{");
				json.append(objectToJsonTuple("" + index,o));
				json.append("}");
				index++;
				if (index < iterable.length) json.append(",");	
			}
			json.append("]");
		} else if (field instanceof int[]) {
			json.append(formatSingle(fieldName));
			json.append(":");
			json.append("[");
			int[] iterable = (int[]) field;
			int index = 0;
			for (Object o : iterable) {
				json.append("{");
				json.append(objectToJsonTuple("" + index,o));
				json.append("}");
				index++;
				if (index < iterable.length) json.append(",");	
			}
			json.append("]");													
		} else if (field instanceof Map) {			
			json.append(formatSingle(fieldName));
			json.append(":");
			json.append("{");
			Map map = (Map) field;
			for (Iterator it = map.keySet().iterator(); it.hasNext();) {
				Object keyObject = it.next();
				Object valueObject = map.get(keyObject);
				json.append(objectToJsonTuple(keyObject.toString(), valueObject));  				
				if (it.hasNext()) json.append(",");
			}
			json.append("}");	
		} else if (field instanceof Number) {
			json.append(formatTupleNum(fieldName, (Number)field));
		} else if (field instanceof Boolean) {
			json.append(formatTupleBool(fieldName, (Boolean)field));
		} else if (field instanceof Date) {
			json.append(formatTuple(fieldName, String.valueOf(((Date)field).getTime())));
		} else if (field instanceof String) {
			json.append(formatTuple(fieldName, field.toString()));
		} else if (field instanceof Locale) {
			json.append(formatTuple(fieldName, field.toString()));			
		} else {
			//TODO: remove system.outs before production.  They are only here because sometimes LOGGER is flakey in Test					
			System.out.println("fieldToJson Unhandled Field Type = " + field.getClass().getName());
			LOGGER.error("fieldToJson Unhandled Field Type = " + field.getClass().getName());
		}
		
		return json.toString();
	}
	
	/**
	 * This will take a JsonNode, and the name of a child node, and convert it child node into an object.
	 * The reason we need the parent node is to access siblings of the child node. 
	 * 
	 * @param cache
	 * @param parentNode
	 * @param fieldName
	 * @return
	 */
	public static Object jsonNodeToObject(ClientCache cache, JsonNode parentNode, String fieldName) {
		JsonNode typeNode = parentNode.get(fieldName + TYPE_SUFFIX);
		JsonNode dataNode = parentNode.get(fieldName);
		
		String type = null;
		if (typeNode != null) type = typeNode.asText();
		
		if (type == null) {
			return null;
		}

		String[] typeParts = type.split(":");
		String pdxInnerClass = null;
		if (typeParts.length > 1) {
			type = typeParts[0];
			pdxInnerClass = typeParts[1];
		}
		switch (type) {
			case "java.lang.String":
				return dataNode.asText(); 				
			case "java.lang.Boolean":
				return dataNode.asBoolean();				
			case "java.lang.Integer":
				return new Integer(dataNode.asInt());
			case "java.lang.Float":
			case "java.lang.Double":
				return new Float(dataNode.asDouble());
			case "java.util.Date":
				return new Date(dataNode.asLong());
			case "java.sql.Timestamp":
				return new Timestamp(dataNode.asLong());							
			case "int":
				return dataNode.asInt();			
			case "java.lang.Long":
				return new Long(dataNode.asLong());
			case "long":
				return dataNode.asLong();
			case "com.gemstone.gemfire.pdx.internal.EnumInfo$PdxInstanceEnumInfo":
				JsonNode enumNode = parentNode.get(fieldName);
				String name = enumNode.get("name").asText();
				Integer ordinal = enumNode.get("ordinal").asInt();
				PdxInstance pdxEnum = cache.createPdxEnum(pdxInnerClass, name, ordinal);
				return pdxEnum;
			case "com.gemstone.gemfire.pdx.internal.PdxInstanceImpl":
				PdxInstanceFactory writeablePdxFactory = cache.createPdxInstanceFactory(pdxInnerClass);
				Iterator<String> it = dataNode.fieldNames();
				while (it.hasNext()) {
					String subFieldName = it.next();
					if (subFieldName.endsWith(TYPE_SUFFIX)) continue;				
					Object subObj = jsonNodeToObject(cache, dataNode, subFieldName);
					writeablePdxFactory.writeObject(subFieldName, subObj);
				}			
				return writeablePdxFactory.create();
			case "java.util.Collections$UnmodifiableMap":				
			case "java.util.HashMap":
				HashMap map = new HashMap();
				Iterator<String> mapKeys = dataNode.fieldNames();
				while (mapKeys.hasNext()) {
					String mapKey = mapKeys.next();
					if (mapKey.endsWith(TYPE_SUFFIX)) continue;
					JsonNode fieldNode = dataNode.get(mapKey); 
					Object subObj = jsonNodeToObject(cache, dataNode, mapKey);
					map.put(mapKey, subObj);			
				}
				if (type.endsWith("UnmodifiableMap")) {
					Map umap = Collections.unmodifiableMap(map);
					return umap;					
				} else {
					return map;
				}
			case "java.util.Locale":
				String value = dataNode.asText();
				Locale loc = null;
				String[] locParts = value.split("_");
				String language = locParts[0];
				if (locParts.length > 1) {
					String country = value.split("_")[1];
					loc = new Locale(language, country);
				} else {
					loc = new Locale(language);
				}
				return loc;							
			case "[I":
				ArrayNode arrayNode = (ArrayNode) parentNode.get(fieldName);
				int[] intArr = new int[arrayNode.size()];
				for (JsonNode node : arrayNode) {		
					for (Iterator<String> nodeFields = node.fieldNames();nodeFields.hasNext();) {
						String nodeField = nodeFields.next();
						if (nodeField.endsWith(TYPE_SUFFIX)) continue;
						JsonNode ixNode = node.get(nodeField);
						int ivalue = ixNode.asInt();
						intArr[Integer.parseInt(nodeField)] = ivalue;
					}
				}
				return intArr;				
//			case "[Ljava.lang.Object;":  //TODO: This hasn't been tested.  I haven't tested an array of objects yet.
//				ArrayNode objArrayNode = (ArrayNode) parentNode.get(fieldName);
//				Object[] objArr = new Object[objArrayNode.size()];
//				int oindex = 0;
//				for (JsonNode node : objArrayNode) {
//					Iterator<String> arFieldNames = node.fieldNames();
//					while (arFieldNames.hasNext()) {
//						String arFieldName = arFieldNames.next();
//						if (arFieldName.endsWith(TYPE_SUFFIX)) continue;
//						Object arrObj = jsonNodeToObject(cache, node, arFieldName);
//						objArr[oindex] = arrObj;					
//					}
//					oindex++;
//				}
//				return objArr;							
			case "java.util.ArrayList":
				ArrayList list = new ArrayList();
				ArrayNode arrayListNode = (ArrayNode) parentNode.get(fieldName);
				int listIndex = 0;
				for (JsonNode node : arrayListNode) {
					Iterator<String> arFieldNames = node.fieldNames();
					while (arFieldNames.hasNext()) {
						String arFieldName = arFieldNames.next();
						if (arFieldName.endsWith(TYPE_SUFFIX)) continue;
						Object arrObj = jsonNodeToObject(cache, node, arFieldName);
						list.add(arrObj);					
					}
					listIndex++;
				}
				return list;
			default:
				//TODO: remove system.outs before production.  They are only here because sometimes LOGGER is flakey in Test
				System.out.println("jsonNodeToObject Unhandled type = " + type);
				LOGGER.error("jsonNodeToObject Unhandled type = " + type);		
		}
		return null;		
	}
	
	
	private static String formatTuple(String left, String right) throws JsonProcessingException {
		if (right == null) return mapper.writeValueAsString(left) + ":" + "null"; 
		return mapper.writeValueAsString(left) + ":" + mapper.writeValueAsString(right);
	}

	private static String formatTupleNum(String left, Number right) throws JsonProcessingException {
		if (right == null) return mapper.writeValueAsString(left) + ":" + "null"; 
		return mapper.writeValueAsString(left) + ":" + right;
	}
	
	private static String formatTupleBool(String left, Boolean right) throws JsonProcessingException {
		if (right == null) return mapper.writeValueAsString(left) + ":" + "null"; 
		return mapper.writeValueAsString(left) + ":" + right;
	}

	
	private static String formatSingle(String left) throws JsonProcessingException {
		if (left == null) return "null";
		return mapper.writeValueAsString(left);
	}
	
	
}