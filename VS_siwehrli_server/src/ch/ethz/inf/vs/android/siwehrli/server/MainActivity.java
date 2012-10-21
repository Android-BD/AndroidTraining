package ch.ethz.inf.vs.android.siwehrli.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.format.Time;
import android.view.Menu;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.protocol.HttpContext;


public class MainActivity extends Activity {

	private static final int SERVER_PORT = 8081;
	private ServerSocket serverSocket;
	private boolean acceptConnections;

	Time time;
	TextView outputView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		outputView = (TextView) findViewById(R.id.serverText);
		time = new Time();

		try {
			serverSocket = new ServerSocket(SERVER_PORT);
			acceptConnections = true;
		} catch (IOException e) {
			time.setToNow();
			outputView.append(time.format3339(false)
					+ ": server socket creation failed.\n\n");
		}
		AcceptConnectionsTask serverTask = new AcceptConnectionsTask();
		serverTask.execute(serverSocket);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	class AcceptConnectionsTask extends AsyncTask<ServerSocket, String, Void> {

		private ArrayList<String> notifications;

		@Override
		protected void onPreExecute() {
			notifications = new ArrayList<String>();
			time.setToNow();
			notifications.add(time.format("%H %M %S")
					+ ": async listening task started.\n\n");
			publishProgress(notifications.toArray(new String[notifications
					.size()]));
		}

		@Override
		protected Void doInBackground(ServerSocket... params) {
			ServerSocket serverSocket = params[0];
			while (acceptConnections) {
				try {
					Socket clientSocket = serverSocket.accept();
					RequestTask clientTask = new RequestTask();
					clientTask.execute(clientSocket);

				} catch (IOException e) {
					time.setToNow();
					notifications.add(time.format("%H %M %S")
							+ ": clientSocket creation failed.\n\n");
					publishProgress(notifications
							.toArray(new String[notifications.size()]));
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			for (String string : params) {
				outputView.append(string);
			}
		}
	}

	class RequestTask extends AsyncTask<Socket, String, Void> {

		private ArrayList<String> notifications;

		@Override
		protected void onPreExecute() {
			notifications = new ArrayList<String>();
			time.setToNow();
			notifications.add(time.format("%H %M %S")
					+ ": async client task started.\n\n");
			publishProgress(notifications.toArray(new String[notifications
					.size()]));
		}

		@Override
		protected Void doInBackground(Socket... params) {
			Socket clientSocket = params[0];
			BufferedReader reader;
			BufferedWriter writer;
			try {
				reader = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(
						clientSocket.getOutputStream()));
				
				time.setToNow();
				notifications.add(time.format("%H %M %S") + ": "
						+ reader.readLine() + "\n\n");
				publishProgress(notifications.toArray(new String[notifications
						.size()]));
				
				String html = "<!DOCTYPE html><html><body><h1>My First Heading</h1><p>My first paragraph.</p></body></html>";
				
				//send response
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Server: bla\r\n");
				writer.write("Content-Length: " + html.length() + "\r\n");
				writer.write("Content-Language: de\r\n");
				writer.write("Connection: close\r\n");
				writer.write("Content-Type: text/html\r\n");
				writer.write(html+"\r\n");
				writer.flush();
				// done with response
				
				time.setToNow();
				notifications.add(time.format("%H %M %S") + ": "
						+ html + " sent\n\n");
				publishProgress(notifications.toArray(new String[notifications
						.size()]));
				writer.close();
				reader.close();
				clientSocket.close();
				
			} catch (IOException e) {
				time.setToNow();
				notifications.add(time.format("%H %M %S")
						+ ": reading from clientSocket failed.\n\n");
				publishProgress(notifications.toArray(new String[notifications
						.size()]));
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			for (String string : params) {
				outputView.append(string);
			}
		}
	}
}
