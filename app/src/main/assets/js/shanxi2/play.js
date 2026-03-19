(async function() {
    const name = '{{channelName}}'
    if (!liveList_shanxi2) {
        await getLiveList()
    }
    const liveItem = liveList_shanxi2.find(item => item.name === name)
    playLive(liveItem.m3u8)
})()