![example](https://raw.githubusercontent.com/Evolution-XYZ-Devices/packages_apps_PixelParts/udc/readme_resources/PixelParts.png)

## Current features

| Category | Feature | Description | QS Tile | Required kernel changes |
| --- | --- | --- | --- | --- |
| **Display** | `Automatic high brightness mode (HBM)` | Enable peak luminance based on sunlight | Yes | N/A |
|  | `Saturation` | Control the saturation level of the display | Yes | N/A |
| **Leds** | `Pixel torch` | Adjust the brightness of the PixelParts flashlight QS-Tile | PixelParts Flashlight QS | N/A |

## Including PixelParts

- Clone this repository to packages/apps/PixelParts directory in your AOSP build tree:

```
croot && git clone https://github.com/Evolution-X-Devices/packages_apps_PixelParts packages/apps/PixelParts
```

- Include the app during compilation by adding the following to device-*.mk:

[Commit 1/1 (device tree)](https://github.com/Evolution-X-Devices/device_google_bluejay/commit/6822dabe27de84fb7d52e85cb34d9a71c14d1112)

```
# PixelParts
include packages/apps/PixelParts/device.mk
```

This line includes the [device.mk](https://github.com/Evolution-XYZ-Devices/packages_apps_PixelParts/blob/udc/device.mk) file from the PixelParts repository, which will add the PixelParts application, its initialization script (init.rc), and the necessary security policies (sepolicies) to your AOSP build during compilation.

## Testing changes

- When testing new changes, it is much faster to compile the application standalone and update it manually rather than running a full AOSP build. Please note that some changes may require you to chmod 0666 sysfs nodes and set selinux to permissive. When compiling a full AOSP build, this is not needed assuming the init cmds and sepolicies have been properly configured.

Lunch your device and run the following cmd:

```
m PixelParts
```
- This also assumes you are already running an AOSP build including PixelParts as a priv-app in /system_ext.

## Credits

| Work                                                        | Author                                                                      |
| ----------------------------------------------------------- | --------------------------------------------------------------------------- |
| CustomSeekBar preference                                    | [Neobuddy89](https://forum.xda-developers.com/m/neobuddy89.3795148/)        |
| Original AutoHBMService                                     | [Hikari no Tenshi](https://forum.xda-developers.com/m/hikari-no-tenshi.4337348/) & [maxwen](https://forum.xda-developers.com/m/maxwen.4683552/) |
