package app.aven.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContextActionActivity extends Activity {
    public static final String EXTRA_PAGE_URL = "aven.context.page_url";
    public static final String EXTRA_TITLE = "aven.context.title";
    public static final String EXTRA_LINK_URL = "aven.context.link_url";
    public static final String EXTRA_MEDIA_URL = "aven.context.media_url";
    public static final String EXTRA_MEDIA_TYPE = "aven.context.media_type";
    public static final String EXTRA_POSTER_URL = "aven.context.poster_url";
    public static final String EXTRA_TEXT = "aven.context.text";
    public static final String EXTRA_USER_AGENT = "aven.context.user_agent";

    private String pageUrl = "";
    private String title = "";
    private String linkUrl = "";
    private String mediaUrl = "";
    private String mediaType = "";
    private String posterUrl = "";
    private String text = "";
    private String userAgent = "";

    private static class ActionItem {
        final String label;
        final Runnable action;
        ActionItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent(getIntent());
        showContextMenu();
    }

    private void readIntent(Intent intent) {
        if (intent == null) return;
        pageUrl = value(intent.getStringExtra(EXTRA_PAGE_URL));
        title = value(intent.getStringExtra(EXTRA_TITLE));
        linkUrl = value(intent.getStringExtra(EXTRA_LINK_URL));
        mediaUrl = value(intent.getStringExtra(EXTRA_MEDIA_URL));
        mediaType = value(intent.getStringExtra(EXTRA_MEDIA_TYPE));
        posterUrl = value(intent.getStringExtra(EXTRA_POSTER_URL));
        text = value(intent.getStringExtra(EXTRA_TEXT));
        userAgent = value(intent.getStringExtra(EXTRA_USER_AGENT));
    }

    private void showContextMenu() {
        List<ActionItem> actions = new ArrayList<>();

        if (!text.isEmpty()) {
            actions.add(new ActionItem("Copiar texto", () -> copy("Texto", text)));
            actions.add(new ActionItem("Compartilhar texto", () -> share(text)));
        }

        if (isHttp(linkUrl)) {
            actions.add(new ActionItem("Abrir link", () -> openInAven(linkUrl)));
            actions.add(new ActionItem("Abrir link em nova aba", () -> openInAven(linkUrl)));
            actions.add(new ActionItem("Copiar endereço do link", () -> copy("Link", linkUrl)));
            actions.add(new ActionItem("Compartilhar link", () -> share(linkUrl)));
        }

        if (isHttp(mediaUrl)) {
            String noun = "mídia";
            if ("image".equals(mediaType)) noun = "imagem";
            else if ("video".equals(mediaType)) noun = "vídeo";
            else if ("audio".equals(mediaType)) noun = "áudio";

            String finalNoun = noun;
            actions.add(new ActionItem("Abrir " + finalNoun, () -> openInAven(mediaUrl)));
            actions.add(new ActionItem("Salvar " + finalNoun, this::saveMedia));
            actions.add(new ActionItem("Copiar endereço da " + finalNoun, () -> copy("Mídia", mediaUrl)));
            actions.add(new ActionItem("Compartilhar " + finalNoun, () -> share(mediaUrl)));
        } else if (isHttp(posterUrl)) {
            actions.add(new ActionItem("Salvar imagem de capa", () -> saveUrl(posterUrl, "image")));
        }

        if (actions.isEmpty()) {
            finish();
            return;
        }

        String[] labels = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++) labels[i] = actions.get(i).label;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(contextTitle())
                .setItems(labels, (d, which) -> {
                    try { actions.get(which).action.run(); }
                    finally { finish(); }
                })
                .setOnCancelListener(d -> finish())
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private String contextTitle() {
        if ("image".equals(mediaType)) return "Imagem";
        if ("video".equals(mediaType)) return "Vídeo";
        if ("audio".equals(mediaType)) return "Áudio";
        if (!linkUrl.isEmpty()) return "Link";
        return "Texto";
    }

    private void saveMedia() {
        if (mediaUrl.toLowerCase(Locale.US).contains(".m3u8")) {
            Intent intent = new Intent(this, MediaPromptActivity.class);
            intent.putExtra(MediaPromptActivity.EXTRA_URL, mediaUrl);
            intent.putExtra(MediaPromptActivity.EXTRA_PAGE_URL, pageUrl);
            intent.putExtra(MediaPromptActivity.EXTRA_TITLE, title.isEmpty() ? "Vídeo" : title);
            intent.putExtra(MediaPromptActivity.EXTRA_MIME, "application/vnd.apple.mpegurl");
            intent.putExtra(MediaPromptActivity.EXTRA_USER_AGENT, userAgent);
            startActivity(intent);
            return;
        }
        saveUrl(mediaUrl, mediaType);
    }

    private void saveUrl(String url, String type) {
        try {
            Uri uri = Uri.parse(url);
            String extension = extensionFor(uri, type);
            String base = title.isEmpty() ? ("image".equals(type) ? "imagem" : "midia") : sanitize(title);
            String filename = base + "." + extension;

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(filename)
                    .setDescription("Baixando com Aven")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Aven/" + filename);

            if (isHttp(pageUrl)) request.addRequestHeader("Referer", pageUrl);
            if (!userAgent.isEmpty()) request.addRequestHeader("User-Agent", userAgent);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Serviço de download indisponível");
            manager.enqueue(request);
            Toast.makeText(this, "Download iniciado em Downloads/Aven", Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            Toast.makeText(this, "Não foi possível salvar: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String extensionFor(Uri uri, String type) {
        String last = uri.getLastPathSegment();
        if (last != null) {
            int query = last.indexOf('?');
            if (query >= 0) last = last.substring(0, query);
            int dot = last.lastIndexOf('.');
            if (dot >= 0 && dot < last.length() - 1) {
                String ext = last.substring(dot + 1).toLowerCase(Locale.US);
                if (ext.matches("jpg|jpeg|png|webp|gif|mp4|webm|m4v|mov|ogv|mp3|m4a|aac")) return ext;
            }
        }
        if ("image".equals(type)) return "jpg";
        if ("audio".equals(type)) return "m4a";
        return "mp4";
    }

    private void openInAven(String url) {
        if (!isHttp(url)) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void copy(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show();
    }

    private void share(String value) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, value);
        startActivity(Intent.createChooser(intent, "Compartilhar com"));
    }

    private String sanitize(String input) {
        String value = input.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_")
                .replaceAll("\\s+", " ").trim();
        if (value.isEmpty()) value = "midia";
        if (value.length() > 70) value = value.substring(0, 70).trim();
        return value;
    }

    private boolean isHttp(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
