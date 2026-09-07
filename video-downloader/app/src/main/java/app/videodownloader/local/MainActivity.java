package app.videodownloader.local;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;

public class MainActivity extends Activity {

    private static final String PROCESS_ID = "video_downloader_process";
    private static final int STORAGE_PERMISSION_REQUEST = 1001;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private EditText urlInput;
    private Button pasteButton;
    private Button downloadButton;
    private Button cancelButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView engineText;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean engineReady = false;
    private volatile boolean downloading = false;
    private volatile boolean cancelRequested = false;
    private String pendingUrlAfterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlInput = findViewById(R.id.urlInput);
        pasteButton = findViewById(R.id.pasteButton);
        downloadButton = findViewById(R.id.downloadButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        engineText = findViewById(R.id.engineText);

        pasteButton.setOnClickListener(v -> pasteFromClipboard());
        downloadButton.setOnClickListener(v -> startDownload());
        cancelButton.setOnClickListener(v -> cancelDownload());

        acceptSharedText(getIntent());
        initializeEngine();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        acceptSharedText(intent);
    }

    private void initializeEngine() {
        setControlsEnabled(false);
        engineText.setText(R.string.engine_preparing);
        executor.execute(() -> {
            try {
                YoutubeDL.getInstance().init(getApplicationContext());
                FFmpeg.getInstance().init(getApplicationContext());

                // A biblioteca Android vem com um yt-dlp antigo embutido. Atualizamos
                // para o canal estável atual antes de liberar o primeiro download.
                runOnUiThread(() -> engineText.setText(R.string.engine_updating));
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                            getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);
                } catch (Exception ignored) {
                    // A build também embute uma versão recente. Se a checagem online
                    // falhar, o app continua funcional com a versão empacotada.
                }

                engineReady = true;
                runOnUiThread(() -> {
                    engineText.setText(R.string.engine_ready);
                    statusText.setText(R.string.ready_to_download);
                    setControlsEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    engineText.setText(R.string.engine_error);
                    statusText.setText(shortError(e));
                    downloadButton.setEnabled(false);
                    pasteButton.setEnabled(true);
                });
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        pasteButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled && !downloading);
        cancelButton.setEnabled(downloading);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence value = clip.getItemAt(0).coerceToText(this);
        String url = extractFirstUrl(value == null ? "" : value.toString());
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.no_link_found, Toast.LENGTH_SHORT).show();
            return;
        }
        urlInput.setText(url);
        urlInput.setSelection(url.length());
    }

    private void acceptSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String url = extractFirstUrl(text == null ? "" : text);
        if (!TextUtils.isEmpty(url) && urlInput != null) {
            urlInput.setText(url);
            urlInput.setSelection(url.length());
        }
    }

    private String extractFirstUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) return null;
        String url = matcher.group();
        while (url.endsWith(")") || url.endsWith("]") || url.endsWith("}") || url.endsWith(",") || url.endsWith(".")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String normalizePlatformUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://x.com/")) {
            return "https://twitter.com/" + url.substring("https://x.com/".length());
        }
        if (lower.startsWith("http://x.com/")) {
            return "https://twitter.com/" + url.substring("http://x.com/".length());
        }
        if (lower.startsWith("https://www.x.com/")) {
            return "https://twitter.com/" + url.substring("https://www.x.com/".length());
        }
        if (lower.startsWith("http://www.x.com/")) {
            return "https://twitter.com/" + url.substring("http://www.x.com/".length());
        }
        return url;
    }

    private void startDownload() {
        if (!engineReady) {
            Toast.makeText(this, R.string.engine_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        if (downloading) return;

        String url = extractFirstUrl(urlInput.getText().toString().trim());
        if (TextUtils.isEmpty(url)) {
            urlInput.setError(getString(R.string.invalid_link));
            return;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            urlInput.setError(getString(R.string.invalid_link));
            return;
        }

        url = normalizePlatformUrl(url);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingUrlAfterPermission = url;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
            return;
        }

        beginDownload(url);
    }

    private void beginDownload(String url) {
        downloading = true;
        cancelRequested = false;
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(getString(R.string.starting_download, detectPlatform(url)));
        setControlsEnabled(true);

        executor.execute(() -> {
            File jobDir = null;
            try {
                File base = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (base == null) base = new File(getFilesDir(), "downloads");
                jobDir = new File(base, "job_" + System.currentTimeMillis());
                if (!jobDir.mkdirs() && !jobDir.isDirectory()) {
                    throw new IllegalStateException("Não foi possível preparar a pasta temporária.");
                }

                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--no-playlist");
                request.addOption("--no-mtime");
                request.addOption("--no-part");
                request.addOption("--force-ipv4");
                request.addOption("--socket-timeout", "20");
                request.addOption("--retries", "5");
                request.addOption("--fragment-retries", "5");
                request.addOption("--extractor-retries", "3");
                request.addOption("--concurrent-fragments", "3");
                request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
                request.addOption("--merge-output-format", "mp4");
                request.addOption("-o", new File(jobDir, "%(title)s.%(ext)s").getAbsolutePath());

                YoutubeDL.getInstance().execute(request, PROCESS_ID, (progress, eta, line) -> {
                    int pct = Math.max(0, Math.min(100, Math.round(progress)));
                    runOnUiThread(() -> {
                        progressBar.setProgress(pct);
                        statusText.setText(getString(R.string.downloading_progress, pct, Math.max(0L, eta)));
                    });
                    return Unit.INSTANCE;
                });

                if (cancelRequested) throw new InterruptedException("cancelado");

                File downloaded = findDownloadedVideo(jobDir);
                if (downloaded == null) {
                    throw new IllegalStateException("O download terminou, mas o arquivo de vídeo não foi localizado.");
                }

                final String savedName = publishToDownloads(downloaded);
                deleteRecursively(jobDir);
                jobDir = null;

                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    statusText.setText(getString(R.string.download_complete, savedName));
                    Toast.makeText(this, R.string.saved_to_downloads, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                final boolean wasCanceled = cancelRequested || e instanceof InterruptedException;
                runOnUiThread(() -> {
                    progressBar.setProgress(0);
                    statusText.setText(wasCanceled ? getString(R.string.download_canceled) : shortError(e));
                    if (!wasCanceled) Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show();
                });
            } finally {
                if (jobDir != null && cancelRequested) deleteRecursively(jobDir);
                downloading = false;
                cancelRequested = false;
                runOnUiThread(() -> setControlsEnabled(engineReady));
            }
        });
    }

    private void cancelDownload() {
        if (!downloading) return;
        cancelRequested = true;
        statusText.setText(R.string.canceling_download);
        try {
            YoutubeDL.getInstance().destroyProcessById(PROCESS_ID);
        } catch (Exception ignored) {
        }
    }

    private File findDownloadedVideo(File dir) {
        File[] files = dir.listFiles(file -> file.isFile() && isVideoFile(file.getName()));
        if (files == null || files.length == 0) return null;
        return Arrays.stream(files).max(Comparator.comparingLong(File::length)).orElse(null);
    }

    private boolean isVideoFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv") ||
                lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".3gp");
    }

    private String publishToDownloads(File source) throws Exception {
        String fileName = source.getName();
        String mime = mimeForName(fileName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Video Downloader");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (destination == null) throw new IllegalStateException("Falha ao criar o arquivo em Downloads.");

            try (InputStream in = new FileInputStream(source); OutputStream out = resolver.openOutputStream(destination)) {
                if (out == null) throw new IllegalStateException("Falha ao abrir o destino do arquivo.");
                copyStream(in, out);
            } catch (Exception e) {
                resolver.delete(destination, null, null);
                throw e;
            }

            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(destination, values, null, null);
            return fileName;
        }

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File folder = new File(root, "Video Downloader");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a pasta Downloads/Video Downloader.");
        }
        File destination = uniqueFile(folder, fileName);
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
            copyStream(in, out);
        }
        MediaScannerConnection.scanFile(this, new String[]{destination.getAbsolutePath()}, new String[]{mime}, null);
        return destination.getName();
    }

    private File uniqueFile(File folder, String originalName) {
        File file = new File(folder, originalName);
        if (!file.exists()) return file;
        int dot = originalName.lastIndexOf('.');
        String base = dot > 0 ? originalName.substring(0, dot) : originalName;
        String ext = dot > 0 ? originalName.substring(dot) : "";
        int i = 2;
        while (file.exists()) {
            file = new File(folder, base + " (" + i + ")" + ext);
            i++;
        }
        return file;
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[128 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    private String mimeForName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".3gp")) return "video/3gpp";
        return "video/mp4";
    }

    private String detectPlatform(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("tiktok.com")) return "TikTok";
        if (lower.contains("instagram.com")) return "Instagram";
        if (lower.contains("twitter.com") || lower.contains("x.com")) return "X/Twitter";
        return "vídeo";
    }

    private String shortError(Throwable error) {
        String message = error.getMessage();
        if (TextUtils.isEmpty(message)) message = error.getClass().getSimpleName();
        message = message.replace("ERROR:", "").trim();
        if (message.contains("no impersonate target is available")) {
            message = "O módulo de compatibilidade de navegador não foi carregado. Reabra o app e tente novamente.";
        } else if (message.contains("No address associated with hostname")) {
            message = "Falha de DNS ao acessar o site. Verifique a conexão e tente novamente.";
        }
        if (message.length() > 420) message = message.substring(0, 420) + "…";
        return getString(R.string.error_prefix, message);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingUrlAfterPermission != null) {
                String url = pendingUrlAfterPermission;
                pendingUrlAfterPermission = null;
                beginDownload(url);
            } else {
                pendingUrlAfterPermission = null;
                Toast.makeText(this, R.string.storage_permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (downloading) {
            cancelRequested = true;
            try {
                YoutubeDL.getInstance().destroyProcessById(PROCESS_ID);
            } catch (Exception ignored) {
            }
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
