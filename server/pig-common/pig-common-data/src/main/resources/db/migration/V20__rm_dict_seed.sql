-- ============================================================
-- V20__rm_dict_seed.sql
-- 研发管理字典数据 + 三级按钮权限码
-- 对应《研发管理工具详细设计计划_v1.0》§3.4 / §1.4
-- ============================================================

-- ---------- (a) sys_dict 种子（10 字段按位置 INSERT） ----------
-- 字段顺序：id, dict_type, description, create_by, update_by, create_time, update_time, remarks, system_flag, del_flag
-- ID 段规划：1901~1912（字典定义），190101~（字典项，前3位=字典ID后3位）

INSERT INTO sys_dict VALUES (1901, 'rm_req_source',      '需求来源',       'admin', 'admin', now(), now(), '迭代/客户(交付)/客户(运维)/售前', '1', '0');
INSERT INTO sys_dict VALUES (1902, 'rm_req_status',      '需求状态',       'admin', 'admin', now(), now(), '草稿~已发布', '1', '0');
INSERT INTO sys_dict VALUES (1903, 'rm_dev_task_status', '开发任务状态',   'admin', 'admin', now(), now(), '待开发~已完成', '1', '0');
INSERT INTO sys_dict VALUES (1904, 'rm_test_task_status','测试任务状态',   'admin', 'admin', now(), now(), '待测试~驳回', '1', '0');
INSERT INTO sys_dict VALUES (1905, 'rm_bug_status',      'Bug状态',        'admin', 'admin', now(), now(), '新建~已关闭', '1', '0');
INSERT INTO sys_dict VALUES (1906, 'rm_bug_source',      'Bug来源',        'admin', 'admin', now(), now(), '测试/项目/运维', '1', '0');
INSERT INTO sys_dict VALUES (1907, 'rm_bug_severity',    'Bug严重等级',    'admin', 'admin', now(), now(), '致命~轻微', '1', '0');
INSERT INTO sys_dict VALUES (1908, 'rm_priority',        '优先级',         'admin', 'admin', now(), now(), '高/中/低', '1', '0');
INSERT INTO sys_dict VALUES (1909, 'rm_schedule_risk',   '时间风险等级',   'admin', 'admin', now(), now(), '正常/预警/超期', '1', '0');
INSERT INTO sys_dict VALUES (1910, 'rm_todo_type',      '待办类型',       'admin', 'admin', now(), now(), '待审批~待验收', '1', '0');
INSERT INTO sys_dict VALUES (1911, 'rm_notify_type',    '通知类型',       'admin', 'admin', now(), now(), '审批/驳回/提测/发版/超期/当日bug', '1', '0');
INSERT INTO sys_dict VALUES (1912, 'rm_case_type',      '用例类型',       'admin', 'admin', now(), now(), '功能/性能/接口', '1', '0');

-- ---------- (b) sys_dict_item 种子（14 字段按位置 INSERT） ----------
-- 字段顺序：id, dict_id, item_value, label, dict_type, description, list_class, sort_order, create_by, update_by, create_time, update_time, remarks, del_flag

-- 需求来源 (dict_id=1901)
INSERT INTO sys_dict_item VALUES (190101, 1901, 'ITERATION',         '迭代需求',       'rm_req_source', '迭代需求',     NULL, 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190102, 1901, 'CUSTOMER_DELIVERY', '客户需求(交付)', 'rm_req_source', '交付中心提报', NULL, 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190103, 1901, 'CUSTOMER_OPS',      '客户需求(运维)', 'rm_req_source', '运维中心提报', NULL, 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190104, 1901, 'PRESALE',           '售前项目需求',   'rm_req_source', '售前提报',     NULL, 4, 'admin', 'admin', now(), now(), '', '0');

