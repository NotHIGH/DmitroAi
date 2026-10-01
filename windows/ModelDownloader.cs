using System.IO;
using System.Net;
using System.Net.Http;

namespace DimaAI.Windows;

public sealed class ModelDownloader
{
    public const long ModelSizeBytes = 1_282_439_264;
    public const string ModelFileName = "Qwen3-1.7B-Q4_K_M.gguf";
    private const string ModelUrl =
        "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf?download=true";

    private static readonly HttpClient Client = new(new HttpClientHandler
    {
        AllowAutoRedirect = true,
        AutomaticDecompression = DecompressionMethods.None
    })
    {
        Timeout = Timeout.InfiniteTimeSpan
    };

    private readonly string _modelPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "DimaAI",
        "Models",
        ModelFileName);

    public bool IsDownloaded => File.Exists(_modelPath) && new FileInfo(_modelPath).Length >= ModelSizeBytes * 99 / 100;
    public string ModelPath => _modelPath;

    public async Task<string> DownloadAsync(IProgress<int> progress, CancellationToken cancellationToken)
    {
        if (IsDownloaded)
            return _modelPath;

        Directory.CreateDirectory(Path.GetDirectoryName(_modelPath)!);
        var drive = new DriveInfo(Path.GetPathRoot(_modelPath)!);
        if (drive.AvailableFreeSpace < ModelSizeBytes + 128L * 1024 * 1024)
            throw new IOException("Освободи минимум 1,4 ГБ на диске Windows.");

        var temporaryPath = _modelPath + ".part";
        try
        {
            using var response = await Client.GetAsync(
                ModelUrl,
                HttpCompletionOption.ResponseHeadersRead,
                cancellationToken);
            response.EnsureSuccessStatusCode();

            var total = response.Content.Headers.ContentLength is > 0
                ? response.Content.Headers.ContentLength.Value
                : ModelSizeBytes;
            await using var source = await response.Content.ReadAsStreamAsync(cancellationToken);
            await using var destination = new FileStream(
                temporaryPath, FileMode.Create, FileAccess.Write, FileShare.None,
                1024 * 128, FileOptions.Asynchronous | FileOptions.SequentialScan);

            var buffer = new byte[1024 * 128];
            long received = 0;
            var lastProgress = -1;
            while (true)
            {
                var count = await source.ReadAsync(buffer, cancellationToken);
                if (count == 0)
                    break;
                await destination.WriteAsync(buffer.AsMemory(0, count), cancellationToken);
                received += count;
                var current = (int)Math.Clamp(received * 100 / total, 0, 99);
                if (current != lastProgress)
                {
                    lastProgress = current;
                    progress.Report(current);
                }
            }
            await destination.FlushAsync(cancellationToken);

            if (received < ModelSizeBytes * 99 / 100)
                throw new IOException("Загрузка неполная. Попробуй ещё раз через стабильный Wi-Fi.");

            File.Move(temporaryPath, _modelPath, overwrite: true);
            progress.Report(100);
            return _modelPath;
        }
        catch
        {
            if (File.Exists(temporaryPath))
                File.Delete(temporaryPath);
            throw;
        }
    }
}