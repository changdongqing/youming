#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MySQL dump (pig.sql) -> PostgreSQL DDL+DML 转换器

输入: db/pig.sql (mysqldump 标准格式)
输出:
  - V1__init_schema.sql  38 张业务表结构 (CREATE TABLE + CREATE INDEX + COMMENT ON)
  - V1__init_data.sql    业务表种子数据 (INSERT)
  - quartz 表由官方脚本单独提供, 本脚本跳过 qrtz_* 表

转换规则见 docs/dbversion/08-DDL转换结果核验清单.md
重点:
  - MySQL 字符串字面量精确解析 (\n \t \' \" \\ 等转义还原)
  - PG 侧用 '' 重编码单引号; 含换行的长串用美元引号 $tag$...$tag$
  - 移除 ENGINE/CHARSET/COLLATE/CHARACTER SET/USING BTREE/ON UPDATE 等
  - 列级/表级 COMMENT 拆为 COMMENT ON 语句
  - 列级 KEY/UNIQUE KEY 拆为 CREATE INDEX
  - datetime->timestamp, bigint unsigned->bigint, longtext->text, tinyint->smallint
  - gen_field_type 用普通 bigint (ASSIGN_ID 雪花, 无序列)
"""
import re
import sys
import os

SRC = os.path.join(os.path.dirname(__file__), '..', 'pig.sql')

# ---------- MySQL 字符串字面量解析 ----------
def parse_mysql_string_literal(s):
    """s 形如 'xxx' 或 "xxx", 返回内部真实字符 (处理 MySQL 反斜杠转义)"""
    quote = s[0]
    body = s[1:-1]
    if quote == "'":
        # MySQL 字符串: 反斜杠转义 + '' 双单引号转义
        out = []
        i = 0
        while i < len(body):
            c = body[i]
            if c == '\\' and i + 1 < len(body):
                nxt = body[i + 1]
                mp = {'n': '\n', 't': '\t', 'r': '\r', '0': '\0', '\\': '\\',
                      "'": "'", '"': '"', 'b': '\b', 'Z': '\x1a'}
                out.append(mp.get(nxt, '\\' + nxt))
                i += 2
            elif c == "'" and i + 1 < len(body) and body[i + 1] == "'":
                out.append("'")
                i += 2
            else:
                out.append(c)
                i += 1
        return ''.join(out)
    else:  # 双引号字符串 (MySQL ANSI 模式下也是字符串)
        out = []
        i = 0
        while i < len(body):
            c = body[i]
            if c == '\\' and i + 1 < len(body):
                nxt = body[i + 1]
                mp = {'n': '\n', 't': '\t', 'r': '\r', '0': '\0', '\\': '\\',
                      "'": "'", '"': '"', 'b': '\b', 'Z': '\x1a'}
                out.append(mp.get(nxt, '\\' + nxt))
                i += 2
            elif c == '"' and i + 1 < len(body) and body[i + 1] == '"':
                out.append('"')
                i += 2
            else:
                out.append(c)
                i += 1
        return ''.join(out)


def to_pg_string(val):
    """将 Python 真实字符值编码为 PG 字面量。
    含换行/特殊字符的长串用美元引号 $ymg$...$ymg$, 其余用单引号 + '' 编码。"""
    if val is None:
        return 'NULL'
    # 需要美元引号的情况: 含换行, 且包含单引号或反斜杠 (避免 '' 编码后难以阅读)
    needs_dollar = ('\n' in val) or ('\r' in val) or ('\t' in val and len(val) > 50)
    if needs_dollar:
        tag = '$ymg$'
        # 确保值内不含 tag 分隔符
        while tag in val:
            tag = '$' + 'x' * (len(tag) - 2) + '$'
        return tag + val + tag
    # 普通编码: 单引号包裹, 内部 ' -> ''
    return "'" + val.replace("'", "''") + "'"


# ---------- tokenize: 把 INSERT VALUES 后的括号内容切成值列表 ----------
def split_values(s):
    """s 是 VALUES (...) 中括号内的字符串 (不含外层括号), 返回各值(已是 Python 对象)。
    识别: 字符串字面量 '...'/"...", 数字, NULL, 函数(如 now() 不应出现在 dump), 十六进制 0x.."""
    vals = []
    i = 0
    n = len(s)
    while i < n:
        # 跳过空白和逗号
        while i < n and s[i] in ' \t\r\n,':
            i += 1
        if i >= n:
            break
        c = s[i]
        if c == "'" or c == '"':
            # 字符串字面量: 找到结束引号 (处理 \\ 和 '' 转义)
            quote = c
            j = i + 1
            while j < n:
                if s[j] == '\\' and j + 1 < n:
                    j += 2
                    continue
                if s[j] == quote:
                    # 检查是否是双引号转义 ''
                    if j + 1 < n and s[j + 1] == quote:
                        j += 2
                        continue
                    break
                j += 1
            literal = s[i:j + 1]
            vals.append(('str', parse_mysql_string_literal(literal)))
            i = j + 1
        elif s[i] in '0123456789-+.' or (s[i] == 'N' and s[i:i + 4] == 'NULL'):
            # 数字或 NULL
            if s[i:i + 4] == 'NULL':
                vals.append(('null', None))
                i += 4
            else:
                j = i
                if s[j] in '+-':
                    j += 1
                # 十六进制
                if s[i:i + 2] in ('0x', '0X'):
                    j = i + 2
                    while j < n and s[j] in '0123456789abcdefABCDEF':
                        j += 1
                    vals.append(('hex', s[i:j]))
                    i = j
                else:
                    while j < n and (s[j].isdigit() or s[j] == '.'):
                        j += 1
                    token = s[i:j]
                    if '.' in token:
                        vals.append(('num', token))
                    else:
                        vals.append(('int', token))
                    i = j
        else:
            # 其他 (函数等), 读到逗号或括号结束
            j = i
            depth = 0
            while j < n and s[j] not in ',':
                if s[j] == '(':
                    depth += 1
                elif s[j] == ')':
                    if depth == 0:
                        break
                    depth -= 1
                j += 1
            vals.append(('raw', s[i:j].strip()))
            i = j
    return vals


def render_value(v):
    kind, val = v
    if kind == 'null':
        return 'NULL'
    if kind == 'str':
        return to_pg_string(val)
    if kind == 'hex':
        # MySQL 0x.. 二进制 -> PG decode('..','hex') 或直接用字节串
        try:
            b = bytes.fromhex(val[2:])
            # PG bytea 用 '\x..' 格式
            return "'\\x" + val[2:].lower() + "'::bytea"
        except Exception:
            return to_pg_string(val)
    if kind in ('int', 'num'):
        return val
    if kind == 'raw':
        return val
    return to_pg_string(str(val))


# ---------- DDL 转换 ----------
# MySQL 类型 -> PG 类型
def conv_type(coldef):
    """coldef 是列定义行 (去反引号后), 返回转换后的列定义片段 (不含 COMMENT)"""
    # 已在外面处理反引号; 这里聚焦类型与属性
    s = coldef
    # 移除 CHARACTER SET xxx COLLATE xxx
    s = re.sub(r'\s+CHARACTER SET \w+', '', s)
    s = re.sub(r'\s+COLLATE \w+', '', s)
    # 1) 先去掉所有整型的 unsigned / zerofill (PG 无 unsigned)
    #    覆盖: bigint unsigned / bigint(20) unsigned / int unsigned / tinyint(1) unsigned 等
    s = re.sub(r'\b(bigint|int|tinyint|smallint|mediumint)\(\d+\)\s+(unsigned|zerofill)',
               lambda m: m.group(1), s, flags=re.I)
    s = re.sub(r'\b(bigint|int|tinyint|smallint|mediumint)\s+(unsigned|zerofill)',
               lambda m: m.group(1), s, flags=re.I)
    # 2) 整型带长度括号 -> 去括号 (bigint(20) -> bigint, int(11) -> integer, tinyint(1) -> smallint)
    s = re.sub(r'\bbigint\(\d+\)', 'bigint', s, flags=re.I)
    s = re.sub(r'\bint\(\d+\)', 'integer', s, flags=re.I)
    s = re.sub(r'\btinyint\(\d+\)', 'smallint', s, flags=re.I)
    s = re.sub(r'\bsmallint\(\d+\)', 'smallint', s, flags=re.I)
    s = re.sub(r'\bmediumint\(\d+\)', 'integer', s, flags=re.I)
    # 2b) 无括号的整型 (tinyint / mediumint) 兜底转换 (int 单独处理, 避免误伤 integer/varchar)
    s = re.sub(r'\btinyint\b', 'smallint', s, flags=re.I)
    s = re.sub(r'\bmediumint\b', 'integer', s, flags=re.I)
    # datetime -> timestamp
    s = re.sub(r'\bdatetime\b', 'timestamp', s, flags=re.I)
    # longtext -> text
    s = re.sub(r'\blongtext\b', 'text', s, flags=re.I)
    s = re.sub(r'\bmediumtext\b', 'text', s, flags=re.I)
    s = re.sub(r'\btinytext\b', 'text', s, flags=re.I)
    # blob 系列 (业务表一般没有, 但兜底)
    s = re.sub(r'\blongblob\b', 'bytea', s, flags=re.I)
    s = re.sub(r'\bmediumblob\b', 'bytea', s, flags=re.I)
    s = re.sub(r'\btinyblob\b', 'bytea', s, flags=re.I)
    s = re.sub(r'\bblob\b', 'bytea', s, flags=re.I)
    # bit(1) -> boolean 兜底 (pig 一般不用)
    # DEFAULT CURRENT_TIMESTAMP -> DEFAULT now()
    s = re.sub(r'DEFAULT\s+CURRENT_TIMESTAMP', 'DEFAULT now()', s, flags=re.I)
    # 删除 ON UPDATE CURRENT_TIMESTAMP (列级)
    s = re.sub(r'\s+ON\s+UPDATE\s+CURRENT_TIMESTAMP', '', s, flags=re.I)
    # 移除 AUTO_INCREMENT
    s = re.sub(r'\s+AUTO_INCREMENT', '', s, flags=re.I)
    # decimal 保持 (PG 支持 decimal 作为 numeric 别名)
    return s


def split_top_level(s, sep=','):
    """按 sep 切分, 但忽略括号内和引号内的 sep"""
    parts = []
    depth = 0
    cur = []
    i = 0
    quote = None
    while i < len(s):
        c = s[i]
        if quote:
            cur.append(c)
            if c == '\\' and i + 1 < len(s):
                cur.append(s[i + 1])
                i += 2
                continue
            if c == quote:
                quote = None
            i += 1
            continue
        if c in "'\"":
            quote = c
            cur.append(c)
        elif c == '(':
            depth += 1
            cur.append(c)
        elif c == ')':
            depth -= 1
            cur.append(c)
        elif c == sep and depth == 0:
            parts.append(''.join(cur))
            cur = []
        else:
            cur.append(c)
        i += 1
    if cur:
        parts.append(''.join(cur))
    return parts


def convert_table_ddl(create_stmt, table_name):
    """
    create_stmt: CREATE TABLE `xxx` ( ... ) ENGINE=... COMMENT='...';
    返回 (pg_create_sql, index_statements, table_comment, columns_info)
    columns_info: [(col_name, comment_text)] 用于生成 COMMENT ON COLUMN
    """
    # 提取表级 COMMENT
    table_comment = None
    m = re.search(r"COMMENT\s*=\s*'((?:[^'\\]|\\.|'')*)'", create_stmt, flags=re.I)
    if m:
        table_comment = parse_mysql_string_literal("'" + m.group(1) + "'")
        create_stmt = create_stmt[:m.start()] + create_stmt[m.end():]

    # 提取括号内的列/索引定义
    m = re.search(r'\((.*)\)\s*(ENGINE=.*)?$', create_stmt, flags=re.I | re.S)
    if not m:
        return None
    body = m.group(1)
    # 切分顶层逗号
    parts = split_top_level(body, ',')

    columns = []          # 列定义 (转换后, 不含 COMMENT)
    column_comments = []  # (col_name, comment)
    indexes = []          # CREATE INDEX 语句
    primary_key = None

    for part in parts:
        p = part.strip()
        if not p:
            continue
        # 去反引号 (标识符)
        # 判断是约束还是列
        upper = p.upper()
        if re.match(r'^PRIMARY\s+KEY', upper):
            # PRIMARY KEY (col) USING BTREE
            pk = re.sub(r'\s+USING\s+BTREE', '', p, flags=re.I)
            pk = pk.replace('`', '')
            primary_key = pk
        elif re.match(r'^UNIQUE\s+KEY', upper) or re.match(r'^UNIQUE\s+INDEX', upper):
            # UNIQUE KEY `uk` (col) USING BTREE
            mm = re.match(r'UNIQUE\s+(?:KEY|INDEX)\s+`?(\w+)?`?\s*\(([^)]+)\)\s*(USING\s+\w+)?',
                          p, flags=re.I)
            if mm:
                idx_name = mm.group(1)
                cols = mm.group(2).replace('`', '').strip()
                if idx_name:
                    indexes.append(f'CREATE UNIQUE INDEX {idx_name} ON {table_name} ({cols});')
                else:
                    indexes.append(f'CREATE UNIQUE INDEX ON {table_name} ({cols});')
        elif re.match(r'^KEY\s+`', upper) or re.match(r'^INDEX\s+`', upper):
            # KEY `idx` (col) USING BTREE
            mm = re.match(r'(?:KEY|INDEX)\s+`?(\w+)?`?\s*\(([^)]+)\)\s*(USING\s+\w+)?', p, flags=re.I)
            if mm:
                idx_name = mm.group(1)
                cols = mm.group(2).replace('`', '').strip()
                if idx_name:
                    indexes.append(f'CREATE INDEX {idx_name} ON {table_name} ({cols});')
                else:
                    indexes.append(f'CREATE INDEX ON {table_name} ({cols});')
        elif re.match(r'^CONSTRAINT', upper) or re.match(r'^FOREIGN\s+KEY', upper):
            # 外键约束: 业务表无外键; qrtz 表的外键故意不保留
            # (与 Quartz 官方 tables_postgres.sql 一致: 官方 PG 脚本不建外键,
            #  以保证导入顺序灵活; Quartz 运行时不依赖外键完整性约束)
            pass
        elif re.match(r'^CHECK', upper):
            pass
        else:
            # 列定义: `col` type ... COMMENT '...'
            # 提取列名
            cm = re.match(r'`?(\w+)`?\s+(.*)', p, flags=re.I | re.S)
            if cm:
                col_name = cm.group(1)
                rest = cm.group(2).strip()
                # 提取列级 COMMENT
                col_comment = None
                ccm = re.search(r"COMMENT\s+'((?:[^'\\]|\\.|'')*)'", rest, flags=re.I)
                if ccm:
                    col_comment = parse_mysql_string_literal("'" + ccm.group(1) + "'")
                    rest = rest[:ccm.start()] + rest[ccm.end():]
                # 类型与属性转换
                rest = conv_type(rest)
                # 去除残留反引号
                rest = rest.replace('`', '')
                col_def = f'{col_name} {rest.strip()}'
                col_def = re.sub(r'\s+', ' ', col_def)
                columns.append(col_def)
                if col_comment is not None:
                    column_comments.append((col_name, col_comment))

    # 组装 CREATE TABLE
    col_lines = ['  ' + c for c in columns]
    if primary_key:
        col_lines.append('  ' + primary_key)
    create_sql = f'CREATE TABLE {table_name} (\n' + ',\n'.join(col_lines) + '\n);'
    return {
        'create_sql': create_sql,
        'indexes': indexes,
        'table_comment': table_comment,
        'column_comments': column_comments,
    }


# ---------- 主流程 ----------
def main():
    with open(SRC, 'r', encoding='utf-8') as f:
        raw = f.read()

    schema_lines = []
    data_lines = []
    schema_lines.append('-- PostgreSQL 业务库结构 (由 pig.sql 转换, 38 张业务表)')
    schema_lines.append('-- 生成工具: convert_mysql_to_pg.py')
    schema_lines.append('-- 转换规则见 docs/dbversion/08-DDL转换结果核验清单.md')
    schema_lines.append('')
    data_lines.append('-- PostgreSQL 种子数据 (由 pig.sql 转换)')
    data_lines.append('')

    # 按 ; 分割语句 (状态机, 正确处理字符串内的 ; ' " \ 与括号)
    def split_statements(text):
        stmts = []
        cur = []
        quote = None
        i = 0
        n = len(text)
        while i < n:
            c = text[i]
            if quote:
                cur.append(c)
                if c == '\\' and i + 1 < n:
                    cur.append(text[i + 1])
                    i += 2
                    continue
                if c == quote:
                    # 处理双引号转义 ('' 或 "")
                    if i + 1 < n and text[i + 1] == quote:
                        cur.append(text[i + 1])
                        i += 2
                        continue
                    quote = None
                i += 1
                continue
            if c in "'\"":
                quote = c
                cur.append(c)
                i += 1
                continue
            # 行注释 -- 到行尾
            if c == '-' and i + 1 < n and text[i + 1] == '-':
                while i < n and text[i] != '\n':
                    cur.append(text[i])
                    i += 1
                continue
            if c == ';':
                stmt = ''.join(cur).strip()
                if stmt:
                    stmts.append(stmt)
                cur = []
                i += 1
                continue
            cur.append(c)
            i += 1
        tail = ''.join(cur).strip()
        if tail:
            stmts.append(tail)
        return stmts

    statements = split_statements(raw)

    # --- 处理 CREATE TABLE ---
    # 语句可能带前导注释 (-- xxx\n), 用 search 并定位 CREATE TABLE 起始
    create_pattern = re.compile(
        r'CREATE TABLE\s+`(\w+)`\s*\((.*)\)\s*(ENGINE=.*)?$',
        flags=re.I | re.S)

    def extract_create(stmt):
        """从语句中提取 CREATE TABLE 子句 (剥离前导注释/换行), 返回 (表名, 完整create语句) 或 None"""
        # 剥离前导 -- 注释行
        lines = stmt.split('\n')
        cleaned = []
        for ln in lines:
            if ln.lstrip().startswith('--'):
                continue
            cleaned.append(ln)
        body = '\n'.join(cleaned).strip()
        m = create_pattern.match(body)
        if m:
            return m.group(1), body
        return None

    # 区分业务表与 qrtz 表
    # qrtz 表不从 MySQL 转换 (类型语义不同: IS_DURABLE 等列 MySQL 是 varchar(1), PG 必须是 boolean),
    # 由手写的官方 tables_postgres.sql 提供 (见 quartz/tables_postgres.sql)
    qrtz_table_names = set()
    for st in statements:
        r = extract_create(st)
        if r and r[0].lower().startswith('qrtz_'):
            qrtz_table_names.add(r[0])

    # 仅业务表 (非 qrtz)
    table_order = []
    converted = {}
    for st in statements:
        r = extract_create(st)
        if not r:
            continue
        tname, body = r
        if tname in qrtz_table_names:
            continue  # qrtz 表跳过, 由官方 tables_postgres.sql 提供
        result = convert_table_ddl(body, tname)
        if result:
            converted[tname] = result
            table_order.append(tname)

    # 输出 schema: 每张表 DROP + CREATE + INDEX + COMMENT
    for tname in table_order:
        r = converted[tname]
        schema_lines.append(f'-- ----------------------------')
        schema_lines.append(f'-- Table structure for {tname}')
        schema_lines.append(f'-- ----------------------------')
        schema_lines.append(f'DROP TABLE IF EXISTS {tname};')
        schema_lines.append(r['create_sql'])
        for idx in r['indexes']:
            schema_lines.append(idx)
        if r['table_comment'] is not None:
            schema_lines.append(f"COMMENT ON TABLE {tname} IS {to_pg_string(r['table_comment'])};")
        for col_name, comment in r['column_comments']:
            schema_lines.append(f"COMMENT ON COLUMN {tname}.{col_name} IS {to_pg_string(comment)};")
        schema_lines.append('')

    # qrtz 表的 schema 不由此脚本生成 (由手写的官方 quartz/tables_postgres.sql 提供)
    # qrtz 的种子数据(若有)单独收集, 由使用方决定是否追加
    quartz_lines = []

    # --- 处理 INSERT ---
    # 用切分好的 statements (每个语句已正确处理字符串内分号)
    insert_re = re.compile(
        r'^INSERT\s+INTO\s+`(\w+)`\s*(\([^)]*\))?\s*VALUES\s*(.*)$',
        re.I | re.S)

    insert_count = {}
    for st in statements:
        m = insert_re.match(st)
        if not m:
            continue
        tname = m.group(1)
        if tname in qrtz_table_names:
            continue  # qrtz 表数据跳过 (由官方脚本管理, 通常无种子数据)
        cols_part = m.group(2)  # 可能 None
        values_part = m.group(3).strip()
        # 切分多个 (...) 元组
        # values_part 形如 (1,'a',NULL),(2,'b',NULL)
        tuples = []
        depth = 0
        cur = []
        quote = None
        i = 0
        while i < len(values_part):
            c = values_part[i]
            if quote:
                cur.append(c)
                if c == '\\' and i + 1 < len(values_part):
                    cur.append(values_part[i + 1])
                    i += 2
                    continue
                if c == quote:
                    quote = None
                i += 1
                continue
            if c in "'\"":
                quote = c
                cur.append(c)
            elif c == '(':
                if depth == 0:
                    cur = []
                else:
                    cur.append(c)
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    tuples.append(''.join(cur))
                else:
                    cur.append(c)
            else:
                if depth > 0:
                    cur.append(c)
            i += 1

        cols_str = ''
        if cols_part:
            cols_str = ' ' + cols_part.replace('`', '')

        for tup in tuples:
            vals = split_values(tup)
            rendered = ', '.join(render_value(v) for v in vals)
            stmt = f'INSERT INTO {tname}{cols_str} VALUES ({rendered});'
            data_lines.append(stmt)
            insert_count[tname] = insert_count.get(tname, 0) + 1

    # 写文件 (qrtz 表由手写的官方 quartz/tables_postgres.sql 提供, 此脚本不覆盖它)
    schema_out = os.path.join(os.path.dirname(__file__), 'V1__init_schema.sql')
    data_out = os.path.join(os.path.dirname(__file__), 'V1__init_data.sql')
    with open(schema_out, 'w', encoding='utf-8') as f:
        f.write('\n'.join(schema_lines) + '\n')
    with open(data_out, 'w', encoding='utf-8') as f:
        f.write('\n'.join(data_lines) + '\n')

    # 统计
    quartz_out = os.path.join(os.path.dirname(__file__), 'quartz', 'tables_postgres.sql')
    print(f'转换完成:')
    print(f'  业务表结构: {len(table_order)} 张 -> {schema_out}')
    print(f'  Quartz 表: {len(qrtz_table_names)} 张 (手写官方脚本, 不转换) -> {quartz_out}')
    print(f'  种子数据 INSERT: {sum(insert_count.values())} 行 -> {data_out}')
    print(f'  表清单(业务): {", ".join(table_order)}')
    print(f'  表清单(qrtz): {", ".join(qrtz_order)}')
    print(f'  各表数据行数:')
    for t, c in sorted(insert_count.items()):
        print(f'    {t}: {c}')


if __name__ == '__main__':
    main()
