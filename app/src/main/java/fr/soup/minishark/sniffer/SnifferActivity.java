package fr.soup.minishark.sniffer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import fr.soup.minishark.R;
import fr.soup.minishark.sniffer.TcpDumpWrapper;

/**
 * Created by cyprien on 08/07/16.
 */
public class SnifferActivity extends Activity{

    public static final String SNIFFER_FLAGS_INTENT = "snifferactivityflagsintent";

    final Context context = this;
    private ListView listView;
    private boolean tcpdumpBound = false;
    private String flags;
    TcpDumpWrapper mService;

    private BroadcastReceiver sharkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == TcpDumpWrapper.REFRESH_DATA_INTENT) {
                for (String data : intent.getStringArrayListExtra(TcpDumpWrapper.REFRESH_DATA)) {
                    TextView textView = new TextView(context);
                    textView.setText(data);
                    listView.addFooterView(textView);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sniffer);

        Intent intent = getIntent();
        flags= intent.getStringExtra(SNIFFER_FLAGS_INTENT);

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifi = wifi.getConnectionInfo();

        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(context.getApplicationContext(), R.string.disconnected_toast, Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        if (mWifi.getSupplicantState() != SupplicantState.COMPLETED) {
            Toast.makeText(context.getApplicationContext(), R.string.no_wifi_connected_toast, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        listView=(ListView)findViewById(R.id.sharkListView);
        registerReceiver(sharkReceiver, new IntentFilter(TcpDumpWrapper.REFRESH_DATA_INTENT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tcpdumpBound) {
            unbindService(mConnection);
            tcpdumpBound = false;
        }
    }

    public void snifferStart(View view) {

        EditText editText = (EditText) findViewById(R.id.manualflags);
        flags += editText.getText().toString() + " ";


        if(((CheckBox)findViewById(R.id.saveinfile)).isChecked()) {
            editText = (EditText) findViewById(R.id.pcapfile);
            flags += "-w /storage/emulated/legacy/" + editText.getText().toString() + " ";
        }

        if(((CheckBox)findViewById(R.id.rununtil)).isChecked()) {
            editText = (EditText) findViewById(R.id.rununtiltime);
            flags += "-G " + editText.getText().toString() + " -W 1 ";
        }

        Intent intent = new Intent(this, TcpDumpWrapper.class);
        intent.putExtra(SnifferActivity.SNIFFER_FLAGS_INTENT, flags);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


     private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TcpDumpWrapper.TcpDumpWrapperBinder binder = (TcpDumpWrapper.TcpDumpWrapperBinder) service;
            mService = binder.getService();
            tcpdumpBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            tcpdumpBound = false;
        }
    };
}