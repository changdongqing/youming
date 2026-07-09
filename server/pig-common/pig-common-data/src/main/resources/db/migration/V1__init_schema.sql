-- PostgreSQL 业务库结构 (由 pig.sql 转换, 38 张业务表)
-- 生成工具: convert_mysql_to_pg.py
-- 转换规则见 docs/dbversion/08-DDL转换结果核验清单.md

-- ----------------------------
-- Table structure for sys_area
-- ----------------------------
DROP TABLE IF EXISTS sys_area;
CREATE TABLE sys_area (
  id bigint NOT NULL,
  pid bigint NOT NULL DEFAULT '0',
  name varchar(255) NOT NULL DEFAULT '',
  letter varchar(255) DEFAULT '',
  adcode bigint NOT NULL,
  location varchar(255) DEFAULT '',
  area_sort bigint DEFAULT NULL,
  area_status char(1) NOT NULL DEFAULT '1',
  area_type char(1) NOT NULL DEFAULT '0',
  hot char(1) NOT NULL DEFAULT '0',
  city_code varchar(30) DEFAULT '',
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64) DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_area IS '行政区划表';
COMMENT ON COLUMN sys_area.id IS '主键ID';
COMMENT ON COLUMN sys_area.pid IS '父ID';
COMMENT ON COLUMN sys_area.name IS '地区名称';
COMMENT ON COLUMN sys_area.letter IS '地区字母';
COMMENT ON COLUMN sys_area.adcode IS '高德地区code';
COMMENT ON COLUMN sys_area.location IS '经纬度';
COMMENT ON COLUMN sys_area.area_sort IS '排序值';
COMMENT ON COLUMN sys_area.area_status IS '0:未生效，1:生效';
COMMENT ON COLUMN sys_area.area_type IS '0:国家,1:省,2:城市,3:区县';
COMMENT ON COLUMN sys_area.hot IS '0:非热门，1:热门';
COMMENT ON COLUMN sys_area.city_code IS '城市编码';
COMMENT ON COLUMN sys_area.create_by IS '创建人';
COMMENT ON COLUMN sys_area.create_time IS '创建时间';
COMMENT ON COLUMN sys_area.update_by IS '更新时间';
COMMENT ON COLUMN sys_area.update_time IS '更新时间';
COMMENT ON COLUMN sys_area.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for sys_api_key
-- ----------------------------
DROP TABLE IF EXISTS sys_api_key;
CREATE TABLE sys_api_key (
  id bigint NOT NULL,
  user_id bigint NOT NULL,
  username varchar(64) NOT NULL,
  name varchar(64) NOT NULL,
  api_key_hash varchar(64) NOT NULL,
  allowed_ips varchar(512) DEFAULT NULL,
  expires_at timestamp DEFAULT NULL,
  status char(1) NOT NULL DEFAULT '0',
  last_used_at timestamp DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_api_key_hash ON sys_api_key (api_key_hash);
CREATE INDEX idx_user_id ON sys_api_key (user_id);
COMMENT ON TABLE sys_api_key IS 'API密钥管理表';
COMMENT ON COLUMN sys_api_key.id IS '主键';
COMMENT ON COLUMN sys_api_key.user_id IS '所属用户ID';
COMMENT ON COLUMN sys_api_key.username IS '所属用户名';
COMMENT ON COLUMN sys_api_key.name IS 'Key名称';
COMMENT ON COLUMN sys_api_key.api_key_hash IS 'SHA-256哈希值';
COMMENT ON COLUMN sys_api_key.allowed_ips IS 'IP白名单（逗号分隔，空=不限制）';
COMMENT ON COLUMN sys_api_key.expires_at IS '过期时间（null=永不过期）';
COMMENT ON COLUMN sys_api_key.status IS '状态：0-正常，1-禁用';
COMMENT ON COLUMN sys_api_key.last_used_at IS '最后使用时间';
COMMENT ON COLUMN sys_api_key.del_flag IS '逻辑删除：0-正常，1-已删除';
COMMENT ON COLUMN sys_api_key.create_by IS '创建者';
COMMENT ON COLUMN sys_api_key.update_by IS '更新者';
COMMENT ON COLUMN sys_api_key.create_time IS '创建时间';
COMMENT ON COLUMN sys_api_key.update_time IS '更新时间';

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  dept_id bigint NOT NULL,
  name varchar(50) DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  parent_id bigint DEFAULT NULL,
  PRIMARY KEY (dept_id)
);
COMMENT ON TABLE sys_dept IS '部门管理';
COMMENT ON COLUMN sys_dept.dept_id IS '部门ID';
COMMENT ON COLUMN sys_dept.name IS '部门名称';
COMMENT ON COLUMN sys_dept.sort_order IS '排序';
COMMENT ON COLUMN sys_dept.create_by IS '创建人';
COMMENT ON COLUMN sys_dept.update_by IS '修改人';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.update_time IS '修改时间';
COMMENT ON COLUMN sys_dept.del_flag IS '删除标志';
COMMENT ON COLUMN sys_dept.parent_id IS '父级部门ID';

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
  id bigint NOT NULL,
  dict_type varchar(100) DEFAULT NULL,
  description varchar(100) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  remarks varchar(255) DEFAULT NULL,
  system_flag char(1) DEFAULT '0',
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
CREATE INDEX sys_dict_del_flag ON sys_dict (del_flag);
COMMENT ON TABLE sys_dict IS '字典表';
COMMENT ON COLUMN sys_dict.id IS '编号';
COMMENT ON COLUMN sys_dict.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict.description IS '描述';
COMMENT ON COLUMN sys_dict.create_by IS '创建人';
COMMENT ON COLUMN sys_dict.update_by IS '修改人';
COMMENT ON COLUMN sys_dict.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict.remarks IS '备注信息';
COMMENT ON COLUMN sys_dict.system_flag IS '系统标志';
COMMENT ON COLUMN sys_dict.del_flag IS '删除标志';

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
  id bigint NOT NULL,
  dict_id bigint NOT NULL,
  item_value varchar(100) DEFAULT NULL,
  label varchar(100) DEFAULT NULL,
  dict_type varchar(100) DEFAULT NULL,
  description varchar(100) DEFAULT NULL,
  list_class varchar(50) DEFAULT NULL,
  sort_order integer NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  remarks varchar(255) DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
CREATE INDEX sys_dict_value ON sys_dict_item (item_value);
CREATE INDEX sys_dict_label ON sys_dict_item (label);
CREATE INDEX sys_dict_item_del_flag ON sys_dict_item (del_flag);
COMMENT ON TABLE sys_dict_item IS '字典项';
COMMENT ON COLUMN sys_dict_item.id IS '编号';
COMMENT ON COLUMN sys_dict_item.dict_id IS '字典ID';
COMMENT ON COLUMN sys_dict_item.item_value IS '字典项值';
COMMENT ON COLUMN sys_dict_item.label IS '字典项名称';
COMMENT ON COLUMN sys_dict_item.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_item.description IS '字典项描述';
COMMENT ON COLUMN sys_dict_item.list_class IS '标签类型';
COMMENT ON COLUMN sys_dict_item.sort_order IS '排序（升序）';
COMMENT ON COLUMN sys_dict_item.create_by IS '创建人';
COMMENT ON COLUMN sys_dict_item.update_by IS '修改人';
COMMENT ON COLUMN sys_dict_item.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_item.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_item.remarks IS '备注信息';
COMMENT ON COLUMN sys_dict_item.del_flag IS '删除标志';

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
  id bigint NOT NULL,
  group_id bigint DEFAULT NULL,
  file_name varchar(100) DEFAULT NULL,
  bucket_name varchar(200) DEFAULT NULL,
  dir varchar(200) DEFAULT NULL,
  original varchar(100) DEFAULT NULL,
  type varchar(50) DEFAULT NULL,
  hash varchar(50) DEFAULT NULL,
  file_size bigint DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_file IS '文件管理表';
