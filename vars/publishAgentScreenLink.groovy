/**
 * Публикует ссылку на экран агента, если у узла задана переменная окружения AGENT_SCREEN_URL
 * (например, адрес noVNC для Xvfb агента). Ссылка пишется в лог сборки и, если установлен
 * плагин badge, добавляется бейджем на страницу сборки. На каждый узел — один бейдж.
 */
void call() {
    String url = env.AGENT_SCREEN_URL
    if (!url) {
        return
    }

    String nodeName = env.NODE_NAME ?: 'agent'
    echo "Экран агента ${nodeName}: ${url}"

    String badgeId = "agent-screen-${nodeName}"
    try {
        removeBadges(id: badgeId)
        addBadge(
            id: badgeId,
            icon: 'symbol-desktop-outline plugin-ionicons-api',
            text: "Экран агента ${nodeName}",
            link: url,
            target: '_blank'
        )
    } catch (NoSuchMethodError ignored) {
        echo 'Плагин badge не установлен, ссылка на экран агента доступна только в логе'
    }
}
