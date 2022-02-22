package com.woleapp.netpos.taponphone.implementations;

import android.util.Log;

import com.mastercard.terminalsdk.iso8825.BerTlv;
import com.mastercard.terminalsdk.listeners.TransactionOutcomeObserver;
import com.mastercard.terminalsdk.objects.ErrorIndication;
import com.mastercard.terminalsdk.objects.OutcomeParameterSet;
import com.mastercard.terminalsdk.objects.ReaderOutcome;
import com.mastercard.terminalsdk.utility.ByteUtility;
import com.netpluspay.nibssclient.models.CardData;
import com.netpluspay.nibssclient.util.TripleDES;

import com.pixplicity.easyprefs.library.Prefs;
import com.woleapp.netpos.taponphone.listener.TransactionListener;
import com.woleapp.netpos.util.ExtensionFunctionsKt;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

import timber.log.Timber;

public class OutcomeObserver implements TransactionOutcomeObserver {

    private static final String TAG = "OutcomeObserver";

    private ReaderOutcome mTransactionOutcome;

    private TransactionListener mTransactionListenerForUI;
    private StringBuilder builder = new StringBuilder();


    @Override
    public void transactionOutcome(final ReaderOutcome readerOutcome) {
        mTransactionOutcome = readerOutcome;
        Log.i(TAG, "Transaction Summary : " + mTransactionOutcome.toString());
        builder.append("Transaction Summary : " + mTransactionOutcome.toString());
        OutcomeParameterSet.Status transactionStatus = readerOutcome.getOutcomeParameterSet().getStatus();

        Timber.e(transactionStatus.getMessage());
        if (transactionStatus == OutcomeParameterSet.Status.END_APPLICATION) {
            //readerOutcome.
        }
        boolean isDataRecordPresent = readerOutcome.getOutcomeParameterSet().isDataRecordPresent();

        Timber.e("data record present %s", isDataRecordPresent);
        builder.append("\ndata record present: " + isDataRecordPresent);
        String track2Data = "";
        String applicationPanSequenceNumber = "";
        String pan = "";
        StringBuilder nibssIccData = new StringBuilder();
        if (isDataRecordPresent) {
            // Extract collection of BerTlv(s)
            ArrayList<BerTlv> data = readerOutcome.getDataRecordContents();
            ArrayList<String> nibssTag = new ArrayList<>();
            nibssTag.add("9F26");
            nibssTag.add("9F27");
            nibssTag.add("9F10");
            nibssTag.add("9F37");
            nibssTag.add("9F36");
            nibssTag.add("95");
            nibssTag.add("9A");
            nibssTag.add("9C");
            nibssTag.add("9F02");
            nibssTag.add("5F2A");
            nibssTag.add("82");
            nibssTag.add("9F1A");
            nibssTag.add("9F34");
            nibssTag.add("9F33");
            nibssTag.add("9F35");
            nibssTag.add("9F1E");
            nibssTag.add("84");
            nibssTag.add("9F09");
            nibssTag.add("9F03");
            nibssTag.add("5F34");
            nibssTag.add("8E");


            // Look for the required TLV
            for (BerTlv berTlv : data) {
                Timber.e(ByteUtility.byteArrayToHexString(ByteUtility.intToByteArray(berTlv.getNTag())).concat(" - ").concat(berTlv.toHexString()));
                String tag = ByteUtility.byteArrayToHexString(ByteUtility.intToByteArray(berTlv.getNTag())).trim();
                if (tag.equals("57"))
                    track2Data = berTlv.toHexString().substring(4);
                if (tag.equals("5F34"))
                    applicationPanSequenceNumber = berTlv.toHexString().substring(6);
                if (tag.equals("5A"))
                    pan = berTlv.toHexString().substring(4);
                if (nibssTag.contains(tag)) {
//                    if (tag.equals("9F34")){
//                        StringBuilder myName = new StringBuilder(berTlv.toHexString().substring(6));
//                        myName.setCharAt(1, '2');
//                        nibssIccData.append("9F3403").append(myName);
//                        continue;
//                    }
                    nibssIccData.append(berTlv.toHexString());
                }
//               if (berTlv.getNTag() == Tags.AMOUNT_AUTHORIZED_NUMERIC.getTag()) {
//                    ByteArrayWrapper value = berTlv.getRawBytes();
//                    Timber.e(new String(value.getBytes()));
//                    builder.append("\n" + new String(value.getBytes()));
//                }
            }
            Timber.e("track2Data is: %s", track2Data);
            Timber.e("pan sequence is: %s", StringUtils.leftPad(applicationPanSequenceNumber, 3, '0'));
            Timber.e("nibssicc is %s", nibssIccData.toString());
        }

        Timber.e("reader outcome");
        builder.append("\nreader outcome");
        builder.append("\ntags size: " + readerOutcome.getDataRecordContents().size());
        builder.append("\ndataRecordTlv: " + readerOutcome.getDataRecordTlv().toString());
        builder.append("\nCVM: " + readerOutcome.getOutcomeParameterSet().getCvm());
        builder.append("\nadditional information size: " + readerOutcome.getAdditionalInformation().size());
        builder.append("\ndis: " + readerOutcome.getDiscretionaryData().toTLV().toString());
        builder.append("\ndis size %s" + readerOutcome.getDiscretionaryData().getAdditionalInformation().size());
        Timber.e("tags size: %s", readerOutcome.getDataRecordContents().size());
        Timber.e("dataRecordTlv: %s", readerOutcome.getDataRecordTlv().toString());
        Timber.e("CVM: %s", readerOutcome.getOutcomeParameterSet().getCvm());
        Timber.e("additional information size %s", readerOutcome.getAdditionalInformation().size());
        Timber.e("dis %s", readerOutcome.getDiscretionaryData().toTLV().toString());
        Timber.e("dis size %s", readerOutcome.getDiscretionaryData().getAdditionalInformation().size());
        logOutcome(readerOutcome);
        mTransactionListenerForUI.logToScreen(builder.toString());
        // Process the outcome Returned from Library
        CardData cardData = new CardData(
                track2Data,
                nibssIccData.toString(),
                StringUtils.leftPad(applicationPanSequenceNumber, 3, '0'),
                "051"
        );
        processOutcome(cardData, pan);
    }



    private void logOutcome(ReaderOutcome readerOutcome) {

        Log.e("OUTCOME", "\nReceived Outcome :");
    }

    private void processOutcome(CardData cardData, String pan) {

        //status Approved and Online - considered as Success

        if (mTransactionListenerForUI != null) {
            switch (mTransactionOutcome.getOutcomeParameterSet().getStatus()) {

                case APPROVED:
                    mTransactionListenerForUI.onTransactionSuccessful();
                    break;
                case ONLINE_REQUEST:
                    mTransactionListenerForUI.onOnlineReferral(cardData, pan);
                    break;
                case DECLINED:
                    mTransactionListenerForUI.onTransactionDeclined();
                    break;
                case END_APPLICATION:
                    ErrorIndication errorIndication = mTransactionOutcome.getDiscretionaryData().getErrorIndication();
                    Timber.e(errorIndication.toString());
                    if (errorIndication.getL3Error() == ErrorIndication.L3_Error_Code.STOP)
                        mTransactionListenerForUI.onTransactionCancelled();
                    else
                        mTransactionListenerForUI.onApplicationEnded();

                    break;
                default:
                    mTransactionListenerForUI.onApplicationEnded();
            }
        }
    }

    public void resetObserver(TransactionListener transactionListenerForUI) {
        mTransactionOutcome = null;
        mTransactionListenerForUI = transactionListenerForUI;
    }
}
