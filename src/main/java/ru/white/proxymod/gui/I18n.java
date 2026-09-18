package ru.white.proxymod.gui;

public final class I18n {

    private I18n() {}

    public static boolean isRu(String lang) {
        return "ru".equalsIgnoreCase(lang);
    }

    public static String title(String lang) {
        return isRu(lang) ? "НАСТРОЙКИ ПРОКСИ" : "PROXY SETTINGS";
    }

    public static String subtitle(String lang) {
        return isRu(lang) ? "// СЕТЬ" : "// NETWORK";
    }

    public static String useProxy(String lang) {
        return isRu(lang) ? "Использовать прокси" : "Enable Proxy";
    }

    public static String protocol(String lang) {
        return isRu(lang) ? "ПРОТОКОЛ" : "PROTOCOL";
    }

    public static String hostLabel(String lang) {
        return isRu(lang) ? "ХОСТ / IP АДРЕС" : "HOST / IP ADDRESS";
    }

    public static String portLabel(String lang) {
        return isRu(lang) ? "ПОРТ" : "PORT";
    }

    public static String userLabel(String lang) {
        return isRu(lang) ? "ЛОГИН (ОПЦИОНАЛЬНО)" : "USERNAME (OPTIONAL)";
    }

    public static String passLabel(String lang) {
        return isRu(lang) ? "ПАРОЛЬ (ОПЦИОНАЛЬНО)" : "PASSWORD (OPTIONAL)";
    }

    public static String userPlaceholder(String lang) {
        return isRu(lang) ? "Логин" : "Username";
    }

    public static String passPlaceholder(String lang) {
        return isRu(lang) ? "Пароль" : "Password";
    }

    public static String checkBtn(String lang) {
        return isRu(lang) ? "Проверить" : "Check";
    }

    public static String cancelBtn(String lang) {
        return isRu(lang) ? "Отмена" : "Cancel";
    }

    public static String saveBtn(String lang) {
        return isRu(lang) ? "Сохранить" : "Save";
    }

    public static String testing(String lang) {
        return isRu(lang) ? "⏳ Проверка..." : "⏳ Testing...";
    }

    public static String success(String lang, long ping) {
        return isRu(lang) ? "Подключение успешно (" + ping + " ms)" : "Connection successful (" + ping + " ms)";
    }

    public static String failed(String lang, String err) {
        return (isRu(lang) ? "Ошибка: " : "Failed: ") + err;
    }

    public static String invalidPort(String lang) {
        return isRu(lang) ? "Неверный порт" : "Invalid port";
    }

    public static String hudOption(String lang) {
        return isRu(lang) ? "HUD в игре" : "In-Game HUD";
    }

    public static String failoverOption(String lang) {
        return isRu(lang) ? "Авто-переключение" : "Auto Failover";
    }

    public static String importClipboard(String lang) {
        return isRu(lang) ? "Импорт" : "Import";
    }

    public static String profileLabel(String lang) {
        return isRu(lang) ? "ПРОФИЛЬ" : "PROFILE";
    }

    public static String addProfile(String lang) {
        return isRu(lang) ? "+ Профиль" : "+ Profile";
    }

    public static String delProfile(String lang) {
        return isRu(lang) ? "Удалить" : "Delete";
    }

    public static String tabGeneral(String lang) {
        return isRu(lang) ? "Общие" : "General";
    }

    public static String tabRules(String lang) {
        return isRu(lang) ? "Правила" : "Rules";
    }

    public static String dnsOption(String lang) {
        return isRu(lang) ? "Защита DNS" : "DNS Leak Protect";
    }

    public static String addRule(String lang) {
        return isRu(lang) ? "+ Правило" : "+ Rule";
    }

    public static String delRule(String lang) {
        return isRu(lang) ? "Удалить" : "Delete";
    }

    public static String rulePatternLabel(String lang) {
        return isRu(lang) ? "ШАБЛОН СЕРВЕРА (НАПР. *.HYPIXEL.NET)" : "SERVER PATTERN (E.G. *.HYPIXEL.NET)";
    }

    public static String ruleActionLabel(String lang) {
        return isRu(lang) ? "ДЕЙСТВИЕ" : "ACTION";
    }

    public static String ruleDirect(String lang) {
        return isRu(lang) ? "Напрямую" : "Direct";
    }

    public static String ruleProfile(String lang) {
        return isRu(lang) ? "Прокси" : "Proxy";
    }

    public static String noRules(String lang) {
        return isRu(lang) ? "Правил нет. Нажмите «+ Правило»" : "No rules. Click «+ Rule»";
    }
}
