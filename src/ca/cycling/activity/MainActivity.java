package ca.cycling.activity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ca.cycling.R;
import ca.cycling.sensor.AccelerometerResultReceiver;
import ca.cycling.sensor.AccelerometerService;
import ca.cycling.sensor.GPSResultReceiver;
import ca.cycling.sensor.GPSService;
import ca.cycling.sensor.GyroscopeResultReceiver;
import ca.cycling.sensor.GyroscopeService;
import ca.cycling.sensor.OrientationResultReceiver;
import ca.cycling.sensor.OrientationService;
import ca.cycling.util.CAFileWriter;
import ca.cycling.util.Constants;
import ca.cycling.util.PreferencesData;

@TargetApi(Build.VERSION_CODES.FROYO)
public class MainActivity extends Activity {
	AccelerometerResultReceiver accelerometerResultReceiver;
	OrientationResultReceiver orientationResultReceiver;
	GyroscopeResultReceiver gyroscopeResultReceiver;
	GPSResultReceiver gpsResultReceiver;

	private static String FILE_PREFIX;
	private static final String FILE_SUFFIX = ".csv";
	CAFileWriter accelerometerWriter;
	CAFileWriter orientationWriter;
	CAFileWriter gyroscopeWriter;
	CAFileWriter gpsWriter;
	CAFileWriter stateWriter;

	private final String SENSOR_NOT_SUPPORTED = "Not supported";

	public static String SHA256 (String text) throws NoSuchAlgorithmException {

	    MessageDigest md = MessageDigest.getInstance("SHA-256");

	    md.update(text.getBytes());
	    byte[] digest = md.digest();

	    return Base64.encodeToString(digest, Base64.DEFAULT);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FILE_PREFIX = PreferencesData.getString(this,
				PreferencesData.PREFERENCE_FILE_PREFIX, "test_");

		setupSensors();
		setupStateToggles();

	}

	private List<ToggleButton> toggleButtons;
	private String currentState = "";

	private void setupStateToggles() {
		toggleButtons = new ArrayList<ToggleButton>();
		/*LinearLayout toggle_layout = (LinearLayout) findViewById(R.id.toggle_state_layout);
		for (int toggle_index = 0; toggle_index < toggle_layout.getChildCount(); toggle_index++) {
			ToggleButton toggle = (ToggleButton) toggle_layout
					.getChildAt(toggle_index);
			toggle.setOnCheckedChangeListener(new ToggleButtonChange());
			toggleButtons.add(toggle);
		}*/
	}

	private class ToggleButtonChange implements OnCheckedChangeListener {

