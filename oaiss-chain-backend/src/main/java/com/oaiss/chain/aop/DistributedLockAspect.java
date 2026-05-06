package com.oaiss.chain.aop;

import com.oaiss.chain.annotation.DistributedLock;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 分布式锁切面
 * Distributed Lock Aspect
 * 
 * <p>拦截标注了@DistributedLock注解的方法，自动获取和释放分布式锁</p>
 * <p>Intercepts methods annotated with @DistributedLock to automatically acquire and release distributed lock</p>
 * 
 * @author OAISS Team
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedisLockService redisLockService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 环绕通知：分布式锁处理
     */
    @Around("@annotation(com.oaiss.chain.annotation.DistributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        // 解析锁键
        String lockKey = parseLockKey(annotation.key(), method, joinPoint.getArgs());

        // 尝试获取锁
        String lockValue;
        if (annotation.waitTime() > 0) {
            lockValue = redisLockService.tryLockWithRetry(
                    lockKey,
                    annotation.waitTime(),
                    annotation.expireTime(),
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
        } else {
            lockValue = redisLockService.tryLock(lockKey, annotation.expireTime(), java.util.concurrent.TimeUnit.SECONDS);
        }

        // 获取锁失败
        if (lockValue == null) {
            log.warn("Distributed lock acquisition failed: key={}, method={}", lockKey, method.getName());
            throw new BusinessException(ErrorCode.OPERATION_IN_PROGRESS, annotation.errorMessage());
        }

        try {
            // 执行原方法
            return joinPoint.proceed();
        } finally {
            // 释放锁
            boolean released = redisLockService.releaseLock(lockKey, lockValue);
            if (!released) {
                log.warn("Distributed lock release failed: key={}", lockKey);
            }
        }
    }

    /**
     * 解析 SpEL 表达式锁键
     * Parse SpEL expression lock key
     */
    private String parseLockKey(String expression, Method method, Object[] args) {
        // 如果不是 SpEL 表达式，直接返回
        if (!expression.contains("#") && !expression.contains("'")) {
            return expression;
        }

        EvaluationContext context = new StandardEvaluationContext();

        // 设置方法参数
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }

        Expression exp = parser.parseExpression(expression);
        Object value = exp.getValue(context);
        return value != null ? value.toString() : expression;
    }
}