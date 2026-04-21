function getLiveList_guangzhou() {
    return fetch("https://gzbn.gztv.com:7443/plus-cloud-manage-app/liveChannel/queryLiveChannelList?type=1", {
      "headers": {
        "accept": "application/json, text/plain, */*",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "cache-control": "no-cache",
        "pragma": "no-cache",
        "sec-ch-ua": "\"Google Chrome\";v=\"147\", \"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"147\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-site",
        "X-Referer": "https://www.gztv.com/"
      },
      "referrer": "https://www.gztv.com/",
      "body": null,
      "method": "GET",
      "mode": "cors",
      "credentials": "omit"
    }).then(res => res.json())
    .then(res => {
        return res.data.map(item => {
            return {
                name: item.name,
                url: item.httpUrl
            }
        })
    })
}

;(async function() {
    const liveList = await getLiveList_guangzhou()
    window.currentTime = Date.now()
    window.liveList = liveList
    const liveItem = liveList.find(item => item.name === '{{channelName}}')

    playLive(liveItem.url, {
        'X-Referer': 'https://www.gztv.com/'
    })
})();