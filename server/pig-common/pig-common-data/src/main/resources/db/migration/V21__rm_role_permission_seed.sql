-- ============================================================
-- V21__rm_role_permission_seed.sql
-- 研发管理角色定义 + 角色-权限关联
-- 对应《研发管理工具详细设计计划_v1.0》§10 角色权限矩阵
-- ============================================================

-- ---------- (a) sys_role 种子（9 字段按位置 INSERT） ----------
-- 字段顺序：role_id, role_name, role_code, role_desc, create_by, update_by, create_time, update_time, del_flag
-- role_id 段：19001~19012

INSERT INTO sys_role VALUES (19001, '产品经理/总工',   'rm_product_approver', '需求合理性审批+需求设计+质量确认', 'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19002, '需求设计人',      'rm_designer',         '需求分析设计',                     'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19003, '开发经理',        'rm_dev_manager',      '任务分解+排期',                     'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19004, '开发人员',        'rm_developer',        '详细设计+开发',                     'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19005, '测试人员',        'rm_tester',           '测试执行+bug',                      'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19006, '技术经理',        'rm_tech_manager',     '发版计划',                           'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19007, '总监/项目经理',   'rm_delivery_manager', '交付中心需求审批+上线申请',         'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19008, '实施人员',        'rm_implementer',      '上线申请+验证+试运行',              'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19009, '运维服务人员',    'rm_ops',              '需求提报+上线执行',                 'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19010, '售前人员',        'rm_presale',          '项目需求提报',                      'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19011, '公司领导',        'rm_leader',           '评审+上线审批',                     'admin', 'admin', now(), now(), '0');
INSERT INTO sys_role VALUES (19012, '系统管理员',      'rm_admin',            '系统配置',                           'admin', 'admin', now(), now(), '0');

-- ---------- (b) sys_role_menu 种子（2 字段按位置 INSERT） ----------
-- 字段顺序：role_id, menu_id
-- 权限矩阵对应设计计划 §10.2

-- 产品经理(19001)：需求审批+设计+质量确认+报表+待办
INSERT INTO sys_role_menu VALUES (19001, 19000), (19001, 19100), (19001, 19101), (19001, 19105), (19001, 19106);
INSERT INTO sys_role_menu VALUES (19001, 19400), (19001, 19401), (19001, 19500), (19001, 19501);

-- 需求设计人(19002)：需求设计+查看+待办
INSERT INTO sys_role_menu VALUES (19002, 19000), (19002, 19100), (19002, 19101), (19002, 19106);
INSERT INTO sys_role_menu VALUES (19002, 19500), (19002, 19501);

-- 开发经理(19003)：任务分解+排期+查看+待办
INSERT INTO sys_role_menu VALUES (19003, 19000), (19003, 19100), (19003, 19101), (19003, 19107);
INSERT INTO sys_role_menu VALUES (19003, 19200), (19003, 19201), (19003, 19202), (19003, 19203);
INSERT INTO sys_role_menu VALUES (19003, 19500), (19003, 19501);

-- 开发人员(19004)：详细设计+开发+查看+待办
INSERT INTO sys_role_menu VALUES (19004, 19000), (19004, 19100), (19004, 19101);
INSERT INTO sys_role_menu VALUES (19004, 19200), (19004, 19201), (19004, 19203), (19004, 19204);
INSERT INTO sys_role_menu VALUES (19004, 19500), (19004, 19501);

-- 测试人员(19005)：测试执行+bug+用例库+查看+待办
INSERT INTO sys_role_menu VALUES (19005, 19000), (19005, 19100), (19005, 19101);
INSERT INTO sys_role_menu VALUES (19005, 19300), (19005, 19301), (19005, 19302), (19005, 19303), (19005, 19304);
INSERT INTO sys_role_menu VALUES (19005, 19305), (19005, 19306), (19005, 19307), (19005, 19308);
INSERT INTO sys_role_menu VALUES (19005, 19500), (19005, 19501);

-- 技术经理(19006)：发版相关+查看+待办（发版功能二期，本阶段仅查看+待办）
INSERT INTO sys_role_menu VALUES (19006, 19000), (19006, 19100), (19006, 19101);
INSERT INTO sys_role_menu VALUES (19006, 19500), (19006, 19501);

-- 总监/项目经理(19007)：需求审批(交付)+查看+待办
INSERT INTO sys_role_menu VALUES (19007, 19000), (19007, 19100), (19007, 19101), (19007, 19102), (19007, 19105);
INSERT INTO sys_role_menu VALUES (19007, 19500), (19007, 19501);

-- 实施人员(19008)：需求提报+查看+待办
INSERT INTO sys_role_menu VALUES (19008, 19000), (19008, 19100), (19008, 19101), (19008, 19102);
INSERT INTO sys_role_menu VALUES (19008, 19500), (19008, 19501);

-- 运维服务人员(19009)：需求提报+查看+待办
INSERT INTO sys_role_menu VALUES (19009, 19000), (19009, 19100), (19009, 19101), (19009, 19102);
INSERT INTO sys_role_menu VALUES (19009, 19500), (19009, 19501);

-- 售前人员(19010)：需求提报+查看+待办
INSERT INTO sys_role_menu VALUES (19010, 19000), (19010, 19100), (19010, 19101), (19010, 19102);
INSERT INTO sys_role_menu VALUES (19010, 19500), (19010, 19501);

-- 公司领导(19011)：设计评审+详细设计评审+查看+待办
INSERT INTO sys_role_menu VALUES (19011, 19000), (19011, 19100), (19011, 19101), (19011, 19105);
INSERT INTO sys_role_menu VALUES (19011, 19200), (19011, 19201), (19011, 19205);
INSERT INTO sys_role_menu VALUES (19011, 19500), (19011, 19501);

-- 系统管理员(19012)：全部 rm 菜单权限
INSERT INTO sys_role_menu VALUES
    (19012, 19000), (19012, 19100), (19012, 19101), (19012, 19102), (19012, 19103), (19012, 19104),
    (19012, 19105), (19012, 19106), (19012, 19107),
    (19012, 19200), (19012, 19201), (19012, 19202), (19012, 19203), (19012, 19204), (19012, 19205),
    (19012, 19300), (19012, 19301), (19012, 19302), (19012, 19303), (19012, 19304),
    (19012, 19305), (19012, 19306), (19012, 19307), (19012, 19308),
    (19012, 19400), (19012, 19401),
    (19012, 19500), (19012, 19501);
