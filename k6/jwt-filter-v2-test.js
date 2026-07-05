import http from "k6/http";
import { check, sleep } from "k6";

export let options = {
    stages: [
                    { duration: '5m', target: 300 }
            ],
};

// setup() 단계에서 로그인하여 Access Token을 자동으로 발급받는다
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

// JwtFilter에서 DB 조회 없이 JWT Claims(userId, role, permissions)만으로 인증
// X-Auth-Version: v2 헤더 → v2(토큰 추출) 경로로 동작
// GET /api/users/mypage: 인증 필요 + user:stats 캐시 히트 시 비즈니스 쿼리 최소화
// → 필터의 DB 조회 비용이 지배적으로 드러나 v1/v2 차이 측정에 최적
export default function (data) {
    let res = http.get("http://localhost:8080/api/users/me", {
        headers: {
            Authorization: "Bearer " + data.token,
            "X-Auth-Version": "v2",
        },
    });

    check(res, {
        "status is 200": (r) => r.status === 200,
    });
    sleep(1);
}
