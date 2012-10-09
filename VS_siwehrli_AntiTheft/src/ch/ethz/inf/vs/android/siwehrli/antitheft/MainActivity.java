package ch.ethz.inf.vs.android.siwehrli.antitheft;

import android.os.Bundle;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	private static final String SETTINGS_NAME = "Settings";

	public static final boolean ACTIVATE_DEFAULT = false;
	public static final int SENSITIVITY_DEFAULT = 60;
	public static final int TIMEOUT_DEFAULT = 5;

	private boolean activate = ACTIVATE_DEFAULT;
	private int sensitivity = SENSITIVITY_DEFAULT;
	private int timeout = TIMEOUT_DEFAULT;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// read settings into private fields
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
				MODE_PRIVATE);
		this.activate = settings.getBoolean("activate", ACTIVATE_DEFAULT);
		this.sensitivity = settings.getInt("sensitivity", SENSITIVITY_DEFAULT);
		this.timeout = settings.getInt("timeout", TIMEOUT_DEFAULT);

		// set setting values to view components
		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButtonActivate);
		tb.setChecked(activate);
		SeekBar bar = (SeekBar) findViewById(R.id.seekBarSensitivity);
		bar.setProgress(sensitivity);
		EditText editText = (EditText) findViewById(R.id.editTextTimeout);
		editText.setHint(timeout + " "
				+ getResources().getString(R.string.timeout_unit));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClickActivate(View view) {
		activate = ((ToggleButton) view).isChecked();
		sensitivity = ((SeekBar) findViewById(R.id.seekBarSensitivity))
				.getProgress();
		try {
			timeout = Integer
					.parseInt(((EditText) findViewById(R.id.editTextTimeout))
							.getText().toString());
		} catch (NumberFormatException e) {

		}

		if (activate) {
			startAntiTheftService();
		} else {
			stopAntiTheftService();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (!intent.getBooleanExtra("activate", ACTIVATE_DEFAULT)) {
			stopAntiTheftService();
		}
	}

	private void startAntiTheftService() {
		Intent intent = new Intent(this, AntiTheftService.class);

		intent.putExtra("ch.ethz.inf.vs.android.siwehrli.antitheft.activate",
				activate);
		intent.putExtra(
				"ch.ethz.inf.vs.android.siwehrli.antitheft.sensitivity",
				sensitivity);
		intent.putExtra("ch.ethz.inf.vs.android.siwehrli.antitheft.timeout",
				timeout);

		startService(intent);
	}

	private void stopAntiTheftService() {
		activate = false;

		Intent intent = new Intent(this, AntiTheftService.class);

		// cancel notification (if exists)
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(AntiTheftService.NOTIFICATION_ACTIVATED_ID);

		// adjust activate button
		ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButtonActivate);
		tb.setChecked(activate);

		// stop service
		stopService(intent);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// save settings
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("activate", activate);
		editor.putInt("sensitivity", sensitivity);
		editor.putInt("timeout", timeout);
		editor.commit(); // Commit changes to file!!!
	}
}
