-- ============================================================
-- V19__rm_schema.sql
-- 研发管理业务表结构（11 张表）
-- 对应《研发管理工具详细设计计划_v1.0》§3 数据库设计
-- ============================================================

-- 1. 软件需求申请单
CREATE TABLE rm_requirement (
    id                      bigint       NOT NULL,
    req_code                varchar(32)  NOT NULL,
    title                   varchar(255) NOT NULL,
    source                  varchar(32)  NOT NULL,
    customer_project        varchar(255),
    description             text         NOT NULL,
    expect_complete_date    date         NOT NULL,
    initiator_id            bigint       NOT NULL,
    initiator_dept_id       bigint       NOT NULL,
    need_review             char(1)      NOT NULL DEFAULT '0',
    review_conclusion       varchar(32),
    review_remark           text,
    design_content          text,
    design_workload         numeric(8,1),
    design_plan_date        date,
    design_actual_date      date,
    schedule_remark         text,
    schedule_risk           varchar(32),
    status                  varchar(32)  NOT NULL DEFAULT 'DRAFT',
    accept_conclusion       varchar(32),
    accept_remark           text,
    complete_time           timestamp,
    priority                varchar(16)  DEFAULT 'MEDIUM',
    create_by               varchar(64)  DEFAULT ' ',
    create_time             timestamp    DEFAULT now(),
    update_by               varchar(64)  DEFAULT ' ',
    update_time             timestamp    DEFAULT now(),
    del_flag                char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_requirement PRIMARY KEY (id),
    CONSTRAINT uk_rm_req_code UNIQUE (req_code)
);
COMMENT ON TABLE  rm_requirement IS '软件需求申请单（需求全流程载体）';
COMMENT ON COLUMN rm_requirement.id                    IS '主键';
COMMENT ON COLUMN rm_requirement.req_code              IS '申请单编号，规则 REQ-YYYYMMDD-XXX';
COMMENT ON COLUMN rm_requirement.title                 IS '需求标题';
COMMENT ON COLUMN rm_requirement.source                IS '需求来源：ITERATION/CUSTOMER_DELIVERY/CUSTOMER_OPS/PRESALE';
COMMENT ON COLUMN rm_requirement.customer_project     IS '关联客户/项目（来源为客户需求时填写）';
COMMENT ON COLUMN rm_requirement.description          IS '需求描述（富文本HTML）——重点：描述清晰的需求内容';
COMMENT ON COLUMN rm_requirement.expect_complete_date  IS '期望完成时间——重点：期望的时间节点';
COMMENT ON COLUMN rm_requirement.initiator_id         IS '发起人ID -> sys_user.user_id';
COMMENT ON COLUMN rm_requirement.initiator_dept_id    IS '发起部门ID -> sys_dept.dept_id';
COMMENT ON COLUMN rm_requirement.need_review          IS '是否需讨论会评审 0否 1是';
COMMENT ON COLUMN rm_requirement.review_conclusion    IS '讨论会评审结论 PASS/FAIL';
COMMENT ON COLUMN rm_requirement.review_remark        IS '讨论会评审意见';
COMMENT ON COLUMN rm_requirement.design_content       IS '需求设计（富文本HTML）——产品经理编制';
COMMENT ON COLUMN rm_requirement.design_workload      IS '需求设计工作量（人日）——考核点2';
COMMENT ON COLUMN rm_requirement.design_plan_date     IS '计划设计完成时间——考核点3';
COMMENT ON COLUMN rm_requirement.design_actual_date   IS '实际设计完成时间——考核点3：按时完成情况';
COMMENT ON COLUMN rm_requirement.schedule_remark      IS '排期说明/时间风险标注';
COMMENT ON COLUMN rm_requirement.schedule_risk        IS '时间风险等级 NORMAL/WARN/OVERDUE';
COMMENT ON COLUMN rm_requirement.status               IS '当前状态：DRAFT/PENDING_APPROVAL/REJECTED/PENDING_REVIEW/DESIGNING/DESIGN_REVIEW/SCHEDULING/DEVELOPING/TESTING/PENDING_ACCEPTANCE/COMPLETED/RELEASED';
COMMENT ON COLUMN rm_requirement.accept_conclusion    IS '验收结论 PASS/FAIL';
COMMENT ON COLUMN rm_requirement.accept_remark        IS '验收意见';
COMMENT ON COLUMN rm_requirement.complete_time        IS '系统自动完成时间（测试通过+质量确认后）';
COMMENT ON COLUMN rm_requirement.priority             IS '优先级 HIGH/MEDIUM/LOW';
CREATE INDEX idx_rm_req_status   ON rm_requirement(status);
CREATE INDEX idx_rm_req_init     ON rm_requirement(initiator_id);
CREATE INDEX idx_rm_req_source   ON rm_requirement(source);

