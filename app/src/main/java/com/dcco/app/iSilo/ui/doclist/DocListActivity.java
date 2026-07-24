package com.dcco.app.iSilo.ui.doclist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dcco.app.iSilo.state.AppState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DocListActivity extends Activity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_MANAGE_STORAGE = 2;

    private LinearLayout root;
    private ListView listView;
    private TextView statusText;
    private Button actionButton;
    private List<File> entries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.docListActivity = this;

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        statusText = new TextView(this);
        statusText.setPadding(48, 48, 48, 48);
        statusText.setTextSize(18);
        statusText.setText("A carregar...");
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        actionButton = new Button(this);
        actionButton.setVisibility(View.GONE);
        root.addView(actionButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        listView = new ListView(this);
        listView.setVisibility(View.GONE);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        checkPermissionAndLoad();
    }

    private void checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (Environment.isExternalStorageManager()) {
                loadFiles();
            } else {
                statusText.setText("Necessita acesso a todos os ficheiros.\nToque em \"Conceder\" e ative \"Permitir gestão de todos os ficheiros\".");
                actionButton.setText("Conceder acesso");
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                    }
                });
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                statusText.setText("Permissão de armazenamento necessária para ler ficheiros iSilo.");
                actionButton.setText("Pedir permissão");
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                    }
                });
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        } else {
            loadFiles();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
                loadFiles();
            } else {
                statusText.setText("Acesso não concedido. Ative nas Definições > \"Permitir gestão de todos os ficheiros\".");
                actionButton.setText("Abrir Definições");
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFiles();
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                statusText.setText("Permissão negada permanentemente.\nAbra Definições > Apps > iSilo Reader > Permissões e ative Armazenamento.");
                actionButton.setText("Abrir Definições");
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });
            } else {
                statusText.setText("Permissão de armazenamento necessária.\nToque no botão para pedir novamente.");
                actionButton.setText("Pedir permissão");
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                    }
                });
            }
        }
    }

    private void loadFiles() {
        statusText.setText("A procurar ficheiros...");
        actionButton.setVisibility(View.GONE);

        File sdcard = Environment.getExternalStorageDirectory();
        File documents = new File(sdcard, "Documents");
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        entries = new ArrayList<>();
        scanDir(sdcard);
        if (!sdcard.equals(documents)) scanDir(documents);
        if (!sdcard.equals(download)) scanDir(download);

        Collections.sort(entries, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        if (entries.isEmpty()) {
            statusText.setText("Nenhum ficheiro .pdb encontrado.\nColoque ficheiros iSilo em Downloads ou Documents e toque em \"Procurar novamente\".");
            actionButton.setText("Procurar novamente");
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadFiles();
                }
            });
            listView.setVisibility(View.GONE);
            return;
        }

        statusText.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        final List<String> names = new ArrayList<>();
        for (File f : entries) names.add(f.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = entries.get(position);
                openFile(file.getAbsolutePath());
            }
        });
    }

    private void scanDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) continue;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".pdb")) {
                entries.add(f);
            }
        }
    }

    private void openFile(String filePath) {
        Intent intent = new Intent(this, com.dcco.app.iSilo.ui.reader.ReadActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }
}
