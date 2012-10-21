package ch.ethz.inf.vs.android.siwehrli.server;

//used for networking
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

//sensor stuff
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

//async task for multithreaded server
import android.os.AsyncTask;

//rest
import android.os.Bundle;
import android.app.Activity;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import java.util.List;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	//used for server
	private static final int SERVER_PORT = 8081;
	private ServerSocket serverSocket;
	private boolean acceptConnections;
	
	//sensor stuff
	private SensorManager sensMng;
	private List<Sensor> sensList;
	private SensorEventListener mySensorListener;
	private String[][] sensorTable;
	
	//used to display log
	Time time;
	TextView outputView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		//initialize log window
		outputView = (TextView) findViewById(R.id.serverText);
		outputView.setMovementMethod(new ScrollingMovementMethod());
		time = new Time();
		
		//initialize sensor stuff
		sensMng = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensList = sensMng.getSensorList(Sensor.TYPE_ALL);
		sensorTable = new String[sensList.size()][6];
		
		mySensorListener = new SensorEventListener() {

			public void onSensorChanged(SensorEvent event) {
				//find place in table
				Sensor sensor = event.sensor;
				int i = -1;
				do {
					i++;
				} while (!sensor.getName().equals(sensorTable[i][0]));
				//set values in table
				sensorTable[i][3] = Float.toString(event.values[0]);
				sensorTable[i][4] = Float.toString(event.values[1]);
				sensorTable[i][5] = Float.toString(event.values[2]);
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		
		//translate sensor type to something human readable
		for (int i = 0; i < sensList.size(); i++) {
			sensMng.registerListener(mySensorListener, sensList.get(i),
					SensorManager.SENSOR_DELAY_NORMAL);
			sensorTable[i][0] = sensList.get(i).getName();
			sensorTable[i][1] = sensList.get(i).getVendor();
			int type = sensList.get(i).getType();
			switch (type) {
			case 1:
				sensorTable[i][2] = "Accelerometer";
				break;
			case 2:
				sensorTable[i][2] = "Magnetic Field Sensor";
				break;
			case 3:
				sensorTable[i][2] = "Orientation Sensor";
				break;
			case 4:
				sensorTable[i][2] = "Gyroscope";
				break;
			case 5:
				sensorTable[i][2] = "Light Sensor";
				break;
			case 6:
				sensorTable[i][2] = "Pressure Sensor";
				break;
			case 7:
				sensorTable[i][2] = "Temperature Sensor";
				break;
			case 8:
				sensorTable[i][2] = "Proximity Sensor";
				break;
			case 9:
				sensorTable[i][2] = "Gravity Sensor";
				break;
			case 10:
				sensorTable[i][2] = "Linear Acceleration Sensor";
				break;
			case 11:
				sensorTable[i][2] = "Rotation Vector Sensor";
				break;
			default:
				sensorTable[i][2] = "No type information available.";
			}
		}

		//set up server socket
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
			acceptConnections = true;
		} catch (IOException e) {
			time.setToNow();
			outputView.append(time.format3339(false)
					+ ": server socket creation failed.\n\n");
		}
		
		//start accepting connections
		AcceptConnectionsTask serverTask = new AcceptConnectionsTask();
		serverTask.execute(serverSocket);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	
	//Async task to accept connections
	class AcceptConnectionsTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected void onPreExecute() {
			time.setToNow();
			publishProgress(new String[] { time.format("%H %M %S")
					+ ": async listening task started.\n\n"});
		}

		@Override
		protected Void doInBackground(ServerSocket... params) {
			//get server socket
			ServerSocket serverSocket = params[0];
			while (acceptConnections) {
				try {
					//get client socket
					Socket clientSocket = serverSocket.accept();
					
					//start async task to serve request
					RequestTask clientTask = new RequestTask();
					clientTask.execute(clientSocket);

				} catch (IOException e) {
					time.setToNow();
					publishProgress(new String[] { time.format("%H %M %S")
							+ ": clientSocket creation failed.\n\n" });
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... params) {
			for (String string : params) {
				//print to log in textview
				outputView.append(string);
			}
		}
	}

	
	//async client task
	class RequestTask extends AsyncTask<Socket, String, Void> {
		@Override
		protected void onPreExecute() {
			time.setToNow();
			publishProgress(new String[] { time.format("%H %M %S")
					+ ": async client task started.\n\n" });
		}

		@Override
		protected Void doInBackground(Socket... params) {
			Socket clientSocket = params[0];
			BufferedWriter writer;
			try {
				//get writer to send html to
				writer = new BufferedWriter(new OutputStreamWriter(
						clientSocket.getOutputStream()));

				time.setToNow();
				publishProgress(new String[] { time.format("%H %M %S")
						+ ": client request received.\n\n" });
				
				//build html
				String html = "<!DOCTYPE html><html><body><h1>Available Sensors:</h1><table border=\"1\" ><tr><td><b>Name</b></td><td><b>Vendor</b></td><td><b>Type</b></td><td><b>Value 1</b></td><td><b>Value 2</b></td><td><b>Value 3</b></td></tr>";
				
				//build sensor table
				for (String[] sensorStrings : sensorTable) {
					html = html.concat("<tr><td>" + sensorStrings[0]
							+ "</td><td>" + sensorStrings[1] + "</td><td>"
							+ sensorStrings[2] + "</td><td>" + sensorStrings[3]
							+ "</td><td>" + sensorStrings[4] + "</td><td>"
							+ sensorStrings[5] + "</td></tr>");
				}
				html = html.concat("</table></body></html>");

				//build and send http package
				writer.write("HTTP/1.1 200 OK\r\n");
				writer.write("Server: bla\r\n");
				writer.write("Content-Length: " + html.length() + "\r\n");
				writer.write("Content-Language: de\r\n");
				writer.write("Connection: close\r\n");
				writer.write("Content-Type: text/html\r\n\r\n");
				writer.write(html + "\r\n");
				writer.flush();
				//done with response

				time.setToNow();
				publishProgress(new String[] { time.format("%H %M %S") + ": "
						+ html + " sent\n\n" });
				
				//close connection
				writer.close();
				clientSocket.close();

			} catch (IOException e) {
				time.setToNow();
				publishProgress(new String[] { time.format("%H %M %S")
						+ ": reading from clientSocket failed.\n\n" });
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
