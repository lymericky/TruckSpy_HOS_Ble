package com.pt.devicemanager.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.ListFragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.pt.devicemanager.R;
import com.pt.sdk.CustomProber;

import java.util.ArrayList;
import java.util.Locale;

public class DevicesFragment extends ListFragment {
    public static final String FRAG_TAG = "usb_devices_view";
    /**
     * Interface required to be implemented by activity.
     */
    public interface OnUSBDeviceSelectedListener {
        /**
         * Fired when user selected the device.
         *
         * @param device
         *            the device to connect to
         * @param name
         */
        void onUsbDeviceSelected(final UsbDevice device, final String name, int port);

    }

    class ListItem {
        UsbDevice device;
        int port;
        UsbSerialDriver driver;

        ListItem(UsbDevice device, int port, UsbSerialDriver driver) {
            this.device = device;
            this.port = port;
            this.driver = driver;
        }
    }
    private OnUSBDeviceSelectedListener mListener;

    private ArrayList<ListItem> listItems = new ArrayList<>();
    private ArrayAdapter<ListItem> listAdapter;
    //private int baudRate = 115200;

    public static DevicesFragment newInstance() {
        DevicesFragment fragment = new DevicesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        listAdapter = new ArrayAdapter<ListItem>(getActivity(), 0, listItems) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                ListItem item = listItems.get(position);
                if (view == null)
                    view = getActivity().getLayoutInflater().inflate(R.layout.usb_device_list_item, parent, false);
                TextView text1 = view.findViewById(R.id.text1);
                TextView text2 = view.findViewById(R.id.text2);
                if(item.driver == null)
                    text1.setText("<no driver>");
                else if(item.driver.getPorts().size() == 1)
                    text1.setText(item.driver.getClass().getSimpleName().replace("SerialDriver",""));
                else
                    text1.setText(item.driver.getClass().getSimpleName().replace("SerialDriver","")+", Port "+item.port);
                text2.setText(String.format(Locale.US, "Name %s ", item.device.getDeviceName()));
                return view;
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.usb_device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("Plug in a tracker...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnUSBDeviceSelectedListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnUSBDeviceSelectedListener");
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_menu_usb_devices, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.refresh) {
            refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void refresh() {
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        UsbSerialProber usbCustomProber = CustomProber.getCustomProber();
        listItems.clear();
        for(UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            if(driver == null) {
                driver = usbCustomProber.probeDevice(device);
            }
            if(driver != null) {
                for(int port = 0; port < driver.getPorts().size(); port++)
                    listItems.add(new ListItem(device, port, driver));
            } else {
                listItems.add(new ListItem(device, 0, null));
            }
        }
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ListItem item = listItems.get(position-1);
        if(item.driver == null) {
            Toast.makeText(getActivity(), "no driver", Toast.LENGTH_SHORT).show();
        } else {
//            Bundle args = new Bundle();
//            args.putInt("device", item.device.getDeviceId());
//            args.putInt("port", item.port);
//            args.putInt("baud", baudRate);

//            Fragment fragment = new TerminalFragment();
//            fragment.setArguments(args);
//            getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null).commit();

            mListener.onUsbDeviceSelected(item.device, "PT4X Serial", item.port);
        }
    }

}
