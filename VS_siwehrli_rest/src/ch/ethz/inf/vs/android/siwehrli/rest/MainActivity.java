package ch.ethz.inf.vs.android.siwehrli.rest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

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

	String host = "vslab.inf.ethz.ch";
	String charset = "UTF8";
	int port = 8081;
	String path = "/sunspots/Spot1/sensors/temperature";

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
			writer.write("Host: "+host+"\r\n");
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

	}

	public void onClickJsonRequest(View view) {

	}
}
