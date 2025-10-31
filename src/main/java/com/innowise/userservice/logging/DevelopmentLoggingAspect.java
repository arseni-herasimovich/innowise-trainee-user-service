package com.innowise.userservice.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@Profile({"dev", "development"})
public class DevelopmentLoggingAspect {
    @Pointcut("execution(public * com.innowise.userservice.service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object log(ProceedingJoinPoint pjp) throws Throwable {
        var className = pjp.getTarget().getClass().getSimpleName();
        var methodName = pjp.getSignature().getName();
        var args = Arrays.toString(pjp.getArgs());

        var start = System.nanoTime();
        log.debug("Entering method: {}.{} with args: {}", className, methodName, args);

        Object result = pjp.proceed();

        var executionTime = (System.nanoTime() - start) / 1000000;
        log.debug("Exiting method: {}.{}. Execution time: {} ms. Result: {}",
                className, methodName, executionTime, result != null ? result.toString() : "void");

        return result;
    }
}