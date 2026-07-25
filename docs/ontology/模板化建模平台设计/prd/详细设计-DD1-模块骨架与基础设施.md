# 详细设计-DD1-模块骨架与基础设施（M0）

| 项 | 内容 |
|---|---|
| 文档名称 | 详细设计-DD1-模块骨架与基础设施 |
| 里程碑 | M0（模块骨架） |
| 上游 PRD | 《本体模板化治理功能产品需求文档.md》v1.2 第 3.1/3.2/3.3 节、NFR-10/11、第十三节、十六节 M0 |
| 设计计划 | 《详细设计计划.md》DD1 |
| 编写日期 | 2026-07-25 |
| 文档状态 | 待实现 |

---

## 一、设计目标与范围

### 1.1 目标

从零搭建 `pig-ontology-biz` 模块骨架，使 youming 具备承载本体治理功能的基础设施，满足：

- 模块可独立编译（`mvn -pl pig-ontology/pig-ontology-biz -am compile` 通过）。
- pig-boot 单体可启动并加载该模块（9999 端口、`/admin` context-path）。
- 微服务形态下 pig-gateway 可路由到 pig-ontology（`/admin/ont/**`）。
- 治理表结构（V4）与菜单/权限/内置模板种子（V5）已建，pig-ui 登录后可见"本体治理"菜单。
- 提供一个健康检查接口，验证模块在两种形态下均可用。

### 1.2 范围（本 DD 做 / 不做）

| 做（M0） | 不做（后续 DD） |
|---|---|
| `pig-ontology-biz` pom + 包结构 + 启动类 | 属性模板业务逻辑（DD2） |
| `server/pom.xml` 注册模块 | 分类模板/编码/类树（DD3） |
| pig-boot 引入依赖 | 单位注册表（DD4） |
| pig-gateway 路由配置（微服务形态） | 注释属性注册表（DD5） |
| V4 治理表结构 DDL（8 张表全建，空表） | 参考本体浏览（DD6） |
| V5 种子：sys_menu（10000+ 段）+ 权限点 + 内置模板/单位/分类树种子（占位，最小集） | 供给接口业务实现（DD2 起逐步） |
| 健康检查接口 `/admin/ont/health` | 治理 UI 业务页面（DD2 起） |

> M0 建表策略说明：V4 一次性建齐 8 张治理表（PRD 9.1~9.8），避免后续里程碑频繁加表导致 Flyway 版本膨胀；表建为空表，业务数据由 V5 种子最小集 + 后续 DD 填充。V5 只放"菜单/权限/最小内置模板（满足 AC-1.1 的 12 个属性模板）"，分类树/单位/注释属性种子随各自 DD 的 V6+ 脚本补。

### 1.3 验收映射（M0 DoD）

| PRD AC / 要求 | 本 DD 实现点 |
|---|---|
| 模块可编译 | 4.1 pom + 4.2 包结构 |
| pig-boot 可启动 | 5.2 pig-boot pom 引依赖 |
| 表已建 | 6.1 V4 DDL |
| 菜单可见 | 6.2 V5 sys_menu 种子 |
| 双形态可用（NFR-10） | 5.3 gateway 路由 + 7.1 健康检查接口 |
| 模块隔离（NFR-11） | 4.1 依赖仅 pig-common-* |
| 权限点就绪（13.2） | 6.2 V5 权限标识 |

---

## 二、落地清单

### 2.1 后端文件清单

```
server/pig-ontology/
├── pom.xml                                      # 聚合 pom（packaging=pom，modules=pig-ontology-biz）
└── pig-ontology-biz/
    ├── pom.xml                                  # biz pom（jar，依赖 pig-common-*）
    └── src/main/
        ├── java/com/pig4cloud/pig/ontology/
        │   ├── PigOntologyApplication.java      # 启动类（微服务形态用）
        │   ├── controller/
        │   │   └── OntologyHealthController.java # 健康检查（M0 唯一接口）
        │   ├── service/                         # 空（后续 DD 填充）
        │   ├── mapper/                          # 空
        │   └── api/
        │       ├── entity/                      # 空（V4 建表，Entity 随 DD2 填）
        │       ├── dto/                         # 空
        │       └── vo/                          # 空
        └── resources/
            ├── application.yml                  # 微服务形态配置（Nacos）
            └── mapper/                          # 空（Mapper XML 随 DD 填）
```

### 2.2 数据库文件清单

```
server/pig-common/pig-common-data/src/main/resources/db/migration/
├── V1__init_schema.sql                          # 已有
├── V2__init_seed_data.sql                       # 已有
├── V3__init_quartz_tables.sql                   # 已有
├── V4__ont_governance_schema.sql                # 新增：8 张治理表 DDL
└── V5__ont_governance_seed.sql                  # 新增：sys_menu + 权限点 + 12 内置属性模板
```

### 2.3 配置文件清单

```
server/pom.xml                                   # 修改：modules 加 pig-ontology
server/pig-boot/pom.xml                          # 修改：依赖加 pig-ontology-biz
server/pig-gateway/src/main/resources/application.yml  # 修改：加 pig-ontology 路由
```

