/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version;

import com.pig4cloud.pig.ontology.version.support.SemVerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SemVerValidator} 单元测试。
 *
 * @author youming
 */
@DisplayName("SemVer版本号校验器测试")
class SemVerValidatorTest {

	private SemVerValidator validator;

	@BeforeEach
	void setUp() {
		validator = new SemVerValidator();
	}

	@Test
	@DisplayName("合法版本号通过校验")
	void testValidVersions() {
		assertTrue(validator.isValid("1.0.0"));
		assertTrue(validator.isValid("0.0.1"));
		assertTrue(validator.isValid("10.20.30"));
		assertTrue(validator.isValid("1.2.3"));
	}

	@Test
	@DisplayName("非法版本号被拒绝")
	void testInvalidVersions() {
		assertFalse(validator.isValid("1.0"));
		assertFalse(validator.isValid("1.0.0.0"));
		assertFalse(validator.isValid("v1.0.0"));
		assertFalse(validator.isValid("1.0.0-SNAPSHOT"));
		assertFalse(validator.isValid("1.0.0-alpha"));
		assertFalse(validator.isValid(""));
		assertFalse(validator.isValid(null));
		assertFalse(validator.isValid("abc"));
	}

	@Test
	@DisplayName("严格递增比较")
	void testStrictlyGreater() {
		assertTrue(validator.isStrictlyGreater("1.1.0", "1.0.0"));
		assertTrue(validator.isStrictlyGreater("2.0.0", "1.9.9"));
		assertTrue(validator.isStrictlyGreater("1.0.1", "1.0.0"));
		assertFalse(validator.isStrictlyGreater("1.0.0", "1.0.0"));
		assertFalse(validator.isStrictlyGreater("0.9.9", "1.0.0"));
		assertFalse(validator.isStrictlyGreater("1.0.0", "1.0.1"));
	}

	@Test
	@DisplayName("比较返回正确符号")
	void testCompare() {
		assertTrue(validator.compare("1.1.0", "1.0.0") > 0);
		assertTrue(validator.compare("1.0.0", "1.1.0") < 0);
		assertEquals(0, validator.compare("1.0.0", "1.0.0"));
	}

	@Test
	@DisplayName("非法版本号比较抛出异常")
	void testCompareInvalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> validator.compare("1.0", "1.0.0"));
		assertThrows(IllegalArgumentException.class, () -> validator.isStrictlyGreater("1.0.0", "invalid"));
	}

}
