package com.innowise.userservice.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@ConditionalOnMissingBean(DevelopmentLoggingAspect.class)
public class ProductionLoggingAspect {
    @Pointcut("execution(public * com.innowise.userservice.service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        var className = pjp.getTarget().getClass().getSimpleName();
        var methodName = pjp.getSignature().getName();

        var start = System.nanoTime();
        log.debug("Entering method: {}.{}", className, methodName);

        Object result = pjp.proceed();

        var executionTime = (System.nanoTime() - start) / 1000000;
        log.debug("Exiting method: {}.{}. Execution time: {} ms.",
                className, methodName, executionTime);

        return result;
    }
}
