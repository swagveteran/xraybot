package com.xraybot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xraybot.config.XrayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

@Service
public class XrayApiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final XrayProperties xrayProperties;
    private static final Logger log = LoggerFactory.getLogger(XrayApiService.class);

    private String sessionCookie;

    public XrayApiService(XrayProperties xrayProperties) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.xrayProperties = xrayProperties;
    }

    public boolean login() throws Exception {

        if (sessionCookie != null) {
            if (isSessionValid()) {
                return true;
            } else {
                log.info("Сессия недействительна — выполняем повторный вход...");
                sessionCookie = null;
            }
        }

        String url = xrayProperties.getBaseUrl() + "/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "username=" + xrayProperties.getUsername() +
                "&password=" + xrayProperties.getPassword();

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.isEmpty()) {
            log.warn("Login response does not contain Set-Cookie header");
            return false;
        }

        for (String rawCookie : cookies) {
            try {
                List<HttpCookie> parsedCookies = HttpCookie.parse(rawCookie);
                for (HttpCookie cookie : parsedCookies) {
                    if ("3x-ui".equals(cookie.getName())) {
                        sessionCookie = cookie.getName() + "=" + cookie.getValue();
                        log.info("Login successful. Session cookie: {}...", sessionCookie.substring(0, Math.min(25, sessionCookie.length())));
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse cookie: {}", rawCookie, e);
            }
        }

        log.warn("Session cookie '3x-ui' not found after login");
        return false;
    }

    public List<Client> getClients(int inboundId) throws Exception {
        if (sessionCookie == null) {
            throw new IllegalStateException("Not logged in");
        }

        String url = xrayProperties.getBaseUrl() + "/panel/api/inbounds/get/" + inboundId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Failed to get clients, status: " + response.getStatusCode().value());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        boolean success = root.path("success").asBoolean(false);
        if (!success) {
            throw new Exception("API returned success=false");
        }

        String settingsJson = root.path("obj").path("settings").asText();
        if (settingsJson.isEmpty()) {
            throw new Exception("No settings found");
        }

        JsonNode settingsNode = objectMapper.readTree(settingsJson);
        JsonNode clientsNode = settingsNode.path("clients");

        List<Client> clients = new ArrayList<>();

        for (JsonNode clientNode : clientsNode) {
            String email = clientNode.path("email").asText();
            long expiryTime = clientNode.path("expiryTime").asLong(0);
            long up = clientNode.path("up").asLong(0);
            long down = clientNode.path("down").asLong(0);
            boolean enable = clientNode.path("enable").asBoolean(true);

            clients.add(new Client(email, expiryTime, up, down, enable));
        }

        return clients;
    }

    public record Client(String email, long expiryTime, long up, long down, boolean enable) {

        @Override
            public String toString() {
                return "Client{" +
                        "email='" + email + '\'' +
                        ", expiryTime=" + expiryTime +
                        '}';
            }
        }

    public Client getClientTraffic(String email) throws Exception {
        String url = xrayProperties.getBaseUrl() + "/panel/api/inbounds/getClientTraffics/" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Ошибка при получении трафика клиента");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        if (!root.path("success").asBoolean(false)) return null;

        JsonNode obj = root.path("obj");

        return new Client(
                obj.path("email").asText(),
                obj.path("expiryTime").asLong(),
                obj.path("up").asLong(),
                obj.path("down").asLong(),
                obj.path("enable").asBoolean(true)
        );
    }

    private boolean isSessionValid() {
        try {
            String url = xrayProperties.getBaseUrl() + "/panel/api/inbounds/list";
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.COOKIE, sessionCookie);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return response.getStatusCode().is2xxSuccessful() &&
                    objectMapper.readTree(response.getBody()).path("success").asBoolean(false);
        } catch (Exception e) {
            log.info("Проверка сессии не удалась: {}", e.getMessage());
            return false;
        }
    }
}
