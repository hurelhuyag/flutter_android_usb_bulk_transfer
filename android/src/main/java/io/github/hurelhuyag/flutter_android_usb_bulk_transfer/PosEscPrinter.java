package io.github.hurelhuyag.flutter_android_usb_bulk_transfer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class PosEscPrinter implements AutoCloseable {

    private static final String ACTION_USB_PERMISSION = "io.github.hurelhuyag.usb.USB_PERMISSION";
    private static final String TAG = "PosEscPrinter";
    private Context context;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbEndpoint endpointOut;
    private UsbEndpoint endpointIn;
    private UsbInterface usbInterface;

    public PosEscPrinter(Context context) {
        this(context, 1305, 8211);
    }

    public PosEscPrinter(Context context, int vid, int pid) {
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbDevice = findPrinter(vid, pid);
        if (usbDevice != null) {
            connect();
        }
    }

    private UsbDevice findPrinter(int vid, int pid) {
        var deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            Log.d(TAG, "Found USB device: " + device.getDeviceName() + " VendorId: " + device.getVendorId() + " ProductId: " + device.getProductId());

            if (device.getVendorId() == vid && device.getProductId() == pid) {
                Log.d(TAG, "Selected USB device: " + device.getDeviceName() + " VendorId: " + device.getVendorId() + " ProductId: " + device.getProductId());
                return device;
            }
        }
        return null;
    }

    public void connect() {
        if (usbDevice == null) {
            Log.d(TAG, "Can't find printer device");
            return;
        }
        if (!usbManager.hasPermission(usbDevice)) {
            var intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(usbReceiver, intentFilter, 0);

            var permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(usbDevice, permissionIntent);
            return;
        }

        usbInterface = usbDevice.getInterface(0);
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);
            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                Log.d(TAG, "USB_DIR_OUT found");
                endpointOut = ep;
            }
            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                Log.d(TAG, "USB_DIR_IN found");
                endpointIn = ep;
            }
        }

        connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
            boolean claimed = connection.claimInterface(usbInterface, true);
            Log.d(TAG, "Interface claimed: " + claimed);
        }
    }

    public void write(byte[] buf) {
        if (connection == null || endpointOut == null) {
            Log.d(TAG, "Printer not connected");
            return;
        }
        var sent = connection.bulkTransfer(endpointOut, buf, 0, buf.length, 5000);
        Log.d(TAG, "Send Status: "  + sent);
        if (sent != buf.length) {
            Log.e(TAG, "Send failed");
        }
    }

    public void read(byte[] buf) {
        if (connection == null || endpointIn == null) {
            Log.d(TAG, "Printer not connected");
            return;
        }
        var received = connection.bulkTransfer(endpointIn, buf, 0, buf.length, 5000);
        Log.d(TAG, "Receive Status: "  + received);
        if (received != buf.length) {
            Log.e(TAG, "Receive failed");
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            var released = connection.releaseInterface(usbInterface);
            Log.d(TAG, "Interface released: " + released);
            connection.close();
            connection = null;
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    var device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null) {
                            Log.d(TAG, "Permission granted for " + device.getVendorId() + ":" + device.getProductId());
                        }
                    }
                }
            }
        }
    };
}
