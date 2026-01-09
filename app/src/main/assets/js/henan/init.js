// 河南卫视

/**
* 初始化获取直播列表
*/
window.initHenanLiveList = async function() {
    const now = Date.now().toString().slice(0, 10)

    const options = {
      method: 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36',
        'Accept': 'application/json, text/plain, */*',
        'Accept-Language': 'zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7',
        'Origin': 'https://static.hntv.tv',
        'Referer': 'https://static.hntv.tv/',
        'Sec-Fetch-Dest': 'empty',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Site': 'same-site',
        'sec-ch-ua': '"Google Chrome";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
        'sign': CryptoJS.SHA256('6ca114a836ac7d73' + now),// CryptoJS.MD5('6ca114a836ac7d73' + now),
        'timestamp': now,
      }
    };

    return fetch('https://pubmod.hntv.tv/program/getAuth/live/class/program/11/', options)
      .then(response => response.json())
      .then(result => {
            window.initTimeStamp = Date.now(); // 记录初始化的时机
            window.channelList_henan = result // 获取到直播列表
      })
      .catch(error => console.log('error', error));
}

async function initHenanTV() {
    // 进行初始化操作
    await initHenanLiveList()

    // 通过频道名称取值
    const channelItem = window.channelList_henan.find(item => item.name === '{{channelName}}')
    playLive(channelItem.video_streams[0], {
        'X-Referer': 'https://static.hntv.tv/',
        'Accept': '*/*',
        'Accept-Language': 'zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7',
    })
}

initHenanTV()