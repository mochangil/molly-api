package org.example.mollyapi.order.event.V2.aspect;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.mollyapi.order.event.V2.EventFutureRegistry;
import org.example.mollyapi.order.event.V2.EventFutureType;
import org.example.mollyapi.order.event.V2.annotation.HandleFutureEvent;
import org.example.mollyapi.order.event.V2.event.order.BaseEvent;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class FutureEventAspect {

    private final EventFutureRegistry eventFutureRegistry;

    @Around("@annotation(handleFutureEvent)")
    public Object handleFuture(ProceedingJoinPoint joinPoint, HandleFutureEvent handleFutureEvent) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof BaseEvent event)) {
            return joinPoint.proceed();
        }

        String tossOrderId = event.tossOrderId();
        EventFutureType eventType = handleFutureEvent.value();

//        eventFutureRegistry.registerFuture(eventType, tossOrderId);

        try {
            Object result = joinPoint.proceed();
            eventFutureRegistry.completeFuture(eventType, tossOrderId);
            return result;
        } catch (Throwable ex) {
            log.error("Error processing event {}: {}", eventType, ex.getMessage());
            throw ex;
        }
    }
}
