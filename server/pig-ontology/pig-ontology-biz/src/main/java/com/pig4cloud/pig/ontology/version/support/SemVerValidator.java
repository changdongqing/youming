/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.support;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.springframework.stereotype.Component;

/**
 * 语义版本号校验器，封装 java-semver 库。
 * <p>
 * 提供版本号格式校验和严格递增比较，不允许字符串比较。
 *
 * @author youming
 */
@Component
public class SemVerValidator {

	/**
	 * 校验版本号格式是否合法（MAJOR.MINOR.PATCH，不含预发布后缀）。
	 * @param version 版本号
	 * @return true 合法，false 非法
	 */
	public boolean isValid(String version) {
		if (version == null || version.isBlank()) {
			return false;
		}
		try {
			Version semver = Version.valueOf(version);
			// 首期只允许稳定版本，不允许预发布后缀
			return semver.getPreReleaseVersion() == null || semver.getPreReleaseVersion().isEmpty();
		}
		catch (ParseException e) {
			return false;
		}
	}

	/**
	 * 比较两个版本号，判断 newVersion 是否严格大于 oldVersion。
	 * @param newVersion 新版本号
	 * @param oldVersion 旧版本号
	 * @return true 如果 newVersion > oldVersion
	 * @throws IllegalArgumentException 如果任一版本号格式非法
	 */
	public boolean isStrictlyGreater(String newVersion, String oldVersion) {
		if (!isValid(newVersion) || !isValid(oldVersion)) {
			throw new IllegalArgumentException("版本号格式非法: new=" + newVersion + ", old=" + oldVersion);
		}
		return Version.valueOf(newVersion).compareTo(Version.valueOf(oldVersion)) > 0;
	}

	/**
	 * 比较两个版本号的大小。
	 * @param v1 版本号1
	 * @param v2 版本号2
	 * @return 负数 v1&lt;v2，0 v1=v2，正数 v1&gt;v2
	 * @throws IllegalArgumentException 如果任一版本号格式非法
	 */
	public int compare(String v1, String v2) {
		if (!isValid(v1) || !isValid(v2)) {
			throw new IllegalArgumentException("版本号格式非法: v1=" + v1 + ", v2=" + v2);
		}
		return Version.valueOf(v1).compareTo(Version.valueOf(v2));
	}

}
