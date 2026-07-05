import http from "k6/http";
import { sleep } from "k6";

export let options = {
    stages: [
                { duration: '5m', target: 200 }
        ],
};

export default function () {
    http.post("http://localhost:8080/api/meow/boast-cat/147516/view");
    sleep(1);
}
