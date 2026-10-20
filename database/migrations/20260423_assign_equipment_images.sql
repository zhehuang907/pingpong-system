-- 为装备分配图片
-- 根据装备名称和品牌匹配合适的图片

USE pingpong_recommendation;

-- 更新蝴蝶 Viscaria (马龙同款)
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_058_viscaria.jpg',
    image_url = '/api/upload/equipment/equipment_058_viscaria.jpg'
WHERE name LIKE '%Viscaria%' OR (brand = '蝴蝶' AND name LIKE '%维斯卡利亚%');

-- 更新红双喜狂飙龙5
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_026_5.png',
    image_url = '/api/upload/equipment/equipment_026_5.png'
WHERE name LIKE '%狂飙龙5%' OR name LIKE '%Hurricane Dragon 5%';

-- 更新红双喜狂飙王（王楚钦同款）
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_028_equipment.png',
    image_url = '/api/upload/equipment/equipment_028_equipment.png'
WHERE name LIKE '%狂飙王%' AND name LIKE '%王楚钦%';

-- 更新蝴蝶樊振东ALC
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_027_alc.jpg',
    image_url = '/api/upload/equipment/equipment_027_alc.jpg'
WHERE name LIKE '%樊振东%' AND name LIKE '%ALC%';

-- 更新蝴蝶樊振东SALC
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_056_salc.jpg',
    image_url = '/api/upload/equipment/equipment_056_salc.jpg'
WHERE name LIKE '%樊振东%' AND name LIKE '%SALC%';

-- 更新斯帝卡OC
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_029_stiga-oc.png',
    image_url = '/api/upload/equipment/equipment_029_stiga-oc.png'
WHERE name LIKE '%斯帝卡%' AND name LIKE '%OC%';

-- 更新尼塔库Acoustic
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_059_nittaku-acoustic.jpg',
    image_url = '/api/upload/equipment/equipment_059_nittaku-acoustic.jpg'
WHERE name LIKE '%尼塔库%' AND name LIKE '%Acoustic%';

-- 更新波尔CAF
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_060_timo-boll-caf.jpg',
    image_url = '/api/upload/equipment/equipment_060_timo-boll-caf.jpg'
WHERE name LIKE '%波尔%' AND name LIKE '%CAF%';

-- 更新Carbonado 190
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_061_carbonado-190.png',
    image_url = '/api/upload/equipment/equipment_061_carbonado-190.png'
WHERE name LIKE '%Carbonado%' AND name LIKE '%190%';

-- 更新Innerforce ALC
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_003_innerforce-alc.jpg',
    image_url = '/api/upload/equipment/equipment_003_innerforce-alc.jpg'
WHERE name LIKE '%Innerforce%' AND name LIKE '%ALC%';

-- 胶皮类装备

-- 更新Tenergy 05
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_004_tenergy-05.png',
    image_url = '/api/upload/equipment/equipment_004_tenergy-05.png'
WHERE name LIKE '%Tenergy%' AND name LIKE '%05%';

-- 更新Rakza 7
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_006_rakza-7.jpg',
    image_url = '/api/upload/equipment/equipment_006_rakza-7.jpg'
WHERE name LIKE '%Rakza%' AND name LIKE '%7%';

-- 更新Rozena
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_062_rozena.jpg',
    image_url = '/api/upload/equipment/equipment_062_rozena.jpg'
WHERE name LIKE '%Rozena%';

-- 更新Vega Europe
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_063_vega-europe.jpg',
    image_url = '/api/upload/equipment/equipment_063_vega-europe.jpg'
WHERE name LIKE '%Vega%' AND name LIKE '%Europe%';

-- 更新狂飙8
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_064_hurricane-8.jpg',
    image_url = '/api/upload/equipment/equipment_064_hurricane-8.jpg'
WHERE name LIKE '%狂飙%' AND name LIKE '%8%';

-- 更新Dignics 09c
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_035_dignics-09c.png',
    image_url = '/api/upload/equipment/equipment_035_dignics-09c.png'
WHERE name LIKE '%Dignics%' AND name LIKE '%09c%';

-- 更新Dynaryz ACC
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_065_dynaryz-acc.png',
    image_url = '/api/upload/equipment/equipment_065_dynaryz-acc.png'
WHERE name LIKE '%Dynaryz%' AND name LIKE '%ACC%';

