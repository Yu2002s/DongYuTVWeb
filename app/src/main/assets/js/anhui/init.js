async function initAnHuiTVLive() {
    if (typeof window.HttpUtil !== 'undefined') {
        return HttpUtil.get('https://mapi.ahtv.cn/api/v1/channel.php?is_audio=0&category_id=1%2C2', {
            headers: {
                accept: "application/json, text/javascript, *; q=0.01",
                "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
                "cache-control": "no-cache",
                pragma: "no-cache",
                "sec-ch-ua":
                  '"Google Chrome";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
                "sec-ch-ua-mobile": "?0",
                "sec-ch-ua-platform": '"Windows"',
                "sec-fetch-dest": "empty",
                "sec-fetch-mode": "cors",
                "sec-fetch-site": "same-site",
                Referer: "https://www.ahtv.cn/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
          },
          responseType: 'json'
        }).then(res => {
            window.initAnHuiTimeStamp = Date.now()
            window.channelList_anhui = res.data
        })
    }

    return fetch('https://mapi.ahtv.cn/api/v1/channel.php?is_audio=0&category_id=1%2C2',{
        method: 'GET',
        headers: {
            accept: "application/json, text/javascript, *; q=0.01",
            "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            "cache-control": "no-cache",
            pragma: "no-cache",
            "sec-ch-ua":
              '"Google Chrome";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": '"Windows"',
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "same-site",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36",
            Referer: "https://www.ahtv.cn/",
            "X-Referer": "https://www.ahtv.cn/"
        },
        Referer: "https://www.ahtv.cn/",
        referrerPolicy: "origin",
          mode: "cors",
          credentials: "include",
    })
    .then(res => res.json())
    .then(res => {
          window.initAnHuiTimeStamp = Date.now()
          window.channelList_anhui = res
    })
}

(async function() {
    await initAnHuiTVLive()
    const channelItem = channelList_anhui.find(item => item.name === '{{channelName}}')
    console.log('channelItem: ' + channelItem.m3u8)
    playLive(channelItem.m3u8)
})();