		private final String TAG = ToggleButtonChange.class.getSimpleName();

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {

				String state = (String) buttonView.getTag();

				for (ToggleButton toggle : toggleButtons) {
					// uncheck others
					if (!toggle.getTag().equals(state)) {
						toggle.setChecked(false);
					}
				}

				if (!state.equals(currentState)) {
					currentState = state;
					Toast.makeText(MainActivity.this,
							"State set to " + currentState, Toast.LENGTH_SHORT)
							.show();
					stateWriter.write(currentState);
				}

			}
		}

	}

	private void setupSensors() {

		Handler handler = new Handler();

		gpsResultReceiver = new GPSResultReceiver(handler);
		gpsResultReceiver.setReceiver(new GPSReceiver());

		SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);
		for (int i = 0; i < sensorList.size(); i++) {
			int type = sensorList.get(i).getType();
			if (type == Sensor.TYPE_ACCELEROMETER) {
				accelerometerResultReceiver = new AccelerometerResultReceiver(
						handler);
				accelerometerResultReceiver
						.setReceiver(new AccelerometerReceiver());
			}
			if (type == Sensor.TYPE_ORIENTATION) {
				orientationResultReceiver = new OrientationResultReceiver(
						handler);
				orientationResultReceiver
						.setReceiver(new OrientationReceiver());
			}
			if (type == Sensor.TYPE_GYROSCOPE) {
				gyroscopeResultReceiver = new GyroscopeResultReceiver(handler);
				gyroscopeResultReceiver.setReceiver(new GyroscopeReceiver());

			}
		}

		if (accelerometerResultReceiver == null) {
			new UpdateUI(R.id.tv_accel_result, SENSOR_NOT_SUPPORTED);
		}
		if (orientationResultReceiver == null) {
			new UpdateUI(R.id.tv_orientation_result, SENSOR_NOT_SUPPORTED);
		}
		if (gyroscopeResultReceiver == null) {
			new UpdateUI(R.id.tv_gyro_result, SENSOR_NOT_SUPPORTED);
		}
		

	}

	private void createFileWriters() {
		accelerometerWriter = new CAFileWriter(
				getQualifiedFilename("accelerometer"), "x", "y", "z");

		orientationWriter = new CAFileWriter(
				getQualifiedFilename("orientation"), "x", "y", "z");

		gyroscopeWriter = new CAFileWriter(getQualifiedFilename("gyroscope"),
				"x", "y", "z");

		gpsWriter = new CAFileWriter(getQualifiedFilename("gps"), "firstfix",
				"lat", "lng", "speed", "altitude", "bearing", "accuracy",
				"time");

		stateWriter = new CAFileWriter(getQualifiedFilename("state"), "state");

	}

	private String getQualifiedFilename(String filename) {
		return FILE_PREFIX + filename + FILE_SUFFIX;
	}

	@Override
	protected void onDestroy() {
		closeWriters();
		super.onDestroy();
	}

	private void closeWriters() {
		accelerometerWriter.close();
		orientationWriter.close();
		gyroscopeWriter.close();
		gpsWriter.close();
		stateWriter.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_file_prefix) {
			LayoutInflater factory = LayoutInflater.from(MainActivity.this);
			final View textEntryView = factory.inflate(
					R.layout.dialog_stringinput, null);
			AlertDialog.Builder alert = new AlertDialog.Builder(
					MainActivity.this);
			alert.setTitle("Set file prefix");
			alert.setView(textEntryView);

			final EditText mUserText = (EditText) textEntryView
					.findViewById(R.id.value);
			mUserText.setText(FILE_PREFIX);

			alert.setPositiveButton(getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							String prefixString = mUserText.getText()
									.toString();

							FILE_PREFIX = prefixString;

							PreferencesData.saveString(MainActivity.this,
									PreferencesData.PREFERENCE_FILE_PREFIX,
									prefixString);

							createFileWriters();

							return;
						}
					});

			alert.setNegativeButton(getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							return;
						}
					});
			alert.show();
		}
		return super.onOptionsItemSelected(item);
	}

	// button click
	public void start(View v) {

		createFileWriters();

		// start GPS service
		Intent gpsIntent = new Intent(this, GPSService.class);
		gpsIntent.putExtra(Constants.EXTRA_RECEIVER, gpsResultReceiver);
		startService(gpsIntent);
		
		// start accelerator service
		if (accelerometerResultReceiver != null) {
			Intent accIntent = new Intent(MainActivity.this, AccelerometerService.class);
			accIntent.putExtra(Constants.EXTRA_RECEIVER,
					accelerometerResultReceiver);
			// idempotent call
			startService(accIntent);
		}

		// start orientation service
		if (orientationResultReceiver != null) {
			Intent orientationIntent = new Intent(MainActivity.this,
					OrientationService.class);
			orientationIntent.putExtra(Constants.EXTRA_RECEIVER,
					orientationResultReceiver);
			// idempotent call
			startService(orientationIntent);
		}

		// start gyroscope service
		if (gyroscopeResultReceiver != null) {
			Intent gyroscopeIntent = new Intent(MainActivity.this, GyroscopeService.class);
			gyroscopeIntent.putExtra(Constants.EXTRA_RECEIVER,
					gyroscopeResultReceiver);
			// idempotent call
			startService(gyroscopeIntent);
		}
	}

	public void stop(View v) {
		if (accelerometerResultReceiver != null)
			stopService(new Intent(this, AccelerometerService.class));
		if (orientationResultReceiver != null)
			stopService(new Intent(this, OrientationService.class));
		if (gyroscopeResultReceiver != null)
			stopService(new Intent(this, GyroscopeService.class));

		stopService(new Intent(this, GPSService.class));
	}

	private class UpdateUI implements Runnable {

		private String mText;
		private TextView mTv;

		public UpdateUI(int textviewid, String text) {
			this.mTv = (TextView) findViewById(textviewid);
			this.mText = text;
		}

		@Override
		public void run() {
			mTv.setText(mText);
		}

	}

	private class AccelerometerReceiver implements
			AccelerometerResultReceiver.Receiver {

		private int resultTextViewID;
		private float x, y, z;

		public AccelerometerReceiver() {
			resultTextViewID = R.id.tv_accel_result;
		}

		private void sendLocationToUI() throws NoSuchAlgorithmException {
			/*double roundx = Math.round(x);
			double roundy = Math.round(y);
			double roundz = Math.round(z);*/
			String SHA = SHA256(x + y + z + "");
			runOnUiThread(new UpdateUI(resultTextViewID,SHA.substring(0,SHA.length()-1)));
			
		}

		@Override
		public void newEvent(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			try {
				sendLocationToUI();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			accelerometerWriter.write(x, y, z);
		}

		@Override
		public void error(String error) {
			runOnUiThread(new UpdateUI(resultTextViewID, error));
		}

	}

	private class OrientationReceiver implements
			OrientationResultReceiver.Receiver {

		private int resultTextViewID;
		private float x, y, z;

		public OrientationReceiver() {
			resultTextViewID = R.id.tv_orientation_result;
		}

		private void sendLocationToUI() throws NoSuchAlgorithmException {
			/*double roundx = Math.round(x);
			double roundy = Math.round(y);
			double roundz = Math.round(z);*/
			String SHA = SHA256(x + y + z + "");
			runOnUiThread(new UpdateUI(resultTextViewID,SHA.substring(0,SHA.length()-1)));
		}

		@Override
		public void newEvent(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			try {
				sendLocationToUI();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			orientationWriter.write(x, y, z);
		}

		@Override
		public void error(String error) {
			runOnUiThread(new UpdateUI(resultTextViewID, error));
		}

	}

	private class GyroscopeReceiver implements GyroscopeResultReceiver.Receiver {

		private int resultTextViewID;
		private float x, y, z;

		public GyroscopeReceiver() {

			resultTextViewID = R.id.tv_gyro_result;
		}

		private void sendLocationToUI() throws NoSuchAlgorithmException {
			/*double roundx = Math.round(x);
			double roundy = Math.round(y);
			double roundz = Math.round(z);*/
			String SHA = SHA256(x + y + z + "");
			runOnUiThread(new UpdateUI(resultTextViewID,SHA.substring(0,SHA.length()-1)));
		}

		@Override
		public void newEvent(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			try {
				sendLocationToUI();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gyroscopeWriter.write(x, y, z);
		}

		@Override
		public void error(String error) {
			runOnUiThread(new UpdateUI(resultTextViewID, error));
		}

	}

	private class GPSReceiver implements GPSResultReceiver.Receiver {

		private int resultTextViewID;

		boolean hasFirstFix;
		float accuracy;
		long time;
		float speed;

		double lat;
		double lng;

		double altitude;
		float bearing;

		public GPSReceiver() {
			resultTextViewID = R.id.tv_gps_result;
		}

		@Override
		public void firstFixChange(boolean hasFirstFix) {
			this.hasFirstFix = hasFirstFix;
			if (hasFirstFix) {
				try {
					sendLocationToUI();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

			}
		}

		@Override
		public void newLocation(Location location) {
			this.accuracy = location.getAccuracy();
			this.time = location.getTime();
			this.speed = location.getSpeed();

			this.lat = location.getLatitude();
			this.lng = location.getLongitude();

			this.altitude = location.getAltitude();
			this.bearing = location.getBearing();

			try {
				sendLocationToUI();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writeLocationToFile();

		}

		private void writeLocationToFile() {
			// gpsWriter.open("timestamp", "firstfix", "lat", "lng", "speed",
			// "altitude", "bearing", "accuracy", "time");
			gpsWriter.write(getStringValues());
		}

		private String[] getStringValues() {
			String[] values = new String[] { String.valueOf(hasFirstFix),
					String.valueOf(lat), String.valueOf(lng),
					String.valueOf(speed), String.valueOf(altitude),
					String.valueOf(bearing), String.valueOf(accuracy),
					String.valueOf(time) };

			return values;
		}

		private void sendLocationToUI() throws NoSuchAlgorithmException {
			runOnUiThread(new UpdateUI(resultTextViewID, "hasFirstFix: "
					+ hasFirstFix + "\n" + "time: " + time + "\n" + "speed: "
					+ speed + "\n" + "accuracy: " + accuracy + "\n"
					+ "latitude: " + lat + "\n" + "longitude: " + lng));
			String SHA = SHA256(time + speed + accuracy + lat + lng + "");
			runOnUiThread(new UpdateUI(resultTextViewID,SHA.substring(0,SHA.length()-1)));
		}

		@Override
		public void error(String error) {
			runOnUiThread(new UpdateUI(resultTextViewID, error));
		}

	}

}
