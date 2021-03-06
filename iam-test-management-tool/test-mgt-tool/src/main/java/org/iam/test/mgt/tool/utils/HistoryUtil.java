package org.iam.test.mgt.tool.utils;

import org.iam.test.mgt.tool.domain.EcHistory;
import org.iam.test.mgt.tool.repository.HistoryRepository;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HistoryUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryUtil.class);

    @Autowired
    HistoryRepository hDao;

    public void addHistory(String logMsg, String slug, HttpServletRequest request, HttpSession session) {
        String user = (String) session.getAttribute("authName");
        LOGGER.info("{} by {} from {}", logMsg, user, getClientIpAddr(request));
        EcHistory history = new EcHistory();
        history.setChangeDate(new Date());
        history.setChangeUser(user);
        history.setChangeSummary(logMsg);
        history.setChangeIp(getClientIpAddr(request));
        history.setSlug(slug);
        hDao.save(history);

    }

    public String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
