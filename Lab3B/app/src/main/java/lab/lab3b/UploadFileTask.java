package lab.lab3b;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class UploadFileTask extends AsyncTask<Void, Void, String> {
	
	UploadFileTask(MainActivity main, String serverAddress, int port, String file) {
		this.main = main;
		this.serverAddress = serverAddress;
		this.port = port;
		this.file = file;
	}
	
	@Override
	protected String doInBackground(Void... voids) {

		String result = "Data uploaded";

		try {
			InetAddress remoteAddress = InetAddress
					.getByName(serverAddress);
			socket = new Socket(remoteAddress, port);
			InputStream is = main.openFileInput(file);
			Log.i("doInBackground", (is == null ? "null" : is.toString()));
			fin = new BufferedReader(new InputStreamReader(is));
			sout = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())));

			String line = fin.readLine();
			Log.i("doInBackground", line == null ? "null" : line);
			while (line != null && !this.isCancelled()) {
				sout.print(line + "\r\n");
				line = fin.readLine();
				Log.i("doInBackground", line == null ? "null" : line);
			} 
		} catch (Exception e) {
			Log.e("doInBackground", e.toString());
			result = "Error uploading data";
		} finally {
			cleanUp();
		}

		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		main.onUploadCompleted(result);
		return;
	}
	
	@Override
	protected void onCancelled() {
		main.onUploadCompleted("Uploading data canceled");
		return;
	}
	
	private MainActivity main;
	private String serverAddress;
	private int port;
	private String file;
	
	private Socket socket = null;
	private BufferedReader fin = null;
	private PrintWriter sout = null;
	
	private void cleanUp() {
		try {
			if (sout != null) sout.close();
			if (fin != null) fin.close();
		} 
		catch (Exception e) {}
		if (socket != null) {
			try {
				socket.close();
				Log.i("doInBackground", "socket closed");
			} 
			catch (Exception e) {}
		}
	}
}
