function getJiangXiLiveUrl(m3u8) {
    const now = Date.now().toString().slice(0, 10)

    const e = {
      data: {
        t: now,
        stream: m3u8
      },
      headers: {}
    }
    const arr = [ "5481460KidgcB", "7GnJcxU", "etag", "then", "4IynSXa", "headers", "baseURL", "464215UvoJZF", "floor", "data", "use", "random", "Authorization", "GPU", "1DSCooS", "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678oOLl9gqVvUuI1", "465783sjOLxi", "1528461MWDcbT", "request", "553286YKZtuN", "6CqaqwS", "catch", "defaults", "post", "reject", "log", "195239zAZOMP", "1862680bLwhJK", "stream" ]

    function t(n) {
      return arr[n - 405]
    }

    for (var n = t(420), r = n.length, s = "", o = 0; o < 8; o++) s += n.charAt(Math[t(413)](Math[t(416)]() * r));
    var i = "";
    [53, 18, 31, 11, 21, 13, 14, 49, 15, 36, 19, 26, 24].forEach((e => {
      i += n.charAt(e);
    })), e[t(410)][t(417)] = CryptoJS.MD5(e[t(414)].t + e.data.stream + s + i).toString(), e[t(410)][t(407)] = s
      // e[t(410)][t(418)] = jl + ", " + Yl;

    fetch("https://cdnauth.jxgdw.com/liveauth/pc", {
      "headers": {
        "accept": "application/json, text/plain, */*",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "authorization": e.headers.Authorization,
        "content-type": "application/json",
        "etag": e.headers.etag,
        "gpu": "Google Inc. (Intel), ANGLE (Intel, Intel(R) UHD Graphics (0x00009A60) Direct3D11 vs_5_0 ps_5_0, D3D11)",
        "priority": "u=1, i",
        "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "cross-site",
        "Referer": "https://www.jxntv.cn/"
      },
      // "body": `{\"t\":${now},\"stream\":\"tv_jxtv1.m3u8\",\"uuid\":\"04206c0b50f5\"}`,
      "body": {
          t: now,
          stream: m3u8,
          uuid: "04206c0b50f5"
      },
      "method": "POST"
    }).then(res => {
      return res.json()
    }).then(res => {
        const playUrl = `https://yun-live.jxtvcn.com.cn/live-jxtv/tv_jxtv1.m3u8?source=pc&t=${res.t}&token=${res.token}&uuid=04206c0b50f5`
        console.log('playUrl:' + playUrl)
        playLive(playUrl, {
            "X-Referer": "https://www.jxntv.cn/"
        })
    })
}

const m3u8 = '{{m3u8Name}}'

console.log('m3u8: ' + m3u8)

getJiangXiLiveUrl(m3u8)