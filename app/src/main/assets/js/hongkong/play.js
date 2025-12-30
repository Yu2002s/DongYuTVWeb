const player = document.querySelector("#player")

player.src = "https://www.hkstv.tv/webcast/livestream/mutfysrq/playlist.m3u8?hls_ctx=7144stj2"
player.play()

console.log('player:' + player)