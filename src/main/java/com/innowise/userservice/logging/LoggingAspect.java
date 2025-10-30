package com.innowise.userservice.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class LoggingAspect {
    @Pointcut("execution(public * com.innowise.userservice.service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        var method = pjp.getSignature().toShortString();
        var start = System.nanoTime();
        log.trace("Entering method: {}", method);

        Object result = pjp.proceed();
        log.debug("Method: {} executed. Time of execution: {} ms", method, (System.nanoTime() - start) / 1000000);
        return result;
    }
}