COMMENT ON COLUMN sys_file.id IS '编号';
COMMENT ON COLUMN sys_file.group_id IS '文件组';
COMMENT ON COLUMN sys_file.file_name IS '文件名';
COMMENT ON COLUMN sys_file.bucket_name IS '文件存储桶名称';
COMMENT ON COLUMN sys_file.dir IS '文件夹名称';
COMMENT ON COLUMN sys_file.original IS '原始文件名';
COMMENT ON COLUMN sys_file.type IS '文件类型';
COMMENT ON COLUMN sys_file.hash IS '文件hash';
COMMENT ON COLUMN sys_file.file_size IS '文件大小';
COMMENT ON COLUMN sys_file.create_by IS '创建人';
COMMENT ON COLUMN sys_file.update_by IS '修改人';
COMMENT ON COLUMN sys_file.create_time IS '上传时间';
COMMENT ON COLUMN sys_file.update_time IS '更新时间';
COMMENT ON COLUMN sys_file.del_flag IS '删除标志';

-- ----------------------------
-- Table structure for sys_file_group
-- ----------------------------
DROP TABLE IF EXISTS sys_file_group;
CREATE TABLE sys_file_group (
  id bigint NOT NULL,
  type smallint DEFAULT '10',
  name varchar(32) DEFAULT '',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  create_by varchar(64) DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  pid bigint DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_file_group IS '文件分类表';
COMMENT ON COLUMN sys_file_group.id IS '主键ID';
COMMENT ON COLUMN sys_file_group.type IS '类型: [10=图片, 20=视频]';
COMMENT ON COLUMN sys_file_group.name IS '分类名称';
COMMENT ON COLUMN sys_file_group.create_time IS '创建时间';
COMMENT ON COLUMN sys_file_group.update_time IS '更新时间';
COMMENT ON COLUMN sys_file_group.del_flag IS '删除标记';
COMMENT ON COLUMN sys_file_group.create_by IS '创建人';
COMMENT ON COLUMN sys_file_group.update_by IS '修改人';
COMMENT ON COLUMN sys_file_group.pid IS '父ID';

-- ----------------------------
-- Table structure for sys_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS sys_sensitive_word;
CREATE TABLE sys_sensitive_word (
  sensitive_id bigint NOT NULL,
  sensitive_word varchar(255) DEFAULT NULL,
  sensitive_type char(1) DEFAULT NULL,
  remark varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (sensitive_id)
);
COMMENT ON TABLE sys_sensitive_word IS '敏感词';
COMMENT ON COLUMN sys_sensitive_word.sensitive_id IS '主键';
COMMENT ON COLUMN sys_sensitive_word.sensitive_word IS '敏感词';
COMMENT ON COLUMN sys_sensitive_word.sensitive_type IS '类型';
COMMENT ON COLUMN sys_sensitive_word.remark IS '备注';
COMMENT ON COLUMN sys_sensitive_word.create_by IS '创建人';
COMMENT ON COLUMN sys_sensitive_word.create_time IS '创建时间';
COMMENT ON COLUMN sys_sensitive_word.update_by IS '修改人';
COMMENT ON COLUMN sys_sensitive_word.update_time IS '修改时间';
COMMENT ON COLUMN sys_sensitive_word.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for sys_i18n
-- ----------------------------
DROP TABLE IF EXISTS sys_i18n;
CREATE TABLE sys_i18n (
  id bigint NOT NULL,
  name varchar(255) NOT NULL,
  zh_cn varchar(255) NOT NULL,
  en varchar(255) NOT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_i18n IS '系统表-国际化';
COMMENT ON COLUMN sys_i18n.id IS 'id';
COMMENT ON COLUMN sys_i18n.name IS 'name';
COMMENT ON COLUMN sys_i18n.zh_cn IS '中文';
COMMENT ON COLUMN sys_i18n.en IS '英文';
COMMENT ON COLUMN sys_i18n.create_by IS '创建人';
COMMENT ON COLUMN sys_i18n.create_time IS '创建时间';
COMMENT ON COLUMN sys_i18n.update_by IS '修改人';
COMMENT ON COLUMN sys_i18n.update_time IS '更新时间';
COMMENT ON COLUMN sys_i18n.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
  id bigint NOT NULL,
  log_type char(1) DEFAULT '0',
  title varchar(255) DEFAULT NULL,
  service_id varchar(32) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  remote_addr varchar(255) DEFAULT NULL,
  user_agent varchar(1000) DEFAULT NULL,
  request_uri varchar(255) DEFAULT NULL,
  method varchar(10) DEFAULT NULL,
  params text,
  time bigint DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  exception text,
  PRIMARY KEY (id)
);
CREATE INDEX sys_log_request_uri ON sys_log (request_uri);
CREATE INDEX sys_log_type ON sys_log (log_type);
CREATE INDEX sys_log_create_date ON sys_log (create_time);
COMMENT ON TABLE sys_log IS '日志表';
COMMENT ON COLUMN sys_log.id IS '编号';
COMMENT ON COLUMN sys_log.log_type IS '日志类型';
COMMENT ON COLUMN sys_log.title IS '日志标题';
COMMENT ON COLUMN sys_log.service_id IS '服务ID';
COMMENT ON COLUMN sys_log.create_by IS '创建人';
COMMENT ON COLUMN sys_log.update_by IS '修改人';
COMMENT ON COLUMN sys_log.create_time IS '创建时间';
COMMENT ON COLUMN sys_log.update_time IS '更新时间';
COMMENT ON COLUMN sys_log.remote_addr IS '远程地址';
COMMENT ON COLUMN sys_log.user_agent IS '用户代理';
COMMENT ON COLUMN sys_log.request_uri IS '请求URI';
COMMENT ON COLUMN sys_log.method IS '请求方法';
COMMENT ON COLUMN sys_log.params IS '请求参数';
COMMENT ON COLUMN sys_log.time IS '执行时间';
COMMENT ON COLUMN sys_log.del_flag IS '删除标志';
COMMENT ON COLUMN sys_log.exception IS '异常信息';

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  menu_id bigint NOT NULL,
  name varchar(32) DEFAULT NULL,
  permission varchar(128) DEFAULT NULL,
  path varchar(128) DEFAULT NULL,
  component varchar(255) DEFAULT NULL,
  parent_id bigint DEFAULT NULL,
  icon varchar(64) DEFAULT NULL,
  visible char(1) DEFAULT '1',
  sort_order integer DEFAULT '1',
  keep_alive char(1) DEFAULT '0',
  embedded char(1) DEFAULT NULL,
  menu_type char(1) DEFAULT '0',
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (menu_id)
);
COMMENT ON TABLE sys_menu IS '菜单权限表';
COMMENT ON COLUMN sys_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.permission IS '权限标识';
COMMENT ON COLUMN sys_menu.path IS '路由路径';
COMMENT ON COLUMN sys_menu.component IS '组件';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.visible IS '是否可见，0隐藏，1显示';
COMMENT ON COLUMN sys_menu.sort_order IS '排序值，越小越靠前';
COMMENT ON COLUMN sys_menu.keep_alive IS '是否缓存，0否，1是';
COMMENT ON COLUMN sys_menu.embedded IS '是否内嵌，0否，1是';
COMMENT ON COLUMN sys_menu.menu_type IS '菜单类型，0:菜单 1:按钮';
COMMENT ON COLUMN sys_menu.create_by IS '创建人';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_by IS '修改人';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.del_flag IS '删除标志，0未删除，1已删除';

