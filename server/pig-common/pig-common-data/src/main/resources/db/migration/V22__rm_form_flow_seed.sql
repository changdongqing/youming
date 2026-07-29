-- ============================================================
-- V22__rm_form_flow_seed.sql
-- 表单字段配置 + 流程节点配置默认值
-- 对应《研发管理工具详细设计计划_v1.0》§3.3.16 / §3.3.17 / §6.2
-- ============================================================

-- ---------- (a) 表单字段配置（rm_form_field_config） ----------
-- 需求申请单表单（form_code=REQUIREMENT，13 个字段）
INSERT INTO rm_form_field_config (id, form_code, field_key, field_label, field_type, required, visible, sortable, dict_code, sort_order) VALUES
    (2201, 'REQUIREMENT', 'req_code',            '申请单编号',   'text',      '0', '1', '1', NULL,             1),
    (2202, 'REQUIREMENT', 'title',                '需求标题',     'text',      '1', '1', '1', NULL,             2),
    (2203, 'REQUIREMENT', 'source',               '需求来源',     'select',    '1', '1', '1', 'rm_req_source',   3),
    (2204, 'REQUIREMENT', 'customer_project',     '关联客户/项目', 'text',     '0', '1', '0', NULL,             4),
    (2205, 'REQUIREMENT', 'description',          '需求描述',     'richtext',  '1', '1', '0', NULL,             5),
    (2206, 'REQUIREMENT', 'expect_complete_date', '期望完成时间', 'date',      '1', '1', '1', NULL,             6),
    (2207, 'REQUIREMENT', 'priority',             '优先级',       'select',    '1', '1', '0', 'rm_priority',     7),
    (2208, 'REQUIREMENT', 'need_review',          '需讨论会评审', 'select',    '1', '1', '0', NULL,             8),
    (2209, 'REQUIREMENT', 'design_content',       '需求设计',     'richtext',  '0', '1', '0', NULL,             9),
    (2210, 'REQUIREMENT', 'design_workload',      '设计工作量',   'number',    '0', '1', '0', NULL,             10),
    (2211, 'REQUIREMENT', 'design_plan_date',     '计划完成时间', 'date',      '0', '1', '0', NULL,             11),
    (2212, 'REQUIREMENT', 'design_actual_date',   '实际完成时间', 'date',      '0', '1', '0', NULL,             12),
    (2213, 'REQUIREMENT', 'status',               '当前状态',     'select',    '0', '1', '1', 'rm_req_status',  13);

-- 开发任务单表单（form_code=DEV_TASK，9 个字段）
INSERT INTO rm_form_field_config (id, form_code, field_key, field_label, field_type, required, visible, sortable, dict_code, sort_order) VALUES
    (2230, 'DEV_TASK', 'task_code',         '任务编号',  'text',     '0', '1', '1', NULL,                  1),
    (2231, 'DEV_TASK', 'task_name',         '任务名称',  'text',     '1', '1', '1', NULL,                  2),
    (2232, 'DEV_TASK', 'task_desc',         '任务描述',  'richtext', '0', '1', '0', NULL,                  3),
    (2233, 'DEV_TASK', 'assignee_id',       '负责人',    'select',   '1', '1', '0', NULL,                  4),
    (2234, 'DEV_TASK', 'plan_start_date',   '计划开始',  'date',     '0', '1', '0', NULL,                  5),
    (2235, 'DEV_TASK', 'plan_end_date',     '计划完成',  'date',     '0', '1', '1', NULL,                  6),
    (2236, 'DEV_TASK', 'detail_design',     '详细设计',  'richtext', '0', '1', '0', NULL,                  7),
    (2237, 'DEV_TASK', 'review_conclusion', '评审结论',  'select',   '0', '1', '0', NULL,                  8),
    (2238, 'DEV_TASK', 'status',            '任务状态',  'select',   '0', '1', '1', 'rm_dev_task_status',  9);

-- ---------- (b) 流程节点配置（rm_flow_node） ----------
-- 需求审批流程（flow_code=REQUIREMENT_FLOW，6 个节点）
-- 对应 PRD 3.2 审批链路 + 设计计划 §6.2
INSERT INTO rm_flow_node (id, flow_code, node_code, node_name, approver_type, approver_ref, next_node_pass, next_node_reject, sort_order) VALUES
    -- 交付中心需求先审批节点（仅 CUSTOMER_DELIVERY 来源走此节点）
    (2301, 'REQUIREMENT_FLOW', 'DELIVERY_APPROVAL', '交付中心审批',   'DEPT_LEADER', '',         'PRODUCT_APPROVAL', 'REJECTED', 1),
    -- 产品经理/总工合理性审批
    (2302, 'REQUIREMENT_FLOW', 'PRODUCT_APPROVAL',  '需求合理性审批', 'ROLE',        'rm_product_approver', 'REVIEW_GATE', 'REJECTED', 2),
    -- 评审网关：判断是否需讨论会（need_review=1 时走此节点）
    (2303, 'REQUIREMENT_FLOW', 'REVIEW_GATE',       '讨论会评审',     'ROLE',        'rm_leader',            'DESIGN',      'REJECTED', 3),
    -- 需求分析设计
    (2304, 'REQUIREMENT_FLOW', 'DESIGN',             '需求分析设计',   'ROLE',        'rm_product_approver', 'DESIGN_REVIEW', '',       4),
    -- 需求设计评审
    (2305, 'REQUIREMENT_FLOW', 'DESIGN_REVIEW',       '需求设计评审',   'ROLE',        'rm_leader',            'SCHEDULE',     'DESIGN',   5),
    -- 开发排期
    (2306, 'REQUIREMENT_FLOW', 'SCHEDULE',           '开发排期',       'ROLE',        'rm_dev_manager',       'DEVELOPING',   '',         6);
