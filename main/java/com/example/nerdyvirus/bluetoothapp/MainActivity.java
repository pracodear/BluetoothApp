package com.example.nerdyvirus.bluetoothapp;



import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener{

    //private FragmentTabHost fragmentTabHost ;
    private final String LOG_TAG=MainActivity.class.getSimpleName();
    private CustomFragmentPagerAdapter fragmentPagerAdapter ;
    private ViewPager viewPager ;
    private ActionBar actionBar ;
    private BluetoothAdapter mBluetoothAdapter ;
    SwitchCompat switchButton ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        viewPager= (ViewPager) findViewById(R.id.pager);
        actionBar=getSupportActionBar();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(adapterStateChangeReceiver, filter);

        fragmentPagerAdapter=new CustomFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);


        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.addTab(actionBar.newTab().setText("Chats").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("Available").setTabListener(this));


        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        View v = (View) menu.findItem(R.id.myswitch).getActionView();

        switchButton= (SwitchCompat) v.findViewById(R.id.switchForActionBar);

        if(mBluetoothAdapter.isEnabled())
            switchButton.setChecked(true);
        else
            switchButton.setChecked(false);

        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.d(LOG_TAG,"Listener 2 called");
                if(switchButton.isChecked() && !mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();

                }
                else
                    mBluetoothAdapter.disable();

            }
        });

        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver adapterStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if(state==BluetoothAdapter.STATE_ON)
                    switchButton.setChecked(true);
                else if(state==BluetoothAdapter.STATE_OFF)
                    switchButton.setChecked(false);
            }
        }
    };

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(adapterStateChangeReceiver);
    }
}
