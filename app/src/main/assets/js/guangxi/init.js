/**
* 广西卫视
*/
(function() {
    function fullscreen() {
        const videoDiv = document.querySelector('#dhc-video')
        if (videoDiv) {
            videoDiv.style.background = "#000"
            videoDiv.style.position = "fixed"
            videoDiv.style.top = "0"
            videoDiv.style.left = "50%"
            videoDiv.style.transform = "translateX(-50%)"
            videoDiv.style['z-index'] = 99999
            const scaleW = screen.width / 940
            const scaleH = screen.height / 570
            const scale = Math.min(scaleW, scaleH)
            videoDiv.style.width = `${scale * 940}px`
            videoDiv.style.height = `${scale * 570}px`
            return
        }

        setTimeout(() => {
            fullscreen()
        }, 12)
    }

    fullscreen()
})();

;(function() {
    let i = 0
    const timerId = setInterval(() => {
        const nav = document.querySelector('.Gxntv_nav')
        if (nav && nav.style.display !== 'none') {
            nav.style.display = 'none'
        }
        const liveList = document.querySelector('.Live_list')
        if (liveList && liveList.style.display !== 'none') {
            liveList.style.display = 'none'
        }
        const header = document.querySelector('.Header')
        if (header && header.style.display !== 'none') {
            header.style.display = 'none'
        }
        if (i++ > 20) {
            clearInterval(timerId)
        }
    }, i * 100)
})();