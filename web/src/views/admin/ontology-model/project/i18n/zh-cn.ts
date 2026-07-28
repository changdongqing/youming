export default {
	modelProject: {
		// 字段标签
		index: '#',
		projectCode: '项目编码',
		name: '项目名称',
		description: '项目描述',
		namespaceBase: '命名空间基址',
		defaultFormat: '默认格式',
		serializationStrategy: '序列化策略',
		status: '状态',
		// 状态选项
		statusDraft: '草稿',
		statusActive: '活跃',
		statusArchived: '归档',
		// 格式
		formatTtl: 'Turtle',
		formatOwlXml: 'OWL XML',
		// 操作
		add: '新增项目',
		edit: '编辑',
		delete: '删除',
		prefixManage: '前缀管理',
		// 前缀管理
		prefix: '前缀名',
		namespace: '命名空间',
		isDefault: '默认前缀',
		addPrefix: '新增前缀',
		yes: '是',
		no: '否',
		// inputXxxTip 系列
		inputProjectCodeTip: '请输入项目编码（如 fire-equipment）',
		inputNameTip: '请输入项目名称',
		inputDescriptionTip: '请输入项目描述',
		inputNamespaceBaseTip: '请输入命名空间基址（如 http://youming.com/onto/fire/）',
		selectDefaultFormatTip: '请选择默认序列化格式',
		selectStatusTip: '请选择项目状态',
		inputPrefixTip: '请输入前缀名（如 ex、qudt）',
		inputNamespaceTip: '请输入命名空间 URI',
		// 确认提示
		deleteTip: '确认删除该项目？删除将一并清理项目下前缀',
		deletePrefixTip: '确认删除该前缀？',
		// 前缀子弹窗标题
		prefixDialogTitle: '前缀管理',
	},
};
