package ch.ethz.inf.vs.android.siwehrli.sensors;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

public class ActuatorsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actuators);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_actuators, menu);
        return true;
    }
    
    
    public void onPlaySoundButtonClick (View v){
    	MediaPlayer mp = MediaPlayer.create(this, R.raw.sentry_mode);
    	mp.setVolume(1.0f, 1.0f);
    	mp.start();
    }
    
    public void onVibrateButtonClick (View v){
    	SeekBar seeker = (SeekBar) findViewById(R.id.vibrateLengthBar);
    	int duration = seeker.getProgress(); //saves current seeker position into duration
    	
    	Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    	long[] pattern = { 0, duration }; //uses duration within this simple pattern (idle for 0, vibrate for 'duration')
    	vib.vibrate(pattern, -1);
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
