/*
 * synergy -- mouse and keyboard sharing utility
 * Copyright (C) 2010 Shaun Patterson
 * Copyright (C) 2010 The Synergy Project
 * Copyright (C) 2009 The Synergy+ Project
 * Copyright (C) 2002 Chris Schoeneman
 * 
 * This package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * found in the file COPYING that should have accompanied this file.
 * 
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.synergy;

import org.synergy.base.Event;
import org.synergy.base.EventJobInterface;
import org.synergy.base.EventQueue;
import org.synergy.base.EventType;
import org.synergy.base.Log;
import org.synergy.client.Client;
import org.synergy.common.screens.BasicScreen;
//import org.synergy.common.screens.PlatformIndependentScreen;
import org.synergy.injection.Injection;
import org.synergy.net.NetworkAddress;
import org.synergy.net.SocketFactoryInterface;
import org.synergy.net.TCPSocketFactory;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class Synergy extends Activity {

	private final static String PROP_clientName = "clientName";
	private final static String PROP_serverHost = "serverHost";
	private final static String PROP_deviceName = "deviceName";

	private Thread mainLoopThread = null;
	private Handler mainHandler = null;
	private Client client;
	private boolean autoReconnect;
	static {
		System.loadLibrary ("synergy-jni");
	}

	private class MainLoopThread extends Thread {

		public void run () {
			//runOnUiThread(new Runnable() {
			//@Override
			//public void run() {
			try {
				Event event = new Event ();
				event = EventQueue.getInstance ().getEvent (event, -1.0);
				Log.note ("Events grabbed...");
				while (event.getType () != EventType.QUIT && mainLoopThread == Thread.currentThread()) {
					Log.note("Event grabbed - type=" + event.getType() + ";target=" + event.getTarget());
					if (event.getType() == EventType.CLIENT_RECONNECT_DISABLED)
					{
						disableClientReconnect();
					}
					else if (event.getType() == EventType.CLIENT_RECONNECT_ENABLED)
					{
						enableClientReconnect();
					}
					else
					{
						EventQueue.getInstance ().dispatchEvent(event);
					}
					// TODO event.deleteData ();
					event = EventQueue.getInstance ().getEvent (event, -1.0);
				}
				mainLoopThread = null;
			} catch (Exception e) {
				e.printStackTrace ();
			} finally {
				Injection.stop();
			}
			//}
			//});
		}
	}

	private void enableClientReconnect() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				final CheckBox reconnectCheckbox = (CheckBox) findViewById(R.id.autoReconnectCheckbox);
				autoReconnect = true;
				reconnectCheckbox.setChecked(true);
			}
		});
	}

	private void disableClientReconnect() {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				final CheckBox reconnectCheckbox = (CheckBox) findViewById(R.id.autoReconnectCheckbox);
				autoReconnect = false;
				reconnectCheckbox.setChecked(false);
			}
		});
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mainHandler = new Handler(Looper.getMainLooper());
		autoReconnect = true;
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		String clientName = preferences.getString(PROP_clientName, null);
		if (clientName != null) {
			((EditText) findViewById (R.id.clientNameEditText)).setText(clientName);
		}
		String serverHost = preferences.getString(PROP_serverHost, null);
		if (serverHost != null) {
			((EditText) findViewById (R.id.serverHostEditText)).setText(serverHost);
		}

		final Button connectButton = (Button) findViewById (R.id.connectButton);
		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg) {
				connect();
			}
		});

		final CheckBox reconnectCheckbox = (CheckBox) findViewById(R.id.autoReconnectCheckbox);
		reconnectCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				autoReconnect = isChecked;
				if (client != null) client.setAutoReconnect(isChecked);
			}
		});
		Log.setLogLevel (Log.Level.DEBUG);

		Log.debug ("Client starting....");

	}

	/** Called when the activity comes into the foreground. */
	@Override
	public void onResume() {
		super.onResume();
		Log.debug("onResume - "+client);
		try {
			Injection.setPermissionsForInputDevice();
			if (client != null && client.getAutoReconnect())
			{
				client.connect();
			}
		} catch (Exception e) {
			// TODO handle exception
		}
	}

	private void connect () {

		String clientName = ((EditText) findViewById (R.id.clientNameEditText)).getText().toString();
		String ipAddress = ((EditText) findViewById (R.id.serverHostEditText)).getText().toString();
		String portStr = ((EditText) findViewById(R.id.serverPortEditText)).getText().toString();
		int port = Integer.parseInt(portStr);
		String deviceName = ((EditText) findViewById(R.id.inputDeviceEditText)).getText().toString();

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor preferencesEditor = preferences.edit();
		preferencesEditor.putString(PROP_clientName, clientName);
		preferencesEditor.putString(PROP_serverHost, ipAddress);
		preferencesEditor.putString(PROP_deviceName, deviceName);
		preferencesEditor.commit();

		try {
			SocketFactoryInterface socketFactory = new TCPSocketFactory();
			NetworkAddress serverAddress = new NetworkAddress (ipAddress, port);
			serverAddress.resolve ();

			Injection.startInjection(deviceName);

			BasicScreen basicScreen = new BasicScreen();

			WindowManager wm = getWindowManager();

			Display display = wm.getDefaultDisplay ();
			basicScreen.setShape(display.getWidth(), display.getHeight());


			//PlatformIndependentScreen screen = new PlatformIndependentScreen(basicScreen);

			Log.debug("Hostname: " + clientName);

			client = new Client (getApplicationContext(), clientName, serverAddress, socketFactory, null, basicScreen, autoReconnect);
			client.connect();

			if (mainLoopThread == null) {
				mainLoopThread = new MainLoopThread();
				mainLoopThread.start();
			}

		} catch (Exception e) {
			e.printStackTrace();
			((EditText) findViewById (R.id.outputEditText)).setText("Connection Failed.");
		}
	}
}
