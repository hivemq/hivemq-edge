(function() {
  this["null"] = this["null"] || {};
  this.PercySDKUtils.TestHelpers = (function (require$$0) {
    'use strict';

    const process = (typeof globalThis !== "undefined" && globalThis.process) || {};
    process.env = process.env || {};
    process.env.__PERCY_BROWSERIFIED__ = true;

    globalThis.process = globalThis.process || process;

    function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

    var require$$0__default = /*#__PURE__*/_interopDefaultLegacy(require$$0);

    const utils = require$$0__default["default"];
    const helpers = {
      async setupTest() {
        let {
          logger = true
        } = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
        utils.percy.version = '';
        delete utils.percy.config;
        delete utils.percy.build;
        delete utils.percy.enabled;
        delete utils.percy.domScript;
        delete utils.logger.log.history;
        delete utils.logger.loglevel.lvl;
        delete process.env.PERCY_LOGLEVEL;
        delete process.env.PERCY_SERVER_ADDRESS;
        utils.logger.log = helpers.logger.__log__;
        if (logger) helpers.logger.mock();
        await helpers.test('reset');
      },
      async test(cmd, arg) {
        let res = await utils.request.post(`/test/api/${cmd}`, arg);
        return res.body;
      },
      async get(what, map) {
        let res = await utils.request(`/test/${what}`);
        if (!map) map = what === 'logs' ? l => l.message : i => i;
        return Array.isArray(res.body[what]) ? res.body[what].map(map) : map(res.body);
      },
      async mockGetCurrentUrl() {
        try {
          await utils.request('/wd/hub/session');
        } catch {}
      },
      get testSnapshotURL() {
        return `${utils.percy.address}/test/snapshot`;
      },
      logger: {
        __log__: utils.logger.log,
        loglevel: utils.logger.loglevel,
        stdout: [],
        stderr: [],
        mock() {
          helpers.logger.reset();
          utils.logger.log = (ns, lvl, msg) => {
            if (lvl === 'debug' && helpers.logger.loglevel.lvl !== 'debug') return;
            msg = `[percy${lvl === 'debug' ? `:${ns}` : ''}] ${msg}`;
            let io = lvl === 'info' ? 'stdout' : 'stderr';
            helpers.logger[io].push(msg);
          };
        },
        reset() {
          helpers.logger.stdout = [];
          helpers.logger.stderr = [];
          helpers.logger.loglevel('info');
        }
      }
    };
    var helpers_1 = helpers;

    return helpers_1;

  })(PercySDKUtils);
}).call(window);

if (typeof define === "function" && define.amd) {
  define("@percy/sdk-utils", [], () => window.PercySDKUtils.TestHelpers);
} else if (typeof module === "object" && module.exports) {
  module.exports = window.PercySDKUtils.TestHelpers;
}
