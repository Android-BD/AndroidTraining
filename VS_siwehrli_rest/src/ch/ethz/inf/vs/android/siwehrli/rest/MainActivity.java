package ch.ethz.inf.vs.android.siwehrli.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	static String host = "vslab.inf.ethz.ch";
	static String charset = "UTF8";
	static int port = 8081;
	String path = "/sunspots/Spot1/sensors/temperature";

	public void onClickStartChartActivity(View view) {
		Intent intent = new Intent(this, ChartActivity.class);
    	startActivity(intent);
	}

	public void onClickRawRequest(View view) {
		Socket requestSocket;
		BufferedWriter writer;
		BufferedReader reader;

		try {
			// 1. creating a socket to connect to the server
			requestSocket = new Socket(host, port);
			Log.d("http", "Connected to host " + host + " on port " + port);

			// 2. get Input and Output streams
			writer = new BufferedWriter(new OutputStreamWriter(
					requestSocket.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(
					requestSocket.getInputStream()));

			Log.d("http", "Reader/Writer created");

			// 3: Communicating with the server
			writer.write("GET " + path + " HTTP/1.1\r\n");
			writer.write("Host: " + host + "\r\n");
			writer.write("Accept: text/html\r\n");
			writer.write("Connection: close\r\n");
			writer.write("\r\n");
			writer.flush();

			// Get response
			String line;
			EditText editText = (EditText) findViewById(R.id.editTextShow);
			editText.setText("");
			while ((line = reader.readLine()) != null) {
				editText.append(line);
			}

			this.showValue(null);

			// 4: Closing connection
			writer.close();
			reader.close();
			requestSocket.close();
		} catch (UnknownHostException unknownHost) {
			Log.d("http", "You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			Log.d("http", "Input/Output failed!");
		} finally {

		}
	}

	public void onClickLibraryRequest(View view) {
		DefaultHttpClient client;
		HttpResponse response;
		HttpGet httpget;

		BufferedReader reader;

		URI uri;
		try {
			uri = org.apache.http.client.utils.URIUtils.createURI("http", host,
					port, path, null, null);

			httpget = new HttpGet(uri);
			httpget.addHeader("Accept", "text/html");
			httpget.addHeader("Connection", "close");

			client = new DefaultHttpClient();
			response = client.execute(httpget);

			HttpEntity entity = response.getEntity();
			reader = new BufferedReader(new InputStreamReader(
					entity.getContent()));

			// read response
			String line;
			EditText editText = (EditText) findViewById(R.id.editTextShow);
			editText.setText("");
			while ((line = reader.readLine()) != null) {
				editText.append(line);
			}

			this.showValue(null);
		} catch (URISyntaxException e) {
			Log.d("http", "Malformed URI!");
		} catch (ClientProtocolException e) {
			Log.d("http", "Client protocol Exception!");
		} catch (IOException e) {
			Log.d("http", "I/O Exception!");
		}

	}

	public void onClickJsonRequest(View view) {
		DefaultHttpClient client;
		HttpResponse response;
		HttpGet httpget;

		BufferedReader reader;

		URI uri;
		try {
			uri = org.apache.http.client.utils.URIUtils.createURI("http", host,
					port, path, null, null);

			httpget = new HttpGet(uri);
			httpget.addHeader("Accept", "application/json");
			httpget.addHeader("Connection", "close");

			client = new DefaultHttpClient();
			response = client.execute(httpget);

			HttpEntity entity = response.getEntity();
			reader = new BufferedReader(new InputStreamReader(
					entity.getContent()));

			// read response
			StringBuilder sb = new StringBuilder();
			String line;
			EditText editText = (EditText) findViewById(R.id.editTextShow);
			editText.setText("");
			while ((line = reader.readLine()) != null) {
				editText.append(line);
				sb.append(line);
			}

			JSONObject jsonData = new JSONObject(sb.toString());
			String value = jsonData.getString("value");

			this.showValue(value);
		} catch (URISyntaxException e) {
			Log.d("http", "Malformed URI!");
		} catch (ClientProtocolException e) {
			Log.d("http", "Client protocol Exception!");
		} catch (IOException e) {
			Log.d("http", "I/O Exception!");
		} catch (JSONException e) {
			Log.d("http", "JSON Exception! " + e.getMessage());
		}
	}

	private void showValue(String value) {
		TextView textView = (TextView) findViewById(R.id.textViewValue);

		if (value == null)
			textView.setText(getResources().getString(R.string.no_value_set));
		else
			textView.setText(value);
	}
}
