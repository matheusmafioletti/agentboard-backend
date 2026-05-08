UPDATE work_item
SET display_key = sub.prefix || sub.rn::text
FROM (
  SELECT
    id,
    CASE type
      WHEN 'FEATURE'    THEN 'F'
      WHEN 'USER_STORY' THEN 'U'
      WHEN 'TASK'       THEN 'T'
    END AS prefix,
    ROW_NUMBER() OVER (
      PARTITION BY project_id, type
      ORDER BY created_at ASC, id ASC
    )::text AS rn
  FROM work_item
) sub
WHERE work_item.id = sub.id;
