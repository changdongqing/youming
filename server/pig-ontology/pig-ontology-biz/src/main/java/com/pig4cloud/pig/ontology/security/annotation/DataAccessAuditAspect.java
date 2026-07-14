/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.annotation;

import com.pig4cloud.pig.ontology.security.audit.DataAccessAuditService;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * {@link DataAccessAudit} 注解的 AOP 切面。
 * <p>
 * 拦截标注了 @DataAccessAudit 的方法，记录成功和失败两种情况。
 * 不按方法名拦截，避免遗漏或误拦截。
 *
 * @author youming
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataAccessAuditAspect {

	private final DataAccessAuditService auditService;

	private final SecuritySubjectResolver subjectResolver;

	@Around("@annotation(dataAccessAudit)")
	public Object audit(ProceedingJoinPoint joinPoint, DataAccessAudit dataAccessAudit) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String methodName = signature.getMethod().getName();
		SecuritySubject subject = subjectResolver.resolve();

		Object result = null;
		String outcome = "SUCCESS";
		String errorCode = null;

		try {
			result = joinPoint.proceed();
		}
		catch (Exception e) {
			outcome = "FAILED";
			errorCode = e.getClass().getSimpleName();
			throw e;
		}
		finally {
			try {
				auditService.recordAudit(null, subject.getUserId(), subject.getUsername(),
					dataAccessAudit.accessType(), dataAccessAudit.resourceType(),
					methodName, null, 0L, null, null, outcome, errorCode, null);
			}
			catch (Exception e) {
				log.warn("Audit recording failed for method {}: {}", methodName, e.getMessage());
			}
		}

		return result;
	}

}
