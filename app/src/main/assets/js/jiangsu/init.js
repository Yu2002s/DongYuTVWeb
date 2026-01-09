function addJsWsQuery(url) {
    // HCPMPKxQNrKAyjzR67JGjswspro6954c5ba
    // jsws4kpro
    const regex = /\/(\w+).m3u8/
    const id = url.match(regex)[1]
    const key = 'tJanAHkyGtaifaQG4dWe' + id // 固定
    const salt = Math.floor(Date.now() / 1e3) + 180
    const txTime = salt.toString(16)
    const txSecret = CryptoJS.MD5(key + txTime).toString()
    return url + '?txSecret=' + txSecret + '&txTime=' + txTime
}

(function() {
    const liveUrl = '{{liveUrl}}'
    const url = addJsWsQuery(liveUrl)
    // https://litchi-play-encrypted-site.jstv.com/applive/jswspro.m3u8
    console.log('url:' + url)
    playLive(url, {
        'X-Referer': 'https://live.jstv.com/'
    })
})();