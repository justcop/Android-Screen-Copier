package dev.local.uiprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)+");

    private EditText targetInput;
    private TextView status;
    private TextView report;
    private TextView hint;

    private TextView text(String content, int size) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(size);
        view.setPadding(0, 10, 0, 10);
        return view;
    }

    private Button button(String content, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(content);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        return b;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int dp = (int) (16 * getResources().getDisplayMetrics().density);
        column.setPadding(dp, dp, dp, dp);
        scroll.addView(column);

        TextView title = text("UI Probe", 24);
        title.setTypeface(null, Typeface.BOLD);
        column.addView(title);
        column.addView(text("On-demand local inspection of accessible UI elements from one app you select. No screenshots, network access, or automatic app interaction.", 15));

        column.addView(text("Target app package identifier", 16));
        targetInput = new EditText(this);
        targetInput.setSingleLine(true);
        targetInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        targetInput.setHint("example.app.package");
        targetInput.setText(prefs().getString("target_package", ""));
        column.addView(targetInput);
        column.addView(button("1. Save target app", v -> saveTarget()));

        status = text("", 16);
        column.addView(status);
        column.addView(button("2. Open Accessibility settings", v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        column.addView(button("3. Start two-minute probe", v -> startProbe()));
        column.addView(button("Stop probing", v -> {
            prefs().edit().putBoolean("armed", false).apply();
            refresh();
        }));
        column.addView(button("Refresh result", v -> refresh()));
        column.addView(button("Copy anonymised counts only", v -> {
            String summary = prefs().getString("summary", "No result yet");
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("UI Probe counts", summary));
            hint.setText("Copied counts only. No UI text copied.");
        }));
        column.addView(button("Delete local result", v -> {
            prefs().edit().remove("report").remove("summary").apply();
            refresh();
        }));

        hint = text("Open your chosen app while the probe is active, navigate to the intended screen and scroll a little. Return here and press Refresh. The detailed report can contain sensitive text. Do not share it without redaction.", 14);
        column.addView(hint);

        TextView reportTitle = text("Latest accessibility result (private, on device)", 18);
        reportTitle.setTypeface(null, Typeface.BOLD);
        column.addView(reportTitle);
        report = text("", 13);
        report.setTextIsSelectable(true);
        report.setTypeface(Typeface.MONOSPACE);
        report.setMovementMethod(new ScrollingMovementMethod());
        column.addView(report);
        setContentView(scroll);
        refresh();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("probe", MODE_PRIVATE);
    }

    private void saveTarget() {
        String requested = targetInput.getText().toString().trim();
        if (!PACKAGE_PATTERN.matcher(requested).matches()) {
            hint.setText("Enter a valid Android package identifier, such as example.app.package.");
            return;
        }
        prefs().edit()
                .putString("target_package", requested)
                .putBoolean("armed", false)
                .remove("report")
                .remove("summary")
                .apply();
        hint.setText("Target saved. Start the probe after enabling Accessibility.");
        refresh();
    }

    private void startProbe() {
        String target = prefs().getString("target_package", "");
        if (target.isEmpty()) {
            hint.setText("First enter and save a target app package identifier.");
            return;
        }
        if (!targetInput.getText().toString().trim().equals(target)) {
            hint.setText("Save the edited target identifier before starting.");
            return;
        }
        prefs().edit()
                .putBoolean("armed", true)
                .putLong("expires", System.currentTimeMillis() + 120_000L)
                .remove("report")
                .remove("summary")
                .apply();
        hint.setText("Probe armed for two minutes. Open the chosen app and navigate to the intended screen.");
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        SharedPreferences p = prefs();
        long expires = p.getLong("expires", 0);
        boolean armed = p.getBoolean("armed", false) && System.currentTimeMillis() < expires;
        if (p.getBoolean("armed", false) && !armed) p.edit().putBoolean("armed", false).apply();
        String state = armed ? "ARMED until " + DateFormat.getTimeInstance().format(new Date(expires))
                             : "Not recording";
        status.setText("Target: " + p.getString("target_package", "Not set")
                + "\nStatus: " + state + "\nAccessibility service: "
                + (ProbeAccessibilityService.connected ? "Connected" : "Not connected"));
        report.setText(p.getString("report", "No result yet. Set a target, enable Accessibility and start a probe."));
    }
}
