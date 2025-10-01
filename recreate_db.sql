-- 重新创建数据库表结构
-- 删除旧表
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS user_tasks;
DROP TABLE IF EXISTS classes;

-- 创建classes表
CREATE TABLE classes (
    class_id VARCHAR(255) PRIMARY KEY,
    class_name VARCHAR(255)
);

-- 创建users表
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    student_number BIGINT,
    password TEXT,
    github_id TEXT,
    class_id VARCHAR(255),
    FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- 创建tasks表
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(255),
    description TEXT,
    deadline DATETIME,
    class_id VARCHAR(255),
    FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- 创建user_tasks表
CREATE TABLE user_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT,
    task_id BIGINT,
    status VARCHAR(255),
    submit_time DATETIME,
    score INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(student_number),
    FOREIGN KEY (task_id) REFERENCES tasks(id)
);

-- 插入基础数据
-- 插入班级数据
INSERT INTO classes (class_id, class_name) VALUES ('class001', '计算机科学与技术1班');
INSERT INTO classes (class_id, class_name) VALUES ('class002', '软件工程1班');

-- 插入用户数据
INSERT INTO users (name, student_number, password, github_id, class_id) VALUES ('OJBKER', 249350208, '123123', '44597827', 'class001');
INSERT INTO users (name, student_number, password, github_id, class_id) VALUES ('测试学生', 123456, 'test123', NULL, 'class001');

-- 插入任务数据
INSERT INTO tasks (title, description, class_id) VALUES ('数学作业1', '完成第一章习题', 'class001');
INSERT INTO tasks (title, description, class_id) VALUES ('英语作业1', '完成课文背默写', 'class001');
INSERT INTO tasks (title, description, class_id) VALUES ('物理实验报告', '完成光学实验报告', 'class001');

-- 插入用户任务数据
INSERT INTO user_tasks (user_id, task_id, status, submit_time, score) VALUES (249350208, 1, '已完成', '2024-09-29 10:30:00', 95);
INSERT INTO user_tasks (user_id, task_id, status, submit_time, score) VALUES (249350208, 2, '进行中', NULL, NULL);
INSERT INTO user_tasks (user_id, task_id, status, submit_time, score) VALUES (123456, 1, '已完成', '2024-09-29 14:20:00', 88);