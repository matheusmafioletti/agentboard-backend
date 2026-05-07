-- 1. Features (no parent) — skip orphaned rows that have no project_id
INSERT INTO work_item (id, tenant_id, project_id, type, title, description, status,
                       parent_id, priority, display_order, created_at, updated_at)
SELECT id, tenant_id, project_id, 'FEATURE', title, description,
       stage::TEXT, NULL, 5, 0, created_at, updated_at
FROM feature
WHERE project_id IS NOT NULL;

-- 2. User Stories — skip rows whose feature has no project_id (would violate NOT NULL)
INSERT INTO work_item (id, tenant_id, project_id, type, title, description, status,
                       parent_id, priority, display_order, created_at, updated_at)
SELECT us.id, us.tenant_id, us.project_id, 'USER_STORY', us.title, us.description,
       us.stage::TEXT, us.feature_id, us.priority, us."order", us.created_at, us.updated_at
FROM user_story us
WHERE us.project_id IS NOT NULL;

-- 3. Tasks — skip tasks whose parent user_story has no project_id
INSERT INTO work_item (id, tenant_id, project_id, type, title, description, status,
                       parent_id, priority, display_order, created_at, updated_at)
SELECT t.id,
       us.tenant_id,
       us.project_id,
       'TASK',
       t.title,
       NULL,
       CASE
         WHEN t.completed = TRUE THEN 'CLOSED'
         WHEN t.blocked   = TRUE THEN 'ACTIVE'
         ELSE                         'NEW'
       END,
       t.user_story_id,
       5,
       0,
       t.created_at,
       t.created_at
FROM task t
JOIN user_story us ON us.id = t.user_story_id
WHERE us.project_id IS NOT NULL;
