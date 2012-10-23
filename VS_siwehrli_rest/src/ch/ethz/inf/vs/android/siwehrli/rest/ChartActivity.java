package ch.ethz.inf.vs.android.siwehrli.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.support.v4.app.NavUtils;

public class ChartActivity extends Activity {
	DownloadImageTask task = null;
	//boolean activate = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chart);

		// set setting values to view components
		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButtonActivate);
		tb.setChecked(false);

		this.updateStatus(false);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_chart, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.button3:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		this.updateStatus(((ToggleButton) findViewById(R.id.toggleButtonActivate)).isChecked());
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.updateStatus(false);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.updateStatus(false);
	}

	public void onClickActivate(View view) {
		this.updateStatus(((ToggleButton) view).isChecked());
	}

	private void updateStatus(boolean activate) {
		if (activate) {
			ImageView imageView = (ImageView) findViewById(R.id.chart_view);
			this.task = new DownloadImageTask(imageView.getWidth(),
					imageView.getHeight());
			task.execute();
		} else if (task != null) {
			this.task.cancel(false);
		}
	}

	private class DownloadImageTask extends AsyncTask<Void, Bitmap, Bitmap> {
		final String basic_url = "https://chart.googleapis.com/chart?";
		final String basic_options = "&cht=lxy&chxt=x,y&chdl=Temperature&chdlp=bv|l";
		int width;
		int height;
		// the google chart tools knows a size limit for charts
		final static int MAX_PIXEL_COUNT = 300000;
		final static int PREFERRED_HEIGHT = 300;
		final static int PREFERRED_WIDTH = 600;
		
		/** Stores the values measured and corresponding time points **/
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Long> timepoints = new ArrayList<Long>();
		Double maxValue = Double.MIN_VALUE;
		Double minValue = Double.MAX_VALUE;
		
		final static long TIME_STEP = 1000;

		long startTime;

		protected DownloadImageTask(int width, int height) {
			this.height = height>0?Math.min(PREFERRED_HEIGHT, height):PREFERRED_HEIGHT;
			this.width = width>0?Math.min(width, MAX_PIXEL_COUNT / this.height):PREFERRED_WIDTH;  
		}

		/**
		 * The system calls this to perform work in a worker thread and delivers
		 * it the parameters given to AsyncTask.execute()
		 */
		protected Bitmap doInBackground(Void... params) {
			StringBuilder sb = new StringBuilder();
			Bitmap chart = BitmapFactory.decodeResource(getResources(),
					android.R.drawable.ic_popup_sync);

			this.startTime = System.currentTimeMillis();

			try {
				while (!this.isCancelled()) {
					// Fetch Data
					JSONObject jsonO = fetchJSONObject("/sunspots/Spot1/sensors/temperature");
					if (jsonO != null) {
						double value = Double.parseDouble(jsonO
								.getString("value"));
						// add measured value, rounded to 2 decimals
						value = (double) Math.round(value * 1000d) / 1000d;
						values.add(value);
						// check for new min/max
						minValue = Math.min(minValue, value);
						maxValue = Math.max(maxValue, value);
						timepoints
								.add((System.currentTimeMillis() - this.startTime) / 1000);
						Log.d("Chart", "Value fetched: " + value);

						// Build up url out of values
						sb.delete(0, sb.length()); // clear builder
						sb.append(basic_url);
						sb.append(basic_options);
						sb.append("&chs=" + this.width + "x" + this.height);
						sb.append("&chxr=0,0,"+ timepoints.get(timepoints.size() - 1)+"|1,"+minValue + "," + maxValue);
						sb.append("&chds=0,"
								+ timepoints.get(timepoints.size() - 1) + ","
								+ minValue + "," + maxValue);

						// The format of the data:
						// "&chd=t:10,20,40,80,90,95,99|20,30,40,50,60,70,80"
						sb.append("&chd=t:");
						for (Long t : timepoints) {
							sb.append(t);
							sb.append(",");
						}
						sb.delete(sb.length() - 1, sb.length());
						sb.append("|");
						for (Double v : values) {
							sb.append(v);
							sb.append(",");
						}
						sb.delete(sb.length() - 1, sb.length());

						publishProgress(createChart(sb.toString()));
					}

					Thread.sleep(TIME_STEP);
				}
			} catch (JSONException e) {
				Log.e("Chart", e.getMessage());
			} catch (InterruptedException e) {
				Log.e("Chart", e.getMessage());
			}

			return chart;
		}

		@Override
		protected void onProgressUpdate(Bitmap... values) {
			ImageView imageView = (ImageView) findViewById(R.id.chart_view);
			imageView.setImageBitmap(values[values.length - 1]);
		}

		/**
		 * The system calls this to perform work in the UI thread and delivers
		 * the result from doInBackground()
		 */
		protected void onPostExecute(Bitmap result) {
			ImageView imageView = (ImageView) findViewById(R.id.chart_view);
			imageView.setImageBitmap(result);
		}

	}

	public static Bitmap createChart(String url) {
		Bitmap bitmap = null;

		try {
			bitmap = BitmapFactory.decodeStream((InputStream) new URL(url)
					.getContent());
		} catch (MalformedURLException e) {
			Log.e("Chart", e.getMessage());
		} catch (IOException e) {
			Log.e("Chart", e.getMessage());
		}

		return bitmap;
	}

	public static JSONObject fetchJSONObject(String path) {
		DefaultHttpClient client;
		HttpResponse response;
		HttpGet httpget;
		JSONObject jsonData = null;

		BufferedReader reader;

		URI uri;
		try {
			uri = org.apache.http.client.utils.URIUtils.createURI("http",
					MainActivity.host, MainActivity.port, path, null, null);

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

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}

			jsonData = new JSONObject(sb.toString());
		} catch (URISyntaxException e) {
			Log.d("http", "Malformed URI!");
		} catch (ClientProtocolException e) {
			Log.d("http", "Client protocol Exception!");
		} catch (IOException e) {
			Log.d("http", "I/O Exception!");
		} catch (JSONException e) {
			Log.d("http", "JSON Exception! " + e.getMessage());
		}

		return jsonData;
	}
}
