package com.quickball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigActivity extends Activity {

    private List<ShortcutConfig.Item> items;
    private LinearLayout listContainer;
    private boolean dark;

    private static final String[] PRESET_COLORS = {
        "#FF6600", "#1677FF", "#07C160", "#00B2FF",
        "#E91E63", "#9C27B0", "#FF5722", "#607D8B"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dark = (getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        items = ShortcutConfig.load(this);
        showListView();
    }

    // --- Theme-aware colors ---
    private int colorBg()       { return dark ? 0xFF121212 : 0xFFF8F8F8; }
    private int colorCard()     { return dark ? 0xFF1E1E1E : 0xFFFFFFFF; }
    private int colorBorder()   { return dark ? 0xFF333333 : 0xFFE8E8E8; }
    private int colorText()     { return dark ? 0xFFE0E0E0 : 0xFF333333; }
    private int colorSub()      { return dark ? 0xFF888888 : 0xFF999999; }
    private int colorDivider()  { return dark ? 0xFF2A2A2A : 0xFFF0F0F0; }

    // --- Styled components ---

    private TextView makeLink(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(dark ? 0xFF82B1FF : 0xFF1677FF);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private TextView makeActionIcon(String icon, int color) {
        TextView tv = new TextView(this);
        tv.setText(icon);
        tv.setTextSize(18);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        return tv;
    }

    private View makeStyledButton(String text, int bgColor, int textColor) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(bgColor);
        btn.setBackground(bg);
        return btn;
    }

    private View makeOutlineButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(colorText());
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(0x00000000);
        bg.setStroke(dp(1), colorBorder());
        btn.setBackground(bg);
        return btn;
    }

    // ===== Layer 1: Shortcut List =====

    private void showListView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(colorBg());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        // Header
        TextView title = new TextView(this);
        title.setText("快捷方式");
        title.setTextSize(28);
        title.setTextColor(colorText());
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(4), 0, 0, dp(4));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("长按悬浮球或通知栏进入此页面");
        hint.setTextSize(13);
        hint.setTextColor(colorSub());
        hint.setPadding(dp(4), 0, 0, dp(20));
        root.addView(hint);

        // Card container for shortcut list
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(12));
        cardBg.setColor(colorCard());
        cardBg.setStroke(1, colorBorder());
        listContainer.setBackground(cardBg);
        listContainer.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(listContainer);
        refreshList();

        // Add buttons row
        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams addRowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        addRowLp.setMargins(0, dp(16), 0, 0);

        View addAppBtn = makeOutlineButton("+ 应用快捷方式");
        addAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAppPicker(); }
        });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnLp.setMargins(0, 0, dp(6), 0);
        addRow.addView(addAppBtn, btnLp);

        View addSysBtn = makeOutlineButton("+ 系统功能");
        addSysBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showSystemPicker(); }
        });
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnLp2.setMargins(dp(6), 0, 0, 0);
        addRow.addView(addSysBtn, btnLp2);

        root.addView(addRow, addRowLp);

        // Bottom action bar
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bbLp.setMargins(0, dp(20), 0, 0);

        TextView resetBtn = makeLink("恢复默认");
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ConfigActivity.this)
                    .setMessage("恢复所有快捷方式为默认设置？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            items = ShortcutConfig.getDefaults();
                            refreshList();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });

        View saveBtn = makeStyledButton("保存并返回",
            dark ? 0xFF2979FF : 0xFF1677FF, 0xFFFFFFFF);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveAndFinish(); }
        });

        bottomBar.addView(resetBtn, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bottomBar.addView(saveBtn, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(bottomBar, bbLp);

        // About
        TextView aboutLabel = new TextView(this);
        aboutLabel.setText("关于");
        aboutLabel.setTextSize(13);
        aboutLabel.setTextColor(colorSub());
        aboutLabel.setGravity(Gravity.CENTER);
        aboutLabel.setPadding(0, dp(32), 0, dp(8));
        root.addView(aboutLabel);

        TextView aboutText = new TextView(this);
        aboutText.setText(
            "QuickBall v0.1.0\n\n"
            + "点击半隐藏的小球弹出快捷菜单，拖动可改变位置，"
            + "3 秒无操作自动隐藏。长按悬浮球进入配置。"
            + "截屏功能需开启无障碍服务。\n\n"
            + "本应用通过系统公开 API 调用第三方应用功能入口，"
            + "不收集、存储或传输任何用户数据。"
            + "因第三方应用更新导致的功能失效，本应用不承担责任。");
        aboutText.setTextSize(12);
        aboutText.setTextColor(dark ? 0xFF555555 : 0xFFBBBBBB);
        aboutText.setLineSpacing(dp(2), 1.0f);
        aboutText.setPadding(dp(12), 0, dp(12), dp(24));
        aboutText.setGravity(Gravity.CENTER);
        root.addView(aboutText);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void refreshList() {
        listContainer.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            final ShortcutConfig.Item item = items.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(4), dp(10));

            // Color bar (thin rounded rect)
            View colorBar = new View(this);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setCornerRadius(dp(2));
            try {
                barBg.setColor(Color.parseColor(item.color));
            } catch (Exception e) {
                barBg.setColor(0xFF666666);
            }
            colorBar.setBackground(barBg);
            row.addView(colorBar, new LinearLayout.LayoutParams(dp(4), dp(28)));

            // Label
            TextView label = new TextView(this);
            label.setText(item.label);
            label.setTextSize(15);
            label.setTextColor(colorText());
            label.setPadding(dp(12), 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            // Action icons
            TextView upBtn = makeActionIcon("↑", idx > 0 ? colorSub() : colorDivider());
            upBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx > 0) { Collections.swap(items, idx, idx - 1); refreshList(); }
                }
            });
            row.addView(upBtn);

            TextView downBtn = makeActionIcon("↓",
                idx < items.size() - 1 ? colorSub() : colorDivider());
            downBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx < items.size() - 1) { Collections.swap(items, idx, idx + 1); refreshList(); }
                }
            });
            row.addView(downBtn);

            TextView delBtn = makeActionIcon("×", 0xFFCC4444);
            delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { items.remove(idx); refreshList(); }
            });
            row.addView(delBtn);

            // Tap row to edit
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showEditForm(item, false); }
            });

            listContainer.addView(row);

            // Divider
            if (i < items.size() - 1) {
                View div = new View(this);
                div.setBackgroundColor(colorDivider());
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divLp.setMargins(dp(28), 0, dp(8), 0);
                listContainer.addView(div, divLp);
            }
        }

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无快捷方式");
            empty.setTextColor(colorSub());
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            listContainer.addView(empty);
        }
    }

    // ===== Layer 2: App Picker =====

    private void showAppPicker() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(colorBg());
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView backLink = makeLink("← 返回");
        backLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showListView(); }
        });
        root.addView(backLink);

        TextView title = new TextView(this);
        title.setText("选择应用");
        title.setTextSize(22);
        title.setTextColor(colorText());
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(12));
        root.addView(title);

        final EditText searchBox = new EditText(this);
        searchBox.setHint("搜索应用...");
        searchBox.setSingleLine();
        searchBox.setTextSize(14);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setCornerRadius(dp(8));
        searchBg.setColor(colorCard());
        searchBg.setStroke(1, colorBorder());
        searchBox.setBackground(searchBg);
        searchBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(searchBox);

        final ListView listView = new ListView(this);
        listView.setDivider(null);
        LinearLayout.LayoutParams lvLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        lvLp.setMargins(0, dp(8), 0, 0);
        root.addView(listView, lvLp);

        setContentView(root);

        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(0);
        final List<ApplicationInfo> launchableApps = new ArrayList<>();
        for (ApplicationInfo ai : allApps) {
            if (pm.getLaunchIntentForPackage(ai.packageName) != null) {
                launchableApps.add(ai);
            }
        }
        Collections.sort(launchableApps, new java.util.Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });

        final List<ApplicationInfo> filtered = new ArrayList<>(launchableApps);
        final AppAdapter adapter = new AppAdapter(this, filtered, pm, dark);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                showSubFunctionPicker(filtered.get(pos));
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().toLowerCase();
                filtered.clear();
                for (ApplicationInfo ai : launchableApps) {
                    String name = pm.getApplicationLabel(ai).toString().toLowerCase();
                    if (name.contains(q) || ai.packageName.toLowerCase().contains(q)) {
                        filtered.add(ai);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    static class AppAdapter extends BaseAdapter {
        private List<ApplicationInfo> apps;
        private PackageManager pm;
        private Context ctx;
        private boolean dark;

        AppAdapter(Context ctx, List<ApplicationInfo> apps, PackageManager pm, boolean dark) {
            this.ctx = ctx; this.apps = apps; this.pm = pm; this.dark = dark;
        }

        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int p) { return apps.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp2(12), dp2(10), dp2(12), dp2(10));

            ApplicationInfo ai = apps.get(pos);

            ImageView icon = new ImageView(ctx);
            icon.setImageDrawable(ai.loadIcon(pm));
            row.addView(icon, new LinearLayout.LayoutParams(dp2(36), dp2(36)));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp2(12), 0, 0, 0);

            TextView name = new TextView(ctx);
            name.setText(pm.getApplicationLabel(ai));
            name.setTextSize(14);
            name.setTextColor(dark ? 0xFFE0E0E0 : 0xFF333333);
            col.addView(name);

            TextView pkg = new TextView(ctx);
            pkg.setText(ai.packageName);
            pkg.setTextSize(11);
            pkg.setTextColor(dark ? 0xFF666666 : 0xFFAAAAAA);
            col.addView(pkg);

            row.addView(col, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            return row;
        }

        private int dp2(int dp) {
            return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
        }
    }

    // ===== Layer 3: Sub-function Discovery =====

    private void showSubFunctionPicker(final ApplicationInfo appInfo) {
        final PackageManager pm = getPackageManager();
        final String appName = pm.getApplicationLabel(appInfo).toString();
        final String pkgName = appInfo.packageName;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(colorBg());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView backLink = makeLink("← 返回");
        backLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAppPicker(); }
        });
        root.addView(backLink);

        TextView title = new TextView(this);
        title.setText(appName);
        title.setTextSize(22);
        title.setTextColor(colorText());
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(16));
        root.addView(title);

        // Card for options
        LinearLayout optCard = new LinearLayout(this);
        optCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable optBg = new GradientDrawable();
        optBg.setCornerRadius(dp(12));
        optBg.setColor(colorCard());
        optBg.setStroke(1, colorBorder());
        optCard.setBackground(optBg);
        optCard.setPadding(dp(16), dp(8), dp(16), dp(8));

        // Open app option
        TextView launchOpt = new TextView(this);
        launchOpt.setText("打开应用");
        launchOpt.setTextSize(15);
        launchOpt.setTextColor(colorText());
        launchOpt.setPadding(0, dp(12), 0, dp(12));
        launchOpt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShortcutConfig.Item item = new ShortcutConfig.Item();
                item.label = appName;
                item.color = PRESET_COLORS[items.size() % PRESET_COLORS.length];
                item.type = "app";
                ShortcutConfig.Action a = new ShortcutConfig.Action();
                a.type = "launch_package";
                a.pkg = pkgName;
                item.actions.add(a);
                showEditForm(item, true);
            }
        });
        optCard.addView(launchOpt);

        // Known sub-functions
        List<ShortcutConfig.Item> knownItems = ShortcutConfig.getKnownShortcuts(pkgName);
        for (final ShortcutConfig.Item ki : knownItems) {
            View div = new View(this);
            div.setBackgroundColor(colorDivider());
            optCard.addView(div, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

            TextView opt = new TextView(this);
            opt.setText(ki.label);
            opt.setTextSize(15);
            opt.setTextColor(colorText());
            opt.setPadding(0, dp(12), 0, dp(12));
            opt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { showEditForm(ki, true); }
            });
            optCard.addView(opt);
        }

        // LauncherApps shortcuts
        List<ShortcutInfo> appShortcuts = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                LauncherApps la = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
                query.setPackage(pkgName);
                appShortcuts = la.getShortcuts(query, Process.myUserHandle());
            }
        } catch (Exception e) {}

        if (appShortcuts != null) {
            for (final ShortcutInfo si : appShortcuts) {
                View div = new View(this);
                div.setBackgroundColor(colorDivider());
                optCard.addView(div, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));

                CharSequence shortLabel = si.getShortLabel();
                final String scLabel = shortLabel != null ? shortLabel.toString() : si.getId();
                TextView opt = new TextView(this);
                opt.setText(scLabel);
                opt.setTextSize(15);
                opt.setTextColor(colorText());
                opt.setPadding(0, dp(12), 0, dp(12));
                opt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShortcutConfig.Item item = new ShortcutConfig.Item();
                        item.label = scLabel;
                        item.color = PRESET_COLORS[items.size() % PRESET_COLORS.length];
                        item.type = "app";
                        if (si.getIntent() != null) {
                            Intent sci = si.getIntent();
                            ShortcutConfig.Action a = new ShortcutConfig.Action();
                            if (sci.getData() != null) {
                                a.type = "uri"; a.uri = sci.getData().toString(); a.pkg = pkgName;
                            } else if (sci.getComponent() != null) {
                                a.type = "component";
                                a.pkg = sci.getComponent().getPackageName();
                                a.className = sci.getComponent().getClassName();
                            } else {
                                a.type = "launch_package"; a.pkg = pkgName;
                            }
                            item.actions.add(a);
                        }
                        ShortcutConfig.Action fb = new ShortcutConfig.Action();
                        fb.type = "launch_package"; fb.pkg = pkgName;
                        item.actions.add(fb);
                        showEditForm(item, true);
                    }
                });
                optCard.addView(opt);
            }
        }

        root.addView(optCard);

        // Custom URI link
        TextView uriLink = makeLink("自定义 URI（高级）");
        uriLink.setPadding(dp(4), dp(16), 0, 0);
        uriLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showCustomUriForm(pkgName, appName); }
        });
        root.addView(uriLink);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ===== Custom URI Form =====

    private void showCustomUriForm(final String pkgName, final String appName) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(colorBg());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView backLink = makeLink("← 返回");
        backLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showAppPicker(); }
        });
        root.addView(backLink);

        TextView title = new TextView(this);
        title.setText("自定义 URI");
        title.setTextSize(22);
        title.setTextColor(colorText());
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(16));
        root.addView(title);

        TextView uriLabel = new TextView(this);
        uriLabel.setText("例如 weixin://dl/scan");
        uriLabel.setTextSize(13);
        uriLabel.setTextColor(colorSub());
        root.addView(uriLabel);

        final EditText uriInput = new EditText(this);
        uriInput.setSingleLine();
        uriInput.setTextSize(14);
        root.addView(uriInput);

        View confirmBtn = makeStyledButton("确定",
            dark ? 0xFF2979FF : 0xFF1677FF, 0xFFFFFFFF);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, dp(16), 0, 0);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uri = uriInput.getText().toString().trim();
                if (uri.isEmpty()) {
                    Toast.makeText(ConfigActivity.this, "请输入 URI", Toast.LENGTH_SHORT).show();
                    return;
                }
                ShortcutConfig.Item item = new ShortcutConfig.Item();
                item.label = appName;
                item.color = PRESET_COLORS[items.size() % PRESET_COLORS.length];
                item.type = "app";
                ShortcutConfig.Action a = new ShortcutConfig.Action();
                a.type = "uri"; a.uri = uri; a.pkg = pkgName;
                item.actions.add(a);
                ShortcutConfig.Action fb = new ShortcutConfig.Action();
                fb.type = "launch_package"; fb.pkg = pkgName;
                item.actions.add(fb);
                showEditForm(item, true);
            }
        });
        root.addView(confirmBtn, cLp);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ===== System Function Picker =====

    private void showSystemPicker() {
        final String[][] sf = {
            {"截屏",       "screenshot",         "#F44336"},
            {"手电筒",     "toggle_torch",       "#FFC107"},
            {"切换流量卡", "toggle_data_sim",    "#FF6600"},
            {"WiFi 设置",  "wifi_settings",      "#4CAF50"},
            {"蓝牙设置",   "bluetooth_settings", "#2196F3"},
            {"移动数据",   "mobile_data",        "#FF9800"},
            {"个人热点",   "hotspot_settings",   "#E91E63"},
            {"显示设置",   "display_settings",   "#9C27B0"},
            {"声音设置",   "sound_settings",     "#00BCD4"},
            {"位置信息",   "location_settings",  "#8BC34A"},
            {"飞行模式",   "airplane_mode",      "#607D8B"},
            {"NFC",        "nfc_settings",       "#795548"},
        };
        String[] names = new String[sf.length];
        for (int i = 0; i < sf.length; i++) names[i] = sf[i][0];

        new AlertDialog.Builder(this)
            .setTitle("选择系统功能")
            .setItems(names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    ShortcutConfig.Item item = new ShortcutConfig.Item();
                    item.label = sf[w][0]; item.type = "system";
                    item.systemKey = sf[w][1]; item.color = sf[w][2];
                    showEditForm(item, true);
                }
            })
            .setNegativeButton("取消", null).show();
    }

    // ===== Edit Form =====

    private void showEditForm(final ShortcutConfig.Item item, final boolean isNew) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(colorBg());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView backLink = makeLink("← 返回");
        backLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showListView(); }
        });
        root.addView(backLink);

        TextView title = new TextView(this);
        title.setText(isNew ? "添加快捷方式" : "编辑快捷方式");
        title.setTextSize(22);
        title.setTextColor(colorText());
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(4), 0, dp(20));
        root.addView(title);

        // Name field
        TextView nameLabel = new TextView(this);
        nameLabel.setText("名称");
        nameLabel.setTextSize(13);
        nameLabel.setTextColor(colorSub());
        root.addView(nameLabel);

        final EditText nameInput = new EditText(this);
        nameInput.setText(item.label);
        nameInput.setSingleLine();
        nameInput.setTextSize(15);
        root.addView(nameInput);

        // Color section
        TextView colorLabel = new TextView(this);
        colorLabel.setText("颜色");
        colorLabel.setTextSize(13);
        colorLabel.setTextColor(colorSub());
        colorLabel.setPadding(0, dp(16), 0, dp(8));
        root.addView(colorLabel);

        final View colorPreview = new View(this);
        GradientDrawable pBg = new GradientDrawable();
        pBg.setCornerRadius(dp(8));
        try { pBg.setColor(Color.parseColor(item.color)); }
        catch (Exception e) { pBg.setColor(0xFF666666); }
        colorPreview.setBackground(pBg);
        root.addView(colorPreview, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));

        // Color swatches
        LinearLayout colorGrid = new LinearLayout(this);
        colorGrid.setGravity(Gravity.CENTER);
        colorGrid.setPadding(0, dp(12), 0, dp(4));
        for (final String c : PRESET_COLORS) {
            View sw = new View(this);
            GradientDrawable swBg = new GradientDrawable();
            swBg.setShape(GradientDrawable.OVAL);
            swBg.setColor(Color.parseColor(c));
            sw.setBackground(swBg);
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(32), dp(32));
            swLp.setMargins(dp(5), 0, dp(5), 0);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.color = c;
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(8));
                    bg.setColor(Color.parseColor(c));
                    colorPreview.setBackground(bg);
                }
            });
            colorGrid.addView(sw, swLp);
        }
        root.addView(colorGrid);

        // Hex input
        final EditText hexInput = new EditText(this);
        hexInput.setHint("#RRGGBB");
        hexInput.setSingleLine();
        hexInput.setTextSize(13);
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int parsed = Color.parseColor(s.toString().trim());
                    item.color = s.toString().trim();
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(8));
                    bg.setColor(parsed);
                    colorPreview.setBackground(bg);
                } catch (Exception e) {}
            }
        });
        root.addView(hexInput);

        // Confirm
        View confirmBtn = makeStyledButton(isNew ? "添加" : "保存修改",
            dark ? 0xFF2979FF : 0xFF1677FF, 0xFFFFFFFF);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, dp(24), 0, 0);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(ConfigActivity.this, "请输入名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.label = name;
                if (isNew) items.add(item);
                showListView();
            }
        });
        root.addView(confirmBtn, cLp);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ===== Save & Reload =====

    private void saveAndFinish() {
        ShortcutConfig.save(this, items);
        Intent svc = new Intent(this, FloatingBallService.class);
        svc.putExtra("reload", true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
