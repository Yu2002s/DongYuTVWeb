;(async function() {
    if (!window.liveList || Date.now() - (window.currentTime || 0) > 120000) {
        window.liveList = await getLiveList_guangzhou()
    }
    const liveItem = liveList.find(item => item.name === '{{channelName}}')

    playLive(liveItem.url, {
        'X-Referer': 'https://www.gztv.com/'
    })
})();