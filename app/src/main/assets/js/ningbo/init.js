async function getLiveConfig() {
  return fetch(`https://web.ncmc.nbtv.cn/vms/site/nbtv/media/playerJson/liveChannel/9ebadf3777b14e0eac6cc99509ae0493_PlayerParamProfile.json`, {
    "headers": {
      "accept": "*/*",
      "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
      "cache-control": "no-cache",
      "content-type": "application/x-www-form-urlencoded",
      "pragma": "no-cache",
      "sec-ch-ua": "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
      "sec-ch-ua-mobile": "?0",
      "sec-ch-ua-platform": "\"Windows\"",
      "sec-fetch-dest": "empty",
      "sec-fetch-mode": "cors",
      "sec-fetch-site": "same-site",
      "Referer": "https://www.ncmc.nbtv.cn/"
    },
    "body": null,
    "method": "GET"
  }).then(res => {
    return res.json()
  }).then(res => {
    const cdnConfigEncrypt = res.paramsConfig.cdnConfigEncrypt
    return cdnConfigEncrypt
  })
}

async function getLiveUrl_ningbo(id) {
  if (!window.cdnConfigEncrypt) {
    window.cdnConfigEncrypt = await getLiveConfig()
  }

  return fetch("https://em.chinamcloud.com/player/encryptUrl", {
    "headers": {
      "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
      "cache-control": "no-cache",
      "content-type": "application/json;charset=UTF-8",
      "pragma": "no-cache",
      "sec-ch-ua": "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
      "sec-ch-ua-mobile": "?0",
      "sec-ch-ua-platform": "\"Windows\"",
      "sec-fetch-dest": "empty",
      "sec-fetch-mode": "cors",
      "sec-fetch-site": "cross-site",
      "Referer": "https://www.ncmc.nbtv.cn/"
    },
    "body": `{\"url\":\"https://liveplay8.nbtv.cn/live/${id}.m3u8\",\"playType\":\"live\",\"type\":\"cdn\",\"cdnEncrypt\":\"${cdnConfigEncrypt}\",\"cdnIndex\":0}`,
    "method": "POST"
  }).then(res2 => res2.json())
    .then(res2 => {
      return res2.url
    })
} 

;(async function() {
  const id = '{{id}}'
  const url = await getLiveUrl_ningbo(id)
  playLive(url)
})();