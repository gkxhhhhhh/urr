package com.urr.api.util;

import javax.servlet.http.HttpServletRequest;

public class IpUtils {

    public static String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && xff.length() > 0) {
            // 可能是 "client, proxy1, proxy2"
            return xff.split(",")[0].trim();
        }
        String xrip = req.getHeader("X-Real-IP");
        if (xrip != null && xrip.length() > 0) return xrip.trim();
        return req.getRemoteAddr();
    }
}