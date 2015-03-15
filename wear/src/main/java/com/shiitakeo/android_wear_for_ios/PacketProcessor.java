package com.shiitakeo.android_wear_for_ios;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * Created by kusabuka on 15/03/15.
 */
public class PacketProcessor {
    private static final String TAG_LOG = "BLE_wear";

    private String ds_app_id;
    private String ds_title;
    private String ds_message;
    private ByteArrayOutputStream processing_attribute_value;

    private int remain_packet_size;
    private int remain_next_packet_byte;

    private enum ds_notification_processing_status{
        init, app_id, title, message, finish
    };
    private ds_notification_processing_status processing_status;

    PacketProcessor(){
        processing_attribute_value = new ByteArrayOutputStream();
        init();
    }

    public void init(){
        processing_status = processing_status.init;
        remain_packet_size = 0;
        remain_next_packet_byte = 0;
        ds_app_id = null;
        ds_title = null;
        ds_message = null;
        processing_attribute_value.reset();
    }

    public void processing(byte[] get_data){
        remain_packet_size = get_data.length;


        int index_att_id;
        while (remain_packet_size > 0) {
            if(processing_status.equals(processing_status.init)){
                //parse first packet include type, uid, app id length/value
//                        att1_id, length
                index_att_id = 5;

                //get att0's length
                int att_length = get_attribute_length(get_data, index_att_id);
                Log.d(TAG_LOG, "$ app id att length: " + att_length);

                int current_packet_att_length = get_data.length - (index_att_id + 3);
                Log.d(TAG_LOG, "$ current_packet_att_length: " + current_packet_att_length);

                if (current_packet_att_length < att_length) {
                    // fragmentations is occure
                    processing_attribute_value.write(get_data, index_att_id + 3, current_packet_att_length);
                    remain_next_packet_byte = att_length - current_packet_att_length;
                    remain_packet_size = 0;
                    processing_status = processing_status.app_id;
                    Log.d(TAG_LOG, "$ app id value is fragment. remain:: " + remain_next_packet_byte);
                } else {
                    //just finish reading app id
                    Log.d(TAG_LOG, att_length + " : " + current_packet_att_length);
                    processing_attribute_value.write(get_data, index_att_id + 3, att_length);
                    remain_packet_size = current_packet_att_length - att_length;
                    remain_next_packet_byte = 0;
                    Log.d(TAG_LOG, "$ app id value is just finish reading. remain:: " + remain_packet_size);

                    update_processing_status();
                }
                Log.d(TAG_LOG, "remain packet size: " + remain_packet_size);
                Log.d(TAG_LOG, "remain next packet size: " + remain_next_packet_byte);
            } else {
                //parse 1st packet remain or 2nd~packet( includefragment data)
                Log.d(TAG_LOG, "else: remain_next_packet size: " + remain_next_packet_byte);

                //2nd~ packet.
                if (remain_next_packet_byte > 0) {
                    Log.d(TAG_LOG, "read fragment data" + remain_next_packet_byte);
                    //read fragment data, continue from pre packet
                    if (remain_next_packet_byte > remain_packet_size) {
                        //continue fragment
                        processing_attribute_value.write(get_data, 0, remain_packet_size);
                        remain_next_packet_byte -= remain_packet_size;
                        remain_packet_size = 0;
                        Log.d(TAG_LOG, "$$ fragment is continue. remain:: " + remain_next_packet_byte);
                    } else {
                        //just finish fragment
                        Log.d(TAG_LOG, "$$  remain next packet seizet:: " + remain_next_packet_byte);
                        processing_attribute_value.write(get_data, 0, remain_next_packet_byte);
                        remain_packet_size -= remain_next_packet_byte;
                        remain_next_packet_byte = 0;
                        Log.d(TAG_LOG, "$$  att value is just finish reading. remai_current_packet:: " + remain_packet_size);

                        update_processing_status();
                    }
                } else {
                    Log.d(TAG_LOG, "$$$ remain packet size: " + remain_packet_size);
                    Log.d(TAG_LOG, "$$$ remain next packet size: " + remain_next_packet_byte);
                    //continue rading current packet data, or just finish reading data in pre packet
                    //continue reading current packet data
                    if (remain_packet_size > 0) {
                        //get next att's length
                        index_att_id = get_data.length - remain_packet_size;

                        //get next attr's length
                        int att_length = get_attribute_length(get_data, index_att_id);
                        Log.d(TAG_LOG, "$$$ next att length: " + att_length);

                        int current_packet_att_length = get_data.length - (index_att_id + 3);

                        //check fragment
                        if (current_packet_att_length < att_length) {
                            // fragmentation is occured
                            processing_attribute_value.write(get_data, index_att_id + 3, current_packet_att_length);
                            remain_next_packet_byte = att_length - current_packet_att_length;
                            remain_packet_size = 0;
                            Log.d(TAG_LOG, "$$$ att value is fragment. remain:: " + remain_next_packet_byte);
                        } else {
                            // no fragmentation
                            Log.d(TAG_LOG, att_length + " : " + current_packet_att_length);
                            processing_attribute_value.write(get_data, index_att_id + 3, att_length);
                            remain_packet_size = current_packet_att_length - att_length;
                            remain_next_packet_byte = 0;
                            Log.d(TAG_LOG, "$ app id value is just finish reading. remain:: " + remain_packet_size);

                            update_processing_status();
                        }
                    } else {
                        Log.d(TAG_LOG, "$$$$ ");
                    }
                }
            }

        }
    }


    public String get_ds_app_id(){
        return ds_app_id;
    }

    public String get_ds_title(){
        return ds_title;
    }

    public String get_ds_message(){
        return ds_message;
    }

    public Boolean is_finish_processing(){
        return processing_status.equals(processing_status.finish);
    }

    private int get_attribute_length(byte[] _get_data, int _index_att){
        //get att0's length
        byte[] byte_ds_length = {_get_data[_index_att + 2], _get_data[_index_att + 1]};
        BigInteger ds_length_big = new BigInteger(byte_ds_length);
        return ds_length_big.intValue();
    }

    private void update_processing_status(){
        switch (processing_status) {
            case init:
                Log.d(TAG_LOG, "$$$ init -> app_id.");
                processing_status = processing_status.title;
                break;

            case app_id:
                Log.d(TAG_LOG, "$$$ finish app id reading.");
                processing_status = processing_status.title;
                try {
                    ds_app_id = new String(processing_attribute_value.toByteArray(), "UTF-8");
                    processing_attribute_value.reset();
                    Log.d(TAG_LOG, "$$$ app_id : " + ds_app_id);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case title:
                Log.d(TAG_LOG, "$$$ finish title  reading.");
                processing_status = processing_status.message;
                try {
                    ds_title = new String(processing_attribute_value.toByteArray(), "UTF-8");
                    processing_attribute_value.reset();
                    Log.d(TAG_LOG, "$$$ title : " + ds_title);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            case message:
                Log.d(TAG_LOG, "$$ finish messgage  reading.");
                processing_status = processing_status.finish;
                try {
                    ds_message = new String(processing_attribute_value.toByteArray(), "UTF-8");
                    processing_attribute_value.reset();
                    Log.d(TAG_LOG, "$$ message : " + ds_message);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Log.d(TAG_LOG, "$$.");
                break;
        }
    }
}
