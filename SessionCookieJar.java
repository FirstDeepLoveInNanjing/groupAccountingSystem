package com.union.accounting.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class SessionCookieJar implements CookieJar {
    private static final String PREFS_NAME = "session_cookies";
    private final Map<String, List<Cookie>> store = new HashMap<>();
    private final SharedPreferences preferences;

    public SessionCookieJar(Context context) {
        preferences = context == null ? null : context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        store.put(url.host(), new ArrayList<>(cookies));
        if (preferences != null) {
            StringBuilder serialized = new StringBuilder();
            for (Cookie cookie : cookies) {
                if (serialized.length() > 0) {
                    serialized.append("\n");
                }
                serialized.append(cookie.toString());
            }
            preferences.edit().putString(url.host(), serialized.toString()).apply();
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = store.get(url.host());
        if (cookies != null) {
            return cookies;
        }
        List<Cookie> loaded = new ArrayList<>();
        if (preferences != null) {
            String serialized = preferences.getString(url.host(), "");
            if (serialized != null && !serialized.isEmpty()) {
                String[] lines = serialized.split("\\n");
                for (String line : lines) {
                    Cookie cookie = Cookie.parse(url, line);
                    if (cookie != null) {
                        loaded.add(cookie);
                    }
                }
            }
        }
        store.put(url.host(), loaded);
        return loaded;
    }

    public void clear() {
        store.clear();
        if (preferences != null) {
            preferences.edit().clear().apply();
        }
    }
}
