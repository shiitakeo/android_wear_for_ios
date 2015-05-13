Android Wear works with iPhone/iOS
===================================

Android wear can get iphon's notification.  
This app get notification using ANCS from iphone.  
so, don't need jail break iphone and rooted wear.  
I developed this app inspired by @MohammadAG's video.

Latest version
--------------
- v2.1(same as play store version '15/05/14)

```
1. support music control without jailbreak iPhone. (don't need my cydia app.but, my app is little better than BLE Utility.)
2. setting time(need rooted wear device)
3. improve reconnection seaquence
```

- v0.3(same as play store version '15/03/30)
update

```
1. install from android handheld. don't need adb.   
  use 4.4<= android handheld.
  
2. implement prototype of auto-reconnect
[check tutorial video on youtube](https://youtu.be/aPZ7UM6_aLw)
```

- v0.2.2
update

```
1. install from android handheld. don't need adb.   
  use 5.0<= android handheld.
```


- v0.2.1
update

```
1. auto screen turn off (after 1sec from getting notification.)
2. swipe to remove notification on iphone.
```

- v0.2

update
```
1. add some icons(include whatsapp)
2. screen awake from sleep when get notification
3. control calling(you can select accept or decline from your wear)
4. delete notificatin on iphone from wear.
```

Tested Devices
--------------
I have G watch R only.  
so, you check other models, please let me know.

| model | result |
|:--    |:--     |
|G Watch R| ◯ (12 hours long time test passed.)|
|G Watch  | ◯ (12 hours long time test passed.)|
|moto360|△ (can get notification, but connection is unstable. 4-5hours after connection is lost.please ambient mode turn on.(maybe moto360's BLE stack is something difflenet.)|
|Gear Live|◯ (12 hours long time test passed.)|
|SmartWatch3| ◯ (6 hours test passed.)|
|ZenWatch| ◯ (12 hours long time test passed.)|

Getting Started
---------------
[tutorial video @ Youtube.](https://www.youtube.com/watch?v=cIYe6ExIjrQ)


1. Install "BLE Utility" app to your iPhone.
2. Open Peripheral tab. don't need creating peripherals.
3. Launch the app on your Android wear.
4. Turn on "Blink" peripheral on LightBlue app.
5. after 10sec, enter pin code.
6. If success connect to iphone, success animation is played.
7. Push wear's crown to back home screen. Don't swipe Activity.
※ if do not success(arrive cloud icon or stoppeted notification), please try restart app, Bluetooth off/on, remove paring bluetooth.
8. tap "music" notification, you can control music app in iPhone.
9. If your wear is getting rooted device, your wear's is automatically setting time.

apk install
---------
### 1. install from play store
you can install from paly store.  
need 4.4<= android handheld.

- [free version](https://play.google.com/store/apps/details?id=com.shiitakeo.android_wear_for_ios)
- [donation version](https://play.google.com/store/apps/details?id=com.shiitakeo.android_wear_for_ios.donation)

### 2. install github's apk with android handheld(adb over Bluetooth).
get apk from [release page](https://github.com/shiitakeo/android_wear_for_ios/releases).  
use mobile-release.apk.  
install the apk using ADB over Bluetooth. need 4.4<= android handheld.

### 3.install github's apk without android handheld(usb adb).
get apk from [release page](https://github.com/shiitakeo/android_wear_for_ios/releases).  
use wearble-release.apk.  
install the apk using ADB.

```sh
$ adb install Wearable-release.apk
```

If you want to use moto360, check [official article](https://developer.android.com/training/wearables/apps/bt-debugging.html) for ADB over Bluetooth.

Community Support
-------
[xda thread](http://forum.xda-developers.com/android-wear/development/android-wear-ios-connectivity-t3052524)  
[my twitter account](https://twitter.com/shiitakeo)
