/**
 * 마이페이지 통계 - 캐시 없는 버전
 * MyPageService.java의 @Cacheable 주석 처리 후 실행
 */
import http from "k6/http";
import { sleep, check } from "k6";

export let options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '30s', target: 50 },
        { duration: '5s', target: 0 },
    ],
};

export function setup() {
    const loginRes = http.post(
        "http://localhost:8080/login",
        JSON.stringify({ loginId: "min3187", password: "1111", rememberMe: false }),
        { headers: { "Content-Type": "application/json" } }
    );
    const body = JSON.parse(loginRes.body);
    return { token: body.accessToken };
}

export default function (data) {
    http.get("http://localhost:8080/api/users/mypage", {
        headers: { Authorization: "Bearer " + data.token },
    });
    sleep(1);
}