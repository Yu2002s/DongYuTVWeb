;(async function() {
  const id = '{{id}}'
  const url = await getLiveUrl_ningbo(id)
  playLive(url)
})();