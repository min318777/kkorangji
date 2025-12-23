# k6 부하 테스트 가이드

## 📁 테스트 파일 설명

### 1. `smoke-test.js` - 스모크 테스트
기본 기능이 정상 작동하는지 확인하는 가벼운 테스트

**실행 방법:**
```bash
k6 run load-tests/smoke-test.js
```

**용도:**
- 배포 전 기본 기능 확인
- API 엔드포인트가 정상인지 체크

---

### 2. `like-test.js` - 좋아요 동시성 테스트
좋아요 기능의 동시성 이슈를 테스트

**실행 방법:**
```bash
k6 run load-tests/like-test.js
```

**체크 포인트:**
- 중복 좋아요가 생성되는지 확인
- Race Condition 발생 여부
- 응답 시간 측정

**수정 필요:**
```javascript
// 실제 테스트 계정과 게시글 ID로 변경
const POST_ID = 1;  // ← 실제 게시글 ID
loginId: 'testuser',  // ← 실제 계정
password: 'password123'  // ← 실제 비밀번호
```

---

### 3. `view-count-test.js` - 조회수 정확성 테스트
조회수 증가 로직의 Lost Update 문제 테스트

**실행 방법:**
```bash
k6 run load-tests/view-count-test.js
```

**체크 포인트:**
- 조회수가 정확히 카운트되는지
- 동시 조회 시 조회수 손실 여부
- 테스트 종료 후 실제 조회수와 예상 조회수 비교

---

## 🚀 실행 순서

### 1단계: 애플리케이션 실행
```bash
# Docker로 실행
docker-compose -f docker-compose.local.yml up -d

# 또는 Gradle로 실행
./gradlew :api:bootRun
```

### 2단계: 테스트 데이터 준비
1. 테스트 계정 생성
2. 게시글 1개 이상 작성
3. 게시글 ID 확인

### 3단계: 스모크 테스트 (기본 확인)
```bash
k6 run load-tests/smoke-test.js
```
→ 모든 체크가 성공하는지 확인

### 4단계: 조회수 테스트
```bash
# 테스트 전 조회수 확인
curl http://localhost:8080/api/meow/boast-cat/1

# 부하 테스트 실행
k6 run load-tests/view-count-test.js

# 테스트 후 조회수 재확인 (손실 여부 체크)
```

### 5단계: 좋아요 동시성 테스트
```bash
k6 run load-tests/like-test.js

# DB에서 중복 좋아요 확인
# SELECT user_id, boast_cat_post_id, COUNT(*)
# FROM post_like
# GROUP BY user_id, boast_cat_post_id
# HAVING COUNT(*) > 1;
```

---

## 📊 결과 해석

### 성공적인 테스트
```
✓ http_req_duration..........: avg=150ms  p(95)=300ms
✓ http_req_failed............: 0.00%
✓ 좋아요 요청 성공...........: 100.00%
```

### 동시성 이슈 발생 시
```
✗ duplicate_likes............: 150 (중복 좋아요 발견!)
✗ http_req_failed............: 5.23% (일부 요청 실패)
⚠️  조회수 예상: 3000, 실제: 2847 (153개 손실)
```

---

## 🔧 부하 조절

### 가벼운 테스트 (로컬 개발)
```javascript
export const options = {
  vus: 10,
  duration: '30s',
};
```

### 중간 부하 (통합 테스트)
```javascript
export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 0 },
  ],
};
```

### 고부하 (스트레스 테스트)
```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 300 },
    { duration: '5m', target: 300 },
    { duration: '2m', target: 0 },
  ],
};
```

---

## 💡 팁

1. **테스트 전 DB 백업**
   - 부하 테스트는 실제 데이터를 생성합니다
   - 로컬 테스트 DB 사용 권장

2. **점진적으로 부하 증가**
   - 처음부터 높은 부하는 금지
   - 10 → 50 → 100 순서로 증가

3. **모니터링 함께 실행**
   - 애플리케이션 로그 확인
   - DB 커넥션 풀 상태 모니터링
   - CPU/메모리 사용률 체크

4. **실패 원인 분석**
   - `summary.json` 파일 확인
   - 애플리케이션 로그에서 에러 찾기
   - DB에서 중복 데이터 쿼리

---

## 🎯 기대 결과

### 동시성 이슈 수정 전
- 좋아요 중복 생성 발생
- 조회수 손실 발생
- 일부 요청 실패

### 동시성 이슈 수정 후
- 중복 좋아요 0건
- 조회수 정확히 일치
- 모든 요청 성공
- 응답 시간 안정적
