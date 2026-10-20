SET NAMES utf8mb4;
START TRANSACTION;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_059_nittaku-acoustic.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_059_nittaku-acoustic.jpg'),
  image_url = 'https://nittakuaustralia.com/products/acoustic',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 59;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_060_timo-boll-caf.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_060_timo-boll-caf.jpg'),
  image_url = 'https://www.butterfly-global.com/en/products/detail/36951.html',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 60;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_061_carbonado-190.png',
  images = JSON_ARRAY('/api/upload/equipment/equipment_061_carbonado-190.png'),
  image_url = 'https://niponsports.com/products/stiga-carbonado-190-blade',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 61;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_062_rozena.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_062_rozena.jpg'),
  image_url = 'https://shop.butterflyonline.com/rozena',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 62;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_063_vega-europe.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_063_vega-europe.jpg'),
  image_url = 'https://rodneystabletennis.co.nz/cdn/shop/products/265031047_1024x1024.jpg?v=1643335217',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 63;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_064_hurricane-8.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_064_hurricane-8.jpg'),
  image_url = 'https://www.ppongsuper.com/product/dhs-hurricane-8-table-tennis-rubber/',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 64;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_065_dynaryz-acc.png',
  images = JSON_ARRAY('/api/upload/equipment/equipment_065_dynaryz-acc.png'),
  image_url = 'https://joola.com/products/dynaryz-acc-table-tennis-rubber',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 65;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_066_wave-drive-neo-3.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_066_wave-drive-neo-3.jpg'),
  image_url = 'https://www.mizuno.com/my/en-us/wave%2Bdrive%2Bneo%2B3%2Bunisex-4571546955063.html',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 66;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_067_attack-excounter-2.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_067_attack-excounter-2.jpg'),
  image_url = 'http://komann.com.hk/cdn/shop/files/1073A002-404.jpg?v=1725338985',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 67;

UPDATE equipment
SET
  main_image = '/api/upload/equipment/equipment_068_racket-bag-pro.jpg',
  images = JSON_ARRAY('/api/upload/equipment/equipment_068_racket-bag-pro.jpg'),
  image_url = 'https://lubeihbts.com/products/dhs-rc530-table-tennis-racket-bag',
  update_time = NOW(),
  updated_at = NOW()
WHERE id = 68;

COMMIT;
