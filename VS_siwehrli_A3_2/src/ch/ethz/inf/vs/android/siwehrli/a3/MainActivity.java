package ch.ethz.inf.vs.android.siwehrli.a3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
	private static final int REGISTER_PORT = 4000;
	private static final int CHAT_PORT = 4001;
	private static final int TOAST_DURATION = Toast.LENGTH_SHORT;
	private static final int PACKET_SIZE = 1024;
	private static final int REGISTRATION_TIMEOUT = 10000;
	private static final int MESSAGE_RECEIVE_TIMEOUT = 5000;
	private static final int MESSAGE_DELIVERY_TIMEOUT = 5000;

	// vector time/lamport time switch
	private static final boolean LAMPORT_MODE = true;

	private static final String SETTINGS_NAME = "Settings";
	private MyArrayAdapter adapter;
	private ReceiveTask receiveTask;
	private boolean running = false;

	/**
	 * On the handler it's possible to post Runnables to be executed by the GUI
	 * thread
	 */
	private Handler handler;

	// data structure for holding messages
	ArrayList<TextMessage> messages = new ArrayList<TextMessage>();
	PriorityBlockingQueue<TextMessage> waitingMessages = new PriorityBlockingQueue<TextMessage>();

	private boolean registered = false;
	private String userName = ""; // is saved if app is stopped by OS
	private int index;
	private Map<Integer, Integer> currentVectorTime = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ListView listView = (ListView) findViewById(R.id.listViewMessages);

		// Assign adapter to ListView
		adapter = new MyArrayAdapter(this, messages);
		listView.setAdapter(adapter);

		// read settings into private fields
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
				MODE_PRIVATE);
		this.userName = settings.getString("user_name", "");
		EditText editName = (EditText) findViewById(R.id.editName);
		editName.setText(this.userName);

		// initial handler with this thread (GUI thread!)
		handler = new Handler();
		running = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// end the other threads
		running = false;
		super.onDestroy();

		// app is exiting, if still registered -> unregister now
		if (registered) {
			RegisterTask task = new RegisterTask(userName, false);
			task.execute();
		}

		// save settings
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("user_name", this.userName);
		editor.commit(); // Commit changes to file!!!
	}

	public void onClickRegister(View view) {
		// toggle button click
		ToggleButton tb = ((ToggleButton) view);

		// save name
		EditText editName = (EditText) findViewById(R.id.editName);
		this.userName = editName.getText().toString();

		// start async task to register
		RegisterTask task = new RegisterTask(userName, tb.isChecked());
		task.execute();
	}

	public void onClickSend(View view) {
		// send method
		// check if registered
		if (registered) {
			EditText editMessage = (EditText) findViewById(R.id.editMessage);
			String message = editMessage.getText().toString();

			// check if there is text
			if (!message.equals("")) {
				// start async task to send message
				SendTask task = new SendTask();
				task.execute(message);
			} else {
				// no text info toast
				Toast.makeText(this, R.string.message_empty, TOAST_DURATION)
						.show();
			}
		} else {
			// register first info toast
			Toast.makeText(this, R.string.register_needed, TOAST_DURATION)
					.show();
		}
	}

	// create register request
	private static String createRequest_register(String userName)
			throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "register");
		object.put("user", userName);

		return object.toString();
	}

	// create deregister request
	private static String createRequest_deregister(String userName)
			throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "deregister");

		return object.toString();
	}

	// create info request (actually never used)
	private static String createRequest_info() throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "info");

		return object.toString();
	}

	// async task which handles registering and deregistering
	private class RegisterTask extends AsyncTask<Void, Void, Void> {
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
			// show busy text according to current action
			if (running) {
				if (registering)
					progressDialog = ProgressDialog.show(MainActivity.this, "",
							getResources().getString(R.string.dialog_register));
				else
					progressDialog = ProgressDialog.show(MainActivity.this, "",
							getResources()
									.getString(R.string.dialog_deregister));
			}

		}

		@Override
		/**
		 * returns if user is registered after background operation has completed
		 */
		protected Void doInBackground(Void... args) {
			if (registering) {
				if (this.userName.equals("")) {
					this.toastID = R.string.name_empty;
				} else {
					register();
					if (registered) {
						this.toastID = R.string.register_ok;
					} else {
						this.toastID = R.string.register_failed;
					}
				}

			} else {
				deregister();
				if (registered) {
					this.toastID = R.string.deregister_failed;
				} else {
					this.toastID = R.string.deregister_ok;
				}
			}
			return null;
		}

		/**
		 * returns if user is registered after operation has completed
		 */
		private void register() {
			Log.d(LOG_TAG, "Register user with name: " + userName);

			DatagramSocket socket = null;
			try {
				// create reusable socket
				DatagramChannel channel = DatagramChannel.open();
				socket = channel.socket();
				socket.setReuseAddress(true);
				InetSocketAddress addr = new InetSocketAddress(REGISTER_PORT);
				socket.bind(addr);
				socket.setSoTimeout(REGISTRATION_TIMEOUT);

				InetAddress to = InetAddress.getByName(HOST_NAME);
				String request = createRequest_register(userName);
				Log.d(LOG_TAG, "Request: " + request);

				byte[] data = request.getBytes();

				DatagramPacket packet = new DatagramPacket(data, data.length,
						to, REGISTER_PORT);

				socket.send(packet);

				// Receive
				data = new byte[PACKET_SIZE];
				DatagramPacket pack = new DatagramPacket(data, PACKET_SIZE);
				socket.receive(pack);

				String answer = new String(pack.getData(), 0, pack.getLength());
				Log.d(LOG_TAG, "Received message: " + answer);

				JSONObject jsonAnswer = new JSONObject(answer);
				String success = jsonAnswer.getString("success");
				if (success.equals("reg_ok")) {
					index = Integer.parseInt(jsonAnswer.getString("index"));
					currentVectorTime = TextMessage.readTimeVector(jsonAnswer
							.getJSONObject("time_vector"));
					socket.close();
					registered = true;
				} else {
					socket.close();
				}
			} catch (SocketTimeoutException e) {
				Log.d(LOG_TAG, "timeout");
			} catch (SocketException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (JSONException e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			if (socket != null) {
				socket.close();
			}
		}

		/**
		 * returns if user is registered after operation has completed
		 */
		private void deregister() {
			Log.d(LOG_TAG, "Deregister user with name: " + userName);
			DatagramSocket socket = null;
			try {
				// create reusable socket
				DatagramChannel channel = DatagramChannel.open();
				socket = channel.socket();
				socket.setReuseAddress(true);
				InetSocketAddress addr = new InetSocketAddress(REGISTER_PORT);
				socket.bind(addr);

				InetAddress to = InetAddress.getByName(HOST_NAME);
				String request = createRequest_deregister(userName);
				Log.d(LOG_TAG, "Request: " + request);

				byte[] data = request.getBytes();

				DatagramPacket packet = new DatagramPacket(data, data.length,
						to, REGISTER_PORT);

				socket.send(packet);

				// Receive
				data = new byte[PACKET_SIZE];
				DatagramPacket pack = new DatagramPacket(data, PACKET_SIZE);
				socket.setSoTimeout(REGISTRATION_TIMEOUT);
				socket.receive(pack);

				String answer = new String(pack.getData(), 0, pack.getLength());
				Log.d(LOG_TAG, "Received message: " + answer);

				JSONObject jsonAnswer = new JSONObject(answer);
				if (jsonAnswer.has("success")) {
					String success = jsonAnswer.getString("success");
					if (success.equals("dreg_ok")) {
						socket.close();
						registered = false;
					} else {
						socket.close();
					}
				} else if (jsonAnswer.has("error")) {
					// the only error that can happen is "not registered"
					// which is the state we are trying to reach, so no error
					// info to the user
					socket.close();
					registered = false;
				}
			} catch (SocketTimeoutException e) {
				Log.d(LOG_TAG, "timeout");
			} catch (SocketException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			} catch (JSONException e) {
				Log.e(LOG_TAG, e.getMessage());
			}

			if (socket != null) {
				socket.close();
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			// (un)lock username textfield and set focus
			EditText editName = (EditText) findViewById(R.id.editName);
			editName.setEnabled(!registered);
			if (registered) {
				EditText editMessage = (EditText) findViewById(R.id.editMessage);
				editMessage.requestFocus();
			}

			// set togglebutton status
			ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButtonRegister);
			tb.setChecked(registered);

			Toast.makeText(getApplicationContext(), this.toastID,
					TOAST_DURATION).show();

			// start listening for messages
			if (registered) {
				// start listening
				if (receiveTask == null || !receiveTask.isAlive()) {
					receiveTask = new ReceiveTask();
					receiveTask.start();
				}
			}
			if (progressDialog != null) {
				progressDialog.dismiss();
			}

		}
	}

	private class SendTask extends AsyncTask<String, Void, Boolean> {

		@Override
		/**
		 * return if message successfully sent
		 */
		protected Boolean doInBackground(String... args) {
			// Time logic
			TextMessage message;
			synchronized (currentVectorTime) {
				// increase lampart time
				currentVectorTime.put(0, currentVectorTime.get(0) + 1);
				// increase own index time
				currentVectorTime.put(index, currentVectorTime.get(index) + 1);
				message = new TextMessage(args[0], userName, currentVectorTime);
			}
			Log.d(LOG_TAG, "Send message: " + message.getFormatedMessage());
			// sending message
			DatagramSocket socket = null;
			try {
				// create reusable socket
				DatagramChannel channel = DatagramChannel.open();
				socket = channel.socket();
				socket.setReuseAddress(true);
				InetSocketAddress addr = new InetSocketAddress(REGISTER_PORT);
				socket.bind(addr);
				socket.setSoTimeout(REGISTRATION_TIMEOUT);

				InetAddress to = InetAddress.getByName(HOST_NAME);

				// build JSON
				JSONObject jsonMessage = message.getJSONObject();
				String request = jsonMessage.toString();
				Log.d(LOG_TAG, "Sending: " + request);

				// send packet
				byte[] data = request.getBytes();
				DatagramPacket packet = new DatagramPacket(data, data.length,
						to, REGISTER_PORT);
				socket.send(packet);

				// only add message to view if sent to the server successful
				messages.add(message);
				publishProgress();
				socket.close();
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

		@Override
		protected void onProgressUpdate(Void... values) {
			adapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (!result)
				Toast.makeText(MainActivity.this,
						R.string.message_sending_failed, TOAST_DURATION).show();
			else {
				// clear message textview
				EditText editMessage = (EditText) findViewById(R.id.editMessage);
				editMessage.setText("");
			}
		}
	}

	private class ReceiveTask extends Thread {

		@Override
		public void run() {
			Log.d(LOG_TAG, "Start receiving messages");

			// receiving messages
			// Receive
			byte[] data;
			DatagramPacket pack;
			DatagramSocket socket = null;
			while (registered && running) {
				try {
					// create reusable socket
					DatagramChannel channel = DatagramChannel.open();
					socket = channel.socket();
					socket.setReuseAddress(true);
					InetSocketAddress addr = new InetSocketAddress(CHAT_PORT);
					socket.bind(addr);
					socket.setSoTimeout(MESSAGE_RECEIVE_TIMEOUT);

					data = new byte[PACKET_SIZE];
					pack = new DatagramPacket(data, PACKET_SIZE);

					socket.receive(pack);

					String answer = new String(pack.getData(), 0,
							pack.getLength());
					Log.d(LOG_TAG, "Received message: " + answer);

					// parse
					TextMessage message = new TextMessage(
							new JSONObject(answer), currentVectorTime);

					// use compareTo of message class via dummy message
					final TextMessage compareMessage = new TextMessage("",
							currentVectorTime);

					// compare to current time
					int comp = message.compareTo(compareMessage);
					if (comp < -1) {
						// delayed message, deliver flagged as delayed
						message.setDelayedPublished();
						Log.d(LOG_TAG,"setdelayPublished set");
						Log.d(LOG_TAG,"current "+compareMessage.getFormatedTime());
						Log.d(LOG_TAG,"message "+message.getFormatedTime());

						deliverMessage(message);
					} else if (comp <= 1) {
						// normal message, just deliver
						deliverMessage(message);
					} else {
						// message early (some messages may be missing)
						// wait for timeout to pass before delivery
						waitingMessages.put(message);
						new TimeoutThread().start();
					}

				} catch (SocketTimeoutException e) {
				} catch (SocketException e) {
					Log.e(LOG_TAG, e.getMessage());
				} catch (IOException e) {
					Log.e(LOG_TAG, e.getMessage());
				} catch (JSONException e) {
					Log.e(LOG_TAG, e.getMessage());
				}

			}
			if (socket != null) {
				socket.close();
			}
			Log.d(LOG_TAG, "Stop receiving messages");
		}
	}

	private class TimeoutThread extends Thread {

		@Override
		public void run() {
			try {
				sleep(MESSAGE_DELIVERY_TIMEOUT);
				Log.d(LOG_TAG, "TimeoutThread woke up normally");
			} catch (InterruptedException e) {
				Log.d(LOG_TAG, e.getMessage());
				Log.d(LOG_TAG, "TimeoutThread interrupted");
			}
			if (running) {
				TextMessage message = waitingMessages.poll();
				if (message != null) {
					// just deliver the first message found
					// this is enough since for every message in waitingMessages
					// a new timeout thread is started
					deliverMessage(message);
				}
			}

		}
	}

	// message deliver (including notifying gui thread and handle time updates)
	private void deliverMessage(final TextMessage message) {
		int comp;
		// update time if necessary
		synchronized (currentVectorTime) {
			final TextMessage compareMessage = new TextMessage("",
					currentVectorTime);
			comp = message.compareTo(compareMessage);
			if (LAMPORT_MODE && comp > 0) {
				currentVectorTime = message.getVectorTime();
			}
			// time vector is only updated when in vector time mode
			else if (comp >= 0) {
				// update existing time vector
				Set<Entry<Integer, Integer>> vectorEntries = message
						.getVectorTime().entrySet();
				for (Entry<Integer, Integer> entry : vectorEntries) {
					if (currentVectorTime.containsKey(entry.getKey())
							&& entry.getValue() > currentVectorTime.get(entry
									.getKey())) {
						currentVectorTime.put(entry.getKey(), entry.getValue());
						Log.d(LOG_TAG, "updated " + entry.getKey() + " to "
								+ entry.getValue());
					}
				}

				// update time vector (eg remove entries of people
				// that left, and add new people)
				// only do that with server info messages, so that no
				// evil user can force our timevector to grow or shrink
				if (message.getSenderName().equals("Server")) {
					try {
						if (message.getFormatedMessage().contains("has left")) {
							String parse_index = message.getFormatedMessage()
									.replaceAll("[\\w\\s]+\\(index\\s", "");
							int rem_index = Integer.parseInt(parse_index
									.substring(0, parse_index.length() - 1));
							currentVectorTime.remove(rem_index);
						} else if (message.getFormatedMessage().contains(
								"has joined")) {
							String parse_index = message.getFormatedMessage()
									.replaceAll("[\\w\\s]+\\(index\\s", "");
							int add_index = Integer.parseInt(parse_index
									.substring(0, parse_index.length() - 1));
							currentVectorTime.put(add_index, message
									.getVectorTime().get(add_index));
						}
					} catch (NumberFormatException e) {
						Log.d(LOG_TAG, e.getMessage());
					}
				}

			}
		}
		synchronized (messages) {
			messages.add(message);
			Collections.sort(messages);
		}

		if (running) {
			// notify gui
			handler.post(new Runnable() {
				public void run() {
					if (message.isDelayedPublished()) {
						Toast.makeText(getApplicationContext(),
								R.string.delayed_message, Toast.LENGTH_SHORT)
								.show();
					}
					adapter.notifyDataSetChanged();
				}
			});
		}
	}
}
