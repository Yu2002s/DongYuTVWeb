(function() {
    var roomId = '{{id}}'
    HttpUtil.get('http://tv.jdynb.xyz/live/parse?id=' + roomId, {
        headers: {
            accept: "application/json, text/javascript, *; q=0.01",
            "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            Referer: 'http://app.jdynb.xyz',
            "Version": "1.0.0"
        },
        responseType: 'json'
    }).then(function(res) {
        // console.log(JSON.stringify(res))
        getLiveUrl(roomId, res.data.data)
    })
})();

function getLiveUrl(roomId, header) {
    var ts = header['x-req-ts']
    HttpUtil.get(
      `https://emas-api.cctvnews.cctv.com/h5/emas.feed.article.live.detail/1.0.0?articleId=${roomId}&scene_type=6&appcode=landscape_web`,
      {
        headers: {
          accept: "*/*",
          "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
          "cache-control": "no-cache",
          cookieuid: header.cookieuid,
          "from-client": "h5",
          pragma: "no-cache",
          priority: "u=1, i",
          "sec-ch-ua":
            '"Chromium";v="146", "Not-A.Brand";v="24", "Google Chrome";v="146"',
          "sec-ch-ua-mobile": "?1",
          "sec-ch-ua-platform": '"iOS"',
          "sec-fetch-dest": "empty",
          "sec-fetch-mode": "cors",
          "sec-fetch-site": "same-site",
          userid: header.userid,
          "x-emas-gw-appkey": header['x-emas-gw-appkey'],
          "x-emas-gw-pv": header['x-emas-gw-pv'],
          "x-emas-gw-t": header['x-emas-gw-t'],
          "x-emas-gw-sign": header['x-emas-gw-sign'],
          "x-req-ts": ts,
          "x-request-id": header['x-request-id'],
          Referer: "https://m-live.cctvnews.cctv.com/",
        },
        responseType: "json",
        body: null,
        method: "GET",
      },
    )
    .then(function (res) {
        var content = JSON.parse(
          CryptoJS.enc.Base64.parse(res.data.response).toString(CryptoJS.enc.Utf8),
        );
        var authUrl =
          content.data.live_room.liveCameraList[0].pullUrlList[0].authResultUrl[0]
            .authUrl;
        var dk = content.data.dk;
        console.log(authUrl);

        var i = o2(dk, ts);
        var k = o4(dk, ts);

        var encryptedData = o$(authUrl);

        try {
          // 将底部的 data 中的 keyMaterial 和 iv 转换为 Uint8Array
          var keyMaterial = new Uint8Array(Object.values(k));
          var iv = new Uint8Array(Object.values(i));

          // 导入 AES-CBC 解密所需的密钥
          crypto.subtle.importKey(
            "raw",
            keyMaterial,
            { name: "AES-CBC", length: 128 },
            false,
            ["decrypt"],
          ).then(function(key) {
            // 解密 ArrayBuffer
              return crypto.subtle.decrypt(
                { name: "AES-CBC", iv: iv },
                key,
                encryptedData,
              );
          }).then(function(decryptedBuffer) {
            // 调用 oZ 方法将解密后的 ArrayBuffer 转为字符串
              var finalResult = oZ(decryptedBuffer);
              console.log("最终解密数据:", finalResult);
              playLive(finalResult)
          })
        } catch (e) {
          console.error("解密失败:", e);
        }
  });
}

function o$(er) {
  try {
    // 检查是否是 Base64 字符串
    for (
      var eo = atob(er), es = eo.length, eu = new Uint8Array(es), ep = 0;
      ep < es;
      ep++
    )
      eu[ep] = eo.charCodeAt(ep);
    return eu.buffer; // 返回 ArrayBuffer
  } catch (eo) {
    console.error(eo);
    return er; // 出错也返回原值
  }
}

function oZ(er) {
  try {
    return new TextDecoder().decode(er);
  } catch (em) {
    if (oK(er, ArrayBuffer)) eo = new Uint8Array(er);
    else if (ArrayBuffer.isView(er))
      eo = new Uint8Array(er.buffer, er.byteOffset, er.byteLength);
    else
      throw TypeError(
        "Input must be an ArrayBuffer, ArrayBufferView or SharedArrayBuffer",
      );
    for (var eo, es = "", eu = 0; eu < eo.length; ) {
      var ep = eo[eu++];
      if (ep < 128) es += String.fromCharCode(ep);
      else if (ep < 224)
        es += String.fromCharCode(((31 & ep) << 6) | (63 & eo[eu++]));
      else if (ep < 240)
        es += String.fromCharCode(
          ((15 & ep) << 12) | ((63 & eo[eu++]) << 6) | (63 & eo[eu++]),
        );
      else {
        var ey =
          (((7 & ep) << 18) |
            ((63 & eo[eu++]) << 12) |
            ((63 & eo[eu++]) << 6) |
            (63 & eo[eu++])) -
          65536;
        es += String.fromCharCode(
          55296 + ((ey >> 10) & 1023),
          56320 + (1023 & ey),
        );
      }
    }
    return es;
  }
}

function oX(er) {
  try {
    return new TextEncoder().encode(er);
  } catch (ep) {
    for (var eo = [], es = 0; es < er.length; es++) {
      var eu = er.charCodeAt(es);
      eu < 128
        ? eo.push(eu)
        : eu < 2048
          ? eo.push((eu >> 6) | 192, (63 & eu) | 128)
          : eu < 65536
            ? eo.push((eu >> 12) | 224, ((eu >> 6) & 63) | 128, (63 & eu) | 128)
            : eu < 1114112 &&
              eo.push(
                (eu >> 18) | 240,
                ((eu >> 12) & 63) | 128,
                ((eu >> 6) & 63) | 128,
                (63 & eu) | 128,
              );
    }
    return new Uint8Array(eo);
  }
}

function o2(er, eo) {
  try {
    var es,
      eu =
        null === (es = null == eo ? void 0 : eo.toString()) || void 0 === es
          ? void 0
          : es.slice(0, -3);
    if (!er || !eu) return null;
    return oX(
      ""
        .concat(null == er ? void 0 : er.slice(-8))
        .concat(null == eu ? void 0 : eu.slice(0, 8)),
    );
  } catch (er) {
    console.log(er);
    return null;
  }
}

function o4(er, eo) {
  try {
    var es,
      eu =
        null === (es = null == eo ? void 0 : eo.toString()) || void 0 === es
          ? void 0
          : es.slice(0, -3);
    if (!er || !eu) return null;
    return oX(
      ""
        .concat(null == er ? void 0 : er.slice(0, 8))
        .concat(null == eu ? void 0 : eu.slice(-8)),
    );
  } catch (er) {
    return null;
  }
}
