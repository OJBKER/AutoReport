-- 修复任务数据编码
DELETE FROM tasks;

INSERT INTO tasks (title, description, class_id) VALUES ('数学作业1', '完成第一章习题', 'class001');
INSERT INTO tasks (title, description, class_id) VALUES ('英语作业1', '完成课文背默写', 'class001');
INSERT INTO tasks (title, description, class_id) VALUES ('物理实验报告', '完成光学实验报告', 'class001');