### 2.4 前端文件清单（M0 最小）

> M0 不建业务页面，仅验证菜单可见。前端业务页面从 DD2 起建。

```
（M0 无前端文件；sys_menu 种子指向的 views/ontology/xxx/index.vue 占位文件由 DD2 起创建）
```

> 注意：sys_menu 种子在 M0 写入后，若对应 vue 文件不存在，pig-ui 点菜单会 404。这是预期行为--M0 只验证"菜单数据已下发"，页面在 DD2 实现后即正常。若需 M0 即可点击，可临时建空 `views/ontology/property-template/index.vue` 占位（可选）。

---

## 三、设计依据（已核查的仓库现状）

| 依据 | 核查结论 | 来源 |
|---|---|---|
| pig-upms 模块结构 | api/biz 双模块，聚合 pom packaging=pom | `server/pig-upms/pom.xml` |
| 启动类注解范式 | `@EnableOpenApi`+`@EnablePigResourceServer`+`@EnableDiscoveryClient`+`@SpringBootApplication` | `PigAdminApplication.java` |
| pig-boot 聚合方式 | pom 依赖引入 biz jar + 默认 `com.pig4cloud.pig` 基包扫描，无 ComponentScan/MapperScan | `PigBootApplication.java` |
| pig-boot 配置 | 9999 端口、`/admin` context-path、禁用 Nacos、PG 数据源、Flyway enabled | `pig-boot/.../application.yml`+`application-dev.yml` |
| 网关路由 | yml 写死（`spring.cloud.gateway.server.webflux.routes`），非 Nacos 动态 | `pig-gateway/.../application.yml` |
| MyBatis-Plus | 自动装配（`MybatisPlusConfiguration`），分页 `POSTGRE_SQL`，逻辑删除 `@TableLogic`+`delFlag` String，审计自动填充 | `pig-common-data/.../mybatis/` |
| Flyway | `pig-common-data` 共享 `db/migration`，`baseline-on-migrate`、`placeholder-replacement:false` | `pig-boot/.../application-dev.yml` |
| sys_menu INSERT | 16 字段按位置 INSERT，menu_id 现有最大 9910 | `V2__init_seed_data.sql:213` |
| Spring Boot 版本 | 4.0.7（根 pom 重复声明，以生效值 4.0.7 为准） | `server/pom.xml` |

---

## 四、后端模块设计

### 4.1 `server/pig-ontology/pom.xml`（聚合 pom）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.pig4cloud</groupId>
        <artifactId>pig</artifactId>
        <version>4.0.0</version>
    </parent>

    <artifactId>pig-ontology</artifactId>
    <packaging>pom</packaging>
    <name>pig-ontology</name>
    <description>本体模板化治理功能（属性模板/分类模板/单位/注释属性/供给）</description>

    <modules>
        <module>pig-ontology-biz</module>
    </modules>
</project>
```

> 说明：本期仅 `pig-ontology-biz` 单模块（S-4）。未来若建模侧需 Feign，再增 `pig-ontology-api` 子模块并加入 `<modules>`。parent 指向 `com.pig4cloud:pig:4.0.0`（对齐 pig-upms）。

### 4.2 `server/pig-ontology/pig-ontology-biz/pom.xml`（biz pom）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.pig4cloud</groupId>
        <artifactId>pig-ontology</artifactId>
        <version>4.0.0</version>
    </parent>

    <artifactId>pig-ontology-biz</artifactId>
    <packaging>jar</packaging>
    <name>pig-ontology-biz</name>

    <dependencies>
        <!-- 数据层：MyBatis-Plus + Flyway + 审计填充 + 分页（PG） -->
        <dependency>
            <groupId>com.pig4cloud</groupId>
            <artifactId>pig-common-data</artifactId>
        </dependency>
        <!-- 核心：R 统一返回 + 权限 + 常量 -->
        <dependency>
            <groupId>com.pig4cloud</groupId>
            <artifactId>pig-common-core</artifactId>
        </dependency>
        <!-- 安全：@HasPermission + 资源服务器 -->
        <dependency>
            <groupId>com.pig4cloud</groupId>
            <artifactId>pig-common-security</artifactId>
        </dependency>
        <!-- 操作日志：@SysLog -->
        <dependency>
            <groupId>com.pig4cloud</groupId>
            <artifactId>pig-common-log</artifactId>
        </dependency>
        <!-- 接口文档：@EnableOpenApi -->
        <dependency>
            <groupId>com.pig4cloud</groupId>
            <artifactId>pig-common-swagger</artifactId>
        </dependency>
        <!-- 注册中心（微服务形态） -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <!-- 配置中心（微服务形态） -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <!-- MyBatis-Plus Spring Boot 4 starter -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <!-- 数据库连接池 + PG 驱动（由 pig-common-data 传递，显式声明保险） -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <!-- Web（单体/微服务均需） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- 微服务形态：repackage 成可执行 jar -->
        <profile>
            <id>cloud</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <finalName>${project.artifactId}</finalName>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>repackage</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <!-- 单体形态：不 repackage，作为 pig-boot 依赖 -->
        <profile>
            <id>boot</id>
        </profile>
    </profiles>
</project>
```