-- ----------------------------
-- Table structure for sys_oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS sys_oauth_client_details;
CREATE TABLE sys_oauth_client_details (
  id bigint NOT NULL,
  client_id varchar(32) NOT NULL,
  resource_ids varchar(256) DEFAULT NULL,
  client_secret varchar(256) DEFAULT NULL,
  scope varchar(256) DEFAULT NULL,
  authorized_grant_types varchar(256) DEFAULT NULL,
  web_server_redirect_uri varchar(256) DEFAULT NULL,
  authorities varchar(256) DEFAULT NULL,
  access_token_validity integer DEFAULT NULL,
  refresh_token_validity integer DEFAULT NULL,
  additional_information varchar(4096) DEFAULT NULL,
  autoapprove varchar(256) DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_oauth_client_details IS '终端信息表';
COMMENT ON COLUMN sys_oauth_client_details.id IS 'ID';
COMMENT ON COLUMN sys_oauth_client_details.client_id IS '客户端ID';
COMMENT ON COLUMN sys_oauth_client_details.resource_ids IS '资源ID集合';
COMMENT ON COLUMN sys_oauth_client_details.client_secret IS '客户端秘钥';
COMMENT ON COLUMN sys_oauth_client_details.scope IS '授权范围';
COMMENT ON COLUMN sys_oauth_client_details.authorized_grant_types IS '授权类型';
COMMENT ON COLUMN sys_oauth_client_details.web_server_redirect_uri IS '回调地址';
COMMENT ON COLUMN sys_oauth_client_details.authorities IS '权限集合';
COMMENT ON COLUMN sys_oauth_client_details.access_token_validity IS '访问令牌有效期（秒）';
COMMENT ON COLUMN sys_oauth_client_details.refresh_token_validity IS '刷新令牌有效期（秒）';
COMMENT ON COLUMN sys_oauth_client_details.additional_information IS '附加信息';
COMMENT ON COLUMN sys_oauth_client_details.autoapprove IS '自动授权';
COMMENT ON COLUMN sys_oauth_client_details.del_flag IS '删除标记，0未删除，1已删除';
COMMENT ON COLUMN sys_oauth_client_details.create_by IS '创建人';
COMMENT ON COLUMN sys_oauth_client_details.update_by IS '修改人';
COMMENT ON COLUMN sys_oauth_client_details.create_time IS '创建时间';
COMMENT ON COLUMN sys_oauth_client_details.update_time IS '更新时间';

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS sys_post;
CREATE TABLE sys_post (
  post_id bigint NOT NULL,
  post_code varchar(64) NOT NULL,
  post_name varchar(50) NOT NULL,
  post_sort integer NOT NULL,
  remark varchar(500) DEFAULT NULL,
  del_flag char(1) NOT NULL DEFAULT '0',
  create_time timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  update_time timestamp DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  PRIMARY KEY (post_id)
);
COMMENT ON TABLE sys_post IS '岗位信息表';
COMMENT ON COLUMN sys_post.post_id IS '岗位ID';
COMMENT ON COLUMN sys_post.post_code IS '岗位编码';
COMMENT ON COLUMN sys_post.post_name IS '岗位名称';
COMMENT ON COLUMN sys_post.post_sort IS '岗位排序';
COMMENT ON COLUMN sys_post.remark IS '岗位描述';
COMMENT ON COLUMN sys_post.del_flag IS '是否删除  -1：已删除  0：正常';
COMMENT ON COLUMN sys_post.create_time IS '创建时间';
COMMENT ON COLUMN sys_post.create_by IS '创建人';
COMMENT ON COLUMN sys_post.update_time IS '更新时间';
COMMENT ON COLUMN sys_post.update_by IS '更新人';

-- ----------------------------
-- Table structure for sys_public_param
-- ----------------------------
DROP TABLE IF EXISTS sys_public_param;
CREATE TABLE sys_public_param (
  public_id bigint NOT NULL,
  public_name varchar(128) DEFAULT NULL,
  public_key varchar(128) DEFAULT NULL,
  public_value varchar(128) DEFAULT NULL,
  status char(1) DEFAULT '0',
  validate_code varchar(64) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT NULL,
  public_type char(1) DEFAULT '0',
  system_flag char(1) DEFAULT '0',
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (public_id)
);
COMMENT ON TABLE sys_public_param IS '公共参数配置表';
COMMENT ON COLUMN sys_public_param.public_id IS '编号';
COMMENT ON COLUMN sys_public_param.public_name IS '名称';
COMMENT ON COLUMN sys_public_param.public_key IS '键';
COMMENT ON COLUMN sys_public_param.public_value IS '值';
COMMENT ON COLUMN sys_public_param.status IS '状态，0禁用，1启用';
COMMENT ON COLUMN sys_public_param.validate_code IS '校验码';
COMMENT ON COLUMN sys_public_param.create_by IS '创建人';
COMMENT ON COLUMN sys_public_param.update_by IS '修改人';
COMMENT ON COLUMN sys_public_param.create_time IS '创建时间';
COMMENT ON COLUMN sys_public_param.update_time IS '更新时间';
COMMENT ON COLUMN sys_public_param.public_type IS '类型，0未知，1系统，2业务';
COMMENT ON COLUMN sys_public_param.system_flag IS '系统标识，0非系统，1系统';
COMMENT ON COLUMN sys_public_param.del_flag IS '删除标记，0未删除，1已删除';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  role_id bigint NOT NULL,
  role_name varchar(64) DEFAULT NULL,
  role_code varchar(64) DEFAULT NULL,
  role_desc varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (role_id)
);
CREATE INDEX role_idx1_role_code ON sys_role (role_code);
COMMENT ON TABLE sys_role IS '系统角色表';
COMMENT ON COLUMN sys_role.role_id IS '角色ID';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.role_desc IS '角色描述';
COMMENT ON COLUMN sys_role.create_by IS '创建人';
COMMENT ON COLUMN sys_role.update_by IS '修改人';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.del_flag IS '删除标记，0未删除，1已删除';

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  role_id bigint NOT NULL,
  menu_id bigint NOT NULL,
  PRIMARY KEY (role_id,menu_id)
);
COMMENT ON TABLE sys_role_menu IS '角色菜单表';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

