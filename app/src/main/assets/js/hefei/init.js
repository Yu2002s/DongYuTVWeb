(async function() {
    const url = `https://app.hfbtc.cn/shows/2/{{id}}.html`

    const res = await HttpUtil.get(url)

    const div = document.createElement('div')

    div.innerHTML = res.data

    document.head.appendChild(div)

    const source = document.querySelector('source')

    playLive(source.src)

    div.remove()
})();

