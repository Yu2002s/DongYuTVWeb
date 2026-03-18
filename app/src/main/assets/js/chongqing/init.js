(function() {
    function fullscreen() {
        const videoDiv = document.querySelector('video')
        if (videoDiv) {
            videoDiv.style.background = "#000"
            videoDiv.style.position = "fixed"
            videoDiv.style.top = "0"
            videoDiv.style.left = "0"
            // videoDiv.style.transform = "translateX(-50%)"
            videoDiv.style['z-index'] = 99999
            videoDiv.style.width = `100vw`
            videoDiv.style.height = `100vh`
            return
        }

        setTimeout(() => {
            fullscreen()
        }, 500)
    }

    fullscreen()
})();

let countNum = 1

let timerId = setInterval(() => {
    const video = document.querySelector('video')
    if (video && video.style.width !== '100vw') {
        video.style.width = '100vw'
        video.style.height = '100vh'
    }

    if (countNum ++ > 10) {
        clearInterval(timerId)
    }
}, 500)