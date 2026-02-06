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
- #号键：强制刷新当前频道（有问题时使用）

> 使用老式的闭路电视操作，适合老人使用！

## 开机自启动

> 确保 App 已经打开过，并允许了悬浮窗权限
> 不保证所有电视都支持，仅在原生系统虚拟机 Android 12 测试通过

**如果上面的方法不奏效，可以尝试以下方法：**

1. [进入电视的工厂模式](https://www.cnblogs.com/05-hust/p/17089031.html)
2. 开启 USB 调试
3. 使用无线调试或数据线连接电视和电脑(手机-需要安装终端)[电脑安装adb](https://blog.csdn.net/chengxuyuanyy/article/details/149743294) 手机可以安装MT管理器，自带终端
4. 进入终端，输入以下命令
```shell
# 无线连接 adb connect ip:端口
adb devices # 查看设备是否连接成功
adb shell # 进入管理员模式
cmd package set-home-activity "xyz.jdynb.tv/.MainActivity" # 设置默认的启动项
reboot # 重启设备
```
> 注意：定制系统可能无效

## 自定义频道

频道完整配置文件 `main/assets/live-2.jsonc` [live-2.jsonc](https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/live-2.jsonc)

最新调整:

- 去除 `number` 频道号码
- 变更 `live.jsonc` 为 `live-2.jsonc`

### 1. 对于没有验证的直播地址

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
                "number": 6, // 填写唯一的频道号码，用于数字换台 (废弃)
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

> 注意：使用fetch请求时，有些会检测Referer请求头，请设置X-Referer请求头，内部会进行处理，
> 请求头数据请使用 `X-Body` 添加到请求头中，这里的数据将作为 Body 发送，

#### `init.js`, 对应上面 init 执行的脚本

##### 通过普通 fetch 发送请求

这种方式有限制，有些请求会检测 `Referer` 请求头，请设置自定义请求头 `X-Referer`，
推荐使用内置的 [HttpUtil](/app/src/main/assets/js/lib/dy-http-util.js) 发送请求

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
        // 如果不检测 Referer的话，可以正常写
        "X-Referer": "https://www.hebtv.com/" // X-Referer 会自动在请求头添加 Referer，防止被检测
        // 必须在设置了 X-Referer 之后，X-Body 才有效
        "X-Body": "" // 这里设置请求体数据
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
##### 使用 HttpUtil 发送请求

内置在 `simple` 播放器中的，可以直接使用，推荐使用这种方式发送请求

```js
const response = await HttpUtil.post(`https://feiying.litenews.cn/api/v1/auth/exchange?t=${now}&s=${s}`, body, {
    headers: {
        "accept": "*",
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "content-type": "text/plain",
        "priority": "u=1, i",
        "sec-ch-ua": "\"Google Chrome\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "cross-site",
        "Referer": "https://v.iqilu.com/"
    },
    responseType: 'text'
})
const result = JSON.parse(decrypt(response.data))
playLive(result.data)
```

`HttpUtil` 的定义参见 [HttpUtil](/app/src/main/assets/js/lib/dy-http-util.js)

#### `play.js` 处理播放

```js
(function() {
    const channelItem = window.channelList.find(item => item.title === '{{channelName}}')
    const playUrl = window.addLiveUrlQuery(channelItem)
    playLive(playUrl)
})();
```

### Base 播放器

对于加密的直播地址，需要使用 Base 播放器，直接通过浏览器加载来实现播放，去除播放器以外的节点，实现全屏播放。

[init.js实现](/app/src/main/assets/js/guangxi/init.js) 全屏并去除其他无用样式进行初始化

[play.js实现](/app/src/main/assets/js/guangxi/play.js) 直接使用 `window.location.href` 进行跳转

[resume_pause.js实现](/app/src/main/assets/js/guangxi/resume_pause.js) 查找按钮并点击

配置参考

> `url` 参数支持动态参数，使用 {{}} 方式传入

```json5
// player 配置
{
  "id": "guangxi",
  "name": "base", // 指定为 base
  "url": "https://tv.gxtv.cn/channel/channelivePlay_{{id}}.html", // url 支持动态参数
  // 设置指定的脚本
  "script": {
    "init": ["https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/js/guangxi/init.js"],
    "play": ["https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/js/guangxi/play.js"],
    "resume_pause": ["https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/js/guangxi/resume_pause.js"]
  }
}
```
> id 参数会注入到上面 url 的 {{id}} 中

```json5
// channel 配置
{
  "channelType": "广西",
  "player": "guangxi",
  "channelList": [
    {
      "channelName": "广西卫视",
      "args": {
        "id": "e7a7ab7df9fe11e88bcfe41f13b60c62"
      }
    },
    {
      "channelName": "综艺旅游",
      "args": {
        "id": "f3335975f9fe11e88bcfe41f13b60c62"
      }
    }
  ]
}
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
    <!-- 这里内部做了处理 -->
    <script src="../js/lib/dy-http-util.js"></script>
    <script src="../js/lib/dy-crypto-js.min.js"></script>
    <script src="../js/lib/dy-hls.min.js"></script>
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

    let hls = null;

    function playLive(url, headers) {
        video.volume = 1
        
        // 检查是否支持 HLS.js 且为 m3u8 文件
        if (Hls.isSupported() && url.includes('.m3u8')) {
            if (hls) {
                hls.destroy();
            }
            
            hls = new Hls({
                debug: false,
                xhrSetup: function(xhr, url) {
                    // xhr.setRequestHeader('X-Referer', headers['Referer'])
                    // 这里设置请求头
                    if (headers) {
                        for (const key in headers) {
                            xhr.setRequestHeader(key, headers[key]);
                        }
                    }
                }
            });
            
            hls.loadSource(url);
            hls.attachMedia(video);
            
            hls.on(Hls.Events.MANIFEST_PARSED, function() {
                video.play();
            });
            
            hls.on(Hls.Events.ERROR, function(event, data) {
                console.error('HLS Error:' + JSON.stringify(data));
                if (data.fatal) {
                    switch(data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
                            console.log('Network error, trying to recover');
                            hls.startLoad();
                            break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
                            console.log('Media error, trying to recover');
                            hls.recoverMediaError();
                            break;
                        default:
                            console.log('Fatal error, destroying HLS instance');
                            hls.destroy();
                            break;
                    }
                }
            });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            // iOS Safari 原生支持
            video.src = url;
            video.play();
        } else {
            // 其他格式直接播放
            video.src = url;
            video.play();
        }
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