> 说明：依赖范围与版本由根 pom 的 `<dependencyManagement>` 管理（对齐 pig-upms-biz）。不依赖 pig-upms/visual，满足 NFR-11 模块隔离。`cloud`/`boot` 双 profile 对齐 pig-upms-biz。

### 4.3 启动类 `PigOntologyApplication.java`

```java
package com.pig4cloud.pig.ontology;

import com.pig4cloud.pig.common.core.annotation.EnableOpenApi;
import com.pig4cloud.pig.common.security.annotation.EnablePigResourceServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 本体模板化治理功能启动类（微服务形态）。
 * <p>
 * 单体形态下本类不启用，由 {@code PigBootApplication} 聚合本模块 jar，
 * 靠默认 {@code com.pig4cloud.pig} 基包扫描装载本模块组件。
 */
@EnableOpenApi("ontology")
@EnablePigResourceServer
@EnableDiscoveryClient
@SpringBootApplication
public class PigOntologyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PigOntologyApplication.class, args);
    }
}
```

> 说明：注解对齐 `PigAdminApplication`。单体模式（pig-boot）下 `spring.cloud.discovery.enabled=false`，`@EnableDiscoveryClient` 自动失效，无副作用。

### 4.4 健康检查 `OntologyHealthController.java`（M0 唯一接口）

```java
package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本体治理模块健康检查（M0 验证用）。
 * 后续里程碑的业务 Controller 随各 DD 实现。
 */
@RestController
@RequestMapping("/ont")
@Tag(name = "本体治理-健康检查", description = "模块可用性验证")
@SecurityRequirement(name = "Authorization")
public class OntologyHealthController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "验证 pig-ontology 模块在当前形态下可用")
    public R<String> health() {
        return R.ok("pig-ontology is up");
    }
}
```

> 说明：路径 `/ont/health`，经 context-path `/admin`（单体）或网关路由（微服务）后对外为 `/admin/ont/health`。M0 暂不加 `@HasPermission`，便于裸验证；后续业务接口按 13.2 加权限点。

---

## 五、模块挂载与配置

### 5.1 `server/pom.xml` 注册模块

在根 pom 的 `<modules>` 块追加（位置：现有 modules 列表末尾，pig-visual 之后）：

```xml
<modules>
    <module>pig-register</module>
    <module>pig-gateway</module>
    <module>pig-auth</module>
    <module>pig-upms</module>
    <module>pig-common</module>
    <module>pig-visual</module>
    <module>pig-ontology</module>   <!-- 新增：本体模板化治理 -->
</modules>
```

> 说明：pig-ontology 加入默认 modules（cloud profile），微服务形态会编译它。`boot` profile 追加 pig-boot 模块（已存在机制）。

### 5.2 `server/pig-boot/pom.xml` 引依赖

在 pig-boot 的 `<dependencies>` 追加（位置：pig-quartz 之后）：

```xml
<dependency>
    <groupId>com.pig4cloud</groupId>
    <artifactId>pig-ontology-biz</artifactId>
    <version>${project.version}</version>
</dependency>
```

> 说明：pig-boot 靠 pom 依赖聚合 biz jar + 默认 `com.pig4cloud.pig` 基包扫描（`PigBootApplication` 无 ComponentScan）。`com.pig4cloud.pig.ontology` 在基包内，自动被扫描，无需额外配置。

### 5.3 `server/pig-gateway/src/main/resources/application.yml` 加路由

