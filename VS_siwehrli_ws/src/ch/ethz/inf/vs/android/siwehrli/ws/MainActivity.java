package ch.ethz.inf.vs.android.siwehrli.ws;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String NAMESPACE = "http://webservices.vslecture.vs.inf.ethz.ch/";
	private static final String METHOD_NAME = "getSpot";
	private static final String URL = "http://vslab.inf.ethz.ch:80/SunSPOTWebServices/SunSPOTWebservice";
	private static final String SOAP_ACTION = "http://vslab.inf.ethz.ch:80/SunSPOTWebServices/SunSPOTWebservice/getSpot";
	
	private TextView resultTextBox;
	private TextView rawXMLView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resultTextBox = (TextView) findViewById(R.id.resultTextBox);
		rawXMLView = (TextView) findViewById(R.id.rawXMLTextView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onGetTempButtonPressed(View v) {
		String[] args = { NAMESPACE, METHOD_NAME, URL, SOAP_ACTION };
		SoapTask myTask = new SoapTask();
		myTask.execute(args);
	}

	class SoapTask extends AsyncTask<String[], Void, SoapObject> {

		String rawXmlResponse;

		protected SoapObject doInBackground(String[]... args) {
			Log.d("bla", "trying to soap");
			SoapObject request = new SoapObject(args[0][0], args[0][1]);
			request.addProperty("id", "Spot3");
			SoapSerializationEnvelope mySoapEnvelope = new SoapSerializationEnvelope(
					SoapEnvelope.VER10);
			mySoapEnvelope.setOutputSoapObject(request);

			HttpTransportSE httpTransport = new HttpTransportSE(args[0][2]);
			httpTransport.debug = true;

			try {
				httpTransport.call(args[0][3], mySoapEnvelope);
				rawXmlResponse = httpTransport.responseDump;
				Log.d("bla", "send");
				SoapObject result = (SoapObject) mySoapEnvelope.getResponse();
				Log.d("bla", "Response: " + result.toString());
				return result;
			} catch (Exception e) {
				Log.d("bla", e.toString());
				Log.d("bla", "failed");
				return null;
			}
		}

		protected void onPostExecute(SoapObject result) {
			try {
				String temp = result
						.getPrimitivePropertyAsString("temperature");
				resultTextBox.setText(temp);
				rawXMLView.setText(rawXmlResponse);
			} catch (NullPointerException e) {
				resultTextBox.setText("Soap call failed. Sorry");
			}

		}
	}
}