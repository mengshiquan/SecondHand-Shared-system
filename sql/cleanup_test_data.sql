-- Cleanup all smoke-test data (users, products, categories and related rows)
DROP TEMPORARY TABLE IF EXISTS tmp_user_ids, tmp_user_ids2, tmp_prod_ids;
CREATE TEMPORARY TABLE tmp_user_ids AS
SELECT id FROM t_user WHERE username LIKE 'smoke%' OR username LIKE 'tmpbuy%';
CREATE TEMPORARY TABLE tmp_user_ids2 AS
SELECT id FROM tmp_user_ids;

CREATE TEMPORARY TABLE tmp_prod_ids AS
SELECT id FROM t_product
WHERE title LIKE 'smoke%' OR title LIKE 'dbg%' OR title LIKE 'adminProd%' OR title LIKE 'tmp%'
   OR user_id IN (SELECT id FROM tmp_user_ids);

DELETE FROM t_comment WHERE product_id IN (SELECT id FROM tmp_prod_ids) OR user_id IN (SELECT id FROM tmp_user_ids);
DELETE FROM t_favorite WHERE product_id IN (SELECT id FROM tmp_prod_ids) OR user_id IN (SELECT id FROM tmp_user_ids);
DELETE FROM t_cart WHERE product_id IN (SELECT id FROM tmp_prod_ids) OR user_id IN (SELECT id FROM tmp_user_ids);
DELETE o FROM t_order o
 LEFT JOIN tmp_user_ids ub ON o.buyer_id = ub.id
 LEFT JOIN tmp_user_ids2 us ON o.seller_id = us.id
 WHERE o.product_id IN (SELECT id FROM tmp_prod_ids) OR ub.id IS NOT NULL OR us.id IS NOT NULL;
DELETE FROM t_notification WHERE user_id IN (SELECT id FROM tmp_user_ids);
DELETE FROM t_address WHERE user_id IN (SELECT id FROM tmp_user_ids);
DELETE cpl FROM t_complaint cpl
 LEFT JOIN tmp_user_ids r ON cpl.reporter_id = r.id
 LEFT JOIN tmp_user_ids2 t ON cpl.target_user_id = t.id
 WHERE r.id IS NOT NULL OR t.id IS NOT NULL;
DELETE FROM t_appeal WHERE user_id IN (SELECT id FROM tmp_user_ids);
DELETE FROM t_product WHERE id IN (SELECT id FROM tmp_prod_ids);
DELETE FROM t_user WHERE id IN (SELECT id FROM tmp_user_ids);
DELETE FROM t_category WHERE name LIKE 'dbg%' OR name LIKE 'smokeMain%' OR name LIKE 'smokeSub%' OR name = 'smokeThird';

DROP TEMPORARY TABLE tmp_user_ids, tmp_user_ids2, tmp_prod_ids;

SELECT (SELECT COUNT(*) FROM t_user WHERE username LIKE 'smoke%' OR username LIKE 'tmpbuy%') AS leftover_users,
       (SELECT COUNT(*) FROM t_product WHERE title LIKE 'smoke%' OR title LIKE 'dbg%' OR title LIKE 'adminProd%' OR title LIKE 'tmp%') AS leftover_products,
       (SELECT COUNT(*) FROM t_category WHERE name LIKE 'dbg%' OR name LIKE 'smokeMain%' OR name LIKE 'smokeSub%') AS leftover_categories;
