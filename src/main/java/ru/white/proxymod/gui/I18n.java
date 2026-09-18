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
        return isRu(lang) ? "⏳ Проверка подключения..." : "⏳ Testing connection...";
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
}
