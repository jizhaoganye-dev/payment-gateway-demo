package com.fintech.payment.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * トランザクションロギングアスペクト
 * 
 * 【設計思想】
 * - 金融グレードの監査ログ
 * - 横断的関心事の分離（AOP）
 * - パフォーマンス計測
 * 
 * 【リファクタリング履歴】
 * BEFORE: 各メソッドにログ出力コードが散在 → 重複、保守困難
 * AFTER: AOPによる集約 → DRY原則、統一フォーマット
 */
@Aspect
@Component
@Slf4j
public class TransactionLoggingAspect {

    /**
     * サービス層のメソッドを対象
     */
    @Pointcut("execution(* com.fintech.payment.service.*.*(..))")
    public void serviceLayer() {}

    /**
     * コントローラー層のメソッドを対象
     */
    @Pointcut("execution(* com.fintech.payment.controller.*.*(..))")
    public void controllerLayer() {}

    /**
     * サービス層のロギング
     */
    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("[TRACE:{}] SERVICE START: {} | Args: {}", 
                traceId, methodName, sanitizeArgs(args));

        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("[TRACE:{}] SERVICE END: {} | Duration: {}ms | Success", 
                    traceId, methodName, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("[TRACE:{}] SERVICE ERROR: {} | Duration: {}ms | Error: {}", 
                    traceId, methodName, duration, e.getMessage());
            
            throw e;
        }
    }

    /**
     * コントローラー層のロギング
     */
    @Around("controllerLayer()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[REQUEST:{}] API CALL: {}", requestId, methodName);

        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("[REQUEST:{}] API RESPONSE: {} | Duration: {}ms", 
                    requestId, methodName, duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("[REQUEST:{}] API ERROR: {} | Duration: {}ms | Error: {}", 
                    requestId, methodName, duration, e.getMessage());
            
            throw e;
        }
    }

    /**
     * 引数をサニタイズ（機密情報をマスク）
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        // 本番環境では機密情報をマスクする
        return Arrays.toString(args)
                .replaceAll("(cardNumber=)[^,}]+", "$1****")
                .replaceAll("(cvv=)[^,}]+", "$1***")
                .replaceAll("(password=)[^,}]+", "$1****");
    }
}