-- 2. 开发任务单
CREATE TABLE rm_dev_task (
    id                  bigint       NOT NULL,
    task_code           varchar(32)  NOT NULL,
    requirement_id      bigint       NOT NULL,
    task_name           varchar(255) NOT NULL,
    task_desc           text,
    assignee_id         bigint,
    plan_start_date     date,
    plan_end_date       date,
    actual_start_date   date,
    actual_end_date     date,
    detail_design       text,
    review_conclusion   varchar(32),
    review_remark       text,
    status              varchar(32)  NOT NULL DEFAULT 'PENDING_DEV',
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_dev_task PRIMARY KEY (id),
    CONSTRAINT uk_rm_dev_task_code UNIQUE (task_code)
);
COMMENT ON TABLE  rm_dev_task IS '开发任务单（由需求分解产生）';
COMMENT ON COLUMN rm_dev_task.id                IS '主键';
COMMENT ON COLUMN rm_dev_task.task_code         IS '任务编号 DEV-XXX';
COMMENT ON COLUMN rm_dev_task.requirement_id    IS '所属需求单ID -> rm_requirement.id';
COMMENT ON COLUMN rm_dev_task.task_name         IS '任务名称';
COMMENT ON COLUMN rm_dev_task.task_desc         IS '任务描述';
COMMENT ON COLUMN rm_dev_task.assignee_id       IS '负责人ID -> sys_user.user_id';
COMMENT ON COLUMN rm_dev_task.plan_start_date   IS '计划开始时间';
COMMENT ON COLUMN rm_dev_task.plan_end_date     IS '计划完成时间';
COMMENT ON COLUMN rm_dev_task.actual_start_date IS '实际开始时间';
COMMENT ON COLUMN rm_dev_task.actual_end_date   IS '实际完成时间';
COMMENT ON COLUMN rm_dev_task.detail_design      IS '详细设计（富文本HTML）';
COMMENT ON COLUMN rm_dev_task.review_conclusion  IS '详细设计评审结论 PASS/FAIL/PENDING';
COMMENT ON COLUMN rm_dev_task.review_remark      IS '评审意见';
COMMENT ON COLUMN rm_dev_task.status            IS '任务状态 PENDING_DEV/IN_DEV/PENDING_TEST/COMPLETED';
CREATE INDEX idx_rm_dev_req      ON rm_dev_task(requirement_id);
CREATE INDEX idx_rm_dev_assignee ON rm_dev_task(assignee_id);
CREATE INDEX idx_rm_dev_status   ON rm_dev_task(status);

-- 3. 测试用例库
CREATE TABLE rm_test_case (
    id              bigint       NOT NULL,
    case_code       varchar(32)  NOT NULL,
    title           varchar(255) NOT NULL,
    requirement_id  bigint,
    pre_condition   text,
    steps           text,
    expect_result   text,
    case_type       varchar(32)  DEFAULT 'FUNCTIONAL',
    status          varchar(16)  NOT NULL DEFAULT 'DRAFT',
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_test_case PRIMARY KEY (id),
    CONSTRAINT uk_rm_case_code UNIQUE (case_code)
);
COMMENT ON TABLE  rm_test_case IS '测试用例库（用例关联需求，支撑覆盖率统计）';
COMMENT ON COLUMN rm_test_case.id             IS '主键';
COMMENT ON COLUMN rm_test_case.case_code       IS '用例编号 CASE-XXX';
COMMENT ON COLUMN rm_test_case.title           IS '用例标题';
COMMENT ON COLUMN rm_test_case.requirement_id  IS '关联需求ID -> rm_requirement.id（支撑覆盖率统计）';
COMMENT ON COLUMN rm_test_case.pre_condition   IS '前置条件';
COMMENT ON COLUMN rm_test_case.steps           IS '测试步骤';
COMMENT ON COLUMN rm_test_case.expect_result   IS '预期结果';
COMMENT ON COLUMN rm_test_case.case_type       IS '用例类型 FUNCTIONAL/PERFORMANCE/API';
COMMENT ON COLUMN rm_test_case.status          IS '用例状态 DRAFT/REVIEWING/ACTIVE/DEPRECATED';
CREATE INDEX idx_rm_case_req ON rm_test_case(requirement_id);

