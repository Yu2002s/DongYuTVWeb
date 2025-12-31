async function initAnHuiTVLive() {
   return fetch("https://mapi.ahtv.cn/api/v1/channel.php?is_audio=0&category_id=1%2C2", {
      headers: {
        accept: "application/json, text/javascript, */*; q=0.01",
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
      },
      body: null,
      method: "GET",
    })
    .then((res) => res.json())
    .then((res) => {
        window.initAnHuiTimeStamp = Date.now()
        window.channelList_anhui = res
    })
}

(async function() {
    await initAnHuiTVLive()
    const channelItem = channelList_anhui.find(item => item.name === '{{channelName}}')
    playLive(channelItem.m3u8)
})();


