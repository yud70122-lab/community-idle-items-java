-- =============================================
-- 物品表空间索引优化方案
-- MySQL 5.7+ 版本支持
-- =============================================

-- 1. 添加空间字段和GeoHash字段
ALTER TABLE item 
    ADD COLUMN location_point POINT NULL COMMENT '空间坐标点' AFTER location,
    ADD COLUMN geohash VARCHAR(12) NULL COMMENT 'GeoHash编码（6位精度约±0.61km）' AFTER location_point,
    ADD SPATIAL INDEX idx_location_point (location_point),
    ADD INDEX idx_geohash (geohash),
    ADD INDEX idx_status_deleted (status, deleted),
    ADD INDEX idx_category_status (category_id, status);

-- 2. 迁移现有数据，填充location_point和geohash字段
UPDATE item 
SET 
    location_point = CASE 
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL 
        THEN POINT(longitude, latitude) 
        ELSE NULL 
    END,
    geohash = CASE 
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL 
        THEN ST_GEOHASH(POINT(longitude, latitude), 6) 
        ELSE NULL 
    END
WHERE location_point IS NULL OR geohash IS NULL;

-- 3. 创建触发器，插入/更新时自动维护location_point和geohash
DELIMITER //

DROP TRIGGER IF EXISTS tr_item_before_insert //
CREATE TRIGGER tr_item_before_insert
BEFORE INSERT ON item
FOR EACH ROW
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        SET NEW.location_point = POINT(NEW.longitude, NEW.latitude);
        SET NEW.geohash = ST_GEOHASH(POINT(NEW.longitude, NEW.latitude), 6);
    ELSE
        SET NEW.location_point = NULL;
        SET NEW.geohash = NULL;
    END IF;
END //

DROP TRIGGER IF EXISTS tr_item_before_update //
CREATE TRIGGER tr_item_before_update
BEFORE UPDATE ON item
FOR EACH ROW
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        IF NEW.latitude != OLD.latitude OR NEW.longitude != OLD.longitude THEN
            SET NEW.location_point = POINT(NEW.longitude, NEW.latitude);
            SET NEW.geohash = ST_GEOHASH(POINT(NEW.longitude, NEW.latitude), 6);
        END IF;
    ELSE
        SET NEW.location_point = NULL;
        SET NEW.geohash = NULL;
    END IF;
END //

DELIMITER ;

-- =============================================
-- 查询优化示例
-- =============================================

-- 示例1: 5km范围内的物品查询（使用三级过滤）
EXPLAIN ANALYZE
SELECT 
    i.*,
    ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 AS distance
FROM item i
WHERE 
    i.deleted = 0
    AND i.status = 1
    AND i.geohash LIKE CONCAT(LEFT(ST_GEOHASH(POINT(121.4737, 31.2304), 6), 4), '%')
    AND MBRContains(
        ST_Buffer(POINT(121.4737, 31.2304), 5 / 111.0),
        i.location_point
    )
    AND ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 <= 5
ORDER BY distance ASC
LIMIT 20;

-- 示例2: 按距离排序查询（使用空间索引）
EXPLAIN ANALYZE
SELECT 
    i.*,
    ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 AS distance
FROM item i
USE INDEX (idx_location_point)
WHERE 
    i.deleted = 0
    AND i.status = 1
    AND ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 <= 10
ORDER BY distance ASC
LIMIT 50;

-- 示例3: 统计各距离段的物品数量
SELECT 
    CASE 
        WHEN dist <= 1 THEN '0-1km'
        WHEN dist <= 3 THEN '1-3km'
        WHEN dist <= 5 THEN '3-5km'
        WHEN dist <= 10 THEN '5-10km'
        ELSE '10km+'
    END AS distance_range,
    COUNT(*) AS item_count
FROM (
    SELECT 
        ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 AS dist
    FROM item i
    WHERE 
        i.deleted = 0
        AND i.status = 1
        AND i.geohash LIKE CONCAT(LEFT(ST_GEOHASH(POINT(121.4737, 31.2304), 6), 4), '%')
) t
GROUP BY distance_range
ORDER BY MIN(dist);

-- =============================================
-- 性能对比测试SQL
-- =============================================

-- 传统Haversine公式查询（优化前）
SELECT 
    SQL_NO_CACHE
    i.*,
    6371.0 * 2 * ASIN(SQRT(POWER(SIN((31.2304 - i.latitude) * PI() / 180 / 2), 2)
        + COS(31.2304 * PI() / 180) * COS(i.latitude * PI() / 180)
        * POWER(SIN((121.4737 - i.longitude) * PI() / 180 / 2), 2))) AS distance
FROM item i
WHERE 
    i.deleted = 0
    AND i.status = 1
    AND 6371.0 * 2 * ASIN(SQRT(POWER(SIN((31.2304 - i.latitude) * PI() / 180 / 2), 2)
        + COS(31.2304 * PI() / 180) * COS(i.latitude * PI() / 180)
        * POWER(SIN((121.4737 - i.longitude) * PI() / 180 / 2), 2))) <= 5
ORDER BY distance ASC
LIMIT 20;

-- 空间索引优化查询（优化后）
SELECT 
    SQL_NO_CACHE
    i.*,
    ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 AS distance
FROM item i
WHERE 
    i.deleted = 0
    AND i.status = 1
    AND i.geohash LIKE CONCAT(LEFT(ST_GEOHASH(POINT(121.4737, 31.2304), 6), 4), '%')
    AND MBRContains(
        ST_Buffer(POINT(121.4737, 31.2304), 5 / 111.0),
        i.location_point
    )
    AND ST_DISTANCE_SPHERE(POINT(121.4737, 31.2304), i.location_point) / 1000.0 <= 5
ORDER BY distance ASC
LIMIT 20;

-- =============================================
-- 索引使用情况检查
-- =============================================

-- 查看空间索引
SHOW INDEX FROM item WHERE Key_name = 'idx_location_point';

-- 查看索引使用统计
SELECT 
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_READ,
    COUNT_WRITE,
    COUNT_FETCH
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_NAME = 'item'
ORDER BY COUNT_READ DESC;

-- 分析表统计信息
ANALYZE TABLE item;
OPTIMIZE TABLE item;
