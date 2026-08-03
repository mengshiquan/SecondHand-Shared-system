-- 简化迁移：仅插入子分类（主分类已存在）
USE secondhand_db;

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('手机', 1, 'Iphone', 1),
('平板电脑', 1, 'Ipad', 2),
('笔记本电脑', 1, 'Notebook', 3),
('智能手表/手环', 1, 'Watch', 4),
('移动电源/充电器', 1, 'Charger', 5),
('蓝牙耳机/音箱', 1, 'Headset', 6),
('相机/摄像机', 1, 'Camera', 7);

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('教材/教辅', 2, 'Document', 1),
('文学/小说', 2, 'Reading', 2),
('考试用书', 2, 'EditPen', 3),
('专业书籍', 2, 'Collection', 4),
('杂志/期刊', 2, 'Postcard', 5);

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('家居装饰', 3, 'Present', 1),
('厨房用品', 3, 'KnifeFork', 2),
('洗护/个护', 3, 'Soap', 3),
('收纳/整理', 3, 'Box', 4),
('日用杂货', 3, 'Goods', 5);

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('男装', 4, 'Male', 1),
('女装', 4, 'Female', 2),
('鞋子', 4, 'Shoe', 3),
('箱包', 4, 'Briefcase', 4),
('配饰/首饰', 4, 'MagicStick', 5);

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('健身器材', 5, 'Bell', 1),
('球类', 5, 'Basketball', 2),
('户外装备', 5, 'Sunny', 3),
('骑行', 5, 'Bicycle', 4),
('游泳', 5, 'Ship', 5);

INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('玩具/玩偶', 7, 'Present', 1),
('礼品/工艺品', 7, 'Collection', 2),
('票券/卡券', 7, 'Ticket', 3),
('其他', 7, 'More', 4);
