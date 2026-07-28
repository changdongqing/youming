-- ============================================================
-- V16: 建模域 - 可视化画布状态持久化（FR-17）
-- 建 ont_model_canvas_state + 菜单种子（11500 段）
-- 详见《详细设计-DD11-可视化建模画布.md》
-- ============================================================

-- ---------- (a) 建表 ----------

CREATE TABLE ont_model_canvas_state (
    id              bigint       NOT NULL,
    project_id      bigint       NOT NULL,
    user_id         varchar(64)  NOT NULL,
    graph_data      text,
    create_by       varchar(64)  DEFAULT ' ',
    create_time     timestamp    DEFAULT now(),
    update_by       varchar(64)  DEFAULT ' ',
    update_time     timestamp    DEFAULT now(),
    del_flag        char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_model_canvas_state PRIMARY KEY (id),
    CONSTRAINT uk_ont_model_canvas_state UNIQUE (project_id, user_id)
);
COMMENT ON TABLE  ont_model_canvas_state IS '可视化画布状态持久化（每项目每用户，FR-17.8）';
COMMENT ON COLUMN ont_model_canvas_state.id          IS '主键';
COMMENT ON COLUMN ont_model_canvas_state.project_id  IS '所属项目 ID';
COMMENT ON COLUMN ont_model_canvas_state.user_id     IS '用户名（与 createBy 一致，每用户独立画布状态）';
COMMENT ON COLUMN ont_model_canvas_state.graph_data  IS 'AntV X6 graph.toJSON() 序列化的图数据（节点位置/边/缩放）';
COMMENT ON COLUMN ont_model_canvas_state.create_by   IS '创建人';
COMMENT ON COLUMN ont_model_canvas_state.create_time IS '创建时间';
COMMENT ON COLUMN ont_model_canvas_state.update_by   IS '修改人';
COMMENT ON COLUMN ont_model_canvas_state.update_time IS '修改时间';
COMMENT ON COLUMN ont_model_canvas_state.del_flag    IS '删除标记，0未删除，1已删除';

-- ---------- (b) 索引 ----------

CREATE INDEX idx_ont_model_canvas_project ON ont_model_canvas_state (project_id) WHERE del_flag = '0';

-- ---------- (c) sys_menu 菜单种子（11500 段） ----------
-- 字段位序（17 列）：menu_id / name / permission / path / component / parent_id / icon /
--                  visible / sort_order / keep_alive / embedded / menu_type /
--                  create_by / create_time / update_by / update_time / del_flag

INSERT INTO sys_menu VALUES (11500, '可视化画布', NULL, '/admin/ontology-model/canvas/index', NULL, 11000, 'iconfont icon-tubiao', '1', 5, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11501, '画布查看', 'ont_canvas_view',   NULL, NULL, 11500, NULL, '1', 1, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (11502, '画布编辑', 'ont_canvas_manage', NULL, NULL, 11500, NULL, '1', 2, '0', NULL, '1', 'admin', now(), 'admin', now(), '0');
