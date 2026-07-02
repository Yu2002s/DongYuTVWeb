function hex_md5(str) {
  return CryptoJS.MD5(str).toString(CryptoJS.enc.Hex)
}

if (!window.interfaceSign) {
  window.interfaceSign =
  {
  }
}
window.interfaceSign.options =
{
  BUTEL_APP_KEY: "web2023", APPEND_USER_AGENT_VALUE: "ButelSign/1.1", SECURITY_KEY: "Ee14eda544a85465bcf0ab8800efdc6e", TST_VALUE: "", PARAMS_KEY_APPKEY: "butelAppkey", PARAMS_KEY_TST: "butelTst", PARAMS_KEY_SIGN: "butelSign",
};
window.interfaceSign.signHelper =
{
  getValidatePart: function (str) {
    var result = '';
    if (str != null && str != 'undefied' && str.length > 0) {
      for (var i = 0;
        i < str.length;
        i++) {
        var c = str.charAt(i);
        if (c.match(/^[a-z0-9A-Z\u4e00-\u9fa5]+$/)) {
          result += c
        }
      }
    }
    return result
  }
  , getValidateSignValue: function (strValue) {
    var validStr = "";
    var regChina = /^[\\/u4E00-\\/u9FA5]*$/;
    if (strValue != null && strValue != 'undefied' && strValue.length > 0) {
      for (var i = 0;
        i < strValue.length;
        i++) {
        var c = strValue.charAt(i);
        if (c >= '0' && c <= '9' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || regChina.test(c + "")) {
          validStr = validStr + c
        }
      }
    }
    return validStr
  }
  , getValidParams: function (data) {
    if (typeof (data) == 'undefined' || data == null || typeof (data) != 'object') {
      return ''
    }
    var o =
    {
    };
    for (var k in data) {
      var value = this.getValidatePart(data[k]);
      var lowerKey = k.toLocaleLowerCase();
      if (value && value.length > 0) {
        o[lowerKey] = value
      }
    }
    return o
  }
  , objKeySort: function (obj) {
    var newKey = Object.keys(obj).sort();
    var newObj =
    {
    };
    for (var i = 0;
      i < newKey.length;
      i++) {
      newObj[newKey[i].toLocaleLowerCase()] = obj[newKey[i]]
    }
    return newObj
  }
  , http_builder: function (data) {
    if (typeof (data) == 'undefined' || data == null || typeof (data) != 'object') {
      return ''
    }
    var o = "";
    for (var k in data) {
      o += ((o.indexOf("=") != -1) ? "&" : "") + k + "=" + data[k]
    }
    return o
  }
  , getSign: function (baseUrl, paramsJsonObj) {
    /*var service = baseUrl;
    if (baseUrl.indexOf('/') != 0) {
      service = '/' + baseUrl
    }*/
    var service = '/bzapi/external/externalService'
    var paramsStr = "";
    var validParams = this.getValidParams(paramsJsonObj);
    if (validParams) {
      var sortedParams = this.objKeySort(validParams);
      paramsStr = this.http_builder(sortedParams)
    }
    var butelTst = new Date().getTime();
    window.interfaceSign.options.TST_VALUE = butelTst;
    console.log('service:', service, ',securitykey:', hex_md5(window.interfaceSign.options.SECURITY_KEY), ',butelTst:', butelTst)
    var butelSign = "service=" + service + "&securitykey=" + hex_md5(window.interfaceSign.options.SECURITY_KEY) + "&butelTst=" + butelTst;
    if (paramsStr && paramsStr.length > 0) {
      var enParamsStr = encodeURIComponent(paramsStr);
      butelSign = butelSign + "&param=" + hex_md5(enParamsStr)
    }
    var lastSign = hex_md5(butelSign);
    return lastSign
  }
  , signBaseUrlAndParams: function (baseUrl, paramsJsonObj) {
    if (baseUrl && paramsJsonObj) {
      var sign = this.getSign(baseUrl, paramsJsonObj);
      paramsJsonObj[window.interfaceSign.options.PARAMS_KEY_APPKEY] = window.interfaceSign.options.BUTEL_APP_KEY;
      paramsJsonObj[window.interfaceSign.options.PARAMS_KEY_TST] = window.interfaceSign.options.TST_VALUE;
      paramsJsonObj[window.interfaceSign.options.PARAMS_KEY_SIGN] = sign;
      return paramsJsonObj
    }
    else {
      var invalidParams =
      {
      };
      return invalidParams
    }
  }
  , getQueryObject: function (url) {
    var obj =
    {
    };
    if (url == null) {
      return obj
    }
    var search = url.substring(url.lastIndexOf("?") + 1);
    var reg = /([^?&=]+)=([^?&=]*)/g;
    search.replace(reg, function (rs, $1, $2) {
      var name = decodeURIComponent($1);
      var val = decodeURIComponent($2);
      val = String(val);
      obj[name] = val;
      return rs
    }
    );
    return obj
  }
  , signUrlWithParams: function (url) {
    if (url == null || url.length == 0) {
      return ""
    }
    var aLink = window.document.createElement('a');
    aLink.href = url;
    var protcoal = aLink.protocol;
    var baseUrl = aLink.hostname;
    var port = aLink.port;
    if (port && port != 443) {
      baseUrl += ":" + port
    }
    if (aLink.pathname.indexOf('/') != 0) {
      baseUrl += '/' + aLink.pathname
    }
    else {
      baseUrl += aLink.pathname
    }
    if (protcoal) {
      baseUrl = protcoal + "//" + baseUrl
    }
    var rawParamsObj = this.getQueryObject(url);
    var signedParamsObj = this.signBaseUrlAndParams(baseUrl, rawParamsObj);
    return baseUrl + "?" + this.http_builder(signedParamsObj)
  }
  ,
};


function playLiveBoZhou(id) {
    const dataParam = {
                          "service": "getMenuContentList",
                          "params": "{\"menuId\":\"stw003001\",\"idx\":0,\"size\":16}",
                          "apiVersion": "1.0",
                          "terminalType": "website"
                      }

    const obj = window.interfaceSign.signHelper.signBaseUrlAndParams('//jkwg.ahbztv.com/bzapi/external/externalService', dataParam)

    let body = ''

    for (const k in obj) {
        const v = encodeURIComponent(obj[k])
        body += `${k}=${v}&`
    }
    body = body.slice(0, -1)
    return fetch("https://jkwg.ahbztv.com/bzapi/external/externalService", {
         "headers": {
           "accept": "application/json, text/javascript, */*; q=0.01",
           "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
           "cache-control": "no-cache",
           "content-type": "application/x-www-form-urlencoded; charset=UTF-8",
           "pragma": "no-cache",
           "sec-ch-ua": "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"",
           "sec-ch-ua-mobile": "?0",
           "sec-ch-ua-platform": "\"Windows\"",
           "sec-fetch-dest": "empty",
           "sec-fetch-mode": "cors",
           "sec-fetch-site": "same-site",
           "X-Referer": "https://www.ahbztv.com/",
           "X-Body": body
         },
         "referrer": "https://www.ahbztv.com/",
         "body": body,
         "method": "POST",
         "mode": "cors",
         "credentials": "omit"
       }).then(res => {
           return res.json()
       }).then(res => {
           window.liveList_BoZhou = res.data.rows
       }).catch(err => {
           console.error('err:', err)
       })
}

;(async function() {
    const id = '{{id}}'
    await playLiveBoZhou(id)
    const item = window.liveList_BoZhou.find(item => item.id === id)
    playLive(item.playUrl.split(',')[1])
})();
