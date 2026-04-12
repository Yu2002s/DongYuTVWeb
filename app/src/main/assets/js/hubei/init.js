(function() {
    function fullscreen() {
        const videoDiv = document.querySelector('#player-con')
        if (videoDiv) {
            videoDiv.style.background = "#000"
            videoDiv.style.position = "fixed"
            videoDiv.style.top = "0"
            videoDiv.style.left = "0"
            videoDiv.style['z-index'] = 99999
            videoDiv.style.width = `100vw`
            videoDiv.style.height = `100vh`
            const video = videoDiv.querySelector('video')
            if (video) {
                video.play()
            }
            if (typeof JSBridge !== 'undefined' && JSBridge.hideLoading) {
                 JSBridge.hideLoading()
             }
            return
        }

        setTimeout(() => {
            fullscreen()
        }, 12)
    }

    fullscreen()
})();