package dev.local.uiprobe;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;

public class ProbeAccessibilityService extends AccessibilityService {
    public static volatile boolean connected = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastCapture = 0;
    private boolean scheduled = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connected = true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        SharedPreferences p = getSharedPreferences("probe", MODE_PRIVATE);
        String target = p.getString("target_package", "");
        if (target.isEmpty() || !target.contentEquals(event.getPackageName())) return;
        if (!p.getBoolean("armed", false)) return;
        long now = System.currentTimeMillis();
        if (now > p.getLong("expires", 0)) {
            p.edit().putBoolean("armed", false).apply();
            return;
        }
        if (scheduled || now - lastCapture < 900) return;
        scheduled = true;
        handler.postDelayed(() -> {
            scheduled = false;
            capture();
        }, 300);
    }

    private void capture() {
        SharedPreferences p = getSharedPreferences("probe", MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!p.getBoolean("armed", false) || now > p.getLong("expires", 0)) return;
        String target = p.getString("target_package", "");
        if (target.isEmpty()) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null ||
                !target.contentEquals(root.getPackageName())) return;
        lastCapture = now;

        int nodes = 0, textNodes = 0, descNodes = 0, idNodes = 0;
        int editableNodes = 0, scrollableNodes = 0;
        StringBuilder samples = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int lines = 0;
        while (!queue.isEmpty() && nodes < 500) {
            AccessibilityNodeInfo node = queue.removeFirst();
            nodes++;
            if (node.isVisibleToUser()) {
                String label = "";
                if (!node.isPassword() && !node.isEditable() && node.getText() != null) {
                    label = node.getText().toString();
                }
                String description = "";
                if (!node.isPassword() && !node.isEditable() && node.getContentDescription() != null) {
                    description = node.getContentDescription().toString();
                }
                String id = node.getViewIdResourceName();
                if (!label.isEmpty()) textNodes++;
                if (!description.isEmpty()) descNodes++;
                if (id != null) idNodes++;
                if (node.isEditable()) editableNodes++;
                if (node.isScrollable()) scrollableNodes++;
                if ((!label.isEmpty() || !description.isEmpty() || id != null) && lines < 180) {
                    Rect bounds = new Rect();
                    node.getBoundsInScreen(bounds);
                    samples.append("#").append(nodes)
                            .append(" ").append(safe(node.getClassName(), 60))
                            .append(" [").append(bounds.left).append(",")
                            .append(bounds.top).append(",")
                            .append(bounds.right).append(",").append(bounds.bottom).append("]\n");
                    if (!label.isEmpty()) samples.append("  text: ").append(safe(label, 220)).append("\n");
                    if (!description.isEmpty()) samples.append("  description: ").append(safe(description, 220)).append("\n");
                    if (id != null) samples.append("  id: ").append(safe(id, 120)).append("\n");
                    lines++;
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(child);
            }
        }
        String summary = "Captured: " + DateFormat.getDateTimeInstance().format(new Date(now))
                + "\nNodes inspected: " + nodes
                + "\nVisible nodes with text: " + textNodes
                + "\nVisible nodes with content descriptions: " + descNodes
                + "\nVisible nodes with resource IDs: " + idNodes
                + "\nEditable nodes (text intentionally hidden): " + editableNodes
                + "\nScrollable nodes: " + scrollableNodes;
        String report = summary + "\n\nVISIBLE UI ELEMENTS\n" + samples
                + "\nOnly currently loaded elements are reported. Passwords and editable drafts are excluded.";
        p.edit().putString("summary", summary).putString("report", report).apply();
    }

    private static String safe(CharSequence value, int max) {
        if (value == null) return "";
        String s = value.toString().replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        connected = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
