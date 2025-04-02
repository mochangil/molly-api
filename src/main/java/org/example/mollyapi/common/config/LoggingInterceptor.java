package org.example.mollyapi.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String QUERY_COUNT_LOG_FORMAT = "URL: {}, QUERY_COUNT: {}";
    private static final String QUERY_COUNT_WARNING_LOG_FORMAT = "쿼리가 {}번 이상 실행되었습니다.";

    private final ApiQueryCounter apiQueryCounter;

    public LoggingInterceptor(final ApiQueryCounter apiQueryCounter) {
        this.apiQueryCounter = apiQueryCounter;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        final int queryCount = apiQueryCounter.getCount();

        // log.info( QUERY_COUNT_LOG_FORMAT ,request.getRequestURI(), queryCount);

        if (queryCount >= 100) {
            log.warn(QUERY_COUNT_WARNING_LOG_FORMAT, queryCount);
        }
    }
}
