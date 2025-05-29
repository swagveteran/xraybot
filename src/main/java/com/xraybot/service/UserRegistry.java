package com.xraybot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserRegistry {

    private static final String FILE_PATH = "user_map.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserRegistry.class);

    // Потокобезопасная карта для хранения username → chatId
    private Map<String, Long> userMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public void registerUser(String username, long chatId) {
        userMap.put(username.toLowerCase(), chatId);
        saveToFile();
    }

    public Long getChatId(String username) {
        return userMap.get(username.toLowerCase());
    }

    private synchronized void saveToFile() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), userMap);
        } catch (IOException e) {
            log.error("Ошибка при отправке сообщения пользователю", e);
        }
    }

    private synchronized void loadFromFile() {
        try {
            File file = new File(FILE_PATH);
            if (file.exists()) {
                userMap = mapper.readValue(file, new TypeReference<>() {
                });
            }
        } catch (IOException e) {
            log.error("Ошибка при чтении файла", e);
        }
    }
}
