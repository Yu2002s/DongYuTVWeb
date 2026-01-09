(async function() {
    const ts = window.initTimeStamp || 0
    // 判断时间间隔是否超过30分钟
    if (Date.now() - ts > 30 * 1000 * 60) {
        // 进行初始化操作
        await initHenanLiveList()
    }
    // 通过频道名称取值
    const channelItem = window.channelList_henan.find(item => item.name === '{{channelName}}')
    console.log('video_streams: ' + channelItem.video_streams[0])
    playLive(channelItem.video_streams[0], {
       'X-Referer': 'https://static.hntv.tv/',
       'Accept': '*/*',
       'Accept-Language': 'zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7',
    })
})();