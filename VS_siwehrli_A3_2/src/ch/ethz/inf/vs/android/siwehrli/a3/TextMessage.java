package ch.ethz.inf.vs.android.siwehrli.a3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The propose of this class is to represent a textmessage with associated
 * lamport and vector time in a generic way. It provides functions to parse from
 * and into corresponding JSON-objects.
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
	// vector time/lamport time switch
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

	public TextMessage(String message, String sender, int lamportTime) {
		this.message = message;
		this.senderName = sender;
		this.vectorTime = new HashMap<Integer, Integer>(1);
		this.vectorTime.put(0, lamportTime);
	}

	public TextMessage(String message, Map<Integer, Integer> vectorTime) {
		this.message = message;
		this.vectorTime = new HashMap<Integer, Integer>();
		this.vectorTime.putAll(vectorTime);
	}

	public TextMessage(String message, String sender,
			Map<Integer, Integer> vectorTime) {
		this.message = message;
		this.senderName = sender;
		this.vectorTime = new HashMap<Integer, Integer>();
		this.vectorTime.putAll(vectorTime);
	}

	public TextMessage(JSONObject jsonMessage, int backupLamportTime)
			throws JSONException {
		this.message = jsonMessage.getString("message");

		if (jsonMessage.has("sender")) {
			this.senderName = jsonMessage.getString("sender");
		}
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

		if (jsonMessage.has("sender")) {
			this.senderName = jsonMessage.getString("sender");
		}
		// it's possible that there is no time vector
		if (jsonMessage.has("time_vector")) {
			this.vectorTime = readTimeVector(jsonMessage
					.getJSONObject("time_vector"));
		} else {
			this.vectorTime = new HashMap<Integer, Integer>();
			this.vectorTime.putAll(backupVectorTime);
		}
	}

	public JSONObject getJSONObject() throws JSONException {
		JSONObject object = new JSONObject();
		object.put("sender", senderName);
		object.put("cmd", "message");
		object.put("text", message);
		object.put("time_vector", getVectorTimeJSONObject(this.getVectorTime()));
		return object;
	}

	public String getFormatedMessage() {
		return message;
	}

	public String getFormatedTime() {
		if (LAMPORT_MODE) {
			return "Lamport time: " + this.vectorTime.get(0);
		} else {
			Set<Entry<Integer, Integer>> vectorEntries = vectorTime.entrySet();
			String formatedTime = "Time vector:";
			for (Entry<Integer, Integer> entry : vectorEntries) {
				formatedTime = formatedTime.concat(" " + entry.getKey() + ": "
						+ entry.getValue() + ";");
			}
			return formatedTime;
		}
	}

	public int getLamportTime() {
		if (vectorTime != null) {
			return this.vectorTime.get(0);
		} else {
			return 0;
		}
	}

	public Map<Integer, Integer> getVectorTime() {
		return this.vectorTime;
	}

	public int compareTo(TextMessage another) {
		if (another.vectorTime == null || vectorTime == null) {
			return 0;
		} else if (LAMPORT_MODE) {
			return (vectorTime.get(0) - another.vectorTime.get(0));
		} else {
			// collect time vector differences
			int min = 0;
			int max = 0;
			Set<Entry<Integer, Integer>> current_set = another.vectorTime
					.entrySet();
			for (Entry<Integer, Integer> entry : current_set) {
				if (entry.getKey() != 0) {
					Integer vectorTimeScalar = vectorTime.get(entry.getKey());
					if (vectorTimeScalar != null) {
						int comp = vectorTimeScalar - entry.getValue();
						if (comp < min) {
							min = comp;
						} else if (comp > max) {
							max = comp;
						}
					}
				}
			}
			if (min < 0 && max == 0) {
				return min;
			} else if (max > 0 && min == 0) {
				return max;
			} else {
				return 0;
			}
		}
	}

	// method to determine if a message is on time
	// if in doubt assume on time (to allow more messages to be displayed)
	public boolean isDeliverable(Map<Integer, Integer> currentVectorTime) {

		TextMessage dummyMessage = new TextMessage("", currentVectorTime);
		int comp = this.compareTo(dummyMessage);

		if (comp <= 1) {
			return true;
		} else {
			return false;
		}
	}

	// determine if too late (if unknown, assume message on time)
	public boolean isDelayed(Map<Integer, Integer> currentVectorTime) {
		TextMessage dummyMessage = new TextMessage("", currentVectorTime);
		int comp = this.compareTo(dummyMessage);

		if (comp < -1) {
			return true;
		} else {
			return false;
		}
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
		Map<Integer, Integer> map;
		if (names == null) {
			map = new HashMap<Integer, Integer>(0);
		} else {
			map = new HashMap<Integer, Integer>(names.length());
		}

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
