'use strict';

if (process.env.NODE_ENV === "production") {
  module.exports = require("./react-select-creatable.cjs.prod.js");
} else {
  module.exports = require("./react-select-creatable.cjs.dev.js");
}
