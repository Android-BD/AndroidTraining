package ch.ethz.inf.vs.android.siwehrli.a3;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The propose of this class is to represent a textmessage with associated
 * lamport and vector time in a generic way. It provides functions to parse from
 * and into coresponding JSON-objects.
 * 
 * @author Simon
 * 
 */
public class TextMessage implements Comparable<TextMessage> {
	private String message;
	private String senderName = "unknown";
	
	public String getSenderName() {
		return senderName;
	}

	private Map<Integer, Integer> vectorTime = null;
	private static final boolean LAMPORT_MODE = true;
	private boolean isDelayedPublished = false;
	private boolean isErrorMessage = false;

	public boolean isDelayedPublished() {
		return isDelayedPublished;
	}

	public boolean isErrorMessage() {
		return isErrorMessage;
	}

	public void setDelayedPublished() {
		this.isDelayedPublished = true;
	}

	public TextMessage(String message, int lamportTime) {
		this.message = message;
		this.vectorTime = new HashMap<Integer, Integer>(1);
		this.vectorTime.put(0, lamportTime);
	}

	public TextMessage(String message, Map<Integer, Integer> vectorTime) {
		this.message = message;
		this.vectorTime = vectorTime;
	}

	public TextMessage(JSONObject jsonMessage, int backupLamportTime)
			throws JSONException {
		this.message = jsonMessage.getString("message");
		// it's possible that there is no time vector
		if (jsonMessage.has("time_vector"))
			this.vectorTime = readTimeVector(jsonMessage
					.getJSONObject("time_vector"));
		else {

			Map<Integer, Integer> backupVectorTime = new HashMap<Integer, Integer>(
					1);
			backupVectorTime.put(0, backupLamportTime);
			this.vectorTime = backupVectorTime;
		}

	}

	public TextMessage(JSONObject jsonMessage,
			Map<Integer, Integer> backupVectorTime) throws JSONException {
		this.message = jsonMessage.getString("text");
		// it's possible that there is no time vector
		if (jsonMessage.has("time_vector"))
			this.vectorTime = readTimeVector(jsonMessage
					.getJSONObject("time_vector"));
		else
			this.vectorTime = backupVectorTime;
	}

	public JSONObject getJSONObject() throws JSONException {
		JSONObject object = new JSONObject();
		object.put("text", message);
		object.put("lamport_time",
				getVectorTimeJSONObject(this.getVectorTime()));
		return object;
	}

	public String getFormatedMessage() {
		return message;
	}

	public String getFormatedTime() {
		if (LAMPORT_MODE) {
			return this.vectorTime.get(0) + "";
		} else {
			// TODO Frederik
			return null;
		}
	}

	public int getLamportTime() {
		return this.vectorTime.get(0);
	}

	public Map<Integer, Integer> getVectorTime() {
		return this.vectorTime;
	}

	public int compareTo(TextMessage another) {
		if (LAMPORT_MODE) {
			return this.getLamportTime() - another.getLamportTime();
		} else {
			// TODO Frederik
			return 0;
		}
	}

	public boolean isDeliverable(Map<Integer, Integer> currentVectorTime) {
		// TODO Frederik
		return this.getLamportTime() - currentVectorTime.get(0) <= 1;
	}

	public boolean isDelayed(Map<Integer, Integer> currentVectorTime) {
		// TODO Frederik
		return this.getLamportTime() - currentVectorTime.get(0) < 0;
	}

	/**
	 * This parses the time vector out of a JSON-Object
	 * 
	 * @param o
	 *            , the JSON-(sub-)object holding the time vector
	 * @return the vector time as a map
	 * @throws JSONException
	 */
	public static Map<Integer, Integer> readTimeVector(JSONObject o)
			throws JSONException {
		JSONArray names = o.names();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>(
				names.length());

		for (int i = 0; i < names.length(); ++i) {
			map.put(names.getInt(i), o.getInt(names.getString(i)));
		}
		return map;
	}

	/**
	 * This builds a JSONObject representing the vector time (and only the
	 * vector!)
	 * 
	 * @param timeVector
	 * @return JSON-Object representation
	 * @throws JSONException
	 */
	public static JSONObject getVectorTimeJSONObject(
			Map<Integer, Integer> timeVector) throws JSONException {
		JSONObject object = new JSONObject();
		for (int i : timeVector.keySet()) {
			object.put(i + "", timeVector.get(i));
		}
		return object;
	}

	public void setErrorType() {
		this.isErrorMessage = true;

	}
}