-- 更新NEO系列
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_036_neo.jpg',
    image_url = '/api/upload/equipment/equipment_036_neo.jpg'
WHERE name LIKE '%NEO%' AND brand = '红双喜';

-- 更新F1
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_039_f1.jpg',
    image_url = '/api/upload/equipment/equipment_039_f1.jpg'
WHERE name LIKE '%F1%' AND brand != '挺拔';

-- 更新Lezoline 11
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_040_lezoline-11.jpg',
    image_url = '/api/upload/equipment/equipment_040_lezoline-11.jpg'
WHERE name LIKE '%Lezoline%' AND name LIKE '%11%';

-- 更新729系列
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_022_729.jpg',
    image_url = '/api/upload/equipment/equipment_022_729.jpg'
WHERE brand = '729' AND name NOT LIKE '%省%' AND name NOT LIKE '%套%';

-- 更新F55
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_023_f55.jpg',
    image_url = '/api/upload/equipment/equipment_023_f55.jpg'
WHERE name LIKE '%F55%';

-- 更新N-10
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_020_n-10.jpg',
    image_url = '/api/upload/equipment/equipment_020_n-10.jpg'
WHERE name LIKE '%N-10%';

-- 更新MC-2
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_021_mc-2.jpg',
    image_url = '/api/upload/equipment/equipment_021_mc-2.jpg'
WHERE name LIKE '%MC-2%';

-- 更新D70
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_031_d70.jpg',
    image_url = '/api/upload/equipment/equipment_031_d70.jpg'
WHERE name LIKE '%D70%';

-- 更新Allround Classic
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_033_allround-classic.png',
    image_url = '/api/upload/equipment/equipment_033_allround-classic.png'
WHERE name LIKE '%Allround%' AND name LIKE '%Classic%';

-- 更新Wave Medal Neo
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_008_wave-medal-neo.png',
    image_url = '/api/upload/equipment/equipment_008_wave-medal-neo.png'
WHERE name LIKE '%Wave%' AND name LIKE '%Medal%';

-- 更新Wave Drive Neo 3
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_066_wave-drive-neo-3.jpg',
    image_url = '/api/upload/equipment/equipment_066_wave-drive-neo-3.jpg'
WHERE name LIKE '%Wave%' AND name LIKE '%Drive%' AND name LIKE '%3%';

-- 更新Attack Excounter 2
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_067_attack-excounter-2.jpg',
    image_url = '/api/upload/equipment/equipment_067_attack-excounter-2.jpg'
WHERE name LIKE '%Excounter%' AND name LIKE '%2%';

-- 更新球包
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_068_racket-bag-pro.jpg',
    image_url = '/api/upload/equipment/equipment_068_racket-bag-pro.jpg'
WHERE name LIKE '%球包%' OR name LIKE '%拍包%' OR name LIKE '%Bag%';

-- 为剩余没有图片的装备分配通用图片
UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_010_equipment.png',
    image_url = '/api/upload/equipment/equipment_010_equipment.png'
WHERE (main_image IS NULL OR main_image = '' OR main_image = '/placeholder.png')
  AND (image_url IS NULL OR image_url = '' OR image_url = '/placeholder.png')
  AND category_id IN (SELECT id FROM category WHERE name LIKE '%底板%');

UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_004_tenergy-05.png',
    image_url = '/api/upload/equipment/equipment_004_tenergy-05.png'
WHERE (main_image IS NULL OR main_image = '' OR main_image = '/placeholder.png')
  AND (image_url IS NULL OR image_url = '' OR image_url = '/placeholder.png')
  AND category_id IN (SELECT id FROM category WHERE name LIKE '%胶皮%');

UPDATE equipment 
SET main_image = '/api/upload/equipment/equipment_009_pro.png',
    image_url = '/api/upload/equipment/equipment_009_pro.png'
WHERE (main_image IS NULL OR main_image = '' OR main_image = '/placeholder.png')
  AND (image_url IS NULL OR image_url = '' OR image_url = '/placeholder.png')
  AND category_id IN (SELECT id FROM category WHERE name LIKE '%球鞋%' OR name LIKE '%服装%');

-- 查询更新结果
SELECT 
    id,
    name,
    brand,
    LEFT(main_image, 50) as main_image,
    LEFT(image_url, 50) as image_url
FROM equipment 
WHERE main_image IS NOT NULL AND main_image != ''
ORDER BY id
LIMIT 30;
