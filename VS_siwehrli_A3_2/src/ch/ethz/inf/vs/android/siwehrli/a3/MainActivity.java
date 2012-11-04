package ch.ethz.inf.vs.android.siwehrli.a3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	private static final String LOG_TAG = "Chat";
	private static final String HOST_NAME = "vslab.inf.ethz.ch";
	private static final int REGISTER_LOCALHOST_PORT = 4000;
	private static final int REGISTER_SERVER_PORT = 4000;
	private static final int CHAT_LOCALHOST_PORT = 4001;
	private static final int CHAT_SERVER_PORT = 4001;
	private static final int TOAST_DURATION = Toast.LENGTH_SHORT;
	private static final int PACKET_SIZE = 1024;
	private static final int REGISTRATION_TIMEOUT = 5000;
	private static final String SETTINGS_NAME = "Settings";
	private MyArrayAdapter adapter;

	// data structure for holding messages
	PriorityBlockingQueue<TextMessage> messages = new PriorityBlockingQueue<TextMessage>();

	private boolean registered = false;
	private String userName =""; // is saved if app is stopped by OS
	private int index = 0;
	private Map<Integer, Integer> initialTimeVector = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ListView listView = (ListView) findViewById(R.id.listViewMessages);
		TextMessage[] values = new TextMessage[] {
				new TextMessage("Hello", 54), new TextMessage("Bye", 545) };

		// Assign adapter to ListView
		adapter = new MyArrayAdapter(this, values);
		listView.setAdapter(adapter);

		// read settings into private fields
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
				MODE_PRIVATE);
		this.userName = settings.getString("user_name", "");
		EditText editName = (EditText) findViewById(R.id.editName);
		editName.setText(this.userName);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (registered)
			this.deregister();
	}

	public void onClickRegister(View view) {
		ToggleButton tb = ((ToggleButton) view);

		if (tb.isChecked()) {
			EditText editName = (EditText) findViewById(R.id.editName);
			this.userName = editName.getText().toString();
			if (this.userName.equals("")) {
				Toast.makeText(this, R.string.name_empty, TOAST_DURATION)
						.show();
			} else {
				if (this.register()) {
					Toast.makeText(this, R.string.register_ok, TOAST_DURATION)
							.show();
				} else {
					Toast.makeText(this, R.string.register_failed,
							TOAST_DURATION).show();
				}
			}

		} else {
			if (this.deregister()) {
				Toast.makeText(this, R.string.deregister_ok, TOAST_DURATION)
						.show();
			} else {
				Toast.makeText(this, R.string.deregister_failed, TOAST_DURATION)
						.show();
			}
		}
		
		tb.setChecked(this.registered);
	}

	public void onClickSend(View view) {
		if (registered) {
			EditText editMessage = (EditText) findViewById(R.id.editMessage);
			String message = editMessage.getText().toString();
			if (!message.equals("")) {
				if (!this.sendMessage(message)) {
					Toast.makeText(this, R.string.message_sending_failed,
							TOAST_DURATION).show();
				}
			} else {
				Toast.makeText(this, R.string.message_empty, TOAST_DURATION)
						.show();
			}
		} else {
			Toast.makeText(this, R.string.register_needed, TOAST_DURATION)
					.show();
		}
	}

	private boolean sendMessage(String message) {
		Log.d(LOG_TAG, "Send message: " + message);
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(CHAT_LOCALHOST_PORT);

			InetAddress to = InetAddress.getByName(HOST_NAME);

			// build JSON
			JSONObject object = new JSONObject();
			object.put("cmd", "message");
			object.put("text", message);
			String request = object.toString();
			Log.d(LOG_TAG, "Sending: " + request);

			// send packet
			byte[] data = request.getBytes();
			DatagramPacket packet = new DatagramPacket(data, data.length, to,
					CHAT_SERVER_PORT);
			socket.send(packet);

			// only add message to view if sent to the server successful
			this.addMessage(new TextMessage(message, initialTimeVector.get(0)));
			return true;
		} catch (SocketException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage());
		}

		return false;
	}

	private boolean register() {
		Log.d(LOG_TAG, "Register user with name: " + userName);
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(REGISTER_LOCALHOST_PORT);

			InetAddress to = InetAddress.getByName(HOST_NAME);
			String request = createRequest_register(userName);
			Log.d(LOG_TAG, "Request: " + request);

			byte[] data = request.getBytes();

			DatagramPacket packet = new DatagramPacket(data, data.length, to,
					REGISTER_SERVER_PORT);

			socket.send(packet);

			// Receive
			data = new byte[PACKET_SIZE];
			DatagramPacket pack = new DatagramPacket(data, PACKET_SIZE);
			socket.setSoTimeout(REGISTRATION_TIMEOUT);
			socket.receive(pack);

			String answer = new String(pack.getData(), 0, pack.getLength());
			Log.d(LOG_TAG, "Received message: " + answer);

			JSONObject jsonAnswer = new JSONObject(answer);
			String success = jsonAnswer.getString("success");
			if (success.equals("reg_ok")) {
				// used only in Task 3
				index = Integer.parseInt(jsonAnswer.getString("index"));
				initialTimeVector = this.readTimeVector(jsonAnswer);
				socket.close();
				this.registered=true;
			} else {
				socket.close();
				this.registered=false;
			}
 
		} catch (SocketException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage());
		}

		return this.registered;
	}

	private boolean deregister() {
		Log.d(LOG_TAG, "Deregister user with name: " + userName);
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(REGISTER_LOCALHOST_PORT);

			InetAddress to = InetAddress.getByName(HOST_NAME);
			String request = createRequest_deregister(userName);
			Log.d(LOG_TAG, "Request: " + request);

			byte[] data = request.getBytes();

			DatagramPacket packet = new DatagramPacket(data, data.length, to,
					REGISTER_SERVER_PORT);

			socket.send(packet);

			// Receive
			data = new byte[PACKET_SIZE];
			DatagramPacket pack = new DatagramPacket(data, PACKET_SIZE);
			socket.setSoTimeout(REGISTRATION_TIMEOUT);
			socket.receive(pack);

			String answer = new String(pack.getData(), 0, pack.getLength());
			Log.d(LOG_TAG, "Received message: " + answer);

			JSONObject jsonAnswer = new JSONObject(answer);
			String success = jsonAnswer.getString("success");
			if (success.equals("dreg_ok")) {
				socket.close();
				this.registered=false;
			} else {
				socket.close();
				this.registered=true;
			}

		} catch (SocketException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
		} catch (JSONException e) {
			Log.e(LOG_TAG, e.getMessage());
		}

		return !this.registered;
	}

	private static String createRequest_register(String userName)
			throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "register");
		object.put("user", userName);

		return object.toString();
	}

	private static String createRequest_deregister(String userName)
			throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "deregister");

		return object.toString();
	}

	private static String createRequest_info() throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "info");

		return object.toString();
	}

	public static Map<Integer, Integer> readTimeVector(JSONObject o)
			throws JSONException {
		JSONObject jsonTimeVector = o.getJSONObject("time_vector");
		JSONArray names = jsonTimeVector.names();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>(
				names.length());

		for (int i = 0; i < names.length(); ++i) {
			map.put(names.getInt(i), jsonTimeVector.getInt(names.getString(i)));
		}
		return map;
	}

	private void addMessage(TextMessage message) {
		this.messages.add(message);
		this.adapter.clear();
		for (TextMessage m : messages)
			this.adapter.add(m);
		this.adapter.notifyDataSetChanged();
	}

	// private class ReceiveMessagesTask extends AsyncTask<String, Void,
	// TextMessage> {
	// /** The system calls this to perform work in a worker thread and
	// * delivers it the parameters given to AsyncTask.execute() */
	// protected Bitmap doInBackground(String... urls) {
	// return loadImageFromNetwork(urls[0]);
	// }
	//
	// /** The system calls this to perform work in the UI thread and delivers
	// * the result from doInBackground() */
	// protected void onPostExecute(Bitmap result) {
	// mImageView.setImageBitmap(result);
	// }
	// }
}
