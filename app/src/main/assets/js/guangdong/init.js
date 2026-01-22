/**
* 广东卫视
*/
(function() {
    function fullscreen() {
        const video = document.querySelector('#video_html5_api')
        if (video) {
            video.style.top = 0
            video.style.left = 0
            video.style.position = 'fixed'
            video.style['z-index'] = 99999
            video.style.background = '#000'
            const scaleW = screen.width / 580
            const scaleH = screen.height / 326
            const scale = Math.min(scaleW, scaleH)
            const videoWidth = scale * 580
            video.style.width = `${videoWidth}px`
            video.style.height = `${scale * 326}px`
            if (screen.width > videoWidth) {
                video.style.left = `${(screen.width - (videoWidth)) / 2}px`
            }
            document.body.innerHTML = ''
            document.body.appendChild(video)
            document.body.style.width = '100vw'
            document.body.style.height = '100vh'
            document.body.style.background = '#000'
            return
        }
        setTimeout(() => {
            fullscreen()
        }, 12)
    }

    fullscreen()
})();