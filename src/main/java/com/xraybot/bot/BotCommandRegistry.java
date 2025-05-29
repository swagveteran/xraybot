package com.xraybot.bot;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;
import java.util.stream.Stream;

public enum BotCommandRegistry {
    START("/start", "Регистрация для получения уведомлений"),
    STATUS("/status", "Показать текущий статус подключения"),
    HELP("/help", "Список всех команд");

    private final String command;
    private final String description;

    BotCommandRegistry(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public static List<BotCommand> asTelegramCommandList() {
        return Stream.of(values())
                .map(c -> new BotCommand(c.getCommand(), c.getDescription()))
                .toList();
    }

    public static String asHelpText() {
        StringBuilder sb = new StringBuilder("📋 *Доступные команды:*\n");
        for (var cmd : values()) {
            sb.append(cmd.getCommand())
                    .append(" — ")
                    .append(cmd.getDescription())
                    .append("\n");
        }
        return sb.toString();
    }
}
