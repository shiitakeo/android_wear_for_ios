Android Wear works with iPhone/iOS
===================================

Android wear can get iphon's notification.  
This app get notification using ANCS from iphone.  
so, don't need jail break iphone and rooted wear.  
I developed this app inspired by @MohammadAG's video.

Tested Devices
--------------
I have G watch R only.  
so, you check other models, please let me know.

| model | result |
|:--    |:--     |
|G Watch R| ◯ (12 hours long time test passed.)|
|G Watch  | ◯(maybe long time test passed.)|
|moto360|△ (can get notification, but connection is unstable. maybe moto360's BLE stack is something difflenet.)|
|Gear Live| - (unconfirmed)|
|SmartWatch3| - (unconfirmed)|
|ZenWatch| - (unconfirmed)|

Getting Started
---------------
[tutorial video @ Youtube.](https://www.youtube.com/watch?v=cIYe6ExIjrQ)

1. Install apk to your Android wear.
2. Install "Light Blue" app to your iPhone.
3. Create virtual peripheral from "Blink" template.  
4. Launch the app on your Android wear.
5. Turn on "Blink" peripheral on LightBlue app.
6. If success connect to iphone, success animation is played.
7. Push wear's crown to back home screen. **Don't swipe Activity.**  

apk install
---------
get apk from [release page](https://github.com/shiitakeo/android_wear_for_ios/releases/download/0.1/wear-release.apk).
install the apk using ADB.

```sh
$ adb install Wearable-release.apk
```

If you want to use moto360, check [official article](https://developer.android.com/training/wearables/apps/bt-debugging.html) for ADB over Bluetooth.

Community Support
-------
[xda thread](http://forum.xda-developers.com/android-wear/development/android-wear-ios-connectivity-t3052524)  
[my twitter account](https://twitter.com/shiitakeo)