-- 需求状态 (dict_id=1902)
INSERT INTO sys_dict_item VALUES (190201, 1902, 'DRAFT',               '草稿',       'rm_req_status', NULL, 'info',     1,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190202, 1902, 'PENDING_APPROVAL',    '待审批',     'rm_req_status', NULL, 'warning',  2,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190203, 1902, 'REJECTED',            '已驳回',     'rm_req_status', NULL, 'danger',   3,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190204, 1902, 'PENDING_REVIEW',      '待评审',     'rm_req_status', NULL, 'warning',  4,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190205, 1902, 'DESIGNING',           '设计中',     'rm_req_status', NULL, 'primary',  5,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190206, 1902, 'DESIGN_REVIEW',       '设计评审中', 'rm_req_status', NULL, 'warning',  6,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190207, 1902, 'SCHEDULING',         '排期中',     'rm_req_status', NULL, 'primary',  7,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190208, 1902, 'DEVELOPING',         '开发中',     'rm_req_status', NULL, 'primary',  8,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190209, 1902, 'TESTING',            '测试中',     'rm_req_status', NULL, 'primary',  9,  'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190210, 1902, 'PENDING_ACCEPTANCE',  '待验收',     'rm_req_status', NULL, 'success',  10, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190211, 1902, 'COMPLETED',           '已完成',     'rm_req_status', NULL, 'success',  11, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190212, 1902, 'RELEASED',            '已发布',     'rm_req_status', NULL, 'info',     12, 'admin', 'admin', now(), now(), '', '0');

-- 开发任务状态 (dict_id=1903)
INSERT INTO sys_dict_item VALUES (190301, 1903, 'PENDING_DEV',  '待开发', 'rm_dev_task_status', NULL, 'info',    1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190302, 1903, 'IN_DEV',       '开发中', 'rm_dev_task_status', NULL, 'primary', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190303, 1903, 'PENDING_TEST', '待测试', 'rm_dev_task_status', NULL, 'warning', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190304, 1903, 'COMPLETED',    '已完成', 'rm_dev_task_status', NULL, 'success', 4, 'admin', 'admin', now(), now(), '', '0');

-- 测试任务状态 (dict_id=1904)
INSERT INTO sys_dict_item VALUES (190401, 1904, 'PENDING_TEST', '待测试', 'rm_test_task_status', NULL, 'info',    1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190402, 1904, 'IN_TEST',      '测试中', 'rm_test_task_status', NULL, 'primary', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190403, 1904, 'PASS',          '通过',   'rm_test_task_status', NULL, 'success', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190404, 1904, 'FAIL',          '驳回',   'rm_test_task_status', NULL, 'danger',  4, 'admin', 'admin', now(), now(), '', '0');

-- Bug状态 (dict_id=1905)
INSERT INTO sys_dict_item VALUES (190501, 1905, 'NEW',          '新建',    'rm_bug_status', NULL, 'info',    1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190502, 1905, 'IN_PROGRESS',  '处理中',  'rm_bug_status', NULL, 'warning', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190503, 1905, 'RESOLVED',     '已解决',  'rm_bug_status', NULL, 'primary', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190504, 1905, 'VERIFIED',    '已验证',  'rm_bug_status', NULL, 'success', 4, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190505, 1905, 'CLOSED',      '已关闭',  'rm_bug_status', NULL, 'info',    5, 'admin', 'admin', now(), now(), '', '0');

-- Bug来源 (dict_id=1906)
INSERT INTO sys_dict_item VALUES (190601, 1906, 'TEST',    '测试发现', 'rm_bug_source', NULL, 'primary', 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190602, 1906, 'PROJECT', '项目提报', 'rm_bug_source', NULL, 'warning', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190603, 1906, 'OPS',     '运维提报', 'rm_bug_source', NULL, 'danger',  3, 'admin', 'admin', now(), now(), '', '0');

-- Bug严重等级 (dict_id=1907)
INSERT INTO sys_dict_item VALUES (190701, 1907, 'CRITICAL', '致命', 'rm_bug_severity', NULL, 'danger',  1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190702, 1907, 'MAJOR',    '严重', 'rm_bug_severity', NULL, 'warning', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190703, 1907, 'MINOR',    '一般', 'rm_bug_severity', NULL, 'primary', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190704, 1907, 'TRIVIAL',  '轻微', 'rm_bug_severity', NULL, 'info',    4, 'admin', 'admin', now(), now(), '', '0');

-- 优先级 (dict_id=1908)
INSERT INTO sys_dict_item VALUES (190801, 1908, 'HIGH',   '高', 'rm_priority', NULL, 'danger',  1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190802, 1908, 'MEDIUM', '中', 'rm_priority', NULL, 'primary', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190803, 1908, 'LOW',    '低', 'rm_priority', NULL, 'info',    3, 'admin', 'admin', now(), now(), '', '0');

-- 时间风险等级 (dict_id=1909)
INSERT INTO sys_dict_item VALUES (190901, 1909, 'NORMAL',  '正常', 'rm_schedule_risk', NULL, 'success', 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190902, 1909, 'WARN',    '预警', 'rm_schedule_risk', NULL, 'warning', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (190903, 1909, 'OVERDUE', '超期', 'rm_schedule_risk', NULL, 'danger',  3, 'admin', 'admin', now(), now(), '', '0');

-- 待办类型 (dict_id=1910)
INSERT INTO sys_dict_item VALUES (191001, 1910, 'APPROVE', '待审批', 'rm_todo_type', NULL, 'warning', 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191002, 1910, 'DESIGN',  '待设计', 'rm_todo_type', NULL, 'primary', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191003, 1910, 'REVIEW',  '待评审', 'rm_todo_type', NULL, 'warning', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191004, 1910, 'DEV',     '待开发', 'rm_todo_type', NULL, 'primary', 4, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191005, 1910, 'TEST',    '待测试', 'rm_todo_type', NULL, 'primary', 5, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191006, 1910, 'ACCEPT',  '待验收', 'rm_todo_type', NULL, 'success', 6, 'admin', 'admin', now(), now(), '', '0');

-- 通知类型 (dict_id=1911)
INSERT INTO sys_dict_item VALUES (191101, 1911, 'APPROVAL',  '审批通知', 'rm_notify_type', NULL, 'primary', 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191102, 1911, 'REJECT',    '驳回通知', 'rm_notify_type', NULL, 'danger',  2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191103, 1911, 'TEST',      '提测通知', 'rm_notify_type', NULL, 'primary', 3, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191104, 1911, 'RELEASE',   '发版通知', 'rm_notify_type', NULL, 'success', 4, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191105, 1911, 'OVERDUE',   '超期预警', 'rm_notify_type', NULL, 'danger',  5, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191106, 1911, 'BUG_DAILY', '当日bug',  'rm_notify_type', NULL, 'warning', 6, 'admin', 'admin', now(), now(), '', '0');

-- 用例类型 (dict_id=1912)
INSERT INTO sys_dict_item VALUES (191201, 1912, 'FUNCTIONAL',  '功能测试', 'rm_case_type', NULL, 'primary', 1, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191202, 1912, 'PERFORMANCE', '性能测试', 'rm_case_type', NULL, 'warning', 2, 'admin', 'admin', now(), now(), '', '0');
INSERT INTO sys_dict_item VALUES (191203, 1912, 'API',         '接口测试', 'rm_case_type', NULL, 'info',    3, 'admin', 'admin', now(), now(), '', '0');

-- ---------- (c) sys_menu 三级按钮权限（17 字段按位置 INSERT） ----------
-- 字段顺序：menu_id, name, permission, path, component, parent_id, icon, visible, sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
-- menu_type='1' 为按钮权限，parent_id 为二级菜单 ID

-- 需求管理按钮权限（parent_id=19100）
INSERT INTO sys_menu VALUES (19101, '需求查看', 'rm_req_view',     NULL, NULL, 19100, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19102, '需求新增', 'rm_req_add',      NULL, NULL, 19100, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19103, '需求编辑', 'rm_req_edit',     NULL, NULL, 19100, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19104, '需求删除', 'rm_req_del',      NULL, NULL, 19100, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19105, '需求审批', 'rm_req_approve',  NULL, NULL, 19100, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19106, '需求设计', 'rm_req_design',   NULL, NULL, 19100, NULL, '1', 6, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19107, '需求排期', 'rm_req_schedule', NULL, NULL, 19100, NULL, '1', 7, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 开发管理按钮权限（parent_id=19200）
INSERT INTO sys_menu VALUES (19201, '任务查看', 'rm_dev_view',   NULL, NULL, 19200, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19202, '任务新增', 'rm_dev_add',    NULL, NULL, 19200, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19203, '任务编辑', 'rm_dev_edit',   NULL, NULL, 19200, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19204, '详细设计', 'rm_dev_design', NULL, NULL, 19200, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19205, '设计评审', 'rm_dev_review', NULL, NULL, 19200, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 测试管理按钮权限（parent_id=19300）
INSERT INTO sys_menu VALUES (19301, '用例查看', 'rm_case_view',   NULL, NULL, 19300, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19302, '用例新增', 'rm_case_add',    NULL, NULL, 19300, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19303, '用例编辑', 'rm_case_edit',   NULL, NULL, 19300, NULL, '1', 3, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19304, '用例删除', 'rm_case_del',    NULL, NULL, 19300, NULL, '1', 4, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19305, '测试执行', 'rm_tst_execute', NULL, NULL, 19300, NULL, '1', 5, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19306, '转Bug',    'rm_tst_bug',     NULL, NULL, 19300, NULL, '1', 6, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19307, 'Bug查看',  'rm_bug_view',    NULL, NULL, 19300, NULL, '1', 7, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (19308, 'Bug处理',  'rm_bug_handle',  NULL, NULL, 19300, NULL, '1', 8, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 报表统计按钮权限（parent_id=19400）
INSERT INTO sys_menu VALUES (19401, '报表查看', 'rm_report', NULL, NULL, 19400, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');

-- 待办中心按钮权限（parent_id=19500）
INSERT INTO sys_menu VALUES (19501, '待办查看', 'rm_todo', NULL, NULL, 19500, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
