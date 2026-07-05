import http from "k6/http";
import { check, sleep } from "k6";

export let options = {
    stages: [
                        { duration: '5m', target: 300 }
                ],
};

export function setup() {
    const loginRes = http.post(
        "http://localhost:8080/api/users/login",
        JSON.stringify({
            loginId: "min3187",
            password: "1111",
            rememberMe: false,
        }),
        { headers: { "Content-Type": "application/json" } }
    );

    check(loginRes, {
        "로그인 성공": (r) => r.status === 200,
    });

    const body = JSON.parse(loginRes.body);
    console.log(`로그인 성공 - userId: ${body.userId}, role: ${body.role}`);

    return { token: body.accessToken };
}

// JwtFilter에서 매 요청마다 userRepository.findById()로 DB를 조회하는 기존 방식
// X-Auth-Version 헤더 없음 → v1(DB 조회) 경로로 동작
// GET /api/users/mypage: 인증 필요 + user:stats 캐시 히트 시 비즈니스 쿼리 최소화
// → 필터의 DB 조회 비용이 지배적으로 드러나 v1/v2 차이 측정에 최적
export default function (data) {
    let res = http.get("http://localhost:8080/api/users/me", {
        headers: {
            Authorization: "Bearer " + data.token,
        },
    });

    check(res, {
        "status is 200": (r) => r.status === 200,
    });
    sleep(1);
}