-- 4. 测试任务单
CREATE TABLE rm_test_task (
    id              bigint       NOT NULL,
    task_code       varchar(32)  NOT NULL,
    dev_task_id     bigint       NOT NULL,
    requirement_id  bigint       NOT NULL,
    tester_id       bigint,
    conclusion      varchar(32),
    workload        numeric(8,1),
    test_start_time timestamp,
    test_end_time   timestamp,
    status          varchar(32)  NOT NULL DEFAULT 'PENDING_TEST',
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_test_task PRIMARY KEY (id),
    CONSTRAINT uk_rm_tst_code UNIQUE (task_code)
);
COMMENT ON TABLE  rm_test_task IS '测试任务单（承载测试执行记录与结果）';
COMMENT ON COLUMN rm_test_task.id              IS '主键';
COMMENT ON COLUMN rm_test_task.task_code       IS '任务单编号 TST-XXX';
COMMENT ON COLUMN rm_test_task.dev_task_id     IS '关联开发任务单ID -> rm_dev_task.id';
COMMENT ON COLUMN rm_test_task.requirement_id   IS '关联需求单ID（冗余便于查询）';
COMMENT ON COLUMN rm_test_task.tester_id       IS '测试人员ID -> sys_user.user_id';
COMMENT ON COLUMN rm_test_task.conclusion      IS '测试结论 PASS/FAIL/BLOCKED';
COMMENT ON COLUMN rm_test_task.workload        IS '测试工作量（人日）——考核点5';
COMMENT ON COLUMN rm_test_task.test_start_time IS '测试开始时间';
COMMENT ON COLUMN rm_test_task.test_end_time   IS '测试结束时间';
COMMENT ON COLUMN rm_test_task.status          IS '状态 PENDING_TEST/IN_TEST/PASS/FAIL';
CREATE INDEX idx_rm_tst_dev    ON rm_test_task(dev_task_id);
CREATE INDEX idx_rm_tst_req    ON rm_test_task(requirement_id);
CREATE INDEX idx_rm_tst_tester ON rm_test_task(tester_id);

-- 5. 测试执行记录
CREATE TABLE rm_test_execution (
    id              bigint       NOT NULL,
    test_task_id    bigint       NOT NULL,
    case_id         bigint       NOT NULL,
    result          varchar(16)  NOT NULL,
    remark          text,
    bug_id          bigint,
    execute_time    timestamp    NOT NULL,
    executor_id     bigint       NOT NULL,
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_test_execution PRIMARY KEY (id)
);
COMMENT ON TABLE  rm_test_execution IS '测试执行记录（按用例逐条执行）';
COMMENT ON COLUMN rm_test_execution.id            IS '主键';
COMMENT ON COLUMN rm_test_execution.test_task_id   IS '所属测试任务单ID -> rm_test_task.id';
COMMENT ON COLUMN rm_test_execution.case_id        IS '测试用例ID -> rm_test_case.id';
COMMENT ON COLUMN rm_test_execution.result         IS '执行结果 PASS/FAIL/BLOCKED';
COMMENT ON COLUMN rm_test_execution.remark         IS '执行备注';
COMMENT ON COLUMN rm_test_execution.bug_id         IS '失败转出的bug ID -> rm_bug.id';
COMMENT ON COLUMN rm_test_execution.execute_time   IS '执行时间';
COMMENT ON COLUMN rm_test_execution.executor_id    IS '执行人ID';
CREATE INDEX idx_rm_exec_task ON rm_test_execution(test_task_id);
CREATE INDEX idx_rm_exec_case ON rm_test_execution(case_id);

-- 6. Bug 管理
CREATE TABLE rm_bug (
    id              bigint       NOT NULL,
    bug_code        varchar(32)  NOT NULL,
    title           varchar(255) NOT NULL,
    test_task_id    bigint,
    execution_id    bigint,
    requirement_id  bigint,
    source          varchar(32)  NOT NULL,
    severity        varchar(16),
    assignee_id     bigint,
    status          varchar(32)  NOT NULL DEFAULT 'NEW',
    create_date     date         NOT NULL,
    resolve_time    timestamp,
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_bug PRIMARY KEY (id),
    CONSTRAINT uk_rm_bug_code UNIQUE (bug_code)
);
COMMENT ON TABLE  rm_bug IS 'Bug管理（失败用例转bug，关联测试任务单）';
COMMENT ON COLUMN rm_bug.id             IS '主键';
COMMENT ON COLUMN rm_bug.bug_code        IS 'bug编号 BUG-XXX';
COMMENT ON COLUMN rm_bug.title          IS 'bug标题';
COMMENT ON COLUMN rm_bug.test_task_id   IS '关联测试任务单ID';
COMMENT ON COLUMN rm_bug.execution_id   IS '关联测试执行记录ID';
COMMENT ON COLUMN rm_bug.requirement_id  IS '关联需求ID（冗余，便于统计）';
COMMENT ON COLUMN rm_bug.source          IS '来源 TEST/PROJECT/OPS——支撑考核点7';
COMMENT ON COLUMN rm_bug.severity        IS '严重等级 CRITICAL/MAJOR/MINOR/TRIVIAL';
COMMENT ON COLUMN rm_bug.assignee_id     IS '处理人ID';
COMMENT ON COLUMN rm_bug.status          IS '状态 NEW/IN_PROGRESS/RESOLVED/VERIFIED/CLOSED';
COMMENT ON COLUMN rm_bug.create_date     IS '提报日期（判断当日bug，bug不隔人天）';
COMMENT ON COLUMN rm_bug.resolve_time     IS '解决时间';
CREATE INDEX idx_rm_bug_test     ON rm_bug(test_task_id);
CREATE INDEX idx_rm_bug_source   ON rm_bug(source);
CREATE INDEX idx_rm_bug_assignee ON rm_bug(assignee_id);
CREATE INDEX idx_rm_bug_date     ON rm_bug(create_date, status);

