package app.aven.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.util.Locale;

public class MediaPromptActivity extends Activity {
    public static final String EXTRA_URL = "aven.media.url";
    public static final String EXTRA_PAGE_URL = "aven.media.page_url";
    public static final String EXTRA_TITLE = "aven.media.title";
    public static final String EXTRA_MIME = "aven.media.mime";
    public static final String EXTRA_COOKIES = "aven.media.cookies";
    public static final String EXTRA_USER_AGENT = "aven.media.user_agent";
    public static final String EXTRA_DURATION = "aven.media.duration";

    private static final int REQUEST_STORAGE = 7201;

    private String url;
    private String pageUrl;
    private String title;
    private String mime;
    private String cookies;
    private String userAgent;
    private double duration;

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

        String message = "Aven detectou um vídeo em reprodução"
                + (site.isEmpty() ? "." : " em " + site + ".")
                + durationText
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
        enqueueDownload();
        finish();
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

            if (pageUrl.startsWith("http")) request.addRequestHeader("Referer", pageUrl);
            if (!cookies.isEmpty()) request.addRequestHeader("Cookie", cookies);
            if (!userAgent.isEmpty()) request.addRequestHeader("User-Agent", userAgent);
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

    private String buildFilename() {
        String safeTitle = title == null ? "video" : title.trim();
        safeTitle = safeTitle.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (safeTitle.isEmpty()) safeTitle = "video";
        if (safeTitle.length() > 80) safeTitle = safeTitle.substring(0, 80).trim();

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
                    if (ext.matches("mp4|webm|m4v|mov|ogv")) extension = ext;
                }
            }
        }
        if (extension == null || extension.isEmpty()) {
            extension = cleanMime.startsWith("video/") ? "mp4" : "video";
        }
        return safeTitle + "." + extension;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_STORAGE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload();
        } else {
            Toast.makeText(this, "Permissão de armazenamento negada.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