-- ----------------------------
-- Table structure for sys_role_widget
-- ----------------------------
DROP TABLE IF EXISTS sys_role_widget;
CREATE TABLE sys_role_widget (
  id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  widget_keys VARCHAR(2000) NOT NULL,
  layout_config TEXT DEFAULT NULL,
  del_flag CHAR(1) DEFAULT '0',
  create_by VARCHAR(64) DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_role_widget IS '角色首页组件配置';
COMMENT ON COLUMN sys_role_widget.id IS '主键';
COMMENT ON COLUMN sys_role_widget.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_widget.widget_keys IS '允许的组件key列表，逗号分隔';
COMMENT ON COLUMN sys_role_widget.layout_config IS '默认布局JSON，格式同前端grid对象';
COMMENT ON COLUMN sys_role_widget.del_flag IS '删除标记,1:已删除,0:正常';
COMMENT ON COLUMN sys_role_widget.create_by IS '创建人';
COMMENT ON COLUMN sys_role_widget.update_by IS '修改人';
COMMENT ON COLUMN sys_role_widget.create_time IS '创建时间';
COMMENT ON COLUMN sys_role_widget.update_time IS '修改时间';

-- ----------------------------
-- Table structure for sys_schedule
-- ----------------------------
DROP TABLE IF EXISTS sys_schedule;
CREATE TABLE sys_schedule (
  id bigint NOT NULL,
  title varchar(255) DEFAULT NULL,
  schedule_type varchar(255) DEFAULT NULL,
  schedule_state varchar(255) DEFAULT NULL,
  content text,
  schedule_time time DEFAULT NULL,
  schedule_date date DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT ' ',
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_schedule IS '系统日程管理表';
COMMENT ON COLUMN sys_schedule.id IS 'id';
COMMENT ON COLUMN sys_schedule.title IS '标题';
COMMENT ON COLUMN sys_schedule.schedule_type IS '日程类型';
COMMENT ON COLUMN sys_schedule.schedule_state IS '状态';
COMMENT ON COLUMN sys_schedule.content IS '内容';
COMMENT ON COLUMN sys_schedule.schedule_time IS '时间';
COMMENT ON COLUMN sys_schedule.schedule_date IS '日期';
COMMENT ON COLUMN sys_schedule.create_by IS '创建人';
COMMENT ON COLUMN sys_schedule.create_time IS '创建时间';
COMMENT ON COLUMN sys_schedule.update_by IS '修改人';
COMMENT ON COLUMN sys_schedule.update_time IS '更新时间';
COMMENT ON COLUMN sys_schedule.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for sys_social_details
-- ----------------------------
DROP TABLE IF EXISTS sys_social_details;
CREATE TABLE sys_social_details (
  id bigint NOT NULL,
  type varchar(16) DEFAULT NULL,
  remark varchar(64) DEFAULT NULL,
  app_id varchar(64) DEFAULT NULL,
  app_secret varchar(1024) DEFAULT NULL,
  redirect_url varchar(128) DEFAULT NULL,
  ext varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT now(),
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_social_details IS '系统社交登录账号表';
COMMENT ON COLUMN sys_social_details.id IS '主键';
COMMENT ON COLUMN sys_social_details.type IS '社交登录类型';
COMMENT ON COLUMN sys_social_details.remark IS '备注';
COMMENT ON COLUMN sys_social_details.app_id IS '应用ID';
COMMENT ON COLUMN sys_social_details.app_secret IS '应用密钥';
COMMENT ON COLUMN sys_social_details.redirect_url IS '回调地址';
COMMENT ON COLUMN sys_social_details.ext IS '拓展字段';
COMMENT ON COLUMN sys_social_details.create_by IS '创建人';
COMMENT ON COLUMN sys_social_details.update_by IS '修改人';
COMMENT ON COLUMN sys_social_details.create_time IS '创建时间';
COMMENT ON COLUMN sys_social_details.update_time IS '更新时间';
COMMENT ON COLUMN sys_social_details.del_flag IS '删除标记，0未删除，1已删除';

-- ----------------------------
-- Table structure for sys_clarity_data
-- ----------------------------
DROP TABLE IF EXISTS sys_clarity_data;
CREATE TABLE sys_clarity_data (
  id bigint NOT NULL,
  data_date date NOT NULL,
  total_sessions integer DEFAULT NULL,
  distinct_users integer DEFAULT NULL,
  pages_per_session decimal(10,2) DEFAULT NULL,
  scroll_depth decimal(10,2) DEFAULT NULL,
  dead_click_rate decimal(10,2) DEFAULT NULL,
  rage_click_rate decimal(10,2) DEFAULT NULL,
  device_data text DEFAULT NULL,
  top_urls text DEFAULT NULL,
  num_of_days smallint NOT NULL DEFAULT 1,
  fetch_status varchar(10) NOT NULL DEFAULT 'pending',
  referrer_url_data text DEFAULT NULL,
  page_title_data text DEFAULT NULL,
  browser_data text DEFAULT NULL,
  create_by varchar(64) DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_clarity_data IS 'Clarity 监控数据缓存表';
COMMENT ON COLUMN sys_clarity_data.id IS '主键（ASSIGN_ID，应用层雪花算法生成）';
COMMENT ON COLUMN sys_clarity_data.data_date IS '数据日期';
COMMENT ON COLUMN sys_clarity_data.total_sessions IS '总会话数（Traffic.totalSessionCount）';
COMMENT ON COLUMN sys_clarity_data.distinct_users IS '独立访客数 UV（Traffic.distinctUserCount）';
COMMENT ON COLUMN sys_clarity_data.pages_per_session IS '每会话页面数（Traffic.pagesPerSessionPercentage）';
COMMENT ON COLUMN sys_clarity_data.scroll_depth IS '平均滚动深度 %（ScrollDepth.averageScrollDepth）';
COMMENT ON COLUMN sys_clarity_data.dead_click_rate IS '死点击率 %（DeadClickCount.sessionsWithMetricPercentage）';
COMMENT ON COLUMN sys_clarity_data.rage_click_rate IS '激怒点击率 %（RageClickCount.sessionsWithMetricPercentage）';
COMMENT ON COLUMN sys_clarity_data.device_data IS '设备分布 JSON 文本：[{"name":"PC","value":569}]';
COMMENT ON COLUMN sys_clarity_data.top_urls IS '热门页面 Top10 JSON 文本：[{"name":"https://...","value":297}]';
COMMENT ON COLUMN sys_clarity_data.num_of_days IS '数据天数';
COMMENT ON COLUMN sys_clarity_data.fetch_status IS '拉取状态：pending=占位中, success=成功, failed=失败';
COMMENT ON COLUMN sys_clarity_data.referrer_url_data IS '来源页面 JSON 文本：[{"name":"https://...","value":69}]';
COMMENT ON COLUMN sys_clarity_data.page_title_data IS '页面标题 JSON 文本：[{"name":"标题","value":24}]';
COMMENT ON COLUMN sys_clarity_data.browser_data IS '浏览器分布 JSON 文本：[{"name":"Chrome","value":108}]';
COMMENT ON COLUMN sys_clarity_data.create_by IS '创建人';
COMMENT ON COLUMN sys_clarity_data.update_by IS '修改人';
COMMENT ON COLUMN sys_clarity_data.del_flag IS '删除标记：0正常，1已删除';
COMMENT ON COLUMN sys_clarity_data.create_time IS '创建时间';
COMMENT ON COLUMN sys_clarity_data.update_time IS '更新时间';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  user_id bigint NOT NULL,
  username varchar(64) DEFAULT NULL,
  password varchar(255) DEFAULT NULL,
  salt varchar(255) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  nickname varchar(64) DEFAULT NULL,
  name varchar(64) DEFAULT NULL,
  email varchar(128) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT NULL,
  lock_flag char(1) DEFAULT '0',
  password_expire_flag char(1) DEFAULT '0',
  password_modify_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  wx_openid varchar(32) DEFAULT NULL,
  mini_openid varchar(32) DEFAULT NULL,
  qq_openid varchar(32) DEFAULT NULL,
  gitee_login varchar(100) DEFAULT NULL,
  osc_id varchar(100) DEFAULT NULL,
  wx_cp_userid varchar(100) DEFAULT NULL,
  wx_ding_userid varchar(100) DEFAULT NULL,
  dept_id bigint DEFAULT NULL,
  PRIMARY KEY (user_id)
);
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.user_id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.salt IS '盐值';
COMMENT ON COLUMN sys_user.phone IS '电话号码';
COMMENT ON COLUMN sys_user.avatar IS '头像';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.name IS '姓名';
COMMENT ON COLUMN sys_user.email IS '邮箱地址';
COMMENT ON COLUMN sys_user.create_by IS '创建人';
COMMENT ON COLUMN sys_user.update_by IS '修改人';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '修改时间';
COMMENT ON COLUMN sys_user.lock_flag IS '锁定标记，0未锁定，9已锁定';
COMMENT ON COLUMN sys_user.password_expire_flag IS '密码是否过期，0未过期，9已过期';
COMMENT ON COLUMN sys_user.password_modify_time IS '修改时间';
COMMENT ON COLUMN sys_user.del_flag IS '删除标记，0未删除，1已删除';
COMMENT ON COLUMN sys_user.wx_openid IS '微信登录openId';
COMMENT ON COLUMN sys_user.mini_openid IS '小程序openId';
COMMENT ON COLUMN sys_user.qq_openid IS 'QQ openId';
COMMENT ON COLUMN sys_user.gitee_login IS '码云标识';
COMMENT ON COLUMN sys_user.osc_id IS '开源中国标识';
COMMENT ON COLUMN sys_user.wx_cp_userid IS '企业微信标识';
COMMENT ON COLUMN sys_user.wx_ding_userid IS '钉钉标识';
COMMENT ON COLUMN sys_user.dept_id IS '主部门ID';

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS sys_user_post;
CREATE TABLE sys_user_post (
  user_id bigint NOT NULL,
  post_id bigint NOT NULL,
  PRIMARY KEY (user_id,post_id)
);
COMMENT ON TABLE sys_user_post IS '用户与岗位关联表';
COMMENT ON COLUMN sys_user_post.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_post.post_id IS '岗位ID';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id bigint NOT NULL,
  role_id bigint NOT NULL,
  PRIMARY KEY (user_id,role_id)
);
COMMENT ON TABLE sys_user_role IS '用户角色表';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- ----------------------------
-- Table structure for sys_message
-- ----------------------------
DROP TABLE IF EXISTS sys_message;
CREATE TABLE sys_message (
  id bigint NOT NULL,
  category varchar(255) DEFAULT NULL,
  title varchar(255) DEFAULT NULL,
  content text,
  send_flag char(1) DEFAULT '0',
  all_flag char(1) DEFAULT '0',
  sort int NOT NULL DEFAULT '0',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  create_by varchar(32) DEFAULT NULL,
  update_by varchar(32) DEFAULT NULL,
  del_flag char(1) NOT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_message IS '站内信';
COMMENT ON COLUMN sys_message.id IS '主键';
COMMENT ON COLUMN sys_message.category IS '分类';
COMMENT ON COLUMN sys_message.title IS '标题';
COMMENT ON COLUMN sys_message.content IS '内容';
COMMENT ON COLUMN sys_message.send_flag IS '是否推送';
COMMENT ON COLUMN sys_message.all_flag IS '全部接受';
COMMENT ON COLUMN sys_message.sort IS '排序 （越大越在前）';
COMMENT ON COLUMN sys_message.create_time IS '创建时间';
COMMENT ON COLUMN sys_message.update_time IS '更新时间';
COMMENT ON COLUMN sys_message.create_by IS '创建人';
COMMENT ON COLUMN sys_message.update_by IS '更新人';
COMMENT ON COLUMN sys_message.del_flag IS '删除时间';

-- ----------------------------
-- Table structure for sys_message_relation
-- ----------------------------
DROP TABLE IF EXISTS sys_message_relation;
CREATE TABLE sys_message_relation (
  id bigint NOT NULL,
  msg_id bigint DEFAULT NULL,
  user_id bigint DEFAULT NULL,
  content text,
  read_flag char(1) DEFAULT '0',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  create_by varchar(32) DEFAULT NULL,
  update_by varchar(32) DEFAULT NULL,
  del_flag char(1) NOT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_message_relation IS '系统消息推送记录';
COMMENT ON COLUMN sys_message_relation.id IS '主键';
COMMENT ON COLUMN sys_message_relation.msg_id IS '消息ID';
COMMENT ON COLUMN sys_message_relation.user_id IS '接收人ID';
COMMENT ON COLUMN sys_message_relation.content IS '内容';
COMMENT ON COLUMN sys_message_relation.read_flag IS '已读（0否，1是）';
COMMENT ON COLUMN sys_message_relation.create_time IS '创建时间';
COMMENT ON COLUMN sys_message_relation.update_time IS '更新时间';
COMMENT ON COLUMN sys_message_relation.create_by IS '创建人';
COMMENT ON COLUMN sys_message_relation.update_by IS '更新人';
COMMENT ON COLUMN sys_message_relation.del_flag IS '删除时间';

-- ----------------------------
-- Table structure for sys_system_config
-- ----------------------------
DROP TABLE IF EXISTS sys_system_config;
CREATE TABLE sys_system_config (
  id bigint NOT NULL,
  config_type varchar(64) DEFAULT NULL,
  config_name varchar(255) DEFAULT NULL,
  config_key varchar(255) DEFAULT NULL,
  config_value text,
  config_status char(1) DEFAULT NULL,
  create_by varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE sys_system_config IS '系统配置';
COMMENT ON COLUMN sys_system_config.id IS '主键';
COMMENT ON COLUMN sys_system_config.config_type IS '配置类型';
COMMENT ON COLUMN sys_system_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_system_config.config_key IS '配置标识';
COMMENT ON COLUMN sys_system_config.config_value IS '配置值';
COMMENT ON COLUMN sys_system_config.config_status IS '开启状态';
COMMENT ON COLUMN sys_system_config.create_by IS '创建人';
COMMENT ON COLUMN sys_system_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_system_config.update_by IS '修改人';
COMMENT ON COLUMN sys_system_config.update_time IS '修改时间';
COMMENT ON COLUMN sys_system_config.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS sys_user_dept;
CREATE TABLE sys_user_dept (
  user_id bigint NOT NULL,
  dept_id bigint NOT NULL,
  PRIMARY KEY (user_id,dept_id)
);
COMMENT ON TABLE sys_user_dept IS '用户部门表';
COMMENT ON COLUMN sys_user_dept.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_dept.dept_id IS '角色ID';

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS sys_job;
CREATE TABLE sys_job (
  job_id bigint NOT NULL,
  job_name varchar(64) NOT NULL,
  job_group varchar(64) NOT NULL,
  job_order char(1) DEFAULT '1',
  job_type char(1) NOT NULL DEFAULT '1',
  execute_path varchar(500) DEFAULT NULL,
  class_name varchar(500) DEFAULT NULL,
  method_name varchar(500) DEFAULT NULL,
  method_params_value varchar(2000) DEFAULT NULL,
  cron_expression varchar(255) DEFAULT NULL,
  misfire_policy varchar(20) DEFAULT '3',
  job_status char(1) DEFAULT '0',
  job_execute_status char(1) DEFAULT '0',
  create_by varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT now(),
  update_by varchar(64) DEFAULT '',
  update_time timestamp DEFAULT now(),
  start_time timestamp NULL DEFAULT NULL,
  previous_time timestamp NULL DEFAULT NULL,
  next_time timestamp NULL DEFAULT NULL,
  remark varchar(500) DEFAULT '',
  PRIMARY KEY (job_id,job_name,job_group)
);
COMMENT ON TABLE sys_job IS '定时任务调度表';
COMMENT ON COLUMN sys_job.job_id IS '任务id';
COMMENT ON COLUMN sys_job.job_name IS '任务名称';
COMMENT ON COLUMN sys_job.job_group IS '任务组名';
COMMENT ON COLUMN sys_job.job_order IS '组内执行顺利，值越大执行优先级越高，最大值9，最小值1';
COMMENT ON COLUMN sys_job.job_type IS '1、java类;2、spring bean名称;3、rest调用;4、jar调用;9其他';
COMMENT ON COLUMN sys_job.execute_path IS 'job_type=3时，rest调用地址，仅支持rest get协议,需要增加String返回值，0成功，1失败;job_type=4时，jar路径;其它值为空';
COMMENT ON COLUMN sys_job.class_name IS 'job_type=1时，类完整路径;job_type=2时，spring bean名称;其它值为空';
COMMENT ON COLUMN sys_job.method_name IS '任务方法';
COMMENT ON COLUMN sys_job.method_params_value IS '参数值';
COMMENT ON COLUMN sys_job.cron_expression IS 'cron执行表达式';
COMMENT ON COLUMN sys_job.misfire_policy IS '错失执行策略（1错失周期立即执行 2错失周期执行一次 3下周期执行）';
COMMENT ON COLUMN sys_job.job_status IS '状态（1、未发布;2、运行中;3、暂停;4、删除;）';
COMMENT ON COLUMN sys_job.job_execute_status IS '状态（0正常 1异常）';
COMMENT ON COLUMN sys_job.create_by IS '创建者';
COMMENT ON COLUMN sys_job.create_time IS '创建时间';
COMMENT ON COLUMN sys_job.update_by IS '更新者';
COMMENT ON COLUMN sys_job.update_time IS '更新时间';
COMMENT ON COLUMN sys_job.start_time IS '初次执行时间';
COMMENT ON COLUMN sys_job.previous_time IS '上次执行时间';
COMMENT ON COLUMN sys_job.next_time IS '下次执行时间';
COMMENT ON COLUMN sys_job.remark IS '备注信息';

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS sys_job_log;
CREATE TABLE sys_job_log (
  job_log_id bigint NOT NULL,
  job_id bigint NOT NULL,
  job_name varchar(64) DEFAULT NULL,
  job_group varchar(64) DEFAULT NULL,
  job_order char(1) DEFAULT NULL,
  job_type char(1) NOT NULL DEFAULT '1',
  execute_path varchar(500) DEFAULT NULL,
  class_name varchar(500) DEFAULT NULL,
  method_name varchar(500) DEFAULT NULL,
  method_params_value varchar(2000) DEFAULT NULL,
  cron_expression varchar(255) DEFAULT NULL,
  job_message varchar(500) DEFAULT NULL,
  job_log_status char(1) DEFAULT '0',
  execute_time varchar(30) DEFAULT NULL,
  exception_info varchar(2000) DEFAULT '',
  scheduled_fire_time timestamp DEFAULT NULL,
  fire_instance_id varchar(128) DEFAULT NULL,
  dedup_status char(1) DEFAULT '0',
  create_time timestamp DEFAULT now(),
  PRIMARY KEY (job_log_id)
);
COMMENT ON TABLE sys_job_log IS '定时任务执行日志表';
COMMENT ON COLUMN sys_job_log.job_log_id IS '任务日志ID';
COMMENT ON COLUMN sys_job_log.job_id IS '任务id';
COMMENT ON COLUMN sys_job_log.job_name IS '任务名称';
COMMENT ON COLUMN sys_job_log.job_group IS '任务组名';
COMMENT ON COLUMN sys_job_log.job_order IS '组内执行顺利，值越大执行优先级越高，最大值9，最小值1';
COMMENT ON COLUMN sys_job_log.job_type IS '1、java类;2、spring bean名称;3、rest调用;4、jar调用;9其他';
COMMENT ON COLUMN sys_job_log.execute_path IS 'job_type=3时，rest调用地址，仅支持post协议;job_type=4时，jar路径;其它值为空';
COMMENT ON COLUMN sys_job_log.class_name IS 'job_type=1时，类完整路径;job_type=2时，spring bean名称;其它值为空';
COMMENT ON COLUMN sys_job_log.method_name IS '任务方法';
COMMENT ON COLUMN sys_job_log.method_params_value IS '参数值';
COMMENT ON COLUMN sys_job_log.cron_expression IS 'cron执行表达式';
COMMENT ON COLUMN sys_job_log.job_message IS '日志信息';
COMMENT ON COLUMN sys_job_log.job_log_status IS '执行状态（0正常 1失败）';
COMMENT ON COLUMN sys_job_log.execute_time IS '执行时间';
COMMENT ON COLUMN sys_job_log.exception_info IS '异常信息';
COMMENT ON COLUMN sys_job_log.scheduled_fire_time IS '计划触发时间';
COMMENT ON COLUMN sys_job_log.fire_instance_id IS 'Quartz触发实例ID';
COMMENT ON COLUMN sys_job_log.dedup_status IS '去重状态（0正常执行 1同一触发点重复 2任务运行中跳过）';
COMMENT ON COLUMN sys_job_log.create_time IS '创建时间';

-- ----------------------------
-- Table structure for gen_datasource_conf
-- ----------------------------
DROP TABLE IF EXISTS gen_datasource_conf;
CREATE TABLE gen_datasource_conf (
  id bigint NOT NULL,
  name varchar(64) DEFAULT NULL,
  url text NOT NULL,
  username varchar(64) DEFAULT NULL,
  password varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT now(),
  del_flag char(1) DEFAULT '0',
  ds_type varchar(64) DEFAULT NULL,
  conf_type char(1) DEFAULT NULL,
  ds_name varchar(64) DEFAULT NULL,
  instance varchar(64) DEFAULT NULL,
  port int DEFAULT NULL,
  host varchar(128) DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE gen_datasource_conf IS '数据源表';
COMMENT ON COLUMN gen_datasource_conf.id IS '主键';
COMMENT ON COLUMN gen_datasource_conf.name IS '别名';
COMMENT ON COLUMN gen_datasource_conf.url IS 'jdbcurl';
COMMENT ON COLUMN gen_datasource_conf.username IS '用户名';
COMMENT ON COLUMN gen_datasource_conf.password IS '密码';
COMMENT ON COLUMN gen_datasource_conf.create_time IS '创建时间';
COMMENT ON COLUMN gen_datasource_conf.update_time IS '更新';
COMMENT ON COLUMN gen_datasource_conf.del_flag IS '删除标记';
COMMENT ON COLUMN gen_datasource_conf.ds_type IS '数据库类型';
COMMENT ON COLUMN gen_datasource_conf.conf_type IS '配置类型';
COMMENT ON COLUMN gen_datasource_conf.ds_name IS '数据库名称';
COMMENT ON COLUMN gen_datasource_conf.instance IS '实例';
COMMENT ON COLUMN gen_datasource_conf.port IS '端口';
COMMENT ON COLUMN gen_datasource_conf.host IS '主机';

-- ----------------------------
-- Table structure for gen_field_type
-- ----------------------------
DROP TABLE IF EXISTS gen_field_type;
CREATE TABLE gen_field_type (
  id bigint NOT NULL,
  column_type varchar(200) DEFAULT NULL,
  attr_type varchar(200) DEFAULT NULL,
  package_name varchar(200) DEFAULT NULL,
  default_form_type varchar(64) DEFAULT NULL,
  default_query_form_type varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  create_by varchar(64) DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX column_type ON gen_field_type (column_type);
COMMENT ON TABLE gen_field_type IS '字段类型管理';
COMMENT ON COLUMN gen_field_type.id IS 'id';
COMMENT ON COLUMN gen_field_type.column_type IS '字段类型';
COMMENT ON COLUMN gen_field_type.attr_type IS '属性类型';
COMMENT ON COLUMN gen_field_type.package_name IS '属性包名';
COMMENT ON COLUMN gen_field_type.default_form_type IS '默认表单类型';
COMMENT ON COLUMN gen_field_type.default_query_form_type IS '默认查询表单类型';
COMMENT ON COLUMN gen_field_type.create_time IS '创建时间';
COMMENT ON COLUMN gen_field_type.create_by IS '创建人';
COMMENT ON COLUMN gen_field_type.update_time IS '修改时间';
COMMENT ON COLUMN gen_field_type.update_by IS '修改人';
COMMENT ON COLUMN gen_field_type.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for gen_form_conf
-- ----------------------------
DROP TABLE IF EXISTS gen_form_conf;
CREATE TABLE gen_form_conf (
  id bigint NOT NULL,
  ds_name varchar(64) DEFAULT NULL,
  table_name varchar(64) DEFAULT NULL,
  form_info text NOT NULL,
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT now(),
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
CREATE INDEX table_name ON gen_form_conf (table_name);
COMMENT ON TABLE gen_form_conf IS '表单配置';
COMMENT ON COLUMN gen_form_conf.id IS 'ID';
COMMENT ON COLUMN gen_form_conf.ds_name IS '数据库名称';
COMMENT ON COLUMN gen_form_conf.table_name IS '表名称';
COMMENT ON COLUMN gen_form_conf.form_info IS '表单信息';
COMMENT ON COLUMN gen_form_conf.create_time IS '创建时间';
COMMENT ON COLUMN gen_form_conf.update_time IS '修改时间';

-- ----------------------------
-- Table structure for gen_group
-- ----------------------------
DROP TABLE IF EXISTS gen_group;
CREATE TABLE gen_group (
  id bigint NOT NULL,
  group_name varchar(255) DEFAULT NULL,
  group_desc varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  create_time timestamp DEFAULT NULL,
  update_time timestamp DEFAULT NULL,
  del_flag char(1) DEFAULT '0',
  PRIMARY KEY (id)
);
COMMENT ON TABLE gen_group IS '模板分组';
COMMENT ON COLUMN gen_group.group_name IS '分组名称';
COMMENT ON COLUMN gen_group.group_desc IS '分组描述';
COMMENT ON COLUMN gen_group.create_by IS '创建人';
COMMENT ON COLUMN gen_group.update_by IS '修改人';
COMMENT ON COLUMN gen_group.create_time IS '创建人';
COMMENT ON COLUMN gen_group.update_time IS '修改人';
COMMENT ON COLUMN gen_group.del_flag IS '删除标记';

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS gen_table;
CREATE TABLE gen_table (
  id bigint NOT NULL,
  table_name varchar(200) DEFAULT NULL,
  class_name varchar(200) DEFAULT NULL,
  db_type varchar(200) DEFAULT NULL,
  table_comment varchar(200) DEFAULT NULL,
  author varchar(200) DEFAULT NULL,
  email varchar(200) DEFAULT NULL,
  package_name varchar(200) DEFAULT NULL,
  version varchar(200) DEFAULT NULL,
  i18n char(1) DEFAULT '0',
  style bigint DEFAULT NULL,
  sync_menu_id bigint DEFAULT NULL,
  sync_route char(1) DEFAULT '0',
  child_table_name varchar(200) DEFAULT NULL,
  main_field varchar(200) DEFAULT NULL,
  child_field varchar(200) DEFAULT NULL,
  parent_field varchar(200) DEFAULT NULL,
  name_field varchar(200) DEFAULT NULL,
  generator_type char(1) DEFAULT '0',
  backend_path varchar(500) DEFAULT NULL,
  frontend_path varchar(500) DEFAULT NULL,
  module_name varchar(200) DEFAULT NULL,
  function_name varchar(200) DEFAULT NULL,
  form_layout smallint DEFAULT NULL,
  ds_name varchar(200) DEFAULT NULL,
  baseclass_id bigint DEFAULT NULL,
  create_time timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX table_name_idx ON gen_table (table_name,ds_name);
COMMENT ON TABLE gen_table IS '代码生成表';
COMMENT ON COLUMN gen_table.id IS 'id';
COMMENT ON COLUMN gen_table.table_name IS '表名';
COMMENT ON COLUMN gen_table.class_name IS '类名';
COMMENT ON COLUMN gen_table.db_type IS '数据库类型';
COMMENT ON COLUMN gen_table.table_comment IS '说明';
COMMENT ON COLUMN gen_table.author IS '作者';
COMMENT ON COLUMN gen_table.email IS '邮箱';
COMMENT ON COLUMN gen_table.package_name IS '项目包名';
COMMENT ON COLUMN gen_table.version IS '项目版本号';
COMMENT ON COLUMN gen_table.i18n IS '是否生成带有i18n 0 不带有 1带有';
COMMENT ON COLUMN gen_table.style IS '代码风格';
COMMENT ON COLUMN gen_table.sync_menu_id IS '所属菜单ID';
COMMENT ON COLUMN gen_table.sync_route IS '是否自动同步路由';
COMMENT ON COLUMN gen_table.child_table_name IS '子表名称';
COMMENT ON COLUMN gen_table.main_field IS '主表关联键';
COMMENT ON COLUMN gen_table.child_field IS '子表关联键';
COMMENT ON COLUMN gen_table.parent_field IS '父表关联键';
COMMENT ON COLUMN gen_table.name_field IS '名称字段';
COMMENT ON COLUMN gen_table.generator_type IS '生成方式  0：zip压缩包   1：自定义目录';
COMMENT ON COLUMN gen_table.backend_path IS '后端生成路径';
COMMENT ON COLUMN gen_table.frontend_path IS '前端生成路径';
COMMENT ON COLUMN gen_table.module_name IS '模块名';
COMMENT ON COLUMN gen_table.function_name IS '功能名';
COMMENT ON COLUMN gen_table.form_layout IS '表单布局  1：一列   2：两列';
COMMENT ON COLUMN gen_table.ds_name IS '数据源ID';
COMMENT ON COLUMN gen_table.baseclass_id IS '基类ID';
COMMENT ON COLUMN gen_table.create_time IS '创建时间';

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS gen_table_column;
CREATE TABLE gen_table_column (
  id bigint NOT NULL,
  ds_name varchar(200) DEFAULT NULL,
  table_name varchar(200) DEFAULT NULL,
  field_name varchar(200) DEFAULT NULL,
  field_type varchar(200) DEFAULT NULL,
  field_comment varchar(200) DEFAULT NULL,
  attr_name varchar(200) DEFAULT NULL,
  attr_type varchar(200) DEFAULT NULL,
  package_name varchar(200) DEFAULT NULL,
  sort int DEFAULT NULL,
  auto_fill varchar(20) DEFAULT NULL,
  primary_pk char(1) DEFAULT '0',
  base_field char(1) DEFAULT '0',
  form_item char(1) DEFAULT '0',
  form_required char(1) DEFAULT '0',
  form_type varchar(200) DEFAULT NULL,
  form_validator varchar(200) DEFAULT NULL,
  grid_item char(1) DEFAULT '0',
  grid_sort char(1) DEFAULT '0',
  query_item char(1) DEFAULT '0',
  query_type varchar(200) DEFAULT NULL,
  query_form_type varchar(200) DEFAULT NULL,
  field_dict varchar(200) DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE gen_table_column IS '代码生成表字段';
COMMENT ON COLUMN gen_table_column.id IS 'id';
COMMENT ON COLUMN gen_table_column.ds_name IS '数据源名称';
COMMENT ON COLUMN gen_table_column.table_name IS '表名称';
COMMENT ON COLUMN gen_table_column.field_name IS '字段名称';
COMMENT ON COLUMN gen_table_column.field_type IS '字段类型';
COMMENT ON COLUMN gen_table_column.field_comment IS '字段说明';
COMMENT ON COLUMN gen_table_column.attr_name IS '属性名';
COMMENT ON COLUMN gen_table_column.attr_type IS '属性类型';
COMMENT ON COLUMN gen_table_column.package_name IS '属性包名';
COMMENT ON COLUMN gen_table_column.sort IS '排序';
COMMENT ON COLUMN gen_table_column.auto_fill IS '自动填充  DEFAULT、INSERT、UPDATE、INSERT_UPDATE';
COMMENT ON COLUMN gen_table_column.primary_pk IS '主键 0：否  1：是';
COMMENT ON COLUMN gen_table_column.base_field IS '基类字段 0：否  1：是';
COMMENT ON COLUMN gen_table_column.form_item IS '表单项 0：否  1：是';
COMMENT ON COLUMN gen_table_column.form_required IS '表单必填 0：否  1：是';
COMMENT ON COLUMN gen_table_column.form_type IS '表单类型';
COMMENT ON COLUMN gen_table_column.form_validator IS '表单效验';
COMMENT ON COLUMN gen_table_column.grid_item IS '列表项 0：否  1：是';
COMMENT ON COLUMN gen_table_column.grid_sort IS '列表排序 0：否  1：是';
COMMENT ON COLUMN gen_table_column.query_item IS '查询项 0：否  1：是';
COMMENT ON COLUMN gen_table_column.query_type IS '查询方式';
COMMENT ON COLUMN gen_table_column.query_form_type IS '查询表单类型';
COMMENT ON COLUMN gen_table_column.field_dict IS '字典类型';

-- ----------------------------
-- Table structure for gen_template
-- ----------------------------
DROP TABLE IF EXISTS gen_template;
CREATE TABLE gen_template (
  id bigint NOT NULL,
  template_name varchar(255) NOT NULL,
  generator_path varchar(255) NOT NULL,
  template_desc varchar(255) NOT NULL,
  template_code text NOT NULL,
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT now(),
  del_flag char(1) NOT NULL DEFAULT '0',
  create_by varchar(64) DEFAULT ' ',
  update_by varchar(64) DEFAULT ' ',
  PRIMARY KEY (id)
);
COMMENT ON TABLE gen_template IS '模板';
COMMENT ON COLUMN gen_template.id IS '主键';
COMMENT ON COLUMN gen_template.template_name IS '模板名称';
COMMENT ON COLUMN gen_template.generator_path IS '模板路径';
COMMENT ON COLUMN gen_template.template_desc IS '模板描述';
COMMENT ON COLUMN gen_template.template_code IS '模板代码';
COMMENT ON COLUMN gen_template.create_time IS '创建时间';
COMMENT ON COLUMN gen_template.update_time IS '更新';
COMMENT ON COLUMN gen_template.del_flag IS '删除标记';
COMMENT ON COLUMN gen_template.create_by IS '创建人';
COMMENT ON COLUMN gen_template.update_by IS '修改人';

-- ----------------------------
-- Table structure for gen_template_group
-- ----------------------------
DROP TABLE IF EXISTS gen_template_group;
CREATE TABLE gen_template_group (
  group_id bigint NOT NULL,
  template_id bigint NOT NULL,
  PRIMARY KEY (group_id,template_id)
);
COMMENT ON TABLE gen_template_group IS '模板分组关联表';
COMMENT ON COLUMN gen_template_group.group_id IS '分组id';
COMMENT ON COLUMN gen_template_group.template_id IS '模板id';

-- ----------------------------
-- Table structure for gen_create_table
-- ----------------------------
DROP TABLE IF EXISTS gen_create_table;
CREATE TABLE gen_create_table (
  id bigint NOT NULL,
  table_name varchar(32) NOT NULL,
  ds_name varchar(32) DEFAULT NULL,
  comments varchar(512) DEFAULT NULL,
  create_by varchar(64) DEFAULT NULL,
  update_by varchar(64) DEFAULT NULL,
  create_time timestamp DEFAULT now(),
  update_time timestamp DEFAULT NULL,
  column_info text NOT NULL,
  del_flag char(1) DEFAULT NULL,
  PRIMARY KEY (id)
);
COMMENT ON TABLE gen_create_table IS '自动创建表管理';
COMMENT ON COLUMN gen_create_table.id IS '主键ID';
COMMENT ON COLUMN gen_create_table.table_name IS '表名称';
COMMENT ON COLUMN gen_create_table.ds_name IS '数据源';
COMMENT ON COLUMN gen_create_table.comments IS '表注释';
COMMENT ON COLUMN gen_create_table.create_by IS '创建人';
COMMENT ON COLUMN gen_create_table.create_time IS '创建时间';
COMMENT ON COLUMN gen_create_table.column_info IS '字段信息';
COMMENT ON COLUMN gen_create_table.del_flag IS '删除标记';

