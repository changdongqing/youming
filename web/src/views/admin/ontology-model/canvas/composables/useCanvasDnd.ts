import emitter from '/@/utils/mitt';
import { addObj as addClass } from '/@/api/ontology-model/class';
import { addEdge as addSubclassEdge } from '/@/api/ontology-model/subclassof';
import { addObj as addObjectProp } from '/@/api/ontology-model/object-property';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

/**
 * 拖拽创建逻辑（FR-17.3 拖拽实例化 + FR-17.5 边创建建关系）
 */
export function useCanvasDnd(projectId: Ref<string>, namespaceBase: Ref<string>) {
	const { t } = useI18n();

	// 拖拽分类模板 -> 创建类（AC-17.3）
	const handleDrop = async (event: DragEvent) => {
		event.preventDefault();
		const templateCode = event.dataTransfer?.getData('templateCode');
		if (!templateCode || !projectId.value) return;

		const localName = window.prompt(t('modelCanvas.inputClassName'));
		if (!localName) return;

		try {
			const res = await addClass({
				projectId: projectId.value,
				localName,
				classIri: namespaceBase.value + localName,
				templateCode,
				label: localName,
			});
			if (res.code === 0) {
				useMessage().success(t('modelCanvas.createSuccess'));
				emitter.emit('canvasNodeAdded', res.data);
			}
		} catch (err: any) {
			useMessage().error(err.msg);
		}
	};

	// 边连接完成 -> 建关系（AC-17.5）
	const handleEdgeConnected = async ({ source, target, edge }: any) => {
		// 弹窗选择关系类型：subClassOf / 对象属性
		const relationType = window.confirm(t('modelCanvas.selectRelationType') + '\nOK=subClassOf, Cancel=对象属性')
			? 'subclass'
			: 'association';
		if (!relationType) {
			edge.remove();
			return;
		}

		try {
			if (relationType === 'subclass') {
				await addSubclassEdge({
					projectId: projectId.value,
					childClassId: source.classId,
					parentClassId: target.classId,
				});
				useMessage().success(t('modelCanvas.createSuccess'));
			} else {
				const propName = window.prompt(t('modelCanvas.inputPropertyName'));
				if (!propName) {
					edge.remove();
					return;
				}
				await addObjectProp({
					projectId: projectId.value,
					domainClassId: source.classId,
					rangeClassId: target.classId,
					localName: propName,
					propertyIri: source.label + '_' + propName,
					label: propName,
				});
				useMessage().success(t('modelCanvas.createSuccess'));
			}
			emitter.emit('canvasEdgeAdded');
		} catch (err: any) {
			edge.remove(); // 失败则删除边
			useMessage().error(err.msg);
		}
	};

	return { handleDrop, handleEdgeConnected };
}
