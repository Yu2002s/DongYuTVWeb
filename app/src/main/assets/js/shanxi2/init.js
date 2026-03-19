function decrypt(data, key, iv) {

    return CryptoJS.AES.decrypt(data, CryptoJS.enc.Utf8.parse(key), {
            iv: CryptoJS.enc.Utf8.parse(iv),
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.ZeroPadding
        }).toString(CryptoJS.enc.Utf8)
}

async function getLiveList() {
    return fetch("http://toutiao.cnwest.com/static/v1/group/stream.js", {
      "headers": {
        "accept": "*/*",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "cache-control": "no-cache",
        "pragma": "no-cache",
        "X-Referer": "http://live.snrtv.com/"
      },
      "referrer": "http://live.snrtv.com/",
      "body": null,
      "method": "GET",
      "mode": "cors",
      "credentials": "omit"
    }).then(res => {
        return res.text()
    }).then(res => {
        window.eval(res)
        const d = decrypt(sTV.substring(16), sTV.substring(0, 16), sRadio.substring(0, 16))
        const sxbc = JSON.parse(d).sxbc
        const arr = []
        for (let key in sxbc) {
            arr.push(sxbc[key])
        }
        window.liveList_shanxi2 = arr
    })
}

(async function() {
    const name = '{{channelName}}'
    await getLiveList()
    const liveItem = liveList_shanxi2.find(item => item.name === name)
    playLive(liveItem.m3u8)
})()