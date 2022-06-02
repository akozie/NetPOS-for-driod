package com.woleapp.netpos.contactless.taponphone.mastercard.implementations;

import android.util.Log;

import com.mastercard.terminalsdk.emv.Tag;
import com.mastercard.terminalsdk.iso8825.BerTlv;
import com.mastercard.terminalsdk.listeners.ScriptProvider;
import com.mastercard.terminalsdk.objects.ApplicationInput;
import com.mastercard.terminalsdk.utility.ByteArrayWrapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


/**
 * This script provider is optimized for initatePayment API of TerminalSDK
 * if used with readCard API of TerminalSDK, current implementation WILL go into an infinite loop
 */
public class ScriptImplementation implements ScriptProvider {

    private final String TAG = "ScriptImplementation";

    boolean mFlag = true;

    byte[] tagstoRead = {(byte) 0x9F, 0x75, (byte) 0x9F, 0x76};

    boolean activateWrite = true;

    @Override
    public ApplicationInput onDataReceived(ArrayList<BerTlv> dataSent,
                                           final ArrayList<BerTlv> dataNeeded) {

        Log.d(TAG, "onDataReceived: " + dataSent);

        final ArrayList<BerTlv> dataRequested = new ArrayList<>();

        for (BerTlv berTlv : dataNeeded) {
            Tag requestedTag = berTlv.getTagObject();
            dataRequested.add(new BerTlv(requestedTag, new ByteArrayWrapper(
                    new byte[requestedTag.getMinLen()])));
        }

        return new ApplicationInput() {

            @Override
            public ArrayList<BerTlv> tlvsToWriteBeforeGenAC() {

                byte[] ba9F76 = {(byte) 0x9F, 0x76};
                Tag tag9f76 = new Tag(ba9F76, Tag.Format.b, 0, 255,
                        "Unprotected DataStore 2");
                BerTlv tlv9F76 = new BerTlv(
                        tag9f76,
                        new ByteArrayWrapper(
                                "48656c6c6f20576f726c64207772697474656e206f6e20" + getCurrentDateTime()));

                ArrayList<BerTlv> tagsToWrite = new ArrayList<>();
                if (activateWrite) {
                    tagsToWrite.add(tlv9F76);
                }
                return tagsToWrite;
            }

            @Override
            public ArrayList<BerTlv> tlvsToWriteAfterGenAC() {
                byte[] ba9F75 = {(byte) 0x9F, 0x75};
                Tag tag9f75 = new Tag(ba9F75, Tag.Format.b, 0, 255,
                        "Unprotected DataStore 1");
                BerTlv tlv9F75 = new BerTlv(
                        tag9f75,
                        new ByteArrayWrapper(
                                "48656c6c6f20576f726c64207772697474656e206f6e20" + getCurrentDateTime()));


                ArrayList<BerTlv> tagsToWrite = new ArrayList<>();
                if (activateWrite) {
                    tagsToWrite.add(tlv9F75);
                }
                return tagsToWrite;
            }

            @Override
            public byte[] tagsToRead() {
                byte[] toBeRead = getTagsToRead();
                resetTagstoRead();
                return toBeRead;
            }

            @Override
            public boolean continueWithTransaction() {
                return true;
            }

            @Override
            public ArrayList<BerTlv> additionalTlvs() {
                return dataRequested;
            }
        };

    }

    private void resetTagstoRead() {
        mFlag = false;
    }

    private byte[] getTagsToRead() {

        if (mFlag) // if intending to use readCard API, this condition should be based on "mFlag"
            return tagstoRead;
        else
            return new byte[0];
    }

    private final String getCurrentDateTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String formattedDate = df.format(Calendar.getInstance().getTime());
        return formattedDate;
    }
}
