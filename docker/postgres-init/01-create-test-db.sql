-- 테스트 전용 DB 를 함께 만듦
--
-- 테스트는 실제 PostgreSQL 에 붙어서 마이그레이션까지 검증하는데(H2 를 쓰지 않음),
-- 개발용 DB 를 그대로 쓰면 테스트가 개발 데이터를 지워 버림.
-- 그래서 nanumi_test 를 따로 둠 (src/test/resources/application-test.yml 참고)
--
-- 이 파일은 컨테이너를 처음 만들 때 한 번만 돎.
-- 이미 만들어진 볼륨에는 적용되지 않으므로 그때는 docker compose down -v 로 다시 만들어야 함
CREATE DATABASE nanumi_test;
GRANT ALL PRIVILEGES ON DATABASE nanumi_test TO abc;
