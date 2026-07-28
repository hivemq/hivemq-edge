<#--
  The OIDC callback result page.

  Delivered for both outcomes of the login flow. It posts a discriminated `oidc-result` message to the
  opener window and closes the popup, so the SPA always settles its pending login rather than waiting
  for a message that never arrives. The token is kept out of the URL by being posted rather than
  redirected with.

  Values are interpolated inside a <script> block, so they are escaped with ?js_string — HTML escaping
  would be the wrong context here. Model:
    messageType  the message discriminator the SPA matches on
    origin       the postMessage target origin, derived from the configured redirect-uri
    token        the HiveMQ Edge JWT, on success (omitted otherwise)
    errorCode    a stable failure code, on failure (omitted otherwise)
    fallbackText shown when the page was not opened as a popup
-->
<!DOCTYPE html>
<html>
<head><title>HiveMQ Edge</title></head>
<body><script>
(function () {
  var result = {
    type: "${messageType?js_string}"<#if token??>,
    token: "${token?js_string}"</#if><#if errorCode??>,
    errorCode: "${errorCode?js_string}"</#if>
  };
  if (window.opener) {
    window.opener.postMessage(result, "${origin?js_string}");
    window.close();
  } else {
    document.body.textContent = "${fallbackText?js_string}";
  }
})();
</script></body>
</html>
