package com.xraybot.bot;

import com.xraybot.config.XrayProperties;
import com.xraybot.service.UserRegistry;
import com.xraybot.service.XrayApiService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.List;

@Component
public class NotifyBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final UserRegistry userRegistry;
    private final XrayApiService xrayApiService;

    public NotifyBot(XrayProperties properties, UserRegistry userRegistry, XrayApiService xrayApiService) {
        this.botUsername = properties.getBotUsername();
        this.botToken = properties.getBotToken();
        this.userRegistry = userRegistry;
        this.xrayApiService = xrayApiService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            if (msg.hasText()) {
                String text = msg.getText();
                long chatId = msg.getChatId();
                String username = msg.getFrom().getUserName();

                if (text.equals("/start")) {
                    if (username == null || username.isEmpty()) {
                        sendMessage(chatId, "Пожалуйста, установите username в настройках Telegram, чтобы я мог вас идентифицировать.");
                        return;
                    }


                    userRegistry.registerUser(username, chatId);
                    sendMessage(chatId, "Привет, @" + username + "! Ты успешно зарегистрирован для получения уведомлений.\n\n" +
                            BotCommandRegistry.asHelpText());

                } else if (text.equals("/status")) {
                    if (username == null || username.isEmpty()) {
                        sendMessage(chatId, "Невозможно определить ваш username. Убедитесь, что он установлен в Telegram.");
                        return;
                    }

                    try {
                        boolean loggedIn = xrayApiService.login();
                        if (!loggedIn) {
                            sendMessage(chatId, "Не удалось авторизоваться в панели 3X-UI.");
                            return;
                        }

                        XrayApiService.Client client = xrayApiService.getClientTraffic(username);

                        if (client == null) {
                            sendMessage(chatId, "Клиент с именем " + username + " не найден.");
                            return;
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("📊 Статус клиента `").append(username).append("`\n");

                        String status = client.enable() ? "🟢 Активен" : "🔴 Заблокирован";
                        sb.append("🔐 Ключ: ").append(status).append("\n");

                        if (client.expiryTime() == 0) {
                            sb.append("⏳ Срок действия: не ограничен").append("\n");
                        } else {
                            long daysLeft = (client.expiryTime() / 1000 - Instant.now().getEpochSecond()) / (60 * 60 * 24);
                            sb.append("⏳ Срок действия: ").append(Math.max(daysLeft, 0)).append(" дн.").append("\n");
                        }

                        sb.append("🔁 Передано:\n");
                        sb.append("  ⬆️ Отправлено: ").append(humanReadableBytes(client.up())).append("\n");
                        sb.append("  ⬇️ Получено: ").append(humanReadableBytes(client.down())).append("\n");

                        SendMessage message = new SendMessage();
                        message.setChatId(Long.toString(chatId));
                        message.setText(sb.toString());
                        message.setParseMode("Markdown");

                        execute(message);

                    } catch (Exception e) {
                        sendMessage(chatId, "Произошла ошибка при получении статуса.");
                        e.printStackTrace();
                    }
                } else if (text.equals("/help")) {
                    sendMessage(chatId, BotCommandRegistry.asHelpText());
                } else {
                    sendMessage(chatId, "Я понимаю только команды /start, /status и /help.");
                }
            }
        }
    }

    @PostConstruct
    public void initCommands() {
        try {
            execute(new SetMyCommands(BotCommandRegistry.asTelegramCommandList(), null, null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(Long.toString(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String humanReadableBytes(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
