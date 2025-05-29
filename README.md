# 🤖 XrayBot — Telegram-бот для 3X-UI (Xray)

Этот бот автоматически уведомляет пользователей о сроке действия их ключей доступа к VPN/Proxy, развернутых через Xray с панелью 3X-UI.

## 🚀 Возможности
- 📬 Уведомление пользователей в Telegram перед окончанием срока действия ключа
- 🔐 Проверка статуса пользователя по команде `/status`
- 🔄 Автоматическая регистрация пользователя при старте
- 🛠 Поддержка локального API (на том же VPS, что и Xray)

## 💡 Команды
| Команда     | Описание                                           |
|-------------|----------------------------------------------------|
| `/start`    | Регистрация пользователя                           |
| `/status`   | Просмотр использованного трафика и срока действия |

## 🧱 Стек технологий
- Java 21
- Spring Boot
- TelegramBots Java API
- RestTemplate
- systemd unit service (для запуска на Linux-сервере)

## ⚙️ Настройка

1. Скопируй `.env`-переменные в `application.properties`:

```properties
xray.base-url=https://example.com:port/xraysuper
xray.username=your_admin
xray.password=your_password
xray.bot-token=bot_token_from_botfather
xray.bot-username=your_bot_username
