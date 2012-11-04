package ch.ethz.inf.vs.android.siwehrli.a3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
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
	private static final int LOCALHOST_PORT = 4000;
	private static final int SERVER_PORT = 4000;
	private static final int TOAST_DURATION = Toast.LENGTH_SHORT;
	private static final int PACKET_SIZE = 1000;
	private static final int REGISTRATION_TIMEOUT = 5000;
	private MyArrayAdapter adapter;
	private boolean registered = false;
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClickRegister(View view) {
		registered = ((ToggleButton) view).isChecked();

		if (registered) {
			EditText editName = (EditText) findViewById(R.id.editName);
			if (this.register(editName.getText().toString()))
				Toast.makeText(this, R.string.register_ok, TOAST_DURATION)
						.show();
			else
				Toast.makeText(this, R.string.register_failed, TOAST_DURATION)
						.show();

		} else {

		}
	}

	private boolean register(String userName) {
		Log.d(LOG_TAG, "Register user under name: " + userName);
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(LOCALHOST_PORT);

			InetAddress to = InetAddress.getByName(HOST_NAME);
			String request = createRequest_register(userName);
			Log.d(LOG_TAG, "Request: " + request);

			byte[] data = request.getBytes();

			DatagramPacket packet = new DatagramPacket(data, data.length, to,
					SERVER_PORT);

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

		return false;
	}

	private String createRequest_register(String userName) throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "register");
		object.put("user", userName);

		return object.toString();
	}

	private String createRequest_deregister(String userName)
			throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "deregister");

		return object.toString();
	}

	private String createRequest_info() throws JSONException {
		JSONObject object = new JSONObject();

		object.put("cmd", "info");

		return object.toString();
	}

	private Map<Integer, Integer> readTimeVector(JSONObject o)
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
}
