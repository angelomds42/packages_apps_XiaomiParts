## Current features

| Category | Feature | Description | QS Tile |
| --- | --- | --- | --- |
| **Display** | `Automatic high brightness mode (HBM)` | Enable peak luminance based on sunlight | Yes |
|  | `Automatic DC Dimming` | Enable anti-flicker mode based on screen brightness | Yes |
|  | `Saturation` | Control the saturation level of the display | Yes |
| **Sound** | `Clear Speaker` | Plays a 30-second audio to clear the speaker. | No |
| **Battery** | `Dynamic Thermal Profiles` | Optimize thermal management by setting specific profiles for each app. | No |

## Including XiaomiParts

- Clone this repository to packages/apps/XiaomiParts directory in your AOSP build tree:

```
croot && git clone https://github.com/angelomds42/packages_apps_XiaomiParts packages/apps/XiaomiParts
```

- Include the app during compilation by adding the following to device-*.mk:

```
# XiaomiParts
include packages/apps/XiaomiParts/device.mk
```

This line includes the [device.mk](https://github.com/angelomds42/packages_apps_XiaomiParts/blob/fifteen/device.mk) file from the XiaomiParts repository, which will add the XiaomiParts application and the necessary security policies (sepolicies) to your AOSP build during compilation.

- Create an overlay for packages/apps/XiaomiParts/res/values/config.xml in your device tree and customize the values.

# Overlay configs
Feature support and device paths are defined via overlays. This allows you to enable or disable features and provide the correct paths for your device without modifying the XiaomiParts code:

```
packages/apps/XiaomiParts/res/values/config.xml
```

- You can use the original [config.xml](https://github.com/angelomds42/packages_apps_XiaomiParts/blob/fifteen/res/values/config.xml) file as a reference.

## Testing changes

- When testing new changes, it is much faster to compile the application standalone and update it manually rather than running a full AOSP build. Please note that some changes may require you to chmod 0666 sysfs nodes and set selinux to permissive. When compiling a full AOSP build, this is not needed assuming the init cmds and sepolicies have been properly configured.

Lunch your device and run the following cmd:

```
m XiaomiParts
```
- This also assumes you are already running an AOSP build including XiaomiParts as a priv-app in /system_ext.

## Credits

| Work                                                        | Author                                                                      |
| ----------------------------------------------------------- | --------------------------------------------------------------------------- |
| Pixel Parts                                                 | [AnierinBliss](https://github.com/Anierinbliss)                             |
| CustomSeekBar preference                                    | [Neobuddy89](https://forum.xda-developers.com/m/neobuddy89.3795148/)        |
| Original AutoHBMService                                     | [Hikari no Tenshi](https://forum.xda-developers.com/m/hikari-no-tenshi.4337348/) & [maxwen](https://forum.xda-developers.com/m/maxwen.4683552/) |
