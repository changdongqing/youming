/**
 * mitt 事件类型定义
 *
 * @method openSettingsDrawer 打开布局设置弹窗
 * @method restoreDefault 分栏布局，鼠标移入、移出数据显示
 * @method setSendColumnsChildren 分栏布局，鼠标移入、移出菜单数据传入到 navMenu 下的菜单中
 * @method setSendClassicChildren 经典布局，开启切割菜单时，菜单数据传入到 navMenu 下的菜单中
 * @method getBreadcrumbIndexSetFilterRoutes 布局设置弹窗，开启切割菜单时，菜单数据传入到 navMenu 下的菜单中
 * @method layoutMobileResize 浏览器窗口改变时，用于适配移动端界面显示
 * @method openOrCloseSortable 布局设置弹窗，开启 TagsView 拖拽
 * @method openShareTagsView 布局设置弹窗，开启 TagsView 共用
 * @method onTagsViewRefreshRouterView tagsview 刷新界面
 * @method onCurrentContextmenuClick tagsview 右键菜单每项点击时
 */
declare type MittType<T = any> = {
	openSettingsDrawer?: string;
	restoreDefault?: string;
	setSendColumnsChildren: T;
	setSendClassicChildren: T;
	getBreadcrumbIndexSetFilterRoutes?: string;
	layoutMobileResize: T;
	openOrCloseSortable?: string;
	openShareTagsView?: string;
	onTagsViewRefreshRouterView?: T;
	onCurrentContextmenuClick?: T;
	// 画布事件键（DD11 可视化建模画布）
	canvasNodeAdded?: T; // 新增节点后刷新画布 + 序列化预览
	canvasEdgeAdded?: T; // 新增边后刷新序列化预览
	openPropertyDrawer?: T; // 双击节点打开属性抽屉
	edgeConnected?: T; // 边连接完成触发关系创建
	refreshSerializePreview?: T; // 画布操作后触发防抖预览
};

// mitt 参数类型定义
declare type LayoutMobileResize = {
	layout: string;
	clientWidth: number;
};

// mitt 参数菜单类型
declare type MittMenu = {
	children: RouteRecordRaw[];
	item?: RouteItem;
};
