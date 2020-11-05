package com.netpluspay.kozenlib.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;

import com.netpluspay.kozenlib.R;
import com.netpluspay.kozenlib.utils.DeviceConfig;
import com.netpluspay.kozenlib.utils.tlv.HexUtil;
import com.pos.sdk.emvcore.POIEmvCoreManager;
import com.pos.sdk.emvcore.POIEmvCoreManager.EmvPinConstraints;
import com.pos.sdk.security.POIHsmManage;
import com.pos.sdk.security.PedRsaPinKey;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class PasswordDialog {
    public static final int OfflinePin = 11;
    public static final int OnlineEncryptPin = 22;
    public static final int OfflineEncryptPin = 33;

    protected String DEFAULT_EXP_PIN_LEN_IND = "0,4,5,6,7,8,9,10,11,12";
    protected int DEFAULT_TIMEOUT_MS = 30000;

    private Handler handler;
    private int tpkIndex;
    private int icSlot;

    private boolean isEncryptCard;
    private int keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK;
    private String pinCard;
    private boolean pinByPass;
    private int pinTryCnt;
    private int pinType;
    private byte[] pinIccRandomData;
    private byte[] pinModData;
    private byte[] pinExpData;
    private Listener pinListener;

    private String title;
    private String message;

    private POIHsmManage hsmManage;
    private PinEventListener pinEventListener;

    private Dialog dialog;
    private TextView tvTitle, tvMessage;
    private EditText etPin;
    private Button btnConfirm;
    private ImageView btnClear;
    private TextView btnEsc, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private Group groupKeyboard;

    public PasswordDialog(Activity context, boolean isIcSlot, Bundle mBundle, int tpkIndex) {
        this.hsmManage = POIHsmManage.getDefault();
        this.pinEventListener = new PinEventListener();
        this.pinListener = null;
        if (isIcSlot) {
            this.icSlot = 0;
        } else {
            this.icSlot = 10;
        }
        this.tpkIndex = tpkIndex;

        switch (mBundle.getInt(EmvPinConstraints.PINTYPE, -1)) {
            case POIEmvCoreManager.PIN_TYPE_ONLINE_PIN:
                pinType = OnlineEncryptPin;
                break;
            case POIEmvCoreManager.PIN_TYPE_OFF_PIN:
                pinType = OfflinePin;
                break;
            case POIEmvCoreManager.PIN_TYPE_OFF_CIPHER_PIN:
                pinType = OfflineEncryptPin;
                break;
            default:
                break;
        }

        if (mBundle.containsKey(EmvPinConstraints.PINENCRYPTCARD)) {
            isEncryptCard = true;
            pinCard = mBundle.getString(EmvPinConstraints.PINENCRYPTCARD);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINCARD)) {
            pinCard = mBundle.getString(EmvPinConstraints.PINCARD);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINALLOWBYPASS)) {
            pinByPass = mBundle.getBoolean(EmvPinConstraints.PINALLOWBYPASS);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINOFFTRYCNT)) {
            pinTryCnt = mBundle.getInt(EmvPinConstraints.PINOFFTRYCNT);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINPUBEXP)) {
            pinExpData = mBundle.getByteArray(EmvPinConstraints.PINPUBEXP);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINPUBMODEL)) {
            pinModData = mBundle.getByteArray(EmvPinConstraints.PINPUBMODEL);
        }

        if (mBundle.containsKey(EmvPinConstraints.PINCARDRND)) {
            pinIccRandomData = mBundle.getByteArray(EmvPinConstraints.PINCARDRND);
        }

        switch (pinType) {
            case OnlineEncryptPin:
                title = "Enter Online PIN";
                break;
            case OfflinePin:
            case OfflineEncryptPin:
                title = "Enter Offline PIN";
                if (pinTryCnt > 1) {
                    message = "PIN " + pinTryCnt + " ";
                } else if (pinTryCnt == 1) {
                    message = "The Last Times";
                }
                break;
            default:
                break;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        ConstraintLayout view = (ConstraintLayout) inflater.inflate(R.layout.layout_password, null);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvMessage = view.findViewById(R.id.tvMessage);
        etPin = view.findViewById(R.id.etPin);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        btnClear = view.findViewById(R.id.btnClear);
        btnEsc = view.findViewById(R.id.btnEsc);
        btn0 = view.findViewById(R.id.btn0);
        btn1 = view.findViewById(R.id.btn1);
        btn2 = view.findViewById(R.id.btn2);
        btn3 = view.findViewById(R.id.btn3);
        btn4 = view.findViewById(R.id.btn4);
        btn5 = view.findViewById(R.id.btn5);
        btn6 = view.findViewById(R.id.btn6);
        btn7 = view.findViewById(R.id.btn7);
        btn8 = view.findViewById(R.id.btn8);
        btn9 = view.findViewById(R.id.btn9);
        groupKeyboard = view.findViewById(R.id.groupKeyboard);

        if (DeviceConfig.isHardwareKeyboard) {
            groupKeyboard.setVisibility(View.GONE);
        }

        tvTitle.setText(title);
        tvMessage.setText(message);
        dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        window.setAttributes(wlp);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.BOTTOM);
        dialog.show();
    }

    public void showDialog() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }
        };

        int ret = -1;
        switch (pinType) {
            case OfflinePin:
                ret = PedVerifyPlainPin();
                break;
            case OnlineEncryptPin:
                ret = PedOnlinePin();
                break;
            case OfflineEncryptPin:
                ret = PedVerifyCipherPin();
                break;
            default:
                break;
        }

        if (ret == -1) {
            return;
        }

        try {
            Looper.getMainLooper().loop();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        }
    }

    private int PedVerifyPlainPin() {
        hsmManage.registerListener(pinEventListener);
        return this.hsmManage.PedVerifyPlainPin(icSlot, 0, DEFAULT_TIMEOUT_MS, DEFAULT_EXP_PIN_LEN_IND);
    }

    private int PedVerifyCipherPin() {
        hsmManage.registerListener(pinEventListener);
        if (pinModData == null) {
            return -1;
        }
        byte[] modData = new byte[pinModData.length];
        byte[] expData = new byte[pinExpData.length];
        byte[] iccRandomData = new byte[pinIccRandomData.length];
        System.arraycopy(pinModData, 0, modData, 0, pinModData.length);
        System.arraycopy(pinExpData, 0, expData, 0, pinExpData.length);
        System.arraycopy(pinIccRandomData, 0, iccRandomData, 0, pinIccRandomData.length);

        PedRsaPinKey mPed = new PedRsaPinKey(modData, expData, iccRandomData);
        return this.hsmManage.PedVerifyCipherPin(icSlot, 0, DEFAULT_TIMEOUT_MS, DEFAULT_EXP_PIN_LEN_IND, mPed);
    }

    private int PedOnlinePin() {
        hsmManage.registerListener(pinEventListener);

        byte[] pPan = new byte[24];
        if (!isEncryptCard) {
            String tPan = pinCard;
            String pan;
            if (tPan == null) {
                pan = "0000000000000000";
            } else if (tPan.length() >= 13) {
                pan = tPan;
            } else {
                String fmt = String.format("%%0%dd", 16 - tPan.length());
                String val = String.format(fmt, 0);
                pan = val + tPan;
            }
            pan = pan.substring(pan.length() - 13, pan.length() - 1);
            int idx = 0;
            int leftLen = 16 - pan.length();
            for (idx = 0; idx < leftLen; idx++) {
                pan = "0" + pan;
            }
            byte[] pTmp = pan.getBytes();
            System.arraycopy(pTmp, 0, pPan, 0, 16);
        } else {
            byte[] pTmp = pinCard.getBytes();
            System.arraycopy(pTmp, 0, pPan, 0, 16);
        }

        byte pFmtData[] = {0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(pFmtData, 0, pPan, 16, 8);

        return hsmManage.PedGetPinBlock(keyMode, tpkIndex, 0, DEFAULT_TIMEOUT_MS, pPan, DEFAULT_EXP_PIN_LEN_IND);
    }

    private class PinEventListener implements POIHsmManage.EventListener {
        private String TAG = "PinEventListener";

        String info = "";

        @Override
        public void onPedVerifyPin(POIHsmManage arg0, int arg1,
                                   byte[] pRead) {
            Log.d(TAG, "onPedVerifyPin :" + Integer.toHexString(arg1));

            if (arg1 == 0x87) {
                int sw1 = (pRead[1] >= 0 ? pRead[1] : (pRead[1] + 256));
                int sw2 = (pRead[2] >= 0 ? pRead[2] : (pRead[2] + 256));
                Log.e(TAG, "Read is " + HexUtil.toHexString(pRead));

                if (sw1 == 0x90 && sw2 == 0x00) {
                    onPinConfirm(EmvPinConstraints.EMV_VERIFY_SUC, null, null);
                } else if (sw1 == 0x63 && (sw2 & 0xc0) == (int) 0xc0) {
                    if ((sw2 & 0x0F) == 0) {
                        onPinError(EmvPinConstraints.EMV_VERIFY_PIN_BLOCK, 0);
                    } else {
                        onPinError(EmvPinConstraints.EMV_VERIFY_PIN_ERROR, sw2 & 0x0F);
                    }
                } else if (sw1 == 0x69 && (sw2 == (int) 0x83 || sw2 == (int) 0x84)) {
                    onPinError(EmvPinConstraints.EMV_VERIFY_PIN_BLOCK, 0);
                } else {
                    onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
                }
            } else if (arg1 == 0x86) {
                int sw1 = (pRead[1] >= 0 ? pRead[1] : (pRead[1] + 256));
                int sw2 = (pRead[2] >= 0 ? pRead[2] : (pRead[2] + 256));
                Log.e(TAG, "Read is " + HexUtil.toHexString(pRead, 0, pRead.length));

                if (sw1 == 0x90 && sw2 == 0x00) {
                    onPinConfirm(EmvPinConstraints.EMV_VERIFY_SUC, null, null);
                } else if (sw1 == 0x63 && (sw2 & 0xc0) == (int) 0xc0) {
                    if ((sw2 & 0x0F) == 0) {
                        onPinError(EmvPinConstraints.EMV_VERIFY_PIN_BLOCK, 0);
                    } else {
                        onPinError(EmvPinConstraints.EMV_VERIFY_PIN_ERROR, sw2 & 0x0F);
                    }
                } else if (sw1 == 0x69 && (sw2 == (int) 0x83 || sw2 == (int) 0x84)) {
                    onPinError(EmvPinConstraints.EMV_VERIFY_PIN_BLOCK, 0);
                } else {
                    onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
                }
            } else {
                Log.d(TAG, "other else");
                onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
            }
            endPwd();
        }

        @Override
        public void onPedPinBlockRet(POIHsmManage arg0, int arg1,
                                     byte[] pRead) {
            Log.e(TAG, "onPedPinBlockRet " + HexUtil.toHexString(pRead, 0, pRead.length));

            if (pRead[0] != 0) {
                byte[] pinBlock = new byte[pRead[0]];
                System.arraycopy(pRead, 1, pinBlock, 0, pRead[0]);
                if (pRead.length > (pRead[0] + 1)) {
                    byte[] ksn = new byte[pRead[pRead[0] + 1]];
                    System.arraycopy(pRead, pRead[0] + 2, ksn, 0, pRead[pRead[0] + 1]);
                    onPinConfirm(EmvPinConstraints.EMV_VERIFY_SUC, pinBlock, ksn);
                } else {
                    onPinConfirm(EmvPinConstraints.EMV_VERIFY_SUC, pinBlock, null);
                }
            }
            endPwd();
        }

        @Override
        public void onKeyboardShow(POIHsmManage arg0, byte[] arg1,
                                   int timeout) {
            Log.e(TAG, "onKeyboardShow");
            hsmManage.PedSetKeyLayout(calculation(arg1), 0);
        }

        @Override
        public void onKeyboardInput(POIHsmManage secMgr, int numKeys) {
            info = "";
            while (0 != (numKeys--))
                info += "*";
            if (info.length() <= 12)
                etPin.setText(info);
        }

        @Override
        public void onInfo(POIHsmManage arg0, int arg1, int arg2) {
            Log.e(TAG, "onInfo");
        }

        @Override
        public void onError(POIHsmManage secMgr, int what, final int extra) {
            Log.e(TAG, "onError:" + extra);
            switch (extra) {
                case 65235:
                    tvMessage.setText("The terminal did not write the PIN key. Please check.");
                    break;
                case 0xffff:
                case 0xFFFC:
                    tvMessage.setText("The terminal triggers a security check.");
                    break;
                case 65202:
                case 65231:// -305
                    if (pinByPass) {
                        onPinError(EmvPinConstraints.EMV_VERIFY_NO_PASSWORD, 0);
                    } else {
                        onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
                    }
                    endPwd();
                    return;
                default:
                    onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
                    endPwd();
                    return;
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (pinByPass) {
                        onPinError(EmvPinConstraints.EMV_VERIFY_NO_PASSWORD, 0);
                    } else {
                        onPinError(EmvPinConstraints.EMV_VERIFY_NO_PINPAD, 0);
                    }
                    endPwd();
                }
            }, 2000);
        }

        @Override
        public void onHwSelfCheckRet(POIHsmManage arg0, int arg1, int arg2) {
            Log.e(TAG, "onHwSelfCheckRet");
        }

        @Override
        public void onHwSensorTriggered(POIHsmManage arg0, int arg1,
                                        byte[] arg2, byte[] arg3) {
            Log.e(TAG, "onHwSensorTriggered");
        }

        @Override
        public void onPedKeyManageRet(POIHsmManage arg0, int arg1) {
            Log.e(TAG, "onPedKeyManageRet");
        }

    }

    private byte[] calculation(byte[] keys) {
        HashMap<String, String> map = new HashMap<String, String>();
        ByteBuffer coordinate = ByteBuffer.allocate(104);

        String esc = "Esc";
        String enter = "Enter";
        String clear = "Clear";

        map.put("0", "0");
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");
        map.put("4", "4");
        map.put("5", "5");
        map.put("6", "6");
        map.put("7", "7");
        map.put("8", "8");
        map.put("9", "9");
        map.put("-21", esc);
        map.put("-35", enter);
        map.put("-40", clear);

        TextView[] keyView = new TextView[11];
        keyView[0] = btn0;
        keyView[1] = btn1;
        keyView[2] = btn2;
        keyView[3] = btn3;
        keyView[4] = btn4;
        keyView[5] = btn5;
        keyView[6] = btn6;
        keyView[7] = btn7;
        keyView[8] = btn8;
        keyView[9] = btn9;
        keyView[10] = btnEsc;
        ImageView ivClear = btnClear;
        Button btnConfirm = this.btnConfirm;
        int viewIndex = 0;

        for (int i = 0; i <= 12; i++) {
            String value = map.get(String.valueOf(keys[i] - 0x30) + "");
            View tv = null;
            if (value.equals(enter)) {
                tv = btnConfirm;
            } else if (value.equals(clear)) {
                tv = ivClear;
            } else {
                keyView[viewIndex].setText(value);
                tv = keyView[viewIndex++];
            }

            byte[] pos = new byte[8];
            int[] location = new int[2];
            tv.getLocationOnScreen(location);
            int leftx = location[0];
            int lefty = location[1];
            int rightx = location[0] + tv.getWidth();
            int righty = location[1] + tv.getHeight();
            byte[] tmp = intToBytes(leftx);
            byte[] tmp1 = intToBytes(lefty);
            byte[] tmp2 = intToBytes(rightx);
            byte[] tmp3 = intToBytes(righty);
            pos[0] = tmp[2];
            pos[1] = tmp[3];
            pos[2] = tmp1[2];
            pos[3] = tmp1[3];
            pos[4] = tmp2[2];
            pos[5] = tmp2[3];
            pos[6] = tmp3[2];
            pos[7] = tmp3[3];
            coordinate.put(pos);
        }
        return coordinate.array();
    }

    private byte[] intToBytes(int value) {
        byte[] src = new byte[]{(byte) (value >> 24 & 255),
                (byte) (value >> 16 & 255), (byte) (value >> 8 & 255),
                (byte) (value & 255)};
        return src;
    }

    private void endPwd() {
        dialog.dismiss();
        hsmManage.unregisterListener(pinEventListener);
        Message m = handler.obtainMessage();
        handler.sendMessage(m);
    }

    private void onPinConfirm(int verifyResult, byte[] pinBlock, byte[] pinKsn) {
        if (pinListener != null) {
            pinListener.onConfirm(verifyResult, pinBlock, pinKsn);
            return;
        }

        Bundle mPinInput = new Bundle();
        mPinInput.putInt(EmvPinConstraints.OUTPINOFFTRYCNT, 0);
        mPinInput.putInt(EmvPinConstraints.OUTPINVERIFYRESULT, verifyResult);

        if (pinBlock != null) {
            mPinInput.putByteArray(EmvPinConstraints.OUTPINBLOCK, pinBlock);
        }

        if (pinKsn != null) {
//                mTransViewModel.pinBlockKsn = HexUtil.toHexString(pinksn);
        }
        POIEmvCoreManager.getDefault().onSetConfirmPin(mPinInput);
    }

    private void onPinError(int verifyResult, int pinTryCntOut) {
        if (pinListener != null) {
            pinListener.onError(verifyResult, pinTryCntOut);
            return;
        }

        Bundle mPinInput = new Bundle();
        mPinInput.putInt(EmvPinConstraints.OUTPINOFFTRYCNT, pinTryCntOut);
        mPinInput.putInt(EmvPinConstraints.OUTPINVERIFYRESULT, verifyResult);
        POIEmvCoreManager.getDefault().onSetConfirmPin(mPinInput);
    }

    public void setPinListener(Listener pinListener) {
        this.pinListener = pinListener;
    }

    public void setKeyMode(int keyMode) {
        this.keyMode = keyMode;
    }

    public interface Listener {

        void onConfirm(int verifyResult, byte[] pinBlock, byte[] pinKsn);

        void onError(int verifyResult, int pinTryCntOut);
    }
}
