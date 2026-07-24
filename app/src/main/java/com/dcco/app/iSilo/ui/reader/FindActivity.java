package com.dcco.app.iSilo.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

public class FindActivity extends Activity {

    public static final String EXTRA_TEXT = "Text";
    public static final String EXTRA_MATCH_CASE = "MatchCase";
    public static final String EXTRA_ENTIRE_DOC = "EntireDocument";
    public static final String EXTRA_RANGE = "Range";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        final EditText searchText = new EditText(this);
        searchText.setHint("Texto a procurar");
        searchText.setSingleLine(true);
        root.addView(searchText, lp());

        final CheckBox matchCase = new CheckBox(this);
        matchCase.setText("Diferenciar maiúsculas/minúsculas");
        root.addView(matchCase, lp());

        final CheckBox entireDoc = new CheckBox(this);
        entireDoc.setText("Documento inteiro");
        root.addView(entireDoc, lp());

        final Spinner rangeSpinner = new Spinner(this);
        String[] ranges = {"Início do documento", "Posição actual", "Seleccionar intervalo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ranges);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rangeSpinner.setAdapter(adapter);
        root.addView(rangeSpinner, lp());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String text = extras.getString(EXTRA_TEXT);
            if (text != null) searchText.setText(text);
            matchCase.setChecked(extras.getBoolean(EXTRA_MATCH_CASE));
            entireDoc.setChecked(extras.getBoolean(EXTRA_ENTIRE_DOC));
            rangeSpinner.setSelection(extras.getInt(EXTRA_RANGE, 0));
        }

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        Button findBtn = new Button(this);
        findBtn.setText("Procurar");
        findBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra(EXTRA_TEXT, searchText.getText().toString());
                data.putExtra(EXTRA_MATCH_CASE, matchCase.isChecked());
                data.putExtra(EXTRA_ENTIRE_DOC, entireDoc.isChecked());
                data.putExtra(EXTRA_RANGE, rangeSpinner.getSelectedItemPosition());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        buttonRow.addView(findBtn, lp(0, 1));

        Button cancelBtn = new Button(this);
        cancelBtn.setText("Cancelar");
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        buttonRow.addView(cancelBtn, lp(0, 1));

        root.addView(buttonRow, lp());
        setContentView(root);
    }

    private static LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams lp(int w, int weight) {
        return new LinearLayout.LayoutParams(
                w == 0 ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }
}
