package ch.ethz.inf.vs.android.siwehrli.a3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
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
	ArrayList<TextMessage> messages = new ArrayList<TextMessage>();
	PriorityBlockingQueue<TextMessage> messagesPrearrived = new PriorityBlockingQueue<TextMessage>();

	private boolean registered = false;
	private String userName = ""; // is saved if app is stopped by OS
	private int index = 0;
	private Map<Integer, Integer> initialTimeVector = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ListView listView = (ListView) findViewById(R.id.listViewMessages);
		messages.add(new TextMessage("Hello testmessage", 54));

		// Assign adapter to ListView
		adapter = new MyArrayAdapter(this, messages);
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
		if (registered) {
			RegisterTask task = new RegisterTask(userName, false);
			task.execute();

			// save settings
			SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
					MODE_PRIVATE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("user_name", this.userName);
			editor.commit(); // Commit changes to file!!!
		}
	}

	public void onClickRegister(View view) {
		ToggleButton tb = ((ToggleButton) view);

		EditText editName = (EditText) findViewById(R.id.editName);
		this.userName = editName.getText().toString();

		RegisterTask task = new RegisterTask(userName, tb.isChecked());
		task.execute();
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
			socket.close();

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
		this.adapter.notifyDataSetChanged();
	}

	private class RegisterTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		String userName;
		int toastID = R.string.hello_world;
		private boolean registering;

		public RegisterTask(String userName, boolean register) {
			this.userName = userName;
			this.registering = register;
		}

		@Override
		protected void onPreExecute() {
			if (registering)
				progressDialog = ProgressDialog.show(MainActivity.this, "",
						getResources().getString(R.string.dialog_register));
			else
				progressDialog = ProgressDialog.show(MainActivity.this, "",
						getResources().getString(R.string.dialog_deregister));
		}

		@Override
		/**
		 * returns if user is registered after background operation has completed
		 */
		protected Boolean doInBackground(Void... args) {
			if (registering) {

				if (this.userName.equals("")) {
					this.toastID = R.string.name_empty;
					return false;
				} else {
					if (this.register()) {
						this.toastID = R.string.register_ok;
						return true;
					} else {
						this.toastID = R.string.register_failed;
						return false;
					}
				}

			} else {
				if (this.deregister()) {
					this.toastID = R.string.deregister_ok;
					return false;
				} else {
					this.toastID = R.string.deregister_failed;
					return true;
				}
			}
		}

		/**
		 * returns if user is registered after operation has completed
		 */
		private boolean register() {
			Log.d(LOG_TAG, "Register user with name: " + userName);
			DatagramSocket socket = null;
			try {
				socket = new DatagramSocket(REGISTER_LOCALHOST_PORT);

				InetAddress to = InetAddress.getByName(HOST_NAME);
				String request = createRequest_register(userName);
				Log.d(LOG_TAG, "Request: " + request);

				byte[] data = request.getBytes();

				DatagramPacket packet = new DatagramPacket(data, data.length,
						to, REGISTER_SERVER_PORT);

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
					initialTimeVector = readTimeVector(jsonAnswer);
					socket.close();
					return true;
				} else {
					socket.close();
					return false;
				}

			} catch (SocketException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (JSONException e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			if (socket != null)
				socket.close();

			return false;
		}

		/**
		 * returns if user is registered after operation has completed
		 */
		private boolean deregister() {
			Log.d(LOG_TAG, "Deregister user with name: " + userName);
			DatagramSocket socket = null;
			try {
				socket = new DatagramSocket(REGISTER_LOCALHOST_PORT);

				InetAddress to = InetAddress.getByName(HOST_NAME);
				String request = createRequest_deregister(userName);
				Log.d(LOG_TAG, "Request: " + request);

				byte[] data = request.getBytes();

				DatagramPacket packet = new DatagramPacket(data, data.length,
						to, REGISTER_SERVER_PORT);

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
					return false;
				} else {
					socket.close();
					return true;
				}

			} catch (SocketException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (JSONException e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			if (socket != null)
				socket.close();

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			registered = result;

			ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButtonRegister);
			tb.setChecked(result);

			Toast.makeText(getApplicationContext(), this.toastID,
					TOAST_DURATION).show();

			progressDialog.dismiss();

		}
	}
}
