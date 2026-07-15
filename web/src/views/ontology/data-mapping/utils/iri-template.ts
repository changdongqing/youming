/**
 * IRI模板工具（18-10 §6）
 * IRI模板编辑器只允许从主键列插入变量，不提供任意文本脚本自动完成。
 * 模板格式：prefix/{column|normalizer}/{column2}/suffix
 */

/** 键列配置项 */
export interface KeyColumnConfig {
	column: string;
	order: number;
	normalizer?: string;
}

/** 从模板中提取的变量 */
export interface IriTemplateVariable {
	/** 列名 */
	column: string;
	/** 归一化器，如 LONG / URL / LOWERCASE */
	normalizer?: string;
	/** 原始匹配文本 */
	raw: string;
}

/**
 * 从 IRI 模板中提取所有变量。
 * 变量格式：{column} 或 {column|normalizer}
 */
export function extractIriVariables(template: string): IriTemplateVariable[] {
	if (!template) return [];
	const regex = /\{([^}]+)\}/g;
	const variables: IriTemplateVariable[] = [];
	let match: RegExpExecArray | null;
	while ((match = regex.exec(template)) !== null) {
		const content = match[1];
		const parts = content.split('|');
		const column = parts[0].trim();
		const normalizer = parts.length > 1 ? parts[1].trim() : undefined;
		if (column) {
			variables.push({ column, normalizer, raw: match[0] });
		}
	}
	return variables;
}

/**
 * 校验模板中的变量是否全部来自 keyColumns。
 * 返回不在 keyColumns 中的变量列表（空数组表示全部合法）。
 */
export function validateIriVariables(template: string, keyColumns: KeyColumnConfig[]): IriTemplateVariable[] {
	const variables = extractIriVariables(template);
	const keyColumnNames = new Set(keyColumns.map((k) => k.column));
	return variables.filter((v) => !keyColumnNames.has(v.column));
}

/**
 * 判断模板是否包含非法变量。
 */
export function hasInvalidVariables(template: string, keyColumns: KeyColumnConfig[]): boolean {
	return validateIriVariables(template, keyColumns).length > 0;
}

/**
 * 将模板变量插入到模板末尾。
 * 如果光标位置可选，此处简化为追加。
 */
export function appendVariable(template: string, column: string, normalizer?: string): string {
	const variable = normalizer ? `{${column}|${normalizer}}` : `{${column}}`;
	return template + variable;
}

/**
 * 渲染样例 IRI 本地名：用示例值替换模板中的变量。
 * @param template IRI模板
 * @param sampleValues 列名→样例值的映射
 */
export function renderLocalName(template: string, sampleValues: Record<string, string>): string {
	if (!template) return '';
	return template.replace(/\{([^}]+)\}/g, (match, content) => {
		const parts = content.split('|');
		const column = parts[0].trim();
		const value = sampleValues[column];
		if (value === undefined || value === null) return match;
		// normalizer 仅做展示，实际归一化由后端执行
		return value;
	});
}

/**
 * 生成完整 IRI：拼接 namespaceUri + 渲染后 localName。
 */
export function renderFullIri(namespaceUri: string, template: string, sampleValues: Record<string, string>): string {
	const localName = renderLocalName(template, sampleValues);
	if (!namespaceUri) return localName;
	// 确保 namespaceUri 以 / 或 # 结尾
	const separator = namespaceUri.endsWith('/') || namespaceUri.endsWith('#') ? '' : '/';
	return namespaceUri + separator + localName;
}

/** 可用的归一化器列表 */
export const NORMALIZERS = [
	{ label: '无（原样）', value: '' },
	{ label: 'LONG（长整型）', value: 'LONG' },
	{ label: 'URL（URL编码）', value: 'URL' },
	{ label: 'LOWERCASE（小写）', value: 'LOWERCASE' },
	{ label: 'UPPERCASE（大写）', value: 'UPPERCASE' },
	{ label: 'TRIM（去空格）', value: 'TRIM' },
];
