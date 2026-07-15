import { defineStore } from 'pinia';
import type { MappingProjectVO, MappingVersionVO, SourceObjectMetadataVO, EntityMappingVO, RelationMappingVO } from '/@/types/ontology/data-mapping';

/**
 * 映射设计器全局状态（18-10 §5）
 * 使用 Pinia store 管理设计器跨组件共享状态，便于 Vue DevTools 调试。
 * 不使用单个超大 reactive 对象深度监听全部配置；
 * 编辑动作通过显式方法更新局部状态并标记 dirty。
 */
export const useMappingDesignerStore = defineStore('mappingDesigner', {
	state: () => ({
		// 当前映射工程
		project: null as MappingProjectVO | null,
		// 当前编辑版本
		version: null as MappingVersionVO | null,
		// 源数据元数据（按需加载，key 为 sourceId）
		sourceMetadata: {} as Record<number, SourceObjectMetadataVO | undefined>,
		// 目标本体 schema 缓存
		ontologySchema: null as any,
		// 实体映射列表
		entityMappings: [] as EntityMappingVO[],
		// 关系映射列表
		relationMappings: [] as RelationMappingVO[],
		// 脏状态标记：哪些子表有未保存修改
		dirtyState: {
			entityMappings: false,
			fieldMappings: {} as Record<number, boolean>,
			relationMappings: false,
		} as { entityMappings: boolean; fieldMappings: Record<number, boolean>; relationMappings: boolean },
		// 加载状态
		loadingState: {
			project: false,
			version: false,
			sourceMetadata: false,
			ontologySchema: false,
			entityMappings: false,
			relationMappings: false,
		} as Record<string, boolean>,
		// 冲突状态（版本冲突/乐观锁冲突）
		conflictState: null as { type: string; message: string; detail?: any } | null,
	}),
	getters: {
		/** 是否有未保存修改 */
		isDirty(): boolean {
			return (
				this.dirtyState.entityMappings || this.dirtyState.relationMappings || Object.values(this.dirtyState.fieldMappings).some((v) => v === true)
			);
		},
		/** 当前版本是否只读（PUBLISHED/VALIDATED/RETIRED） */
		isReadonly(): boolean {
			if (!this.version) return true;
			return ['PUBLISHED', 'VALIDATED', 'RETIRED', 'VALIDATING'].includes(this.version.versionStatus);
		},
		/** 当前版本状态 */
		versionStatus(): string {
			return this.version?.versionStatus || '';
		},
	},
	actions: {
		/** 设置当前工程 */
		setProject(project: MappingProjectVO | null) {
			this.project = project;
		},
		/** 设置当前版本 */
		setVersion(version: MappingVersionVO | null) {
			this.version = version;
		},
		/** 设置实体映射列表 */
		setEntityMappings(mappings: EntityMappingVO[]) {
			this.entityMappings = mappings;
		},
		/** 设置关系映射列表 */
		setRelationMappings(mappings: RelationMappingVO[]) {
			this.relationMappings = mappings;
		},
		/** 缓存源数据元数据 */
		setSourceMetadata(sourceId: number, metadata: SourceObjectMetadataVO) {
			this.sourceMetadata[sourceId] = metadata;
		},
		/** 标记实体映射为脏 */
		markEntityMappingsDirty(dirty: boolean) {
			this.dirtyState.entityMappings = dirty;
		},
		/** 标记某实体映射的字段映射为脏 */
		markFieldMappingsDirty(entityMappingId: number, dirty: boolean) {
			this.dirtyState.fieldMappings[entityMappingId] = dirty;
		},
		/** 标记关系映射为脏 */
		markRelationMappingsDirty(dirty: boolean) {
			this.dirtyState.relationMappings = dirty;
		},
		/** 设置加载状态 */
		setLoading(key: string, loading: boolean) {
			this.loadingState[key] = loading;
		},
		/** 设置冲突状态 */
		setConflict(conflict: { type: string; message: string; detail?: any } | null) {
			this.conflictState = conflict;
		},
		/** 清除所有脏标记（保存成功后调用） */
		clearDirty() {
			this.dirtyState.entityMappings = false;
			this.dirtyState.fieldMappings = {};
			this.dirtyState.relationMappings = false;
		},
		/** 重置整个 store（离开设计器时调用） */
		reset() {
			this.project = null;
			this.version = null;
			this.sourceMetadata = {};
			this.ontologySchema = null;
			this.entityMappings = [];
			this.relationMappings = [];
			this.dirtyState = { entityMappings: false, fieldMappings: {}, relationMappings: false };
			this.loadingState = {
				project: false,
				version: false,
				sourceMetadata: false,
				ontologySchema: false,
				entityMappings: false,
				relationMappings: false,
			};
			this.conflictState = null;
		},
	},
});