-- 7. 审批记录
CREATE TABLE rm_approval_record (
    id              bigint       NOT NULL,
    bill_type       varchar(32)  NOT NULL,
    bill_id         bigint       NOT NULL,
    node_code       varchar(32)  NOT NULL,
    approver_id     bigint       NOT NULL,
    conclusion      varchar(16)  NOT NULL,
    opinion         text,
    approve_time    timestamp    NOT NULL,
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_approval_record PRIMARY KEY (id)
);
COMMENT ON TABLE  rm_approval_record IS '审批记录（只增不删，操作留痕不可篡改）';
COMMENT ON COLUMN rm_approval_record.id            IS '主键';
COMMENT ON COLUMN rm_approval_record.bill_type     IS '单据类型 REQUIREMENT/DEV_TASK/ONLINE_APPLY/DATA_ACCESS';
COMMENT ON COLUMN rm_approval_record.bill_id       IS '单据ID';
COMMENT ON COLUMN rm_approval_record.node_code      IS '流程节点编码';
COMMENT ON COLUMN rm_approval_record.approver_id    IS '审批人ID';
COMMENT ON COLUMN rm_approval_record.conclusion     IS '审批结论 PASS/REJECT';
COMMENT ON COLUMN rm_approval_record.opinion        IS '审批意见';
COMMENT ON COLUMN rm_approval_record.approve_time   IS '审批时间';
CREATE INDEX idx_rm_appr_bill ON rm_approval_record(bill_type, bill_id);

-- 8. 待办
CREATE TABLE rm_todo (
    id          bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    bill_type   varchar(32)  NOT NULL,
    bill_id     bigint       NOT NULL,
    todo_type   varchar(32)  NOT NULL,
    title       varchar(255) NOT NULL,
    url         varchar(255),
    status      varchar(16)  NOT NULL DEFAULT 'PENDING',
    due_time    timestamp,
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_todo PRIMARY KEY (id)
);
COMMENT ON TABLE  rm_todo IS '待办事项（各角色聚合）';
COMMENT ON COLUMN rm_todo.id         IS '主键';
COMMENT ON COLUMN rm_todo.user_id    IS '待办归属人ID';
COMMENT ON COLUMN rm_todo.bill_type  IS '单据类型 REQUIREMENT/DEV_TASK/TEST_TASK/BUG';
COMMENT ON COLUMN rm_todo.bill_id    IS '单据ID';
COMMENT ON COLUMN rm_todo.todo_type  IS '待办类型 APPROVE/DESIGN/REVIEW/DEV/TEST/ACCEPT';
COMMENT ON COLUMN rm_todo.title      IS '待办标题';
COMMENT ON COLUMN rm_todo.url        IS '跳转路径';
COMMENT ON COLUMN rm_todo.status     IS '状态 PENDING/DONE/CANCEL';
COMMENT ON COLUMN rm_todo.due_time   IS '截止时间（超期预警用）';
CREATE INDEX idx_rm_todo_user ON rm_todo(user_id, status);
CREATE INDEX idx_rm_todo_bill ON rm_todo(bill_type, bill_id);

