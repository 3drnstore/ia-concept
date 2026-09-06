package net.osmand.plus.nuwe;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NuweSheets {
    private static final Handler UI = new Handler(Looper.getMainLooper());

    private NuweSheets() {}

    public static void showLauncher(@NonNull MapActivity activity) {
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(NuweUi.dp(activity,18), NuweUi.dp(activity,8), NuweUi.dp(activity,18), NuweUi.dp(activity,20));
        content.setBackground(NuweUi.rounded(NuweUi.BG, 28, activity));

        View handle = new View(activity);
        handle.setBackground(NuweUi.rounded(Color.rgb(184,188,198), 999, activity));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(NuweUi.dp(activity,46), NuweUi.dp(activity,5));
        hp.gravity = Gravity.CENTER_HORIZONTAL; hp.bottomMargin = NuweUi.dp(activity,10);
        content.addView(handle, hp);

        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = NuweUi.text(activity, "Nuwe Mapa", 23, NuweUi.TEXT, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, NuweUi.dp(activity,46), 1f));
        FrameLayout close = NuweUi.iconButton(activity, NuweUi.Glyph.CLOSE, 42, false, v -> d.dismiss());
        close.setElevation(0);
        titleRow.addView(close);
        content.addView(titleRow);

        TextView sub = NuweUi.text(activity, "Navegação offline", 13, NuweUi.MUTED, false);
        sub.setPadding(0, 0, 0, NuweUi.dp(activity,12));
        content.addView(sub);

        LinearLayout profiles = profileRow(activity);
        content.addView(profiles);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);

        body.addView(NuweUi.section(activity, "Acesso rápido"));
        body.addView(actionRow(activity,
                action(activity, NuweUi.Glyph.SEARCH, "Buscar", () -> { d.dismiss(); showSearch(activity, false); }),
                action(activity, NuweUi.Glyph.ROUTE, "Trajeto", () -> { d.dismiss(); showSearch(activity, true); }),
                action(activity, NuweUi.Glyph.STAR, "Favoritos", () -> { d.dismiss(); showFavorites(activity); }),
                action(activity, NuweUi.Glyph.TRACK, "Trilhas", () -> { d.dismiss(); showTracks(activity); })));
        body.addView(actionRow(activity,
                action(activity, NuweUi.Glyph.MAP, "Mapas locais", () -> { d.dismiss(); showMaps(activity); }),
                action(activity, NuweUi.Glyph.PIN, "Marcador", () -> { d.dismiss(); showAddMarker(activity, null); }),
                action(activity, NuweUi.Glyph.SHARE, "Compartilhar", () -> { d.dismiss(); NuweMapShell.shareLocation(activity, null); }),
                action(activity, NuweUi.Glyph.SETTINGS, "Ajustes", () -> { d.dismiss(); showSettings(activity); })));

        TextView more = NuweUi.text(activity, "Mais opções", 15, NuweUi.ACCENT, true);
        more.setGravity(Gravity.CENTER);
        more.setPadding(0, NuweUi.dp(activity,14), 0, NuweUi.dp(activity,14));
        more.setClickable(true);
        body.addView(more);

        LinearLayout extra = NuweUi.card(activity);
        extra.setVisibility(View.GONE);
        extra.addView(NuweUi.menuRow(activity, NuweUi.Glyph.LAYERS, "Camadas", "Favoritos, relevo e conteúdo", v -> { d.dismiss(); showLayers(activity); }));
        extra.addView(NuweUi.divider(activity));
        extra.addView(NuweUi.menuRow(activity, NuweUi.Glyph.SATELLITE, "GNSS e localização", "Configurações do aparelho", v -> openSystem(activity, Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        extra.addView(NuweUi.divider(activity));
        extra.addView(NuweUi.menuRow(activity, NuweUi.Glyph.VOICE, "Voz offline", "Mecanismo TTS local", v -> openSystem(activity, Settings.ACTION_TTS_SETTINGS)));
        extra.addView(NuweUi.divider(activity));
        extra.addView(NuweUi.menuRow(activity, NuweUi.Glyph.INFO, "Sobre", "Nuwe Mapa 0.5.0", v -> showAbout(activity)));
        body.addView(extra);

        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = NuweUi.dp(activity,8); extra.setLayoutParams(ep);

        more.setOnClickListener(v -> {
            boolean open = extra.getVisibility() != View.VISIBLE;
            extra.setVisibility(open ? View.VISIBLE : View.GONE);
            more.setText(open ? "Menos opções" : "Mais opções");
            resizeDialog(d, activity, open ? 88 : 58);
        });

        scroll.addView(body);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        content.addView(scroll, sp);

        d.setContentView(content);
        showBottom(d, activity, 58);
        makeDraggable(handle, d, activity);
    }

    private static LinearLayout profileRow(@NonNull MapActivity activity) {
        LinearLayout profiles = new LinearLayout(activity);
        profiles.setOrientation(LinearLayout.HORIZONTAL);
        profiles.setGravity(Gravity.CENTER);
        ApplicationMode current = activity.getSettings().getApplicationMode();
        profiles.addView(NuweUi.chip(activity, "CARRO", current == ApplicationMode.CAR, v -> { activity.getSettings().setApplicationMode(ApplicationMode.CAR); refreshProfileRow(profiles, activity); }), chipParams(activity));
        profiles.addView(NuweUi.chip(activity, "BIKE", current == ApplicationMode.BICYCLE, v -> { activity.getSettings().setApplicationMode(ApplicationMode.BICYCLE); refreshProfileRow(profiles, activity); }), chipParams(activity));
        profiles.addView(NuweUi.chip(activity, "TRILHA", current == ApplicationMode.PEDESTRIAN, v -> { activity.getSettings().setApplicationMode(ApplicationMode.PEDESTRIAN); refreshProfileRow(profiles, activity); }), chipParams(activity));
        return profiles;
    }

    private static LinearLayout.LayoutParams chipParams(@NonNull Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.leftMargin = NuweUi.dp(c,4); p.rightMargin = NuweUi.dp(c,4); return p;
    }

    private static void refreshProfileRow(@NonNull LinearLayout row, @NonNull MapActivity activity) {
        ViewGroup parent = (ViewGroup) row.getParent();
        if (parent == null) return;
        int index = parent.indexOfChild(row);
        parent.removeView(row);
        parent.addView(profileRow(activity), index);
        NuweMapShell.refreshMap(activity);
    }

    private static LinearLayout action(@NonNull MapActivity a, NuweUi.Glyph glyph, String text, Runnable r) {
        return NuweUi.actionTile(a, glyph, text, v -> r.run());
    }

    private static LinearLayout actionRow(@NonNull Context c, View... actions) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : actions) row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    public static void showSettings(@NonNull MapActivity activity) {
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(NuweUi.dp(activity,18), NuweUi.dp(activity,10), NuweUi.dp(activity,18), NuweUi.dp(activity,30));
        root.setBackground(NuweUi.BG);

        root.addView(sheetHeader(activity, "Ajustes Nuwe", "Só o que importa para navegar", d));
        root.addView(NuweUi.section(activity, "Perfis"));
        root.addView(profileRow(activity));

        root.addView(NuweUi.section(activity, "Navegação"));
        LinearLayout nav = NuweUi.card(activity);
        nav.addView(NuweUi.menuRow(activity, NuweUi.Glyph.ROUTE, "Rota", "Carro, bike ou trilha conforme o perfil", v -> { d.dismiss(); showSearch(activity, true); }));
        nav.addView(NuweUi.divider(activity));
        nav.addView(NuweUi.menuRow(activity, NuweUi.Glyph.VOICE, "Voz offline", "Configurar TTS instalado no aparelho", v -> openSystem(activity, Settings.ACTION_TTS_SETTINGS)));
        root.addView(nav);

        root.addView(NuweUi.section(activity, "Mapa"));
        LinearLayout map = NuweUi.card(activity);
        map.addView(NuweUi.menuRow(activity, NuweUi.Glyph.LAYERS, "Mapa e camadas", "Favoritos, relevo e elementos 3D", v -> showLayers(activity)));
        map.addView(NuweUi.divider(activity));
        map.addView(NuweUi.menuRow(activity, NuweUi.Glyph.MAP, "Mapas locais", "Arquivos offline no aparelho", v -> showMaps(activity)));
        root.addView(map);

        root.addView(NuweUi.section(activity, "Localização e dados"));
        LinearLayout data = NuweUi.card(activity);
        data.addView(NuweUi.menuRow(activity, NuweUi.Glyph.SATELLITE, "GNSS e localização", "Galileo, BeiDou, GLONASS e GPS", v -> openSystem(activity, Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        data.addView(NuweUi.divider(activity));
        data.addView(NuweUi.menuRow(activity, NuweUi.Glyph.TRACK, "Trilhas e GPX", "Gravação e arquivos locais", v -> showTracks(activity)));
        data.addView(NuweUi.divider(activity));
        data.addView(NuweUi.menuRow(activity, NuweUi.Glyph.STAR, "Favoritos", "Pontos salvos no aparelho", v -> showFavorites(activity)));
        root.addView(data);

        root.addView(NuweUi.section(activity, "Aplicativo"));
        LinearLayout app = NuweUi.card(activity);
        app.addView(NuweUi.menuRow(activity, NuweUi.Glyph.INFO, "Sobre o Nuwe Mapa", "Versão 0.5.0 · offline", v -> showAbout(activity)));
        root.addView(app);

        scroll.addView(root);
        d.setContentView(scroll);
        showBottom(d, activity, 94);
    }

    public static void showLayers(@NonNull MapActivity activity) {
        Dialog d = baseSheet(activity, "Camadas", "Conteúdo visível no mapa");
        LinearLayout body = (LinearLayout) d.findViewById(android.R.id.content).findViewWithTag("body");
        if (body == null) return;

        body.addView(toggleRow(activity, "Favoritos", "Mostrar pontos salvos", activity.getSettings().SHOW_FAVORITES.get(), checked -> {
            activity.getSettings().SHOW_FAVORITES.set(checked); NuweMapShell.refreshMap(activity);
        }));

        SRTMPlugin terrain = PluginsHelper.getPlugin(SRTMPlugin.class);
        if (terrain != null) {
            body.addView(NuweUi.divider(activity));
            body.addView(toggleRow(activity, "Relevo", "Camada de terreno local", terrain.TERRAIN.get(), checked -> {
                terrain.TERRAIN.set(checked); terrain.updateElevationConfiguration(); NuweMapShell.refreshMap(activity);
            }));
            body.addView(NuweUi.divider(activity));
            body.addView(toggleRow(activity, "Objetos 3D", "Edifícios e objetos quando disponíveis", terrain.ENABLE_3D_MAP_OBJECTS.get(), checked -> {
                terrain.ENABLE_3D_MAP_OBJECTS.set(checked); NuweMapShell.refreshMap(activity);
            }));
        }
        d.show();
        showBottomAfterShow(d, activity, 54);
    }

    private interface Toggle { void set(boolean value); }
    private static LinearLayout toggleRow(Context c, String title, String sub, boolean checked, Toggle action) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(NuweUi.dp(c,4), NuweUi.dp(c,12), NuweUi.dp(c,4), NuweUi.dp(c,12));
        LinearLayout labels = new LinearLayout(c); labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(NuweUi.text(c,title,16,NuweUi.TEXT,false));
        labels.addView(NuweUi.text(c,sub,12,NuweUi.MUTED,false));
        row.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        Switch sw = new Switch(c); sw.setChecked(checked); sw.setOnCheckedChangeListener((b,v)->action.set(v)); row.addView(sw);
        return row;
    }

    public static void showCompass(@NonNull MapActivity activity) {
        Dialog d = baseSheet(activity, "Orientação do mapa", "Escolha como o mapa gira");
        LinearLayout body = (LinearLayout) d.findViewById(android.R.id.content).findViewWithTag("body");
        if (body == null) return;
        addCompassRow(body, activity, d, "Norte para cima", "Mantém o norte no topo", CompassMode.NORTH_IS_UP);
        body.addView(NuweUi.divider(activity));
        addCompassRow(body, activity, d, "Direção do movimento", "Gira de acordo com o deslocamento", CompassMode.MOVEMENT_DIRECTION);
        body.addView(NuweUi.divider(activity));
        addCompassRow(body, activity, d, "Bússola", "Usa a orientação do aparelho", CompassMode.COMPASS_DIRECTION);
        body.addView(NuweUi.divider(activity));
        addCompassRow(body, activity, d, "Rotação livre", "Permite girar o mapa manualmente", CompassMode.MANUALLY_ROTATED);
        d.show(); showBottomAfterShow(d, activity, 50);
    }

    private static void addCompassRow(LinearLayout body, MapActivity a, Dialog d, String title, String sub, CompassMode mode) {
        body.addView(NuweUi.menuRow(a, NuweUi.Glyph.COMPASS, title, sub, v -> { NuweMapShell.setCompassMode(a, mode); d.dismiss(); }));
    }

    public static void showSearch(@NonNull MapActivity activity, boolean routeMode) {
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(NuweUi.dp(activity,16), NuweUi.dp(activity,10), NuweUi.dp(activity,16), NuweUi.dp(activity,20));
        root.setBackground(NuweUi.BG);
        root.addView(sheetHeader(activity, routeMode ? "Escolher destino" : "Buscar", "Pesquisa offline nos mapas instalados", d));

        EditText field = new EditText(activity);
        field.setSingleLine(true);
        field.setTextSize(17);
        field.setTextColor(NuweUi.TEXT);
        field.setHintTextColor(NuweUi.MUTED);
        field.setHint(routeMode ? "Para onde vamos?" : "Endereço, cidade ou lugar");
        field.setPadding(NuweUi.dp(activity,16),0,NuweUi.dp(activity,16),0);
        field.setBackground(NuweUi.roundedStroke(Color.WHITE,18,NuweUi.LINE,1,activity));
        root.addView(field,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,NuweUi.dp(activity,56)));

        TextView status = NuweUi.text(activity, "Digite para pesquisar", 13, NuweUi.MUTED, false);
        status.setPadding(NuweUi.dp(activity,4),NuweUi.dp(activity,12),0,NuweUi.dp(activity,6));
        root.addView(status);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout results = new LinearLayout(activity); results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results);
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));

        d.setContentView(root);
        showBottom(d, activity, 92);

        QuickSearchHelper helper = activity.getApp().getSearchUICore();
        helper.initSearchUICore();
        SearchUICore core = helper.getCore();
        Handler debounce = new Handler(Looper.getMainLooper());
        Runnable[] pending = new Runnable[1];

        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {}
            public void afterTextChanged(Editable e) {
                if (pending[0] != null) debounce.removeCallbacks(pending[0]);
                String q = e.toString().trim();
                if (q.length() < 2) { results.removeAllViews(); status.setText("Digite para pesquisar"); return; }
                pending[0] = () -> runSearch(activity, core, q, results, status, d, routeMode);
                debounce.postDelayed(pending[0], 350);
            }
        });
        field.requestFocus();
        UI.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
        },250);
    }

    private static void runSearch(MapActivity activity, SearchUICore core, String query, LinearLayout results, TextView status, Dialog d, boolean routeMode) {
        status.setText("Pesquisando…");
        LatLon center = activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
        SearchSettings settings = new SearchSettings(core.getSearchSettings())
                .resetSearchTypes().setRadiusLevel(1).setEmptyQueryAllowed(false).setSortByName(false).setOriginalLocation(center);
        core.setOnResultsComplete(() -> activity.runOnUiThread(() -> {
            results.removeAllViews();
            SearchUICore.SearchResultCollection collection = core.getCurrentSearchResult();
            if (collection == null) { status.setText("Nenhum resultado"); return; }
            int shown = 0;
            for (SearchResult r : collection.getCurrentSearchResults()) {
                if (r == null || r.location == null) continue;
                String name = QuickSearchListItem.getName(activity.getApp(), r);
                if (Algorithms.isEmpty(name)) continue;
                LatLon ll = r.location;
                String dist = "";
                if (center != null) {
                    double meters = net.osmand.util.MapUtils.getDistance(center, ll);
                    dist = OsmAndFormatter.getFormattedDistance((float) meters, activity.getApp());
                }
                PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_LOCATION, name);
                LinearLayout row = NuweUi.menuRow(activity, routeMode ? NuweUi.Glyph.ROUTE : NuweUi.Glyph.PIN, name, dist, v -> {
                    d.dismiss();
                    if (routeMode) NuweMapShell.routeTo(activity, ll, name);
                    else { NuweMapShell.centerOn(activity,ll); activity.getContextMenu().show(ll,pd,r.object); }
                });
                results.addView(row);
                results.addView(NuweUi.divider(activity));
                if (++shown >= 25) break;
            }
            status.setText(shown == 0 ? "Nenhum resultado offline" : shown + " resultados");
        }));
        core.search(query, true, null, settings);
    }

    public static void showFavorites(@NonNull MapActivity activity) {
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(NuweUi.dp(activity,16),NuweUi.dp(activity,10),NuweUi.dp(activity,16),NuweUi.dp(activity,24)); root.setBackground(NuweUi.BG);
        root.addView(sheetHeader(activity,"Favoritos","Pontos salvos somente no aparelho",d));
        int count = 0;
        for (FavoriteGroup group : activity.getApp().getFavoritesHelper().getFavoriteGroups()) {
            for (FavouritePoint p : group.getPoints()) {
                LatLon ll = new LatLon(p.getLatitude(), p.getLongitude());
                PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_FAVORITE, p.getName());
                root.addView(NuweUi.menuRow(activity,NuweUi.Glyph.STAR,p.getName(),group.getDisplayName(activity),v->{ d.dismiss(); NuweMapShell.centerOn(activity,ll); activity.getContextMenu().show(ll,pd,p); }));
                root.addView(NuweUi.divider(activity));
                count++;
            }
        }
        if (count == 0) { TextView empty=NuweUi.text(activity,"Nenhum favorito salvo",16,NuweUi.MUTED,false); empty.setGravity(Gravity.CENTER); empty.setPadding(0,NuweUi.dp(activity,50),0,NuweUi.dp(activity,50)); root.addView(empty); }
        scroll.addView(root); d.setContentView(scroll); showBottom(d,activity,86);
    }

    public static void showTracks(@NonNull MapActivity activity) {
        Dialog d = baseSheet(activity,"Trilhas e GPX","Gravação local, sem nuvem");
        LinearLayout body = (LinearLayout) d.findViewById(android.R.id.content).findViewWithTag("body");
        if (body == null) return;
        OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
        if (plugin == null) {
            body.addView(NuweUi.text(activity,"O módulo de gravação não está disponível nesta compilação.",14,NuweUi.MUTED,false));
        } else {
            boolean recording = plugin.isRecordingTrack();
            body.addView(NuweUi.menuRow(activity, recording ? NuweUi.Glyph.PAUSE : NuweUi.Glyph.PLAY,
                    recording ? "Pausar gravação" : "Iniciar gravação",
                    recording ? "A trilha continuará salva na memória local" : "Registrar o caminho com o GNSS",
                    v -> { if (plugin.isRecordingTrack()) plugin.pauseOrResumeRecording(); else plugin.startRecording(activity); d.dismiss(); Toast.makeText(activity, plugin.isRecordingTrack()?"Gravação ativa":"Gravação pausada",Toast.LENGTH_SHORT).show(); }));
            body.addView(NuweUi.divider(activity));
            body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.SAVE,"Salvar trilha atual","Finalizar e gravar GPX no aparelho",v->{ saveTrackLocally(activity,plugin); d.dismiss(); }));
        }
        body.addView(NuweUi.divider(activity));
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.INFO,"Importar GPX","Abra o arquivo .gpx no gerenciador e escolha Nuwe Mapa",v->Toast.makeText(activity,"Use ‘Abrir com’ no arquivo GPX e escolha Nuwe Mapa",Toast.LENGTH_LONG).show()));
        d.show(); showBottomAfterShow(d,activity,52);
    }

    private static void saveTrackLocally(MapActivity activity, OsmandMonitoringPlugin plugin) {
        plugin.stopRecording();
        Toast.makeText(activity,"Salvando trilha…",Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                activity.getApp().getSavingTrackHelper().saveDataToGpx(activity.getApp().getAppCustomization().getTracksDir());
                activity.getApp().getSavingTrackHelper().close();
                activity.runOnUiThread(() -> Toast.makeText(activity,"GPX salvo no aparelho",Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                activity.runOnUiThread(() -> Toast.makeText(activity,"Não foi possível salvar a trilha",Toast.LENGTH_SHORT).show());
            }
        },"nuwe-save-gpx").start();
    }

    public static void showMaps(@NonNull MapActivity activity) {
        Dialog d = baseSheet(activity,"Mapas locais","Arquivos offline do Nuwe Mapa");
        LinearLayout body = (LinearLayout) d.findViewById(android.R.id.content).findViewWithTag("body");
        if (body == null) return;
        File base = activity.getApp().getAppPath();
        List<File> obf = new ArrayList<>(); collectByExtension(base,".obf",obf,80);
        TextView info = NuweUi.text(activity, obf.size()+" arquivo(s) .obf encontrado(s)\n"+base.getAbsolutePath(),13,NuweUi.MUTED,false);
        info.setPadding(NuweUi.dp(activity,4),NuweUi.dp(activity,8),NuweUi.dp(activity,4),NuweUi.dp(activity,14)); body.addView(info);
        for (File f : obf) {
            body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.MAP,f.getName(),formatBytes(f.length()),v->{}));
            body.addView(NuweUi.divider(activity));
        }
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.INFO,"Importar outro mapa","Abra um arquivo .obf pelo gerenciador de arquivos e escolha Nuwe Mapa",v->Toast.makeText(activity,"Abra o .obf e escolha Nuwe Mapa em ‘Abrir com’",Toast.LENGTH_LONG).show()));
        d.show(); showBottomAfterShow(d,activity,72);
    }

    private static void collectByExtension(File dir,String ext,List<File> out,int limit) {
        if (dir==null || !dir.exists() || out.size()>=limit) return; File[] files=dir.listFiles(); if(files==null)return;
        for(File f:files){ if(out.size()>=limit)return; if(f.isDirectory())collectByExtension(f,ext,out,limit); else if(f.getName().toLowerCase(Locale.ROOT).endsWith(ext))out.add(f); }
    }
    private static String formatBytes(long b){ if(b>1024L*1024*1024)return String.format(Locale.getDefault(),"%.1f GB",b/(1024d*1024*1024)); if(b>1024L*1024)return String.format(Locale.getDefault(),"%.1f MB",b/(1024d*1024)); return String.format(Locale.getDefault(),"%.0f KB",b/1024d); }

    public static void showAddMarker(@NonNull MapActivity activity, @Nullable LatLon point) {
        Dialog d = new Dialog(activity); d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(NuweUi.dp(activity,18),NuweUi.dp(activity,12),NuweUi.dp(activity,18),NuweUi.dp(activity,24)); root.setBackground(NuweUi.BG);
        root.addView(sheetHeader(activity,"Adicionar marcador",point==null?"No centro atual do mapa":"Neste ponto",d));
        EditText name = new EditText(activity); name.setHint("Nome do marcador"); name.setSingleLine(true); name.setTextColor(NuweUi.TEXT); name.setHintTextColor(NuweUi.MUTED); name.setBackground(NuweUi.roundedStroke(Color.WHITE,16,NuweUi.LINE,1,activity)); name.setPadding(NuweUi.dp(activity,14),0,NuweUi.dp(activity,14),0); root.addView(name,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,NuweUi.dp(activity,54)));
        TextView save=NuweUi.text(activity,"Salvar marcador",16,Color.WHITE,true); save.setGravity(Gravity.CENTER); save.setBackground(NuweUi.pressable(NuweUi.ACCENT,NuweUi.ACCENT_DARK,18,activity)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,NuweUi.dp(activity,54)); bp.topMargin=NuweUi.dp(activity,16); root.addView(save,bp); save.setOnClickListener(v->{ if(point==null)NuweMapShell.addFavoriteAtCenter(activity,name.getText().toString()); else NuweMapShell.addFavoriteAt(activity,point,name.getText().toString()); d.dismiss(); });
        d.setContentView(root); showBottom(d,activity,38);
    }

    public static void showPoint(@NonNull MapActivity activity, @NonNull LatLon ll, @Nullable PointDescription pd, @Nullable Object object) {
        Dialog d = new Dialog(activity); d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(NuweUi.dp(activity,18),NuweUi.dp(activity,10),NuweUi.dp(activity,18),NuweUi.dp(activity,22)); root.setBackground(NuweUi.BG);
        String title = pd != null ? pd.getSimpleName(activity,false) : "Ponto no mapa"; if(Algorithms.isEmpty(title))title="Ponto no mapa";
        root.addView(sheetHeader(activity,title,String.format(Locale.US,"%.6f, %.6f",ll.getLatitude(),ll.getLongitude()),d));
        root.addView(actionRow(activity,
                action(activity,NuweUi.Glyph.ROUTE,"Navegar",()->{d.dismiss();NuweMapShell.routeTo(activity,ll,title);}),
                action(activity,NuweUi.Glyph.STAR,"Favorito",()->{d.dismiss();showAddMarker(activity,ll);}),
                action(activity,NuweUi.Glyph.SHARE,"Compartilhar",()->{d.dismiss();NuweMapShell.shareLocation(activity,ll);}),
                action(activity,NuweUi.Glyph.PIN,"Centralizar",()->{d.dismiss();NuweMapShell.centerOn(activity,ll);})));
        d.setOnDismissListener(x->{ try{ if(activity.getContextMenu().isVisible()) activity.getContextMenu().close(false); }catch(Exception ignored){} });
        d.setContentView(root); showBottom(d,activity,40);
    }

    public static void showMeasureResult(@NonNull MapActivity activity, LatLon a, LatLon b, double meters) {
        Dialog d = baseSheet(activity,"Distância medida",OsmAndFormatter.getFormattedDistance((float)meters,activity.getApp()));
        LinearLayout body=(LinearLayout)d.findViewById(android.R.id.content).findViewWithTag("body"); if(body==null)return;
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.RULER,"Nova medição","Escolher outros dois pontos",v->{d.dismiss();NuweMapShell.startMeasure(activity);}));
        body.addView(NuweUi.divider(activity));
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.CLOSE,"Encerrar régua","Voltar ao uso normal do mapa",v->{NuweMapShell.cancelMeasure();d.dismiss();}));
        d.show();showBottomAfterShow(d,activity,38);
    }

    public static void showNavigation(@NonNull MapActivity activity) {
        Dialog d=baseSheet(activity,"Navegação ativa","Rota calculada pelo motor offline"); LinearLayout body=(LinearLayout)d.findViewById(android.R.id.content).findViewWithTag("body"); if(body==null)return;
        int left=activity.getRoutingHelper().getLeftDistance(); int secs=activity.getRoutingHelper().getLeftTime();
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.ROUTE,"Restante: "+OsmAndFormatter.getFormattedDistance(left,activity.getApp()),Math.max(1,secs/60)+" min estimados",v->{}));
        body.addView(NuweUi.divider(activity));
        body.addView(NuweUi.menuRow(activity,NuweUi.Glyph.CLOSE,"Encerrar navegação","Cancelar rota atual",v->{NuweMapShell.stopNavigation(activity);d.dismiss();}));
        d.show();showBottomAfterShow(d,activity,40);
    }

    public static void showAbout(@NonNull MapActivity activity) {
        Dialog d=baseSheet(activity,"Nuwe Mapa 0.5.0","Navegação offline com interface própria"); LinearLayout body=(LinearLayout)d.findViewById(android.R.id.content).findViewWithTag("body"); if(body==null)return;
        TextView t=NuweUi.text(activity,"O Nuwe Mapa usa componentes open source do OsmAnd como motor interno de mapas, busca, roteamento e GPX. A interface principal desta versão é própria do Nuwe.\n\nUso pessoal/offline. Licenças open source permanecem aplicáveis aos componentes derivados.",14,NuweUi.TEXT,false); t.setLineSpacing(0,1.2f); t.setPadding(NuweUi.dp(activity,4),NuweUi.dp(activity,8),NuweUi.dp(activity,4),NuweUi.dp(activity,12)); body.addView(t);
        d.show();showBottomAfterShow(d,activity,48);
    }

    private static Dialog baseSheet(MapActivity activity,String title,String subtitle) {
        Dialog d=new Dialog(activity); d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll=new ScrollView(activity);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(NuweUi.dp(activity,18),NuweUi.dp(activity,10),NuweUi.dp(activity,18),NuweUi.dp(activity,24));root.setBackground(NuweUi.BG);
        root.addView(sheetHeader(activity,title,subtitle,d));
        LinearLayout body=NuweUi.card(activity);body.setTag("body");root.addView(body);
        scroll.addView(root);d.setContentView(scroll);return d;
    }

    private static LinearLayout sheetHeader(Context c,String title,String subtitle,Dialog d) {
        LinearLayout wrap=new LinearLayout(c);wrap.setOrientation(LinearLayout.VERTICAL);
        View handle=new View(c);handle.setBackground(NuweUi.rounded(Color.rgb(184,188,198),999,c));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(NuweUi.dp(c,46),NuweUi.dp(c,5));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.bottomMargin=NuweUi.dp(c,10);wrap.addView(handle,hp);
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(NuweUi.text(c,title,22,NuweUi.TEXT,true),new LinearLayout.LayoutParams(0,NuweUi.dp(c,44),1f));FrameLayout close=NuweUi.iconButton(c,NuweUi.Glyph.CLOSE,40,false,v->d.dismiss());close.setElevation(0);row.addView(close);wrap.addView(row);
        TextView sub=NuweUi.text(c,subtitle,13,NuweUi.MUTED,false);sub.setPadding(0,0,0,NuweUi.dp(c,14));wrap.addView(sub);return wrap;
    }

    private static void openSystem(MapActivity a,String action){try{a.startActivity(new Intent(action));}catch(Exception e){a.startActivity(new Intent(Settings.ACTION_SETTINGS));}}

    private static void showBottom(Dialog d,MapActivity a,int percent){ d.show();showBottomAfterShow(d,a,percent); }
    private static void showBottomAfterShow(Dialog d,MapActivity a,int percent){ Window w=d.getWindow();if(w==null)return;w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setGravity(Gravity.BOTTOM);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams at=w.getAttributes();at.dimAmount=.18f;w.setAttributes(at);resizeDialog(d,a,percent);View decor=w.getDecorView();int h=a.getResources().getDisplayMetrics().heightPixels;decor.setTranslationY(h*.35f);decor.animate().translationY(0).setDuration(230).start(); }
    private static void resizeDialog(Dialog d,MapActivity a,int percent){Window w=d.getWindow();if(w==null)return;int h=a.getResources().getDisplayMetrics().heightPixels;w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,(int)(h*(percent/100f)));w.setGravity(Gravity.BOTTOM);}
    private static void makeDraggable(View handle,Dialog d,MapActivity a){final float[]sy={0};final int[]sh={0};handle.setOnTouchListener((v,e)->{Window w=d.getWindow();if(w==null)return false;int screen=a.getResources().getDisplayMetrics().heightPixels;switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:sy[0]=e.getRawY();sh[0]=w.getAttributes().height>0?w.getAttributes().height:(int)(screen*.58f);return true;case MotionEvent.ACTION_MOVE:int nh=(int)(sh[0]+sy[0]-e.getRawY());nh=Math.max((int)(screen*.45f),Math.min((int)(screen*.94f),nh));w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,nh);return true;case MotionEvent.ACTION_UP:int now=w.getAttributes().height;w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,now>screen*.70f?(int)(screen*.90f):(int)(screen*.58f));return true;}return false;});}
}
