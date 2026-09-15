-- email/display_name을 AES-GCM으로 암호화해서 저장하면 평문보다 길어진다
-- (nonce 12바이트 + GCM 태그 16바이트 + Base64 인코딩, 개인정보 처리 방침 9/13).
-- 기존 email VARCHAR(320)/display_name VARCHAR(100)로는 넘칠 수 있어 넉넉히 넓힌다.
-- uk_member_email/uk_member_provider 유니크 제약은 컬럼 재정의 없이 그대로 유지된다.
ALTER TABLE member MODIFY COLUMN email VARCHAR(500) NOT NULL;
ALTER TABLE member MODIFY COLUMN display_name VARCHAR(500) NOT NULL;
