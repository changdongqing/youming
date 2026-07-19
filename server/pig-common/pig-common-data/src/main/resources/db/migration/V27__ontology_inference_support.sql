-- =============================
-- V27: 对象属性推理能力契约（inference_support）
-- 对应设计文档：37-关系能力契约与推理支持范围字典详细设计
-- =============================

-- 1. 主表新增 inference_support 列（jsonb，空数组表示非推理托管）
ALTER TABLE ont_object_property
  ADD COLUMN IF NOT EXISTS inference_support jsonb DEFAULT '[]'::jsonb;

COMMENT ON COLUMN ont_object_property.inference_support IS
  '推理引擎实际支持的能力子集，取值受ReasonerCapability枚举约束；空数组表示非推理托管';

-- 2. CHECK 约束：inference_support 必须是数组形式
ALTER TABLE ont_object_property
  DROP CONSTRAINT IF EXISTS ck_ont_object_property_inference_support;

ALTER TABLE ont_object_property
  ADD CONSTRAINT ck_ont_object_property_inference_support
  CHECK (
    inference_support IS NULL
    OR jsonb_typeof(inference_support::jsonb) = 'array'
  );

-- 3. GIN 索引（支持 by-capability 的 @> 包含查询）
CREATE INDEX IF NOT EXISTS idx_ont_object_property_inference_support
  ON ont_object_property USING gin (inference_support)
  WHERE del_flag = '0';

-- 4. 种子数据：仅 issuedBy(960006) 声明 FUNCTIONAL_CHECK
UPDATE ont_object_property
  SET inference_support = '["FUNCTIONAL_CHECK"]'::jsonb
  WHERE id = 960006 AND del_flag = '0';

-- 5. 其余 34 条预置关系显式置空数组（非推理托管）
UPDATE ont_object_property
  SET inference_support = '[]'::jsonb
  WHERE id IN (960001, 960002, 960003, 960004, 960005,
               960007, 960008, 960009, 960010, 960011,
               960012, 960013, 960014, 960015, 960016,
               960017, 960018, 960019, 960020, 960021,
               960022, 960023, 960024, 960025, 960026,
               960027, 960028, 960029, 960030, 960031,
               960032, 960033, 960034, 960035)
  AND del_flag = '0';

-- 6. 完整性断言
DO $$
DECLARE
  v_total int;
  v_functional_check_count int;
  v_issued_by_functional text;
BEGIN
  -- 6.1 35 条预置关系的 inference_support 均非 NULL
  SELECT count(*) INTO v_total
  FROM ont_object_property
  WHERE id BETWEEN 960001 AND 960035 AND del_flag = '0'
    AND inference_support IS NOT NULL;
  ASSERT v_total = 35, '35条预置关系的inference_support必须全部非空';

  -- 6.2 仅 issuedBy(960006) 声明 FUNCTIONAL_CHECK
  SELECT count(*) INTO v_functional_check_count
  FROM ont_object_property
  WHERE del_flag = '0'
    AND inference_support @> '["FUNCTIONAL_CHECK"]'::jsonb;
  ASSERT v_functional_check_count = 1, '只有issuedBy应该声明FUNCTIONAL_CHECK';

  -- 6.3 issuedBy 的 is_functional='1'（L3 ⊆ L2 约束）
  SELECT is_functional INTO v_issued_by_functional
  FROM ont_object_property WHERE id = 960006 AND del_flag = '0';
  ASSERT v_issued_by_functional = '1',
    'issuedBy声明FUNCTIONAL_CHECK时is_functional必须为1';

  -- 6.4 不存在本期未启用的推理能力
  ASSERT NOT EXISTS (
    SELECT 1 FROM ont_object_property WHERE del_flag = '0'
      AND (inference_support ? 'TRANSITIVE_INFERENCE'
        OR inference_support ? 'SYMMETRIC_INFERENCE'
        OR inference_support ? 'CUSTOM_RULE_INFERENCE')
  ), '预置关系不得声明本期未启用的推理能力';
END $$;