-- 9. 消息通知
CREATE TABLE rm_notify (
    id          bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    notify_type varchar(32)  NOT NULL,
    title       varchar(255) NOT NULL,
    content     text,
    bill_type   varchar(32),
    bill_id     bigint,
    is_read     char(1)      NOT NULL DEFAULT '0',
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_notify PRIMARY KEY (id)
);
COMMENT ON TABLE  rm_notify IS '消息通知（站内信）';
COMMENT ON COLUMN rm_notify.id          IS '主键';
COMMENT ON COLUMN rm_notify.user_id     IS '接收人ID';
COMMENT ON COLUMN rm_notify.notify_type IS '类型 APPROVAL/REJECT/TEST/RELEASE/OVERDUE/BUG_DAILY';
COMMENT ON COLUMN rm_notify.title       IS '标题';
COMMENT ON COLUMN rm_notify.content      IS '内容';
COMMENT ON COLUMN rm_notify.bill_type   IS '关联单据类型';
COMMENT ON COLUMN rm_notify.bill_id     IS '关联单据ID';
COMMENT ON COLUMN rm_notify.is_read     IS '已读标记 0未读 1已读';
CREATE INDEX idx_rm_notify_user ON rm_notify(user_id, is_read);

-- 10. 表单字段配置
CREATE TABLE rm_form_field_config (
    id          bigint       NOT NULL,
    form_code   varchar(32)  NOT NULL,
    field_key   varchar(64)  NOT NULL,
    field_label varchar(128) NOT NULL,
    field_type  varchar(32)  NOT NULL,
    required    char(1)      NOT NULL DEFAULT '0',
    visible     char(1)      NOT NULL DEFAULT '1',
    sortable    char(1)      NOT NULL DEFAULT '0',
    dict_code   varchar(64),
    sort_order  int          NOT NULL DEFAULT 0,
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_form_field_config PRIMARY KEY (id),
    CONSTRAINT uk_rm_form_field UNIQUE (form_code, field_key)
);
COMMENT ON TABLE  rm_form_field_config IS '表单字段配置（表单自定义配置）';
COMMENT ON COLUMN rm_form_field_config.id          IS '主键';
COMMENT ON COLUMN rm_form_field_config.form_code   IS '表单编码 REQUIREMENT/DEV_TASK/TEST_TASK';
COMMENT ON COLUMN rm_form_field_config.field_key   IS '字段key';
COMMENT ON COLUMN rm_form_field_config.field_label IS '字段标签';
COMMENT ON COLUMN rm_form_field_config.field_type IS '类型 text/richtext/date/number/select';
COMMENT ON COLUMN rm_form_field_config.required    IS '是否必填 0否 1是';
COMMENT ON COLUMN rm_form_field_config.visible     IS '是否可见 0否 1是';
COMMENT ON COLUMN rm_form_field_config.sortable    IS '列表是否可排序 0否 1是';
COMMENT ON COLUMN rm_form_field_config.dict_code   IS '关联字典编码';
COMMENT ON COLUMN rm_form_field_config.sort_order  IS '排序';
CREATE INDEX idx_rm_form_code ON rm_form_field_config(form_code);

-- 11. 流程节点配置
CREATE TABLE rm_flow_node (
    id              bigint       NOT NULL,
    flow_code       varchar(32)  NOT NULL,
    node_code       varchar(32)  NOT NULL,
    node_name       varchar(64)  NOT NULL,
    approver_type   varchar(32)  NOT NULL,
    approver_ref    varchar(128),
    next_node_pass  varchar(32),
    next_node_reject varchar(32),
    sort_order      int          NOT NULL DEFAULT 0,
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_rm_flow_node PRIMARY KEY (id),
    CONSTRAINT uk_rm_flow_node UNIQUE (flow_code, node_code)
);
COMMENT ON TABLE  rm_flow_node IS '流程节点配置（审批链路可视化配置）';
COMMENT ON COLUMN rm_flow_node.id              IS '主键';
COMMENT ON COLUMN rm_flow_node.flow_code       IS '流程编码 REQUIREMENT_FLOW/DEV_TASK_FLOW';
COMMENT ON COLUMN rm_flow_node.node_code       IS '节点编码';
COMMENT ON COLUMN rm_flow_node.node_name       IS '节点名称';
COMMENT ON COLUMN rm_flow_node.approver_type   IS '审批人类型 ROLE/DEPT_LEADER/USER/INITIATOR_LEADER';
COMMENT ON COLUMN rm_flow_node.approver_ref    IS '审批人引用（角色编码/部门ID/用户ID）';
COMMENT ON COLUMN rm_flow_node.next_node_pass  IS '通过后下一节点编码';
COMMENT ON COLUMN rm_flow_node.next_node_reject IS '驳回后下一节点编码';
COMMENT ON COLUMN rm_flow_node.sort_order      IS '节点顺序';
CREATE INDEX idx_rm_flow_code ON rm_flow_node(flow_code);
