(function() {
    const liveUrl = '{{liveUrl}}'
    playLive(addJsWsQuery(liveUrl), {
        'X-Referer': 'https://live.jstv.com/'
    })
})();