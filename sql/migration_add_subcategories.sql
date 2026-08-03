-- 现有数据库迁移：添加子分类支持
-- 在已有 secondhand_db 上执行

ALTER TABLE t_category ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父级分类ID，NULL为一级分类' AFTER name, ADD INDEX idx_parent (parent_id);

-- 更新原有的其他闲置 ID (6 → 7)
UPDATE t_category SET id = 7 WHERE id = 6 AND name = '其他闲置';

-- 添加音乐器材一级分类
INSERT INTO t_category (id, name, icon, sort) VALUES (6, '音乐器材', 'Headset', 6);

-- 二级分类：数码电子 (parent_id=1)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('手机', 1, 'Iphone', 1),
('平板电脑', 1, 'Ipad', 2),
('笔记本电脑', 1, 'Notebook', 3),
('智能手表/手环', 1, 'Watch', 4),
('移动电源/充电器', 1, 'Charger', 5),
('蓝牙耳机/音箱', 1, 'Headset', 6),
('相机/摄像机', 1, 'Camera', 7);

-- 二级分类：图书教材 (parent_id=2)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('教材/教辅', 2, 'Document', 1),
('文学/小说', 2, 'Reading', 2),
('考试用书', 2, 'EditPen', 3),
('专业书籍', 2, 'Collection', 4),
('杂志/期刊', 2, 'Postcard', 5);

-- 二级分类：生活用品 (parent_id=3)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('家居装饰', 3, 'Present', 1),
('厨房用品', 3, 'KnifeFork', 2),
('洗护/个护', 3, 'Soap', 3),
('收纳/整理', 3, 'Box', 4),
('日用杂货', 3, 'Goods', 5);

-- 二级分类：服饰鞋包 (parent_id=4)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('男装', 4, 'Male', 1),
('女装', 4, 'Female', 2),
('鞋子', 4, 'Shoe', 3),
('箱包', 4, 'Briefcase', 4),
('配饰/首饰', 4, 'MagicStick', 5);

-- 二级分类：运动户外 (parent_id=5)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('健身器材', 5, 'Bell', 1),
('球类', 5, 'Basketball', 2),
('户外装备', 5, 'Sunny', 3),
('骑行', 5, 'Bicycle', 4),
('游泳', 5, 'Ship', 5);

-- 二级分类：音乐器材 (parent_id=6)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('吉他', 6, 'Headset', 1),
('钢琴/电子琴', 6, 'Headset', 2),
('打击乐器', 6, 'Headset', 3),
('管乐器', 6, 'Headset', 4),
('其他乐器', 6, 'Headset', 5);

-- 二级分类：其他闲置 (parent_id=7)
INSERT INTO t_category (name, parent_id, icon, sort) VALUES
('玩具/玩偶', 7, 'Present', 1),
('礼品/工艺品', 7, 'Collection', 2),
('票券/卡券', 7, 'Ticket', 3),
('其他', 7, 'More', 4);
