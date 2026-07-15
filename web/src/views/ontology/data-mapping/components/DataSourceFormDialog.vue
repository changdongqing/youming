<template>
	<el-dialog v-model="visible" :title="title" width="720px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-steps :active="currentStep" finish-status="success" class="mb20">
			<el-step title="基本信息" />
			<el-step title="连接配置" />
			<el-step title="凭证" />
			<el-step title="Schema白名单" />
			<el-step title="测试并启用" />
		</el-steps>

		<el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading">
			<!-- ==================== 步骤1：基本信息 ==================== -->
			<div v-show="currentStep === 0">
				<el-form-item label="数据源编码" prop="sourceCode">
					<el-input v-model="form.sourceCode" placeholder="大写字母开头，3-64位大写字母/数字/下划线" :disabled="isEdit" />
				</el-form-item>
				<el-form-item label="数据源名称" prop="sourceName">
					<el-input v-model="form.sourceName" placeholder="数据源名称" maxlength="128" show-word-limit />
				</el-form-item>
				<el-form-item label="源类型" prop="sourceType">
					<el-select v-model="form.sourceType" placeholder="选择源类型" style="width: 100%">
						<el-option label="PostgreSQL" value="POSTGRESQL" />
						<el-option label="MySQL" value="MYSQL" />
						<el-option label="Oracle" value="ORACLE" />
						<el-option label="SQL Server" value="SQLSERVER" />
					</el-select>
				</el-form-item>
				<el-form-item label="安全级别">
					<el-select v-model="form.securityLevelCode" placeholder="默认 RESTRICTED" style="width: 100%">
						<el-option label="公开" value="PUBLIC" />
						<el-option label="内部" value="INTERNAL" />
						<el-option label="受限" value="RESTRICTED" />
						<el-option label="机密" value="CONFIDENTIAL" />
					</el-select>
				</el-form-item>
				<el-form-item label="备注">
					<el-input v-model="form.remarks" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
			</div>

			<!-- ==================== 步骤2：连接配置 ==================== -->
			<div v-show="currentStep === 1">
				<el-form-item label="连接模式" prop="connectionMode">
					<el-radio-group v-model="form.connectionMode">
						<el-radio value="HOST">主机/端口</el-radio>
						<el-radio value="JDBC_URL">JDBC URL</el-radio>
					</el-radio-group>
				</el-form-item>
				<!-- HOST 模式 -->
				<template v-if="form.connectionMode === 'HOST'">
					<el-form-item label="主机地址" prop="hostConfig.host">
						<el-input v-model="hostConfig.host" placeholder="如 127.0.0.1" />
					</el-form-item>
					<el-form-item label="端口" prop="hostConfig.port">
						<el-input-number v-model="hostConfig.port" :min="1" :max="65535" controls-position="right" />
					</el-form-item>
					<el-form-item label="数据库名" prop="hostConfig.database">
						<el-input v-model="hostConfig.database" placeholder="如 youmingdb" />
					</el-form-item>
				</template>
				<!-- JDBC_URL 模式 -->
				<el-form-item v-if="form.connectionMode === 'JDBC_URL'" label="JDBC URL" prop="jdbcConfig.url">
					<el-input v-model="jdbcConfig.url" type="textarea" :rows="3" placeholder="如 jdbc:postgresql://127.0.0.1:5432/youmingdb" />
					<el-alert type="warning" :closable="false" show-icon class="mt4">
						<template #title>JDBC URL 中禁止包含 user/password 参数，凭证请在下一步配置</template>
					</el-alert>
				</el-form-item>
			</div>

			<!-- ==================== 步骤3：凭证 ==================== -->
			<div v-show="currentStep === 2">
				<el-alert v-if="isEdit && form.credentialConfigured" type="info" :closable="false" show-icon class="mb12">
					<template #title>当前已配置凭证（用户名脱敏：{{ form.usernameMasked }}）。密码框留空表示保持原凭证不变。</template>
				</el-alert>
				<el-form-item label="用户名" prop="username">
					<el-input
						v-model="form.username"
						:placeholder="isEdit && form.credentialConfigured ? '留空保持原凭证' : '数据库用户名'"
						autocomplete="off"
					/>
				</el-form-item>
				<el-form-item label="密码" prop="password">
					<el-input
						v-model="form.password"
						type="password"
						show-password
						:placeholder="isEdit && form.credentialConfigured ? '留空保持原凭证' : '数据库密码'"
						autocomplete="new-password"
					/>
				</el-form-item>
				<el-alert v-if="isEdit && form.credentialConfigured" type="warning" :closable="false" show-icon class="mt4">
					<template #title>编辑时密码框永远为空，输入新值表示更新凭证。不把后端掩码字符串重新提交为密码。</template>
				</el-alert>
			</div>

			<!-- ==================== 步骤4：Schema白名单 ==================== -->
			<div v-show="currentStep === 3">
				<el-form-item label="允许Schema">
					<el-select
						v-model="form.allowedSchemas"
						multiple
						filterable
						allow-create
						default-first-option
						placeholder="输入或选择Schema"
						style="width: 100%"
					>
						<el-option v-for="s in schemaOptions" :key="s" :label="s" :value="s" />
					</el-select>
				</el-form-item>
				<el-form-item label="允许对象">
					<el-select
						v-model="form.allowedObjects"
						multiple
						filterable
						allow-create
						default-first-option
						placeholder="为空则允许全部对象"
						style="width: 100%"
					>
					</el-select>
				</el-form-item>
				<el-alert type="info" :closable="false" show-icon class="mt4">
					<template #title>白名单至少一个业务Schema</template>
				</el-alert>
			</div>

			<!-- ==================== 步骤5：测试并启用 ==================== -->
			<div v-show="currentStep === 4">
				<el-alert type="info" :closable="false" show-icon class="mb12">
					<template #title>测试成功后可启用数据源，最终状态以后端为准。</template>
				</el-alert>
				<el-descriptions :column="1" border>
					<el-descriptions-item label="数据源编码">{{ form.sourceCode }}</el-descriptions-item>
					<el-descriptions-item label="数据源名称">{{ form.sourceName }}</el-descriptions-item>
					<el-descriptions-item label="连接模式">{{ form.connectionMode }}</el-descriptions-item>
					<el-descriptions-item label="用户名">{{ form.username || (isEdit ? '(保持原凭证)' : '(未填写)' }}</el-descriptions-item>
					<el-descriptions-item label="Schema白名单">{{ form.allowedSchemas?.join(', ') || '-' }}</el-descriptions-item>
				</el-descriptions>
			</div>
		</el-form>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button @click="handlePrev" v-if="currentStep > 0" :disabled="loading">上一步</el-button>
			<el-button type="primary" @click="handleNext" v-if="currentStep < 4" :loading="loading">下一步</el-button>
			<el-button type="success" @click="handleSubmit" v-if="currentStep === 4" :loading="loading">保存</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { useMessage } from '/@/hooks/message';
import { dataSourceApi } from '/@/api/ontology/data-mapping';
import type { DataSourceVO, DataSourceCreateRequest, DataSourceUpdateRequest } from '/@/types/ontology/data-mapping';

const emit = defineEmits<{
	(e: 'success'): void;
}>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const currentStep = ref(0);
const isEdit = ref(false);

const formRef = ref();
const schemaOptions = ref<string[]>([]);

const form = reactive({
	id: undefined as number | undefined,
	sourceCode: '',
	sourceName: '',
	sourceType: 'POSTGRESQL',
	connectionMode: 'HOST' as 'HOST' | 'JDBC_URL',
	/** 连接配置JSON字符串 */
	connectionConfig: '',
	securityLevelCode: 'RESTRICTED',
	remarks: '',
	allowedSchemas: [] as string[],
	allowedObjects: [] as string[],
	username: '',
	password: '',
	credentialConfigured: false,
	usernameMasked: '',
});

const hostConfig = reactive({ host: '', port: 5432, database: '' });
const jdbcConfig = reactive({ url: '' });

const title = computed(() => (isEdit.value ? '编辑数据源' : '新增数据源'));

const rules = {
	sourceCode: [
		{ required: true, message: '请输入数据源编码', trigger: 'blur' },
		{ pattern: /^[A-Z][A-Z0-9_]{2,63}$/, message: '大写字母开头，3-64位大写字母/数字/下划线', trigger: 'blur' },
	],
	sourceName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
	sourceType: [{ required: true, message: '请选择源类型', trigger: 'change' }],
	connectionMode: [{ required: true, message: '请选择连接模式', trigger: 'change' }],
	username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
	password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

/** 打开对话框 */
const openDialog = async (id?: number) => {
	visible.value = true;
	currentStep.value = 0;
	isEdit.value = !!id;

	// 重置表单
	Object.assign(form, {
		id: undefined,
		sourceCode: '',
		sourceName: '',
		sourceType: 'POSTGRESQL',
		connectionMode: 'HOST',
		connectionConfig: '',
		securityLevelCode: 'RESTRICTED',
		remarks: '',
		allowedSchemas: [],
		allowedObjects: [],
		username: '',
		password: '',
		credentialConfigured: false,
		usernameMasked: '',
	});
	Object.assign(hostConfig, { host: '', port: 5432, database: '' });
	Object.assign(jdbcConfig, { url: '' });

	if (id) {
		loading.value = true;
		try {
			const { data } = await dataSourceApi.getById(id);
			form.id = data.id;
			form.sourceCode = data.sourceCode;
			form.sourceName = data.sourceName;
			form.sourceType = data.sourceType || 'POSTGRESQL';
			form.connectionMode = data.connectionMode;
			form.securityLevelCode = data.securityLevelCode || 'RESTRICTED';
			form.remarks = data.remarks || '';
			form.allowedSchemas = data.allowedSchemas || [];
			form.allowedObjects = data.allowedObjects || [];
			form.credentialConfigured = data.credentialConfigured || false;
			form.usernameMasked = data.usernameMasked || '';
			// username/password 留空，编辑时保持原凭证
			form.username = '';
			form.password = '';
			// 解析连接配置
			const config = typeof data.connectionConfig === 'string' ? JSON.parse(data.connectionConfig) : data.connectionConfig;
			if (form.connectionMode === 'HOST' && config) {
				hostConfig.host = config.host || '';
				hostConfig.port = config.port || 5432;
				hostConfig.database = config.database || '';
			} else if (form.connectionMode === 'JDBC_URL' && config) {
				jdbcConfig.url = config.url || '';
			}
		} catch (e: any) {
			msgError(e.message || '获取数据源详情失败');
		} finally {
			loading.value = false;
		}
	}
};

const buildConnectionConfig = (): string => {
	if (form.connectionMode === 'HOST') {
		return JSON.stringify({ host: hostConfig.host, port: hostConfig.port, database: hostConfig.database });
	} else {
		return JSON.stringify({ url: jdbcConfig.url });
	}
};

const handlePrev = () => {
	if (currentStep.value > 0) currentStep.value--;
};

const handleNext = async () => {
	// 步骤间校验
	if (currentStep.value === 0) {
		try {
			await formRef.value?.validateField(['sourceCode', 'sourceName', 'sourceType']);
		} catch {
			return;
		}
	}
	if (currentStep.value === 1) {
		// 连接配置校验
		if (form.connectionMode === 'HOST') {
			if (!hostConfig.host || !hostConfig.database) {
				msgError('请填写主机地址和数据库名');
				return;
			}
		} else {
			if (!jdbcConfig.url) {
				msgError('请填写 JDBC URL');
				return;
			}
			if (jdbcConfig.url.toLowerCase().includes('user=') || jdbcConfig.url.toLowerCase().includes('password=')) {
				msgError('JDBC URL 中禁止包含 user/password 参数');
				return;
			}
		}
		form.connectionConfig = buildConnectionConfig();
	}
	if (currentStep.value === 2) {
		// 凭证校验：新建必须填写；编辑可留空保持原凭证
		if (!isEdit.value) {
			if (!form.username || !form.password) {
				msgError('新建数据源必须填写用户名和密码');
				return;
			}
		}
	}
	if (currentStep.value === 3) {
		// 白名单至少一个Schema
		if (!form.allowedSchemas || form.allowedSchemas.length === 0) {
			msgError('白名单至少一个业务Schema');
			return;
		}
	}
	currentStep.value++;
};

const handleSubmit = async () => {
	loading.value = true;
	try {
		const connectionConfig = buildConnectionConfig();
		if (isEdit.value) {
			const payload: DataSourceUpdateRequest = {
				id: form.id!,
				sourceName: form.sourceName,
				connectionMode: form.connectionMode,
				connectionConfig,
				allowedSchemas: form.allowedSchemas,
				allowedObjects: form.allowedObjects,
				securityLevelCode: form.securityLevelCode,
				remarks: form.remarks,
			};
			// 编辑时只有填写了用户名/密码才更新凭证
			if (form.username) payload.username = form.username;
			if (form.password) payload.password = form.password;
			await dataSourceApi.update(form.id!, payload);
			msgSuccess('数据源更新成功');
		} else {
			const payload: DataSourceCreateRequest = {
				sourceCode: form.sourceCode,
				sourceName: form.sourceName,
				connectionMode: form.connectionMode,
				connectionConfig,
				username: form.username,
				password: form.password,
				allowedSchemas: form.allowedSchemas,
				allowedObjects: form.allowedObjects,
				securityLevelCode: form.securityLevelCode,
				remarks: form.remarks,
			};
			await dataSourceApi.create(payload);
			msgSuccess('数据源创建成功');
		}
		visible.value = false;
		emit('success');
	} catch (e: any) {
		msgError(e.message || '保存失败');
	} finally {
		loading.value = false;
	}
};

defineExpose({ openDialog });
</script>

<style scoped>
.mb20 {
	margin-bottom: 20px;
}
.mb12 {
	margin-bottom: 12px;
}
.mt4 {
	margin-top: 4px;
}
</style>
