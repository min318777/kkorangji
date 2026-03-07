import http from "k6/http";
import { sleep, check } from "k6";

export let options = {
    stages: [
                        { duration: '10m', target: 6000 }
                ],
};

// setup() 단계에서 로그인하여 Access Token을 자동으로 발급받는다
export function setup() {
    const loginRes = http.post(
        "http://localhost:8080/login",
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
// DB 쿼리 없는 경량 엔드포인트로 순수 JwtFilter 성능만 측정
export default function (data) {
    let res = http.get("http://localhost:8080/api/auth/test", {
        headers: {
            Authorization: "Bearer " + data.token,
        },
    });

    check(res, {
        "status is 200": (r) => r.status === 200,
    });

    sleep(1);
}
