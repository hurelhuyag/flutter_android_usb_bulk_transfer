## 0.0.3

* Raise the black/white cutoff for `writePdf` from 50% to 70% luminance. Logos with light gradients now print
  whole, white text stays readable on grey and coloured fills, and light coloured or grey text no longer disappears.
  QR codes and black text print the same, with slightly bolder edges.
* `writePdf` takes optional `threshold` (0-1, default 0.7) and `dithering` parameters. `dithering` is
  `Dithering.none` (default: solid black and white for logos, QR codes and text), `Dithering.floydSteinberg()` or
  `Dithering.atkinson()`. Both algorithms take an optional `strength` (0-1, default 1) to keep more of the image
  solid.
* README: "How logos print" shows sample logos converted at each setting.

## 0.0.2

* Fix gibberish output when printing long PDFs: the raster image is now sent as 256-row `GS v 0` bands instead of
  one command, which exceeded the printer's maximum image height.
* Send `ESC @` (printer reset) before each PDF image so leftover printer state from earlier jobs is cleared.
* Fix off-by-one dot in the last byte of each image row.
* Use a luminance threshold for black/white conversion, so anti-aliased PDF text no longer prints bold and blotchy.
* Increase the USB write timeout from 5 s to 30 s and resume partial transfers instead of aborting.
* **Behavior change:** `write` and `writePdf` now throw a `PlatformException` with code `WRITE_FAILED` when the
  data could not be fully sent, instead of silently reporting success. Treat the printer as out of sync when this
  happens.
* Fix USB permission request on Android 14+: the permission receiver is registered with `RECEIVER_NOT_EXPORTED`
  (previously threw `SecurityException`), and on API 24-25 uses the 2-arg `registerReceiver` overload.
* Fix USB permission result always reading as denied: the permission `PendingIntent` is now mutable with an
  explicit package, so the system can attach `EXTRA_PERMISSION_GRANTED`.
* **Breaking:** `connect` now returns `Future<bool>` and completes only after the connection is established or has
  failed, waiting for the user's answer when the USB permission dialog is shown. It completes with `false` when the
  device is not found, permission is denied, or the device can't be opened. Previously it returned before the
  permission dialog was answered and `connect` had to be called a second time.
* The permission receiver is registered once and unregistered after the answer, instead of leaking a receiver per
  `connect` call.
* Select the first interface with a bulk OUT endpoint instead of assuming interface 0.
* Close the previous connection when `connect` is called again.

## 0.0.1

* Initial release: list USB devices, connect by vendor/product ID, raw bulk `write`/`read`, `writePdf` (renders
  the first PDF page as an ESC/POS raster image), `disconnect` and `isConnected`.
