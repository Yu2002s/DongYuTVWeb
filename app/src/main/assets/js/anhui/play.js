(async function() {
    const ts = window.initAnHuiTimeStamp || 0
    // 判断时间间隔是否超过30分钟
    if (Date.now() - ts > 30 * 1000 * 60) {
        // 进行初始化操作
        await initAnHuiTVLive()
    }
    // 通过频道名称取值
    const channelItem = window.channelList_anhui.find(item => item.name === '{{channelName}}')
    console.log('video_streams: ' + channelItem.m3u8)
    playLive(channelItem.m3u8)
})();