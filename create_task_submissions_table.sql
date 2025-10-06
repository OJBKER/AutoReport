-- 创建任务提交记录表 task_submissions
CREATE TABLE IF NOT EXISTS task_submissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_url TEXT,                    -- 存储任务的URL
    student_number BIGINT,            -- 内部学生的学号
    github_id TEXT,                   -- GitHub的ID
    update_time DATETIME,             -- 更新时间
    task_id BIGINT,                   -- 任务ID (引用 tasks.id)
    ai_context_url TEXT,              -- 存储和AI对话的上下文记录的URL
    user_id BIGINT,                   -- 用户学号 (引用 users.student_number，与user_tasks表保持一致)
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (user_id) REFERENCES users(student_number)
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_task_submissions_task_id ON task_submissions(task_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_user_id ON task_submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_student_number ON task_submissions(student_number);
CREATE INDEX IF NOT EXISTS idx_task_submissions_github_id ON task_submissions(github_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_update_time ON task_submissions(update_time);

-- 创建复合索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_task_submissions_user_task ON task_submissions(user_id, task_id);