在 `spring.cloud.gateway.server.webflux.routes` 列表追加（对齐 pig-upms-biz 路由写法）：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            # ... 现有路由 ...
            - id: pig-ontology
              uri: lb://pig-ontology-biz
              predicates:
                - Path=/admin/ont/**
              metadata:
                response-timeout: 30000
```

> 说明：微服务形态下 `/admin/ont/**` 经网关路由到 `pig-ontology-biz` 服务。单体形态下不走网关，由 pig-boot context-path `/admin` 直接收敛到 `/admin/ont/**`。服务名 `pig-ontology-biz`（对齐 artifactId，由 `spring.application.name` 配置，见 5.4）。

### 5.4 `pig-ontology-biz/src/main/resources/application.yml`（微服务形态配置）

```yaml
# 微服务形态配置；单体形态由 pig-boot 的 application.yml 覆盖
spring:
  application:
    name: pig-ontology-biz
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_HOST:pig-register}:${NACOS_PORT:8848}
      config:
        server-addr: ${NACOS_HOST:pig-register}:${NACOS_PORT:8848}
    # 避免与 MVC 冲突（对齐 pig-upms-biz）
    gateway:
      server:
        webflux:
          enabled: false
  config:
    import:
      - optional:nacos:application-dev.yml
      - optional:nacos:${spring.application.name}-dev.yml
```

> 说明：对齐 pig-upms-biz 的 `application.yml`。端口由 Nacos 的 `pig-ontology-biz-dev.yml` 下发（M0 可不配，默认 0 随机端口即可，或运维侧配固定端口如 4003）。单体形态下本文件不生效（pig-boot 的 `spring.cloud.nacos.config.enabled=false`）。

---

## 六、数据库设计（V4 + V5）

### 6.1 `V4__ont_governance_schema.sql`（8 张治理表 DDL）

> 一次性建齐 PRD 9.1~9.8 的 8 张表，空表。字段类型遵循 3.2 约定（char(1) 布尔、timestamp 时间、varchar(64) 审计、bigint 主键）。所有表加 `COMMENT ON`。

```sql
-- ============================================================
-- V4__ont_governance_schema.sql
-- 本体模板化治理功能表结构（8 张表）
-- 对应 PRD v1.2 第九节 9.1~9.8
-- ============================================================

-- 9.1 属性模板（数据属性 + 对象属性，同构）
CREATE TABLE ont_property_template (
    id                  bigint       NOT NULL,
    template_code       varchar(64)  NOT NULL,
    kind                varchar(16)  NOT NULL,
    label               varchar(128) NOT NULL,
    description         varchar(512),
    category            varchar(64),
    type                varchar(32),
    is_identifier       char(1)      DEFAULT '0',
    unit_ref            varchar(255),
    values              text,
    default_cardinality varchar(32),
    source              varchar(16)  NOT NULL DEFAULT 'custom',
    deprecated          char(1)      DEFAULT '0',
    create_by           varchar(64)  DEFAULT ' ',
    create_time         timestamp    DEFAULT now(),
    update_by           varchar(64)  DEFAULT ' ',
    update_time         timestamp    DEFAULT now(),
    del_flag            char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_property_template PRIMARY KEY (id),
    CONSTRAINT uk_ont_prop_tpl_code UNIQUE (template_code)
);
COMMENT ON TABLE  ont_property_template IS '属性模板（数据属性+对象属性同构）';
COMMENT ON COLUMN ont_property_template.template_code       IS '唯一标识，如 name/contains/price';
COMMENT ON COLUMN ont_property_template.kind                IS 'datatype / object';
COMMENT ON COLUMN ont_property_template.category            IS '分组：basic/contact/monetary/temporal/status/containment/attribution';
COMMENT ON COLUMN ont_property_template.type                IS 'datatype 专属：string/integer/decimal/boolean/datetime';
COMMENT ON COLUMN ont_property_template.is_identifier       IS '0/1，datatype 专属标识符';
COMMENT ON COLUMN ont_property_template.unit_ref            IS '预设 QUDT 单位 IRI（FR-3 交汇）';
COMMENT ON COLUMN ont_property_template.values              IS '枚举值（逗号分隔），datatype 专属';
COMMENT ON COLUMN ont_property_template.default_cardinality IS 'object 专属：one-to-many/many-to-one/...';
COMMENT ON COLUMN ont_property_template.source              IS 'builtin / custom';
COMMENT ON COLUMN ont_property_template.deprecated          IS '0/1 弃用标记';

-- 9.2 分类模板（外观+骨架+父子继承+编码，融合表）
CREATE TABLE ont_class_template (
    id                   bigint       NOT NULL,
    template_code        varchar(64)  NOT NULL,
    classification_code  varchar(64)  NOT NULL,
    label                varchar(128) NOT NULL,
    label_cn             varchar(128),
    description          varchar(512),
    parent_id            bigint,
    tree_root            varchar(64)  NOT NULL,
    icon                 varchar(64),
    color                varchar(16),
    inherit_appearance   char(1)      DEFAULT '1',
    source               varchar(16)  NOT NULL DEFAULT 'custom',
    source_ref           varchar(64),
    deprecated           char(1)      DEFAULT '0',
    sort_order           int          DEFAULT 0,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_class_template PRIMARY KEY (id),
    CONSTRAINT uk_ont_class_tpl_code      UNIQUE (template_code),
    CONSTRAINT uk_ont_class_tpl_cls_code  UNIQUE (classification_code)
);
COMMENT ON TABLE  ont_class_template IS '分类模板（外观+骨架+父子继承+编码）';
COMMENT ON COLUMN ont_class_template.classification_code IS '规范分类编码，如 30-01-01（承载层级，FR-8）';
COMMENT ON COLUMN ont_class_template.parent_id           IS '父分类模板 id，NULL=根节点（FR-2 父子继承）';
COMMENT ON COLUMN ont_class_template.tree_root           IS '所属分类树标识，如 equipment';
COMMENT ON COLUMN ont_class_template.inherit_appearance  IS '0/1 是否继承父外观（默认 1）';
COMMENT ON COLUMN ont_class_template.source_ref          IS '来源本体标识，如 brick（FR-7 导入）';

-- 9.3 分类模板属性/关系引用（结构骨架）
CREATE TABLE ont_class_template_ref (
    id                      bigint      NOT NULL,
    class_template_id       bigint      NOT NULL,
    property_template_code  varchar(64) NOT NULL,
    ref_type                varchar(16) NOT NULL,
    sort_order              int         DEFAULT 0,
    inherit_flag            char(1)     DEFAULT '0',
    create_by               varchar(64) DEFAULT ' ',
    create_time             timestamp   DEFAULT now(),
    update_by               varchar(64) DEFAULT ' ',
    update_time             timestamp   DEFAULT now(),
    del_flag                char(1)     DEFAULT '0',
    CONSTRAINT pk_ont_class_template_ref PRIMARY KEY (id)
);
COMMENT ON TABLE  ont_class_template_ref IS '分类模板属性/关系引用（结构骨架）';
COMMENT ON COLUMN ont_class_template_ref.class_template_id      IS '-> ont_class_template.id';
COMMENT ON COLUMN ont_class_template_ref.property_template_code IS '-> ont_property_template.template_code';
COMMENT ON COLUMN ont_class_template_ref.ref_type               IS 'property / relationship';
COMMENT ON COLUMN ont_class_template_ref.inherit_flag           IS '0/1 继承自父 vs 本节点新增';

-- 9.4 量纲
CREATE TABLE ont_quantity_kind (
    id               bigint       NOT NULL,
    qudt_iri         varchar(255) NOT NULL,
    label            varchar(64)  NOT NULL,
    label_cn         varchar(64),
    dimension_vector varchar(32),
    sort_order       int          DEFAULT 0,
    create_by        varchar(64)  DEFAULT ' ',
    create_time      timestamp    DEFAULT now(),
    update_by        varchar(64)  DEFAULT ' ',
    update_time      timestamp    DEFAULT now(),
    del_flag         char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_quantity_kind PRIMARY KEY (id),
    CONSTRAINT uk_ont_qk_iri UNIQUE (qudt_iri)
);
COMMENT ON TABLE  ont_quantity_kind IS '量纲（引用 QUDT）';
COMMENT ON COLUMN ont_quantity_kind.qudt_iri         IS '如 .../quantitykind/Length';
COMMENT ON COLUMN ont_quantity_kind.dimension_vector IS 'A0E0L1I0M0H0T0D0';

-- 9.5 单位
CREATE TABLE ont_unit (
    id                    bigint       NOT NULL,
    qudt_iri              varchar(255) NOT NULL,
    symbol                varchar(32),
    label                 varchar(64)  NOT NULL,
    label_cn              varchar(64),
    quantity_kind_id      bigint       NOT NULL,
    conversion_multiplier numeric,
    conversion_offset     numeric,
    scaling_of            varchar(255),
    ucum_code             varchar(32),
    source                varchar(16)  NOT NULL DEFAULT 'custom',
    source_ref            varchar(64),
    deprecated            char(1)      DEFAULT '0',
    create_by             varchar(64)  DEFAULT ' ',
    create_time           timestamp    DEFAULT now(),
    update_by             varchar(64)  DEFAULT ' ',
    update_time           timestamp    DEFAULT now(),
    del_flag              char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_unit PRIMARY KEY (id),
    CONSTRAINT uk_ont_unit_iri UNIQUE (qudt_iri)
);
COMMENT ON TABLE  ont_unit IS '单位（引用 QUDT）';
COMMENT ON COLUMN ont_unit.qudt_iri              IS '如 .../unit/KiloGM';
COMMENT ON COLUMN ont_unit.symbol                IS '符号，冗余存储降级用';
COMMENT ON COLUMN ont_unit.conversion_multiplier IS '相对基准换算系数';
COMMENT ON COLUMN ont_unit.conversion_offset     IS '换算偏移（温度等）';
COMMENT ON COLUMN ont_unit.scaling_of            IS '基准单位 qudt_iri';
COMMENT ON COLUMN ont_unit.source_ref            IS '来源本体标识，如 qudt';

-- 9.6 注释属性注册表
CREATE TABLE ont_annotation_property (
    id          bigint       NOT NULL,
    local_name  varchar(64)  NOT NULL,
    label       varchar(128) NOT NULL,
    range_xsd   varchar(64),
    applies_to  varchar(32),
    description varchar(512),
    sort_order  int          DEFAULT 0,
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_annotation_property PRIMARY KEY (id),
    CONSTRAINT uk_ont_ap_local_name UNIQUE (local_name)
);
COMMENT ON TABLE  ont_annotation_property IS '注释属性注册表';
COMMENT ON COLUMN ont_annotation_property.local_name IS '如 icon/unitRef';
COMMENT ON COLUMN ont_annotation_property.range_xsd  IS 'xsd:string / xsd:boolean';
COMMENT ON COLUMN ont_annotation_property.applies_to IS 'class/datatypeProperty/objectProperty/individual/all';

-- 9.7 分类编码规则
CREATE TABLE ont_classification_rule (
    id          bigint       NOT NULL,
    tree_root   varchar(64)  NOT NULL,
    separator   varchar(4)   DEFAULT '-',
    level_digits int         DEFAULT 2,
    base_number  int         DEFAULT 0,
    zero_pad    char(1)      DEFAULT '1',
    description varchar(512),
    create_by   varchar(64)  DEFAULT ' ',
    create_time timestamp    DEFAULT now(),
    update_by   varchar(64)  DEFAULT ' ',
    update_time timestamp    DEFAULT now(),
    del_flag    char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_classification_rule PRIMARY KEY (id),
    CONSTRAINT uk_ont_cls_rule_tree_root UNIQUE (tree_root)
);
COMMENT ON TABLE  ont_classification_rule IS '分类编码规则（FR-8）';
COMMENT ON COLUMN ont_classification_rule.tree_root     IS '所属分类树标识，对应 ont_class_template.tree_root';
COMMENT ON COLUMN ont_classification_rule.separator     IS '分隔符，默认 -';
COMMENT ON COLUMN ont_classification_rule.level_digits  IS '每级位数，默认 2';
COMMENT ON COLUMN ont_classification_rule.base_number   IS '根级编码基数，如 30';
COMMENT ON COLUMN ont_classification_rule.zero_pad      IS '0/1 是否零填充（默认 1）';

-- 9.8 本体类分类树镜像（subClassOf，只读）
CREATE TABLE ont_class_hierarchy (
    id                   bigint       NOT NULL,
    child_class_iri      varchar(255) NOT NULL,
    parent_class_iri     varchar(255) NOT NULL,
    source_template_ref  varchar(64),
    tree_root            varchar(64),
    sync_status          char(1)      DEFAULT '0',
    sync_time            timestamp,
    create_by            varchar(64)  DEFAULT ' ',
    create_time          timestamp    DEFAULT now(),
    update_by            varchar(64)  DEFAULT ' ',
    update_time          timestamp    DEFAULT now(),
    del_flag             char(1)      DEFAULT '0',
    CONSTRAINT pk_ont_class_hierarchy PRIMARY KEY (id)
);
COMMENT ON TABLE  ont_class_hierarchy IS '本体类分类树镜像（subClassOf，FR-9，只读）';
COMMENT ON COLUMN ont_class_hierarchy.child_class_iri     IS '子类 IRI（建模侧类，只镜像不创建）';
COMMENT ON COLUMN ont_class_hierarchy.parent_class_iri    IS '父类 IRI';
COMMENT ON COLUMN ont_class_hierarchy.source_template_ref IS '溯源：建议来源的分类模板 template_code';
COMMENT ON COLUMN ont_class_hierarchy.sync_status         IS '0=待同步 1=已同步 2=已失效';
COMMENT ON COLUMN ont_class_hierarchy.sync_time           IS '最近同步时间';

-- 索引（高频查询）
CREATE INDEX idx_ont_prop_tpl_kind      ON ont_property_template (kind) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_parent   ON ont_class_template (parent_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_tree     ON ont_class_template (tree_root) WHERE del_flag = '0';
CREATE INDEX idx_ont_class_tpl_ref_tid  ON ont_class_template_ref (class_template_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_unit_qk            ON ont_unit (quantity_kind_id) WHERE del_flag = '0';
CREATE INDEX idx_ont_hierarchy_child    ON ont_class_hierarchy (child_class_iri) WHERE del_flag = '0';
```

> 说明：
> - 8 表主键均 `bigint NOT NULL`，不建序列（应用层用 `IdType.ASSIGN_ID` 雪花 ID，对齐 pig 范式）。
> - 布尔字段统一 `char(1) DEFAULT '0'/'1'`，逻辑删除 `del_flag char(1) DEFAULT '0'`，与 pig 现有表一致（审计字段范式见 PRD 3.2）。
> - 部分索引带 `WHERE del_flag='0'`（PG 部分索引），提升有效数据查询效率。
> - `ont_class_template` 的 `template_code` 与 `classification_code` 双唯一约束（AC-2.x + AC-8.3）。
> - `ont_class_hierarchy` 无唯一约束（建模侧可能推送多条同边关系的历史镜像，靠 sync_status 区分）。

### 6.2 `V5__ont_governance_seed.sql`（菜单 + 权限 + 12 内置属性模板）

> 含三部分：(a) sys_menu 种子（10000+ 段）；(b) 内置 12 属性模板（满足 AC-1.1，DD2 起消费）；(c) 分类编码规则占位 + 治理元数据占位。

```sql
-- ============================================================
-- V5__ont_governance_seed.sql
-- 本体治理菜单 + 权限点 + 内置属性模板种子
-- 对应 PRD v1.2 第十三节、FR-1 AC-1.1
-- ============================================================

-- ---------- (a) sys_menu 种子（16 字段按位置 INSERT） ----------
-- 字段顺序：menu_id, name, permission, path, component, parent_id, icon, visible,
--           sort_order, keep_alive, embedded, menu_type, create_by, create_time,
--           update_by, update_time, del_flag
-- menu_type: '0'=目录 '1'=菜单 '2'=按钮

INSERT INTO sys_menu VALUES (10000, '本体治理', NULL, '/ontology', NULL, NULL, 'iconfont icon-shujujicheng', '1', 30, '0', '0', '0', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10100, '属性模板库', NULL, '/admin/ontology/property-template/index', NULL, 10000, 'iconfont icon-shuxing', '1', 1, '0', '0', '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10101, '属性模板新增', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 1, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10102, '属性模板编辑', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 2, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10103, '属性模板删除', 'ont_prop_tpl_manage', NULL, NULL, 10100, NULL, '1', 3, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10104, '属性模板查看', 'ont_prop_tpl_view', NULL, NULL, 10100, NULL, '1', 4, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10200, '分类模板', NULL, '/admin/ontology/class-template/index', NULL, 10000, 'iconfont icon-fenlei', '1', 2, '0', '0', '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10201, '分类模板新增', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 1, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10202, '分类模板编辑', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 2, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10203, '分类模板删除', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 3, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10204, '编码规则配置', 'ont_class_tpl_manage', NULL, NULL, 10200, NULL, '1', 4, '0', NULL, '2', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10250, '类分类树', NULL, '/admin/ontology/class-hierarchy/index', NULL, 10000, 'iconfont icon-jiegoushujuguanli', '1', 3, '0', '0', '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10300, '单位注册表', NULL, '/admin/ontology/unit/index', NULL, 10000, 'iconfont icon-danwei', '1', 4, '0', '0', '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10400, '注释属性注册表', NULL, '/admin/ontology/annotation-property/index', NULL, 10000, 'iconfont icon-biaoqian', '1', 5, '0', '0', '1', 'admin', now(), 'admin', now(), '0');
INSERT INTO sys_menu VALUES (10500, '参考本体库', NULL, '/admin/ontology/reference/index', NULL, 10000, 'iconfont icon-canshupeizhi', '1', 6, '0', '0', '1', 'admin', now(), 'admin', now(), '0');

-- ---------- (b) 内置属性模板 12 项（AC-1.1：8 数据属性 + 4 对象属性） ----------
-- id 用固定值便于引用（雪花 ID 由 ASSIGN_ID 生成，种子用显式 bigint 占位）

-- 8 数据属性
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, type, is_identifier, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(1001, 'name',          'datatype', '名称',     '实体名称',           'basic',     'string',   '1', 'builtin', '0', 'admin', now(), 'admin', now()),
(1002, 'description',   'datatype', '描述',     '实体描述',           'basic',     'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1003, 'email',         'datatype', '邮箱',     '联系邮箱',           'contact',   'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1004, 'phone',         'datatype', '电话',     '联系电话',           'contact',   'string',   '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1005, 'price',         'datatype', '价格',     '货币金额',           'monetary',  'decimal',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1006, 'amount',        'datatype', '数量',     '数值量',             'monetary',  'decimal',  '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1007, 'createdDate',   'datatype', '创建日期', '创建时间',           'temporal',  'datetime', '0', 'builtin', '0', 'admin', now(), 'admin', now()),
(1008, 'isActive',      'datatype', '是否启用', '状态标记',           'status',    'boolean',  '0', 'builtin', '0', 'admin', now(), 'admin', now());

-- 4 对象属性
INSERT INTO ont_property_template (id, template_code, kind, label, description, category, default_cardinality, source, deprecated, create_by, create_time, update_by, update_time)
VALUES
(2001, 'contains',   'object', '包含', '包含关系',     'containment',  'one-to-many',  'builtin', '0', 'admin', now(), 'admin', now()),
(2002, 'belongsTo',  'object', '属于', '归属关系',     'attribution',  'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now()),
(2003, 'hasPart',    'object', '拥有部分', '组合关系', 'containment',  'one-to-many',  'builtin', '0', 'admin', now(), 'admin', now()),
(2004, 'references', 'object', '引用', '引用关系',     'attribution',  'many-to-one',  'builtin', '0', 'admin', now(), 'admin', now());

-- ---------- (c) 治理元数据占位（QUDT 版本快照，AC-3.7） ----------
-- 独立元数据表 ont_governance_meta（V4 之外的最小补充，用动态 DDL 避免改 V4）
CREATE TABLE IF NOT EXISTS ont_governance_meta (
    meta_key   varchar(64)  NOT NULL,
    meta_value varchar(255),
    create_by  varchar(64)  DEFAULT ' ',
    create_time timestamp   DEFAULT now(),
    update_by  varchar(64)  DEFAULT ' ',
    update_time timestamp   DEFAULT now(),
    CONSTRAINT pk_ont_governance_meta PRIMARY KEY (meta_key)
);
COMMENT ON TABLE ont_governance_meta IS '治理元数据（QUDT 版本快照等）';

INSERT INTO ont_governance_meta (meta_key, meta_value) VALUES
('qudt_version', '3.1.4'),
('qudt_source_ref', 'docs/ontology/参考开源本体库/qudt/qudt-all.ttl')
ON CONFLICT (meta_key) DO NOTHING;
```

> 说明：
> - sys_menu 的 `path` 字段值（如 `/admin/ontology/property-template/index`）须与未来 `web/src/views/ontology/property-template/index.vue` 严格对应（S-6），backEnd.ts 据此 glob 匹配。
> - 内置模板 `id` 用语义化固定值（1001-1008 数据属性、2001-2004 对象属性），便于 V5 自身和后续脚本引用；生产环境 ASSIGN_ID 雪花 ID 仅用于 custom 模板。
> - `ont_governance_meta` 是 9.5 节备注提到的元数据表（QUDT 版本快照），放 V5 末尾动态建（`IF NOT EXISTS`），不返工 V4。
> - 分类树/单位/注释属性种子**不在 M0**，随 DD3/DD4/DD5 的 V6+ 脚本补。

---

## 七、横切设计

### 7.1 双形态验证

| 形态 | 验证步骤 | 预期 |
|---|---|---|
| 单体（pig-boot） | `mvn -pl pig-boot -am package -Pboot`；启动；`curl http://localhost:9999/admin/ont/health` | `{"code":0,"data":"pig-ontology is up"}` |
| 微服务 | `mvn -pl pig-ontology/pig-ontology-biz -am package`；启动 pig-gateway + pig-ontology-biz；`curl http://localhost:9999/admin/ont/health`（经网关） | 同上 |

### 7.2 Flyway 验证

- 启动 pig-boot 后，`flyway_schema_history` 应出现 V4/V5 成功记录。
- `SELECT count(*) FROM ont_property_template WHERE source='builtin'` 应为 12（AC-1.1）。
- `SELECT count(*) FROM sys_menu WHERE menu_id BETWEEN 10000 AND 10999` 应为 15（1 目录 + 6 菜单 + 8 按钮）。

### 7.3 菜单可见性验证

- pig-ui 用 admin 登录，侧边栏应出现"本体治理"一级菜单及 6 个子菜单。
- M0 阶段子菜单点击 404 属预期（vue 文件未建）；DD2 实现后正常。

### 7.4 模块隔离验证

- `mvn dependency:tree -pl pig-ontology/pig-ontology-biz` 不应出现 `pig-upms`/`pig-visual` 依赖（NFR-11）。

---

## 八、测试要点

| 类别 | 要点 |
|---|---|
| 编译 | `mvn -pl pig-ontology/pig-ontology-biz -am clean compile` 通过 |
| 启动 | pig-boot 单体启动无 Bean 冲突；`com.pig4cloud.pig.ontology.*` 被扫描 |
| DDL | V4 在空库执行成功；重复执行因 Flyway 校验不报错（已应用脚本不改） |
| 种子 | V5 的 12 模板、15 菜单写入成功；`ont_governance_meta` 2 条 |
| 健康检查 | 单体/微服务两种形态 `/admin/ont/health` 均返回 200 |
| 网关 | 微服务形态 `/admin/ont/**` 路由到 pig-ontology-biz，非该前缀不路由 |
| 回归 | pig 现有功能（用户/角色/代码生成）不受影响 |

---

## 九、风险与缓解

| 风险 | 缓解 |
|---|---|
| Flyway V4/V5 与其他开发分支脚本版本撞车（R-2） | M0 先占 V4/V5；合并时若冲突，按需上调版本号 |
| pig-boot 聚合后 Bean 名冲突 | pig-ontology 包独立（`com.pig4cloud.pig.ontology`），无同名 Bean；Controller 路径 `/ont/**` 不与其他模块冲突 |
| 网关路由失效导致微服务形态 404 | 路由配置对齐 pig-upms-biz 写法；验证步骤 7.1 覆盖 |
| sys_menu path 与未来 vue 文件不匹配（S-6） | path 严格按 `views/ontology/<feature>/index.vue` 约定写；DD2 起按此约定建文件 |

---

## 十、交付物清单

| 类型 | 文件 | 说明 |
|---|---|---|
| 新建 | `server/pig-ontology/pom.xml` | 聚合 pom |
| 新建 | `server/pig-ontology/pig-ontology-biz/pom.xml` | biz pom |
| 新建 | `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/PigOntologyApplication.java` | 启动类 |
| 新建 | `server/pig-ontology/pig-ontology-biz/src/main/java/com/pig4cloud/pig/ontology/controller/OntologyHealthController.java` | 健康检查 |
| 新建 | `server/pig-ontology/pig-ontology-biz/src/main/resources/application.yml` | 微服务配置 |
| 新建 | `server/pig-common/pig-common-data/src/main/resources/db/migration/V4__ont_governance_schema.sql` | 8 表 DDL |
| 新建 | `server/pig-common/pig-common-data/src/main/resources/db/migration/V5__ont_governance_seed.sql` | 菜单+模板种子 |
| 修改 | `server/pom.xml` | modules 加 pig-ontology |
| 修改 | `server/pig-boot/pom.xml` | 依赖加 pig-ontology-biz |
| 修改 | `server/pig-gateway/src/main/resources/application.yml` | 加 pig-ontology 路由 |

---

*本详细设计对应里程碑 M0，是后续 DD2~DD6 的基础设施。实现完成后，按《详细设计计划.md》依赖顺序进入 DD2（属性模板库 + 供给接口）。*
