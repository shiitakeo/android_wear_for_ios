package com.shiitakeo.android_wear_for_ios;

/**
 * Created by kusabuka on 15/03/15.
 */
public class IconImageManager {
    private static final int app_icon = R.drawable.ic_launcher;
    private static final int line = R.drawable.line;
    private static final int phone = R.drawable.call;
    private static final int facebook = R.drawable.facebook;
    private static final int messanger = R.drawable.messenger;
    private static final int twitter = R.drawable.twitter;
    private static final int gmail = R.drawable.gmail;
    private static final int whatsapp = R.drawable.whatsapp;
    private static final int mail = R.drawable.mail;
    private static final int calendar = R.drawable.calendar;
    private static final int hangouts = R.drawable.hangouts;

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
            case "com.tapbots.Tweetbot":
                return twitter;
            case "com.tapbots.Tweetbot3":
                return twitter;
            case "net.whatsapp.WhatsApp":
                return whatsapp;
            case "com.apple.mobilemail":
                return mail;
            case "com.apple.mobilecal":
                return calendar;
            case "com.google.hangouts":
                return hangouts;
            default:
                return app_icon;
        }
    }
}
