package com.xraybot.task;

import com.xraybot.service.UserRegistry;
import com.xraybot.service.XrayApiService;
import com.xraybot.service.XrayApiService.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private final XrayApiService xrayApiService;
    private final UserRegistry userRegistry;
    private final AbsSender bot;
    private final int inboundId = 2;

    public NotificationScheduler(XrayApiService xrayApiService,
                                 UserRegistry userRegistry,
                                 AbsSender bot) {
        this.xrayApiService = xrayApiService;
        this.userRegistry = userRegistry;
        this.bot = bot;
    }

    @Scheduled(cron = "${xray.cron.expiry-check}")
    public void sendExpiryNotifications() {
        log.info("Выполняется проверка клиентов на окончание срока действия...");

        try {
            boolean loggedIn = xrayApiService.login();
            if (!loggedIn) {
                log.warn("Не удалось авторизоваться в 3X-UI");
                return;
            }

            List<Client> clients = xrayApiService.getClients(inboundId);
            long now = Instant.now().getEpochSecond();

            for (Client client : clients) {
                if (client.expiryTime() == 0) {
                    // Нет ограничения по времени — пропускаем
                    continue;
                }

                long expirySeconds  = client.expiryTime() / 1000;
                long secondsLeft = expirySeconds - now;
                long daysLeft = secondsLeft / (60 * 60 * 24);

                if (daysLeft <= 2) {
                    String email = client.email();
                    Long chatId = userRegistry.getChatId(email);

                    if (chatId != null) {
                        String text = String.format(
                                "🔔 Ваш доступ по ключу [%s] истекает через %d дн.\n" +
                                        "Пожалуйста, продлите доступ во избежание отключения.",
                                email, Math.max(daysLeft, 0)
                        );

                        SendMessage message = new SendMessage();
                        message.setChatId(chatId.toString());
                        message.setText(text);

                        bot.execute(message);
                        log.info("Уведомление отправлено для {} (chatId={})", email, chatId);
                    } else {
                        log.info("Для пользователя {} не найден chatId — возможно, он не запускал /start", email);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при выполнении уведомлений:", e);
        }
    }
}
