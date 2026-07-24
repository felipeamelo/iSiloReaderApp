package com.dcco.app.iSilo.ui.reader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dcco.app.iSilo.engine.format.DocFormat;
import com.dcco.app.iSilo.engine.format.DocFormats;
import com.dcco.app.iSilo.engine.format.PalmDBImpl;
import com.dcco.app.iSilo.engine.data.FileDataStream;
import com.dcco.app.iSilo.engine.util.DebugLog;
import com.dcco.app.iSilo.state.AppState;

public class ReadActivity extends Activity {

    private static final int REQUEST_FIND = 1001;

    private ReadView readView;
    private DocFormat doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.readActivity = this;

        readView = new ReadView(this);
        readView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(readView);
        setContentView(layout);

        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            openFile(filePath);
        }
    }

    private void openFile(String filePath) {
        doc = loadDocument(filePath);
        if (doc != null) {
            try {
                readView.openDocument(doc);
            } catch (Exception e) {
                copyLogToClipboard("openFile_error");
                Toast.makeText(this, "Erro: " + e.getMessage() + " | Log copiado", Toast.LENGTH_LONG).show();
            }
        }
        copyLogToClipboard("openFile_done");
        String log = DebugLog.get();
        int logLen = log.length();
        if (logLen > 0) {
            String preview = log.length() > 80 ? log.substring(0, 80) + "..." : log;
            Toast.makeText(this, "Log copiado (" + logLen + " chars): " + preview, Toast.LENGTH_LONG).show();
        }
    }

    private DocFormat loadDocument(String filePath) {
        DebugLog.clear();
        DebugLog.add("LOAD_DOC", "filePath=%s", filePath);
        try {
            FileDataStream stream = new FileDataStream();
            int res = stream.open(filePath, 0);
            if (res < 0) {
                Toast.makeText(this, "Erro ao abrir ficheiro: " + res, Toast.LENGTH_LONG).show();
                return null;
            }

            PalmDBImpl pdb = new PalmDBImpl();
            res = pdb.Open(stream, 0);
            if (res < 0) {
                stream.Close();
                Toast.makeText(this, "Erro ao ler PalmDB: " + res, Toast.LENGTH_LONG).show();
                return null;
            }

            DocFormat doc = DocFormats.openFormat(pdb);
            pdb.Destroy();
            if (doc == null) {
                String diag = DocFormats.lastDiagnostic;
                String msg = "Formato não reconhecido";
                if (!diag.isEmpty()) msg += ": " + diag;
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return null;
            }
            if (doc.getInfo() != null) {
                String title = doc.getInfo().title;
                if (title == null) title = "(sem titulo)";
                String diag = DocFormats.lastDiagnostic;
                String diagMsg = !diag.isEmpty() ? " [" + diag + "]" : "";
                String enc = " cs=" + doc.getInfo().charset + " rawEnc=0x" + Integer.toHexString(doc.getInfo().rawEncodingFlags);
                Toast.makeText(this, title + diagMsg + enc, Toast.LENGTH_LONG).show();
            }
            return doc;
        } catch (Exception e) {
            String log = DebugLog.get();
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("DebugLog", log));
            } catch (Exception ignored) {}
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            String preview = log.length() > 100 ? log.substring(0, Math.min(100, log.length())) + "..." : log;
            Toast.makeText(this, "Excepção: " + msg + " | Log copiado (" + log.length() + " chars)", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        copyLogToClipboard("onDestroy");
        super.onDestroy();
        if (doc != null) doc.close();
        AppState.readActivity = null;
    }

    private void copyLogToClipboard(String reason) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String log = DebugLog.get();
            if (log.length() > 0) {
                cm.setPrimaryClip(ClipData.newPlainText("DebugLog", log));
                android.util.Log.d("ReadActivity", "Log copied (" + log.length() + " chars) reason=" + reason);
            }
        } catch (Exception ignored) {}
    }

    private void showFindDialog() {
        Intent intent = new Intent(this, FindActivity.class);
        startActivityForResult(intent, REQUEST_FIND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FIND && resultCode == RESULT_OK && data != null) {
            String query = data.getStringExtra(FindActivity.EXTRA_TEXT);
            boolean entireDoc = data.getBooleanExtra(FindActivity.EXTRA_ENTIRE_DOC, false);
            int range = data.getIntExtra(FindActivity.EXTRA_RANGE, 0);

            if (query == null || query.isEmpty()) return;
            if (doc == null) return;

            int startOffset = 0;
            if (!entireDoc && range == 1) {
                int currentPage = readView.getCurrentPage();
                if (currentPage > 0) {
                    startOffset = readView.getCurrentPageStartOffset();
                }
            }

            int[] matchOffset = new int[1];
            int[] matchLength = new int[1];
            int res = doc.findString(query, startOffset, matchOffset, matchLength);

            if (res >= 0 && doc.getInfo() != null) {
                int page = readView.findPageAtOffset(matchOffset[0]);
                readView.goToPage(page);
                Toast.makeText(this, "Encontrado na página " + (page + 1),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Texto não encontrado",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Procurar")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, 2, 0, "Copiar Log");
        if (doc != null && doc.getInfo() != null && doc.getInfo().tocTitles != null
                && doc.getInfo().tocTitles.length > 0) {
            menu.add(0, 3, 0, "Índice");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            showFindDialog();
            return true;
        }
        if (item.getItemId() == 2) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String log = DebugLog.get();
            ClipData clip = ClipData.newPlainText("DebugLog", log);
            clipboard.setPrimaryClip(clip);
            String preview = log.length() > 200 ? log.substring(0, 200) + "..." : log;
            Toast.makeText(this, "Log copiado (" + log.length() + " chars): " + preview, Toast.LENGTH_LONG).show();
            return true;
        }
        if (item.getItemId() == 3) {
            showTOC();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTOC() {
        if (doc == null || doc.getInfo() == null || doc.getInfo().tocTitles == null) return;
        final String[] titles = doc.getInfo().tocTitles;
        final int[] offsets = doc.getInfo().tocOffsets;
        new android.app.AlertDialog.Builder(this)
                .setTitle("Índice")
                .setItems(titles, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which < offsets.length) {
                            int page = readView.findPageAtOffset(offsets[which]);
                            readView.goToPage(page);
                        }
                    }
                })
                .setPositiveButton("Fechar", null)
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            if (readView != null) readView.nextPage();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            if (readView != null) readView.previousPage();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
