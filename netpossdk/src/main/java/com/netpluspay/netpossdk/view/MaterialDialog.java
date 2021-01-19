package com.netpluspay.netpossdk.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.netpluspay.netpossdk.R;

public class MaterialDialog {
    private Context context;
    private MaterialAlertDialogBuilder builder;
    int select = 0;

    public MaterialDialog(Context context) {
        this.builder = new MaterialAlertDialogBuilder(context, R.style.BottomSheetDialog);
        this.context = context;
    }

    public void showInputDialog(String title, String defaultText, final OnInputListener inputCallback) {
        this.showInputDialog(title, null, null, defaultText, InputType.TYPE_CLASS_TEXT, inputCallback, null);
    }

    public void showInputDialog(String title, String defaultText, final int inputType,
                                final OnInputListener inputCallback) {
        this.showInputDialog(title, null, null, defaultText, inputType, inputCallback, null);
    }

    public void showInputDialog(String title, String defaultText, final int inputType,
                                final OnInputListener inputCallback, final OnInputCancelListener inputCancelListener) {
        this.showInputDialog(title, null, null, defaultText, inputType, inputCallback, inputCancelListener);
    }

    public void showInputDialog(String title, String hint, String message,
                                final int inputType, final OnInputListener inputCallback) {
        this.showInputDialog(title, hint, message, null, inputType, inputCallback, null);
    }

    public void showInputDialog(String title, String hint, String message, String defaultText,
                                final int inputType, final OnInputListener inputCallback,
                                final OnInputCancelListener onInputCancelListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.layout_input, null);
        final EditText input = view.findViewById(android.R.id.input);
        if (input == null) return;
        input.setHint(hint);
        input.setSingleLine();
//        input.setTextColor(builder.contentColor);
        input.setInputType(inputType);
        input.setText(defaultText);
        input.setSelection(input.length());
        if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }

        Dialog dialog = builder.setTitle(title)
                .setMessage(message)
                .setView(view)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (TextUtils.isEmpty(input.getText())) {
                            if ((inputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
                                inputCallback.onInput("0");
                            } else {
                                inputCallback.onInput(input.getText());
                            }
                        } else {
                            inputCallback.onInput(input.getText());
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (onInputCancelListener != null)
                            onInputCancelListener.onCancel();
                    }
                })
                .show();

        if (onInputCancelListener != null) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    public void showConfirmDialog(String title, String message, final OnConfirmListener onConfirmListener) {
        Dialog dialog =  builder.setTitle(title).setMessage(message)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onConfirmListener.onConfirm();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialogInterface, int i) {
                        onConfirmListener.onCancel();
                    }
                }).show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });
    }

    public void showListConfirmChoseDialog(String title, String[] list, final OnChoseListener onChoseListener) {
        Dialog dialog = builder.setTitle(title).setSingleChoiceItems(list, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        select = i;
                    }
                })
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onChoseListener.onChose(select);
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialogInterface, int i) {
                        onChoseListener.onChose(-1);
                    }
                }).show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });
    }

    public void showListSimpleChoseDialog(String title, String[] list, final OnChoseListener onChoseListener) {
        this.showListSimpleChoseDialog(title, list, 0, onChoseListener);
    }

    public void showListSimpleChoseDialog(String title, String[] list, int checkedItem, final OnChoseListener onChoseListener) {
        builder.setTitle(title).setSingleChoiceItems(list, checkedItem,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        select = i;
                    }
                })
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onChoseListener.onChose(select);
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    /*public void showSignatureDialog(String title, final OnSignatureListener onSignatureListener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.layout_signature, null);
        final HandWriteView handwriteview = view.findViewById(R.id.handWriteView);
        if (handwriteview == null) return;
        Dialog dialog = builder.setTitle(title)
                .setView(view)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onSignatureListener.onSignature(handwriteview.getCacheBitmap());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onSignatureListener.onCancel();
                    }
                })
                .show();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });
    }*/

    public interface OnConfirmListener {
        void onConfirm();

        void onCancel();
    }

    public interface OnChoseListener {
        void onChose(int i);
    }

    public interface OnInputListener {
        void onInput(CharSequence input);
    }

    public interface OnInputCancelListener {
        void onCancel();
    }

    public interface OnSignatureListener {
        void onSignature(Bitmap bitmap);

        void onCancel();
    }
}