/*
 * Lembretes. This software is intended for students from UNICAMP as a simple reminder of the daily meal.
 * Copyright (C) 2013  Edson Duarte (edsonduarte1990@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.lostrealm.lembretes;

import java.io.File;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	public static final String CLASS_TAG = "com.lostrealm.lembretes.MainActivity";

	private TextView mealView;
	private TextView updateView;

	/**
	 * Method updates views with content from disk.
	 * @param context Context received from the receiver.
	 */
	private void updateView(Context context) {
		String content = ContentManager.getContent(context);
		if (content != null) {
			mealView.setText(Html.fromHtml(content));
			updateView.setText(this.getSharedPreferences("last_update", MODE_PRIVATE).getString("last_update", getString(R.string.main_activity_update_view)));
		} else
			mealView.setText(R.string.downloading_error);

		context.startService(LoggerIntentService.newLogIntent(context, CLASS_TAG, "View updated."));
	}

	private void setDefaultValuesToViews() {
		mealView.setText(R.string.main_activity_view);
		updateView.setText(R.string.main_activity_note);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			context.startService(LoggerIntentService.newLogIntent(context, CLASS_TAG, "Broadcast received."));
			updateView(context);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		mealView = (TextView) findViewById(R.id.mainActivityMealView);
		updateView = (TextView) findViewById(R.id.mainActivityUpdateView);

		this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "=========="));
		this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Started! " + getString(R.string.app_version)));
	}

	@Override
	protected void onResume() {
		super.onResume();
		// As this activity just receives broadcasts when running, we can register its receiver using the LocalBroadcastManager.
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(CLASS_TAG));

		// App is running for the first time.
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("first_time", true)) {
			this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Started! " + getString(R.string.app_version) + "."));

			refreshContent();

			this.startService(new Intent(this, UpdateIntentService.class));
			this.startService(new Intent(this, ReminderIntentService.class));
			this.startActivity(new Intent(this, SettingsActivity.class));

			// TODO move this to some "ContentManager"
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File file = new File(Environment.getExternalStorageDirectory(), "data/com.lostrealm.lembretes");
				if (!file.exists()) {
					file.mkdirs();
					this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Created location on media."));
				}
			}
		}
		// Load content from disk.
		else if (!this.getSharedPreferences("last_update", MODE_PRIVATE).getString("last_update", "").equals(getString(R.string.main_activity_note)))
			updateView(this);
		else
			this.setDefaultValuesToViews();

		// Cancels any active notification.
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ReminderIntentService.REMINDER_ID);

		this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Showing MainActivity."));
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Terminated Application."));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			refreshContent();
			break;
		case R.id.action_settings:
			this.startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.action_feedback:
			String fileName = "data/com.lostrealm.lembretes/log";
			File file = null;

			String[] recipients = new String[]{"edsonduarte1990@gmail.com"};

			Intent intent = new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[" + getString(R.string.app_name) + " - Feedback]");

			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_logging", false)) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					file = new File(Environment.getExternalStorageDirectory(), fileName);
					intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(file));
					Toast.makeText(this, R.string.file_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show();
			}

			this.startActivity(Intent.createChooser(intent, getString(R.string.main_activity_chooser)));

			this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Opened feedback."));
			break;
		case R.id.action_about:
			this.startActivity(new Intent(this, AboutActivity.class));
			break;
		default:
			super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void refreshContent() {
		this.startService(LoggerIntentService.newLogIntent(this, CLASS_TAG, "Refreshing."));
		this.startService(new Intent(this, NetworkIntentService.class));
		this.setDefaultValuesToViews();
		this.getSharedPreferences("last_update", MODE_PRIVATE).edit().putString("last_update", getString(R.string.main_activity_note)).commit();
	}
}
