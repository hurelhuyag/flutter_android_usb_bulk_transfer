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
    private static final int WRITE_TIMEOUT_MS = 30_000;
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
            Log.d(TAG, "Requesting USB permission");

            var intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(usbReceiver, intentFilter, 0);

            var permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(usbDevice, permissionIntent);
            return;
        }

        // Use the first interface that has a bulk OUT endpoint; interface 0 is not always the printer one.
        for (int i = 0; i < usbDevice.getInterfaceCount() && endpointOut == null; i++) {
            var intf = usbDevice.getInterface(i);
            UsbEndpoint out = null;
            UsbEndpoint in = null;
            for (int j = 0; j < intf.getEndpointCount(); j++) {
                var ep = intf.getEndpoint(j);
                if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    continue;
                }
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT && out == null) {
                    out = ep;
                } else if (ep.getDirection() == UsbConstants.USB_DIR_IN && in == null) {
                    in = ep;
                }
            }
            if (out != null) {
                Log.d(TAG, "Using interface " + i + ", bulk OUT found, bulk IN " + (in != null ? "found" : "missing"));
                usbInterface = intf;
                endpointOut = out;
                endpointIn = in;
            }
        }
        if (endpointOut == null) {
            Log.e(TAG, "No bulk OUT endpoint found");
            return;
        }

        connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
            boolean claimed = connection.claimInterface(usbInterface, true);
            Log.d(TAG, "Interface claimed: " + claimed);
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    /// Returns false when the data could not be fully sent. The printer may then be stuck in the middle of a command
    /// (bulkTransfer returns -1 on timeout without saying how many bytes went out), so the caller must treat the
    /// printer as out of sync.
    public boolean write(byte[] buf) {
        if (connection == null || endpointOut == null) {
            Log.d(TAG, "Printer not connected");
            return false;
        }

        var chunkSize = 4096;
        var offset = 0;
        while (offset < buf.length) {
            var len = Math.min(buf.length - offset, chunkSize);
            Log.d(TAG, "Sending: offset:" + offset + ", len:" + len);
            // The printer stops accepting data while busy printing a large bitmap, so allow a generous timeout.
            var sent = connection.bulkTransfer(endpointOut, buf, offset, len, WRITE_TIMEOUT_MS);
            Log.d(TAG, "Send Status: " + sent);
            if (sent <= 0) {
                Log.e(TAG, "Send failed at offset " + offset + " of " + buf.length);
                return false;
            }
            offset += sent;
        }
        return true;
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
