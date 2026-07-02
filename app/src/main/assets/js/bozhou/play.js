;(async function() {
    const id = '{{id}}'
    if (!window.liveList_BoZhou) {
        await playLiveBoZhou(id)
    }
    const item = window.liveList_BoZhou.find(item => item.id === id)
    playLive(item.playUrl.split(',')[1])
})();
