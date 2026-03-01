const classNames = ['newtopbz'];
 classNames.forEach(className => {
     Array.from(document.getElementsByClassName(className)).forEach(div => {
         div.style.display = 'none'
     });
 });

(function AutoFullscreen() {
    var player = document.querySelector('#h5player_player');
    if (player != null) {
        player.style.position = 'fixed'
        player.style.left = 0
        player.style.top = 0
        player.style.width = '100vw'
        player.style.height = '100vh'
        player.style.zIndex = 9999
    } else {
        setTimeout(function() {
            AutoFullscreen();
        }, 16)
    }
})();