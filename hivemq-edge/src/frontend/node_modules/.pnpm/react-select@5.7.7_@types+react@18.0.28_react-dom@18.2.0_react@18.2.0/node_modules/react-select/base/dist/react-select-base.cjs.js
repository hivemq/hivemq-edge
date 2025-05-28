'use strict';

if (process.env.NODE_ENV === "production") {
  module.exports = require("./react-select-base.cjs.prod.js");
} else {
  module.exports = require("./react-select-base.cjs.dev.js");
}
