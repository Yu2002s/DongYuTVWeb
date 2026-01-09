## DongYuTVWeb

轻松在电视上观看直播，操作简单，适合家中老人使用，使用 WebView JavaScript 注入方式实现

## 为什么要做这个项目

随着短视频平台的兴起，出现了各种各样的短剧，实在不想家里老人天天看那些雷人的短剧。加上现在的网络电视，想看以前的闭路电视的那些
频道太困难，各种广告，而且对于老年人来说，也根本不会操作，导致了虽然家里有电视，但是年轻人没在家根本没法使用，就闲置着。
当然电视上播放的内容也有些问题，比如那些《做出了一个违背祖宗的决定》之类的广告，也是无处不在！

## 操作

- 方向上: 上一个频道
- 方向下: 下一个频道
- 左键: 声音减
- 右键: 声音加
- 确定键: 播放/暂停
- 数字键: 换台
- 菜单键：选择换台
- 返回键：返回/退出

> 使用老式的闭路电视操作，适合老人使用！

## 自定义频道

频道完整配置文件 `main/assets/live.jsonc` [live.jsonc](https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/live.jsonc)

### 1. 对于普通不懂开发的用户

修改 channel 频道列表

```json5
{
    // ...
    "channel": [
        // ...
        {
            "channelType": "xxx", // 频道类型（分类名称）
            "player": "common", // 播放器 id，这里写上，下方的 channelList 中没有设置的就默认为这个值
            "channelList": [
              {
                "channelName": "香港卫视", // 频道名称 (需要唯一)
                "tvLogo": "", // logo 目前没用到，可以不写
                "number": 6, // 填写唯一的频道号码，用于数字换台
                "player": "common", // 播放器 id，这个上方已经内置了，只需要写 common
                "args": { // 播放参数，固定 liveUrl 即可
                  "liveUrl": "https://wwww.tv.com/playlist.m3u8?"
                }
              }
            ]
        }
    ]
}
```

> 注意上方的 `liveUrl` 只能是普通的 m3u8地址，并且没有带时间戳等相关的参数

### 2. 对于开发人员

有些直播地址是有验证参数的，需要在脚本中进行处理生成

增加自定义频道，修改 `channel` 配置

```json5
// 省略其他
{
  // ...
  "channel": [
    // ...
    {
      "channelType": "xx电视分类",
      "player": "custom", // 设置自定义播放 id
      "channelList": [
        // ...
        {
          "channelName": "xx电视",
          "tvLogo": "https://web.cmc.hebtv.com/cms/rmt0336/upload/Image/9/9tpklm/2025/07/18/30aaea69a1de46a881cef5d759d30b3e.png",
          "number": 23
        }
        // ...
      ]
    }
  ]
}
```

### 修改 `player` 配置

```json5
{
  "player": {
    "id": "custom", // 播放器 id，和上面定义的频道 player 参数一致
    "name": "simple", // 内置的播放器名称，做了特殊处理，默认写这个
    "script": { // 需要执行的 js 脚本
      "init": [ // 在网页初始化完成时候执行的 js 脚本
        "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/js/hebei/init.js"
      ],
      "play": [ // 响应播放事件执行的 js 脚本
        "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/js/hebei/play.js"
      ],
      "resume_pause": [] // 暂停恢复执行的 js 脚本，这里留空，因为内部已经做了处理了
    }
  }
}
```

### 编辑 js 脚本

> 不需要云服务器，在 gitee 上建一个仓库，上面脚本地址自己写上仓库中实际的地址即可，具体格式参照上面的

> simple 播放器已经内置了 CryptoJS，所以这里就不需要引入了，直接使用即可

> 脚本中，`{{channelName}}` 会被替换成用户选择的频道名称，当然其他 args 参数也会被这样替换进入

> 内部已经关闭了跨域限制，可以直接请求

> `playLive` 是内置的方法，可以直接调用传入地址进行播放

#### `init.js`, 对应上面 init 执行的脚本

```js
(function() {
    // 这里在播放地址上面加上需要的参数
    window.addLiveUrlQuery = function (obj) {
       let ts = parseInt(new Date().getTime() / (1000)) + (7200);
       // CryptoJS 已经是内置的，可以直接使用
       return obj.liveVideo[0].formats[0].url + "?t=" + ts + '&k=' + CryptoJS.MD5(obj.appCustomParams.movie.liveUri + obj.appCustomParams.movie.liveKey + ts);;
    }

    fetch("https://api.cmc.hebtv.com/scms/api/com/article/getArticleList?catalogId=32557&siteId=1", {
      "headers": {
        "accept": "application/json, text/javascript, */*; q=0.01",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-site",
        "Referer": "https://www.hebtv.com/"
      },
      "body": null,
      "method": "GET"
    }).then(res => {
      return res.json()
    }).then(res => {
        const news = res.returnData.news
        window.channelList = news.map(item => {
            return {
                title: item.title,
                liveVideo: item.liveVideo,
                appCustomParams: item.appCustomParams
            }
        })

        // {{channelName}} 会被替换成频道名称，上面的 channel中的属性都会被传入到脚本中，这里可以进行自定义处理
        const channelItem = window.channelList.find(item => item.title === '{{channelName}}')
        const playUrl = window.addLiveUrlQuery(channelItem)
        // 初始化时机执行的，所以这里需要对当前播放的频道进行播放，play.js 不会在初始化时候调用生效
        playLive(playUrl)
    })
})();
```

#### `play.js` 处理播放

```js
(function() {
    const channelItem = window.channelList.find(item => item.title === '{{channelName}}')
    const playUrl = window.addLiveUrlQuery(channelItem)
    playLive(playUrl)
})();
```

### simple 播放器内置的 html 代码

`/main/assets/html/simple_player.html`

```html
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Document</title>
    <style>
        body {
            background: #000000;
        }

        #video {
            width: 100vw;
            height: 100vh;
        }

        #loading {
            position: fixed;
            left: 0;
            top: 0;
            z-index: 99;
            width: 100vw;
            height: 100vh;
            font-size: 30px;
            font-weight: bold;
            color: white;
            display:flex;
            align-items: center;
            justify-content: center;
        }
    </style>
    <!-- 这里内部做了处理的，所以这里以这种形式引入 -->
    <script src="../js/lib/dy-crypto-js.min.js"></script>
</head>
<body>
<video autoplay id="video"></video>
<div class="loading-overlay" id="loading">
    加载中...请稍后
</div>
</body>

<script>
    const video = document.querySelector("#video")

    video.addEventListener('loadeddata', function() {
        hideLoading()
    })

    function hideLoading() {
        const loading = document.getElementById('loading');
        loading.style.display = 'none';
    }

    function resumeOrPause() {
        if (video.paused) {
            video.play();
        } else {
            video.pause();
        }
    }

    function playLive(url) {
        video.volume = 1
        video.src = url
        video.play()
    }
</script>
</html>
```

其他使用方法，请查看项目的 `/app/src/main/assets` 中的代码

## 截图

![截图](./screenshots/MuMu12-20251223-161936.png)

![截图](./screenshots/MuMu12-20251223-161942.png)

![截图](./screenshots/MuMu12-20251223-161947.png)

## 注意

仅供学习交流，请勿用于商业用途
