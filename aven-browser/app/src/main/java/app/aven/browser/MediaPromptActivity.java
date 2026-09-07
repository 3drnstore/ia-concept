package app.aven.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MediaPromptActivity extends Activity {
    public static final String EXTRA_URL = "aven.media.url";
    public static final String EXTRA_PAGE_URL = "aven.media.page_url";
    public static final String EXTRA_TITLE = "aven.media.title";
    public static final String EXTRA_MIME = "aven.media.mime";
    public static final String EXTRA_COOKIES = "aven.media.cookies";
    public static final String EXTRA_USER_AGENT = "aven.media.user_agent";
    public static final String EXTRA_DURATION = "aven.media.duration";
    public static final String EXTRA_MEDIA_KIND = "aven.media.kind";

    private static final int REQUEST_STORAGE = 7201;

    private String url;
    private String pageUrl;
    private String title;
    private String mime;
    private String cookies;
    private String userAgent;
    private String mediaKind;
    private double duration;

    private static class HlsPlaylist {
        final String playlistUrl;
        final List<String> segments;
        final String initSegment;
        final boolean fragmentedMp4;

        HlsPlaylist(String playlistUrl, List<String> segments, String initSegment, boolean fragmentedMp4) {
            this.playlistUrl = playlistUrl;
            this.segments = segments;
            this.initSegment = initSegment;
            this.fragmentedMp4 = fragmentedMp4;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readIntent(getIntent());
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            finish();
            return;
        }
        showDownloadPrompt();
    }

    private void readIntent(Intent intent) {
        if (intent == null) return;
        url = intent.getStringExtra(EXTRA_URL);
        pageUrl = valueOrEmpty(intent.getStringExtra(EXTRA_PAGE_URL));
        title = valueOrEmpty(intent.getStringExtra(EXTRA_TITLE));
        mime = valueOrEmpty(intent.getStringExtra(EXTRA_MIME));
        cookies = valueOrEmpty(intent.getStringExtra(EXTRA_COOKIES));
        userAgent = valueOrEmpty(intent.getStringExtra(EXTRA_USER_AGENT));
        mediaKind = valueOrEmpty(intent.getStringExtra(EXTRA_MEDIA_KIND));
        duration = intent.getDoubleExtra(EXTRA_DURATION, 0d);
    }

    private void showDownloadPrompt() {
        String site = "";
        try {
            String host = Uri.parse(pageUrl).getHost();
            if (host != null) site = host;
        } catch (Exception ignored) {}

        String durationText = "";
        if (duration >= 1d && Double.isFinite(duration)) {
            long total = Math.round(duration);
            long minutes = total / 60;
            long seconds = total % 60;
            durationText = "\nDuração aproximada: " + minutes + ":" + String.format(Locale.US, "%02d", seconds);
        }

        String streamText = isHls() ? "\nStream HLS público detectado." : "";
        String message = "Aven detectou um vídeo em reprodução"
                + (site.isEmpty() ? "." : " em " + site + ".")
                + durationText
                + streamText
                + "\n\nDeseja baixar agora?";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vídeo detectado")
                .setMessage(message)
                .setNegativeButton("Agora não", (d, which) -> finish())
                .setPositiveButton("Baixar", (d, which) -> prepareDownload())
                .setOnCancelListener(d -> finish())
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void prepareDownload() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            return;
        }

        if (isHls()) {
            startHlsDownload();
        } else {
            enqueueDownload();
            finish();
        }
    }

    private boolean isHls() {
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.US);
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.US);
        return "hls".equals(mediaKind) || lowerUrl.contains(".m3u8") || lowerMime.contains("mpegurl");
    }

    private void enqueueDownload() {
        try {
            Uri uri = Uri.parse(url);
            String filename = buildFilename();
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(filename)
                    .setDescription("Baixando vídeo com Aven")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Aven/" + filename);

            addRequestHeaders(request);
            if (!mime.isEmpty()) request.setMimeType(mime.split(";", 2)[0].trim());

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                Toast.makeText(this, "Serviço de download indisponível.", Toast.LENGTH_LONG).show();
                return;
            }
            manager.enqueue(request);
            Toast.makeText(this, "Download iniciado em Downloads/Aven", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Não foi possível iniciar o download: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addRequestHeaders(DownloadManager.Request request) {
        if (pageUrl.startsWith("http")) request.addRequestHeader("Referer", pageUrl);
        if (!cookies.isEmpty()) request.addRequestHeader("Cookie", cookies);
        if (!userAgent.isEmpty()) request.addRequestHeader("User-Agent", userAgent);
    }

    private void startHlsDownload() {
        TextView progress = new TextView(this);
        progress.setText("Baixando vídeo…\n\nO Aven está reunindo os segmentos do stream.");
        progress.setTextSize(17);
        progress.setGravity(Gravity.CENTER);
        progress.setPadding(40, 40, 40, 40);
        setContentView(progress);

        new Thread(() -> {
            Uri mediaStoreUri = null;
            File legacyFile = null;
            try {
                HlsPlaylist playlist = resolveHlsPlaylist(url, 0);
                if (playlist.segments.isEmpty()) throw new IllegalStateException("playlist HLS sem segmentos");

                String extension = playlist.fragmentedMp4 ? "mp4" : "ts";
                String filename = buildBaseFilename() + "." + extension;
                String outputMime = playlist.fragmentedMp4 ? "video/mp4" : "video/mp2t";

                OutputStream output;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, outputMime);
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Aven");
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    mediaStoreUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (mediaStoreUri == null) throw new IllegalStateException("não foi possível criar o arquivo de destino");
                    output = resolver.openOutputStream(mediaStoreUri, "w");
                    if (output == null) throw new IllegalStateException("não foi possível abrir o arquivo de destino");
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Aven");
                    if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("não foi possível criar Downloads/Aven");
                    legacyFile = new File(dir, filename);
                    output = new FileOutputStream(legacyFile);
                }

                try (OutputStream out = output) {
                    if (!playlist.initSegment.isEmpty()) copyUrlTo(playlist.initSegment, out);
                    int total = playlist.segments.size();
                    for (int i = 0; i < total; i++) {
                        copyUrlTo(playlist.segments.get(i), out);
                        final int done = i + 1;
                        runOnUiThread(() -> progress.setText("Baixando vídeo…\n\nSegmentos: " + done + "/" + total));
                    }
                    out.flush();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaStoreUri != null) {
                    ContentValues done = new ContentValues();
                    done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    getContentResolver().update(mediaStoreUri, done, null, null);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Vídeo salvo em Downloads/Aven", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Throwable error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaStoreUri != null) {
                    try { getContentResolver().delete(mediaStoreUri, null, null); } catch (Throwable ignored) {}
                }
                if (legacyFile != null) {
                    try { legacyFile.delete(); } catch (Throwable ignored) {}
                }
                String message = error.getMessage() == null ? "erro desconhecido" : error.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Não foi possível baixar este stream: " + message, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, "Aven-HLS-Downloader").start();
    }

    private HlsPlaylist resolveHlsPlaylist(String playlistUrl, int depth) throws Exception {
        if (depth > 4) throw new IllegalStateException("playlist HLS muito profunda");
        String body = readText(playlistUrl);
        if (!body.contains("#EXTM3U")) throw new IllegalStateException("resposta não é uma playlist HLS");

        String[] lines = body.replace("\r", "").split("\n");
        String bestVariant = "";
        long bestBandwidth = -1;
        boolean waitingVariant = false;
        long pendingBandwidth = 0;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                waitingVariant = true;
                pendingBandwidth = parseBandwidth(line);
                continue;
            }
            if (waitingVariant && !line.isEmpty() && !line.startsWith("#")) {
                if (pendingBandwidth >= bestBandwidth) {
                    bestBandwidth = pendingBandwidth;
                    bestVariant = resolveUrl(playlistUrl, line);
                }
                waitingVariant = false;
            }
        }

        if (!bestVariant.isEmpty()) return resolveHlsPlaylist(bestVariant, depth + 1);

        for (String raw : lines) {
            String line = raw.trim().toUpperCase(Locale.US);
            if (line.startsWith("#EXT-X-KEY:") && !line.contains("METHOD=NONE")) {
                throw new SecurityException("stream criptografado/DRM não é suportado");
            }
        }

        String initSegment = "";
        List<String> segments = new ArrayList<>();
        boolean fragmented = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("#EXT-X-MAP:")) {
                String uri = quotedAttribute(line, "URI");
                if (!uri.isEmpty()) {
                    initSegment = resolveUrl(playlistUrl, uri);
                    fragmented = true;
                }
                continue;
            }
            if (line.isEmpty() || line.startsWith("#")) continue;
            String segment = resolveUrl(playlistUrl, line);
            segments.add(segment);
            String lower = segment.toLowerCase(Locale.US);
            if (lower.contains(".m4s") || lower.contains(".mp4")) fragmented = true;
        }
        return new HlsPlaylist(playlistUrl, segments, initSegment, fragmented);
    }

    private long parseBandwidth(String line) {
        try {
            int start = line.indexOf("BANDWIDTH=");
            if (start < 0) return 0;
            start += "BANDWIDTH=".length();
            int end = line.indexOf(',', start);
            String value = end < 0 ? line.substring(start) : line.substring(start, end);
            return Long.parseLong(value.trim());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String quotedAttribute(String line, String name) {
        String token = name + "=\"";
        int start = line.indexOf(token);
        if (start < 0) return "";
        start += token.length();
        int end = line.indexOf('"', start);
        return end < 0 ? "" : line.substring(start, end);
    }

    private String readText(String value) throws Exception {
        HttpURLConnection connection = openConnection(value);
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
                if (body.length() > 5_000_000) throw new IllegalStateException("playlist HLS grande demais");
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private void copyUrlTo(String value, OutputStream out) throws Exception {
        HttpURLConnection connection = openConnection(value);
        try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) out.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String value) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("Accept", "*/*");
        if (!pageUrl.isEmpty()) connection.setRequestProperty("Referer", pageUrl);
        if (!cookies.isEmpty()) connection.setRequestProperty("Cookie", cookies);
        if (!userAgent.isEmpty()) connection.setRequestProperty("User-Agent", userAgent);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 400) throw new IllegalStateException("servidor respondeu HTTP " + code);
        return connection;
    }

    private String resolveUrl(String base, String relative) throws Exception {
        return new URL(new URL(base), relative).toString();
    }

    private String buildFilename() {
        String safeTitle = buildBaseFilename();
        String cleanMime = mime == null ? "" : mime.split(";", 2)[0].trim().toLowerCase(Locale.US);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime);
        if (extension == null || extension.isEmpty()) {
            String path = Uri.parse(url).getLastPathSegment();
            if (path != null) {
                int dot = path.lastIndexOf('.');
                if (dot >= 0 && dot < path.length() - 1) {
                    String ext = path.substring(dot + 1).toLowerCase(Locale.US);
                    int query = ext.indexOf('?');
                    if (query >= 0) ext = ext.substring(0, query);
                    if (ext.matches("mp4|webm|m4v|mov|ogv|mp3|m4a|aac")) extension = ext;
                }
            }
        }
        if (extension == null || extension.isEmpty()) {
            extension = cleanMime.startsWith("audio/") ? "m4a" : "mp4";
        }
        return safeTitle + "." + extension;
    }

    private String buildBaseFilename() {
        String safeTitle = title == null ? "video" : title.trim();
        safeTitle = safeTitle.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (safeTitle.isEmpty()) safeTitle = "video";
        if (safeTitle.length() > 80) safeTitle = safeTitle.substring(0, 80).trim();
        return safeTitle;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_STORAGE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (isHls()) startHlsDownload();
            else {
                enqueueDownload();
                finish();
            }
        } else {
            Toast.makeText(this, "Permissão de armazenamento negada.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
