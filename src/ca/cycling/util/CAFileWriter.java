package ca.cycling.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

public class CAFileWriter {

	private String filepath;
	private BufferedWriter out;

	public CAFileWriter(String filename, String... header) {
		String externalSDPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath();

		String dirpath = externalSDPath + File.separator + Constants.APP_NAME;

		File pathFile = new File(dirpath);
		pathFile.mkdirs();

		filepath = dirpath + File.separator + filename;

		File file = new File(filepath);
		if (!file.exists()) {
			open(header); // create new file with header
		} else {
			open(); // open existing file
		}

	}

	public void write(String... args) {
		write(true, args);
	}

	public void write(float... args) {
		write(true, args);
	}

	public void write(boolean addTimeStamp, String... args) {
//		if (!addTimeStamp) {
//			new WriteTask().execute(args);
//		} else {
//			new WriteTask(SystemClock.elapsedRealtime()).execute(args);
//		}
	}

	public void write(boolean addTimeStamp, float... args) {
//		String[] strings = new String[args.length];
//		int index = 0;
//		for (Float arg : args) {
//			strings[index] = String.valueOf(arg);
//			index++;
//		}
//		new WriteTask(SystemClock.elapsedRealtime()).execute(strings);
	}

	private class WriteTask extends AsyncTask<String, Void, Void> {

		private static final String TAG = "WriteTask";

		StringBuilder b = new StringBuilder();

		public WriteTask() {

		}

		public WriteTask(long timestamp) {
			b.append(timestamp);
			b.append(",");
		}

		@Override
		protected Void doInBackground(String... params) {
			try {
				int counter = 1;
				for (String arg : params) {
					b.append(arg);
					if (counter != params.length)
						b.append(",");
					counter++;
				}

				if (out != null) {
					Log.d(TAG, "Wrote to: " + filepath);
					out.write(b.toString());
					out.newLine();
					out.flush();
				} else {
					Log.e(TAG, "out was null");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	private void open(String... header) {

		String[] header_with_timestamp = new String[header.length + 1];
		header_with_timestamp[0] = "timestamp";

		for (int i = 0; i < header.length; i++) {
			// Log.d("OPEN", header[i]);
			header_with_timestamp[(i + 1)] = header[i];
		}

		// Log.d("OPEN", "header_with_timestamp.length " +
		// header_with_timestamp.length);

		File file = new File(filepath);
		try {
			if (!(header.length == 0)) {
				out = new BufferedWriter(new FileWriter(file, false));
				write(false, header_with_timestamp);
			}
			out = new BufferedWriter(new FileWriter(file, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
