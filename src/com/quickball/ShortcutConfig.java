package com.quickball;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShortcutConfig {

    private static final String PREFS_NAME = "quickball_shortcuts";
    private static final String KEY_SHORTCUTS = "shortcuts_json";

    // --- Data model ---

    public static class Action {
        public String type;  // launch_package, uri, action, action_extras, component
        public String pkg;
        public String uri;
        public String action;
        public String extraKey;
        public String extraValue;
        public String className;

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("type", type);
            if (pkg != null) o.put("pkg", pkg);
            if (uri != null) o.put("uri", uri);
            if (action != null) o.put("action", action);
            if (extraKey != null) o.put("extraKey", extraKey);
            if (extraValue != null) o.put("extraValue", extraValue);
            if (className != null) o.put("className", className);
            return o;
        }

        public static Action fromJson(JSONObject o) throws JSONException {
            Action a = new Action();
            a.type = o.getString("type");
            a.pkg = o.optString("pkg", null);
            a.uri = o.optString("uri", null);
            a.action = o.optString("action", null);
            a.extraKey = o.optString("extraKey", null);
            a.extraValue = o.optString("extraValue", null);
            a.className = o.optString("className", null);
            return a;
        }
    }

    public static class Item {
        public String label;
        public String color;
        public String type;       // "app" or "system"
        public String systemKey;  // for type="system", e.g. "toggle_data_sim"
        public List<Action> actions = new ArrayList<>();

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("label", label);
            o.put("color", color);
            o.put("type", type != null ? type : "app");
            if (systemKey != null) o.put("systemKey", systemKey);
            JSONArray arr = new JSONArray();
            for (Action a : actions) {
                arr.put(a.toJson());
            }
            o.put("actions", arr);
            return o;
        }

        public static Item fromJson(JSONObject o) throws JSONException {
            Item item = new Item();
            item.label = o.getString("label");
            item.color = o.getString("color");
            item.type = o.optString("type", "app");
            item.systemKey = o.optString("systemKey", null);
            JSONArray arr = o.optJSONArray("actions");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    item.actions.add(Action.fromJson(arr.getJSONObject(i)));
                }
            }
            return item;
        }
    }

    // --- Persistence ---

    public static List<Item> load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SHORTCUTS, null);
        if (json != null) {
            try {
                return fromJsonArray(json);
            } catch (JSONException e) {
                // corrupted, fall through to defaults
            }
        }
        return getDefaults();
    }

    public static void save(Context ctx, List<Item> items) {
        try {
            String json = toJsonArray(items);
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SHORTCUTS, json).apply();
        } catch (JSONException e) {
            // should not happen
        }
    }

    public static void initIfNeeded(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_SHORTCUTS)) {
            save(ctx, getDefaults());
        }
    }

    public static void resetToDefaults(Context ctx) {
        save(ctx, getDefaults());
    }

    // --- JSON helpers ---

    private static String toJsonArray(List<Item> items) throws JSONException {
        JSONArray arr = new JSONArray();
        for (Item item : items) {
            arr.put(item.toJson());
        }
        return arr.toString();
    }

    private static List<Item> fromJsonArray(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        List<Item> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(Item.fromJson(arr.getJSONObject(i)));
        }
        return list;
    }

    // --- Default shortcuts (mirrors original hardcoded ones) ---

    public static List<Item> getDefaults() {
        List<Item> list = new ArrayList<>();

        // 1. Toggle data SIM
        Item sim = new Item();
        sim.label = "切换流量卡";
        sim.color = "#FF6600";
        sim.type = "system";
        sim.systemKey = "toggle_data_sim";
        list.add(sim);

        // 2. Alipay payment code
        list.add(makeApp("支付宝付款码", "#1677FF",
            makeAction("uri", "com.eg.android.AlipayGphone",
                "alipays://platformapi/startapp?appId=20000056", null, null, null, null),
            makeAction("launch_package", "com.eg.android.AlipayGphone",
                null, null, null, null, null)));

        // 3. WeChat payment code
        list.add(makeApp("微信付款码", "#07C160",
            makeAction("action_extras", "com.tencent.mm",
                null, "com.tencent.mm.ui.ShortCutDispatchAction",
                "LauncherUI.Shortcut.LaunchType", "launch_type_offline_wallet", null),
            makeAction("uri", "com.tencent.mm",
                "weixin://dl/offlinepay", null, null, null, null),
            makeAction("launch_package", "com.tencent.mm",
                null, null, null, null, null)));

        // 4. Alipay scan
        list.add(makeApp("支付宝扫一扫", "#1677FF",
            makeAction("uri", "com.eg.android.AlipayGphone",
                "alipays://platformapi/startapp?appId=10000007", null, null, null, null),
            makeAction("launch_package", "com.eg.android.AlipayGphone",
                null, null, null, null, null)));

        // 5. WeChat scan
        list.add(makeApp("微信扫一扫", "#07C160",
            makeAction("action_extras", "com.tencent.mm",
                null, "com.tencent.mm.ui.ShortCutDispatchAction",
                "LauncherUI.Shortcut.LaunchType", "launch_type_scan_qrcode", null),
            makeAction("uri", "com.tencent.mm",
                "weixin://dl/scan", null, null, null, null),
            makeAction("launch_package", "com.tencent.mm",
                null, null, null, null, null)));

        // 6. Hellobike scan
        list.add(makeApp("哈啰扫码", "#00B2FF",
            makeAction("action", "com.jingyao.easybike",
                null, "com.jingyao.easybike.driverlife.scan_code", null, null, null),
            makeAction("launch_package", "com.jingyao.easybike",
                null, null, null, null, null)));

        return list;
    }

    private static Item makeApp(String label, String color, Action... actions) {
        Item item = new Item();
        item.label = label;
        item.color = color;
        item.type = "app";
        for (Action a : actions) {
            item.actions.add(a);
        }
        return item;
    }

    private static Action makeAction(String type, String pkg, String uri,
            String action, String extraKey, String extraValue, String className) {
        Action a = new Action();
        a.type = type;
        a.pkg = pkg;
        a.uri = uri;
        a.action = action;
        a.extraKey = extraKey;
        a.extraValue = extraValue;
        a.className = className;
        return a;
    }

    // --- Known app shortcuts (for popular apps) ---

    public static List<Item> getKnownShortcuts(String packageName) {
        List<Item> list = new ArrayList<>();
        switch (packageName) {
            case "com.tencent.mm": // WeChat
                list.add(makeApp("微信扫一扫", "#07C160",
                    makeAction("action_extras", "com.tencent.mm", null,
                        "com.tencent.mm.ui.ShortCutDispatchAction",
                        "LauncherUI.Shortcut.LaunchType", "launch_type_scan_qrcode", null),
                    makeAction("uri", "com.tencent.mm", "weixin://dl/scan", null, null, null, null),
                    makeAction("launch_package", "com.tencent.mm", null, null, null, null, null)));
                list.add(makeApp("微信付款码", "#07C160",
                    makeAction("action_extras", "com.tencent.mm", null,
                        "com.tencent.mm.ui.ShortCutDispatchAction",
                        "LauncherUI.Shortcut.LaunchType", "launch_type_offline_wallet", null),
                    makeAction("uri", "com.tencent.mm", "weixin://dl/offlinepay", null, null, null, null),
                    makeAction("launch_package", "com.tencent.mm", null, null, null, null, null)));
                break;
            case "com.eg.android.AlipayGphone": // Alipay
                list.add(makeApp("支付宝扫一扫", "#1677FF",
                    makeAction("uri", "com.eg.android.AlipayGphone",
                        "alipays://platformapi/startapp?appId=10000007", null, null, null, null),
                    makeAction("launch_package", "com.eg.android.AlipayGphone", null, null, null, null, null)));
                list.add(makeApp("支付宝付款码", "#1677FF",
                    makeAction("uri", "com.eg.android.AlipayGphone",
                        "alipays://platformapi/startapp?appId=20000056", null, null, null, null),
                    makeAction("launch_package", "com.eg.android.AlipayGphone", null, null, null, null, null)));
                list.add(makeApp("支付宝乘车码", "#1677FF",
                    makeAction("uri", "com.eg.android.AlipayGphone",
                        "alipays://platformapi/startapp?appId=200011235", null, null, null, null),
                    makeAction("launch_package", "com.eg.android.AlipayGphone", null, null, null, null, null)));
                list.add(makeApp("支付宝健康码", "#1677FF",
                    makeAction("uri", "com.eg.android.AlipayGphone",
                        "alipays://platformapi/startapp?appId=2021001107610820", null, null, null, null),
                    makeAction("launch_package", "com.eg.android.AlipayGphone", null, null, null, null, null)));
                break;
            case "com.jingyao.easybike": // Hellobike
                list.add(makeApp("哈啰扫码", "#00B2FF",
                    makeAction("action", "com.jingyao.easybike", null,
                        "com.jingyao.easybike.driverlife.scan_code", null, null, null),
                    makeAction("launch_package", "com.jingyao.easybike", null, null, null, null, null)));
                break;
        }
        return list;
    }

    // --- Execution engine ---

    public static void executeShortcut(Context ctx, Item item) {
        if ("system".equals(item.type)) {
            executeSystem(ctx, item.systemKey);
            return;
        }
        // Try each action in order (chain fallback)
        for (Action action : item.actions) {
            Intent intent = buildIntent(ctx, action);
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                    return;
                } catch (Exception e) {
                    // try next action
                }
            }
        }
    }

    private static boolean torchOn = false;

    private static void executeSystem(Context ctx, String key) {
        if (key == null) return;
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            switch (key) {
                case "toggle_data_sim":
                    intent.setClassName("com.android.settings",
                        "com.android.settings.sim.SimDialogActivity");
                    intent.putExtra("dialog_type", 0);
                    break;
                case "wifi_settings":
                    intent.setAction("android.settings.WIFI_SETTINGS");
                    break;
                case "bluetooth_settings":
                    intent.setAction("android.settings.BLUETOOTH_SETTINGS");
                    break;
                case "mobile_data":
                    intent.setAction("android.settings.DATA_USAGE_SETTINGS");
                    break;
                case "hotspot_settings":
                    intent.setAction("android.settings.TETHER_SETTINGS");
                    break;
                case "display_settings":
                    intent.setAction("android.settings.DISPLAY_SETTINGS");
                    break;
                case "sound_settings":
                    intent.setAction("android.settings.SOUND_SETTINGS");
                    break;
                case "location_settings":
                    intent.setAction("android.settings.LOCATION_SOURCE_SETTINGS");
                    break;
                case "airplane_mode":
                    intent.setAction("android.settings.AIRPLANE_MODE_SETTINGS");
                    break;
                case "nfc_settings":
                    intent.setAction("android.settings.NFC_SETTINGS");
                    break;
                case "screenshot":
                    if (QuickBallAccessibilityService.isEnabled()) {
                        QuickBallAccessibilityService.takeScreenshot();
                    } else {
                        Intent accIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
                        accIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(accIntent);
                        Toast.makeText(ctx, "请先启用 QuickBall 无障碍服务", Toast.LENGTH_LONG).show();
                    }
                    return;
                case "toggle_torch":
                    toggleTorch(ctx);
                    return;
                default:
                    return;
            }
            ctx.startActivity(intent);
        } catch (Exception e) {}
    }

    private static Intent buildIntent(Context ctx, Action action) {
        if (action == null || action.type == null) return null;
        switch (action.type) {
            case "launch_package":
                if (action.pkg == null) return null;
                return ctx.getPackageManager().getLaunchIntentForPackage(action.pkg);

            case "uri":
                if (action.uri == null) return null;
                Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(action.uri));
                if (action.pkg != null) uriIntent.setPackage(action.pkg);
                uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NO_HISTORY);
                return uriIntent;

            case "action":
                if (action.action == null) return null;
                Intent actionIntent = new Intent(action.action);
                if (action.pkg != null) actionIntent.setPackage(action.pkg);
                return actionIntent;

            case "action_extras":
                if (action.action == null) return null;
                Intent extrasIntent = new Intent(action.action);
                if (action.pkg != null) extrasIntent.setPackage(action.pkg);
                if (action.extraKey != null && action.extraValue != null) {
                    extrasIntent.putExtra(action.extraKey, action.extraValue);
                }
                return extrasIntent;

            case "component":
                if (action.pkg == null || action.className == null) return null;
                Intent compIntent = new Intent();
                compIntent.setComponent(new ComponentName(action.pkg, action.className));
                return compIntent;

            default:
                return null;
        }
    }

    private static void toggleTorch(Context ctx) {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cm.getCameraIdList()[0];
            torchOn = !torchOn;
            cm.setTorchMode(cameraId, torchOn);
            Toast.makeText(ctx, torchOn ? "手电筒已开启" : "手电筒已关闭",
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            torchOn = false;
            Toast.makeText(ctx, "手电筒不可用", Toast.LENGTH_SHORT).show();
        }
    }
}
