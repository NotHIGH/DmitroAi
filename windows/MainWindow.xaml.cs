using System.IO;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;

namespace DimaAI.Windows;

public partial class MainWindow : Window
{
    private static readonly string HistoryPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "DimaAI",
        "chat.json");

    private readonly ModelDownloader _modelDownloader = new();
    private readonly QwenChatService _qwen = new();
    private readonly List<ConversationTurn> _history = [];
    private readonly CancellationTokenSource _lifetime = new();
    private bool _internetEnabled = true;
    private bool _busy;

    public MainWindow()
    {
        InitializeComponent();
        LoadHistory();
        ModelStateText.Text = _modelDownloader.IsDownloaded
            ? "Модель скачана. Нажми, чтобы загрузить её в память."
            : "Готовая модель скачивается один раз и остаётся на компьютере.";
        ModelButton.Content = _modelDownloader.IsDownloaded
            ? "Запустить Qwen"
            : "Скачать Qwen · 1,28 ГБ";
    }

    private async void ModelButton_Click(object sender, RoutedEventArgs e)
    {
        if (_busy || _qwen.IsLoaded)
            return;

        _busy = true;
        ModelButton.IsEnabled = false;
        ModelProgress.Visibility = Visibility.Visible;
        try
        {
            var downloadProgress = new Progress<int>(value =>
            {
                ModelProgress.Value = value;
                ModelButton.Content = value == 100 ? "Загрузка завершена" : $"Скачиваю Qwen · {value}%";
            });
            var modelPath = await _modelDownloader.DownloadAsync(downloadProgress, _lifetime.Token);

            ModelStateText.Text = "Загружаю Qwen в оперативную память…";
            ModelProgress.IsIndeterminate = true;
            ModelButton.Content = "Запускаю Qwen…";
            var loadProgress = new Progress<float>(value =>
                ModelStateText.Text = $"Загружаю модель в память · {value:P0}");
            await _qwen.LoadAsync(modelPath, loadProgress, _lifetime.Token);

            ModelStateText.Text = "Qwen работает локально и может отвечать без интернета.";
            ModelButton.Content = "Qwen готова · офлайн";
            ConnectionStateText.Text = "Модель готова на этом компьютере";
        }
        catch (OperationCanceledException)
        {
            ModelStateText.Text = "Загрузка отменена.";
            ModelButton.Content = _modelDownloader.IsDownloaded ? "Запустить Qwen" : "Скачать Qwen · 1,28 ГБ";
        }
        catch (Exception error)
        {
            ModelStateText.Text = error.Message;
            ModelButton.Content = _modelDownloader.IsDownloaded ? "Повторить запуск Qwen" : "Повторить загрузку Qwen";
        }
        finally
        {
            ModelProgress.IsIndeterminate = false;
            ModelProgress.Visibility = Visibility.Collapsed;
            ModelButton.IsEnabled = true;
            _busy = false;
        }
    }

    private void InternetButton_Click(object sender, RoutedEventArgs e)
    {
        _internetEnabled = !_internetEnabled;
        InternetButton.Content = _internetEnabled ? "Интернет: ВКЛ" : "Интернет: ВЫКЛ";
        InternetButton.Foreground = _internetEnabled
            ? (Brush)FindResource("PrimaryTextBrush")
            : (Brush)FindResource("MutedTextBrush");
        ConnectionStateText.Text = _internetEnabled
            ? "Ссылки HTTPS можно читать в чате"
            : "Интернет выключен · Qwen работает локально";
    }

    private void NewChatButton_Click(object sender, RoutedEventArgs e)
    {
        _history.Clear();
        ChatMessages.Children.Clear();
        WelcomePanel.Visibility = Visibility.Visible;
        SaveHistory();
    }

    private async void SendButton_Click(object sender, RoutedEventArgs e) => await SendAsync();

    private async void MessageInput_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter && Keyboard.Modifiers != ModifierKeys.Shift)
        {
            e.Handled = true;
            await SendAsync();
        }
    }

    private async Task SendAsync()
    {
        var text = MessageInput.Text.Trim();
        if (text.Length == 0 || _busy)
            return;
        MessageInput.Clear();
        WelcomePanel.Visibility = Visibility.Collapsed;
        AddMessage(text, assistant: false);
        _history.Add(new ConversationTurn("user", text));
        SaveHistory();

        if (!_qwen.IsLoaded)
        {
            AddMessage("Сначала скачай и запусти Qwen кнопкой слева. Без модели я не буду подменять AI готовыми фразами.", assistant: true);
            _history.Add(new ConversationTurn("assistant", "Сначала скачай и запусти Qwen кнопкой слева."));
            SaveHistory();
            return;
        }

        _busy = true;
        SendButton.IsEnabled = false;
        ConnectionStateText.Text = "Qwen думает локально…";
        try
        {
            string? pageUrl = null;
            string? pageText = null;
            if (_internetEnabled && WebsiteReader.FindUrl(text) is { } url)
            {
                ConnectionStateText.Text = "Читаю публичную HTTPS-страницу…";
                try
                {
                    var page = await WebsiteReader.ReadAsync(url, _lifetime.Token);
                    pageUrl = page?.Url ?? url;
                    pageText = page?.Text;
                }
                catch (Exception error)
                {
                    pageUrl = url;
                    ConnectionStateText.Text = $"Не удалось прочитать страницу: {error.Message}";
                }
            }

            ConnectionStateText.Text = "Qwen формирует ответ…";
            var reply = await _qwen.GenerateAsync(_history, pageUrl, pageText, _lifetime.Token);
            AddMessage(reply, assistant: true);
            _history.Add(new ConversationTurn("assistant", reply));
            SaveHistory();
            ConnectionStateText.Text = _internetEnabled
                ? "Ответ создан локально · интернет включён для ссылок"
                : "Ответ создан локально · офлайн";
        }
        catch (OperationCanceledException)
        {
            ConnectionStateText.Text = "Запрос отменён.";
        }
        catch (Exception error)
        {
            var reply = $"Не удалось получить ответ: {error.Message}";
            AddMessage(reply, assistant: true);
            _history.Add(new ConversationTurn("assistant", reply));
            SaveHistory();
            ConnectionStateText.Text = "Ошибка генерации локального ответа";
        }
        finally
        {
            _busy = false;
            SendButton.IsEnabled = true;
            MessageInput.Focus();
        }
    }

    private void AddMessage(string text, bool assistant)
    {
        var card = new Border
        {
            Background = (Brush)FindResource(assistant ? "PanelBrush" : "RaisedBrush"),
            CornerRadius = new CornerRadius(14),
            Padding = new Thickness(16, 12, 16, 12),
            Margin = new Thickness(0, 0, 0, 12),
            MaxWidth = 690,
            HorizontalAlignment = assistant ? HorizontalAlignment.Left : HorizontalAlignment.Right
        };
        var content = new StackPanel();
        content.Children.Add(new TextBlock
        {
            Text = assistant ? "DIMA" : "ВЫ",
            Foreground = assistant ? (Brush)FindResource("AccentBrush") : (Brush)FindResource("MutedTextBrush"),
            FontSize = 10,
            FontWeight = FontWeights.Bold,
            Margin = new Thickness(0, 0, 0, 5)
        });
        content.Children.Add(new TextBlock
        {
            Text = text,
            Foreground = (Brush)FindResource("PrimaryTextBrush"),
            TextWrapping = TextWrapping.Wrap,
            FontSize = 14,
            LineHeight = 22
        });
        card.Child = content;
        ChatMessages.Children.Add(card);
        ChatScroll.ScrollToEnd();
    }

    private void LoadHistory()
    {
        try
        {
            if (!File.Exists(HistoryPath))
                return;
            var saved = JsonSerializer.Deserialize<List<ConversationTurn>>(File.ReadAllText(HistoryPath));
            if (saved is null)
                return;
            _history.AddRange(saved.TakeLast(100));
            if (_history.Count == 0)
                return;
            WelcomePanel.Visibility = Visibility.Collapsed;
            foreach (var turn in _history)
                AddMessage(turn.Content, turn.Role == "assistant");
        }
        catch
        {
            _history.Clear();
        }
    }

    private void SaveHistory()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(HistoryPath)!);
            File.WriteAllText(HistoryPath, JsonSerializer.Serialize(_history.TakeLast(100)));
        }
        catch
        {
            ConnectionStateText.Text = "Не удалось сохранить историю чата.";
        }
    }

    protected override void OnClosed(EventArgs e)
    {
        _lifetime.Cancel();
        _qwen.Dispose();
        _lifetime.Dispose();
        base.OnClosed(e);
    }
}