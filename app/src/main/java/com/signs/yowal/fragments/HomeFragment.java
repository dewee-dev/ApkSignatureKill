package com.signs.yowal.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.signs.yowal.utils.BinSignatureTool;
import com.signs.yowal.utils.MyAppInfo;
import com.signs.yowal.utils.Preferences;
import com.signs.yowal.utils.SignatureTool;
import com.tianyu.killer.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class HomeFragment extends Fragment {
    public static AppCompatEditText apkPath;
    private AppCompatImageView apkIcon;
    private AppCompatTextView apkName;
    private AppCompatTextView apkPack;
    private AppCompatCheckBox binSignatureTool;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.activity_main, container, false);

        apkIcon = mView.findViewById(R.id.apkIcon);
        apkName = mView.findViewById(R.id.apkName);
        apkPack = mView.findViewById(R.id.apkPackage);
        apkPath = mView.findViewById(R.id.apkPath);

        apkPath.setText(Preferences.isApkPath());
        apkPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
                if (!p1.toString().isEmpty()) {
                    File apk = new File(p1.toString());
                    if (apk.exists()) {
                        apkIcon.setImageDrawable(new MyAppInfo(getContext(), apk.getAbsolutePath()).getIcon());
                        apkName.setText(MyAppInfo.getAppName());
                        apkPack.setText(MyAppInfo.getPackage());
                    } else {
                        apkIcon.setImageResource(R.mipmap.ic_launcher);
                        apkName.setText("Select apk");
                        apkPack.setText("none");
                    }
                }
            }
        });

        binSignatureTool = mView.findViewById(R.id.binMtSignatureKill);
        binSignatureTool.setChecked(Preferences.getBinMtSignatureKill());
        binSignatureTool.setOnCheckedChangeListener((p1, p2) -> {
            Preferences.setBinMtSignatureKill(p2);
        });

        (mView.findViewById(R.id.browseApk)).setOnClickListener(p1 -> {
            browseApk();
        });

        (mView.findViewById(R.id.hookRun)).setOnClickListener(p1 -> {
            hookRun();
        });

        return mView;
    }

    public void browseApk() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"apk", "APK"};
        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties);
        dialog.setTitle("Выберите установочный пакет");
        dialog.setDialogSelectionListener(files -> {
            for (String path : files) {
                File file = new File(path);
                if (file.getName().endsWith(".apk") || file.getName().endsWith(".APK")) {
                    apkPath.setText(file.getAbsolutePath());
                } else {
                    toast("Error");
                }
            }
        });
        dialog.show();
    }

    public void hookRun() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Обработка...");
        progressDialog.setMessage("Подождите...");
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                progressDialog.dismiss();
            }
        };

        new Thread() {
            public void run() {
                Looper.prepare();
                String srcApk = apkPath.getText().toString();
                String outApk = apkPath.getText().toString().replace(".apk", "_kill.apk");

                new File(outApk);
                try {
                    if (Preferences.getBinMtSignatureKill()) {
                        BinSignatureTool binSignatureTool = new BinSignatureTool(getContext());
                        binSignatureTool.setPath(srcApk, outApk);
                        binSignatureTool.Kill();
                    } else {
                        SignatureTool signatureTool = new SignatureTool(getContext());
                        signatureTool.setPath(srcApk, outApk);
                        signatureTool.Kill();
                    }
                    toast("Обработка завершена, подпишите самостоятельно" + outApk);
                    dialogFinished();
                } catch (Exception e) {
                    toast("Обработка не удалась:" + e.toString());
                }
                mHandler.sendEmptyMessage(0);
                Looper.loop();
            }
        }.start();
    }

    public void toast(String str) {
        Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
    }

    public void dialogFinished() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        alertDialog.setTitle("Обработка завершена");
        alertDialog.setMessage("Файл был сохранен в каталоге, подпишите его");
        alertDialog.setCancelable(true);
        alertDialog.setPositiveButton("Ок", null);
        alertDialog.show();
    }
}
