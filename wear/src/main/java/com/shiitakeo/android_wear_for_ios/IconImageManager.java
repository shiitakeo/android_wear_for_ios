package com.shiitakeo.android_wear_for_ios;

/**
 * Created by kusabuka on 15/03/15.
 */
public class IconImageManager {
    private static final int line = R.drawable.line;
    private static final int phone = R.drawable.call;
    private static final int facebook = R.drawable.facebook;
    private static final int messanger = R.drawable.messenger;
    private static final int twitter = R.drawable.twitter;
    private static final int gmail = R.drawable.gmail;
    private static final int app_icon = R.drawable.ic_launcher;

    IconImageManager(){

    }

    public int get_image_index(String _app_id){
        switch (_app_id) {
            case "com.apple.mobilephone":
                return phone;
            case "com.apple.MobileSMS":
                return messanger;
            case "com.google.Gmail":
                return gmail;
            case "jp.naver.line":
                return line;
            case "facebook":
                return facebook;
            case "twitter":
                return twitter;
            default:
                return app_icon;
        }
    }
}
