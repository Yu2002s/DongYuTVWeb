fetch("https://web.cbg.cn/live/getLiveUrl?url=https%3A%2F%2Fsjlivecdn.cbg.cn%2Fapp_2%2F_definst_%2Fls_3.stream%2Fchunklist.m3u8", {
  "headers": {
    "accept": "*/*",
    "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
    "cache-control": "no-cache",
    "pragma": "no-cache",
    "priority": "u=1, i",
    "sec-ch-ua": "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
    "sec-ch-ua-mobile": "?0",
    "sec-ch-ua-platform": "\"Windows\"",
    "sec-fetch-dest": "empty",
    "sec-fetch-mode": "cors",
    "sec-fetch-site": "same-site"
  },
  "referrer": "https://www.cbg.cn/web/list/4918/1.html?5DEFC70C1A4AOD5173881C62BD4ACAD0",
  "body": null,
  "method": "GET",
  "mode": "cors",
  "credentials": "omit"
}).then(res => res.json())
.then(res => {
    playLive(res.data.url)
})