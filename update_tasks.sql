-- 更新tasks表内容为实验报告类似的内容
UPDATE tasks SET 
    title = '计算机网络实验报告', 
    description = '完成TCP/IP协议分析实验，包括数据包捕获与分析，撰写详细的实验过程和结果报告'
WHERE id = 1;

UPDATE tasks SET 
    title = '数据库系统实验报告', 
    description = '设计并实现一个学生管理系统数据库，包括表结构设计、SQL查询优化和事务处理，提交完整的实验报告'
WHERE id = 2;

UPDATE tasks SET 
    title = '操作系统实验报告', 
    description = '实现进程调度算法（FCFS、SJF、时间片轮转），分析不同算法的性能表现，撰写实验报告并提交源代码'
WHERE id = 3;