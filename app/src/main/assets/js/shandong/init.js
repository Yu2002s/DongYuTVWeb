const key = 'BDNZNQSRYWXYCKNA'
const iv = '0000000000000000'
const salt = 'GBHWERMTGPHFELSJ'

function decrypt(d) {
  const a = CryptoJS.AES.decrypt(d, CryptoJS.enc.Utf8.parse(key), {
    iv: CryptoJS.enc.Utf8.parse(iv),
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  });
  const b = a.toString(CryptoJS.enc.Utf8);
  return b;
}

function encrypt(c) {
  if (!c) {
    return "".split("").reverse().join("");
  }
  return CryptoJS.AES.encrypt(c, CryptoJS.enc.Utf8.parse(key), {
    iv: CryptoJS.enc.Utf8.parse(iv),
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  }).toString();
}

async function getShanDongLiveUrl(channelId) {
    const now = Date.now()
    const s = CryptoJS.MD5(channelId + now + salt)
    console.log('s: ' + s)
    const body = encrypt(JSON.stringify({
      'channelMark': channelId
    }))
    // console.log('body: ' + body)
    // console.log(`https://feiying.litenews.cn/api/v1/auth/exchange?t=${now}&s=${s}`)

    if (typeof window.HttpUtil !== 'undefined') {
        const response = await HttpUtil.post(`https://feiying.litenews.cn/api/v1/auth/exchange?t=${now}&s=${s}`, body, {
            headers: {
                "accept": "*",
                "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
                "content-type": "text/plain",
                "priority": "u=1, i",
                "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
                "sec-ch-ua-mobile": "?0",
                "sec-ch-ua-platform": "\"Windows\"",
                "sec-fetch-dest": "empty",
                "sec-fetch-mode": "cors",
                "sec-fetch-site": "cross-site",
                "Referer": "https://v.iqilu.com/"
            },
            responseType: 'text'
        })
        const result = JSON.parse(decrypt(response.data))
        playLive(result.data)
    } else {
        const response = await fetch(`https://feiying.litenews.cn/api/v1/auth/exchange?t=${now}&s=${s}`, {
            method: 'POST',
            headers: {
                "accept": "*",
                "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
                "content-type": "text/plain",
                "priority": "u=1, i",
                "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
                "sec-ch-ua-mobile": "?0",
                "sec-ch-ua-platform": "\"Windows\"",
                "sec-fetch-dest": "empty",
                "sec-fetch-mode": "cors",
                "sec-fetch-site": "cross-site",
                "Referer": "https://v.iqilu.com/",
                "X-Referer": "https://v.iqilu.com/",
                "X-Body": body,
            },
            body,
        }).then(res => {
            return res.text()
        })

         // console.log('response:' + response)
         const result = JSON.parse(decrypt(response))
         playLive(result.data)
    }
}

const channelId = '{{channelId}}'

// console.log('channelId:' + channelId)

getShanDongLiveUrl(channelId)