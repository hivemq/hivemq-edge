"use strict";
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __esm = (fn, res) => function __init() {
  return fn && (res = (0, fn[__getOwnPropNames(fn)[0]])(fn = 0)), res;
};
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __export = (target, all2) => {
  for (var name in all2)
    __defProp(target, name, { get: all2[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// ../../node_modules/.pnpm/tsup@6.5.0_@swc+core@1.3.24_postcss@8.4.20_typescript@5.0.2/node_modules/tsup/assets/cjs_shims.js
var getImportMetaUrl, importMetaUrl;
var init_cjs_shims = __esm({
  "../../node_modules/.pnpm/tsup@6.5.0_@swc+core@1.3.24_postcss@8.4.20_typescript@5.0.2/node_modules/tsup/assets/cjs_shims.js"() {
    getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
    importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
  }
});

// ../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/polyfills.js
var require_polyfills = __commonJS({
  "../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/polyfills.js"(exports, module2) {
    init_cjs_shims();
    var constants = require("constants");
    var origCwd = process.cwd;
    var cwd = null;
    var platform = process.env.GRACEFUL_FS_PLATFORM || process.platform;
    process.cwd = function() {
      if (!cwd)
        cwd = origCwd.call(process);
      return cwd;
    };
    try {
      process.cwd();
    } catch (er) {
    }
    if (typeof process.chdir === "function") {
      chdir = process.chdir;
      process.chdir = function(d) {
        cwd = null;
        chdir.call(process, d);
      };
      if (Object.setPrototypeOf)
        Object.setPrototypeOf(process.chdir, chdir);
    }
    var chdir;
    module2.exports = patch;
    function patch(fs3) {
      if (constants.hasOwnProperty("O_SYMLINK") && process.version.match(/^v0\.6\.[0-2]|^v0\.5\./)) {
        patchLchmod(fs3);
      }
      if (!fs3.lutimes) {
        patchLutimes(fs3);
      }
      fs3.chown = chownFix(fs3.chown);
      fs3.fchown = chownFix(fs3.fchown);
      fs3.lchown = chownFix(fs3.lchown);
      fs3.chmod = chmodFix(fs3.chmod);
      fs3.fchmod = chmodFix(fs3.fchmod);
      fs3.lchmod = chmodFix(fs3.lchmod);
      fs3.chownSync = chownFixSync(fs3.chownSync);
      fs3.fchownSync = chownFixSync(fs3.fchownSync);
      fs3.lchownSync = chownFixSync(fs3.lchownSync);
      fs3.chmodSync = chmodFixSync(fs3.chmodSync);
      fs3.fchmodSync = chmodFixSync(fs3.fchmodSync);
      fs3.lchmodSync = chmodFixSync(fs3.lchmodSync);
      fs3.stat = statFix(fs3.stat);
      fs3.fstat = statFix(fs3.fstat);
      fs3.lstat = statFix(fs3.lstat);
      fs3.statSync = statFixSync(fs3.statSync);
      fs3.fstatSync = statFixSync(fs3.fstatSync);
      fs3.lstatSync = statFixSync(fs3.lstatSync);
      if (fs3.chmod && !fs3.lchmod) {
        fs3.lchmod = function(path6, mode, cb) {
          if (cb)
            process.nextTick(cb);
        };
        fs3.lchmodSync = function() {
        };
      }
      if (fs3.chown && !fs3.lchown) {
        fs3.lchown = function(path6, uid, gid, cb) {
          if (cb)
            process.nextTick(cb);
        };
        fs3.lchownSync = function() {
        };
      }
      if (platform === "win32") {
        fs3.rename = typeof fs3.rename !== "function" ? fs3.rename : function(fs$rename) {
          function rename(from, to, cb) {
            var start = Date.now();
            var backoff = 0;
            fs$rename(from, to, function CB(er) {
              if (er && (er.code === "EACCES" || er.code === "EPERM") && Date.now() - start < 6e4) {
                setTimeout(function() {
                  fs3.stat(to, function(stater, st) {
                    if (stater && stater.code === "ENOENT")
                      fs$rename(from, to, CB);
                    else
                      cb(er);
                  });
                }, backoff);
                if (backoff < 100)
                  backoff += 10;
                return;
              }
              if (cb)
                cb(er);
            });
          }
          if (Object.setPrototypeOf)
            Object.setPrototypeOf(rename, fs$rename);
          return rename;
        }(fs3.rename);
      }
      fs3.read = typeof fs3.read !== "function" ? fs3.read : function(fs$read) {
        function read(fd, buffer, offset, length, position, callback_) {
          var callback;
          if (callback_ && typeof callback_ === "function") {
            var eagCounter = 0;
            callback = function(er, _, __) {
              if (er && er.code === "EAGAIN" && eagCounter < 10) {
                eagCounter++;
                return fs$read.call(fs3, fd, buffer, offset, length, position, callback);
              }
              callback_.apply(this, arguments);
            };
          }
          return fs$read.call(fs3, fd, buffer, offset, length, position, callback);
        }
        if (Object.setPrototypeOf)
          Object.setPrototypeOf(read, fs$read);
        return read;
      }(fs3.read);
      fs3.readSync = typeof fs3.readSync !== "function" ? fs3.readSync : function(fs$readSync) {
        return function(fd, buffer, offset, length, position) {
          var eagCounter = 0;
          while (true) {
            try {
              return fs$readSync.call(fs3, fd, buffer, offset, length, position);
            } catch (er) {
              if (er.code === "EAGAIN" && eagCounter < 10) {
                eagCounter++;
                continue;
              }
              throw er;
            }
          }
        };
      }(fs3.readSync);
      function patchLchmod(fs4) {
        fs4.lchmod = function(path6, mode, callback) {
          fs4.open(
            path6,
            constants.O_WRONLY | constants.O_SYMLINK,
            mode,
            function(err, fd) {
              if (err) {
                if (callback)
                  callback(err);
                return;
              }
              fs4.fchmod(fd, mode, function(err2) {
                fs4.close(fd, function(err22) {
                  if (callback)
                    callback(err2 || err22);
                });
              });
            }
          );
        };
        fs4.lchmodSync = function(path6, mode) {
          var fd = fs4.openSync(path6, constants.O_WRONLY | constants.O_SYMLINK, mode);
          var threw = true;
          var ret;
          try {
            ret = fs4.fchmodSync(fd, mode);
            threw = false;
          } finally {
            if (threw) {
              try {
                fs4.closeSync(fd);
              } catch (er) {
              }
            } else {
              fs4.closeSync(fd);
            }
          }
          return ret;
        };
      }
      function patchLutimes(fs4) {
        if (constants.hasOwnProperty("O_SYMLINK") && fs4.futimes) {
          fs4.lutimes = function(path6, at, mt, cb) {
            fs4.open(path6, constants.O_SYMLINK, function(er, fd) {
              if (er) {
                if (cb)
                  cb(er);
                return;
              }
              fs4.futimes(fd, at, mt, function(er2) {
                fs4.close(fd, function(er22) {
                  if (cb)
                    cb(er2 || er22);
                });
              });
            });
          };
          fs4.lutimesSync = function(path6, at, mt) {
            var fd = fs4.openSync(path6, constants.O_SYMLINK);
            var ret;
            var threw = true;
            try {
              ret = fs4.futimesSync(fd, at, mt);
              threw = false;
            } finally {
              if (threw) {
                try {
                  fs4.closeSync(fd);
                } catch (er) {
                }
              } else {
                fs4.closeSync(fd);
              }
            }
            return ret;
          };
        } else if (fs4.futimes) {
          fs4.lutimes = function(_a, _b, _c, cb) {
            if (cb)
              process.nextTick(cb);
          };
          fs4.lutimesSync = function() {
          };
        }
      }
      function chmodFix(orig) {
        if (!orig)
          return orig;
        return function(target, mode, cb) {
          return orig.call(fs3, target, mode, function(er) {
            if (chownErOk(er))
              er = null;
            if (cb)
              cb.apply(this, arguments);
          });
        };
      }
      function chmodFixSync(orig) {
        if (!orig)
          return orig;
        return function(target, mode) {
          try {
            return orig.call(fs3, target, mode);
          } catch (er) {
            if (!chownErOk(er))
              throw er;
          }
        };
      }
      function chownFix(orig) {
        if (!orig)
          return orig;
        return function(target, uid, gid, cb) {
          return orig.call(fs3, target, uid, gid, function(er) {
            if (chownErOk(er))
              er = null;
            if (cb)
              cb.apply(this, arguments);
          });
        };
      }
      function chownFixSync(orig) {
        if (!orig)
          return orig;
        return function(target, uid, gid) {
          try {
            return orig.call(fs3, target, uid, gid);
          } catch (er) {
            if (!chownErOk(er))
              throw er;
          }
        };
      }
      function statFix(orig) {
        if (!orig)
          return orig;
        return function(target, options, cb) {
          if (typeof options === "function") {
            cb = options;
            options = null;
          }
          function callback(er, stats) {
            if (stats) {
              if (stats.uid < 0)
                stats.uid += 4294967296;
              if (stats.gid < 0)
                stats.gid += 4294967296;
            }
            if (cb)
              cb.apply(this, arguments);
          }
          return options ? orig.call(fs3, target, options, callback) : orig.call(fs3, target, callback);
        };
      }
      function statFixSync(orig) {
        if (!orig)
          return orig;
        return function(target, options) {
          var stats = options ? orig.call(fs3, target, options) : orig.call(fs3, target);
          if (stats) {
            if (stats.uid < 0)
              stats.uid += 4294967296;
            if (stats.gid < 0)
              stats.gid += 4294967296;
          }
          return stats;
        };
      }
      function chownErOk(er) {
        if (!er)
          return true;
        if (er.code === "ENOSYS")
          return true;
        var nonroot = !process.getuid || process.getuid() !== 0;
        if (nonroot) {
          if (er.code === "EINVAL" || er.code === "EPERM")
            return true;
        }
        return false;
      }
    }
  }
});

// ../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/legacy-streams.js
var require_legacy_streams = __commonJS({
  "../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/legacy-streams.js"(exports, module2) {
    init_cjs_shims();
    var Stream = require("stream").Stream;
    module2.exports = legacy;
    function legacy(fs3) {
      return {
        ReadStream,
        WriteStream
      };
      function ReadStream(path6, options) {
        if (!(this instanceof ReadStream))
          return new ReadStream(path6, options);
        Stream.call(this);
        var self = this;
        this.path = path6;
        this.fd = null;
        this.readable = true;
        this.paused = false;
        this.flags = "r";
        this.mode = 438;
        this.bufferSize = 64 * 1024;
        options = options || {};
        var keys = Object.keys(options);
        for (var index = 0, length = keys.length; index < length; index++) {
          var key = keys[index];
          this[key] = options[key];
        }
        if (this.encoding)
          this.setEncoding(this.encoding);
        if (this.start !== void 0) {
          if ("number" !== typeof this.start) {
            throw TypeError("start must be a Number");
          }
          if (this.end === void 0) {
            this.end = Infinity;
          } else if ("number" !== typeof this.end) {
            throw TypeError("end must be a Number");
          }
          if (this.start > this.end) {
            throw new Error("start must be <= end");
          }
          this.pos = this.start;
        }
        if (this.fd !== null) {
          process.nextTick(function() {
            self._read();
          });
          return;
        }
        fs3.open(this.path, this.flags, this.mode, function(err, fd) {
          if (err) {
            self.emit("error", err);
            self.readable = false;
            return;
          }
          self.fd = fd;
          self.emit("open", fd);
          self._read();
        });
      }
      function WriteStream(path6, options) {
        if (!(this instanceof WriteStream))
          return new WriteStream(path6, options);
        Stream.call(this);
        this.path = path6;
        this.fd = null;
        this.writable = true;
        this.flags = "w";
        this.encoding = "binary";
        this.mode = 438;
        this.bytesWritten = 0;
        options = options || {};
        var keys = Object.keys(options);
        for (var index = 0, length = keys.length; index < length; index++) {
          var key = keys[index];
          this[key] = options[key];
        }
        if (this.start !== void 0) {
          if ("number" !== typeof this.start) {
            throw TypeError("start must be a Number");
          }
          if (this.start < 0) {
            throw new Error("start must be >= zero");
          }
          this.pos = this.start;
        }
        this.busy = false;
        this._queue = [];
        if (this.fd === null) {
          this._open = fs3.open;
          this._queue.push([this._open, this.path, this.flags, this.mode, void 0]);
          this.flush();
        }
      }
    }
  }
});

// ../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/clone.js
var require_clone = __commonJS({
  "../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/clone.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = clone;
    var getPrototypeOf = Object.getPrototypeOf || function(obj) {
      return obj.__proto__;
    };
    function clone(obj) {
      if (obj === null || typeof obj !== "object")
        return obj;
      if (obj instanceof Object)
        var copy = { __proto__: getPrototypeOf(obj) };
      else
        var copy = /* @__PURE__ */ Object.create(null);
      Object.getOwnPropertyNames(obj).forEach(function(key) {
        Object.defineProperty(copy, key, Object.getOwnPropertyDescriptor(obj, key));
      });
      return copy;
    }
  }
});

// ../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/graceful-fs.js
var require_graceful_fs = __commonJS({
  "../../node_modules/.pnpm/graceful-fs@4.2.10/node_modules/graceful-fs/graceful-fs.js"(exports, module2) {
    init_cjs_shims();
    var fs3 = require("fs");
    var polyfills = require_polyfills();
    var legacy = require_legacy_streams();
    var clone = require_clone();
    var util = require("util");
    var gracefulQueue;
    var previousSymbol;
    if (typeof Symbol === "function" && typeof Symbol.for === "function") {
      gracefulQueue = Symbol.for("graceful-fs.queue");
      previousSymbol = Symbol.for("graceful-fs.previous");
    } else {
      gracefulQueue = "___graceful-fs.queue";
      previousSymbol = "___graceful-fs.previous";
    }
    function noop3() {
    }
    function publishQueue(context, queue2) {
      Object.defineProperty(context, gracefulQueue, {
        get: function() {
          return queue2;
        }
      });
    }
    var debug = noop3;
    if (util.debuglog)
      debug = util.debuglog("gfs4");
    else if (/\bgfs4\b/i.test(process.env.NODE_DEBUG || ""))
      debug = function() {
        var m = util.format.apply(util, arguments);
        m = "GFS4: " + m.split(/\n/).join("\nGFS4: ");
        console.error(m);
      };
    if (!fs3[gracefulQueue]) {
      queue = global[gracefulQueue] || [];
      publishQueue(fs3, queue);
      fs3.close = function(fs$close) {
        function close(fd, cb) {
          return fs$close.call(fs3, fd, function(err) {
            if (!err) {
              resetQueue();
            }
            if (typeof cb === "function")
              cb.apply(this, arguments);
          });
        }
        Object.defineProperty(close, previousSymbol, {
          value: fs$close
        });
        return close;
      }(fs3.close);
      fs3.closeSync = function(fs$closeSync) {
        function closeSync(fd) {
          fs$closeSync.apply(fs3, arguments);
          resetQueue();
        }
        Object.defineProperty(closeSync, previousSymbol, {
          value: fs$closeSync
        });
        return closeSync;
      }(fs3.closeSync);
      if (/\bgfs4\b/i.test(process.env.NODE_DEBUG || "")) {
        process.on("exit", function() {
          debug(fs3[gracefulQueue]);
          require("assert").equal(fs3[gracefulQueue].length, 0);
        });
      }
    }
    var queue;
    if (!global[gracefulQueue]) {
      publishQueue(global, fs3[gracefulQueue]);
    }
    module2.exports = patch(clone(fs3));
    if (process.env.TEST_GRACEFUL_FS_GLOBAL_PATCH && !fs3.__patched) {
      module2.exports = patch(fs3);
      fs3.__patched = true;
    }
    function patch(fs4) {
      polyfills(fs4);
      fs4.gracefulify = patch;
      fs4.createReadStream = createReadStream;
      fs4.createWriteStream = createWriteStream;
      var fs$readFile = fs4.readFile;
      fs4.readFile = readFile;
      function readFile(path6, options, cb) {
        if (typeof options === "function")
          cb = options, options = null;
        return go$readFile(path6, options, cb);
        function go$readFile(path7, options2, cb2, startTime) {
          return fs$readFile(path7, options2, function(err) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([go$readFile, [path7, options2, cb2], err, startTime || Date.now(), Date.now()]);
            else {
              if (typeof cb2 === "function")
                cb2.apply(this, arguments);
            }
          });
        }
      }
      var fs$writeFile = fs4.writeFile;
      fs4.writeFile = writeFile;
      function writeFile(path6, data, options, cb) {
        if (typeof options === "function")
          cb = options, options = null;
        return go$writeFile(path6, data, options, cb);
        function go$writeFile(path7, data2, options2, cb2, startTime) {
          return fs$writeFile(path7, data2, options2, function(err) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([go$writeFile, [path7, data2, options2, cb2], err, startTime || Date.now(), Date.now()]);
            else {
              if (typeof cb2 === "function")
                cb2.apply(this, arguments);
            }
          });
        }
      }
      var fs$appendFile = fs4.appendFile;
      if (fs$appendFile)
        fs4.appendFile = appendFile;
      function appendFile(path6, data, options, cb) {
        if (typeof options === "function")
          cb = options, options = null;
        return go$appendFile(path6, data, options, cb);
        function go$appendFile(path7, data2, options2, cb2, startTime) {
          return fs$appendFile(path7, data2, options2, function(err) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([go$appendFile, [path7, data2, options2, cb2], err, startTime || Date.now(), Date.now()]);
            else {
              if (typeof cb2 === "function")
                cb2.apply(this, arguments);
            }
          });
        }
      }
      var fs$copyFile = fs4.copyFile;
      if (fs$copyFile)
        fs4.copyFile = copyFile;
      function copyFile(src, dest, flags, cb) {
        if (typeof flags === "function") {
          cb = flags;
          flags = 0;
        }
        return go$copyFile(src, dest, flags, cb);
        function go$copyFile(src2, dest2, flags2, cb2, startTime) {
          return fs$copyFile(src2, dest2, flags2, function(err) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([go$copyFile, [src2, dest2, flags2, cb2], err, startTime || Date.now(), Date.now()]);
            else {
              if (typeof cb2 === "function")
                cb2.apply(this, arguments);
            }
          });
        }
      }
      var fs$readdir = fs4.readdir;
      fs4.readdir = readdir;
      var noReaddirOptionVersions = /^v[0-5]\./;
      function readdir(path6, options, cb) {
        if (typeof options === "function")
          cb = options, options = null;
        var go$readdir = noReaddirOptionVersions.test(process.version) ? function go$readdir2(path7, options2, cb2, startTime) {
          return fs$readdir(path7, fs$readdirCallback(
            path7,
            options2,
            cb2,
            startTime
          ));
        } : function go$readdir2(path7, options2, cb2, startTime) {
          return fs$readdir(path7, options2, fs$readdirCallback(
            path7,
            options2,
            cb2,
            startTime
          ));
        };
        return go$readdir(path6, options, cb);
        function fs$readdirCallback(path7, options2, cb2, startTime) {
          return function(err, files) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([
                go$readdir,
                [path7, options2, cb2],
                err,
                startTime || Date.now(),
                Date.now()
              ]);
            else {
              if (files && files.sort)
                files.sort();
              if (typeof cb2 === "function")
                cb2.call(this, err, files);
            }
          };
        }
      }
      if (process.version.substr(0, 4) === "v0.8") {
        var legStreams = legacy(fs4);
        ReadStream = legStreams.ReadStream;
        WriteStream = legStreams.WriteStream;
      }
      var fs$ReadStream = fs4.ReadStream;
      if (fs$ReadStream) {
        ReadStream.prototype = Object.create(fs$ReadStream.prototype);
        ReadStream.prototype.open = ReadStream$open;
      }
      var fs$WriteStream = fs4.WriteStream;
      if (fs$WriteStream) {
        WriteStream.prototype = Object.create(fs$WriteStream.prototype);
        WriteStream.prototype.open = WriteStream$open;
      }
      Object.defineProperty(fs4, "ReadStream", {
        get: function() {
          return ReadStream;
        },
        set: function(val) {
          ReadStream = val;
        },
        enumerable: true,
        configurable: true
      });
      Object.defineProperty(fs4, "WriteStream", {
        get: function() {
          return WriteStream;
        },
        set: function(val) {
          WriteStream = val;
        },
        enumerable: true,
        configurable: true
      });
      var FileReadStream = ReadStream;
      Object.defineProperty(fs4, "FileReadStream", {
        get: function() {
          return FileReadStream;
        },
        set: function(val) {
          FileReadStream = val;
        },
        enumerable: true,
        configurable: true
      });
      var FileWriteStream = WriteStream;
      Object.defineProperty(fs4, "FileWriteStream", {
        get: function() {
          return FileWriteStream;
        },
        set: function(val) {
          FileWriteStream = val;
        },
        enumerable: true,
        configurable: true
      });
      function ReadStream(path6, options) {
        if (this instanceof ReadStream)
          return fs$ReadStream.apply(this, arguments), this;
        else
          return ReadStream.apply(Object.create(ReadStream.prototype), arguments);
      }
      function ReadStream$open() {
        var that = this;
        open(that.path, that.flags, that.mode, function(err, fd) {
          if (err) {
            if (that.autoClose)
              that.destroy();
            that.emit("error", err);
          } else {
            that.fd = fd;
            that.emit("open", fd);
            that.read();
          }
        });
      }
      function WriteStream(path6, options) {
        if (this instanceof WriteStream)
          return fs$WriteStream.apply(this, arguments), this;
        else
          return WriteStream.apply(Object.create(WriteStream.prototype), arguments);
      }
      function WriteStream$open() {
        var that = this;
        open(that.path, that.flags, that.mode, function(err, fd) {
          if (err) {
            that.destroy();
            that.emit("error", err);
          } else {
            that.fd = fd;
            that.emit("open", fd);
          }
        });
      }
      function createReadStream(path6, options) {
        return new fs4.ReadStream(path6, options);
      }
      function createWriteStream(path6, options) {
        return new fs4.WriteStream(path6, options);
      }
      var fs$open = fs4.open;
      fs4.open = open;
      function open(path6, flags, mode, cb) {
        if (typeof mode === "function")
          cb = mode, mode = null;
        return go$open(path6, flags, mode, cb);
        function go$open(path7, flags2, mode2, cb2, startTime) {
          return fs$open(path7, flags2, mode2, function(err, fd) {
            if (err && (err.code === "EMFILE" || err.code === "ENFILE"))
              enqueue([go$open, [path7, flags2, mode2, cb2], err, startTime || Date.now(), Date.now()]);
            else {
              if (typeof cb2 === "function")
                cb2.apply(this, arguments);
            }
          });
        }
      }
      return fs4;
    }
    function enqueue(elem) {
      debug("ENQUEUE", elem[0].name, elem[1]);
      fs3[gracefulQueue].push(elem);
      retry();
    }
    var retryTimer;
    function resetQueue() {
      var now = Date.now();
      for (var i = 0; i < fs3[gracefulQueue].length; ++i) {
        if (fs3[gracefulQueue][i].length > 2) {
          fs3[gracefulQueue][i][3] = now;
          fs3[gracefulQueue][i][4] = now;
        }
      }
      retry();
    }
    function retry() {
      clearTimeout(retryTimer);
      retryTimer = void 0;
      if (fs3[gracefulQueue].length === 0)
        return;
      var elem = fs3[gracefulQueue].shift();
      var fn = elem[0];
      var args = elem[1];
      var err = elem[2];
      var startTime = elem[3];
      var lastTime = elem[4];
      if (startTime === void 0) {
        debug("RETRY", fn.name, args);
        fn.apply(null, args);
      } else if (Date.now() - startTime >= 6e4) {
        debug("TIMEOUT", fn.name, args);
        var cb = args.pop();
        if (typeof cb === "function")
          cb.call(null, err);
      } else {
        var sinceAttempt = Date.now() - lastTime;
        var sinceStart = Math.max(lastTime - startTime, 1);
        var desiredDelay = Math.min(sinceStart * 1.2, 100);
        if (sinceAttempt >= desiredDelay) {
          debug("RETRY", fn.name, args);
          fn.apply(null, args.concat([startTime]));
        } else {
          fs3[gracefulQueue].push(elem);
        }
      }
      if (retryTimer === void 0) {
        retryTimer = setTimeout(retry, 0);
      }
    }
  }
});

// ../../node_modules/.pnpm/imurmurhash@0.1.4/node_modules/imurmurhash/imurmurhash.js
var require_imurmurhash = __commonJS({
  "../../node_modules/.pnpm/imurmurhash@0.1.4/node_modules/imurmurhash/imurmurhash.js"(exports, module2) {
    init_cjs_shims();
    (function() {
      var cache;
      function MurmurHash3(key, seed) {
        var m = this instanceof MurmurHash3 ? this : cache;
        m.reset(seed);
        if (typeof key === "string" && key.length > 0) {
          m.hash(key);
        }
        if (m !== this) {
          return m;
        }
      }
      ;
      MurmurHash3.prototype.hash = function(key) {
        var h1, k1, i, top, len;
        len = key.length;
        this.len += len;
        k1 = this.k1;
        i = 0;
        switch (this.rem) {
          case 0:
            k1 ^= len > i ? key.charCodeAt(i++) & 65535 : 0;
          case 1:
            k1 ^= len > i ? (key.charCodeAt(i++) & 65535) << 8 : 0;
          case 2:
            k1 ^= len > i ? (key.charCodeAt(i++) & 65535) << 16 : 0;
          case 3:
            k1 ^= len > i ? (key.charCodeAt(i) & 255) << 24 : 0;
            k1 ^= len > i ? (key.charCodeAt(i++) & 65280) >> 8 : 0;
        }
        this.rem = len + this.rem & 3;
        len -= this.rem;
        if (len > 0) {
          h1 = this.h1;
          while (1) {
            k1 = k1 * 11601 + (k1 & 65535) * 3432906752 & 4294967295;
            k1 = k1 << 15 | k1 >>> 17;
            k1 = k1 * 13715 + (k1 & 65535) * 461832192 & 4294967295;
            h1 ^= k1;
            h1 = h1 << 13 | h1 >>> 19;
            h1 = h1 * 5 + 3864292196 & 4294967295;
            if (i >= len) {
              break;
            }
            k1 = key.charCodeAt(i++) & 65535 ^ (key.charCodeAt(i++) & 65535) << 8 ^ (key.charCodeAt(i++) & 65535) << 16;
            top = key.charCodeAt(i++);
            k1 ^= (top & 255) << 24 ^ (top & 65280) >> 8;
          }
          k1 = 0;
          switch (this.rem) {
            case 3:
              k1 ^= (key.charCodeAt(i + 2) & 65535) << 16;
            case 2:
              k1 ^= (key.charCodeAt(i + 1) & 65535) << 8;
            case 1:
              k1 ^= key.charCodeAt(i) & 65535;
          }
          this.h1 = h1;
        }
        this.k1 = k1;
        return this;
      };
      MurmurHash3.prototype.result = function() {
        var k1, h1;
        k1 = this.k1;
        h1 = this.h1;
        if (k1 > 0) {
          k1 = k1 * 11601 + (k1 & 65535) * 3432906752 & 4294967295;
          k1 = k1 << 15 | k1 >>> 17;
          k1 = k1 * 13715 + (k1 & 65535) * 461832192 & 4294967295;
          h1 ^= k1;
        }
        h1 ^= this.len;
        h1 ^= h1 >>> 16;
        h1 = h1 * 51819 + (h1 & 65535) * 2246770688 & 4294967295;
        h1 ^= h1 >>> 13;
        h1 = h1 * 44597 + (h1 & 65535) * 3266445312 & 4294967295;
        h1 ^= h1 >>> 16;
        return h1 >>> 0;
      };
      MurmurHash3.prototype.reset = function(seed) {
        this.h1 = typeof seed === "number" ? seed : 0;
        this.rem = this.k1 = this.len = 0;
        return this;
      };
      cache = new MurmurHash3();
      if (typeof module2 != "undefined") {
        module2.exports = MurmurHash3;
      } else {
        this.MurmurHash3 = MurmurHash3;
      }
    })();
  }
});

// ../../node_modules/.pnpm/signal-exit@3.0.7/node_modules/signal-exit/signals.js
var require_signals = __commonJS({
  "../../node_modules/.pnpm/signal-exit@3.0.7/node_modules/signal-exit/signals.js"(exports, module2) {
    init_cjs_shims();
    module2.exports = [
      "SIGABRT",
      "SIGALRM",
      "SIGHUP",
      "SIGINT",
      "SIGTERM"
    ];
    if (process.platform !== "win32") {
      module2.exports.push(
        "SIGVTALRM",
        "SIGXCPU",
        "SIGXFSZ",
        "SIGUSR2",
        "SIGTRAP",
        "SIGSYS",
        "SIGQUIT",
        "SIGIOT"
      );
    }
    if (process.platform === "linux") {
      module2.exports.push(
        "SIGIO",
        "SIGPOLL",
        "SIGPWR",
        "SIGSTKFLT",
        "SIGUNUSED"
      );
    }
  }
});

// ../../node_modules/.pnpm/signal-exit@3.0.7/node_modules/signal-exit/index.js
var require_signal_exit = __commonJS({
  "../../node_modules/.pnpm/signal-exit@3.0.7/node_modules/signal-exit/index.js"(exports, module2) {
    init_cjs_shims();
    var process9 = global.process;
    var processOk = function(process10) {
      return process10 && typeof process10 === "object" && typeof process10.removeListener === "function" && typeof process10.emit === "function" && typeof process10.reallyExit === "function" && typeof process10.listeners === "function" && typeof process10.kill === "function" && typeof process10.pid === "number" && typeof process10.on === "function";
    };
    if (!processOk(process9)) {
      module2.exports = function() {
        return function() {
        };
      };
    } else {
      assert2 = require("assert");
      signals = require_signals();
      isWin = /^win/i.test(process9.platform);
      EE = require("events");
      if (typeof EE !== "function") {
        EE = EE.EventEmitter;
      }
      if (process9.__signal_exit_emitter__) {
        emitter = process9.__signal_exit_emitter__;
      } else {
        emitter = process9.__signal_exit_emitter__ = new EE();
        emitter.count = 0;
        emitter.emitted = {};
      }
      if (!emitter.infinite) {
        emitter.setMaxListeners(Infinity);
        emitter.infinite = true;
      }
      module2.exports = function(cb, opts) {
        if (!processOk(global.process)) {
          return function() {
          };
        }
        assert2.equal(typeof cb, "function", "a callback must be provided for exit handler");
        if (loaded === false) {
          load();
        }
        var ev = "exit";
        if (opts && opts.alwaysLast) {
          ev = "afterexit";
        }
        var remove = function() {
          emitter.removeListener(ev, cb);
          if (emitter.listeners("exit").length === 0 && emitter.listeners("afterexit").length === 0) {
            unload();
          }
        };
        emitter.on(ev, cb);
        return remove;
      };
      unload = function unload2() {
        if (!loaded || !processOk(global.process)) {
          return;
        }
        loaded = false;
        signals.forEach(function(sig) {
          try {
            process9.removeListener(sig, sigListeners[sig]);
          } catch (er) {
          }
        });
        process9.emit = originalProcessEmit;
        process9.reallyExit = originalProcessReallyExit;
        emitter.count -= 1;
      };
      module2.exports.unload = unload;
      emit = function emit2(event, code, signal) {
        if (emitter.emitted[event]) {
          return;
        }
        emitter.emitted[event] = true;
        emitter.emit(event, code, signal);
      };
      sigListeners = {};
      signals.forEach(function(sig) {
        sigListeners[sig] = function listener() {
          if (!processOk(global.process)) {
            return;
          }
          var listeners = process9.listeners(sig);
          if (listeners.length === emitter.count) {
            unload();
            emit("exit", null, sig);
            emit("afterexit", null, sig);
            if (isWin && sig === "SIGHUP") {
              sig = "SIGINT";
            }
            process9.kill(process9.pid, sig);
          }
        };
      });
      module2.exports.signals = function() {
        return signals;
      };
      loaded = false;
      load = function load2() {
        if (loaded || !processOk(global.process)) {
          return;
        }
        loaded = true;
        emitter.count += 1;
        signals = signals.filter(function(sig) {
          try {
            process9.on(sig, sigListeners[sig]);
            return true;
          } catch (er) {
            return false;
          }
        });
        process9.emit = processEmit;
        process9.reallyExit = processReallyExit;
      };
      module2.exports.load = load;
      originalProcessReallyExit = process9.reallyExit;
      processReallyExit = function processReallyExit2(code) {
        if (!processOk(global.process)) {
          return;
        }
        process9.exitCode = code || 0;
        emit("exit", process9.exitCode, null);
        emit("afterexit", process9.exitCode, null);
        originalProcessReallyExit.call(process9, process9.exitCode);
      };
      originalProcessEmit = process9.emit;
      processEmit = function processEmit2(ev, arg) {
        if (ev === "exit" && processOk(global.process)) {
          if (arg !== void 0) {
            process9.exitCode = arg;
          }
          var ret = originalProcessEmit.apply(this, arguments);
          emit("exit", process9.exitCode, null);
          emit("afterexit", process9.exitCode, null);
          return ret;
        } else {
          return originalProcessEmit.apply(this, arguments);
        }
      };
    }
    var assert2;
    var signals;
    var isWin;
    var EE;
    var emitter;
    var unload;
    var emit;
    var sigListeners;
    var loaded;
    var load;
    var originalProcessReallyExit;
    var processReallyExit;
    var originalProcessEmit;
    var processEmit;
  }
});

// ../../node_modules/.pnpm/is-typedarray@1.0.0/node_modules/is-typedarray/index.js
var require_is_typedarray = __commonJS({
  "../../node_modules/.pnpm/is-typedarray@1.0.0/node_modules/is-typedarray/index.js"(exports, module2) {
    init_cjs_shims();
    module2.exports = isTypedArray;
    isTypedArray.strict = isStrictTypedArray;
    isTypedArray.loose = isLooseTypedArray;
    var toString2 = Object.prototype.toString;
    var names = {
      "[object Int8Array]": true,
      "[object Int16Array]": true,
      "[object Int32Array]": true,
      "[object Uint8Array]": true,
      "[object Uint8ClampedArray]": true,
      "[object Uint16Array]": true,
      "[object Uint32Array]": true,
      "[object Float32Array]": true,
      "[object Float64Array]": true
    };
    function isTypedArray(arr) {
      return isStrictTypedArray(arr) || isLooseTypedArray(arr);
    }
    function isStrictTypedArray(arr) {
      return arr instanceof Int8Array || arr instanceof Int16Array || arr instanceof Int32Array || arr instanceof Uint8Array || arr instanceof Uint8ClampedArray || arr instanceof Uint16Array || arr instanceof Uint32Array || arr instanceof Float32Array || arr instanceof Float64Array;
    }
    function isLooseTypedArray(arr) {
      return names[toString2.call(arr)];
    }
  }
});

// ../../node_modules/.pnpm/typedarray-to-buffer@3.1.5/node_modules/typedarray-to-buffer/index.js
var require_typedarray_to_buffer = __commonJS({
  "../../node_modules/.pnpm/typedarray-to-buffer@3.1.5/node_modules/typedarray-to-buffer/index.js"(exports, module2) {
    init_cjs_shims();
    var isTypedArray = require_is_typedarray().strict;
    module2.exports = function typedarrayToBuffer(arr) {
      if (isTypedArray(arr)) {
        var buf = Buffer.from(arr.buffer);
        if (arr.byteLength !== arr.buffer.byteLength) {
          buf = buf.slice(arr.byteOffset, arr.byteOffset + arr.byteLength);
        }
        return buf;
      } else {
        return Buffer.from(arr);
      }
    };
  }
});

// ../../node_modules/.pnpm/write-file-atomic@3.0.3/node_modules/write-file-atomic/index.js
var require_write_file_atomic = __commonJS({
  "../../node_modules/.pnpm/write-file-atomic@3.0.3/node_modules/write-file-atomic/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = writeFile;
    module2.exports.sync = writeFileSync;
    module2.exports._getTmpname = getTmpname;
    module2.exports._cleanupOnExit = cleanupOnExit;
    var fs3 = require("fs");
    var MurmurHash3 = require_imurmurhash();
    var onExit = require_signal_exit();
    var path6 = require("path");
    var isTypedArray = require_is_typedarray();
    var typedArrayToBuffer = require_typedarray_to_buffer();
    var { promisify: promisify5 } = require("util");
    var activeFiles = {};
    var threadId = function getId() {
      try {
        const workerThreads = require("worker_threads");
        return workerThreads.threadId;
      } catch (e) {
        return 0;
      }
    }();
    var invocations = 0;
    function getTmpname(filename) {
      return filename + "." + MurmurHash3(__filename).hash(String(process.pid)).hash(String(threadId)).hash(String(++invocations)).result();
    }
    function cleanupOnExit(tmpfile) {
      return () => {
        try {
          fs3.unlinkSync(typeof tmpfile === "function" ? tmpfile() : tmpfile);
        } catch (_) {
        }
      };
    }
    function serializeActiveFile(absoluteName) {
      return new Promise((resolve) => {
        if (!activeFiles[absoluteName])
          activeFiles[absoluteName] = [];
        activeFiles[absoluteName].push(resolve);
        if (activeFiles[absoluteName].length === 1)
          resolve();
      });
    }
    function isChownErrOk(err) {
      if (err.code === "ENOSYS") {
        return true;
      }
      const nonroot = !process.getuid || process.getuid() !== 0;
      if (nonroot) {
        if (err.code === "EINVAL" || err.code === "EPERM") {
          return true;
        }
      }
      return false;
    }
    async function writeFileAsync(filename, data, options = {}) {
      if (typeof options === "string") {
        options = { encoding: options };
      }
      let fd;
      let tmpfile;
      const removeOnExitHandler = onExit(cleanupOnExit(() => tmpfile));
      const absoluteName = path6.resolve(filename);
      try {
        await serializeActiveFile(absoluteName);
        const truename = await promisify5(fs3.realpath)(filename).catch(() => filename);
        tmpfile = getTmpname(truename);
        if (!options.mode || !options.chown) {
          const stats = await promisify5(fs3.stat)(truename).catch(() => {
          });
          if (stats) {
            if (options.mode == null) {
              options.mode = stats.mode;
            }
            if (options.chown == null && process.getuid) {
              options.chown = { uid: stats.uid, gid: stats.gid };
            }
          }
        }
        fd = await promisify5(fs3.open)(tmpfile, "w", options.mode);
        if (options.tmpfileCreated) {
          await options.tmpfileCreated(tmpfile);
        }
        if (isTypedArray(data)) {
          data = typedArrayToBuffer(data);
        }
        if (Buffer.isBuffer(data)) {
          await promisify5(fs3.write)(fd, data, 0, data.length, 0);
        } else if (data != null) {
          await promisify5(fs3.write)(fd, String(data), 0, String(options.encoding || "utf8"));
        }
        if (options.fsync !== false) {
          await promisify5(fs3.fsync)(fd);
        }
        await promisify5(fs3.close)(fd);
        fd = null;
        if (options.chown) {
          await promisify5(fs3.chown)(tmpfile, options.chown.uid, options.chown.gid).catch((err) => {
            if (!isChownErrOk(err)) {
              throw err;
            }
          });
        }
        if (options.mode) {
          await promisify5(fs3.chmod)(tmpfile, options.mode).catch((err) => {
            if (!isChownErrOk(err)) {
              throw err;
            }
          });
        }
        await promisify5(fs3.rename)(tmpfile, truename);
      } finally {
        if (fd) {
          await promisify5(fs3.close)(fd).catch(
            () => {
            }
          );
        }
        removeOnExitHandler();
        await promisify5(fs3.unlink)(tmpfile).catch(() => {
        });
        activeFiles[absoluteName].shift();
        if (activeFiles[absoluteName].length > 0) {
          activeFiles[absoluteName][0]();
        } else
          delete activeFiles[absoluteName];
      }
    }
    function writeFile(filename, data, options, callback) {
      if (options instanceof Function) {
        callback = options;
        options = {};
      }
      const promise = writeFileAsync(filename, data, options);
      if (callback) {
        promise.then(callback, callback);
      }
      return promise;
    }
    function writeFileSync(filename, data, options) {
      if (typeof options === "string")
        options = { encoding: options };
      else if (!options)
        options = {};
      try {
        filename = fs3.realpathSync(filename);
      } catch (ex) {
      }
      const tmpfile = getTmpname(filename);
      if (!options.mode || !options.chown) {
        try {
          const stats = fs3.statSync(filename);
          options = Object.assign({}, options);
          if (!options.mode) {
            options.mode = stats.mode;
          }
          if (!options.chown && process.getuid) {
            options.chown = { uid: stats.uid, gid: stats.gid };
          }
        } catch (ex) {
        }
      }
      let fd;
      const cleanup = cleanupOnExit(tmpfile);
      const removeOnExitHandler = onExit(cleanup);
      let threw = true;
      try {
        fd = fs3.openSync(tmpfile, "w", options.mode || 438);
        if (options.tmpfileCreated) {
          options.tmpfileCreated(tmpfile);
        }
        if (isTypedArray(data)) {
          data = typedArrayToBuffer(data);
        }
        if (Buffer.isBuffer(data)) {
          fs3.writeSync(fd, data, 0, data.length, 0);
        } else if (data != null) {
          fs3.writeSync(fd, String(data), 0, String(options.encoding || "utf8"));
        }
        if (options.fsync !== false) {
          fs3.fsyncSync(fd);
        }
        fs3.closeSync(fd);
        fd = null;
        if (options.chown) {
          try {
            fs3.chownSync(tmpfile, options.chown.uid, options.chown.gid);
          } catch (err) {
            if (!isChownErrOk(err)) {
              throw err;
            }
          }
        }
        if (options.mode) {
          try {
            fs3.chmodSync(tmpfile, options.mode);
          } catch (err) {
            if (!isChownErrOk(err)) {
              throw err;
            }
          }
        }
        fs3.renameSync(tmpfile, filename);
        threw = false;
      } finally {
        if (fd) {
          try {
            fs3.closeSync(fd);
          } catch (ex) {
          }
        }
        removeOnExitHandler();
        if (threw) {
          cleanup();
        }
      }
    }
  }
});

// ../../node_modules/.pnpm/is-obj@2.0.0/node_modules/is-obj/index.js
var require_is_obj = __commonJS({
  "../../node_modules/.pnpm/is-obj@2.0.0/node_modules/is-obj/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = (value) => {
      const type = typeof value;
      return value !== null && (type === "object" || type === "function");
    };
  }
});

// ../../node_modules/.pnpm/dot-prop@6.0.1/node_modules/dot-prop/index.js
var require_dot_prop = __commonJS({
  "../../node_modules/.pnpm/dot-prop@6.0.1/node_modules/dot-prop/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var isObj = require_is_obj();
    var disallowedKeys = /* @__PURE__ */ new Set([
      "__proto__",
      "prototype",
      "constructor"
    ]);
    var isValidPath = (pathSegments) => !pathSegments.some((segment) => disallowedKeys.has(segment));
    function getPathSegments(path6) {
      const pathArray = path6.split(".");
      const parts = [];
      for (let i = 0; i < pathArray.length; i++) {
        let p = pathArray[i];
        while (p[p.length - 1] === "\\" && pathArray[i + 1] !== void 0) {
          p = p.slice(0, -1) + ".";
          p += pathArray[++i];
        }
        parts.push(p);
      }
      if (!isValidPath(parts)) {
        return [];
      }
      return parts;
    }
    module2.exports = {
      get(object, path6, value) {
        if (!isObj(object) || typeof path6 !== "string") {
          return value === void 0 ? object : value;
        }
        const pathArray = getPathSegments(path6);
        if (pathArray.length === 0) {
          return;
        }
        for (let i = 0; i < pathArray.length; i++) {
          object = object[pathArray[i]];
          if (object === void 0 || object === null) {
            if (i !== pathArray.length - 1) {
              return value;
            }
            break;
          }
        }
        return object === void 0 ? value : object;
      },
      set(object, path6, value) {
        if (!isObj(object) || typeof path6 !== "string") {
          return object;
        }
        const root = object;
        const pathArray = getPathSegments(path6);
        for (let i = 0; i < pathArray.length; i++) {
          const p = pathArray[i];
          if (!isObj(object[p])) {
            object[p] = {};
          }
          if (i === pathArray.length - 1) {
            object[p] = value;
          }
          object = object[p];
        }
        return root;
      },
      delete(object, path6) {
        if (!isObj(object) || typeof path6 !== "string") {
          return false;
        }
        const pathArray = getPathSegments(path6);
        for (let i = 0; i < pathArray.length; i++) {
          const p = pathArray[i];
          if (i === pathArray.length - 1) {
            delete object[p];
            return true;
          }
          object = object[p];
          if (!isObj(object)) {
            return false;
          }
        }
      },
      has(object, path6) {
        if (!isObj(object) || typeof path6 !== "string") {
          return false;
        }
        const pathArray = getPathSegments(path6);
        if (pathArray.length === 0) {
          return false;
        }
        for (let i = 0; i < pathArray.length; i++) {
          if (isObj(object)) {
            if (!(pathArray[i] in object)) {
              return false;
            }
            object = object[pathArray[i]];
          } else {
            return false;
          }
        }
        return true;
      }
    };
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/constants.js
var require_constants = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/constants.js"(exports, module2) {
    init_cjs_shims();
    var SEMVER_SPEC_VERSION = "2.0.0";
    var MAX_LENGTH = 256;
    var MAX_SAFE_INTEGER = Number.MAX_SAFE_INTEGER || 9007199254740991;
    var MAX_SAFE_COMPONENT_LENGTH = 16;
    module2.exports = {
      SEMVER_SPEC_VERSION,
      MAX_LENGTH,
      MAX_SAFE_INTEGER,
      MAX_SAFE_COMPONENT_LENGTH
    };
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/debug.js
var require_debug = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/debug.js"(exports, module2) {
    init_cjs_shims();
    var debug = typeof process === "object" && process.env && process.env.NODE_DEBUG && /\bsemver\b/i.test(process.env.NODE_DEBUG) ? (...args) => console.error("SEMVER", ...args) : () => {
    };
    module2.exports = debug;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/re.js
var require_re = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/re.js"(exports, module2) {
    init_cjs_shims();
    var { MAX_SAFE_COMPONENT_LENGTH } = require_constants();
    var debug = require_debug();
    exports = module2.exports = {};
    var re = exports.re = [];
    var src = exports.src = [];
    var t = exports.t = {};
    var R = 0;
    var createToken = (name, value, isGlobal) => {
      const index = R++;
      debug(name, index, value);
      t[name] = index;
      src[index] = value;
      re[index] = new RegExp(value, isGlobal ? "g" : void 0);
    };
    createToken("NUMERICIDENTIFIER", "0|[1-9]\\d*");
    createToken("NUMERICIDENTIFIERLOOSE", "[0-9]+");
    createToken("NONNUMERICIDENTIFIER", "\\d*[a-zA-Z-][a-zA-Z0-9-]*");
    createToken("MAINVERSION", `(${src[t.NUMERICIDENTIFIER]})\\.(${src[t.NUMERICIDENTIFIER]})\\.(${src[t.NUMERICIDENTIFIER]})`);
    createToken("MAINVERSIONLOOSE", `(${src[t.NUMERICIDENTIFIERLOOSE]})\\.(${src[t.NUMERICIDENTIFIERLOOSE]})\\.(${src[t.NUMERICIDENTIFIERLOOSE]})`);
    createToken("PRERELEASEIDENTIFIER", `(?:${src[t.NUMERICIDENTIFIER]}|${src[t.NONNUMERICIDENTIFIER]})`);
    createToken("PRERELEASEIDENTIFIERLOOSE", `(?:${src[t.NUMERICIDENTIFIERLOOSE]}|${src[t.NONNUMERICIDENTIFIER]})`);
    createToken("PRERELEASE", `(?:-(${src[t.PRERELEASEIDENTIFIER]}(?:\\.${src[t.PRERELEASEIDENTIFIER]})*))`);
    createToken("PRERELEASELOOSE", `(?:-?(${src[t.PRERELEASEIDENTIFIERLOOSE]}(?:\\.${src[t.PRERELEASEIDENTIFIERLOOSE]})*))`);
    createToken("BUILDIDENTIFIER", "[0-9A-Za-z-]+");
    createToken("BUILD", `(?:\\+(${src[t.BUILDIDENTIFIER]}(?:\\.${src[t.BUILDIDENTIFIER]})*))`);
    createToken("FULLPLAIN", `v?${src[t.MAINVERSION]}${src[t.PRERELEASE]}?${src[t.BUILD]}?`);
    createToken("FULL", `^${src[t.FULLPLAIN]}$`);
    createToken("LOOSEPLAIN", `[v=\\s]*${src[t.MAINVERSIONLOOSE]}${src[t.PRERELEASELOOSE]}?${src[t.BUILD]}?`);
    createToken("LOOSE", `^${src[t.LOOSEPLAIN]}$`);
    createToken("GTLT", "((?:<|>)?=?)");
    createToken("XRANGEIDENTIFIERLOOSE", `${src[t.NUMERICIDENTIFIERLOOSE]}|x|X|\\*`);
    createToken("XRANGEIDENTIFIER", `${src[t.NUMERICIDENTIFIER]}|x|X|\\*`);
    createToken("XRANGEPLAIN", `[v=\\s]*(${src[t.XRANGEIDENTIFIER]})(?:\\.(${src[t.XRANGEIDENTIFIER]})(?:\\.(${src[t.XRANGEIDENTIFIER]})(?:${src[t.PRERELEASE]})?${src[t.BUILD]}?)?)?`);
    createToken("XRANGEPLAINLOOSE", `[v=\\s]*(${src[t.XRANGEIDENTIFIERLOOSE]})(?:\\.(${src[t.XRANGEIDENTIFIERLOOSE]})(?:\\.(${src[t.XRANGEIDENTIFIERLOOSE]})(?:${src[t.PRERELEASELOOSE]})?${src[t.BUILD]}?)?)?`);
    createToken("XRANGE", `^${src[t.GTLT]}\\s*${src[t.XRANGEPLAIN]}$`);
    createToken("XRANGELOOSE", `^${src[t.GTLT]}\\s*${src[t.XRANGEPLAINLOOSE]}$`);
    createToken("COERCE", `${"(^|[^\\d])(\\d{1,"}${MAX_SAFE_COMPONENT_LENGTH}})(?:\\.(\\d{1,${MAX_SAFE_COMPONENT_LENGTH}}))?(?:\\.(\\d{1,${MAX_SAFE_COMPONENT_LENGTH}}))?(?:$|[^\\d])`);
    createToken("COERCERTL", src[t.COERCE], true);
    createToken("LONETILDE", "(?:~>?)");
    createToken("TILDETRIM", `(\\s*)${src[t.LONETILDE]}\\s+`, true);
    exports.tildeTrimReplace = "$1~";
    createToken("TILDE", `^${src[t.LONETILDE]}${src[t.XRANGEPLAIN]}$`);
    createToken("TILDELOOSE", `^${src[t.LONETILDE]}${src[t.XRANGEPLAINLOOSE]}$`);
    createToken("LONECARET", "(?:\\^)");
    createToken("CARETTRIM", `(\\s*)${src[t.LONECARET]}\\s+`, true);
    exports.caretTrimReplace = "$1^";
    createToken("CARET", `^${src[t.LONECARET]}${src[t.XRANGEPLAIN]}$`);
    createToken("CARETLOOSE", `^${src[t.LONECARET]}${src[t.XRANGEPLAINLOOSE]}$`);
    createToken("COMPARATORLOOSE", `^${src[t.GTLT]}\\s*(${src[t.LOOSEPLAIN]})$|^$`);
    createToken("COMPARATOR", `^${src[t.GTLT]}\\s*(${src[t.FULLPLAIN]})$|^$`);
    createToken("COMPARATORTRIM", `(\\s*)${src[t.GTLT]}\\s*(${src[t.LOOSEPLAIN]}|${src[t.XRANGEPLAIN]})`, true);
    exports.comparatorTrimReplace = "$1$2$3";
    createToken("HYPHENRANGE", `^\\s*(${src[t.XRANGEPLAIN]})\\s+-\\s+(${src[t.XRANGEPLAIN]})\\s*$`);
    createToken("HYPHENRANGELOOSE", `^\\s*(${src[t.XRANGEPLAINLOOSE]})\\s+-\\s+(${src[t.XRANGEPLAINLOOSE]})\\s*$`);
    createToken("STAR", "(<|>)?=?\\s*\\*");
    createToken("GTE0", "^\\s*>=\\s*0\\.0\\.0\\s*$");
    createToken("GTE0PRE", "^\\s*>=\\s*0\\.0\\.0-0\\s*$");
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/parse-options.js
var require_parse_options = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/parse-options.js"(exports, module2) {
    init_cjs_shims();
    var opts = ["includePrerelease", "loose", "rtl"];
    var parseOptions = (options) => !options ? {} : typeof options !== "object" ? { loose: true } : opts.filter((k) => options[k]).reduce((o, k) => {
      o[k] = true;
      return o;
    }, {});
    module2.exports = parseOptions;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/identifiers.js
var require_identifiers = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/internal/identifiers.js"(exports, module2) {
    init_cjs_shims();
    var numeric = /^[0-9]+$/;
    var compareIdentifiers = (a, b) => {
      const anum = numeric.test(a);
      const bnum = numeric.test(b);
      if (anum && bnum) {
        a = +a;
        b = +b;
      }
      return a === b ? 0 : anum && !bnum ? -1 : bnum && !anum ? 1 : a < b ? -1 : 1;
    };
    var rcompareIdentifiers = (a, b) => compareIdentifiers(b, a);
    module2.exports = {
      compareIdentifiers,
      rcompareIdentifiers
    };
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/semver.js
var require_semver = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/semver.js"(exports, module2) {
    init_cjs_shims();
    var debug = require_debug();
    var { MAX_LENGTH, MAX_SAFE_INTEGER } = require_constants();
    var { re, t } = require_re();
    var parseOptions = require_parse_options();
    var { compareIdentifiers } = require_identifiers();
    var SemVer = class {
      constructor(version, options) {
        options = parseOptions(options);
        if (version instanceof SemVer) {
          if (version.loose === !!options.loose && version.includePrerelease === !!options.includePrerelease) {
            return version;
          } else {
            version = version.version;
          }
        } else if (typeof version !== "string") {
          throw new TypeError(`Invalid Version: ${version}`);
        }
        if (version.length > MAX_LENGTH) {
          throw new TypeError(
            `version is longer than ${MAX_LENGTH} characters`
          );
        }
        debug("SemVer", version, options);
        this.options = options;
        this.loose = !!options.loose;
        this.includePrerelease = !!options.includePrerelease;
        const m = version.trim().match(options.loose ? re[t.LOOSE] : re[t.FULL]);
        if (!m) {
          throw new TypeError(`Invalid Version: ${version}`);
        }
        this.raw = version;
        this.major = +m[1];
        this.minor = +m[2];
        this.patch = +m[3];
        if (this.major > MAX_SAFE_INTEGER || this.major < 0) {
          throw new TypeError("Invalid major version");
        }
        if (this.minor > MAX_SAFE_INTEGER || this.minor < 0) {
          throw new TypeError("Invalid minor version");
        }
        if (this.patch > MAX_SAFE_INTEGER || this.patch < 0) {
          throw new TypeError("Invalid patch version");
        }
        if (!m[4]) {
          this.prerelease = [];
        } else {
          this.prerelease = m[4].split(".").map((id) => {
            if (/^[0-9]+$/.test(id)) {
              const num = +id;
              if (num >= 0 && num < MAX_SAFE_INTEGER) {
                return num;
              }
            }
            return id;
          });
        }
        this.build = m[5] ? m[5].split(".") : [];
        this.format();
      }
      format() {
        this.version = `${this.major}.${this.minor}.${this.patch}`;
        if (this.prerelease.length) {
          this.version += `-${this.prerelease.join(".")}`;
        }
        return this.version;
      }
      toString() {
        return this.version;
      }
      compare(other) {
        debug("SemVer.compare", this.version, this.options, other);
        if (!(other instanceof SemVer)) {
          if (typeof other === "string" && other === this.version) {
            return 0;
          }
          other = new SemVer(other, this.options);
        }
        if (other.version === this.version) {
          return 0;
        }
        return this.compareMain(other) || this.comparePre(other);
      }
      compareMain(other) {
        if (!(other instanceof SemVer)) {
          other = new SemVer(other, this.options);
        }
        return compareIdentifiers(this.major, other.major) || compareIdentifiers(this.minor, other.minor) || compareIdentifiers(this.patch, other.patch);
      }
      comparePre(other) {
        if (!(other instanceof SemVer)) {
          other = new SemVer(other, this.options);
        }
        if (this.prerelease.length && !other.prerelease.length) {
          return -1;
        } else if (!this.prerelease.length && other.prerelease.length) {
          return 1;
        } else if (!this.prerelease.length && !other.prerelease.length) {
          return 0;
        }
        let i = 0;
        do {
          const a = this.prerelease[i];
          const b = other.prerelease[i];
          debug("prerelease compare", i, a, b);
          if (a === void 0 && b === void 0) {
            return 0;
          } else if (b === void 0) {
            return 1;
          } else if (a === void 0) {
            return -1;
          } else if (a === b) {
            continue;
          } else {
            return compareIdentifiers(a, b);
          }
        } while (++i);
      }
      compareBuild(other) {
        if (!(other instanceof SemVer)) {
          other = new SemVer(other, this.options);
        }
        let i = 0;
        do {
          const a = this.build[i];
          const b = other.build[i];
          debug("prerelease compare", i, a, b);
          if (a === void 0 && b === void 0) {
            return 0;
          } else if (b === void 0) {
            return 1;
          } else if (a === void 0) {
            return -1;
          } else if (a === b) {
            continue;
          } else {
            return compareIdentifiers(a, b);
          }
        } while (++i);
      }
      inc(release, identifier) {
        switch (release) {
          case "premajor":
            this.prerelease.length = 0;
            this.patch = 0;
            this.minor = 0;
            this.major++;
            this.inc("pre", identifier);
            break;
          case "preminor":
            this.prerelease.length = 0;
            this.patch = 0;
            this.minor++;
            this.inc("pre", identifier);
            break;
          case "prepatch":
            this.prerelease.length = 0;
            this.inc("patch", identifier);
            this.inc("pre", identifier);
            break;
          case "prerelease":
            if (this.prerelease.length === 0) {
              this.inc("patch", identifier);
            }
            this.inc("pre", identifier);
            break;
          case "major":
            if (this.minor !== 0 || this.patch !== 0 || this.prerelease.length === 0) {
              this.major++;
            }
            this.minor = 0;
            this.patch = 0;
            this.prerelease = [];
            break;
          case "minor":
            if (this.patch !== 0 || this.prerelease.length === 0) {
              this.minor++;
            }
            this.patch = 0;
            this.prerelease = [];
            break;
          case "patch":
            if (this.prerelease.length === 0) {
              this.patch++;
            }
            this.prerelease = [];
            break;
          case "pre":
            if (this.prerelease.length === 0) {
              this.prerelease = [0];
            } else {
              let i = this.prerelease.length;
              while (--i >= 0) {
                if (typeof this.prerelease[i] === "number") {
                  this.prerelease[i]++;
                  i = -2;
                }
              }
              if (i === -1) {
                this.prerelease.push(0);
              }
            }
            if (identifier) {
              if (compareIdentifiers(this.prerelease[0], identifier) === 0) {
                if (isNaN(this.prerelease[1])) {
                  this.prerelease = [identifier, 0];
                }
              } else {
                this.prerelease = [identifier, 0];
              }
            }
            break;
          default:
            throw new Error(`invalid increment argument: ${release}`);
        }
        this.format();
        this.raw = this.version;
        return this;
      }
    };
    module2.exports = SemVer;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/parse.js
var require_parse = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/parse.js"(exports, module2) {
    init_cjs_shims();
    var { MAX_LENGTH } = require_constants();
    var { re, t } = require_re();
    var SemVer = require_semver();
    var parseOptions = require_parse_options();
    var parse = (version, options) => {
      options = parseOptions(options);
      if (version instanceof SemVer) {
        return version;
      }
      if (typeof version !== "string") {
        return null;
      }
      if (version.length > MAX_LENGTH) {
        return null;
      }
      const r = options.loose ? re[t.LOOSE] : re[t.FULL];
      if (!r.test(version)) {
        return null;
      }
      try {
        return new SemVer(version, options);
      } catch (er) {
        return null;
      }
    };
    module2.exports = parse;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/valid.js
var require_valid = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/valid.js"(exports, module2) {
    init_cjs_shims();
    var parse = require_parse();
    var valid = (version, options) => {
      const v = parse(version, options);
      return v ? v.version : null;
    };
    module2.exports = valid;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/clean.js
var require_clean = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/clean.js"(exports, module2) {
    init_cjs_shims();
    var parse = require_parse();
    var clean = (version, options) => {
      const s = parse(version.trim().replace(/^[=v]+/, ""), options);
      return s ? s.version : null;
    };
    module2.exports = clean;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/inc.js
var require_inc = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/inc.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var inc = (version, release, options, identifier) => {
      if (typeof options === "string") {
        identifier = options;
        options = void 0;
      }
      try {
        return new SemVer(
          version instanceof SemVer ? version.version : version,
          options
        ).inc(release, identifier).version;
      } catch (er) {
        return null;
      }
    };
    module2.exports = inc;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare.js
var require_compare = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var compare = (a, b, loose) => new SemVer(a, loose).compare(new SemVer(b, loose));
    module2.exports = compare;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/eq.js
var require_eq = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/eq.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var eq = (a, b, loose) => compare(a, b, loose) === 0;
    module2.exports = eq;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/diff.js
var require_diff = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/diff.js"(exports, module2) {
    init_cjs_shims();
    var parse = require_parse();
    var eq = require_eq();
    var diff = (version1, version2) => {
      if (eq(version1, version2)) {
        return null;
      } else {
        const v1 = parse(version1);
        const v2 = parse(version2);
        const hasPre = v1.prerelease.length || v2.prerelease.length;
        const prefix = hasPre ? "pre" : "";
        const defaultResult = hasPre ? "prerelease" : "";
        for (const key in v1) {
          if (key === "major" || key === "minor" || key === "patch") {
            if (v1[key] !== v2[key]) {
              return prefix + key;
            }
          }
        }
        return defaultResult;
      }
    };
    module2.exports = diff;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/major.js
var require_major = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/major.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var major2 = (a, loose) => new SemVer(a, loose).major;
    module2.exports = major2;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/minor.js
var require_minor = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/minor.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var minor2 = (a, loose) => new SemVer(a, loose).minor;
    module2.exports = minor2;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/patch.js
var require_patch = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/patch.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var patch = (a, loose) => new SemVer(a, loose).patch;
    module2.exports = patch;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/prerelease.js
var require_prerelease = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/prerelease.js"(exports, module2) {
    init_cjs_shims();
    var parse = require_parse();
    var prerelease = (version, options) => {
      const parsed = parse(version, options);
      return parsed && parsed.prerelease.length ? parsed.prerelease : null;
    };
    module2.exports = prerelease;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/rcompare.js
var require_rcompare = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/rcompare.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var rcompare = (a, b, loose) => compare(b, a, loose);
    module2.exports = rcompare;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare-loose.js
var require_compare_loose = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare-loose.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var compareLoose = (a, b) => compare(a, b, true);
    module2.exports = compareLoose;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare-build.js
var require_compare_build = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/compare-build.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var compareBuild = (a, b, loose) => {
      const versionA = new SemVer(a, loose);
      const versionB = new SemVer(b, loose);
      return versionA.compare(versionB) || versionA.compareBuild(versionB);
    };
    module2.exports = compareBuild;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/sort.js
var require_sort = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/sort.js"(exports, module2) {
    init_cjs_shims();
    var compareBuild = require_compare_build();
    var sort = (list, loose) => list.sort((a, b) => compareBuild(a, b, loose));
    module2.exports = sort;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/rsort.js
var require_rsort = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/rsort.js"(exports, module2) {
    init_cjs_shims();
    var compareBuild = require_compare_build();
    var rsort = (list, loose) => list.sort((a, b) => compareBuild(b, a, loose));
    module2.exports = rsort;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/gt.js
var require_gt = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/gt.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var gt = (a, b, loose) => compare(a, b, loose) > 0;
    module2.exports = gt;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/lt.js
var require_lt = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/lt.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var lt = (a, b, loose) => compare(a, b, loose) < 0;
    module2.exports = lt;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/neq.js
var require_neq = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/neq.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var neq = (a, b, loose) => compare(a, b, loose) !== 0;
    module2.exports = neq;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/gte.js
var require_gte = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/gte.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var gte = (a, b, loose) => compare(a, b, loose) >= 0;
    module2.exports = gte;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/lte.js
var require_lte = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/lte.js"(exports, module2) {
    init_cjs_shims();
    var compare = require_compare();
    var lte = (a, b, loose) => compare(a, b, loose) <= 0;
    module2.exports = lte;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/cmp.js
var require_cmp = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/cmp.js"(exports, module2) {
    init_cjs_shims();
    var eq = require_eq();
    var neq = require_neq();
    var gt = require_gt();
    var gte = require_gte();
    var lt = require_lt();
    var lte = require_lte();
    var cmp = (a, op, b, loose) => {
      switch (op) {
        case "===":
          if (typeof a === "object") {
            a = a.version;
          }
          if (typeof b === "object") {
            b = b.version;
          }
          return a === b;
        case "!==":
          if (typeof a === "object") {
            a = a.version;
          }
          if (typeof b === "object") {
            b = b.version;
          }
          return a !== b;
        case "":
        case "=":
        case "==":
          return eq(a, b, loose);
        case "!=":
          return neq(a, b, loose);
        case ">":
          return gt(a, b, loose);
        case ">=":
          return gte(a, b, loose);
        case "<":
          return lt(a, b, loose);
        case "<=":
          return lte(a, b, loose);
        default:
          throw new TypeError(`Invalid operator: ${op}`);
      }
    };
    module2.exports = cmp;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/coerce.js
var require_coerce = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/coerce.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var parse = require_parse();
    var { re, t } = require_re();
    var coerce = (version, options) => {
      if (version instanceof SemVer) {
        return version;
      }
      if (typeof version === "number") {
        version = String(version);
      }
      if (typeof version !== "string") {
        return null;
      }
      options = options || {};
      let match = null;
      if (!options.rtl) {
        match = version.match(re[t.COERCE]);
      } else {
        let next;
        while ((next = re[t.COERCERTL].exec(version)) && (!match || match.index + match[0].length !== version.length)) {
          if (!match || next.index + next[0].length !== match.index + match[0].length) {
            match = next;
          }
          re[t.COERCERTL].lastIndex = next.index + next[1].length + next[2].length;
        }
        re[t.COERCERTL].lastIndex = -1;
      }
      if (match === null) {
        return null;
      }
      return parse(`${match[2]}.${match[3] || "0"}.${match[4] || "0"}`, options);
    };
    module2.exports = coerce;
  }
});

// ../../node_modules/.pnpm/yallist@4.0.0/node_modules/yallist/iterator.js
var require_iterator = __commonJS({
  "../../node_modules/.pnpm/yallist@4.0.0/node_modules/yallist/iterator.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = function(Yallist) {
      Yallist.prototype[Symbol.iterator] = function* () {
        for (let walker = this.head; walker; walker = walker.next) {
          yield walker.value;
        }
      };
    };
  }
});

// ../../node_modules/.pnpm/yallist@4.0.0/node_modules/yallist/yallist.js
var require_yallist = __commonJS({
  "../../node_modules/.pnpm/yallist@4.0.0/node_modules/yallist/yallist.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = Yallist;
    Yallist.Node = Node;
    Yallist.create = Yallist;
    function Yallist(list) {
      var self = this;
      if (!(self instanceof Yallist)) {
        self = new Yallist();
      }
      self.tail = null;
      self.head = null;
      self.length = 0;
      if (list && typeof list.forEach === "function") {
        list.forEach(function(item) {
          self.push(item);
        });
      } else if (arguments.length > 0) {
        for (var i = 0, l = arguments.length; i < l; i++) {
          self.push(arguments[i]);
        }
      }
      return self;
    }
    Yallist.prototype.removeNode = function(node) {
      if (node.list !== this) {
        throw new Error("removing node which does not belong to this list");
      }
      var next = node.next;
      var prev = node.prev;
      if (next) {
        next.prev = prev;
      }
      if (prev) {
        prev.next = next;
      }
      if (node === this.head) {
        this.head = next;
      }
      if (node === this.tail) {
        this.tail = prev;
      }
      node.list.length--;
      node.next = null;
      node.prev = null;
      node.list = null;
      return next;
    };
    Yallist.prototype.unshiftNode = function(node) {
      if (node === this.head) {
        return;
      }
      if (node.list) {
        node.list.removeNode(node);
      }
      var head = this.head;
      node.list = this;
      node.next = head;
      if (head) {
        head.prev = node;
      }
      this.head = node;
      if (!this.tail) {
        this.tail = node;
      }
      this.length++;
    };
    Yallist.prototype.pushNode = function(node) {
      if (node === this.tail) {
        return;
      }
      if (node.list) {
        node.list.removeNode(node);
      }
      var tail = this.tail;
      node.list = this;
      node.prev = tail;
      if (tail) {
        tail.next = node;
      }
      this.tail = node;
      if (!this.head) {
        this.head = node;
      }
      this.length++;
    };
    Yallist.prototype.push = function() {
      for (var i = 0, l = arguments.length; i < l; i++) {
        push(this, arguments[i]);
      }
      return this.length;
    };
    Yallist.prototype.unshift = function() {
      for (var i = 0, l = arguments.length; i < l; i++) {
        unshift(this, arguments[i]);
      }
      return this.length;
    };
    Yallist.prototype.pop = function() {
      if (!this.tail) {
        return void 0;
      }
      var res = this.tail.value;
      this.tail = this.tail.prev;
      if (this.tail) {
        this.tail.next = null;
      } else {
        this.head = null;
      }
      this.length--;
      return res;
    };
    Yallist.prototype.shift = function() {
      if (!this.head) {
        return void 0;
      }
      var res = this.head.value;
      this.head = this.head.next;
      if (this.head) {
        this.head.prev = null;
      } else {
        this.tail = null;
      }
      this.length--;
      return res;
    };
    Yallist.prototype.forEach = function(fn, thisp) {
      thisp = thisp || this;
      for (var walker = this.head, i = 0; walker !== null; i++) {
        fn.call(thisp, walker.value, i, this);
        walker = walker.next;
      }
    };
    Yallist.prototype.forEachReverse = function(fn, thisp) {
      thisp = thisp || this;
      for (var walker = this.tail, i = this.length - 1; walker !== null; i--) {
        fn.call(thisp, walker.value, i, this);
        walker = walker.prev;
      }
    };
    Yallist.prototype.get = function(n) {
      for (var i = 0, walker = this.head; walker !== null && i < n; i++) {
        walker = walker.next;
      }
      if (i === n && walker !== null) {
        return walker.value;
      }
    };
    Yallist.prototype.getReverse = function(n) {
      for (var i = 0, walker = this.tail; walker !== null && i < n; i++) {
        walker = walker.prev;
      }
      if (i === n && walker !== null) {
        return walker.value;
      }
    };
    Yallist.prototype.map = function(fn, thisp) {
      thisp = thisp || this;
      var res = new Yallist();
      for (var walker = this.head; walker !== null; ) {
        res.push(fn.call(thisp, walker.value, this));
        walker = walker.next;
      }
      return res;
    };
    Yallist.prototype.mapReverse = function(fn, thisp) {
      thisp = thisp || this;
      var res = new Yallist();
      for (var walker = this.tail; walker !== null; ) {
        res.push(fn.call(thisp, walker.value, this));
        walker = walker.prev;
      }
      return res;
    };
    Yallist.prototype.reduce = function(fn, initial) {
      var acc;
      var walker = this.head;
      if (arguments.length > 1) {
        acc = initial;
      } else if (this.head) {
        walker = this.head.next;
        acc = this.head.value;
      } else {
        throw new TypeError("Reduce of empty list with no initial value");
      }
      for (var i = 0; walker !== null; i++) {
        acc = fn(acc, walker.value, i);
        walker = walker.next;
      }
      return acc;
    };
    Yallist.prototype.reduceReverse = function(fn, initial) {
      var acc;
      var walker = this.tail;
      if (arguments.length > 1) {
        acc = initial;
      } else if (this.tail) {
        walker = this.tail.prev;
        acc = this.tail.value;
      } else {
        throw new TypeError("Reduce of empty list with no initial value");
      }
      for (var i = this.length - 1; walker !== null; i--) {
        acc = fn(acc, walker.value, i);
        walker = walker.prev;
      }
      return acc;
    };
    Yallist.prototype.toArray = function() {
      var arr = new Array(this.length);
      for (var i = 0, walker = this.head; walker !== null; i++) {
        arr[i] = walker.value;
        walker = walker.next;
      }
      return arr;
    };
    Yallist.prototype.toArrayReverse = function() {
      var arr = new Array(this.length);
      for (var i = 0, walker = this.tail; walker !== null; i++) {
        arr[i] = walker.value;
        walker = walker.prev;
      }
      return arr;
    };
    Yallist.prototype.slice = function(from, to) {
      to = to || this.length;
      if (to < 0) {
        to += this.length;
      }
      from = from || 0;
      if (from < 0) {
        from += this.length;
      }
      var ret = new Yallist();
      if (to < from || to < 0) {
        return ret;
      }
      if (from < 0) {
        from = 0;
      }
      if (to > this.length) {
        to = this.length;
      }
      for (var i = 0, walker = this.head; walker !== null && i < from; i++) {
        walker = walker.next;
      }
      for (; walker !== null && i < to; i++, walker = walker.next) {
        ret.push(walker.value);
      }
      return ret;
    };
    Yallist.prototype.sliceReverse = function(from, to) {
      to = to || this.length;
      if (to < 0) {
        to += this.length;
      }
      from = from || 0;
      if (from < 0) {
        from += this.length;
      }
      var ret = new Yallist();
      if (to < from || to < 0) {
        return ret;
      }
      if (from < 0) {
        from = 0;
      }
      if (to > this.length) {
        to = this.length;
      }
      for (var i = this.length, walker = this.tail; walker !== null && i > to; i--) {
        walker = walker.prev;
      }
      for (; walker !== null && i > from; i--, walker = walker.prev) {
        ret.push(walker.value);
      }
      return ret;
    };
    Yallist.prototype.splice = function(start, deleteCount, ...nodes) {
      if (start > this.length) {
        start = this.length - 1;
      }
      if (start < 0) {
        start = this.length + start;
      }
      for (var i = 0, walker = this.head; walker !== null && i < start; i++) {
        walker = walker.next;
      }
      var ret = [];
      for (var i = 0; walker && i < deleteCount; i++) {
        ret.push(walker.value);
        walker = this.removeNode(walker);
      }
      if (walker === null) {
        walker = this.tail;
      }
      if (walker !== this.head && walker !== this.tail) {
        walker = walker.prev;
      }
      for (var i = 0; i < nodes.length; i++) {
        walker = insert(this, walker, nodes[i]);
      }
      return ret;
    };
    Yallist.prototype.reverse = function() {
      var head = this.head;
      var tail = this.tail;
      for (var walker = head; walker !== null; walker = walker.prev) {
        var p = walker.prev;
        walker.prev = walker.next;
        walker.next = p;
      }
      this.head = tail;
      this.tail = head;
      return this;
    };
    function insert(self, node, value) {
      var inserted = node === self.head ? new Node(value, null, node, self) : new Node(value, node, node.next, self);
      if (inserted.next === null) {
        self.tail = inserted;
      }
      if (inserted.prev === null) {
        self.head = inserted;
      }
      self.length++;
      return inserted;
    }
    function push(self, item) {
      self.tail = new Node(item, self.tail, null, self);
      if (!self.head) {
        self.head = self.tail;
      }
      self.length++;
    }
    function unshift(self, item) {
      self.head = new Node(item, null, self.head, self);
      if (!self.tail) {
        self.tail = self.head;
      }
      self.length++;
    }
    function Node(value, prev, next, list) {
      if (!(this instanceof Node)) {
        return new Node(value, prev, next, list);
      }
      this.list = list;
      this.value = value;
      if (prev) {
        prev.next = this;
        this.prev = prev;
      } else {
        this.prev = null;
      }
      if (next) {
        next.prev = this;
        this.next = next;
      } else {
        this.next = null;
      }
    }
    try {
      require_iterator()(Yallist);
    } catch (er) {
    }
  }
});

// ../../node_modules/.pnpm/lru-cache@6.0.0/node_modules/lru-cache/index.js
var require_lru_cache = __commonJS({
  "../../node_modules/.pnpm/lru-cache@6.0.0/node_modules/lru-cache/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var Yallist = require_yallist();
    var MAX = Symbol("max");
    var LENGTH = Symbol("length");
    var LENGTH_CALCULATOR = Symbol("lengthCalculator");
    var ALLOW_STALE = Symbol("allowStale");
    var MAX_AGE = Symbol("maxAge");
    var DISPOSE = Symbol("dispose");
    var NO_DISPOSE_ON_SET = Symbol("noDisposeOnSet");
    var LRU_LIST = Symbol("lruList");
    var CACHE = Symbol("cache");
    var UPDATE_AGE_ON_GET = Symbol("updateAgeOnGet");
    var naiveLength = () => 1;
    var LRUCache = class {
      constructor(options) {
        if (typeof options === "number")
          options = { max: options };
        if (!options)
          options = {};
        if (options.max && (typeof options.max !== "number" || options.max < 0))
          throw new TypeError("max must be a non-negative number");
        const max = this[MAX] = options.max || Infinity;
        const lc = options.length || naiveLength;
        this[LENGTH_CALCULATOR] = typeof lc !== "function" ? naiveLength : lc;
        this[ALLOW_STALE] = options.stale || false;
        if (options.maxAge && typeof options.maxAge !== "number")
          throw new TypeError("maxAge must be a number");
        this[MAX_AGE] = options.maxAge || 0;
        this[DISPOSE] = options.dispose;
        this[NO_DISPOSE_ON_SET] = options.noDisposeOnSet || false;
        this[UPDATE_AGE_ON_GET] = options.updateAgeOnGet || false;
        this.reset();
      }
      set max(mL) {
        if (typeof mL !== "number" || mL < 0)
          throw new TypeError("max must be a non-negative number");
        this[MAX] = mL || Infinity;
        trim(this);
      }
      get max() {
        return this[MAX];
      }
      set allowStale(allowStale) {
        this[ALLOW_STALE] = !!allowStale;
      }
      get allowStale() {
        return this[ALLOW_STALE];
      }
      set maxAge(mA) {
        if (typeof mA !== "number")
          throw new TypeError("maxAge must be a non-negative number");
        this[MAX_AGE] = mA;
        trim(this);
      }
      get maxAge() {
        return this[MAX_AGE];
      }
      set lengthCalculator(lC) {
        if (typeof lC !== "function")
          lC = naiveLength;
        if (lC !== this[LENGTH_CALCULATOR]) {
          this[LENGTH_CALCULATOR] = lC;
          this[LENGTH] = 0;
          this[LRU_LIST].forEach((hit) => {
            hit.length = this[LENGTH_CALCULATOR](hit.value, hit.key);
            this[LENGTH] += hit.length;
          });
        }
        trim(this);
      }
      get lengthCalculator() {
        return this[LENGTH_CALCULATOR];
      }
      get length() {
        return this[LENGTH];
      }
      get itemCount() {
        return this[LRU_LIST].length;
      }
      rforEach(fn, thisp) {
        thisp = thisp || this;
        for (let walker = this[LRU_LIST].tail; walker !== null; ) {
          const prev = walker.prev;
          forEachStep(this, fn, walker, thisp);
          walker = prev;
        }
      }
      forEach(fn, thisp) {
        thisp = thisp || this;
        for (let walker = this[LRU_LIST].head; walker !== null; ) {
          const next = walker.next;
          forEachStep(this, fn, walker, thisp);
          walker = next;
        }
      }
      keys() {
        return this[LRU_LIST].toArray().map((k) => k.key);
      }
      values() {
        return this[LRU_LIST].toArray().map((k) => k.value);
      }
      reset() {
        if (this[DISPOSE] && this[LRU_LIST] && this[LRU_LIST].length) {
          this[LRU_LIST].forEach((hit) => this[DISPOSE](hit.key, hit.value));
        }
        this[CACHE] = /* @__PURE__ */ new Map();
        this[LRU_LIST] = new Yallist();
        this[LENGTH] = 0;
      }
      dump() {
        return this[LRU_LIST].map((hit) => isStale(this, hit) ? false : {
          k: hit.key,
          v: hit.value,
          e: hit.now + (hit.maxAge || 0)
        }).toArray().filter((h) => h);
      }
      dumpLru() {
        return this[LRU_LIST];
      }
      set(key, value, maxAge) {
        maxAge = maxAge || this[MAX_AGE];
        if (maxAge && typeof maxAge !== "number")
          throw new TypeError("maxAge must be a number");
        const now = maxAge ? Date.now() : 0;
        const len = this[LENGTH_CALCULATOR](value, key);
        if (this[CACHE].has(key)) {
          if (len > this[MAX]) {
            del(this, this[CACHE].get(key));
            return false;
          }
          const node = this[CACHE].get(key);
          const item = node.value;
          if (this[DISPOSE]) {
            if (!this[NO_DISPOSE_ON_SET])
              this[DISPOSE](key, item.value);
          }
          item.now = now;
          item.maxAge = maxAge;
          item.value = value;
          this[LENGTH] += len - item.length;
          item.length = len;
          this.get(key);
          trim(this);
          return true;
        }
        const hit = new Entry(key, value, len, now, maxAge);
        if (hit.length > this[MAX]) {
          if (this[DISPOSE])
            this[DISPOSE](key, value);
          return false;
        }
        this[LENGTH] += hit.length;
        this[LRU_LIST].unshift(hit);
        this[CACHE].set(key, this[LRU_LIST].head);
        trim(this);
        return true;
      }
      has(key) {
        if (!this[CACHE].has(key))
          return false;
        const hit = this[CACHE].get(key).value;
        return !isStale(this, hit);
      }
      get(key) {
        return get(this, key, true);
      }
      peek(key) {
        return get(this, key, false);
      }
      pop() {
        const node = this[LRU_LIST].tail;
        if (!node)
          return null;
        del(this, node);
        return node.value;
      }
      del(key) {
        del(this, this[CACHE].get(key));
      }
      load(arr) {
        this.reset();
        const now = Date.now();
        for (let l = arr.length - 1; l >= 0; l--) {
          const hit = arr[l];
          const expiresAt = hit.e || 0;
          if (expiresAt === 0)
            this.set(hit.k, hit.v);
          else {
            const maxAge = expiresAt - now;
            if (maxAge > 0) {
              this.set(hit.k, hit.v, maxAge);
            }
          }
        }
      }
      prune() {
        this[CACHE].forEach((value, key) => get(this, key, false));
      }
    };
    var get = (self, key, doUse) => {
      const node = self[CACHE].get(key);
      if (node) {
        const hit = node.value;
        if (isStale(self, hit)) {
          del(self, node);
          if (!self[ALLOW_STALE])
            return void 0;
        } else {
          if (doUse) {
            if (self[UPDATE_AGE_ON_GET])
              node.value.now = Date.now();
            self[LRU_LIST].unshiftNode(node);
          }
        }
        return hit.value;
      }
    };
    var isStale = (self, hit) => {
      if (!hit || !hit.maxAge && !self[MAX_AGE])
        return false;
      const diff = Date.now() - hit.now;
      return hit.maxAge ? diff > hit.maxAge : self[MAX_AGE] && diff > self[MAX_AGE];
    };
    var trim = (self) => {
      if (self[LENGTH] > self[MAX]) {
        for (let walker = self[LRU_LIST].tail; self[LENGTH] > self[MAX] && walker !== null; ) {
          const prev = walker.prev;
          del(self, walker);
          walker = prev;
        }
      }
    };
    var del = (self, node) => {
      if (node) {
        const hit = node.value;
        if (self[DISPOSE])
          self[DISPOSE](hit.key, hit.value);
        self[LENGTH] -= hit.length;
        self[CACHE].delete(hit.key);
        self[LRU_LIST].removeNode(node);
      }
    };
    var Entry = class {
      constructor(key, value, length, now, maxAge) {
        this.key = key;
        this.value = value;
        this.length = length;
        this.now = now;
        this.maxAge = maxAge || 0;
      }
    };
    var forEachStep = (self, fn, node, thisp) => {
      let hit = node.value;
      if (isStale(self, hit)) {
        del(self, node);
        if (!self[ALLOW_STALE])
          hit = void 0;
      }
      if (hit)
        fn.call(thisp, hit.value, hit.key, self);
    };
    module2.exports = LRUCache;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/range.js
var require_range = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/range.js"(exports, module2) {
    init_cjs_shims();
    var Range = class {
      constructor(range, options) {
        options = parseOptions(options);
        if (range instanceof Range) {
          if (range.loose === !!options.loose && range.includePrerelease === !!options.includePrerelease) {
            return range;
          } else {
            return new Range(range.raw, options);
          }
        }
        if (range instanceof Comparator) {
          this.raw = range.value;
          this.set = [[range]];
          this.format();
          return this;
        }
        this.options = options;
        this.loose = !!options.loose;
        this.includePrerelease = !!options.includePrerelease;
        this.raw = range;
        this.set = range.split("||").map((r) => this.parseRange(r.trim())).filter((c) => c.length);
        if (!this.set.length) {
          throw new TypeError(`Invalid SemVer Range: ${range}`);
        }
        if (this.set.length > 1) {
          const first = this.set[0];
          this.set = this.set.filter((c) => !isNullSet(c[0]));
          if (this.set.length === 0) {
            this.set = [first];
          } else if (this.set.length > 1) {
            for (const c of this.set) {
              if (c.length === 1 && isAny(c[0])) {
                this.set = [c];
                break;
              }
            }
          }
        }
        this.format();
      }
      format() {
        this.range = this.set.map((comps) => {
          return comps.join(" ").trim();
        }).join("||").trim();
        return this.range;
      }
      toString() {
        return this.range;
      }
      parseRange(range) {
        range = range.trim();
        const memoOpts = Object.keys(this.options).join(",");
        const memoKey = `parseRange:${memoOpts}:${range}`;
        const cached = cache.get(memoKey);
        if (cached) {
          return cached;
        }
        const loose = this.options.loose;
        const hr = loose ? re[t.HYPHENRANGELOOSE] : re[t.HYPHENRANGE];
        range = range.replace(hr, hyphenReplace(this.options.includePrerelease));
        debug("hyphen replace", range);
        range = range.replace(re[t.COMPARATORTRIM], comparatorTrimReplace);
        debug("comparator trim", range);
        range = range.replace(re[t.TILDETRIM], tildeTrimReplace);
        range = range.replace(re[t.CARETTRIM], caretTrimReplace);
        range = range.split(/\s+/).join(" ");
        let rangeList = range.split(" ").map((comp) => parseComparator(comp, this.options)).join(" ").split(/\s+/).map((comp) => replaceGTE0(comp, this.options));
        if (loose) {
          rangeList = rangeList.filter((comp) => {
            debug("loose invalid filter", comp, this.options);
            return !!comp.match(re[t.COMPARATORLOOSE]);
          });
        }
        debug("range list", rangeList);
        const rangeMap = /* @__PURE__ */ new Map();
        const comparators = rangeList.map((comp) => new Comparator(comp, this.options));
        for (const comp of comparators) {
          if (isNullSet(comp)) {
            return [comp];
          }
          rangeMap.set(comp.value, comp);
        }
        if (rangeMap.size > 1 && rangeMap.has("")) {
          rangeMap.delete("");
        }
        const result = [...rangeMap.values()];
        cache.set(memoKey, result);
        return result;
      }
      intersects(range, options) {
        if (!(range instanceof Range)) {
          throw new TypeError("a Range is required");
        }
        return this.set.some((thisComparators) => {
          return isSatisfiable(thisComparators, options) && range.set.some((rangeComparators) => {
            return isSatisfiable(rangeComparators, options) && thisComparators.every((thisComparator) => {
              return rangeComparators.every((rangeComparator) => {
                return thisComparator.intersects(rangeComparator, options);
              });
            });
          });
        });
      }
      test(version) {
        if (!version) {
          return false;
        }
        if (typeof version === "string") {
          try {
            version = new SemVer(version, this.options);
          } catch (er) {
            return false;
          }
        }
        for (let i = 0; i < this.set.length; i++) {
          if (testSet(this.set[i], version, this.options)) {
            return true;
          }
        }
        return false;
      }
    };
    module2.exports = Range;
    var LRU = require_lru_cache();
    var cache = new LRU({ max: 1e3 });
    var parseOptions = require_parse_options();
    var Comparator = require_comparator();
    var debug = require_debug();
    var SemVer = require_semver();
    var {
      re,
      t,
      comparatorTrimReplace,
      tildeTrimReplace,
      caretTrimReplace
    } = require_re();
    var isNullSet = (c) => c.value === "<0.0.0-0";
    var isAny = (c) => c.value === "";
    var isSatisfiable = (comparators, options) => {
      let result = true;
      const remainingComparators = comparators.slice();
      let testComparator = remainingComparators.pop();
      while (result && remainingComparators.length) {
        result = remainingComparators.every((otherComparator) => {
          return testComparator.intersects(otherComparator, options);
        });
        testComparator = remainingComparators.pop();
      }
      return result;
    };
    var parseComparator = (comp, options) => {
      debug("comp", comp, options);
      comp = replaceCarets(comp, options);
      debug("caret", comp);
      comp = replaceTildes(comp, options);
      debug("tildes", comp);
      comp = replaceXRanges(comp, options);
      debug("xrange", comp);
      comp = replaceStars(comp, options);
      debug("stars", comp);
      return comp;
    };
    var isX = (id) => !id || id.toLowerCase() === "x" || id === "*";
    var replaceTildes = (comp, options) => comp.trim().split(/\s+/).map((c) => {
      return replaceTilde(c, options);
    }).join(" ");
    var replaceTilde = (comp, options) => {
      const r = options.loose ? re[t.TILDELOOSE] : re[t.TILDE];
      return comp.replace(r, (_, M, m, p, pr) => {
        debug("tilde", comp, _, M, m, p, pr);
        let ret;
        if (isX(M)) {
          ret = "";
        } else if (isX(m)) {
          ret = `>=${M}.0.0 <${+M + 1}.0.0-0`;
        } else if (isX(p)) {
          ret = `>=${M}.${m}.0 <${M}.${+m + 1}.0-0`;
        } else if (pr) {
          debug("replaceTilde pr", pr);
          ret = `>=${M}.${m}.${p}-${pr} <${M}.${+m + 1}.0-0`;
        } else {
          ret = `>=${M}.${m}.${p} <${M}.${+m + 1}.0-0`;
        }
        debug("tilde return", ret);
        return ret;
      });
    };
    var replaceCarets = (comp, options) => comp.trim().split(/\s+/).map((c) => {
      return replaceCaret(c, options);
    }).join(" ");
    var replaceCaret = (comp, options) => {
      debug("caret", comp, options);
      const r = options.loose ? re[t.CARETLOOSE] : re[t.CARET];
      const z = options.includePrerelease ? "-0" : "";
      return comp.replace(r, (_, M, m, p, pr) => {
        debug("caret", comp, _, M, m, p, pr);
        let ret;
        if (isX(M)) {
          ret = "";
        } else if (isX(m)) {
          ret = `>=${M}.0.0${z} <${+M + 1}.0.0-0`;
        } else if (isX(p)) {
          if (M === "0") {
            ret = `>=${M}.${m}.0${z} <${M}.${+m + 1}.0-0`;
          } else {
            ret = `>=${M}.${m}.0${z} <${+M + 1}.0.0-0`;
          }
        } else if (pr) {
          debug("replaceCaret pr", pr);
          if (M === "0") {
            if (m === "0") {
              ret = `>=${M}.${m}.${p}-${pr} <${M}.${m}.${+p + 1}-0`;
            } else {
              ret = `>=${M}.${m}.${p}-${pr} <${M}.${+m + 1}.0-0`;
            }
          } else {
            ret = `>=${M}.${m}.${p}-${pr} <${+M + 1}.0.0-0`;
          }
        } else {
          debug("no pr");
          if (M === "0") {
            if (m === "0") {
              ret = `>=${M}.${m}.${p}${z} <${M}.${m}.${+p + 1}-0`;
            } else {
              ret = `>=${M}.${m}.${p}${z} <${M}.${+m + 1}.0-0`;
            }
          } else {
            ret = `>=${M}.${m}.${p} <${+M + 1}.0.0-0`;
          }
        }
        debug("caret return", ret);
        return ret;
      });
    };
    var replaceXRanges = (comp, options) => {
      debug("replaceXRanges", comp, options);
      return comp.split(/\s+/).map((c) => {
        return replaceXRange(c, options);
      }).join(" ");
    };
    var replaceXRange = (comp, options) => {
      comp = comp.trim();
      const r = options.loose ? re[t.XRANGELOOSE] : re[t.XRANGE];
      return comp.replace(r, (ret, gtlt, M, m, p, pr) => {
        debug("xRange", comp, ret, gtlt, M, m, p, pr);
        const xM = isX(M);
        const xm = xM || isX(m);
        const xp = xm || isX(p);
        const anyX = xp;
        if (gtlt === "=" && anyX) {
          gtlt = "";
        }
        pr = options.includePrerelease ? "-0" : "";
        if (xM) {
          if (gtlt === ">" || gtlt === "<") {
            ret = "<0.0.0-0";
          } else {
            ret = "*";
          }
        } else if (gtlt && anyX) {
          if (xm) {
            m = 0;
          }
          p = 0;
          if (gtlt === ">") {
            gtlt = ">=";
            if (xm) {
              M = +M + 1;
              m = 0;
              p = 0;
            } else {
              m = +m + 1;
              p = 0;
            }
          } else if (gtlt === "<=") {
            gtlt = "<";
            if (xm) {
              M = +M + 1;
            } else {
              m = +m + 1;
            }
          }
          if (gtlt === "<") {
            pr = "-0";
          }
          ret = `${gtlt + M}.${m}.${p}${pr}`;
        } else if (xm) {
          ret = `>=${M}.0.0${pr} <${+M + 1}.0.0-0`;
        } else if (xp) {
          ret = `>=${M}.${m}.0${pr} <${M}.${+m + 1}.0-0`;
        }
        debug("xRange return", ret);
        return ret;
      });
    };
    var replaceStars = (comp, options) => {
      debug("replaceStars", comp, options);
      return comp.trim().replace(re[t.STAR], "");
    };
    var replaceGTE0 = (comp, options) => {
      debug("replaceGTE0", comp, options);
      return comp.trim().replace(re[options.includePrerelease ? t.GTE0PRE : t.GTE0], "");
    };
    var hyphenReplace = (incPr) => ($0, from, fM, fm, fp, fpr, fb, to, tM, tm, tp, tpr, tb) => {
      if (isX(fM)) {
        from = "";
      } else if (isX(fm)) {
        from = `>=${fM}.0.0${incPr ? "-0" : ""}`;
      } else if (isX(fp)) {
        from = `>=${fM}.${fm}.0${incPr ? "-0" : ""}`;
      } else if (fpr) {
        from = `>=${from}`;
      } else {
        from = `>=${from}${incPr ? "-0" : ""}`;
      }
      if (isX(tM)) {
        to = "";
      } else if (isX(tm)) {
        to = `<${+tM + 1}.0.0-0`;
      } else if (isX(tp)) {
        to = `<${tM}.${+tm + 1}.0-0`;
      } else if (tpr) {
        to = `<=${tM}.${tm}.${tp}-${tpr}`;
      } else if (incPr) {
        to = `<${tM}.${tm}.${+tp + 1}-0`;
      } else {
        to = `<=${to}`;
      }
      return `${from} ${to}`.trim();
    };
    var testSet = (set, version, options) => {
      for (let i = 0; i < set.length; i++) {
        if (!set[i].test(version)) {
          return false;
        }
      }
      if (version.prerelease.length && !options.includePrerelease) {
        for (let i = 0; i < set.length; i++) {
          debug(set[i].semver);
          if (set[i].semver === Comparator.ANY) {
            continue;
          }
          if (set[i].semver.prerelease.length > 0) {
            const allowed = set[i].semver;
            if (allowed.major === version.major && allowed.minor === version.minor && allowed.patch === version.patch) {
              return true;
            }
          }
        }
        return false;
      }
      return true;
    };
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/comparator.js
var require_comparator = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/classes/comparator.js"(exports, module2) {
    init_cjs_shims();
    var ANY = Symbol("SemVer ANY");
    var Comparator = class {
      static get ANY() {
        return ANY;
      }
      constructor(comp, options) {
        options = parseOptions(options);
        if (comp instanceof Comparator) {
          if (comp.loose === !!options.loose) {
            return comp;
          } else {
            comp = comp.value;
          }
        }
        debug("comparator", comp, options);
        this.options = options;
        this.loose = !!options.loose;
        this.parse(comp);
        if (this.semver === ANY) {
          this.value = "";
        } else {
          this.value = this.operator + this.semver.version;
        }
        debug("comp", this);
      }
      parse(comp) {
        const r = this.options.loose ? re[t.COMPARATORLOOSE] : re[t.COMPARATOR];
        const m = comp.match(r);
        if (!m) {
          throw new TypeError(`Invalid comparator: ${comp}`);
        }
        this.operator = m[1] !== void 0 ? m[1] : "";
        if (this.operator === "=") {
          this.operator = "";
        }
        if (!m[2]) {
          this.semver = ANY;
        } else {
          this.semver = new SemVer(m[2], this.options.loose);
        }
      }
      toString() {
        return this.value;
      }
      test(version) {
        debug("Comparator.test", version, this.options.loose);
        if (this.semver === ANY || version === ANY) {
          return true;
        }
        if (typeof version === "string") {
          try {
            version = new SemVer(version, this.options);
          } catch (er) {
            return false;
          }
        }
        return cmp(version, this.operator, this.semver, this.options);
      }
      intersects(comp, options) {
        if (!(comp instanceof Comparator)) {
          throw new TypeError("a Comparator is required");
        }
        if (!options || typeof options !== "object") {
          options = {
            loose: !!options,
            includePrerelease: false
          };
        }
        if (this.operator === "") {
          if (this.value === "") {
            return true;
          }
          return new Range(comp.value, options).test(this.value);
        } else if (comp.operator === "") {
          if (comp.value === "") {
            return true;
          }
          return new Range(this.value, options).test(comp.semver);
        }
        const sameDirectionIncreasing = (this.operator === ">=" || this.operator === ">") && (comp.operator === ">=" || comp.operator === ">");
        const sameDirectionDecreasing = (this.operator === "<=" || this.operator === "<") && (comp.operator === "<=" || comp.operator === "<");
        const sameSemVer = this.semver.version === comp.semver.version;
        const differentDirectionsInclusive = (this.operator === ">=" || this.operator === "<=") && (comp.operator === ">=" || comp.operator === "<=");
        const oppositeDirectionsLessThan = cmp(this.semver, "<", comp.semver, options) && (this.operator === ">=" || this.operator === ">") && (comp.operator === "<=" || comp.operator === "<");
        const oppositeDirectionsGreaterThan = cmp(this.semver, ">", comp.semver, options) && (this.operator === "<=" || this.operator === "<") && (comp.operator === ">=" || comp.operator === ">");
        return sameDirectionIncreasing || sameDirectionDecreasing || sameSemVer && differentDirectionsInclusive || oppositeDirectionsLessThan || oppositeDirectionsGreaterThan;
      }
    };
    module2.exports = Comparator;
    var parseOptions = require_parse_options();
    var { re, t } = require_re();
    var cmp = require_cmp();
    var debug = require_debug();
    var SemVer = require_semver();
    var Range = require_range();
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/satisfies.js
var require_satisfies = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/functions/satisfies.js"(exports, module2) {
    init_cjs_shims();
    var Range = require_range();
    var satisfies = (version, range, options) => {
      try {
        range = new Range(range, options);
      } catch (er) {
        return false;
      }
      return range.test(version);
    };
    module2.exports = satisfies;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/to-comparators.js
var require_to_comparators = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/to-comparators.js"(exports, module2) {
    init_cjs_shims();
    var Range = require_range();
    var toComparators = (range, options) => new Range(range, options).set.map((comp) => comp.map((c) => c.value).join(" ").trim().split(" "));
    module2.exports = toComparators;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/max-satisfying.js
var require_max_satisfying = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/max-satisfying.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var Range = require_range();
    var maxSatisfying = (versions, range, options) => {
      let max = null;
      let maxSV = null;
      let rangeObj = null;
      try {
        rangeObj = new Range(range, options);
      } catch (er) {
        return null;
      }
      versions.forEach((v) => {
        if (rangeObj.test(v)) {
          if (!max || maxSV.compare(v) === -1) {
            max = v;
            maxSV = new SemVer(max, options);
          }
        }
      });
      return max;
    };
    module2.exports = maxSatisfying;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/min-satisfying.js
var require_min_satisfying = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/min-satisfying.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var Range = require_range();
    var minSatisfying = (versions, range, options) => {
      let min = null;
      let minSV = null;
      let rangeObj = null;
      try {
        rangeObj = new Range(range, options);
      } catch (er) {
        return null;
      }
      versions.forEach((v) => {
        if (rangeObj.test(v)) {
          if (!min || minSV.compare(v) === 1) {
            min = v;
            minSV = new SemVer(min, options);
          }
        }
      });
      return min;
    };
    module2.exports = minSatisfying;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/min-version.js
var require_min_version = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/min-version.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var Range = require_range();
    var gt = require_gt();
    var minVersion = (range, loose) => {
      range = new Range(range, loose);
      let minver = new SemVer("0.0.0");
      if (range.test(minver)) {
        return minver;
      }
      minver = new SemVer("0.0.0-0");
      if (range.test(minver)) {
        return minver;
      }
      minver = null;
      for (let i = 0; i < range.set.length; ++i) {
        const comparators = range.set[i];
        let setMin = null;
        comparators.forEach((comparator) => {
          const compver = new SemVer(comparator.semver.version);
          switch (comparator.operator) {
            case ">":
              if (compver.prerelease.length === 0) {
                compver.patch++;
              } else {
                compver.prerelease.push(0);
              }
              compver.raw = compver.format();
            case "":
            case ">=":
              if (!setMin || gt(compver, setMin)) {
                setMin = compver;
              }
              break;
            case "<":
            case "<=":
              break;
            default:
              throw new Error(`Unexpected operation: ${comparator.operator}`);
          }
        });
        if (setMin && (!minver || gt(minver, setMin))) {
          minver = setMin;
        }
      }
      if (minver && range.test(minver)) {
        return minver;
      }
      return null;
    };
    module2.exports = minVersion;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/valid.js
var require_valid2 = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/valid.js"(exports, module2) {
    init_cjs_shims();
    var Range = require_range();
    var validRange = (range, options) => {
      try {
        return new Range(range, options).range || "*";
      } catch (er) {
        return null;
      }
    };
    module2.exports = validRange;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/outside.js
var require_outside = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/outside.js"(exports, module2) {
    init_cjs_shims();
    var SemVer = require_semver();
    var Comparator = require_comparator();
    var { ANY } = Comparator;
    var Range = require_range();
    var satisfies = require_satisfies();
    var gt = require_gt();
    var lt = require_lt();
    var lte = require_lte();
    var gte = require_gte();
    var outside = (version, range, hilo, options) => {
      version = new SemVer(version, options);
      range = new Range(range, options);
      let gtfn, ltefn, ltfn, comp, ecomp;
      switch (hilo) {
        case ">":
          gtfn = gt;
          ltefn = lte;
          ltfn = lt;
          comp = ">";
          ecomp = ">=";
          break;
        case "<":
          gtfn = lt;
          ltefn = gte;
          ltfn = gt;
          comp = "<";
          ecomp = "<=";
          break;
        default:
          throw new TypeError('Must provide a hilo val of "<" or ">"');
      }
      if (satisfies(version, range, options)) {
        return false;
      }
      for (let i = 0; i < range.set.length; ++i) {
        const comparators = range.set[i];
        let high = null;
        let low = null;
        comparators.forEach((comparator) => {
          if (comparator.semver === ANY) {
            comparator = new Comparator(">=0.0.0");
          }
          high = high || comparator;
          low = low || comparator;
          if (gtfn(comparator.semver, high.semver, options)) {
            high = comparator;
          } else if (ltfn(comparator.semver, low.semver, options)) {
            low = comparator;
          }
        });
        if (high.operator === comp || high.operator === ecomp) {
          return false;
        }
        if ((!low.operator || low.operator === comp) && ltefn(version, low.semver)) {
          return false;
        } else if (low.operator === ecomp && ltfn(version, low.semver)) {
          return false;
        }
      }
      return true;
    };
    module2.exports = outside;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/gtr.js
var require_gtr = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/gtr.js"(exports, module2) {
    init_cjs_shims();
    var outside = require_outside();
    var gtr = (version, range, options) => outside(version, range, ">", options);
    module2.exports = gtr;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/ltr.js
var require_ltr = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/ltr.js"(exports, module2) {
    init_cjs_shims();
    var outside = require_outside();
    var ltr = (version, range, options) => outside(version, range, "<", options);
    module2.exports = ltr;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/intersects.js
var require_intersects = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/intersects.js"(exports, module2) {
    init_cjs_shims();
    var Range = require_range();
    var intersects = (r1, r2, options) => {
      r1 = new Range(r1, options);
      r2 = new Range(r2, options);
      return r1.intersects(r2);
    };
    module2.exports = intersects;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/simplify.js
var require_simplify = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/simplify.js"(exports, module2) {
    init_cjs_shims();
    var satisfies = require_satisfies();
    var compare = require_compare();
    module2.exports = (versions, range, options) => {
      const set = [];
      let first = null;
      let prev = null;
      const v = versions.sort((a, b) => compare(a, b, options));
      for (const version of v) {
        const included = satisfies(version, range, options);
        if (included) {
          prev = version;
          if (!first) {
            first = version;
          }
        } else {
          if (prev) {
            set.push([first, prev]);
          }
          prev = null;
          first = null;
        }
      }
      if (first) {
        set.push([first, null]);
      }
      const ranges = [];
      for (const [min, max] of set) {
        if (min === max) {
          ranges.push(min);
        } else if (!max && min === v[0]) {
          ranges.push("*");
        } else if (!max) {
          ranges.push(`>=${min}`);
        } else if (min === v[0]) {
          ranges.push(`<=${max}`);
        } else {
          ranges.push(`${min} - ${max}`);
        }
      }
      const simplified = ranges.join(" || ");
      const original = typeof range.raw === "string" ? range.raw : String(range);
      return simplified.length < original.length ? simplified : range;
    };
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/subset.js
var require_subset = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/ranges/subset.js"(exports, module2) {
    init_cjs_shims();
    var Range = require_range();
    var Comparator = require_comparator();
    var { ANY } = Comparator;
    var satisfies = require_satisfies();
    var compare = require_compare();
    var subset = (sub, dom, options = {}) => {
      if (sub === dom) {
        return true;
      }
      sub = new Range(sub, options);
      dom = new Range(dom, options);
      let sawNonNull = false;
      OUTER:
        for (const simpleSub of sub.set) {
          for (const simpleDom of dom.set) {
            const isSub = simpleSubset(simpleSub, simpleDom, options);
            sawNonNull = sawNonNull || isSub !== null;
            if (isSub) {
              continue OUTER;
            }
          }
          if (sawNonNull) {
            return false;
          }
        }
      return true;
    };
    var simpleSubset = (sub, dom, options) => {
      if (sub === dom) {
        return true;
      }
      if (sub.length === 1 && sub[0].semver === ANY) {
        if (dom.length === 1 && dom[0].semver === ANY) {
          return true;
        } else if (options.includePrerelease) {
          sub = [new Comparator(">=0.0.0-0")];
        } else {
          sub = [new Comparator(">=0.0.0")];
        }
      }
      if (dom.length === 1 && dom[0].semver === ANY) {
        if (options.includePrerelease) {
          return true;
        } else {
          dom = [new Comparator(">=0.0.0")];
        }
      }
      const eqSet = /* @__PURE__ */ new Set();
      let gt, lt;
      for (const c of sub) {
        if (c.operator === ">" || c.operator === ">=") {
          gt = higherGT(gt, c, options);
        } else if (c.operator === "<" || c.operator === "<=") {
          lt = lowerLT(lt, c, options);
        } else {
          eqSet.add(c.semver);
        }
      }
      if (eqSet.size > 1) {
        return null;
      }
      let gtltComp;
      if (gt && lt) {
        gtltComp = compare(gt.semver, lt.semver, options);
        if (gtltComp > 0) {
          return null;
        } else if (gtltComp === 0 && (gt.operator !== ">=" || lt.operator !== "<=")) {
          return null;
        }
      }
      for (const eq of eqSet) {
        if (gt && !satisfies(eq, String(gt), options)) {
          return null;
        }
        if (lt && !satisfies(eq, String(lt), options)) {
          return null;
        }
        for (const c of dom) {
          if (!satisfies(eq, String(c), options)) {
            return false;
          }
        }
        return true;
      }
      let higher, lower;
      let hasDomLT, hasDomGT;
      let needDomLTPre = lt && !options.includePrerelease && lt.semver.prerelease.length ? lt.semver : false;
      let needDomGTPre = gt && !options.includePrerelease && gt.semver.prerelease.length ? gt.semver : false;
      if (needDomLTPre && needDomLTPre.prerelease.length === 1 && lt.operator === "<" && needDomLTPre.prerelease[0] === 0) {
        needDomLTPre = false;
      }
      for (const c of dom) {
        hasDomGT = hasDomGT || c.operator === ">" || c.operator === ">=";
        hasDomLT = hasDomLT || c.operator === "<" || c.operator === "<=";
        if (gt) {
          if (needDomGTPre) {
            if (c.semver.prerelease && c.semver.prerelease.length && c.semver.major === needDomGTPre.major && c.semver.minor === needDomGTPre.minor && c.semver.patch === needDomGTPre.patch) {
              needDomGTPre = false;
            }
          }
          if (c.operator === ">" || c.operator === ">=") {
            higher = higherGT(gt, c, options);
            if (higher === c && higher !== gt) {
              return false;
            }
          } else if (gt.operator === ">=" && !satisfies(gt.semver, String(c), options)) {
            return false;
          }
        }
        if (lt) {
          if (needDomLTPre) {
            if (c.semver.prerelease && c.semver.prerelease.length && c.semver.major === needDomLTPre.major && c.semver.minor === needDomLTPre.minor && c.semver.patch === needDomLTPre.patch) {
              needDomLTPre = false;
            }
          }
          if (c.operator === "<" || c.operator === "<=") {
            lower = lowerLT(lt, c, options);
            if (lower === c && lower !== lt) {
              return false;
            }
          } else if (lt.operator === "<=" && !satisfies(lt.semver, String(c), options)) {
            return false;
          }
        }
        if (!c.operator && (lt || gt) && gtltComp !== 0) {
          return false;
        }
      }
      if (gt && hasDomLT && !lt && gtltComp !== 0) {
        return false;
      }
      if (lt && hasDomGT && !gt && gtltComp !== 0) {
        return false;
      }
      if (needDomGTPre || needDomLTPre) {
        return false;
      }
      return true;
    };
    var higherGT = (a, b, options) => {
      if (!a) {
        return b;
      }
      const comp = compare(a.semver, b.semver, options);
      return comp > 0 ? a : comp < 0 ? b : b.operator === ">" && a.operator === ">=" ? b : a;
    };
    var lowerLT = (a, b, options) => {
      if (!a) {
        return b;
      }
      const comp = compare(a.semver, b.semver, options);
      return comp < 0 ? a : comp > 0 ? b : b.operator === "<" && a.operator === "<=" ? b : a;
    };
    module2.exports = subset;
  }
});

// ../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/index.js
var require_semver2 = __commonJS({
  "../../node_modules/.pnpm/semver@7.3.7/node_modules/semver/index.js"(exports, module2) {
    init_cjs_shims();
    var internalRe = require_re();
    module2.exports = {
      re: internalRe.re,
      src: internalRe.src,
      tokens: internalRe.t,
      SEMVER_SPEC_VERSION: require_constants().SEMVER_SPEC_VERSION,
      SemVer: require_semver(),
      compareIdentifiers: require_identifiers().compareIdentifiers,
      rcompareIdentifiers: require_identifiers().rcompareIdentifiers,
      parse: require_parse(),
      valid: require_valid(),
      clean: require_clean(),
      inc: require_inc(),
      diff: require_diff(),
      major: require_major(),
      minor: require_minor(),
      patch: require_patch(),
      prerelease: require_prerelease(),
      compare: require_compare(),
      rcompare: require_rcompare(),
      compareLoose: require_compare_loose(),
      compareBuild: require_compare_build(),
      sort: require_sort(),
      rsort: require_rsort(),
      gt: require_gt(),
      lt: require_lt(),
      eq: require_eq(),
      neq: require_neq(),
      gte: require_gte(),
      lte: require_lte(),
      cmp: require_cmp(),
      coerce: require_coerce(),
      Comparator: require_comparator(),
      Range: require_range(),
      satisfies: require_satisfies(),
      toComparators: require_to_comparators(),
      maxSatisfying: require_max_satisfying(),
      minSatisfying: require_min_satisfying(),
      minVersion: require_min_version(),
      validRange: require_valid2(),
      outside: require_outside(),
      gtr: require_gtr(),
      ltr: require_ltr(),
      intersects: require_intersects(),
      simplifyRange: require_simplify(),
      subset: require_subset()
    };
  }
});

// ../../node_modules/.pnpm/defer-to-connect@2.0.1/node_modules/defer-to-connect/dist/source/index.js
var require_source = __commonJS({
  "../../node_modules/.pnpm/defer-to-connect@2.0.1/node_modules/defer-to-connect/dist/source/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    Object.defineProperty(exports, "__esModule", { value: true });
    function isTLSSocket(socket) {
      return socket.encrypted;
    }
    var deferToConnect2 = (socket, fn) => {
      let listeners;
      if (typeof fn === "function") {
        const connect = fn;
        listeners = { connect };
      } else {
        listeners = fn;
      }
      const hasConnectListener = typeof listeners.connect === "function";
      const hasSecureConnectListener = typeof listeners.secureConnect === "function";
      const hasCloseListener = typeof listeners.close === "function";
      const onConnect = () => {
        if (hasConnectListener) {
          listeners.connect();
        }
        if (isTLSSocket(socket) && hasSecureConnectListener) {
          if (socket.authorized) {
            listeners.secureConnect();
          } else if (!socket.authorizationError) {
            socket.once("secureConnect", listeners.secureConnect);
          }
        }
        if (hasCloseListener) {
          socket.once("close", listeners.close);
        }
      };
      if (socket.writable && !socket.connecting) {
        onConnect();
      } else if (socket.connecting) {
        socket.once("connect", onConnect);
      } else if (socket.destroyed && hasCloseListener) {
        listeners.close(socket._hadError);
      }
    };
    exports.default = deferToConnect2;
    module2.exports = deferToConnect2;
    module2.exports.default = deferToConnect2;
  }
});

// ../../node_modules/.pnpm/get-stream@6.0.1/node_modules/get-stream/buffer-stream.js
var require_buffer_stream = __commonJS({
  "../../node_modules/.pnpm/get-stream@6.0.1/node_modules/get-stream/buffer-stream.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { PassThrough: PassThroughStream2 } = require("stream");
    module2.exports = (options) => {
      options = { ...options };
      const { array } = options;
      let { encoding } = options;
      const isBuffer = encoding === "buffer";
      let objectMode = false;
      if (array) {
        objectMode = !(encoding || isBuffer);
      } else {
        encoding = encoding || "utf8";
      }
      if (isBuffer) {
        encoding = null;
      }
      const stream2 = new PassThroughStream2({ objectMode });
      if (encoding) {
        stream2.setEncoding(encoding);
      }
      let length = 0;
      const chunks = [];
      stream2.on("data", (chunk) => {
        chunks.push(chunk);
        if (objectMode) {
          length = chunks.length;
        } else {
          length += chunk.length;
        }
      });
      stream2.getBufferedValue = () => {
        if (array) {
          return chunks;
        }
        return isBuffer ? Buffer.concat(chunks, length) : chunks.join("");
      };
      stream2.getBufferedLength = () => length;
      return stream2;
    };
  }
});

// ../../node_modules/.pnpm/get-stream@6.0.1/node_modules/get-stream/index.js
var require_get_stream = __commonJS({
  "../../node_modules/.pnpm/get-stream@6.0.1/node_modules/get-stream/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { constants: BufferConstants } = require("buffer");
    var stream2 = require("stream");
    var { promisify: promisify5 } = require("util");
    var bufferStream = require_buffer_stream();
    var streamPipelinePromisified = promisify5(stream2.pipeline);
    var MaxBufferError = class extends Error {
      constructor() {
        super("maxBuffer exceeded");
        this.name = "MaxBufferError";
      }
    };
    async function getStream2(inputStream, options) {
      if (!inputStream) {
        throw new Error("Expected a stream");
      }
      options = {
        maxBuffer: Infinity,
        ...options
      };
      const { maxBuffer } = options;
      const stream3 = bufferStream(options);
      await new Promise((resolve, reject) => {
        const rejectPromise = (error) => {
          if (error && stream3.getBufferedLength() <= BufferConstants.MAX_LENGTH) {
            error.bufferedData = stream3.getBufferedValue();
          }
          reject(error);
        };
        (async () => {
          try {
            await streamPipelinePromisified(inputStream, stream3);
            resolve();
          } catch (error) {
            rejectPromise(error);
          }
        })();
        stream3.on("data", () => {
          if (stream3.getBufferedLength() > maxBuffer) {
            rejectPromise(new MaxBufferError());
          }
        });
      });
      return stream3.getBufferedValue();
    }
    module2.exports = getStream2;
    module2.exports.buffer = (stream3, options) => getStream2(stream3, { ...options, encoding: "buffer" });
    module2.exports.array = (stream3, options) => getStream2(stream3, { ...options, array: true });
    module2.exports.MaxBufferError = MaxBufferError;
  }
});

// ../../node_modules/.pnpm/http-cache-semantics@4.1.1/node_modules/http-cache-semantics/index.js
var require_http_cache_semantics = __commonJS({
  "../../node_modules/.pnpm/http-cache-semantics@4.1.1/node_modules/http-cache-semantics/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var statusCodeCacheableByDefault = /* @__PURE__ */ new Set([
      200,
      203,
      204,
      206,
      300,
      301,
      308,
      404,
      405,
      410,
      414,
      501
    ]);
    var understoodStatuses = /* @__PURE__ */ new Set([
      200,
      203,
      204,
      300,
      301,
      302,
      303,
      307,
      308,
      404,
      405,
      410,
      414,
      501
    ]);
    var errorStatusCodes = /* @__PURE__ */ new Set([
      500,
      502,
      503,
      504
    ]);
    var hopByHopHeaders = {
      date: true,
      connection: true,
      "keep-alive": true,
      "proxy-authenticate": true,
      "proxy-authorization": true,
      te: true,
      trailer: true,
      "transfer-encoding": true,
      upgrade: true
    };
    var excludedFromRevalidationUpdate = {
      "content-length": true,
      "content-encoding": true,
      "transfer-encoding": true,
      "content-range": true
    };
    function toNumberOrZero(s) {
      const n = parseInt(s, 10);
      return isFinite(n) ? n : 0;
    }
    function isErrorResponse(response) {
      if (!response) {
        return true;
      }
      return errorStatusCodes.has(response.status);
    }
    function parseCacheControl(header) {
      const cc = {};
      if (!header)
        return cc;
      const parts = header.trim().split(/,/);
      for (const part of parts) {
        const [k, v] = part.split(/=/, 2);
        cc[k.trim()] = v === void 0 ? true : v.trim().replace(/^"|"$/g, "");
      }
      return cc;
    }
    function formatCacheControl(cc) {
      let parts = [];
      for (const k in cc) {
        const v = cc[k];
        parts.push(v === true ? k : k + "=" + v);
      }
      if (!parts.length) {
        return void 0;
      }
      return parts.join(", ");
    }
    module2.exports = class CachePolicy {
      constructor(req, res, {
        shared,
        cacheHeuristic,
        immutableMinTimeToLive,
        ignoreCargoCult,
        _fromObject
      } = {}) {
        if (_fromObject) {
          this._fromObject(_fromObject);
          return;
        }
        if (!res || !res.headers) {
          throw Error("Response headers missing");
        }
        this._assertRequestHasHeaders(req);
        this._responseTime = this.now();
        this._isShared = shared !== false;
        this._cacheHeuristic = void 0 !== cacheHeuristic ? cacheHeuristic : 0.1;
        this._immutableMinTtl = void 0 !== immutableMinTimeToLive ? immutableMinTimeToLive : 24 * 3600 * 1e3;
        this._status = "status" in res ? res.status : 200;
        this._resHeaders = res.headers;
        this._rescc = parseCacheControl(res.headers["cache-control"]);
        this._method = "method" in req ? req.method : "GET";
        this._url = req.url;
        this._host = req.headers.host;
        this._noAuthorization = !req.headers.authorization;
        this._reqHeaders = res.headers.vary ? req.headers : null;
        this._reqcc = parseCacheControl(req.headers["cache-control"]);
        if (ignoreCargoCult && "pre-check" in this._rescc && "post-check" in this._rescc) {
          delete this._rescc["pre-check"];
          delete this._rescc["post-check"];
          delete this._rescc["no-cache"];
          delete this._rescc["no-store"];
          delete this._rescc["must-revalidate"];
          this._resHeaders = Object.assign({}, this._resHeaders, {
            "cache-control": formatCacheControl(this._rescc)
          });
          delete this._resHeaders.expires;
          delete this._resHeaders.pragma;
        }
        if (res.headers["cache-control"] == null && /no-cache/.test(res.headers.pragma)) {
          this._rescc["no-cache"] = true;
        }
      }
      now() {
        return Date.now();
      }
      storable() {
        return !!(!this._reqcc["no-store"] && ("GET" === this._method || "HEAD" === this._method || "POST" === this._method && this._hasExplicitExpiration()) && understoodStatuses.has(this._status) && !this._rescc["no-store"] && (!this._isShared || !this._rescc.private) && (!this._isShared || this._noAuthorization || this._allowsStoringAuthenticated()) && (this._resHeaders.expires || this._rescc["max-age"] || this._isShared && this._rescc["s-maxage"] || this._rescc.public || statusCodeCacheableByDefault.has(this._status)));
      }
      _hasExplicitExpiration() {
        return this._isShared && this._rescc["s-maxage"] || this._rescc["max-age"] || this._resHeaders.expires;
      }
      _assertRequestHasHeaders(req) {
        if (!req || !req.headers) {
          throw Error("Request headers missing");
        }
      }
      satisfiesWithoutRevalidation(req) {
        this._assertRequestHasHeaders(req);
        const requestCC = parseCacheControl(req.headers["cache-control"]);
        if (requestCC["no-cache"] || /no-cache/.test(req.headers.pragma)) {
          return false;
        }
        if (requestCC["max-age"] && this.age() > requestCC["max-age"]) {
          return false;
        }
        if (requestCC["min-fresh"] && this.timeToLive() < 1e3 * requestCC["min-fresh"]) {
          return false;
        }
        if (this.stale()) {
          const allowsStale = requestCC["max-stale"] && !this._rescc["must-revalidate"] && (true === requestCC["max-stale"] || requestCC["max-stale"] > this.age() - this.maxAge());
          if (!allowsStale) {
            return false;
          }
        }
        return this._requestMatches(req, false);
      }
      _requestMatches(req, allowHeadMethod) {
        return (!this._url || this._url === req.url) && this._host === req.headers.host && (!req.method || this._method === req.method || allowHeadMethod && "HEAD" === req.method) && this._varyMatches(req);
      }
      _allowsStoringAuthenticated() {
        return this._rescc["must-revalidate"] || this._rescc.public || this._rescc["s-maxage"];
      }
      _varyMatches(req) {
        if (!this._resHeaders.vary) {
          return true;
        }
        if (this._resHeaders.vary === "*") {
          return false;
        }
        const fields = this._resHeaders.vary.trim().toLowerCase().split(/\s*,\s*/);
        for (const name of fields) {
          if (req.headers[name] !== this._reqHeaders[name])
            return false;
        }
        return true;
      }
      _copyWithoutHopByHopHeaders(inHeaders) {
        const headers = {};
        for (const name in inHeaders) {
          if (hopByHopHeaders[name])
            continue;
          headers[name] = inHeaders[name];
        }
        if (inHeaders.connection) {
          const tokens = inHeaders.connection.trim().split(/\s*,\s*/);
          for (const name of tokens) {
            delete headers[name];
          }
        }
        if (headers.warning) {
          const warnings = headers.warning.split(/,/).filter((warning) => {
            return !/^\s*1[0-9][0-9]/.test(warning);
          });
          if (!warnings.length) {
            delete headers.warning;
          } else {
            headers.warning = warnings.join(",").trim();
          }
        }
        return headers;
      }
      responseHeaders() {
        const headers = this._copyWithoutHopByHopHeaders(this._resHeaders);
        const age = this.age();
        if (age > 3600 * 24 && !this._hasExplicitExpiration() && this.maxAge() > 3600 * 24) {
          headers.warning = (headers.warning ? `${headers.warning}, ` : "") + '113 - "rfc7234 5.5.4"';
        }
        headers.age = `${Math.round(age)}`;
        headers.date = new Date(this.now()).toUTCString();
        return headers;
      }
      date() {
        const serverDate = Date.parse(this._resHeaders.date);
        if (isFinite(serverDate)) {
          return serverDate;
        }
        return this._responseTime;
      }
      age() {
        let age = this._ageValue();
        const residentTime = (this.now() - this._responseTime) / 1e3;
        return age + residentTime;
      }
      _ageValue() {
        return toNumberOrZero(this._resHeaders.age);
      }
      maxAge() {
        if (!this.storable() || this._rescc["no-cache"]) {
          return 0;
        }
        if (this._isShared && (this._resHeaders["set-cookie"] && !this._rescc.public && !this._rescc.immutable)) {
          return 0;
        }
        if (this._resHeaders.vary === "*") {
          return 0;
        }
        if (this._isShared) {
          if (this._rescc["proxy-revalidate"]) {
            return 0;
          }
          if (this._rescc["s-maxage"]) {
            return toNumberOrZero(this._rescc["s-maxage"]);
          }
        }
        if (this._rescc["max-age"]) {
          return toNumberOrZero(this._rescc["max-age"]);
        }
        const defaultMinTtl = this._rescc.immutable ? this._immutableMinTtl : 0;
        const serverDate = this.date();
        if (this._resHeaders.expires) {
          const expires = Date.parse(this._resHeaders.expires);
          if (Number.isNaN(expires) || expires < serverDate) {
            return 0;
          }
          return Math.max(defaultMinTtl, (expires - serverDate) / 1e3);
        }
        if (this._resHeaders["last-modified"]) {
          const lastModified = Date.parse(this._resHeaders["last-modified"]);
          if (isFinite(lastModified) && serverDate > lastModified) {
            return Math.max(
              defaultMinTtl,
              (serverDate - lastModified) / 1e3 * this._cacheHeuristic
            );
          }
        }
        return defaultMinTtl;
      }
      timeToLive() {
        const age = this.maxAge() - this.age();
        const staleIfErrorAge = age + toNumberOrZero(this._rescc["stale-if-error"]);
        const staleWhileRevalidateAge = age + toNumberOrZero(this._rescc["stale-while-revalidate"]);
        return Math.max(0, age, staleIfErrorAge, staleWhileRevalidateAge) * 1e3;
      }
      stale() {
        return this.maxAge() <= this.age();
      }
      _useStaleIfError() {
        return this.maxAge() + toNumberOrZero(this._rescc["stale-if-error"]) > this.age();
      }
      useStaleWhileRevalidate() {
        return this.maxAge() + toNumberOrZero(this._rescc["stale-while-revalidate"]) > this.age();
      }
      static fromObject(obj) {
        return new this(void 0, void 0, { _fromObject: obj });
      }
      _fromObject(obj) {
        if (this._responseTime)
          throw Error("Reinitialized");
        if (!obj || obj.v !== 1)
          throw Error("Invalid serialization");
        this._responseTime = obj.t;
        this._isShared = obj.sh;
        this._cacheHeuristic = obj.ch;
        this._immutableMinTtl = obj.imm !== void 0 ? obj.imm : 24 * 3600 * 1e3;
        this._status = obj.st;
        this._resHeaders = obj.resh;
        this._rescc = obj.rescc;
        this._method = obj.m;
        this._url = obj.u;
        this._host = obj.h;
        this._noAuthorization = obj.a;
        this._reqHeaders = obj.reqh;
        this._reqcc = obj.reqcc;
      }
      toObject() {
        return {
          v: 1,
          t: this._responseTime,
          sh: this._isShared,
          ch: this._cacheHeuristic,
          imm: this._immutableMinTtl,
          st: this._status,
          resh: this._resHeaders,
          rescc: this._rescc,
          m: this._method,
          u: this._url,
          h: this._host,
          a: this._noAuthorization,
          reqh: this._reqHeaders,
          reqcc: this._reqcc
        };
      }
      revalidationHeaders(incomingReq) {
        this._assertRequestHasHeaders(incomingReq);
        const headers = this._copyWithoutHopByHopHeaders(incomingReq.headers);
        delete headers["if-range"];
        if (!this._requestMatches(incomingReq, true) || !this.storable()) {
          delete headers["if-none-match"];
          delete headers["if-modified-since"];
          return headers;
        }
        if (this._resHeaders.etag) {
          headers["if-none-match"] = headers["if-none-match"] ? `${headers["if-none-match"]}, ${this._resHeaders.etag}` : this._resHeaders.etag;
        }
        const forbidsWeakValidators = headers["accept-ranges"] || headers["if-match"] || headers["if-unmodified-since"] || this._method && this._method != "GET";
        if (forbidsWeakValidators) {
          delete headers["if-modified-since"];
          if (headers["if-none-match"]) {
            const etags = headers["if-none-match"].split(/,/).filter((etag) => {
              return !/^\s*W\//.test(etag);
            });
            if (!etags.length) {
              delete headers["if-none-match"];
            } else {
              headers["if-none-match"] = etags.join(",").trim();
            }
          }
        } else if (this._resHeaders["last-modified"] && !headers["if-modified-since"]) {
          headers["if-modified-since"] = this._resHeaders["last-modified"];
        }
        return headers;
      }
      revalidatedPolicy(request, response) {
        this._assertRequestHasHeaders(request);
        if (this._useStaleIfError() && isErrorResponse(response)) {
          return {
            modified: false,
            matches: false,
            policy: this
          };
        }
        if (!response || !response.headers) {
          throw Error("Response headers missing");
        }
        let matches = false;
        if (response.status !== void 0 && response.status != 304) {
          matches = false;
        } else if (response.headers.etag && !/^\s*W\//.test(response.headers.etag)) {
          matches = this._resHeaders.etag && this._resHeaders.etag.replace(/^\s*W\//, "") === response.headers.etag;
        } else if (this._resHeaders.etag && response.headers.etag) {
          matches = this._resHeaders.etag.replace(/^\s*W\//, "") === response.headers.etag.replace(/^\s*W\//, "");
        } else if (this._resHeaders["last-modified"]) {
          matches = this._resHeaders["last-modified"] === response.headers["last-modified"];
        } else {
          if (!this._resHeaders.etag && !this._resHeaders["last-modified"] && !response.headers.etag && !response.headers["last-modified"]) {
            matches = true;
          }
        }
        if (!matches) {
          return {
            policy: new this.constructor(request, response),
            modified: response.status != 304,
            matches: false
          };
        }
        const headers = {};
        for (const k in this._resHeaders) {
          headers[k] = k in response.headers && !excludedFromRevalidationUpdate[k] ? response.headers[k] : this._resHeaders[k];
        }
        const newResponse = Object.assign({}, response, {
          status: this._status,
          method: this._method,
          headers
        });
        return {
          policy: new this.constructor(request, newResponse, {
            shared: this._isShared,
            cacheHeuristic: this._cacheHeuristic,
            immutableMinTimeToLive: this._immutableMinTtl
          }),
          modified: false,
          matches: true
        };
      }
    };
  }
});

// ../../node_modules/.pnpm/json-buffer@3.0.1/node_modules/json-buffer/index.js
var require_json_buffer = __commonJS({
  "../../node_modules/.pnpm/json-buffer@3.0.1/node_modules/json-buffer/index.js"(exports) {
    init_cjs_shims();
    exports.stringify = function stringify(o) {
      if ("undefined" == typeof o)
        return o;
      if (o && Buffer.isBuffer(o))
        return JSON.stringify(":base64:" + o.toString("base64"));
      if (o && o.toJSON)
        o = o.toJSON();
      if (o && "object" === typeof o) {
        var s = "";
        var array = Array.isArray(o);
        s = array ? "[" : "{";
        var first = true;
        for (var k in o) {
          var ignore = "function" == typeof o[k] || !array && "undefined" === typeof o[k];
          if (Object.hasOwnProperty.call(o, k) && !ignore) {
            if (!first)
              s += ",";
            first = false;
            if (array) {
              if (o[k] == void 0)
                s += "null";
              else
                s += stringify(o[k]);
            } else if (o[k] !== void 0) {
              s += stringify(k) + ":" + stringify(o[k]);
            }
          }
        }
        s += array ? "]" : "}";
        return s;
      } else if ("string" === typeof o) {
        return JSON.stringify(/^:/.test(o) ? ":" + o : o);
      } else if ("undefined" === typeof o) {
        return "null";
      } else
        return JSON.stringify(o);
    };
    exports.parse = function(s) {
      return JSON.parse(s, function(key, value) {
        if ("string" === typeof value) {
          if (/^:base64:/.test(value))
            return Buffer.from(value.substring(8), "base64");
          else
            return /^:/.test(value) ? value.substring(1) : value;
        }
        return value;
      });
    };
  }
});

// ../../node_modules/.pnpm/keyv@4.5.2/node_modules/keyv/src/index.js
var require_src = __commonJS({
  "../../node_modules/.pnpm/keyv@4.5.2/node_modules/keyv/src/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var EventEmitter3 = require("events");
    var JSONB = require_json_buffer();
    var loadStore = (options) => {
      const adapters = {
        redis: "@keyv/redis",
        rediss: "@keyv/redis",
        mongodb: "@keyv/mongo",
        mongo: "@keyv/mongo",
        sqlite: "@keyv/sqlite",
        postgresql: "@keyv/postgres",
        postgres: "@keyv/postgres",
        mysql: "@keyv/mysql",
        etcd: "@keyv/etcd",
        offline: "@keyv/offline",
        tiered: "@keyv/tiered"
      };
      if (options.adapter || options.uri) {
        const adapter = options.adapter || /^[^:+]*/.exec(options.uri)[0];
        return new (require(adapters[adapter]))(options);
      }
      return /* @__PURE__ */ new Map();
    };
    var iterableAdapters = [
      "sqlite",
      "postgres",
      "mysql",
      "mongo",
      "redis",
      "tiered"
    ];
    var Keyv2 = class extends EventEmitter3 {
      constructor(uri, { emitErrors = true, ...options } = {}) {
        super();
        this.opts = {
          namespace: "keyv",
          serialize: JSONB.stringify,
          deserialize: JSONB.parse,
          ...typeof uri === "string" ? { uri } : uri,
          ...options
        };
        if (!this.opts.store) {
          const adapterOptions = { ...this.opts };
          this.opts.store = loadStore(adapterOptions);
        }
        if (this.opts.compression) {
          const compression = this.opts.compression;
          this.opts.serialize = compression.serialize.bind(compression);
          this.opts.deserialize = compression.deserialize.bind(compression);
        }
        if (typeof this.opts.store.on === "function" && emitErrors) {
          this.opts.store.on("error", (error) => this.emit("error", error));
        }
        this.opts.store.namespace = this.opts.namespace;
        const generateIterator = (iterator) => async function* () {
          for await (const [key, raw] of typeof iterator === "function" ? iterator(this.opts.store.namespace) : iterator) {
            const data = this.opts.deserialize(raw);
            if (this.opts.store.namespace && !key.includes(this.opts.store.namespace)) {
              continue;
            }
            if (typeof data.expires === "number" && Date.now() > data.expires) {
              this.delete(key);
              continue;
            }
            yield [this._getKeyUnprefix(key), data.value];
          }
        };
        if (typeof this.opts.store[Symbol.iterator] === "function" && this.opts.store instanceof Map) {
          this.iterator = generateIterator(this.opts.store);
        } else if (typeof this.opts.store.iterator === "function" && this.opts.store.opts && this._checkIterableAdaptar()) {
          this.iterator = generateIterator(this.opts.store.iterator.bind(this.opts.store));
        }
      }
      _checkIterableAdaptar() {
        return iterableAdapters.includes(this.opts.store.opts.dialect) || iterableAdapters.findIndex((element) => this.opts.store.opts.url.includes(element)) >= 0;
      }
      _getKeyPrefix(key) {
        return `${this.opts.namespace}:${key}`;
      }
      _getKeyPrefixArray(keys) {
        return keys.map((key) => `${this.opts.namespace}:${key}`);
      }
      _getKeyUnprefix(key) {
        return key.split(":").splice(1).join(":");
      }
      get(key, options) {
        const { store } = this.opts;
        const isArray = Array.isArray(key);
        const keyPrefixed = isArray ? this._getKeyPrefixArray(key) : this._getKeyPrefix(key);
        if (isArray && store.getMany === void 0) {
          const promises = [];
          for (const key2 of keyPrefixed) {
            promises.push(
              Promise.resolve().then(() => store.get(key2)).then((data) => typeof data === "string" ? this.opts.deserialize(data) : this.opts.compression ? this.opts.deserialize(data) : data).then((data) => {
                if (data === void 0 || data === null) {
                  return void 0;
                }
                if (typeof data.expires === "number" && Date.now() > data.expires) {
                  return this.delete(key2).then(() => void 0);
                }
                return options && options.raw ? data : data.value;
              })
            );
          }
          return Promise.allSettled(promises).then((values) => {
            const data = [];
            for (const value of values) {
              data.push(value.value);
            }
            return data;
          });
        }
        return Promise.resolve().then(() => isArray ? store.getMany(keyPrefixed) : store.get(keyPrefixed)).then((data) => typeof data === "string" ? this.opts.deserialize(data) : this.opts.compression ? this.opts.deserialize(data) : data).then((data) => {
          if (data === void 0 || data === null) {
            return void 0;
          }
          if (isArray) {
            const result = [];
            for (let row of data) {
              if (typeof row === "string") {
                row = this.opts.deserialize(row);
              }
              if (row === void 0 || row === null) {
                result.push(void 0);
                continue;
              }
              if (typeof row.expires === "number" && Date.now() > row.expires) {
                this.delete(key).then(() => void 0);
                result.push(void 0);
              } else {
                result.push(options && options.raw ? row : row.value);
              }
            }
            return result;
          }
          if (typeof data.expires === "number" && Date.now() > data.expires) {
            return this.delete(key).then(() => void 0);
          }
          return options && options.raw ? data : data.value;
        });
      }
      set(key, value, ttl2) {
        const keyPrefixed = this._getKeyPrefix(key);
        if (typeof ttl2 === "undefined") {
          ttl2 = this.opts.ttl;
        }
        if (ttl2 === 0) {
          ttl2 = void 0;
        }
        const { store } = this.opts;
        return Promise.resolve().then(() => {
          const expires = typeof ttl2 === "number" ? Date.now() + ttl2 : null;
          if (typeof value === "symbol") {
            this.emit("error", "symbol cannot be serialized");
          }
          value = { value, expires };
          return this.opts.serialize(value);
        }).then((value2) => store.set(keyPrefixed, value2, ttl2)).then(() => true);
      }
      delete(key) {
        const { store } = this.opts;
        if (Array.isArray(key)) {
          const keyPrefixed2 = this._getKeyPrefixArray(key);
          if (store.deleteMany === void 0) {
            const promises = [];
            for (const key2 of keyPrefixed2) {
              promises.push(store.delete(key2));
            }
            return Promise.allSettled(promises).then((values) => values.every((x) => x.value === true));
          }
          return Promise.resolve().then(() => store.deleteMany(keyPrefixed2));
        }
        const keyPrefixed = this._getKeyPrefix(key);
        return Promise.resolve().then(() => store.delete(keyPrefixed));
      }
      clear() {
        const { store } = this.opts;
        return Promise.resolve().then(() => store.clear());
      }
      has(key) {
        const keyPrefixed = this._getKeyPrefix(key);
        const { store } = this.opts;
        return Promise.resolve().then(async () => {
          if (typeof store.has === "function") {
            return store.has(keyPrefixed);
          }
          const value = await store.get(keyPrefixed);
          return value !== void 0;
        });
      }
      disconnect() {
        const { store } = this.opts;
        if (typeof store.disconnect === "function") {
          return store.disconnect();
        }
      }
    };
    module2.exports = Keyv2;
  }
});

// ../../node_modules/.pnpm/mimic-response@3.1.0/node_modules/mimic-response/index.js
var require_mimic_response = __commonJS({
  "../../node_modules/.pnpm/mimic-response@3.1.0/node_modules/mimic-response/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var knownProperties2 = [
      "aborted",
      "complete",
      "headers",
      "httpVersion",
      "httpVersionMinor",
      "httpVersionMajor",
      "method",
      "rawHeaders",
      "rawTrailers",
      "setTimeout",
      "socket",
      "statusCode",
      "statusMessage",
      "trailers",
      "url"
    ];
    module2.exports = (fromStream, toStream) => {
      if (toStream._readableState.autoDestroy) {
        throw new Error("The second stream must have the `autoDestroy` option set to `false`");
      }
      const fromProperties = new Set(Object.keys(fromStream).concat(knownProperties2));
      const properties = {};
      for (const property of fromProperties) {
        if (property in toStream) {
          continue;
        }
        properties[property] = {
          get() {
            const value = fromStream[property];
            const isFunction2 = typeof value === "function";
            return isFunction2 ? value.bind(fromStream) : value;
          },
          set(value) {
            fromStream[property] = value;
          },
          enumerable: true,
          configurable: false
        };
      }
      Object.defineProperties(toStream, properties);
      fromStream.once("aborted", () => {
        toStream.destroy();
        toStream.emit("aborted");
      });
      fromStream.once("close", () => {
        if (fromStream.complete) {
          if (toStream.readable) {
            toStream.once("end", () => {
              toStream.emit("close");
            });
          } else {
            toStream.emit("close");
          }
        } else {
          toStream.emit("close");
        }
      });
      return toStream;
    };
  }
});

// ../../node_modules/.pnpm/decompress-response@6.0.0/node_modules/decompress-response/index.js
var require_decompress_response = __commonJS({
  "../../node_modules/.pnpm/decompress-response@6.0.0/node_modules/decompress-response/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { Transform, PassThrough } = require("stream");
    var zlib = require("zlib");
    var mimicResponse2 = require_mimic_response();
    module2.exports = (response) => {
      const contentEncoding = (response.headers["content-encoding"] || "").toLowerCase();
      if (!["gzip", "deflate", "br"].includes(contentEncoding)) {
        return response;
      }
      const isBrotli = contentEncoding === "br";
      if (isBrotli && typeof zlib.createBrotliDecompress !== "function") {
        response.destroy(new Error("Brotli is not supported on Node.js < 12"));
        return response;
      }
      let isEmpty = true;
      const checker = new Transform({
        transform(data, _encoding, callback) {
          isEmpty = false;
          callback(null, data);
        },
        flush(callback) {
          callback();
        }
      });
      const finalStream = new PassThrough({
        autoDestroy: false,
        destroy(error, callback) {
          response.destroy();
          callback(error);
        }
      });
      const decompressStream = isBrotli ? zlib.createBrotliDecompress() : zlib.createUnzip();
      decompressStream.once("error", (error) => {
        if (isEmpty && !response.readable) {
          finalStream.end();
          return;
        }
        finalStream.destroy(error);
      });
      mimicResponse2(response, finalStream);
      response.pipe(checker).pipe(decompressStream).pipe(finalStream);
      return finalStream;
    };
  }
});

// ../../node_modules/.pnpm/quick-lru@5.1.1/node_modules/quick-lru/index.js
var require_quick_lru = __commonJS({
  "../../node_modules/.pnpm/quick-lru@5.1.1/node_modules/quick-lru/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var QuickLRU = class {
      constructor(options = {}) {
        if (!(options.maxSize && options.maxSize > 0)) {
          throw new TypeError("`maxSize` must be a number greater than 0");
        }
        this.maxSize = options.maxSize;
        this.onEviction = options.onEviction;
        this.cache = /* @__PURE__ */ new Map();
        this.oldCache = /* @__PURE__ */ new Map();
        this._size = 0;
      }
      _set(key, value) {
        this.cache.set(key, value);
        this._size++;
        if (this._size >= this.maxSize) {
          this._size = 0;
          if (typeof this.onEviction === "function") {
            for (const [key2, value2] of this.oldCache.entries()) {
              this.onEviction(key2, value2);
            }
          }
          this.oldCache = this.cache;
          this.cache = /* @__PURE__ */ new Map();
        }
      }
      get(key) {
        if (this.cache.has(key)) {
          return this.cache.get(key);
        }
        if (this.oldCache.has(key)) {
          const value = this.oldCache.get(key);
          this.oldCache.delete(key);
          this._set(key, value);
          return value;
        }
      }
      set(key, value) {
        if (this.cache.has(key)) {
          this.cache.set(key, value);
        } else {
          this._set(key, value);
        }
        return this;
      }
      has(key) {
        return this.cache.has(key) || this.oldCache.has(key);
      }
      peek(key) {
        if (this.cache.has(key)) {
          return this.cache.get(key);
        }
        if (this.oldCache.has(key)) {
          return this.oldCache.get(key);
        }
      }
      delete(key) {
        const deleted = this.cache.delete(key);
        if (deleted) {
          this._size--;
        }
        return this.oldCache.delete(key) || deleted;
      }
      clear() {
        this.cache.clear();
        this.oldCache.clear();
        this._size = 0;
      }
      *keys() {
        for (const [key] of this) {
          yield key;
        }
      }
      *values() {
        for (const [, value] of this) {
          yield value;
        }
      }
      *[Symbol.iterator]() {
        for (const item of this.cache) {
          yield item;
        }
        for (const item of this.oldCache) {
          const [key] = item;
          if (!this.cache.has(key)) {
            yield item;
          }
        }
      }
      get size() {
        let oldCacheSize = 0;
        for (const key of this.oldCache.keys()) {
          if (!this.cache.has(key)) {
            oldCacheSize++;
          }
        }
        return Math.min(this._size + oldCacheSize, this.maxSize);
      }
    };
    module2.exports = QuickLRU;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/delay-async-destroy.js
var require_delay_async_destroy = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/delay-async-destroy.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = (stream2) => {
      if (stream2.listenerCount("error") !== 0) {
        return stream2;
      }
      stream2.__destroy = stream2._destroy;
      stream2._destroy = (...args) => {
        const callback = args.pop();
        stream2.__destroy(...args, async (error) => {
          await Promise.resolve();
          callback(error);
        });
      };
      const onError = (error) => {
        Promise.resolve().then(() => {
          stream2.emit("error", error);
        });
      };
      stream2.once("error", onError);
      Promise.resolve().then(() => {
        stream2.off("error", onError);
      });
      return stream2;
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/agent.js
var require_agent = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/agent.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { URL: URL4 } = require("url");
    var EventEmitter3 = require("events");
    var tls = require("tls");
    var http22 = require("http2");
    var QuickLRU = require_quick_lru();
    var delayAsyncDestroy = require_delay_async_destroy();
    var kCurrentStreamCount = Symbol("currentStreamCount");
    var kRequest = Symbol("request");
    var kOriginSet = Symbol("cachedOriginSet");
    var kGracefullyClosing = Symbol("gracefullyClosing");
    var kLength = Symbol("length");
    var nameKeys = [
      "createConnection",
      "maxDeflateDynamicTableSize",
      "maxSettings",
      "maxSessionMemory",
      "maxHeaderListPairs",
      "maxOutstandingPings",
      "maxReservedRemoteStreams",
      "maxSendHeaderBlockLength",
      "paddingStrategy",
      "peerMaxConcurrentStreams",
      "settings",
      "family",
      "localAddress",
      "rejectUnauthorized",
      "pskCallback",
      "minDHSize",
      "path",
      "socket",
      "ca",
      "cert",
      "sigalgs",
      "ciphers",
      "clientCertEngine",
      "crl",
      "dhparam",
      "ecdhCurve",
      "honorCipherOrder",
      "key",
      "privateKeyEngine",
      "privateKeyIdentifier",
      "maxVersion",
      "minVersion",
      "pfx",
      "secureOptions",
      "secureProtocol",
      "sessionIdContext",
      "ticketKeys"
    ];
    var getSortedIndex = (array, value, compare) => {
      let low = 0;
      let high = array.length;
      while (low < high) {
        const mid = low + high >>> 1;
        if (compare(array[mid], value)) {
          low = mid + 1;
        } else {
          high = mid;
        }
      }
      return low;
    };
    var compareSessions = (a, b) => a.remoteSettings.maxConcurrentStreams > b.remoteSettings.maxConcurrentStreams;
    var closeCoveredSessions = (where, session) => {
      for (let index = 0; index < where.length; index++) {
        const coveredSession = where[index];
        if (coveredSession[kOriginSet].length > 0 && coveredSession[kOriginSet].length < session[kOriginSet].length && coveredSession[kOriginSet].every((origin) => session[kOriginSet].includes(origin)) && coveredSession[kCurrentStreamCount] + session[kCurrentStreamCount] <= session.remoteSettings.maxConcurrentStreams) {
          gracefullyClose(coveredSession);
        }
      }
    };
    var closeSessionIfCovered = (where, coveredSession) => {
      for (let index = 0; index < where.length; index++) {
        const session = where[index];
        if (coveredSession[kOriginSet].length > 0 && coveredSession[kOriginSet].length < session[kOriginSet].length && coveredSession[kOriginSet].every((origin) => session[kOriginSet].includes(origin)) && coveredSession[kCurrentStreamCount] + session[kCurrentStreamCount] <= session.remoteSettings.maxConcurrentStreams) {
          gracefullyClose(coveredSession);
          return true;
        }
      }
      return false;
    };
    var gracefullyClose = (session) => {
      session[kGracefullyClosing] = true;
      if (session[kCurrentStreamCount] === 0) {
        session.close();
      }
    };
    var Agent = class extends EventEmitter3 {
      constructor({ timeout = 0, maxSessions = Number.POSITIVE_INFINITY, maxEmptySessions = 10, maxCachedTlsSessions = 100 } = {}) {
        super();
        this.sessions = {};
        this.queue = {};
        this.timeout = timeout;
        this.maxSessions = maxSessions;
        this.maxEmptySessions = maxEmptySessions;
        this._emptySessionCount = 0;
        this._sessionCount = 0;
        this.settings = {
          enablePush: false,
          initialWindowSize: 1024 * 1024 * 32
        };
        this.tlsSessionCache = new QuickLRU({ maxSize: maxCachedTlsSessions });
      }
      get protocol() {
        return "https:";
      }
      normalizeOptions(options) {
        let normalized = "";
        for (let index = 0; index < nameKeys.length; index++) {
          const key = nameKeys[index];
          normalized += ":";
          if (options && options[key] !== void 0) {
            normalized += options[key];
          }
        }
        return normalized;
      }
      _processQueue() {
        if (this._sessionCount >= this.maxSessions) {
          this.closeEmptySessions(this.maxSessions - this._sessionCount + 1);
          return;
        }
        for (const normalizedOptions in this.queue) {
          for (const normalizedOrigin in this.queue[normalizedOptions]) {
            const item = this.queue[normalizedOptions][normalizedOrigin];
            if (!item.completed) {
              item.completed = true;
              item();
            }
          }
        }
      }
      _isBetterSession(thisStreamCount, thatStreamCount) {
        return thisStreamCount > thatStreamCount;
      }
      _accept(session, listeners, normalizedOrigin, options) {
        let index = 0;
        while (index < listeners.length && session[kCurrentStreamCount] < session.remoteSettings.maxConcurrentStreams) {
          listeners[index].resolve(session);
          index++;
        }
        listeners.splice(0, index);
        if (listeners.length > 0) {
          this.getSession(normalizedOrigin, options, listeners);
          listeners.length = 0;
        }
      }
      getSession(origin, options, listeners) {
        return new Promise((resolve, reject) => {
          if (Array.isArray(listeners) && listeners.length > 0) {
            listeners = [...listeners];
            resolve();
          } else {
            listeners = [{ resolve, reject }];
          }
          try {
            if (typeof origin === "string") {
              origin = new URL4(origin);
            } else if (!(origin instanceof URL4)) {
              throw new TypeError("The `origin` argument needs to be a string or an URL object");
            }
            if (options) {
              const { servername } = options;
              const { hostname } = origin;
              if (servername && hostname !== servername) {
                throw new Error(`Origin ${hostname} differs from servername ${servername}`);
              }
            }
          } catch (error) {
            for (let index = 0; index < listeners.length; index++) {
              listeners[index].reject(error);
            }
            return;
          }
          const normalizedOptions = this.normalizeOptions(options);
          const normalizedOrigin = origin.origin;
          if (normalizedOptions in this.sessions) {
            const sessions = this.sessions[normalizedOptions];
            let maxConcurrentStreams = -1;
            let currentStreamsCount = -1;
            let optimalSession;
            for (let index = 0; index < sessions.length; index++) {
              const session = sessions[index];
              const sessionMaxConcurrentStreams = session.remoteSettings.maxConcurrentStreams;
              if (sessionMaxConcurrentStreams < maxConcurrentStreams) {
                break;
              }
              if (!session[kOriginSet].includes(normalizedOrigin)) {
                continue;
              }
              const sessionCurrentStreamsCount = session[kCurrentStreamCount];
              if (sessionCurrentStreamsCount >= sessionMaxConcurrentStreams || session[kGracefullyClosing] || session.destroyed) {
                continue;
              }
              if (!optimalSession) {
                maxConcurrentStreams = sessionMaxConcurrentStreams;
              }
              if (this._isBetterSession(sessionCurrentStreamsCount, currentStreamsCount)) {
                optimalSession = session;
                currentStreamsCount = sessionCurrentStreamsCount;
              }
            }
            if (optimalSession) {
              this._accept(optimalSession, listeners, normalizedOrigin, options);
              return;
            }
          }
          if (normalizedOptions in this.queue) {
            if (normalizedOrigin in this.queue[normalizedOptions]) {
              this.queue[normalizedOptions][normalizedOrigin].listeners.push(...listeners);
              return;
            }
          } else {
            this.queue[normalizedOptions] = {
              [kLength]: 0
            };
          }
          const removeFromQueue = () => {
            if (normalizedOptions in this.queue && this.queue[normalizedOptions][normalizedOrigin] === entry) {
              delete this.queue[normalizedOptions][normalizedOrigin];
              if (--this.queue[normalizedOptions][kLength] === 0) {
                delete this.queue[normalizedOptions];
              }
            }
          };
          const entry = async () => {
            this._sessionCount++;
            const name = `${normalizedOrigin}:${normalizedOptions}`;
            let receivedSettings = false;
            let socket;
            try {
              const computedOptions = { ...options };
              if (computedOptions.settings === void 0) {
                computedOptions.settings = this.settings;
              }
              if (computedOptions.session === void 0) {
                computedOptions.session = this.tlsSessionCache.get(name);
              }
              const createConnection = computedOptions.createConnection || this.createConnection;
              socket = await createConnection.call(this, origin, computedOptions);
              computedOptions.createConnection = () => socket;
              const session = http22.connect(origin, computedOptions);
              session[kCurrentStreamCount] = 0;
              session[kGracefullyClosing] = false;
              const getOriginSet = () => {
                const { socket: socket2 } = session;
                let originSet;
                if (socket2.servername === false) {
                  socket2.servername = socket2.remoteAddress;
                  originSet = session.originSet;
                  socket2.servername = false;
                } else {
                  originSet = session.originSet;
                }
                return originSet;
              };
              const isFree = () => session[kCurrentStreamCount] < session.remoteSettings.maxConcurrentStreams;
              session.socket.once("session", (tlsSession) => {
                this.tlsSessionCache.set(name, tlsSession);
              });
              session.once("error", (error) => {
                for (let index = 0; index < listeners.length; index++) {
                  listeners[index].reject(error);
                }
                this.tlsSessionCache.delete(name);
              });
              session.setTimeout(this.timeout, () => {
                session.destroy();
              });
              session.once("close", () => {
                this._sessionCount--;
                if (receivedSettings) {
                  this._emptySessionCount--;
                  const where = this.sessions[normalizedOptions];
                  if (where.length === 1) {
                    delete this.sessions[normalizedOptions];
                  } else {
                    where.splice(where.indexOf(session), 1);
                  }
                } else {
                  removeFromQueue();
                  const error = new Error("Session closed without receiving a SETTINGS frame");
                  error.code = "HTTP2WRAPPER_NOSETTINGS";
                  for (let index = 0; index < listeners.length; index++) {
                    listeners[index].reject(error);
                  }
                }
                this._processQueue();
              });
              const processListeners = () => {
                const queue = this.queue[normalizedOptions];
                if (!queue) {
                  return;
                }
                const originSet = session[kOriginSet];
                for (let index = 0; index < originSet.length; index++) {
                  const origin2 = originSet[index];
                  if (origin2 in queue) {
                    const { listeners: listeners2, completed } = queue[origin2];
                    let index2 = 0;
                    while (index2 < listeners2.length && isFree()) {
                      listeners2[index2].resolve(session);
                      index2++;
                    }
                    queue[origin2].listeners.splice(0, index2);
                    if (queue[origin2].listeners.length === 0 && !completed) {
                      delete queue[origin2];
                      if (--queue[kLength] === 0) {
                        delete this.queue[normalizedOptions];
                        break;
                      }
                    }
                    if (!isFree()) {
                      break;
                    }
                  }
                }
              };
              session.on("origin", () => {
                session[kOriginSet] = getOriginSet() || [];
                session[kGracefullyClosing] = false;
                closeSessionIfCovered(this.sessions[normalizedOptions], session);
                if (session[kGracefullyClosing] || !isFree()) {
                  return;
                }
                processListeners();
                if (!isFree()) {
                  return;
                }
                closeCoveredSessions(this.sessions[normalizedOptions], session);
              });
              session.once("remoteSettings", () => {
                if (entry.destroyed) {
                  const error = new Error("Agent has been destroyed");
                  for (let index = 0; index < listeners.length; index++) {
                    listeners[index].reject(error);
                  }
                  session.destroy();
                  return;
                }
                if (session.setLocalWindowSize) {
                  session.setLocalWindowSize(1024 * 1024 * 4);
                }
                session[kOriginSet] = getOriginSet() || [];
                if (session.socket.encrypted) {
                  const mainOrigin = session[kOriginSet][0];
                  if (mainOrigin !== normalizedOrigin) {
                    const error = new Error(`Requested origin ${normalizedOrigin} does not match server ${mainOrigin}`);
                    for (let index = 0; index < listeners.length; index++) {
                      listeners[index].reject(error);
                    }
                    session.destroy();
                    return;
                  }
                }
                removeFromQueue();
                {
                  const where = this.sessions;
                  if (normalizedOptions in where) {
                    const sessions = where[normalizedOptions];
                    sessions.splice(getSortedIndex(sessions, session, compareSessions), 0, session);
                  } else {
                    where[normalizedOptions] = [session];
                  }
                }
                receivedSettings = true;
                this._emptySessionCount++;
                this.emit("session", session);
                this._accept(session, listeners, normalizedOrigin, options);
                if (session[kCurrentStreamCount] === 0 && this._emptySessionCount > this.maxEmptySessions) {
                  this.closeEmptySessions(this._emptySessionCount - this.maxEmptySessions);
                }
                session.on("remoteSettings", () => {
                  if (!isFree()) {
                    return;
                  }
                  processListeners();
                  if (!isFree()) {
                    return;
                  }
                  closeCoveredSessions(this.sessions[normalizedOptions], session);
                });
              });
              session[kRequest] = session.request;
              session.request = (headers, streamOptions) => {
                if (session[kGracefullyClosing]) {
                  throw new Error("The session is gracefully closing. No new streams are allowed.");
                }
                const stream2 = session[kRequest](headers, streamOptions);
                session.ref();
                if (session[kCurrentStreamCount]++ === 0) {
                  this._emptySessionCount--;
                }
                stream2.once("close", () => {
                  if (--session[kCurrentStreamCount] === 0) {
                    this._emptySessionCount++;
                    session.unref();
                    if (this._emptySessionCount > this.maxEmptySessions || session[kGracefullyClosing]) {
                      session.close();
                      return;
                    }
                  }
                  if (session.destroyed || session.closed) {
                    return;
                  }
                  if (isFree() && !closeSessionIfCovered(this.sessions[normalizedOptions], session)) {
                    closeCoveredSessions(this.sessions[normalizedOptions], session);
                    processListeners();
                    if (session[kCurrentStreamCount] === 0) {
                      this._processQueue();
                    }
                  }
                });
                return stream2;
              };
            } catch (error) {
              removeFromQueue();
              this._sessionCount--;
              for (let index = 0; index < listeners.length; index++) {
                listeners[index].reject(error);
              }
            }
          };
          entry.listeners = listeners;
          entry.completed = false;
          entry.destroyed = false;
          this.queue[normalizedOptions][normalizedOrigin] = entry;
          this.queue[normalizedOptions][kLength]++;
          this._processQueue();
        });
      }
      request(origin, options, headers, streamOptions) {
        return new Promise((resolve, reject) => {
          this.getSession(origin, options, [{
            reject,
            resolve: (session) => {
              try {
                const stream2 = session.request(headers, streamOptions);
                delayAsyncDestroy(stream2);
                resolve(stream2);
              } catch (error) {
                reject(error);
              }
            }
          }]);
        });
      }
      async createConnection(origin, options) {
        return Agent.connect(origin, options);
      }
      static connect(origin, options) {
        options.ALPNProtocols = ["h2"];
        const port = origin.port || 443;
        const host = origin.hostname;
        if (typeof options.servername === "undefined") {
          options.servername = host;
        }
        const socket = tls.connect(port, host, options);
        if (options.socket) {
          socket._peername = {
            family: void 0,
            address: void 0,
            port
          };
        }
        return socket;
      }
      closeEmptySessions(maxCount = Number.POSITIVE_INFINITY) {
        let closedCount = 0;
        const { sessions } = this;
        for (const key in sessions) {
          const thisSessions = sessions[key];
          for (let index = 0; index < thisSessions.length; index++) {
            const session = thisSessions[index];
            if (session[kCurrentStreamCount] === 0) {
              closedCount++;
              session.close();
              if (closedCount >= maxCount) {
                return closedCount;
              }
            }
          }
        }
        return closedCount;
      }
      destroy(reason) {
        const { sessions, queue } = this;
        for (const key in sessions) {
          const thisSessions = sessions[key];
          for (let index = 0; index < thisSessions.length; index++) {
            thisSessions[index].destroy(reason);
          }
        }
        for (const normalizedOptions in queue) {
          const entries2 = queue[normalizedOptions];
          for (const normalizedOrigin in entries2) {
            entries2[normalizedOrigin].destroyed = true;
          }
        }
        this.queue = {};
        this.tlsSessionCache.clear();
      }
      get emptySessionCount() {
        return this._emptySessionCount;
      }
      get pendingSessionCount() {
        return this._sessionCount - this._emptySessionCount;
      }
      get sessionCount() {
        return this._sessionCount;
      }
    };
    Agent.kCurrentStreamCount = kCurrentStreamCount;
    Agent.kGracefullyClosing = kGracefullyClosing;
    module2.exports = {
      Agent,
      globalAgent: new Agent()
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/incoming-message.js
var require_incoming_message = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/incoming-message.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { Readable } = require("stream");
    var IncomingMessage = class extends Readable {
      constructor(socket, highWaterMark) {
        super({
          emitClose: false,
          autoDestroy: true,
          highWaterMark
        });
        this.statusCode = null;
        this.statusMessage = "";
        this.httpVersion = "2.0";
        this.httpVersionMajor = 2;
        this.httpVersionMinor = 0;
        this.headers = {};
        this.trailers = {};
        this.req = null;
        this.aborted = false;
        this.complete = false;
        this.upgrade = null;
        this.rawHeaders = [];
        this.rawTrailers = [];
        this.socket = socket;
        this._dumped = false;
      }
      get connection() {
        return this.socket;
      }
      set connection(value) {
        this.socket = value;
      }
      _destroy(error, callback) {
        if (!this.readableEnded) {
          this.aborted = true;
        }
        callback();
        this.req._request.destroy(error);
      }
      setTimeout(ms, callback) {
        this.req.setTimeout(ms, callback);
        return this;
      }
      _dump() {
        if (!this._dumped) {
          this._dumped = true;
          this.removeAllListeners("data");
          this.resume();
        }
      }
      _read() {
        if (this.req) {
          this.req._request.resume();
        }
      }
    };
    module2.exports = IncomingMessage;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/proxy-events.js
var require_proxy_events = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/proxy-events.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = (from, to, events) => {
      for (const event of events) {
        from.on(event, (...args) => to.emit(event, ...args));
      }
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/errors.js
var require_errors = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/errors.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var makeError = (Base, key, getMessage) => {
      module2.exports[key] = class NodeError extends Base {
        constructor(...args) {
          super(typeof getMessage === "string" ? getMessage : getMessage(args));
          this.name = `${super.name} [${key}]`;
          this.code = key;
        }
      };
    };
    makeError(TypeError, "ERR_INVALID_ARG_TYPE", (args) => {
      const type = args[0].includes(".") ? "property" : "argument";
      let valid = args[1];
      const isManyTypes = Array.isArray(valid);
      if (isManyTypes) {
        valid = `${valid.slice(0, -1).join(", ")} or ${valid.slice(-1)}`;
      }
      return `The "${args[0]}" ${type} must be ${isManyTypes ? "one of" : "of"} type ${valid}. Received ${typeof args[2]}`;
    });
    makeError(
      TypeError,
      "ERR_INVALID_PROTOCOL",
      (args) => `Protocol "${args[0]}" not supported. Expected "${args[1]}"`
    );
    makeError(
      Error,
      "ERR_HTTP_HEADERS_SENT",
      (args) => `Cannot ${args[0]} headers after they are sent to the client`
    );
    makeError(
      TypeError,
      "ERR_INVALID_HTTP_TOKEN",
      (args) => `${args[0]} must be a valid HTTP token [${args[1]}]`
    );
    makeError(
      TypeError,
      "ERR_HTTP_INVALID_HEADER_VALUE",
      (args) => `Invalid value "${args[0]} for header "${args[1]}"`
    );
    makeError(
      TypeError,
      "ERR_INVALID_CHAR",
      (args) => `Invalid character in ${args[0]} [${args[1]}]`
    );
    makeError(
      Error,
      "ERR_HTTP2_NO_SOCKET_MANIPULATION",
      "HTTP/2 sockets should not be directly manipulated (e.g. read and written)"
    );
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/is-request-pseudo-header.js
var require_is_request_pseudo_header = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/is-request-pseudo-header.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = (header) => {
      switch (header) {
        case ":method":
        case ":scheme":
        case ":authority":
        case ":path":
          return true;
        default:
          return false;
      }
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/validate-header-name.js
var require_validate_header_name = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/validate-header-name.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { ERR_INVALID_HTTP_TOKEN } = require_errors();
    var isRequestPseudoHeader = require_is_request_pseudo_header();
    var isValidHttpToken = /^[\^`\-\w!#$%&*+.|~]+$/;
    module2.exports = (name) => {
      if (typeof name !== "string" || !isValidHttpToken.test(name) && !isRequestPseudoHeader(name)) {
        throw new ERR_INVALID_HTTP_TOKEN("Header name", name);
      }
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/validate-header-value.js
var require_validate_header_value = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/validate-header-value.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var {
      ERR_HTTP_INVALID_HEADER_VALUE,
      ERR_INVALID_CHAR
    } = require_errors();
    var isInvalidHeaderValue = /[^\t\u0020-\u007E\u0080-\u00FF]/;
    module2.exports = (name, value) => {
      if (typeof value === "undefined") {
        throw new ERR_HTTP_INVALID_HEADER_VALUE(value, name);
      }
      if (isInvalidHeaderValue.test(value)) {
        throw new ERR_INVALID_CHAR("header content", name);
      }
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/proxy-socket-handler.js
var require_proxy_socket_handler = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/proxy-socket-handler.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { ERR_HTTP2_NO_SOCKET_MANIPULATION } = require_errors();
    var proxySocketHandler = {
      has(stream2, property) {
        const reference = stream2.session === void 0 ? stream2 : stream2.session.socket;
        return property in stream2 || property in reference;
      },
      get(stream2, property) {
        switch (property) {
          case "on":
          case "once":
          case "end":
          case "emit":
          case "destroy":
            return stream2[property].bind(stream2);
          case "writable":
          case "destroyed":
            return stream2[property];
          case "readable":
            if (stream2.destroyed) {
              return false;
            }
            return stream2.readable;
          case "setTimeout": {
            const { session } = stream2;
            if (session !== void 0) {
              return session.setTimeout.bind(session);
            }
            return stream2.setTimeout.bind(stream2);
          }
          case "write":
          case "read":
          case "pause":
          case "resume":
            throw new ERR_HTTP2_NO_SOCKET_MANIPULATION();
          default: {
            const reference = stream2.session === void 0 ? stream2 : stream2.session.socket;
            const value = reference[property];
            return typeof value === "function" ? value.bind(reference) : value;
          }
        }
      },
      getPrototypeOf(stream2) {
        if (stream2.session !== void 0) {
          return Reflect.getPrototypeOf(stream2.session.socket);
        }
        return Reflect.getPrototypeOf(stream2);
      },
      set(stream2, property, value) {
        switch (property) {
          case "writable":
          case "readable":
          case "destroyed":
          case "on":
          case "once":
          case "end":
          case "emit":
          case "destroy":
            stream2[property] = value;
            return true;
          case "setTimeout": {
            const { session } = stream2;
            if (session === void 0) {
              stream2.setTimeout = value;
            } else {
              session.setTimeout = value;
            }
            return true;
          }
          case "write":
          case "read":
          case "pause":
          case "resume":
            throw new ERR_HTTP2_NO_SOCKET_MANIPULATION();
          default: {
            const reference = stream2.session === void 0 ? stream2 : stream2.session.socket;
            reference[property] = value;
            return true;
          }
        }
      }
    };
    module2.exports = proxySocketHandler;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/client-request.js
var require_client_request = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/client-request.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { URL: URL4, urlToHttpOptions } = require("url");
    var http22 = require("http2");
    var { Writable } = require("stream");
    var { Agent, globalAgent } = require_agent();
    var IncomingMessage = require_incoming_message();
    var proxyEvents2 = require_proxy_events();
    var {
      ERR_INVALID_ARG_TYPE,
      ERR_INVALID_PROTOCOL,
      ERR_HTTP_HEADERS_SENT
    } = require_errors();
    var validateHeaderName = require_validate_header_name();
    var validateHeaderValue = require_validate_header_value();
    var proxySocketHandler = require_proxy_socket_handler();
    var {
      HTTP2_HEADER_STATUS,
      HTTP2_HEADER_METHOD,
      HTTP2_HEADER_PATH,
      HTTP2_HEADER_AUTHORITY,
      HTTP2_METHOD_CONNECT
    } = http22.constants;
    var kHeaders = Symbol("headers");
    var kOrigin = Symbol("origin");
    var kSession = Symbol("session");
    var kOptions = Symbol("options");
    var kFlushedHeaders = Symbol("flushedHeaders");
    var kJobs = Symbol("jobs");
    var kPendingAgentPromise = Symbol("pendingAgentPromise");
    var ClientRequest = class extends Writable {
      constructor(input, options, callback) {
        super({
          autoDestroy: false,
          emitClose: false
        });
        if (typeof input === "string") {
          input = urlToHttpOptions(new URL4(input));
        } else if (input instanceof URL4) {
          input = urlToHttpOptions(input);
        } else {
          input = { ...input };
        }
        if (typeof options === "function" || options === void 0) {
          callback = options;
          options = input;
        } else {
          options = Object.assign(input, options);
        }
        if (options.h2session) {
          this[kSession] = options.h2session;
          if (this[kSession].destroyed) {
            throw new Error("The session has been closed already");
          }
          this.protocol = this[kSession].socket.encrypted ? "https:" : "http:";
        } else if (options.agent === false) {
          this.agent = new Agent({ maxEmptySessions: 0 });
        } else if (typeof options.agent === "undefined" || options.agent === null) {
          this.agent = globalAgent;
        } else if (typeof options.agent.request === "function") {
          this.agent = options.agent;
        } else {
          throw new ERR_INVALID_ARG_TYPE("options.agent", ["http2wrapper.Agent-like Object", "undefined", "false"], options.agent);
        }
        if (this.agent) {
          this.protocol = this.agent.protocol;
        }
        if (options.protocol && options.protocol !== this.protocol) {
          throw new ERR_INVALID_PROTOCOL(options.protocol, this.protocol);
        }
        if (!options.port) {
          options.port = options.defaultPort || this.agent && this.agent.defaultPort || 443;
        }
        options.host = options.hostname || options.host || "localhost";
        delete options.hostname;
        const { timeout } = options;
        options.timeout = void 0;
        this[kHeaders] = /* @__PURE__ */ Object.create(null);
        this[kJobs] = [];
        this[kPendingAgentPromise] = void 0;
        this.socket = null;
        this.connection = null;
        this.method = options.method || "GET";
        if (!(this.method === "CONNECT" && (options.path === "/" || options.path === void 0))) {
          this.path = options.path;
        }
        this.res = null;
        this.aborted = false;
        this.reusedSocket = false;
        const { headers } = options;
        if (headers) {
          for (const header in headers) {
            this.setHeader(header, headers[header]);
          }
        }
        if (options.auth && !("authorization" in this[kHeaders])) {
          this[kHeaders].authorization = "Basic " + Buffer.from(options.auth).toString("base64");
        }
        options.session = options.tlsSession;
        options.path = options.socketPath;
        this[kOptions] = options;
        this[kOrigin] = new URL4(`${this.protocol}//${options.servername || options.host}:${options.port}`);
        const reuseSocket = options._reuseSocket;
        if (reuseSocket) {
          options.createConnection = (...args) => {
            if (reuseSocket.destroyed) {
              return this.agent.createConnection(...args);
            }
            return reuseSocket;
          };
          this.agent.getSession(this[kOrigin], this[kOptions]).catch(() => {
          });
        }
        if (timeout) {
          this.setTimeout(timeout);
        }
        if (callback) {
          this.once("response", callback);
        }
        this[kFlushedHeaders] = false;
      }
      get method() {
        return this[kHeaders][HTTP2_HEADER_METHOD];
      }
      set method(value) {
        if (value) {
          this[kHeaders][HTTP2_HEADER_METHOD] = value.toUpperCase();
        }
      }
      get path() {
        const header = this.method === "CONNECT" ? HTTP2_HEADER_AUTHORITY : HTTP2_HEADER_PATH;
        return this[kHeaders][header];
      }
      set path(value) {
        if (value) {
          const header = this.method === "CONNECT" ? HTTP2_HEADER_AUTHORITY : HTTP2_HEADER_PATH;
          this[kHeaders][header] = value;
        }
      }
      get host() {
        return this[kOrigin].hostname;
      }
      set host(_value) {
      }
      get _mustNotHaveABody() {
        return this.method === "GET" || this.method === "HEAD" || this.method === "DELETE";
      }
      _write(chunk, encoding, callback) {
        if (this._mustNotHaveABody) {
          callback(new Error("The GET, HEAD and DELETE methods must NOT have a body"));
          return;
        }
        this.flushHeaders();
        const callWrite = () => this._request.write(chunk, encoding, callback);
        if (this._request) {
          callWrite();
        } else {
          this[kJobs].push(callWrite);
        }
      }
      _final(callback) {
        this.flushHeaders();
        const callEnd = () => {
          if (this._mustNotHaveABody || this.method === "CONNECT") {
            callback();
            return;
          }
          this._request.end(callback);
        };
        if (this._request) {
          callEnd();
        } else {
          this[kJobs].push(callEnd);
        }
      }
      abort() {
        if (this.res && this.res.complete) {
          return;
        }
        if (!this.aborted) {
          process.nextTick(() => this.emit("abort"));
        }
        this.aborted = true;
        this.destroy();
      }
      async _destroy(error, callback) {
        if (this.res) {
          this.res._dump();
        }
        if (this._request) {
          this._request.destroy();
        } else {
          process.nextTick(() => {
            this.emit("close");
          });
        }
        try {
          await this[kPendingAgentPromise];
        } catch (internalError) {
          if (this.aborted) {
            error = internalError;
          }
        }
        callback(error);
      }
      async flushHeaders() {
        if (this[kFlushedHeaders] || this.destroyed) {
          return;
        }
        this[kFlushedHeaders] = true;
        const isConnectMethod = this.method === HTTP2_METHOD_CONNECT;
        const onStream = (stream2) => {
          this._request = stream2;
          if (this.destroyed) {
            stream2.destroy();
            return;
          }
          if (!isConnectMethod) {
            proxyEvents2(stream2, this, ["timeout", "continue"]);
          }
          stream2.once("error", (error) => {
            this.destroy(error);
          });
          stream2.once("aborted", () => {
            const { res } = this;
            if (res) {
              res.aborted = true;
              res.emit("aborted");
              res.destroy();
            } else {
              this.destroy(new Error("The server aborted the HTTP/2 stream"));
            }
          });
          const onResponse = (headers, flags, rawHeaders) => {
            const response = new IncomingMessage(this.socket, stream2.readableHighWaterMark);
            this.res = response;
            response.url = `${this[kOrigin].origin}${this.path}`;
            response.req = this;
            response.statusCode = headers[HTTP2_HEADER_STATUS];
            response.headers = headers;
            response.rawHeaders = rawHeaders;
            response.once("end", () => {
              response.complete = true;
              response.socket = null;
              response.connection = null;
            });
            if (isConnectMethod) {
              response.upgrade = true;
              if (this.emit("connect", response, stream2, Buffer.alloc(0))) {
                this.emit("close");
              } else {
                stream2.destroy();
              }
            } else {
              stream2.on("data", (chunk) => {
                if (!response._dumped && !response.push(chunk)) {
                  stream2.pause();
                }
              });
              stream2.once("end", () => {
                if (!this.aborted) {
                  response.push(null);
                }
              });
              if (!this.emit("response", response)) {
                response._dump();
              }
            }
          };
          stream2.once("response", onResponse);
          stream2.once("headers", (headers) => this.emit("information", { statusCode: headers[HTTP2_HEADER_STATUS] }));
          stream2.once("trailers", (trailers, flags, rawTrailers) => {
            const { res } = this;
            if (res === null) {
              onResponse(trailers, flags, rawTrailers);
              return;
            }
            res.trailers = trailers;
            res.rawTrailers = rawTrailers;
          });
          stream2.once("close", () => {
            const { aborted, res } = this;
            if (res) {
              if (aborted) {
                res.aborted = true;
                res.emit("aborted");
                res.destroy();
              }
              const finish = () => {
                res.emit("close");
                this.destroy();
                this.emit("close");
              };
              if (res.readable) {
                res.once("end", finish);
              } else {
                finish();
              }
              return;
            }
            if (!this.destroyed) {
              this.destroy(new Error("The HTTP/2 stream has been early terminated"));
              this.emit("close");
              return;
            }
            this.destroy();
            this.emit("close");
          });
          this.socket = new Proxy(stream2, proxySocketHandler);
          for (const job of this[kJobs]) {
            job();
          }
          this[kJobs].length = 0;
          this.emit("socket", this.socket);
        };
        if (!(HTTP2_HEADER_AUTHORITY in this[kHeaders]) && !isConnectMethod) {
          this[kHeaders][HTTP2_HEADER_AUTHORITY] = this[kOrigin].host;
        }
        if (this[kSession]) {
          try {
            onStream(this[kSession].request(this[kHeaders]));
          } catch (error) {
            this.destroy(error);
          }
        } else {
          this.reusedSocket = true;
          try {
            const promise = this.agent.request(this[kOrigin], this[kOptions], this[kHeaders]);
            this[kPendingAgentPromise] = promise;
            onStream(await promise);
            this[kPendingAgentPromise] = false;
          } catch (error) {
            this[kPendingAgentPromise] = false;
            this.destroy(error);
          }
        }
      }
      get connection() {
        return this.socket;
      }
      set connection(value) {
        this.socket = value;
      }
      getHeaderNames() {
        return Object.keys(this[kHeaders]);
      }
      hasHeader(name) {
        if (typeof name !== "string") {
          throw new ERR_INVALID_ARG_TYPE("name", "string", name);
        }
        return Boolean(this[kHeaders][name.toLowerCase()]);
      }
      getHeader(name) {
        if (typeof name !== "string") {
          throw new ERR_INVALID_ARG_TYPE("name", "string", name);
        }
        return this[kHeaders][name.toLowerCase()];
      }
      get headersSent() {
        return this[kFlushedHeaders];
      }
      removeHeader(name) {
        if (typeof name !== "string") {
          throw new ERR_INVALID_ARG_TYPE("name", "string", name);
        }
        if (this.headersSent) {
          throw new ERR_HTTP_HEADERS_SENT("remove");
        }
        delete this[kHeaders][name.toLowerCase()];
      }
      setHeader(name, value) {
        if (this.headersSent) {
          throw new ERR_HTTP_HEADERS_SENT("set");
        }
        validateHeaderName(name);
        validateHeaderValue(name, value);
        const lowercased = name.toLowerCase();
        if (lowercased === "connection") {
          if (value.toLowerCase() === "keep-alive") {
            return;
          }
          throw new Error(`Invalid 'connection' header: ${value}`);
        }
        if (lowercased === "host" && this.method === "CONNECT") {
          this[kHeaders][HTTP2_HEADER_AUTHORITY] = value;
        } else {
          this[kHeaders][lowercased] = value;
        }
      }
      setNoDelay() {
      }
      setSocketKeepAlive() {
      }
      setTimeout(ms, callback) {
        const applyTimeout = () => this._request.setTimeout(ms, callback);
        if (this._request) {
          applyTimeout();
        } else {
          this[kJobs].push(applyTimeout);
        }
        return this;
      }
      get maxHeadersCount() {
        if (!this.destroyed && this._request) {
          return this._request.session.localSettings.maxHeaderListSize;
        }
        return void 0;
      }
      set maxHeadersCount(_value) {
      }
    };
    module2.exports = ClientRequest;
  }
});

// ../../node_modules/.pnpm/resolve-alpn@1.2.1/node_modules/resolve-alpn/index.js
var require_resolve_alpn = __commonJS({
  "../../node_modules/.pnpm/resolve-alpn@1.2.1/node_modules/resolve-alpn/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var tls = require("tls");
    module2.exports = (options = {}, connect = tls.connect) => new Promise((resolve, reject) => {
      let timeout = false;
      let socket;
      const callback = async () => {
        await socketPromise;
        socket.off("timeout", onTimeout);
        socket.off("error", reject);
        if (options.resolveSocket) {
          resolve({ alpnProtocol: socket.alpnProtocol, socket, timeout });
          if (timeout) {
            await Promise.resolve();
            socket.emit("timeout");
          }
        } else {
          socket.destroy();
          resolve({ alpnProtocol: socket.alpnProtocol, timeout });
        }
      };
      const onTimeout = async () => {
        timeout = true;
        callback();
      };
      const socketPromise = (async () => {
        try {
          socket = await connect(options, callback);
          socket.on("error", reject);
          socket.once("timeout", onTimeout);
        } catch (error) {
          reject(error);
        }
      })();
    });
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/calculate-server-name.js
var require_calculate_server_name = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/calculate-server-name.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { isIP } = require("net");
    var assert2 = require("assert");
    var getHost = (host) => {
      if (host[0] === "[") {
        const idx2 = host.indexOf("]");
        assert2(idx2 !== -1);
        return host.slice(1, idx2);
      }
      const idx = host.indexOf(":");
      if (idx === -1) {
        return host;
      }
      return host.slice(0, idx);
    };
    module2.exports = (host) => {
      const servername = getHost(host);
      if (isIP(servername)) {
        return "";
      }
      return servername;
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/auto.js
var require_auto = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/auto.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { URL: URL4, urlToHttpOptions } = require("url");
    var http3 = require("http");
    var https2 = require("https");
    var resolveALPN = require_resolve_alpn();
    var QuickLRU = require_quick_lru();
    var { Agent, globalAgent } = require_agent();
    var Http2ClientRequest = require_client_request();
    var calculateServerName = require_calculate_server_name();
    var delayAsyncDestroy = require_delay_async_destroy();
    var cache = new QuickLRU({ maxSize: 100 });
    var queue = /* @__PURE__ */ new Map();
    var installSocket = (agent, socket, options) => {
      socket._httpMessage = { shouldKeepAlive: true };
      const onFree = () => {
        agent.emit("free", socket, options);
      };
      socket.on("free", onFree);
      const onClose = () => {
        agent.removeSocket(socket, options);
      };
      socket.on("close", onClose);
      const onTimeout = () => {
        const { freeSockets } = agent;
        for (const sockets of Object.values(freeSockets)) {
          if (sockets.includes(socket)) {
            socket.destroy();
            return;
          }
        }
      };
      socket.on("timeout", onTimeout);
      const onRemove = () => {
        agent.removeSocket(socket, options);
        socket.off("close", onClose);
        socket.off("free", onFree);
        socket.off("timeout", onTimeout);
        socket.off("agentRemove", onRemove);
      };
      socket.on("agentRemove", onRemove);
      agent.emit("free", socket, options);
    };
    var createResolveProtocol = (cache2, queue2 = /* @__PURE__ */ new Map(), connect = void 0) => {
      return async (options) => {
        const name = `${options.host}:${options.port}:${options.ALPNProtocols.sort()}`;
        if (!cache2.has(name)) {
          if (queue2.has(name)) {
            const result = await queue2.get(name);
            return { alpnProtocol: result.alpnProtocol };
          }
          const { path: path6 } = options;
          options.path = options.socketPath;
          const resultPromise = resolveALPN(options, connect);
          queue2.set(name, resultPromise);
          try {
            const result = await resultPromise;
            cache2.set(name, result.alpnProtocol);
            queue2.delete(name);
            options.path = path6;
            return result;
          } catch (error) {
            queue2.delete(name);
            options.path = path6;
            throw error;
          }
        }
        return { alpnProtocol: cache2.get(name) };
      };
    };
    var defaultResolveProtocol = createResolveProtocol(cache, queue);
    module2.exports = async (input, options, callback) => {
      if (typeof input === "string") {
        input = urlToHttpOptions(new URL4(input));
      } else if (input instanceof URL4) {
        input = urlToHttpOptions(input);
      } else {
        input = { ...input };
      }
      if (typeof options === "function" || options === void 0) {
        callback = options;
        options = input;
      } else {
        options = Object.assign(input, options);
      }
      options.ALPNProtocols = options.ALPNProtocols || ["h2", "http/1.1"];
      if (!Array.isArray(options.ALPNProtocols) || options.ALPNProtocols.length === 0) {
        throw new Error("The `ALPNProtocols` option must be an Array with at least one entry");
      }
      options.protocol = options.protocol || "https:";
      const isHttps = options.protocol === "https:";
      options.host = options.hostname || options.host || "localhost";
      options.session = options.tlsSession;
      options.servername = options.servername || calculateServerName(options.headers && options.headers.host || options.host);
      options.port = options.port || (isHttps ? 443 : 80);
      options._defaultAgent = isHttps ? https2.globalAgent : http3.globalAgent;
      const resolveProtocol = options.resolveProtocol || defaultResolveProtocol;
      let { agent } = options;
      if (agent !== void 0 && agent !== false && agent.constructor.name !== "Object") {
        throw new Error("The `options.agent` can be only an object `http`, `https` or `http2` properties");
      }
      if (isHttps) {
        options.resolveSocket = true;
        let { socket, alpnProtocol, timeout } = await resolveProtocol(options);
        if (timeout) {
          if (socket) {
            socket.destroy();
          }
          const error = new Error(`Timed out resolving ALPN: ${options.timeout} ms`);
          error.code = "ETIMEDOUT";
          error.ms = options.timeout;
          throw error;
        }
        if (socket && options.createConnection) {
          socket.destroy();
          socket = void 0;
        }
        delete options.resolveSocket;
        const isHttp2 = alpnProtocol === "h2";
        if (agent) {
          agent = isHttp2 ? agent.http2 : agent.https;
          options.agent = agent;
        }
        if (agent === void 0) {
          agent = isHttp2 ? globalAgent : https2.globalAgent;
        }
        if (socket) {
          if (agent === false) {
            socket.destroy();
          } else {
            const defaultCreateConnection = (isHttp2 ? Agent : https2.Agent).prototype.createConnection;
            if (agent.createConnection === defaultCreateConnection) {
              if (isHttp2) {
                options._reuseSocket = socket;
              } else {
                installSocket(agent, socket, options);
              }
            } else {
              socket.destroy();
            }
          }
        }
        if (isHttp2) {
          return delayAsyncDestroy(new Http2ClientRequest(options, callback));
        }
      } else if (agent) {
        options.agent = agent.http;
      }
      return delayAsyncDestroy(http3.request(options, callback));
    };
    module2.exports.protocolCache = cache;
    module2.exports.resolveProtocol = defaultResolveProtocol;
    module2.exports.createResolveProtocol = createResolveProtocol;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/js-stream-socket.js
var require_js_stream_socket = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/js-stream-socket.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var stream2 = require("stream");
    var tls = require("tls");
    var JSStreamSocket = new tls.TLSSocket(new stream2.PassThrough())._handle._parentWrap.constructor;
    module2.exports = JSStreamSocket;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/unexpected-status-code-error.js
var require_unexpected_status_code_error = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/unexpected-status-code-error.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var UnexpectedStatusCodeError = class extends Error {
      constructor(statusCode, statusMessage = "") {
        super(`The proxy server rejected the request with status code ${statusCode} (${statusMessage || "empty status message"})`);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
      }
    };
    module2.exports = UnexpectedStatusCodeError;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/check-type.js
var require_check_type = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/utils/check-type.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var checkType = (name, value, types2) => {
      const valid = types2.some((type) => {
        const typeofType = typeof type;
        if (typeofType === "string") {
          return typeof value === type;
        }
        return value instanceof type;
      });
      if (!valid) {
        const names = types2.map((type) => typeof type === "string" ? type : type.name);
        throw new TypeError(`Expected '${name}' to be a type of ${names.join(" or ")}, got ${typeof value}`);
      }
    };
    module2.exports = checkType;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/initialize.js
var require_initialize = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/initialize.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { URL: URL4 } = require("url");
    var checkType = require_check_type();
    module2.exports = (self, proxyOptions) => {
      checkType("proxyOptions", proxyOptions, ["object"]);
      checkType("proxyOptions.headers", proxyOptions.headers, ["object", "undefined"]);
      checkType("proxyOptions.raw", proxyOptions.raw, ["boolean", "undefined"]);
      checkType("proxyOptions.url", proxyOptions.url, [URL4, "string"]);
      const url = new URL4(proxyOptions.url);
      self.proxyOptions = {
        raw: true,
        ...proxyOptions,
        headers: { ...proxyOptions.headers },
        url
      };
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/get-auth-headers.js
var require_get_auth_headers = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/get-auth-headers.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = (self) => {
      const { username, password } = self.proxyOptions.url;
      if (username || password) {
        const data = `${username}:${password}`;
        const authorization = `Basic ${Buffer.from(data).toString("base64")}`;
        return {
          "proxy-authorization": authorization,
          authorization
        };
      }
      return {};
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h1-over-h2.js
var require_h1_over_h2 = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h1-over-h2.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var tls = require("tls");
    var http3 = require("http");
    var https2 = require("https");
    var JSStreamSocket = require_js_stream_socket();
    var { globalAgent } = require_agent();
    var UnexpectedStatusCodeError = require_unexpected_status_code_error();
    var initialize = require_initialize();
    var getAuthorizationHeaders = require_get_auth_headers();
    var createConnection = (self, options, callback) => {
      (async () => {
        try {
          const { proxyOptions } = self;
          const { url, headers, raw } = proxyOptions;
          const stream2 = await globalAgent.request(url, proxyOptions, {
            ...getAuthorizationHeaders(self),
            ...headers,
            ":method": "CONNECT",
            ":authority": `${options.host}:${options.port}`
          });
          stream2.once("error", callback);
          stream2.once("response", (headers2) => {
            const statusCode = headers2[":status"];
            if (statusCode !== 200) {
              callback(new UnexpectedStatusCodeError(statusCode, ""));
              return;
            }
            const encrypted = self instanceof https2.Agent;
            if (raw && encrypted) {
              options.socket = stream2;
              const secureStream = tls.connect(options);
              secureStream.once("close", () => {
                stream2.destroy();
              });
              callback(null, secureStream);
              return;
            }
            const socket = new JSStreamSocket(stream2);
            socket.encrypted = false;
            socket._handle.getpeername = (out) => {
              out.family = void 0;
              out.address = void 0;
              out.port = void 0;
            };
            callback(null, socket);
          });
        } catch (error) {
          callback(error);
        }
      })();
    };
    var HttpOverHttp2 = class extends http3.Agent {
      constructor(options) {
        super(options);
        initialize(this, options.proxyOptions);
      }
      createConnection(options, callback) {
        createConnection(this, options, callback);
      }
    };
    var HttpsOverHttp2 = class extends https2.Agent {
      constructor(options) {
        super(options);
        initialize(this, options.proxyOptions);
      }
      createConnection(options, callback) {
        createConnection(this, options, callback);
      }
    };
    module2.exports = {
      HttpOverHttp2,
      HttpsOverHttp2
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-hx.js
var require_h2_over_hx = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-hx.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { Agent } = require_agent();
    var JSStreamSocket = require_js_stream_socket();
    var UnexpectedStatusCodeError = require_unexpected_status_code_error();
    var initialize = require_initialize();
    var Http2OverHttpX = class extends Agent {
      constructor(options) {
        super(options);
        initialize(this, options.proxyOptions);
      }
      async createConnection(origin, options) {
        const authority = `${origin.hostname}:${origin.port || 443}`;
        const [stream2, statusCode, statusMessage] = await this._getProxyStream(authority);
        if (statusCode !== 200) {
          throw new UnexpectedStatusCodeError(statusCode, statusMessage);
        }
        if (this.proxyOptions.raw) {
          options.socket = stream2;
        } else {
          const socket = new JSStreamSocket(stream2);
          socket.encrypted = false;
          socket._handle.getpeername = (out) => {
            out.family = void 0;
            out.address = void 0;
            out.port = void 0;
          };
          return socket;
        }
        return super.createConnection(origin, options);
      }
    };
    module2.exports = Http2OverHttpX;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-h2.js
var require_h2_over_h2 = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-h2.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { globalAgent } = require_agent();
    var Http2OverHttpX = require_h2_over_hx();
    var getAuthorizationHeaders = require_get_auth_headers();
    var getStatusCode = (stream2) => new Promise((resolve, reject) => {
      stream2.once("error", reject);
      stream2.once("response", (headers) => {
        stream2.off("error", reject);
        resolve(headers[":status"]);
      });
    });
    var Http2OverHttp2 = class extends Http2OverHttpX {
      async _getProxyStream(authority) {
        const { proxyOptions } = this;
        const headers = {
          ...getAuthorizationHeaders(this),
          ...proxyOptions.headers,
          ":method": "CONNECT",
          ":authority": authority
        };
        const stream2 = await globalAgent.request(proxyOptions.url, proxyOptions, headers);
        const statusCode = await getStatusCode(stream2);
        return [stream2, statusCode, ""];
      }
    };
    module2.exports = Http2OverHttp2;
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-h1.js
var require_h2_over_h1 = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/proxies/h2-over-h1.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var http3 = require("http");
    var https2 = require("https");
    var Http2OverHttpX = require_h2_over_hx();
    var getAuthorizationHeaders = require_get_auth_headers();
    var getStream2 = (request) => new Promise((resolve, reject) => {
      const onConnect = (response, socket, head) => {
        socket.unshift(head);
        request.off("error", reject);
        resolve([socket, response.statusCode, response.statusMessage]);
      };
      request.once("error", reject);
      request.once("connect", onConnect);
    });
    var Http2OverHttp = class extends Http2OverHttpX {
      async _getProxyStream(authority) {
        const { proxyOptions } = this;
        const { url, headers } = this.proxyOptions;
        const network = url.protocol === "https:" ? https2 : http3;
        const request = network.request({
          ...proxyOptions,
          hostname: url.hostname,
          port: url.port,
          path: authority,
          headers: {
            ...getAuthorizationHeaders(this),
            ...headers,
            host: authority
          },
          method: "CONNECT"
        }).end();
        return getStream2(request);
      }
    };
    module2.exports = {
      Http2OverHttp,
      Http2OverHttps: Http2OverHttp
    };
  }
});

// ../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/index.js
var require_source2 = __commonJS({
  "../../node_modules/.pnpm/http2-wrapper@2.2.0/node_modules/http2-wrapper/source/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var http22 = require("http2");
    var {
      Agent,
      globalAgent
    } = require_agent();
    var ClientRequest = require_client_request();
    var IncomingMessage = require_incoming_message();
    var auto = require_auto();
    var {
      HttpOverHttp2,
      HttpsOverHttp2
    } = require_h1_over_h2();
    var Http2OverHttp2 = require_h2_over_h2();
    var {
      Http2OverHttp,
      Http2OverHttps
    } = require_h2_over_h1();
    var validateHeaderName = require_validate_header_name();
    var validateHeaderValue = require_validate_header_value();
    var request = (url, options, callback) => new ClientRequest(url, options, callback);
    var get = (url, options, callback) => {
      const req = new ClientRequest(url, options, callback);
      req.end();
      return req;
    };
    module2.exports = {
      ...http22,
      ClientRequest,
      IncomingMessage,
      Agent,
      globalAgent,
      request,
      get,
      auto,
      proxies: {
        HttpOverHttp2,
        HttpsOverHttp2,
        Http2OverHttp2,
        Http2OverHttp,
        Http2OverHttps
      },
      validateHeaderName,
      validateHeaderValue
    };
  }
});

// ../../node_modules/.pnpm/ini@1.3.8/node_modules/ini/ini.js
var require_ini = __commonJS({
  "../../node_modules/.pnpm/ini@1.3.8/node_modules/ini/ini.js"(exports) {
    init_cjs_shims();
    exports.parse = exports.decode = decode;
    exports.stringify = exports.encode = encode;
    exports.safe = safe;
    exports.unsafe = unsafe;
    var eol = typeof process !== "undefined" && process.platform === "win32" ? "\r\n" : "\n";
    function encode(obj, opt) {
      var children = [];
      var out = "";
      if (typeof opt === "string") {
        opt = {
          section: opt,
          whitespace: false
        };
      } else {
        opt = opt || {};
        opt.whitespace = opt.whitespace === true;
      }
      var separator = opt.whitespace ? " = " : "=";
      Object.keys(obj).forEach(function(k, _, __) {
        var val = obj[k];
        if (val && Array.isArray(val)) {
          val.forEach(function(item) {
            out += safe(k + "[]") + separator + safe(item) + "\n";
          });
        } else if (val && typeof val === "object")
          children.push(k);
        else
          out += safe(k) + separator + safe(val) + eol;
      });
      if (opt.section && out.length)
        out = "[" + safe(opt.section) + "]" + eol + out;
      children.forEach(function(k, _, __) {
        var nk = dotSplit(k).join("\\.");
        var section = (opt.section ? opt.section + "." : "") + nk;
        var child = encode(obj[k], {
          section,
          whitespace: opt.whitespace
        });
        if (out.length && child.length)
          out += eol;
        out += child;
      });
      return out;
    }
    function dotSplit(str) {
      return str.replace(/\1/g, "LITERAL\\1LITERAL").replace(/\\\./g, "").split(/\./).map(function(part) {
        return part.replace(/\1/g, "\\.").replace(/\2LITERAL\\1LITERAL\2/g, "");
      });
    }
    function decode(str) {
      var out = {};
      var p = out;
      var section = null;
      var re = /^\[([^\]]*)\]$|^([^=]+)(=(.*))?$/i;
      var lines = str.split(/[\r\n]+/g);
      lines.forEach(function(line, _, __) {
        if (!line || line.match(/^\s*[;#]/))
          return;
        var match = line.match(re);
        if (!match)
          return;
        if (match[1] !== void 0) {
          section = unsafe(match[1]);
          if (section === "__proto__") {
            p = {};
            return;
          }
          p = out[section] = out[section] || {};
          return;
        }
        var key = unsafe(match[2]);
        if (key === "__proto__")
          return;
        var value = match[3] ? unsafe(match[4]) : true;
        switch (value) {
          case "true":
          case "false":
          case "null":
            value = JSON.parse(value);
        }
        if (key.length > 2 && key.slice(-2) === "[]") {
          key = key.substring(0, key.length - 2);
          if (key === "__proto__")
            return;
          if (!p[key])
            p[key] = [];
          else if (!Array.isArray(p[key]))
            p[key] = [p[key]];
        }
        if (Array.isArray(p[key]))
          p[key].push(value);
        else
          p[key] = value;
      });
      Object.keys(out).filter(function(k, _, __) {
        if (!out[k] || typeof out[k] !== "object" || Array.isArray(out[k]))
          return false;
        var parts = dotSplit(k);
        var p2 = out;
        var l = parts.pop();
        var nl = l.replace(/\\\./g, ".");
        parts.forEach(function(part, _2, __2) {
          if (part === "__proto__")
            return;
          if (!p2[part] || typeof p2[part] !== "object")
            p2[part] = {};
          p2 = p2[part];
        });
        if (p2 === out && nl === l)
          return false;
        p2[nl] = out[k];
        return true;
      }).forEach(function(del, _, __) {
        delete out[del];
      });
      return out;
    }
    function isQuoted(val) {
      return val.charAt(0) === '"' && val.slice(-1) === '"' || val.charAt(0) === "'" && val.slice(-1) === "'";
    }
    function safe(val) {
      return typeof val !== "string" || val.match(/[=\r\n]/) || val.match(/^\[/) || val.length > 1 && isQuoted(val) || val !== val.trim() ? JSON.stringify(val) : val.replace(/;/g, "\\;").replace(/#/g, "\\#");
    }
    function unsafe(val, doUnesc) {
      val = (val || "").trim();
      if (isQuoted(val)) {
        if (val.charAt(0) === "'")
          val = val.substr(1, val.length - 2);
        try {
          val = JSON.parse(val);
        } catch (_) {
        }
      } else {
        var esc = false;
        var unesc = "";
        for (var i = 0, l = val.length; i < l; i++) {
          var c = val.charAt(i);
          if (esc) {
            if ("\\;#".indexOf(c) !== -1)
              unesc += c;
            else
              unesc += "\\" + c;
            esc = false;
          } else if (";#".indexOf(c) !== -1)
            break;
          else if (c === "\\")
            esc = true;
          else
            unesc += c;
        }
        if (esc)
          unesc += "\\";
        return unesc.trim();
      }
      return val;
    }
  }
});

// ../../node_modules/.pnpm/strip-json-comments@2.0.1/node_modules/strip-json-comments/index.js
var require_strip_json_comments = __commonJS({
  "../../node_modules/.pnpm/strip-json-comments@2.0.1/node_modules/strip-json-comments/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var singleComment = 1;
    var multiComment = 2;
    function stripWithoutWhitespace() {
      return "";
    }
    function stripWithWhitespace(str, start, end) {
      return str.slice(start, end).replace(/\S/g, " ");
    }
    module2.exports = function(str, opts) {
      opts = opts || {};
      var currentChar;
      var nextChar;
      var insideString = false;
      var insideComment = false;
      var offset = 0;
      var ret = "";
      var strip = opts.whitespace === false ? stripWithoutWhitespace : stripWithWhitespace;
      for (var i = 0; i < str.length; i++) {
        currentChar = str[i];
        nextChar = str[i + 1];
        if (!insideComment && currentChar === '"') {
          var escaped = str[i - 1] === "\\" && str[i - 2] !== "\\";
          if (!escaped) {
            insideString = !insideString;
          }
        }
        if (insideString) {
          continue;
        }
        if (!insideComment && currentChar + nextChar === "//") {
          ret += str.slice(offset, i);
          offset = i;
          insideComment = singleComment;
          i++;
        } else if (insideComment === singleComment && currentChar + nextChar === "\r\n") {
          i++;
          insideComment = false;
          ret += strip(str, offset, i);
          offset = i;
          continue;
        } else if (insideComment === singleComment && currentChar === "\n") {
          insideComment = false;
          ret += strip(str, offset, i);
          offset = i;
        } else if (!insideComment && currentChar + nextChar === "/*") {
          ret += str.slice(offset, i);
          offset = i;
          insideComment = multiComment;
          i++;
          continue;
        } else if (insideComment === multiComment && currentChar + nextChar === "*/") {
          i++;
          insideComment = false;
          ret += strip(str, offset, i + 1);
          offset = i + 1;
          continue;
        }
      }
      return ret + (insideComment ? strip(str.substr(offset)) : str.substr(offset));
    };
  }
});

// ../../node_modules/.pnpm/rc@1.2.8/node_modules/rc/lib/utils.js
var require_utils = __commonJS({
  "../../node_modules/.pnpm/rc@1.2.8/node_modules/rc/lib/utils.js"(exports) {
    "use strict";
    init_cjs_shims();
    var fs3 = require("fs");
    var ini = require_ini();
    var path6 = require("path");
    var stripJsonComments = require_strip_json_comments();
    var parse = exports.parse = function(content) {
      if (/^\s*{/.test(content))
        return JSON.parse(stripJsonComments(content));
      return ini.parse(content);
    };
    var file = exports.file = function() {
      var args = [].slice.call(arguments).filter(function(arg) {
        return arg != null;
      });
      for (var i in args)
        if ("string" !== typeof args[i])
          return;
      var file2 = path6.join.apply(null, args);
      var content;
      try {
        return fs3.readFileSync(file2, "utf-8");
      } catch (err) {
        return;
      }
    };
    var json = exports.json = function() {
      var content = file.apply(null, arguments);
      return content ? parse(content) : null;
    };
    var env3 = exports.env = function(prefix, env4) {
      env4 = env4 || process.env;
      var obj = {};
      var l = prefix.length;
      for (var k in env4) {
        if (k.toLowerCase().indexOf(prefix.toLowerCase()) === 0) {
          var keypath = k.substring(l).split("__");
          var _emptyStringIndex;
          while ((_emptyStringIndex = keypath.indexOf("")) > -1) {
            keypath.splice(_emptyStringIndex, 1);
          }
          var cursor = obj;
          keypath.forEach(function _buildSubObj(_subkey, i) {
            if (!_subkey || typeof cursor !== "object")
              return;
            if (i === keypath.length - 1)
              cursor[_subkey] = env4[k];
            if (cursor[_subkey] === void 0)
              cursor[_subkey] = {};
            cursor = cursor[_subkey];
          });
        }
      }
      return obj;
    };
    var find = exports.find = function() {
      var rel = path6.join.apply(null, [].slice.call(arguments));
      function find2(start, rel2) {
        var file2 = path6.join(start, rel2);
        try {
          fs3.statSync(file2);
          return file2;
        } catch (err) {
          if (path6.dirname(start) !== start)
            return find2(path6.dirname(start), rel2);
        }
      }
      return find2(process.cwd(), rel);
    };
  }
});

// ../../node_modules/.pnpm/deep-extend@0.6.0/node_modules/deep-extend/lib/deep-extend.js
var require_deep_extend = __commonJS({
  "../../node_modules/.pnpm/deep-extend@0.6.0/node_modules/deep-extend/lib/deep-extend.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    function isSpecificValue(val) {
      return val instanceof Buffer || val instanceof Date || val instanceof RegExp ? true : false;
    }
    function cloneSpecificValue(val) {
      if (val instanceof Buffer) {
        var x = Buffer.alloc ? Buffer.alloc(val.length) : new Buffer(val.length);
        val.copy(x);
        return x;
      } else if (val instanceof Date) {
        return new Date(val.getTime());
      } else if (val instanceof RegExp) {
        return new RegExp(val);
      } else {
        throw new Error("Unexpected situation");
      }
    }
    function deepCloneArray(arr) {
      var clone = [];
      arr.forEach(function(item, index) {
        if (typeof item === "object" && item !== null) {
          if (Array.isArray(item)) {
            clone[index] = deepCloneArray(item);
          } else if (isSpecificValue(item)) {
            clone[index] = cloneSpecificValue(item);
          } else {
            clone[index] = deepExtend({}, item);
          }
        } else {
          clone[index] = item;
        }
      });
      return clone;
    }
    function safeGetProperty(object, property) {
      return property === "__proto__" ? void 0 : object[property];
    }
    var deepExtend = module2.exports = function() {
      if (arguments.length < 1 || typeof arguments[0] !== "object") {
        return false;
      }
      if (arguments.length < 2) {
        return arguments[0];
      }
      var target = arguments[0];
      var args = Array.prototype.slice.call(arguments, 1);
      var val, src, clone;
      args.forEach(function(obj) {
        if (typeof obj !== "object" || obj === null || Array.isArray(obj)) {
          return;
        }
        Object.keys(obj).forEach(function(key) {
          src = safeGetProperty(target, key);
          val = safeGetProperty(obj, key);
          if (val === target) {
            return;
          } else if (typeof val !== "object" || val === null) {
            target[key] = val;
            return;
          } else if (Array.isArray(val)) {
            target[key] = deepCloneArray(val);
            return;
          } else if (isSpecificValue(val)) {
            target[key] = cloneSpecificValue(val);
            return;
          } else if (typeof src !== "object" || src === null || Array.isArray(src)) {
            target[key] = deepExtend({}, val);
            return;
          } else {
            target[key] = deepExtend(src, val);
            return;
          }
        });
      });
      return target;
    };
  }
});

// ../../node_modules/.pnpm/minimist@1.2.6/node_modules/minimist/index.js
var require_minimist = __commonJS({
  "../../node_modules/.pnpm/minimist@1.2.6/node_modules/minimist/index.js"(exports, module2) {
    init_cjs_shims();
    module2.exports = function(args, opts) {
      if (!opts)
        opts = {};
      var flags = { bools: {}, strings: {}, unknownFn: null };
      if (typeof opts["unknown"] === "function") {
        flags.unknownFn = opts["unknown"];
      }
      if (typeof opts["boolean"] === "boolean" && opts["boolean"]) {
        flags.allBools = true;
      } else {
        [].concat(opts["boolean"]).filter(Boolean).forEach(function(key2) {
          flags.bools[key2] = true;
        });
      }
      var aliases2 = {};
      Object.keys(opts.alias || {}).forEach(function(key2) {
        aliases2[key2] = [].concat(opts.alias[key2]);
        aliases2[key2].forEach(function(x) {
          aliases2[x] = [key2].concat(aliases2[key2].filter(function(y) {
            return x !== y;
          }));
        });
      });
      [].concat(opts.string).filter(Boolean).forEach(function(key2) {
        flags.strings[key2] = true;
        if (aliases2[key2]) {
          flags.strings[aliases2[key2]] = true;
        }
      });
      var defaults2 = opts["default"] || {};
      var argv = { _: [] };
      Object.keys(flags.bools).forEach(function(key2) {
        setArg(key2, defaults2[key2] === void 0 ? false : defaults2[key2]);
      });
      var notFlags = [];
      if (args.indexOf("--") !== -1) {
        notFlags = args.slice(args.indexOf("--") + 1);
        args = args.slice(0, args.indexOf("--"));
      }
      function argDefined(key2, arg2) {
        return flags.allBools && /^--[^=]+$/.test(arg2) || flags.strings[key2] || flags.bools[key2] || aliases2[key2];
      }
      function setArg(key2, val, arg2) {
        if (arg2 && flags.unknownFn && !argDefined(key2, arg2)) {
          if (flags.unknownFn(arg2) === false)
            return;
        }
        var value2 = !flags.strings[key2] && isNumber(val) ? Number(val) : val;
        setKey(argv, key2.split("."), value2);
        (aliases2[key2] || []).forEach(function(x) {
          setKey(argv, x.split("."), value2);
        });
      }
      function setKey(obj, keys, value2) {
        var o = obj;
        for (var i2 = 0; i2 < keys.length - 1; i2++) {
          var key2 = keys[i2];
          if (isConstructorOrProto(o, key2))
            return;
          if (o[key2] === void 0)
            o[key2] = {};
          if (o[key2] === Object.prototype || o[key2] === Number.prototype || o[key2] === String.prototype)
            o[key2] = {};
          if (o[key2] === Array.prototype)
            o[key2] = [];
          o = o[key2];
        }
        var key2 = keys[keys.length - 1];
        if (isConstructorOrProto(o, key2))
          return;
        if (o === Object.prototype || o === Number.prototype || o === String.prototype)
          o = {};
        if (o === Array.prototype)
          o = [];
        if (o[key2] === void 0 || flags.bools[key2] || typeof o[key2] === "boolean") {
          o[key2] = value2;
        } else if (Array.isArray(o[key2])) {
          o[key2].push(value2);
        } else {
          o[key2] = [o[key2], value2];
        }
      }
      function aliasIsBoolean(key2) {
        return aliases2[key2].some(function(x) {
          return flags.bools[x];
        });
      }
      for (var i = 0; i < args.length; i++) {
        var arg = args[i];
        if (/^--.+=/.test(arg)) {
          var m = arg.match(/^--([^=]+)=([\s\S]*)$/);
          var key = m[1];
          var value = m[2];
          if (flags.bools[key]) {
            value = value !== "false";
          }
          setArg(key, value, arg);
        } else if (/^--no-.+/.test(arg)) {
          var key = arg.match(/^--no-(.+)/)[1];
          setArg(key, false, arg);
        } else if (/^--.+/.test(arg)) {
          var key = arg.match(/^--(.+)/)[1];
          var next = args[i + 1];
          if (next !== void 0 && !/^-/.test(next) && !flags.bools[key] && !flags.allBools && (aliases2[key] ? !aliasIsBoolean(key) : true)) {
            setArg(key, next, arg);
            i++;
          } else if (/^(true|false)$/.test(next)) {
            setArg(key, next === "true", arg);
            i++;
          } else {
            setArg(key, flags.strings[key] ? "" : true, arg);
          }
        } else if (/^-[^-]+/.test(arg)) {
          var letters = arg.slice(1, -1).split("");
          var broken = false;
          for (var j = 0; j < letters.length; j++) {
            var next = arg.slice(j + 2);
            if (next === "-") {
              setArg(letters[j], next, arg);
              continue;
            }
            if (/[A-Za-z]/.test(letters[j]) && /=/.test(next)) {
              setArg(letters[j], next.split("=")[1], arg);
              broken = true;
              break;
            }
            if (/[A-Za-z]/.test(letters[j]) && /-?\d+(\.\d*)?(e-?\d+)?$/.test(next)) {
              setArg(letters[j], next, arg);
              broken = true;
              break;
            }
            if (letters[j + 1] && letters[j + 1].match(/\W/)) {
              setArg(letters[j], arg.slice(j + 2), arg);
              broken = true;
              break;
            } else {
              setArg(letters[j], flags.strings[letters[j]] ? "" : true, arg);
            }
          }
          var key = arg.slice(-1)[0];
          if (!broken && key !== "-") {
            if (args[i + 1] && !/^(-|--)[^-]/.test(args[i + 1]) && !flags.bools[key] && (aliases2[key] ? !aliasIsBoolean(key) : true)) {
              setArg(key, args[i + 1], arg);
              i++;
            } else if (args[i + 1] && /^(true|false)$/.test(args[i + 1])) {
              setArg(key, args[i + 1] === "true", arg);
              i++;
            } else {
              setArg(key, flags.strings[key] ? "" : true, arg);
            }
          }
        } else {
          if (!flags.unknownFn || flags.unknownFn(arg) !== false) {
            argv._.push(
              flags.strings["_"] || !isNumber(arg) ? arg : Number(arg)
            );
          }
          if (opts.stopEarly) {
            argv._.push.apply(argv._, args.slice(i + 1));
            break;
          }
        }
      }
      Object.keys(defaults2).forEach(function(key2) {
        if (!hasKey(argv, key2.split("."))) {
          setKey(argv, key2.split("."), defaults2[key2]);
          (aliases2[key2] || []).forEach(function(x) {
            setKey(argv, x.split("."), defaults2[key2]);
          });
        }
      });
      if (opts["--"]) {
        argv["--"] = new Array();
        notFlags.forEach(function(key2) {
          argv["--"].push(key2);
        });
      } else {
        notFlags.forEach(function(key2) {
          argv._.push(key2);
        });
      }
      return argv;
    };
    function hasKey(obj, keys) {
      var o = obj;
      keys.slice(0, -1).forEach(function(key2) {
        o = o[key2] || {};
      });
      var key = keys[keys.length - 1];
      return key in o;
    }
    function isNumber(x) {
      if (typeof x === "number")
        return true;
      if (/^0x[0-9a-f]+$/i.test(x))
        return true;
      return /^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(e[-+]?\d+)?$/.test(x);
    }
    function isConstructorOrProto(obj, key) {
      return key === "constructor" && typeof obj[key] === "function" || key === "__proto__";
    }
  }
});

// ../../node_modules/.pnpm/rc@1.2.8/node_modules/rc/index.js
var require_rc = __commonJS({
  "../../node_modules/.pnpm/rc@1.2.8/node_modules/rc/index.js"(exports, module2) {
    init_cjs_shims();
    var cc = require_utils();
    var join = require("path").join;
    var deepExtend = require_deep_extend();
    var etc = "/etc";
    var win = process.platform === "win32";
    var home = win ? process.env.USERPROFILE : process.env.HOME;
    module2.exports = function(name, defaults2, argv, parse) {
      if ("string" !== typeof name)
        throw new Error("rc(name): name *must* be string");
      if (!argv)
        argv = require_minimist()(process.argv.slice(2));
      defaults2 = ("string" === typeof defaults2 ? cc.json(defaults2) : defaults2) || {};
      parse = parse || cc.parse;
      var env3 = cc.env(name + "_");
      var configs = [defaults2];
      var configFiles = [];
      function addConfigFile(file) {
        if (configFiles.indexOf(file) >= 0)
          return;
        var fileConfig = cc.file(file);
        if (fileConfig) {
          configs.push(parse(fileConfig));
          configFiles.push(file);
        }
      }
      if (!win)
        [
          join(etc, name, "config"),
          join(etc, name + "rc")
        ].forEach(addConfigFile);
      if (home)
        [
          join(home, ".config", name, "config"),
          join(home, ".config", name),
          join(home, "." + name, "config"),
          join(home, "." + name + "rc")
        ].forEach(addConfigFile);
      addConfigFile(cc.find("." + name + "rc"));
      if (env3.config)
        addConfigFile(env3.config);
      if (argv.config)
        addConfigFile(argv.config);
      return deepExtend.apply(null, configs.concat([
        env3,
        argv,
        configFiles.length ? { configs: configFiles, config: configFiles[configFiles.length - 1] } : void 0
      ]));
    };
  }
});

// ../../node_modules/.pnpm/@pnpm+network.ca-file@1.0.2/node_modules/@pnpm/network.ca-file/dist/ca-file.js
var require_ca_file = __commonJS({
  "../../node_modules/.pnpm/@pnpm+network.ca-file@1.0.2/node_modules/@pnpm/network.ca-file/dist/ca-file.js"(exports) {
    "use strict";
    init_cjs_shims();
    var __importDefault = exports && exports.__importDefault || function(mod) {
      return mod && mod.__esModule ? mod : { "default": mod };
    };
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.readCAFileSync = void 0;
    var graceful_fs_1 = __importDefault(require_graceful_fs());
    function readCAFileSync(filePath) {
      try {
        const contents = graceful_fs_1.default.readFileSync(filePath, "utf8");
        const delim = "-----END CERTIFICATE-----";
        const output = contents.split(delim).filter((ca) => Boolean(ca.trim())).map((ca) => `${ca.trimLeft()}${delim}`);
        return output;
      } catch (err) {
        if (err.code === "ENOENT")
          return void 0;
        throw err;
      }
    }
    exports.readCAFileSync = readCAFileSync;
  }
});

// ../../node_modules/.pnpm/@pnpm+network.ca-file@1.0.2/node_modules/@pnpm/network.ca-file/dist/index.js
var require_dist = __commonJS({
  "../../node_modules/.pnpm/@pnpm+network.ca-file@1.0.2/node_modules/@pnpm/network.ca-file/dist/index.js"(exports) {
    "use strict";
    init_cjs_shims();
    var __createBinding = exports && exports.__createBinding || (Object.create ? function(o, m, k, k2) {
      if (k2 === void 0)
        k2 = k;
      var desc = Object.getOwnPropertyDescriptor(m, k);
      if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
        desc = { enumerable: true, get: function() {
          return m[k];
        } };
      }
      Object.defineProperty(o, k2, desc);
    } : function(o, m, k, k2) {
      if (k2 === void 0)
        k2 = k;
      o[k2] = m[k];
    });
    var __exportStar = exports && exports.__exportStar || function(m, exports2) {
      for (var p in m)
        if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports2, p))
          __createBinding(exports2, m, p);
    };
    Object.defineProperty(exports, "__esModule", { value: true });
    __exportStar(require_ca_file(), exports);
  }
});

// ../../node_modules/.pnpm/proto-list@1.2.4/node_modules/proto-list/proto-list.js
var require_proto_list = __commonJS({
  "../../node_modules/.pnpm/proto-list@1.2.4/node_modules/proto-list/proto-list.js"(exports, module2) {
    init_cjs_shims();
    module2.exports = ProtoList;
    function setProto(obj, proto2) {
      if (typeof Object.setPrototypeOf === "function")
        return Object.setPrototypeOf(obj, proto2);
      else
        obj.__proto__ = proto2;
    }
    function ProtoList() {
      this.list = [];
      var root = null;
      Object.defineProperty(this, "root", {
        get: function() {
          return root;
        },
        set: function(r) {
          root = r;
          if (this.list.length) {
            setProto(this.list[this.list.length - 1], r);
          }
        },
        enumerable: true,
        configurable: true
      });
    }
    ProtoList.prototype = {
      get length() {
        return this.list.length;
      },
      get keys() {
        var k = [];
        for (var i in this.list[0])
          k.push(i);
        return k;
      },
      get snapshot() {
        var o = {};
        this.keys.forEach(function(k) {
          o[k] = this.get(k);
        }, this);
        return o;
      },
      get store() {
        return this.list[0];
      },
      push: function(obj) {
        if (typeof obj !== "object")
          obj = { valueOf: obj };
        if (this.list.length >= 1) {
          setProto(this.list[this.list.length - 1], obj);
        }
        setProto(obj, this.root);
        return this.list.push(obj);
      },
      pop: function() {
        if (this.list.length >= 2) {
          setProto(this.list[this.list.length - 2], this.root);
        }
        return this.list.pop();
      },
      unshift: function(obj) {
        setProto(obj, this.list[0] || this.root);
        return this.list.unshift(obj);
      },
      shift: function() {
        if (this.list.length === 1) {
          setProto(this.list[0], this.root);
        }
        return this.list.shift();
      },
      get: function(key) {
        return this.list[0][key];
      },
      set: function(key, val, save) {
        if (!this.length)
          this.push({});
        if (save && this.list[0].hasOwnProperty(key))
          this.push({});
        return this.list[0][key] = val;
      },
      forEach: function(fn, thisp) {
        for (var key in this.list[0])
          fn.call(thisp, key, this.list[0][key]);
      },
      slice: function() {
        return this.list.slice.apply(this.list, arguments);
      },
      splice: function() {
        var ret = this.list.splice.apply(this.list, arguments);
        for (var i = 0, l = this.list.length; i < l; i++) {
          setProto(this.list[i], this.list[i + 1] || this.root);
        }
        return ret;
      }
    };
  }
});

// ../../node_modules/.pnpm/config-chain@1.1.13/node_modules/config-chain/index.js
var require_config_chain = __commonJS({
  "../../node_modules/.pnpm/config-chain@1.1.13/node_modules/config-chain/index.js"(exports, module2) {
    init_cjs_shims();
    var ProtoList = require_proto_list();
    var path6 = require("path");
    var fs3 = require("fs");
    var ini = require_ini();
    var EE = require("events").EventEmitter;
    var url = require("url");
    var http3 = require("http");
    var exports = module2.exports = function() {
      var args = [].slice.call(arguments), conf = new ConfigChain();
      while (args.length) {
        var a = args.shift();
        if (a)
          conf.push("string" === typeof a ? json(a) : a);
      }
      return conf;
    };
    var find = exports.find = function() {
      var rel = path6.join.apply(null, [].slice.call(arguments));
      function find2(start, rel2) {
        var file = path6.join(start, rel2);
        try {
          fs3.statSync(file);
          return file;
        } catch (err) {
          if (path6.dirname(start) !== start)
            return find2(path6.dirname(start), rel2);
        }
      }
      return find2(__dirname, rel);
    };
    var parse = exports.parse = function(content, file, type) {
      content = "" + content;
      if (!type) {
        try {
          return JSON.parse(content);
        } catch (er) {
          return ini.parse(content);
        }
      } else if (type === "json") {
        if (this.emit) {
          try {
            return JSON.parse(content);
          } catch (er) {
            this.emit("error", er);
          }
        } else {
          return JSON.parse(content);
        }
      } else {
        return ini.parse(content);
      }
    };
    var json = exports.json = function() {
      var args = [].slice.call(arguments).filter(function(arg) {
        return arg != null;
      });
      var file = path6.join.apply(null, args);
      var content;
      try {
        content = fs3.readFileSync(file, "utf-8");
      } catch (err) {
        return;
      }
      return parse(content, file, "json");
    };
    var env3 = exports.env = function(prefix, env4) {
      env4 = env4 || process.env;
      var obj = {};
      var l = prefix.length;
      for (var k in env4) {
        if (k.indexOf(prefix) === 0)
          obj[k.substring(l)] = env4[k];
      }
      return obj;
    };
    exports.ConfigChain = ConfigChain;
    function ConfigChain() {
      EE.apply(this);
      ProtoList.apply(this, arguments);
      this._awaiting = 0;
      this._saving = 0;
      this.sources = {};
    }
    var extras = {
      constructor: { value: ConfigChain }
    };
    Object.keys(EE.prototype).forEach(function(k) {
      extras[k] = Object.getOwnPropertyDescriptor(EE.prototype, k);
    });
    ConfigChain.prototype = Object.create(ProtoList.prototype, extras);
    ConfigChain.prototype.del = function(key, where) {
      if (where) {
        var target = this.sources[where];
        target = target && target.data;
        if (!target) {
          return this.emit("error", new Error("not found " + where));
        }
        delete target[key];
      } else {
        for (var i = 0, l = this.list.length; i < l; i++) {
          delete this.list[i][key];
        }
      }
      return this;
    };
    ConfigChain.prototype.set = function(key, value, where) {
      var target;
      if (where) {
        target = this.sources[where];
        target = target && target.data;
        if (!target) {
          return this.emit("error", new Error("not found " + where));
        }
      } else {
        target = this.list[0];
        if (!target) {
          return this.emit("error", new Error("cannot set, no confs!"));
        }
      }
      target[key] = value;
      return this;
    };
    ConfigChain.prototype.get = function(key, where) {
      if (where) {
        where = this.sources[where];
        if (where)
          where = where.data;
        if (where && Object.hasOwnProperty.call(where, key))
          return where[key];
        return void 0;
      }
      return this.list[0][key];
    };
    ConfigChain.prototype.save = function(where, type, cb) {
      if (typeof type === "function")
        cb = type, type = null;
      var target = this.sources[where];
      if (!target || !(target.path || target.source) || !target.data) {
        return this.emit("error", new Error("bad save target: " + where));
      }
      if (target.source) {
        var pref = target.prefix || "";
        Object.keys(target.data).forEach(function(k) {
          target.source[pref + k] = target.data[k];
        });
        return this;
      }
      var type = type || target.type;
      var data = target.data;
      if (target.type === "json") {
        data = JSON.stringify(data);
      } else {
        data = ini.stringify(data);
      }
      this._saving++;
      fs3.writeFile(target.path, data, "utf8", function(er) {
        this._saving--;
        if (er) {
          if (cb)
            return cb(er);
          else
            return this.emit("error", er);
        }
        if (this._saving === 0) {
          if (cb)
            cb();
          this.emit("save");
        }
      }.bind(this));
      return this;
    };
    ConfigChain.prototype.addFile = function(file, type, name) {
      name = name || file;
      var marker = { __source__: name };
      this.sources[name] = { path: file, type };
      this.push(marker);
      this._await();
      fs3.readFile(file, "utf8", function(er, data) {
        if (er)
          this.emit("error", er);
        this.addString(data, file, type, marker);
      }.bind(this));
      return this;
    };
    ConfigChain.prototype.addEnv = function(prefix, env4, name) {
      name = name || "env";
      var data = exports.env(prefix, env4);
      this.sources[name] = { data, source: env4, prefix };
      return this.add(data, name);
    };
    ConfigChain.prototype.addUrl = function(req, type, name) {
      this._await();
      var href = url.format(req);
      name = name || href;
      var marker = { __source__: name };
      this.sources[name] = { href, type };
      this.push(marker);
      http3.request(req, function(res) {
        var c = [];
        var ct = res.headers["content-type"];
        if (!type) {
          type = ct.indexOf("json") !== -1 ? "json" : ct.indexOf("ini") !== -1 ? "ini" : href.match(/\.json$/) ? "json" : href.match(/\.ini$/) ? "ini" : null;
          marker.type = type;
        }
        res.on("data", c.push.bind(c)).on("end", function() {
          this.addString(Buffer.concat(c), href, type, marker);
        }.bind(this)).on("error", this.emit.bind(this, "error"));
      }.bind(this)).on("error", this.emit.bind(this, "error")).end();
      return this;
    };
    ConfigChain.prototype.addString = function(data, file, type, marker) {
      data = this.parse(data, file, type);
      this.add(data, marker);
      return this;
    };
    ConfigChain.prototype.add = function(data, marker) {
      if (marker && typeof marker === "object") {
        var i = this.list.indexOf(marker);
        if (i === -1) {
          return this.emit("error", new Error("bad marker"));
        }
        this.splice(i, 1, data);
        marker = marker.__source__;
        this.sources[marker] = this.sources[marker] || {};
        this.sources[marker].data = data;
        this._resolve();
      } else {
        if (typeof marker === "string") {
          this.sources[marker] = this.sources[marker] || {};
          this.sources[marker].data = data;
        }
        this._await();
        this.push(data);
        process.nextTick(this._resolve.bind(this));
      }
      return this;
    };
    ConfigChain.prototype.parse = exports.parse;
    ConfigChain.prototype._await = function() {
      this._awaiting++;
    };
    ConfigChain.prototype._resolve = function() {
      this._awaiting--;
      if (this._awaiting === 0)
        this.emit("load", this);
    };
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/envKeyToSetting.js
var require_envKeyToSetting = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/envKeyToSetting.js"(exports, module2) {
    init_cjs_shims();
    module2.exports = function(x) {
      const colonIndex = x.indexOf(":");
      if (colonIndex === -1) {
        return normalize(x);
      }
      const firstPart = x.substr(0, colonIndex);
      const secondPart = x.substr(colonIndex + 1);
      return `${normalize(firstPart)}:${normalize(secondPart)}`;
    };
    function normalize(s) {
      s = s.toLowerCase();
      if (s === "_authtoken")
        return "_authToken";
      let r = s[0];
      for (let i = 1; i < s.length; i++) {
        r += s[i] === "_" ? "-" : s[i];
      }
      return r;
    }
  }
});

// ../../node_modules/.pnpm/@pnpm+config.env-replace@1.1.0/node_modules/@pnpm/config.env-replace/dist/env-replace.js
var require_env_replace = __commonJS({
  "../../node_modules/.pnpm/@pnpm+config.env-replace@1.1.0/node_modules/@pnpm/config.env-replace/dist/env-replace.js"(exports) {
    "use strict";
    init_cjs_shims();
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.envReplace = void 0;
    var ENV_EXPR = /(?<!\\)(\\*)\$\{([^${}]+)\}/g;
    function envReplace(settingValue, env3) {
      return settingValue.replace(ENV_EXPR, replaceEnvMatch.bind(null, env3));
    }
    exports.envReplace = envReplace;
    function replaceEnvMatch(env3, orig, escape, name) {
      if (escape.length % 2) {
        return orig.slice((escape.length + 1) / 2);
      }
      const envValue = getEnvValue(env3, name);
      if (envValue === void 0) {
        throw new Error(`Failed to replace env in config: ${orig}`);
      }
      return `${escape.slice(escape.length / 2)}${envValue}`;
    }
    var ENV_VALUE = /([^:-]+)(:?)-(.+)/;
    function getEnvValue(env3, name) {
      const matched = name.match(ENV_VALUE);
      if (!matched)
        return env3[name];
      const [, variableName, colon, fallback] = matched;
      if (Object.prototype.hasOwnProperty.call(env3, variableName)) {
        return !env3[variableName] && colon ? fallback : env3[variableName];
      }
      return fallback;
    }
  }
});

// ../../node_modules/.pnpm/@pnpm+config.env-replace@1.1.0/node_modules/@pnpm/config.env-replace/dist/index.js
var require_dist2 = __commonJS({
  "../../node_modules/.pnpm/@pnpm+config.env-replace@1.1.0/node_modules/@pnpm/config.env-replace/dist/index.js"(exports) {
    "use strict";
    init_cjs_shims();
    Object.defineProperty(exports, "__esModule", { value: true });
    exports.envReplace = void 0;
    var env_replace_1 = require_env_replace();
    Object.defineProperty(exports, "envReplace", { enumerable: true, get: function() {
      return env_replace_1.envReplace;
    } });
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/util.js
var require_util = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/util.js"(exports) {
    "use strict";
    init_cjs_shims();
    var fs3 = require("fs");
    var path6 = require("path");
    var { envReplace } = require_dist2();
    var parseField = (types2, field, key) => {
      if (typeof field !== "string") {
        return field;
      }
      const typeList = [].concat(types2[key]);
      const isPath = typeList.indexOf(path6) !== -1;
      const isBool = typeList.indexOf(Boolean) !== -1;
      const isString = typeList.indexOf(String) !== -1;
      const isNumber = typeList.indexOf(Number) !== -1;
      field = `${field}`.trim();
      if (/^".*"$/.test(field)) {
        try {
          field = JSON.parse(field);
        } catch (error) {
          throw new Error(`Failed parsing JSON config key ${key}: ${field}`);
        }
      }
      if (isBool && !isString && field === "") {
        return true;
      }
      switch (field) {
        case "true": {
          return true;
        }
        case "false": {
          return false;
        }
        case "null": {
          return null;
        }
        case "undefined": {
          return void 0;
        }
      }
      field = envReplace(field, process.env);
      if (isPath) {
        const regex = process.platform === "win32" ? /^~(\/|\\)/ : /^~\//;
        if (regex.test(field) && process.env.HOME) {
          field = path6.resolve(process.env.HOME, field.substr(2));
        }
        field = path6.resolve(field);
      }
      if (isNumber && !isNaN(field)) {
        field = Number(field);
      }
      return field;
    };
    var findPrefix = (name) => {
      name = path6.resolve(name);
      let walkedUp = false;
      while (path6.basename(name) === "node_modules") {
        name = path6.dirname(name);
        walkedUp = true;
      }
      if (walkedUp) {
        return name;
      }
      const find = (name2, original) => {
        const regex = /^[a-zA-Z]:(\\|\/)?$/;
        if (name2 === "/" || process.platform === "win32" && regex.test(name2)) {
          return original;
        }
        try {
          const files = fs3.readdirSync(name2);
          if (files.includes("node_modules") || files.includes("package.json") || files.includes("package.json5") || files.includes("package.yaml") || files.includes("pnpm-workspace.yaml")) {
            return name2;
          }
          const dirname = path6.dirname(name2);
          if (dirname === name2) {
            return original;
          }
          return find(dirname, original);
        } catch (error) {
          if (name2 === original) {
            if (error.code === "ENOENT") {
              return original;
            }
            throw error;
          }
          return original;
        }
      };
      return find(name, name);
    };
    exports.envReplace = envReplace;
    exports.findPrefix = findPrefix;
    exports.parseField = parseField;
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/types.js
var require_types = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/types.js"(exports) {
    "use strict";
    init_cjs_shims();
    var path6 = require("path");
    var Stream = require("stream").Stream;
    var url = require("url");
    var Umask = () => {
    };
    var getLocalAddresses = () => [];
    var semver4 = () => {
    };
    exports.types = {
      access: [null, "restricted", "public"],
      "allow-same-version": Boolean,
      "always-auth": Boolean,
      also: [null, "dev", "development"],
      audit: Boolean,
      "auth-type": ["legacy", "sso", "saml", "oauth"],
      "bin-links": Boolean,
      browser: [null, String],
      ca: [null, String, Array],
      cafile: path6,
      cache: path6,
      "cache-lock-stale": Number,
      "cache-lock-retries": Number,
      "cache-lock-wait": Number,
      "cache-max": Number,
      "cache-min": Number,
      cert: [null, String],
      cidr: [null, String, Array],
      color: ["always", Boolean],
      depth: Number,
      description: Boolean,
      dev: Boolean,
      "dry-run": Boolean,
      editor: String,
      "engine-strict": Boolean,
      force: Boolean,
      "fetch-retries": Number,
      "fetch-retry-factor": Number,
      "fetch-retry-mintimeout": Number,
      "fetch-retry-maxtimeout": Number,
      git: String,
      "git-tag-version": Boolean,
      "commit-hooks": Boolean,
      global: Boolean,
      globalconfig: path6,
      "global-style": Boolean,
      group: [Number, String],
      "https-proxy": [null, url],
      "user-agent": String,
      "ham-it-up": Boolean,
      "heading": String,
      "if-present": Boolean,
      "ignore-prepublish": Boolean,
      "ignore-scripts": Boolean,
      "init-module": path6,
      "init-author-name": String,
      "init-author-email": String,
      "init-author-url": ["", url],
      "init-license": String,
      "init-version": semver4,
      json: Boolean,
      key: [null, String],
      "legacy-bundling": Boolean,
      link: Boolean,
      "local-address": getLocalAddresses(),
      loglevel: ["silent", "error", "warn", "notice", "http", "timing", "info", "verbose", "silly"],
      logstream: Stream,
      "logs-max": Number,
      long: Boolean,
      maxsockets: Number,
      message: String,
      "metrics-registry": [null, String],
      "node-options": [null, String],
      "node-version": [null, semver4],
      "no-proxy": [null, String, Array],
      offline: Boolean,
      "onload-script": [null, String],
      only: [null, "dev", "development", "prod", "production"],
      optional: Boolean,
      "package-lock": Boolean,
      otp: [null, String],
      "package-lock-only": Boolean,
      parseable: Boolean,
      "prefer-offline": Boolean,
      "prefer-online": Boolean,
      prefix: path6,
      production: Boolean,
      progress: Boolean,
      proxy: [null, false, url],
      provenance: Boolean,
      "read-only": Boolean,
      "rebuild-bundle": Boolean,
      registry: [null, url],
      rollback: Boolean,
      save: Boolean,
      "save-bundle": Boolean,
      "save-dev": Boolean,
      "save-exact": Boolean,
      "save-optional": Boolean,
      "save-prefix": String,
      "save-prod": Boolean,
      scope: String,
      "script-shell": [null, String],
      "scripts-prepend-node-path": [false, true, "auto", "warn-only"],
      searchopts: String,
      searchexclude: [null, String],
      searchlimit: Number,
      searchstaleness: Number,
      "send-metrics": Boolean,
      shell: String,
      shrinkwrap: Boolean,
      "sign-git-tag": Boolean,
      "sso-poll-frequency": Number,
      "sso-type": [null, "oauth", "saml"],
      "strict-ssl": Boolean,
      tag: String,
      timing: Boolean,
      tmp: path6,
      unicode: Boolean,
      "unsafe-perm": Boolean,
      usage: Boolean,
      user: [Number, String],
      userconfig: path6,
      umask: Umask,
      version: Boolean,
      "tag-version-prefix": String,
      versions: Boolean,
      viewer: String,
      _exit: Boolean
    };
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/conf.js
var require_conf = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/conf.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var { readCAFileSync } = require_dist();
    var fs3 = require("fs");
    var path6 = require("path");
    var { ConfigChain } = require_config_chain();
    var envKeyToSetting = require_envKeyToSetting();
    var util = require_util();
    var Conf = class extends ConfigChain {
      constructor(base, types2) {
        super(base);
        this.root = base;
        this._parseField = util.parseField.bind(null, types2 || require_types());
      }
      add(data, marker) {
        try {
          for (const x of Object.keys(data)) {
            data[x] = this._parseField(data[x], x);
          }
        } catch (error) {
          throw error;
        }
        return super.add(data, marker);
      }
      addFile(file, name) {
        name = name || file;
        const marker = { __source__: name };
        this.sources[name] = { path: file, type: "ini" };
        this.push(marker);
        this._await();
        try {
          const contents = fs3.readFileSync(file, "utf8");
          this.addString(contents, file, "ini", marker);
        } catch (error) {
          if (error.code === "ENOENT") {
            this.add({}, marker);
          } else {
            return `Issue while reading "${file}". ${error.message}`;
          }
        }
      }
      addEnv(env3) {
        env3 = env3 || process.env;
        const conf = {};
        Object.keys(env3).filter((x) => /^npm_config_/i.test(x)).forEach((x) => {
          if (!env3[x]) {
            return;
          }
          conf[envKeyToSetting(x.substr(11))] = env3[x];
        });
        return super.addEnv("", conf, "env");
      }
      loadPrefix() {
        const cli = this.list[0];
        Object.defineProperty(this, "prefix", {
          enumerable: true,
          set: (prefix) => {
            const g = this.get("global");
            this[g ? "globalPrefix" : "localPrefix"] = prefix;
          },
          get: () => {
            const g = this.get("global");
            return g ? this.globalPrefix : this.localPrefix;
          }
        });
        Object.defineProperty(this, "globalPrefix", {
          enumerable: true,
          set: (prefix) => {
            this.set("prefix", prefix);
          },
          get: () => {
            return path6.resolve(this.get("prefix"));
          }
        });
        let p;
        Object.defineProperty(this, "localPrefix", {
          enumerable: true,
          set: (prefix) => {
            p = prefix;
          },
          get: () => {
            return p;
          }
        });
        if (Object.prototype.hasOwnProperty.call(cli, "prefix")) {
          p = path6.resolve(cli.prefix);
        } else {
          try {
            const prefix = util.findPrefix(process.cwd());
            p = prefix;
          } catch (error) {
            throw error;
          }
        }
        return p;
      }
      loadCAFile(file) {
        if (!file) {
          return;
        }
        const ca = readCAFileSync(file);
        if (ca) {
          this.set("ca", ca);
        }
      }
      loadUser() {
        const defConf = this.root;
        if (this.get("global")) {
          return;
        }
        if (process.env.SUDO_UID) {
          defConf.user = Number(process.env.SUDO_UID);
          return;
        }
        const prefix = path6.resolve(this.get("prefix"));
        try {
          const stats = fs3.statSync(prefix);
          defConf.user = stats.uid;
        } catch (error) {
          if (error.code === "ENOENT") {
            return;
          }
          throw error;
        }
      }
    };
    module2.exports = Conf;
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/defaults.js
var require_defaults = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/lib/defaults.js"(exports) {
    "use strict";
    init_cjs_shims();
    var os5 = require("os");
    var path6 = require("path");
    var temp = os5.tmpdir();
    var uidOrPid = process.getuid ? process.getuid() : process.pid;
    var hasUnicode = () => true;
    var isWindows = process.platform === "win32";
    var osenv = {
      editor: () => process.env.EDITOR || process.env.VISUAL || (isWindows ? "notepad.exe" : "vi"),
      shell: () => isWindows ? process.env.COMSPEC || "cmd.exe" : process.env.SHELL || "/bin/bash"
    };
    var umask = {
      fromString: () => process.umask()
    };
    var home = os5.homedir();
    if (home) {
      process.env.HOME = home;
    } else {
      home = path6.resolve(temp, "npm-" + uidOrPid);
    }
    var cacheExtra = process.platform === "win32" ? "npm-cache" : ".npm";
    var cacheRoot = process.platform === "win32" ? process.env.APPDATA : home;
    var cache = path6.resolve(cacheRoot, cacheExtra);
    var defaults2;
    var globalPrefix;
    Object.defineProperty(exports, "defaults", {
      get: function() {
        if (defaults2)
          return defaults2;
        if (process.env.PREFIX) {
          globalPrefix = process.env.PREFIX;
        } else if (process.platform === "win32") {
          globalPrefix = path6.dirname(process.execPath);
        } else {
          globalPrefix = path6.dirname(path6.dirname(process.execPath));
          if (process.env.DESTDIR) {
            globalPrefix = path6.join(process.env.DESTDIR, globalPrefix);
          }
        }
        defaults2 = {
          access: null,
          "allow-same-version": false,
          "always-auth": false,
          also: null,
          audit: true,
          "auth-type": "legacy",
          "bin-links": true,
          browser: null,
          ca: null,
          cafile: null,
          cache,
          "cache-lock-stale": 6e4,
          "cache-lock-retries": 10,
          "cache-lock-wait": 1e4,
          "cache-max": Infinity,
          "cache-min": 10,
          cert: null,
          cidr: null,
          color: process.env.NO_COLOR == null,
          depth: Infinity,
          description: true,
          dev: false,
          "dry-run": false,
          editor: osenv.editor(),
          "engine-strict": false,
          force: false,
          "fetch-retries": 2,
          "fetch-retry-factor": 10,
          "fetch-retry-mintimeout": 1e4,
          "fetch-retry-maxtimeout": 6e4,
          git: "git",
          "git-tag-version": true,
          "commit-hooks": true,
          global: false,
          globalconfig: path6.resolve(globalPrefix, "etc", "npmrc"),
          "global-style": false,
          group: process.platform === "win32" ? 0 : process.env.SUDO_GID || process.getgid && process.getgid(),
          "ham-it-up": false,
          heading: "npm",
          "if-present": false,
          "ignore-prepublish": false,
          "ignore-scripts": false,
          "init-module": path6.resolve(home, ".npm-init.js"),
          "init-author-name": "",
          "init-author-email": "",
          "init-author-url": "",
          "init-version": "1.0.0",
          "init-license": "ISC",
          json: false,
          key: null,
          "legacy-bundling": false,
          link: false,
          "local-address": void 0,
          loglevel: "notice",
          logstream: process.stderr,
          "logs-max": 10,
          long: false,
          maxsockets: 50,
          message: "%s",
          "metrics-registry": null,
          "node-options": null,
          "offline": false,
          "onload-script": false,
          only: null,
          optional: true,
          otp: null,
          "package-lock": true,
          "package-lock-only": false,
          parseable: false,
          "prefer-offline": false,
          "prefer-online": false,
          prefix: globalPrefix,
          production: process.env.NODE_ENV === "production",
          "progress": !process.env.TRAVIS && !process.env.CI,
          provenance: false,
          proxy: null,
          "https-proxy": null,
          "no-proxy": null,
          "user-agent": "npm/{npm-version} node/{node-version} {platform} {arch}",
          "read-only": false,
          "rebuild-bundle": true,
          registry: "https://registry.npmjs.org/",
          rollback: true,
          save: true,
          "save-bundle": false,
          "save-dev": false,
          "save-exact": false,
          "save-optional": false,
          "save-prefix": "^",
          "save-prod": false,
          scope: "",
          "script-shell": null,
          "scripts-prepend-node-path": "warn-only",
          searchopts: "",
          searchexclude: null,
          searchlimit: 20,
          searchstaleness: 15 * 60,
          "send-metrics": false,
          shell: osenv.shell(),
          shrinkwrap: true,
          "sign-git-tag": false,
          "sso-poll-frequency": 500,
          "sso-type": "oauth",
          "strict-ssl": true,
          tag: "latest",
          "tag-version-prefix": "v",
          timing: false,
          tmp: temp,
          unicode: hasUnicode(),
          "unsafe-perm": process.platform === "win32" || process.platform === "cygwin" || !(process.getuid && process.setuid && process.getgid && process.setgid) || process.getuid() !== 0,
          usage: false,
          user: process.platform === "win32" ? 0 : "nobody",
          userconfig: path6.resolve(home, ".npmrc"),
          umask: process.umask ? process.umask() : umask.fromString("022"),
          version: false,
          versions: false,
          viewer: process.platform === "win32" ? "browser" : "man",
          _exit: true
        };
        return defaults2;
      }
    });
  }
});

// ../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/index.js
var require_npm_conf = __commonJS({
  "../../node_modules/.pnpm/@pnpm+npm-conf@2.2.0/node_modules/@pnpm/npm-conf/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var path6 = require("path");
    var Conf = require_conf();
    var _defaults = require_defaults();
    module2.exports = (opts, types2, defaults2) => {
      const conf = new Conf(Object.assign({}, _defaults.defaults, defaults2), types2);
      conf.add(Object.assign({}, opts), "cli");
      const warnings = [];
      let failedToLoadBuiltInConfig = false;
      if (require.resolve.paths) {
        const paths = require.resolve.paths("npm");
        let npmPath;
        try {
          npmPath = require.resolve("npm", { paths: paths.slice(-1) });
        } catch (error) {
          failedToLoadBuiltInConfig = true;
        }
        if (npmPath) {
          warnings.push(conf.addFile(path6.resolve(path6.dirname(npmPath), "..", "npmrc"), "builtin"));
        }
      }
      conf.addEnv();
      conf.loadPrefix();
      const projectConf = path6.resolve(conf.localPrefix, ".npmrc");
      const userConf = conf.get("userconfig");
      if (!conf.get("global") && projectConf !== userConf) {
        warnings.push(conf.addFile(projectConf, "project"));
      } else {
        conf.add({}, "project");
      }
      if (conf.get("workspace-prefix") && conf.get("workspace-prefix") !== projectConf) {
        const workspaceConf = path6.resolve(conf.get("workspace-prefix"), ".npmrc");
        warnings.push(conf.addFile(workspaceConf, "workspace"));
      }
      warnings.push(conf.addFile(conf.get("userconfig"), "user"));
      if (conf.get("prefix")) {
        const etc = path6.resolve(conf.get("prefix"), "etc");
        conf.root.globalconfig = path6.resolve(etc, "npmrc");
        conf.root.globalignorefile = path6.resolve(etc, "npmignore");
      }
      warnings.push(conf.addFile(conf.get("globalconfig"), "global"));
      conf.loadUser();
      const caFile = conf.get("cafile");
      if (caFile) {
        conf.loadCAFile(caFile);
      }
      return {
        config: conf,
        warnings: warnings.filter(Boolean),
        failedToLoadBuiltInConfig
      };
    };
    Object.defineProperty(module2.exports, "defaults", {
      get() {
        return _defaults.defaults;
      },
      enumerable: true
    });
  }
});

// ../../node_modules/.pnpm/registry-auth-token@5.0.2/node_modules/registry-auth-token/index.js
var require_registry_auth_token = __commonJS({
  "../../node_modules/.pnpm/registry-auth-token@5.0.2/node_modules/registry-auth-token/index.js"(exports, module2) {
    init_cjs_shims();
    var url = require("url");
    var npmConf = require_npm_conf();
    var tokenKey = ":_authToken";
    var legacyTokenKey = ":_auth";
    var userKey = ":username";
    var passwordKey = ":_password";
    module2.exports = function getRegistryAuthToken() {
      let checkUrl;
      let options;
      if (arguments.length >= 2) {
        checkUrl = arguments[0];
        options = Object.assign({}, arguments[1]);
      } else if (typeof arguments[0] === "string") {
        checkUrl = arguments[0];
      } else {
        options = Object.assign({}, arguments[0]);
      }
      options = options || {};
      const providedNpmrc = options.npmrc;
      options.npmrc = (options.npmrc ? {
        config: {
          get: (key) => providedNpmrc[key]
        }
      } : npmConf()).config;
      checkUrl = checkUrl || options.npmrc.get("registry") || npmConf.defaults.registry;
      return getRegistryAuthInfo(checkUrl, options) || getLegacyAuthInfo(options.npmrc);
    };
    function getRegistryAuthInfo(checkUrl, options) {
      const parsed = url.parse(checkUrl, false, true);
      let pathname;
      while (pathname !== "/" && parsed.pathname !== pathname) {
        pathname = parsed.pathname || "/";
        const regUrl = "//" + parsed.host + pathname.replace(/\/$/, "");
        const authInfo = getAuthInfoForUrl(regUrl, options.npmrc);
        if (authInfo) {
          return authInfo;
        }
        if (!options.recursive) {
          return /\/$/.test(checkUrl) ? void 0 : getRegistryAuthInfo(url.resolve(checkUrl, "."), options);
        }
        parsed.pathname = url.resolve(normalizePath(pathname), "..") || "/";
      }
      return void 0;
    }
    function getLegacyAuthInfo(npmrc) {
      if (!npmrc.get("_auth")) {
        return void 0;
      }
      const token = replaceEnvironmentVariable(npmrc.get("_auth"));
      return { token, type: "Basic" };
    }
    function normalizePath(path6) {
      return path6[path6.length - 1] === "/" ? path6 : path6 + "/";
    }
    function getAuthInfoForUrl(regUrl, npmrc) {
      const bearerAuth = getBearerToken(npmrc.get(regUrl + tokenKey) || npmrc.get(regUrl + "/" + tokenKey));
      if (bearerAuth) {
        return bearerAuth;
      }
      const username = npmrc.get(regUrl + userKey) || npmrc.get(regUrl + "/" + userKey);
      const password = npmrc.get(regUrl + passwordKey) || npmrc.get(regUrl + "/" + passwordKey);
      const basicAuth = getTokenForUsernameAndPassword(username, password);
      if (basicAuth) {
        return basicAuth;
      }
      const basicAuthWithToken = getLegacyAuthToken(npmrc.get(regUrl + legacyTokenKey) || npmrc.get(regUrl + "/" + legacyTokenKey));
      if (basicAuthWithToken) {
        return basicAuthWithToken;
      }
      return void 0;
    }
    function replaceEnvironmentVariable(token) {
      return token.replace(/^\$\{?([^}]*)\}?$/, function(fullMatch, envVar) {
        return process.env[envVar];
      });
    }
    function getBearerToken(tok) {
      if (!tok) {
        return void 0;
      }
      const token = replaceEnvironmentVariable(tok);
      return { token, type: "Bearer" };
    }
    function getTokenForUsernameAndPassword(username, password) {
      if (!username || !password) {
        return void 0;
      }
      const pass = Buffer.from(replaceEnvironmentVariable(password), "base64").toString("utf8");
      const token = Buffer.from(username + ":" + pass, "utf8").toString("base64");
      return {
        token,
        type: "Basic",
        password: pass,
        username
      };
    }
    function getLegacyAuthToken(tok) {
      if (!tok) {
        return void 0;
      }
      const token = replaceEnvironmentVariable(tok);
      return { token, type: "Basic" };
    }
  }
});

// ../../node_modules/.pnpm/ini@2.0.0/node_modules/ini/ini.js
var require_ini2 = __commonJS({
  "../../node_modules/.pnpm/ini@2.0.0/node_modules/ini/ini.js"(exports, module2) {
    init_cjs_shims();
    var { hasOwnProperty } = Object.prototype;
    var eol = typeof process !== "undefined" && process.platform === "win32" ? "\r\n" : "\n";
    var encode = (obj, opt) => {
      const children = [];
      let out = "";
      if (typeof opt === "string") {
        opt = {
          section: opt,
          whitespace: false
        };
      } else {
        opt = opt || /* @__PURE__ */ Object.create(null);
        opt.whitespace = opt.whitespace === true;
      }
      const separator = opt.whitespace ? " = " : "=";
      for (const k of Object.keys(obj)) {
        const val = obj[k];
        if (val && Array.isArray(val)) {
          for (const item of val)
            out += safe(k + "[]") + separator + safe(item) + "\n";
        } else if (val && typeof val === "object")
          children.push(k);
        else
          out += safe(k) + separator + safe(val) + eol;
      }
      if (opt.section && out.length)
        out = "[" + safe(opt.section) + "]" + eol + out;
      for (const k of children) {
        const nk = dotSplit(k).join("\\.");
        const section = (opt.section ? opt.section + "." : "") + nk;
        const { whitespace } = opt;
        const child = encode(obj[k], {
          section,
          whitespace
        });
        if (out.length && child.length)
          out += eol;
        out += child;
      }
      return out;
    };
    var dotSplit = (str) => str.replace(/\1/g, "LITERAL\\1LITERAL").replace(/\\\./g, "").split(/\./).map((part) => part.replace(/\1/g, "\\.").replace(/\2LITERAL\\1LITERAL\2/g, ""));
    var decode = (str) => {
      const out = /* @__PURE__ */ Object.create(null);
      let p = out;
      let section = null;
      const re = /^\[([^\]]*)\]$|^([^=]+)(=(.*))?$/i;
      const lines = str.split(/[\r\n]+/g);
      for (const line of lines) {
        if (!line || line.match(/^\s*[;#]/))
          continue;
        const match = line.match(re);
        if (!match)
          continue;
        if (match[1] !== void 0) {
          section = unsafe(match[1]);
          if (section === "__proto__") {
            p = /* @__PURE__ */ Object.create(null);
            continue;
          }
          p = out[section] = out[section] || /* @__PURE__ */ Object.create(null);
          continue;
        }
        const keyRaw = unsafe(match[2]);
        const isArray = keyRaw.length > 2 && keyRaw.slice(-2) === "[]";
        const key = isArray ? keyRaw.slice(0, -2) : keyRaw;
        if (key === "__proto__")
          continue;
        const valueRaw = match[3] ? unsafe(match[4]) : true;
        const value = valueRaw === "true" || valueRaw === "false" || valueRaw === "null" ? JSON.parse(valueRaw) : valueRaw;
        if (isArray) {
          if (!hasOwnProperty.call(p, key))
            p[key] = [];
          else if (!Array.isArray(p[key]))
            p[key] = [p[key]];
        }
        if (Array.isArray(p[key]))
          p[key].push(value);
        else
          p[key] = value;
      }
      const remove = [];
      for (const k of Object.keys(out)) {
        if (!hasOwnProperty.call(out, k) || typeof out[k] !== "object" || Array.isArray(out[k]))
          continue;
        const parts = dotSplit(k);
        let p2 = out;
        const l = parts.pop();
        const nl = l.replace(/\\\./g, ".");
        for (const part of parts) {
          if (part === "__proto__")
            continue;
          if (!hasOwnProperty.call(p2, part) || typeof p2[part] !== "object")
            p2[part] = /* @__PURE__ */ Object.create(null);
          p2 = p2[part];
        }
        if (p2 === out && nl === l)
          continue;
        p2[nl] = out[k];
        remove.push(k);
      }
      for (const del of remove)
        delete out[del];
      return out;
    };
    var isQuoted = (val) => val.charAt(0) === '"' && val.slice(-1) === '"' || val.charAt(0) === "'" && val.slice(-1) === "'";
    var safe = (val) => typeof val !== "string" || val.match(/[=\r\n]/) || val.match(/^\[/) || val.length > 1 && isQuoted(val) || val !== val.trim() ? JSON.stringify(val) : val.replace(/;/g, "\\;").replace(/#/g, "\\#");
    var unsafe = (val, doUnesc) => {
      val = (val || "").trim();
      if (isQuoted(val)) {
        if (val.charAt(0) === "'")
          val = val.substr(1, val.length - 2);
        try {
          val = JSON.parse(val);
        } catch (_) {
        }
      } else {
        let esc = false;
        let unesc = "";
        for (let i = 0, l = val.length; i < l; i++) {
          const c = val.charAt(i);
          if (esc) {
            if ("\\;#".indexOf(c) !== -1)
              unesc += c;
            else
              unesc += "\\" + c;
            esc = false;
          } else if (";#".indexOf(c) !== -1)
            break;
          else if (c === "\\")
            esc = true;
          else
            unesc += c;
        }
        if (esc)
          unesc += "\\";
        return unesc.trim();
      }
      return val;
    };
    module2.exports = {
      parse: decode,
      decode,
      stringify: encode,
      encode,
      safe,
      unsafe
    };
  }
});

// ../../node_modules/.pnpm/global-dirs@3.0.0/node_modules/global-dirs/index.js
var require_global_dirs = __commonJS({
  "../../node_modules/.pnpm/global-dirs@3.0.0/node_modules/global-dirs/index.js"(exports) {
    "use strict";
    init_cjs_shims();
    var path6 = require("path");
    var os5 = require("os");
    var fs3 = require("fs");
    var ini = require_ini2();
    var isWindows = process.platform === "win32";
    var readRc = (filePath) => {
      try {
        return ini.parse(fs3.readFileSync(filePath, "utf8")).prefix;
      } catch {
      }
    };
    var getEnvNpmPrefix = () => {
      return Object.keys(process.env).reduce((prefix, name) => {
        return /^npm_config_prefix$/i.test(name) ? process.env[name] : prefix;
      }, void 0);
    };
    var getGlobalNpmrc = () => {
      if (isWindows && process.env.APPDATA) {
        return path6.join(process.env.APPDATA, "/npm/etc/npmrc");
      }
      if (process.execPath.includes("/Cellar/node")) {
        const homebrewPrefix = process.execPath.slice(0, process.execPath.indexOf("/Cellar/node"));
        return path6.join(homebrewPrefix, "/lib/node_modules/npm/npmrc");
      }
      if (process.execPath.endsWith("/bin/node")) {
        const installDir = path6.dirname(path6.dirname(process.execPath));
        return path6.join(installDir, "/etc/npmrc");
      }
    };
    var getDefaultNpmPrefix = () => {
      if (isWindows) {
        return path6.dirname(process.execPath);
      }
      return path6.dirname(path6.dirname(process.execPath));
    };
    var getNpmPrefix = () => {
      const envPrefix = getEnvNpmPrefix();
      if (envPrefix) {
        return envPrefix;
      }
      const homePrefix = readRc(path6.join(os5.homedir(), ".npmrc"));
      if (homePrefix) {
        return homePrefix;
      }
      if (process.env.PREFIX) {
        return process.env.PREFIX;
      }
      const globalPrefix = readRc(getGlobalNpmrc());
      if (globalPrefix) {
        return globalPrefix;
      }
      return getDefaultNpmPrefix();
    };
    var npmPrefix = path6.resolve(getNpmPrefix());
    var getYarnWindowsDirectory = () => {
      if (isWindows && process.env.LOCALAPPDATA) {
        const dir = path6.join(process.env.LOCALAPPDATA, "Yarn");
        if (fs3.existsSync(dir)) {
          return dir;
        }
      }
      return false;
    };
    var getYarnPrefix = () => {
      if (process.env.PREFIX) {
        return process.env.PREFIX;
      }
      const windowsPrefix = getYarnWindowsDirectory();
      if (windowsPrefix) {
        return windowsPrefix;
      }
      const configPrefix = path6.join(os5.homedir(), ".config/yarn");
      if (fs3.existsSync(configPrefix)) {
        return configPrefix;
      }
      const homePrefix = path6.join(os5.homedir(), ".yarn-config");
      if (fs3.existsSync(homePrefix)) {
        return homePrefix;
      }
      return npmPrefix;
    };
    exports.npm = {};
    exports.npm.prefix = npmPrefix;
    exports.npm.packages = path6.join(npmPrefix, isWindows ? "node_modules" : "lib/node_modules");
    exports.npm.binaries = isWindows ? npmPrefix : path6.join(npmPrefix, "bin");
    var yarnPrefix = path6.resolve(getYarnPrefix());
    exports.yarn = {};
    exports.yarn.prefix = yarnPrefix;
    exports.yarn.packages = path6.join(yarnPrefix, getYarnWindowsDirectory() ? "Data/global/node_modules" : "global/node_modules");
    exports.yarn.binaries = path6.join(exports.yarn.packages, ".bin");
  }
});

// ../../node_modules/.pnpm/is-path-inside@3.0.3/node_modules/is-path-inside/index.js
var require_is_path_inside = __commonJS({
  "../../node_modules/.pnpm/is-path-inside@3.0.3/node_modules/is-path-inside/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var path6 = require("path");
    module2.exports = (childPath, parentPath) => {
      const relation = path6.relative(parentPath, childPath);
      return Boolean(
        relation && relation !== ".." && !relation.startsWith(`..${path6.sep}`) && relation !== path6.resolve(childPath)
      );
    };
  }
});

// ../../node_modules/.pnpm/is-installed-globally@0.4.0/node_modules/is-installed-globally/index.js
var require_is_installed_globally = __commonJS({
  "../../node_modules/.pnpm/is-installed-globally@0.4.0/node_modules/is-installed-globally/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var fs3 = require("fs");
    var globalDirs = require_global_dirs();
    var isPathInside = require_is_path_inside();
    module2.exports = (() => {
      try {
        return isPathInside(__dirname, globalDirs.yarn.packages) || isPathInside(__dirname, fs3.realpathSync(globalDirs.npm.packages));
      } catch {
        return false;
      }
    })();
  }
});

// ../../node_modules/.pnpm/eastasianwidth@0.2.0/node_modules/eastasianwidth/eastasianwidth.js
var require_eastasianwidth = __commonJS({
  "../../node_modules/.pnpm/eastasianwidth@0.2.0/node_modules/eastasianwidth/eastasianwidth.js"(exports, module2) {
    init_cjs_shims();
    var eaw = {};
    if ("undefined" == typeof module2) {
      window.eastasianwidth = eaw;
    } else {
      module2.exports = eaw;
    }
    eaw.eastAsianWidth = function(character) {
      var x = character.charCodeAt(0);
      var y = character.length == 2 ? character.charCodeAt(1) : 0;
      var codePoint = x;
      if (55296 <= x && x <= 56319 && (56320 <= y && y <= 57343)) {
        x &= 1023;
        y &= 1023;
        codePoint = x << 10 | y;
        codePoint += 65536;
      }
      if (12288 == codePoint || 65281 <= codePoint && codePoint <= 65376 || 65504 <= codePoint && codePoint <= 65510) {
        return "F";
      }
      if (8361 == codePoint || 65377 <= codePoint && codePoint <= 65470 || 65474 <= codePoint && codePoint <= 65479 || 65482 <= codePoint && codePoint <= 65487 || 65490 <= codePoint && codePoint <= 65495 || 65498 <= codePoint && codePoint <= 65500 || 65512 <= codePoint && codePoint <= 65518) {
        return "H";
      }
      if (4352 <= codePoint && codePoint <= 4447 || 4515 <= codePoint && codePoint <= 4519 || 4602 <= codePoint && codePoint <= 4607 || 9001 <= codePoint && codePoint <= 9002 || 11904 <= codePoint && codePoint <= 11929 || 11931 <= codePoint && codePoint <= 12019 || 12032 <= codePoint && codePoint <= 12245 || 12272 <= codePoint && codePoint <= 12283 || 12289 <= codePoint && codePoint <= 12350 || 12353 <= codePoint && codePoint <= 12438 || 12441 <= codePoint && codePoint <= 12543 || 12549 <= codePoint && codePoint <= 12589 || 12593 <= codePoint && codePoint <= 12686 || 12688 <= codePoint && codePoint <= 12730 || 12736 <= codePoint && codePoint <= 12771 || 12784 <= codePoint && codePoint <= 12830 || 12832 <= codePoint && codePoint <= 12871 || 12880 <= codePoint && codePoint <= 13054 || 13056 <= codePoint && codePoint <= 19903 || 19968 <= codePoint && codePoint <= 42124 || 42128 <= codePoint && codePoint <= 42182 || 43360 <= codePoint && codePoint <= 43388 || 44032 <= codePoint && codePoint <= 55203 || 55216 <= codePoint && codePoint <= 55238 || 55243 <= codePoint && codePoint <= 55291 || 63744 <= codePoint && codePoint <= 64255 || 65040 <= codePoint && codePoint <= 65049 || 65072 <= codePoint && codePoint <= 65106 || 65108 <= codePoint && codePoint <= 65126 || 65128 <= codePoint && codePoint <= 65131 || 110592 <= codePoint && codePoint <= 110593 || 127488 <= codePoint && codePoint <= 127490 || 127504 <= codePoint && codePoint <= 127546 || 127552 <= codePoint && codePoint <= 127560 || 127568 <= codePoint && codePoint <= 127569 || 131072 <= codePoint && codePoint <= 194367 || 177984 <= codePoint && codePoint <= 196605 || 196608 <= codePoint && codePoint <= 262141) {
        return "W";
      }
      if (32 <= codePoint && codePoint <= 126 || 162 <= codePoint && codePoint <= 163 || 165 <= codePoint && codePoint <= 166 || 172 == codePoint || 175 == codePoint || 10214 <= codePoint && codePoint <= 10221 || 10629 <= codePoint && codePoint <= 10630) {
        return "Na";
      }
      if (161 == codePoint || 164 == codePoint || 167 <= codePoint && codePoint <= 168 || 170 == codePoint || 173 <= codePoint && codePoint <= 174 || 176 <= codePoint && codePoint <= 180 || 182 <= codePoint && codePoint <= 186 || 188 <= codePoint && codePoint <= 191 || 198 == codePoint || 208 == codePoint || 215 <= codePoint && codePoint <= 216 || 222 <= codePoint && codePoint <= 225 || 230 == codePoint || 232 <= codePoint && codePoint <= 234 || 236 <= codePoint && codePoint <= 237 || 240 == codePoint || 242 <= codePoint && codePoint <= 243 || 247 <= codePoint && codePoint <= 250 || 252 == codePoint || 254 == codePoint || 257 == codePoint || 273 == codePoint || 275 == codePoint || 283 == codePoint || 294 <= codePoint && codePoint <= 295 || 299 == codePoint || 305 <= codePoint && codePoint <= 307 || 312 == codePoint || 319 <= codePoint && codePoint <= 322 || 324 == codePoint || 328 <= codePoint && codePoint <= 331 || 333 == codePoint || 338 <= codePoint && codePoint <= 339 || 358 <= codePoint && codePoint <= 359 || 363 == codePoint || 462 == codePoint || 464 == codePoint || 466 == codePoint || 468 == codePoint || 470 == codePoint || 472 == codePoint || 474 == codePoint || 476 == codePoint || 593 == codePoint || 609 == codePoint || 708 == codePoint || 711 == codePoint || 713 <= codePoint && codePoint <= 715 || 717 == codePoint || 720 == codePoint || 728 <= codePoint && codePoint <= 731 || 733 == codePoint || 735 == codePoint || 768 <= codePoint && codePoint <= 879 || 913 <= codePoint && codePoint <= 929 || 931 <= codePoint && codePoint <= 937 || 945 <= codePoint && codePoint <= 961 || 963 <= codePoint && codePoint <= 969 || 1025 == codePoint || 1040 <= codePoint && codePoint <= 1103 || 1105 == codePoint || 8208 == codePoint || 8211 <= codePoint && codePoint <= 8214 || 8216 <= codePoint && codePoint <= 8217 || 8220 <= codePoint && codePoint <= 8221 || 8224 <= codePoint && codePoint <= 8226 || 8228 <= codePoint && codePoint <= 8231 || 8240 == codePoint || 8242 <= codePoint && codePoint <= 8243 || 8245 == codePoint || 8251 == codePoint || 8254 == codePoint || 8308 == codePoint || 8319 == codePoint || 8321 <= codePoint && codePoint <= 8324 || 8364 == codePoint || 8451 == codePoint || 8453 == codePoint || 8457 == codePoint || 8467 == codePoint || 8470 == codePoint || 8481 <= codePoint && codePoint <= 8482 || 8486 == codePoint || 8491 == codePoint || 8531 <= codePoint && codePoint <= 8532 || 8539 <= codePoint && codePoint <= 8542 || 8544 <= codePoint && codePoint <= 8555 || 8560 <= codePoint && codePoint <= 8569 || 8585 == codePoint || 8592 <= codePoint && codePoint <= 8601 || 8632 <= codePoint && codePoint <= 8633 || 8658 == codePoint || 8660 == codePoint || 8679 == codePoint || 8704 == codePoint || 8706 <= codePoint && codePoint <= 8707 || 8711 <= codePoint && codePoint <= 8712 || 8715 == codePoint || 8719 == codePoint || 8721 == codePoint || 8725 == codePoint || 8730 == codePoint || 8733 <= codePoint && codePoint <= 8736 || 8739 == codePoint || 8741 == codePoint || 8743 <= codePoint && codePoint <= 8748 || 8750 == codePoint || 8756 <= codePoint && codePoint <= 8759 || 8764 <= codePoint && codePoint <= 8765 || 8776 == codePoint || 8780 == codePoint || 8786 == codePoint || 8800 <= codePoint && codePoint <= 8801 || 8804 <= codePoint && codePoint <= 8807 || 8810 <= codePoint && codePoint <= 8811 || 8814 <= codePoint && codePoint <= 8815 || 8834 <= codePoint && codePoint <= 8835 || 8838 <= codePoint && codePoint <= 8839 || 8853 == codePoint || 8857 == codePoint || 8869 == codePoint || 8895 == codePoint || 8978 == codePoint || 9312 <= codePoint && codePoint <= 9449 || 9451 <= codePoint && codePoint <= 9547 || 9552 <= codePoint && codePoint <= 9587 || 9600 <= codePoint && codePoint <= 9615 || 9618 <= codePoint && codePoint <= 9621 || 9632 <= codePoint && codePoint <= 9633 || 9635 <= codePoint && codePoint <= 9641 || 9650 <= codePoint && codePoint <= 9651 || 9654 <= codePoint && codePoint <= 9655 || 9660 <= codePoint && codePoint <= 9661 || 9664 <= codePoint && codePoint <= 9665 || 9670 <= codePoint && codePoint <= 9672 || 9675 == codePoint || 9678 <= codePoint && codePoint <= 9681 || 9698 <= codePoint && codePoint <= 9701 || 9711 == codePoint || 9733 <= codePoint && codePoint <= 9734 || 9737 == codePoint || 9742 <= codePoint && codePoint <= 9743 || 9748 <= codePoint && codePoint <= 9749 || 9756 == codePoint || 9758 == codePoint || 9792 == codePoint || 9794 == codePoint || 9824 <= codePoint && codePoint <= 9825 || 9827 <= codePoint && codePoint <= 9829 || 9831 <= codePoint && codePoint <= 9834 || 9836 <= codePoint && codePoint <= 9837 || 9839 == codePoint || 9886 <= codePoint && codePoint <= 9887 || 9918 <= codePoint && codePoint <= 9919 || 9924 <= codePoint && codePoint <= 9933 || 9935 <= codePoint && codePoint <= 9953 || 9955 == codePoint || 9960 <= codePoint && codePoint <= 9983 || 10045 == codePoint || 10071 == codePoint || 10102 <= codePoint && codePoint <= 10111 || 11093 <= codePoint && codePoint <= 11097 || 12872 <= codePoint && codePoint <= 12879 || 57344 <= codePoint && codePoint <= 63743 || 65024 <= codePoint && codePoint <= 65039 || 65533 == codePoint || 127232 <= codePoint && codePoint <= 127242 || 127248 <= codePoint && codePoint <= 127277 || 127280 <= codePoint && codePoint <= 127337 || 127344 <= codePoint && codePoint <= 127386 || 917760 <= codePoint && codePoint <= 917999 || 983040 <= codePoint && codePoint <= 1048573 || 1048576 <= codePoint && codePoint <= 1114109) {
        return "A";
      }
      return "N";
    };
    eaw.characterLength = function(character) {
      var code = this.eastAsianWidth(character);
      if (code == "F" || code == "W" || code == "A") {
        return 2;
      } else {
        return 1;
      }
    };
    function stringToArray(string) {
      return string.match(/[\uD800-\uDBFF][\uDC00-\uDFFF]|[^\uD800-\uDFFF]/g) || [];
    }
    eaw.length = function(string) {
      var characters = stringToArray(string);
      var len = 0;
      for (var i = 0; i < characters.length; i++) {
        len = len + this.characterLength(characters[i]);
      }
      return len;
    };
    eaw.slice = function(text, start, end) {
      textLen = eaw.length(text);
      start = start ? start : 0;
      end = end ? end : 1;
      if (start < 0) {
        start = textLen + start;
      }
      if (end < 0) {
        end = textLen + end;
      }
      var result = "";
      var eawLen = 0;
      var chars = stringToArray(text);
      for (var i = 0; i < chars.length; i++) {
        var char = chars[i];
        var charLen = eaw.length(char);
        if (eawLen >= start - (charLen == 2 ? 1 : 0)) {
          if (eawLen + charLen <= end) {
            result += char;
          } else {
            break;
          }
        }
        eawLen += charLen;
      }
      return result;
    };
  }
});

// ../../node_modules/.pnpm/emoji-regex@9.2.2/node_modules/emoji-regex/index.js
var require_emoji_regex = __commonJS({
  "../../node_modules/.pnpm/emoji-regex@9.2.2/node_modules/emoji-regex/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = function() {
      return /\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62(?:\uDB40\uDC77\uDB40\uDC6C\uDB40\uDC73|\uDB40\uDC73\uDB40\uDC63\uDB40\uDC74|\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67)\uDB40\uDC7F|(?:\uD83E\uDDD1\uD83C\uDFFF\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFF\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFB-\uDFFE])|(?:\uD83E\uDDD1\uD83C\uDFFE\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFE\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFB-\uDFFD\uDFFF])|(?:\uD83E\uDDD1\uD83C\uDFFD\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFD\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFB\uDFFC\uDFFE\uDFFF])|(?:\uD83E\uDDD1\uD83C\uDFFC\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFC\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFB\uDFFD-\uDFFF])|(?:\uD83E\uDDD1\uD83C\uDFFB\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFB\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFC-\uDFFF])|\uD83D\uDC68(?:\uD83C\uDFFB(?:\u200D(?:\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFF])|\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFF]))|\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFC-\uDFFF])|[\u2695\u2696\u2708]\uFE0F|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD]))?|(?:\uD83C[\uDFFC-\uDFFF])\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFF])|\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFF]))|\u200D(?:\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83D\uDC68|(?:\uD83D[\uDC68\uDC69])\u200D(?:\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67]))|\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFF\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFE])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFE\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFD\uDFFF])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFD\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB\uDFFC\uDFFE\uDFFF])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFC\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB\uDFFD-\uDFFF])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|(?:\uD83C\uDFFF\u200D[\u2695\u2696\u2708]|\uD83C\uDFFE\u200D[\u2695\u2696\u2708]|\uD83C\uDFFD\u200D[\u2695\u2696\u2708]|\uD83C\uDFFC\u200D[\u2695\u2696\u2708]|\u200D[\u2695\u2696\u2708])\uFE0F|\u200D(?:(?:\uD83D[\uDC68\uDC69])\u200D(?:\uD83D[\uDC66\uDC67])|\uD83D[\uDC66\uDC67])|\uD83C\uDFFF|\uD83C\uDFFE|\uD83C\uDFFD|\uD83C\uDFFC)?|(?:\uD83D\uDC69(?:\uD83C\uDFFB\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D(?:\uD83D[\uDC68\uDC69])|\uD83D[\uDC68\uDC69])|(?:\uD83C[\uDFFC-\uDFFF])\u200D\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D(?:\uD83D[\uDC68\uDC69])|\uD83D[\uDC68\uDC69]))|\uD83E\uDDD1(?:\uD83C[\uDFFB-\uDFFF])\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1)(?:\uD83C[\uDFFB-\uDFFF])|\uD83D\uDC69\u200D\uD83D\uDC69\u200D(?:\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67]))|\uD83D\uDC69(?:\u200D(?:\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D(?:\uD83D[\uDC68\uDC69])|\uD83D[\uDC68\uDC69])|\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFF\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFE\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFD\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFC\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFB\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD]))|\uD83E\uDDD1(?:\u200D(?:\uD83E\uDD1D\u200D\uD83E\uDDD1|\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFF\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFE\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFD\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFC\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFB\u200D(?:\uD83C[\uDF3E\uDF73\uDF7C\uDF84\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD]))|\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC69\u200D\uD83D\uDC69\u200D(?:\uD83D[\uDC66\uDC67])|\uD83D\uDC69\u200D\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67])|(?:\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8|\uD83E\uDDD1(?:\uD83C\uDFFF\u200D[\u2695\u2696\u2708]|\uD83C\uDFFE\u200D[\u2695\u2696\u2708]|\uD83C\uDFFD\u200D[\u2695\u2696\u2708]|\uD83C\uDFFC\u200D[\u2695\u2696\u2708]|\uD83C\uDFFB\u200D[\u2695\u2696\u2708]|\u200D[\u2695\u2696\u2708])|\uD83D\uDC69(?:\uD83C\uDFFF\u200D[\u2695\u2696\u2708]|\uD83C\uDFFE\u200D[\u2695\u2696\u2708]|\uD83C\uDFFD\u200D[\u2695\u2696\u2708]|\uD83C\uDFFC\u200D[\u2695\u2696\u2708]|\uD83C\uDFFB\u200D[\u2695\u2696\u2708]|\u200D[\u2695\u2696\u2708])|\uD83D\uDE36\u200D\uD83C\uDF2B|\uD83C\uDFF3\uFE0F\u200D\u26A7|\uD83D\uDC3B\u200D\u2744|(?:(?:\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC70\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD35\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD4\uDDD6-\uDDDD])(?:\uD83C[\uDFFB-\uDFFF])|\uD83D\uDC6F|\uD83E[\uDD3C\uDDDE\uDDDF])\u200D[\u2640\u2642]|(?:\u26F9|\uD83C[\uDFCB\uDFCC]|\uD83D\uDD75)(?:\uFE0F|\uD83C[\uDFFB-\uDFFF])\u200D[\u2640\u2642]|\uD83C\uDFF4\u200D\u2620|(?:\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC70\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD35\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD4\uDDD6-\uDDDD])\u200D[\u2640\u2642]|[\xA9\xAE\u203C\u2049\u2122\u2139\u2194-\u2199\u21A9\u21AA\u2328\u23CF\u23ED-\u23EF\u23F1\u23F2\u23F8-\u23FA\u24C2\u25AA\u25AB\u25B6\u25C0\u25FB\u25FC\u2600-\u2604\u260E\u2611\u2618\u2620\u2622\u2623\u2626\u262A\u262E\u262F\u2638-\u263A\u2640\u2642\u265F\u2660\u2663\u2665\u2666\u2668\u267B\u267E\u2692\u2694-\u2697\u2699\u269B\u269C\u26A0\u26A7\u26B0\u26B1\u26C8\u26CF\u26D1\u26D3\u26E9\u26F0\u26F1\u26F4\u26F7\u26F8\u2702\u2708\u2709\u270F\u2712\u2714\u2716\u271D\u2721\u2733\u2734\u2744\u2747\u2763\u27A1\u2934\u2935\u2B05-\u2B07\u3030\u303D\u3297\u3299]|\uD83C[\uDD70\uDD71\uDD7E\uDD7F\uDE02\uDE37\uDF21\uDF24-\uDF2C\uDF36\uDF7D\uDF96\uDF97\uDF99-\uDF9B\uDF9E\uDF9F\uDFCD\uDFCE\uDFD4-\uDFDF\uDFF5\uDFF7]|\uD83D[\uDC3F\uDCFD\uDD49\uDD4A\uDD6F\uDD70\uDD73\uDD76-\uDD79\uDD87\uDD8A-\uDD8D\uDDA5\uDDA8\uDDB1\uDDB2\uDDBC\uDDC2-\uDDC4\uDDD1-\uDDD3\uDDDC-\uDDDE\uDDE1\uDDE3\uDDE8\uDDEF\uDDF3\uDDFA\uDECB\uDECD-\uDECF\uDEE0-\uDEE5\uDEE9\uDEF0\uDEF3])\uFE0F|\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08|\uD83D\uDC69\u200D\uD83D\uDC67|\uD83D\uDC69\u200D\uD83D\uDC66|\uD83D\uDE35\u200D\uD83D\uDCAB|\uD83D\uDE2E\u200D\uD83D\uDCA8|\uD83D\uDC15\u200D\uD83E\uDDBA|\uD83E\uDDD1(?:\uD83C\uDFFF|\uD83C\uDFFE|\uD83C\uDFFD|\uD83C\uDFFC|\uD83C\uDFFB)?|\uD83D\uDC69(?:\uD83C\uDFFF|\uD83C\uDFFE|\uD83C\uDFFD|\uD83C\uDFFC|\uD83C\uDFFB)?|\uD83C\uDDFD\uD83C\uDDF0|\uD83C\uDDF6\uD83C\uDDE6|\uD83C\uDDF4\uD83C\uDDF2|\uD83D\uDC08\u200D\u2B1B|\u2764\uFE0F\u200D(?:\uD83D\uDD25|\uD83E\uDE79)|\uD83D\uDC41\uFE0F|\uD83C\uDFF3\uFE0F|\uD83C\uDDFF(?:\uD83C[\uDDE6\uDDF2\uDDFC])|\uD83C\uDDFE(?:\uD83C[\uDDEA\uDDF9])|\uD83C\uDDFC(?:\uD83C[\uDDEB\uDDF8])|\uD83C\uDDFB(?:\uD83C[\uDDE6\uDDE8\uDDEA\uDDEC\uDDEE\uDDF3\uDDFA])|\uD83C\uDDFA(?:\uD83C[\uDDE6\uDDEC\uDDF2\uDDF3\uDDF8\uDDFE\uDDFF])|\uD83C\uDDF9(?:\uD83C[\uDDE6\uDDE8\uDDE9\uDDEB-\uDDED\uDDEF-\uDDF4\uDDF7\uDDF9\uDDFB\uDDFC\uDDFF])|\uD83C\uDDF8(?:\uD83C[\uDDE6-\uDDEA\uDDEC-\uDDF4\uDDF7-\uDDF9\uDDFB\uDDFD-\uDDFF])|\uD83C\uDDF7(?:\uD83C[\uDDEA\uDDF4\uDDF8\uDDFA\uDDFC])|\uD83C\uDDF5(?:\uD83C[\uDDE6\uDDEA-\uDDED\uDDF0-\uDDF3\uDDF7-\uDDF9\uDDFC\uDDFE])|\uD83C\uDDF3(?:\uD83C[\uDDE6\uDDE8\uDDEA-\uDDEC\uDDEE\uDDF1\uDDF4\uDDF5\uDDF7\uDDFA\uDDFF])|\uD83C\uDDF2(?:\uD83C[\uDDE6\uDDE8-\uDDED\uDDF0-\uDDFF])|\uD83C\uDDF1(?:\uD83C[\uDDE6-\uDDE8\uDDEE\uDDF0\uDDF7-\uDDFB\uDDFE])|\uD83C\uDDF0(?:\uD83C[\uDDEA\uDDEC-\uDDEE\uDDF2\uDDF3\uDDF5\uDDF7\uDDFC\uDDFE\uDDFF])|\uD83C\uDDEF(?:\uD83C[\uDDEA\uDDF2\uDDF4\uDDF5])|\uD83C\uDDEE(?:\uD83C[\uDDE8-\uDDEA\uDDF1-\uDDF4\uDDF6-\uDDF9])|\uD83C\uDDED(?:\uD83C[\uDDF0\uDDF2\uDDF3\uDDF7\uDDF9\uDDFA])|\uD83C\uDDEC(?:\uD83C[\uDDE6\uDDE7\uDDE9-\uDDEE\uDDF1-\uDDF3\uDDF5-\uDDFA\uDDFC\uDDFE])|\uD83C\uDDEB(?:\uD83C[\uDDEE-\uDDF0\uDDF2\uDDF4\uDDF7])|\uD83C\uDDEA(?:\uD83C[\uDDE6\uDDE8\uDDEA\uDDEC\uDDED\uDDF7-\uDDFA])|\uD83C\uDDE9(?:\uD83C[\uDDEA\uDDEC\uDDEF\uDDF0\uDDF2\uDDF4\uDDFF])|\uD83C\uDDE8(?:\uD83C[\uDDE6\uDDE8\uDDE9\uDDEB-\uDDEE\uDDF0-\uDDF5\uDDF7\uDDFA-\uDDFF])|\uD83C\uDDE7(?:\uD83C[\uDDE6\uDDE7\uDDE9-\uDDEF\uDDF1-\uDDF4\uDDF6-\uDDF9\uDDFB\uDDFC\uDDFE\uDDFF])|\uD83C\uDDE6(?:\uD83C[\uDDE8-\uDDEC\uDDEE\uDDF1\uDDF2\uDDF4\uDDF6-\uDDFA\uDDFC\uDDFD\uDDFF])|[#\*0-9]\uFE0F\u20E3|\u2764\uFE0F|(?:\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC70\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD35\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD4\uDDD6-\uDDDD])(?:\uD83C[\uDFFB-\uDFFF])|(?:\u26F9|\uD83C[\uDFCB\uDFCC]|\uD83D\uDD75)(?:\uFE0F|\uD83C[\uDFFB-\uDFFF])|\uD83C\uDFF4|(?:[\u270A\u270B]|\uD83C[\uDF85\uDFC2\uDFC7]|\uD83D[\uDC42\uDC43\uDC46-\uDC50\uDC66\uDC67\uDC6B-\uDC6D\uDC72\uDC74-\uDC76\uDC78\uDC7C\uDC83\uDC85\uDC8F\uDC91\uDCAA\uDD7A\uDD95\uDD96\uDE4C\uDE4F\uDEC0\uDECC]|\uD83E[\uDD0C\uDD0F\uDD18-\uDD1C\uDD1E\uDD1F\uDD30-\uDD34\uDD36\uDD77\uDDB5\uDDB6\uDDBB\uDDD2\uDDD3\uDDD5])(?:\uD83C[\uDFFB-\uDFFF])|(?:[\u261D\u270C\u270D]|\uD83D[\uDD74\uDD90])(?:\uFE0F|\uD83C[\uDFFB-\uDFFF])|[\u270A\u270B]|\uD83C[\uDF85\uDFC2\uDFC7]|\uD83D[\uDC08\uDC15\uDC3B\uDC42\uDC43\uDC46-\uDC50\uDC66\uDC67\uDC6B-\uDC6D\uDC72\uDC74-\uDC76\uDC78\uDC7C\uDC83\uDC85\uDC8F\uDC91\uDCAA\uDD7A\uDD95\uDD96\uDE2E\uDE35\uDE36\uDE4C\uDE4F\uDEC0\uDECC]|\uD83E[\uDD0C\uDD0F\uDD18-\uDD1C\uDD1E\uDD1F\uDD30-\uDD34\uDD36\uDD77\uDDB5\uDDB6\uDDBB\uDDD2\uDDD3\uDDD5]|\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC70\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD35\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD4\uDDD6-\uDDDD]|\uD83D\uDC6F|\uD83E[\uDD3C\uDDDE\uDDDF]|[\u231A\u231B\u23E9-\u23EC\u23F0\u23F3\u25FD\u25FE\u2614\u2615\u2648-\u2653\u267F\u2693\u26A1\u26AA\u26AB\u26BD\u26BE\u26C4\u26C5\u26CE\u26D4\u26EA\u26F2\u26F3\u26F5\u26FA\u26FD\u2705\u2728\u274C\u274E\u2753-\u2755\u2757\u2795-\u2797\u27B0\u27BF\u2B1B\u2B1C\u2B50\u2B55]|\uD83C[\uDC04\uDCCF\uDD8E\uDD91-\uDD9A\uDE01\uDE1A\uDE2F\uDE32-\uDE36\uDE38-\uDE3A\uDE50\uDE51\uDF00-\uDF20\uDF2D-\uDF35\uDF37-\uDF7C\uDF7E-\uDF84\uDF86-\uDF93\uDFA0-\uDFC1\uDFC5\uDFC6\uDFC8\uDFC9\uDFCF-\uDFD3\uDFE0-\uDFF0\uDFF8-\uDFFF]|\uD83D[\uDC00-\uDC07\uDC09-\uDC14\uDC16-\uDC3A\uDC3C-\uDC3E\uDC40\uDC44\uDC45\uDC51-\uDC65\uDC6A\uDC79-\uDC7B\uDC7D-\uDC80\uDC84\uDC88-\uDC8E\uDC90\uDC92-\uDCA9\uDCAB-\uDCFC\uDCFF-\uDD3D\uDD4B-\uDD4E\uDD50-\uDD67\uDDA4\uDDFB-\uDE2D\uDE2F-\uDE34\uDE37-\uDE44\uDE48-\uDE4A\uDE80-\uDEA2\uDEA4-\uDEB3\uDEB7-\uDEBF\uDEC1-\uDEC5\uDED0-\uDED2\uDED5-\uDED7\uDEEB\uDEEC\uDEF4-\uDEFC\uDFE0-\uDFEB]|\uD83E[\uDD0D\uDD0E\uDD10-\uDD17\uDD1D\uDD20-\uDD25\uDD27-\uDD2F\uDD3A\uDD3F-\uDD45\uDD47-\uDD76\uDD78\uDD7A-\uDDB4\uDDB7\uDDBA\uDDBC-\uDDCB\uDDD0\uDDE0-\uDDFF\uDE70-\uDE74\uDE78-\uDE7A\uDE80-\uDE86\uDE90-\uDEA8\uDEB0-\uDEB6\uDEC0-\uDEC2\uDED0-\uDED6]|(?:[\u231A\u231B\u23E9-\u23EC\u23F0\u23F3\u25FD\u25FE\u2614\u2615\u2648-\u2653\u267F\u2693\u26A1\u26AA\u26AB\u26BD\u26BE\u26C4\u26C5\u26CE\u26D4\u26EA\u26F2\u26F3\u26F5\u26FA\u26FD\u2705\u270A\u270B\u2728\u274C\u274E\u2753-\u2755\u2757\u2795-\u2797\u27B0\u27BF\u2B1B\u2B1C\u2B50\u2B55]|\uD83C[\uDC04\uDCCF\uDD8E\uDD91-\uDD9A\uDDE6-\uDDFF\uDE01\uDE1A\uDE2F\uDE32-\uDE36\uDE38-\uDE3A\uDE50\uDE51\uDF00-\uDF20\uDF2D-\uDF35\uDF37-\uDF7C\uDF7E-\uDF93\uDFA0-\uDFCA\uDFCF-\uDFD3\uDFE0-\uDFF0\uDFF4\uDFF8-\uDFFF]|\uD83D[\uDC00-\uDC3E\uDC40\uDC42-\uDCFC\uDCFF-\uDD3D\uDD4B-\uDD4E\uDD50-\uDD67\uDD7A\uDD95\uDD96\uDDA4\uDDFB-\uDE4F\uDE80-\uDEC5\uDECC\uDED0-\uDED2\uDED5-\uDED7\uDEEB\uDEEC\uDEF4-\uDEFC\uDFE0-\uDFEB]|\uD83E[\uDD0C-\uDD3A\uDD3C-\uDD45\uDD47-\uDD78\uDD7A-\uDDCB\uDDCD-\uDDFF\uDE70-\uDE74\uDE78-\uDE7A\uDE80-\uDE86\uDE90-\uDEA8\uDEB0-\uDEB6\uDEC0-\uDEC2\uDED0-\uDED6])|(?:[#\*0-9\xA9\xAE\u203C\u2049\u2122\u2139\u2194-\u2199\u21A9\u21AA\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA\u24C2\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE\u2600-\u2604\u260E\u2611\u2614\u2615\u2618\u261D\u2620\u2622\u2623\u2626\u262A\u262E\u262F\u2638-\u263A\u2640\u2642\u2648-\u2653\u265F\u2660\u2663\u2665\u2666\u2668\u267B\u267E\u267F\u2692-\u2697\u2699\u269B\u269C\u26A0\u26A1\u26A7\u26AA\u26AB\u26B0\u26B1\u26BD\u26BE\u26C4\u26C5\u26C8\u26CE\u26CF\u26D1\u26D3\u26D4\u26E9\u26EA\u26F0-\u26F5\u26F7-\u26FA\u26FD\u2702\u2705\u2708-\u270D\u270F\u2712\u2714\u2716\u271D\u2721\u2728\u2733\u2734\u2744\u2747\u274C\u274E\u2753-\u2755\u2757\u2763\u2764\u2795-\u2797\u27A1\u27B0\u27BF\u2934\u2935\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D\u3297\u3299]|\uD83C[\uDC04\uDCCF\uDD70\uDD71\uDD7E\uDD7F\uDD8E\uDD91-\uDD9A\uDDE6-\uDDFF\uDE01\uDE02\uDE1A\uDE2F\uDE32-\uDE3A\uDE50\uDE51\uDF00-\uDF21\uDF24-\uDF93\uDF96\uDF97\uDF99-\uDF9B\uDF9E-\uDFF0\uDFF3-\uDFF5\uDFF7-\uDFFF]|\uD83D[\uDC00-\uDCFD\uDCFF-\uDD3D\uDD49-\uDD4E\uDD50-\uDD67\uDD6F\uDD70\uDD73-\uDD7A\uDD87\uDD8A-\uDD8D\uDD90\uDD95\uDD96\uDDA4\uDDA5\uDDA8\uDDB1\uDDB2\uDDBC\uDDC2-\uDDC4\uDDD1-\uDDD3\uDDDC-\uDDDE\uDDE1\uDDE3\uDDE8\uDDEF\uDDF3\uDDFA-\uDE4F\uDE80-\uDEC5\uDECB-\uDED2\uDED5-\uDED7\uDEE0-\uDEE5\uDEE9\uDEEB\uDEEC\uDEF0\uDEF3-\uDEFC\uDFE0-\uDFEB]|\uD83E[\uDD0C-\uDD3A\uDD3C-\uDD45\uDD47-\uDD78\uDD7A-\uDDCB\uDDCD-\uDDFF\uDE70-\uDE74\uDE78-\uDE7A\uDE80-\uDE86\uDE90-\uDEA8\uDEB0-\uDEB6\uDEC0-\uDEC2\uDED0-\uDED6])\uFE0F|(?:[\u261D\u26F9\u270A-\u270D]|\uD83C[\uDF85\uDFC2-\uDFC4\uDFC7\uDFCA-\uDFCC]|\uD83D[\uDC42\uDC43\uDC46-\uDC50\uDC66-\uDC78\uDC7C\uDC81-\uDC83\uDC85-\uDC87\uDC8F\uDC91\uDCAA\uDD74\uDD75\uDD7A\uDD90\uDD95\uDD96\uDE45-\uDE47\uDE4B-\uDE4F\uDEA3\uDEB4-\uDEB6\uDEC0\uDECC]|\uD83E[\uDD0C\uDD0F\uDD18-\uDD1F\uDD26\uDD30-\uDD39\uDD3C-\uDD3E\uDD77\uDDB5\uDDB6\uDDB8\uDDB9\uDDBB\uDDCD-\uDDCF\uDDD1-\uDDDD])/g;
    };
  }
});

// ../../node_modules/.pnpm/cli-boxes@3.0.0/node_modules/cli-boxes/boxes.json
var require_boxes = __commonJS({
  "../../node_modules/.pnpm/cli-boxes@3.0.0/node_modules/cli-boxes/boxes.json"(exports, module2) {
    module2.exports = {
      single: {
        topLeft: "\u250C",
        top: "\u2500",
        topRight: "\u2510",
        right: "\u2502",
        bottomRight: "\u2518",
        bottom: "\u2500",
        bottomLeft: "\u2514",
        left: "\u2502"
      },
      double: {
        topLeft: "\u2554",
        top: "\u2550",
        topRight: "\u2557",
        right: "\u2551",
        bottomRight: "\u255D",
        bottom: "\u2550",
        bottomLeft: "\u255A",
        left: "\u2551"
      },
      round: {
        topLeft: "\u256D",
        top: "\u2500",
        topRight: "\u256E",
        right: "\u2502",
        bottomRight: "\u256F",
        bottom: "\u2500",
        bottomLeft: "\u2570",
        left: "\u2502"
      },
      bold: {
        topLeft: "\u250F",
        top: "\u2501",
        topRight: "\u2513",
        right: "\u2503",
        bottomRight: "\u251B",
        bottom: "\u2501",
        bottomLeft: "\u2517",
        left: "\u2503"
      },
      singleDouble: {
        topLeft: "\u2553",
        top: "\u2500",
        topRight: "\u2556",
        right: "\u2551",
        bottomRight: "\u255C",
        bottom: "\u2500",
        bottomLeft: "\u2559",
        left: "\u2551"
      },
      doubleSingle: {
        topLeft: "\u2552",
        top: "\u2550",
        topRight: "\u2555",
        right: "\u2502",
        bottomRight: "\u255B",
        bottom: "\u2550",
        bottomLeft: "\u2558",
        left: "\u2502"
      },
      classic: {
        topLeft: "+",
        top: "-",
        topRight: "+",
        right: "|",
        bottomRight: "+",
        bottom: "-",
        bottomLeft: "+",
        left: "|"
      },
      arrow: {
        topLeft: "\u2198",
        top: "\u2193",
        topRight: "\u2199",
        right: "\u2190",
        bottomRight: "\u2196",
        bottom: "\u2191",
        bottomLeft: "\u2197",
        left: "\u2192"
      }
    };
  }
});

// ../../node_modules/.pnpm/cli-boxes@3.0.0/node_modules/cli-boxes/index.js
var require_cli_boxes = __commonJS({
  "../../node_modules/.pnpm/cli-boxes@3.0.0/node_modules/cli-boxes/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var cliBoxes2 = require_boxes();
    module2.exports = cliBoxes2;
    module2.exports.default = cliBoxes2;
  }
});

// ../../node_modules/.pnpm/ansi-regex@5.0.1/node_modules/ansi-regex/index.js
var require_ansi_regex = __commonJS({
  "../../node_modules/.pnpm/ansi-regex@5.0.1/node_modules/ansi-regex/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = ({ onlyFirst = false } = {}) => {
      const pattern = [
        "[\\u001B\\u009B][[\\]()#;?]*(?:(?:(?:(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]+)*|[a-zA-Z\\d]+(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]*)*)?\\u0007)",
        "(?:(?:\\d{1,4}(?:;\\d{0,4})*)?[\\dA-PR-TZcf-ntqry=><~]))"
      ].join("|");
      return new RegExp(pattern, onlyFirst ? void 0 : "g");
    };
  }
});

// ../../node_modules/.pnpm/strip-ansi@6.0.1/node_modules/strip-ansi/index.js
var require_strip_ansi = __commonJS({
  "../../node_modules/.pnpm/strip-ansi@6.0.1/node_modules/strip-ansi/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var ansiRegex2 = require_ansi_regex();
    module2.exports = (string) => typeof string === "string" ? string.replace(ansiRegex2(), "") : string;
  }
});

// ../../node_modules/.pnpm/is-fullwidth-code-point@3.0.0/node_modules/is-fullwidth-code-point/index.js
var require_is_fullwidth_code_point = __commonJS({
  "../../node_modules/.pnpm/is-fullwidth-code-point@3.0.0/node_modules/is-fullwidth-code-point/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var isFullwidthCodePoint = (codePoint) => {
      if (Number.isNaN(codePoint)) {
        return false;
      }
      if (codePoint >= 4352 && (codePoint <= 4447 || codePoint === 9001 || codePoint === 9002 || 11904 <= codePoint && codePoint <= 12871 && codePoint !== 12351 || 12880 <= codePoint && codePoint <= 19903 || 19968 <= codePoint && codePoint <= 42182 || 43360 <= codePoint && codePoint <= 43388 || 44032 <= codePoint && codePoint <= 55203 || 63744 <= codePoint && codePoint <= 64255 || 65040 <= codePoint && codePoint <= 65049 || 65072 <= codePoint && codePoint <= 65131 || 65281 <= codePoint && codePoint <= 65376 || 65504 <= codePoint && codePoint <= 65510 || 110592 <= codePoint && codePoint <= 110593 || 127488 <= codePoint && codePoint <= 127569 || 131072 <= codePoint && codePoint <= 262141)) {
        return true;
      }
      return false;
    };
    module2.exports = isFullwidthCodePoint;
    module2.exports.default = isFullwidthCodePoint;
  }
});

// ../../node_modules/.pnpm/emoji-regex@8.0.0/node_modules/emoji-regex/index.js
var require_emoji_regex2 = __commonJS({
  "../../node_modules/.pnpm/emoji-regex@8.0.0/node_modules/emoji-regex/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = function() {
      return /\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62(?:\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67|\uDB40\uDC73\uDB40\uDC63\uDB40\uDC74|\uDB40\uDC77\uDB40\uDC6C\uDB40\uDC73)\uDB40\uDC7F|\uD83D\uDC68(?:\uD83C\uDFFC\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68\uD83C\uDFFB|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFF\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFE])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFE\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFD])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFD\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB\uDFFC])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\u200D(?:\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D)?\uD83D\uDC68|(?:\uD83D[\uDC68\uDC69])\u200D(?:\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67]))|\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67])|(?:\uD83D[\uDC68\uDC69])\u200D(?:\uD83D[\uDC66\uDC67])|[\u2695\u2696\u2708]\uFE0F|\uD83D[\uDC66\uDC67]|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|(?:\uD83C\uDFFB\u200D[\u2695\u2696\u2708]|\uD83C\uDFFF\u200D[\u2695\u2696\u2708]|\uD83C\uDFFE\u200D[\u2695\u2696\u2708]|\uD83C\uDFFD\u200D[\u2695\u2696\u2708]|\uD83C\uDFFC\u200D[\u2695\u2696\u2708])\uFE0F|\uD83C\uDFFB\u200D(?:\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C[\uDFFB-\uDFFF])|(?:\uD83E\uDDD1\uD83C\uDFFB\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFC\u200D\uD83E\uDD1D\u200D\uD83D\uDC69)\uD83C\uDFFB|\uD83E\uDDD1(?:\uD83C\uDFFF\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1(?:\uD83C[\uDFFB-\uDFFF])|\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1)|(?:\uD83E\uDDD1\uD83C\uDFFE\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFF\u200D\uD83E\uDD1D\u200D(?:\uD83D[\uDC68\uDC69]))(?:\uD83C[\uDFFB-\uDFFE])|(?:\uD83E\uDDD1\uD83C\uDFFC\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFD\u200D\uD83E\uDD1D\u200D\uD83D\uDC69)(?:\uD83C[\uDFFB\uDFFC])|\uD83D\uDC69(?:\uD83C\uDFFE\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB-\uDFFD\uDFFF])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFC\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB\uDFFD-\uDFFF])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFB\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFC-\uDFFF])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFD\u200D(?:\uD83E\uDD1D\u200D\uD83D\uDC68(?:\uD83C[\uDFFB\uDFFC\uDFFE\uDFFF])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\u200D(?:\u2764\uFE0F\u200D(?:\uD83D\uDC8B\u200D(?:\uD83D[\uDC68\uDC69])|\uD83D[\uDC68\uDC69])|\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD])|\uD83C\uDFFF\u200D(?:\uD83C[\uDF3E\uDF73\uDF93\uDFA4\uDFA8\uDFEB\uDFED]|\uD83D[\uDCBB\uDCBC\uDD27\uDD2C\uDE80\uDE92]|\uD83E[\uDDAF-\uDDB3\uDDBC\uDDBD]))|\uD83D\uDC69\u200D\uD83D\uDC69\u200D(?:\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67]))|(?:\uD83E\uDDD1\uD83C\uDFFD\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1|\uD83D\uDC69\uD83C\uDFFE\u200D\uD83E\uDD1D\u200D\uD83D\uDC69)(?:\uD83C[\uDFFB-\uDFFD])|\uD83D\uDC69\u200D\uD83D\uDC66\u200D\uD83D\uDC66|\uD83D\uDC69\u200D\uD83D\uDC69\u200D(?:\uD83D[\uDC66\uDC67])|(?:\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8|\uD83D\uDC69(?:\uD83C\uDFFF\u200D[\u2695\u2696\u2708]|\uD83C\uDFFE\u200D[\u2695\u2696\u2708]|\uD83C\uDFFC\u200D[\u2695\u2696\u2708]|\uD83C\uDFFB\u200D[\u2695\u2696\u2708]|\uD83C\uDFFD\u200D[\u2695\u2696\u2708]|\u200D[\u2695\u2696\u2708])|(?:(?:\u26F9|\uD83C[\uDFCB\uDFCC]|\uD83D\uDD75)\uFE0F|\uD83D\uDC6F|\uD83E[\uDD3C\uDDDE\uDDDF])\u200D[\u2640\u2642]|(?:\u26F9|\uD83C[\uDFCB\uDFCC]|\uD83D\uDD75)(?:\uD83C[\uDFFB-\uDFFF])\u200D[\u2640\u2642]|(?:\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD6-\uDDDD])(?:(?:\uD83C[\uDFFB-\uDFFF])\u200D[\u2640\u2642]|\u200D[\u2640\u2642])|\uD83C\uDFF4\u200D\u2620)\uFE0F|\uD83D\uDC69\u200D\uD83D\uDC67\u200D(?:\uD83D[\uDC66\uDC67])|\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08|\uD83D\uDC15\u200D\uD83E\uDDBA|\uD83D\uDC69\u200D\uD83D\uDC66|\uD83D\uDC69\u200D\uD83D\uDC67|\uD83C\uDDFD\uD83C\uDDF0|\uD83C\uDDF4\uD83C\uDDF2|\uD83C\uDDF6\uD83C\uDDE6|[#\*0-9]\uFE0F\u20E3|\uD83C\uDDE7(?:\uD83C[\uDDE6\uDDE7\uDDE9-\uDDEF\uDDF1-\uDDF4\uDDF6-\uDDF9\uDDFB\uDDFC\uDDFE\uDDFF])|\uD83C\uDDF9(?:\uD83C[\uDDE6\uDDE8\uDDE9\uDDEB-\uDDED\uDDEF-\uDDF4\uDDF7\uDDF9\uDDFB\uDDFC\uDDFF])|\uD83C\uDDEA(?:\uD83C[\uDDE6\uDDE8\uDDEA\uDDEC\uDDED\uDDF7-\uDDFA])|\uD83E\uDDD1(?:\uD83C[\uDFFB-\uDFFF])|\uD83C\uDDF7(?:\uD83C[\uDDEA\uDDF4\uDDF8\uDDFA\uDDFC])|\uD83D\uDC69(?:\uD83C[\uDFFB-\uDFFF])|\uD83C\uDDF2(?:\uD83C[\uDDE6\uDDE8-\uDDED\uDDF0-\uDDFF])|\uD83C\uDDE6(?:\uD83C[\uDDE8-\uDDEC\uDDEE\uDDF1\uDDF2\uDDF4\uDDF6-\uDDFA\uDDFC\uDDFD\uDDFF])|\uD83C\uDDF0(?:\uD83C[\uDDEA\uDDEC-\uDDEE\uDDF2\uDDF3\uDDF5\uDDF7\uDDFC\uDDFE\uDDFF])|\uD83C\uDDED(?:\uD83C[\uDDF0\uDDF2\uDDF3\uDDF7\uDDF9\uDDFA])|\uD83C\uDDE9(?:\uD83C[\uDDEA\uDDEC\uDDEF\uDDF0\uDDF2\uDDF4\uDDFF])|\uD83C\uDDFE(?:\uD83C[\uDDEA\uDDF9])|\uD83C\uDDEC(?:\uD83C[\uDDE6\uDDE7\uDDE9-\uDDEE\uDDF1-\uDDF3\uDDF5-\uDDFA\uDDFC\uDDFE])|\uD83C\uDDF8(?:\uD83C[\uDDE6-\uDDEA\uDDEC-\uDDF4\uDDF7-\uDDF9\uDDFB\uDDFD-\uDDFF])|\uD83C\uDDEB(?:\uD83C[\uDDEE-\uDDF0\uDDF2\uDDF4\uDDF7])|\uD83C\uDDF5(?:\uD83C[\uDDE6\uDDEA-\uDDED\uDDF0-\uDDF3\uDDF7-\uDDF9\uDDFC\uDDFE])|\uD83C\uDDFB(?:\uD83C[\uDDE6\uDDE8\uDDEA\uDDEC\uDDEE\uDDF3\uDDFA])|\uD83C\uDDF3(?:\uD83C[\uDDE6\uDDE8\uDDEA-\uDDEC\uDDEE\uDDF1\uDDF4\uDDF5\uDDF7\uDDFA\uDDFF])|\uD83C\uDDE8(?:\uD83C[\uDDE6\uDDE8\uDDE9\uDDEB-\uDDEE\uDDF0-\uDDF5\uDDF7\uDDFA-\uDDFF])|\uD83C\uDDF1(?:\uD83C[\uDDE6-\uDDE8\uDDEE\uDDF0\uDDF7-\uDDFB\uDDFE])|\uD83C\uDDFF(?:\uD83C[\uDDE6\uDDF2\uDDFC])|\uD83C\uDDFC(?:\uD83C[\uDDEB\uDDF8])|\uD83C\uDDFA(?:\uD83C[\uDDE6\uDDEC\uDDF2\uDDF3\uDDF8\uDDFE\uDDFF])|\uD83C\uDDEE(?:\uD83C[\uDDE8-\uDDEA\uDDF1-\uDDF4\uDDF6-\uDDF9])|\uD83C\uDDEF(?:\uD83C[\uDDEA\uDDF2\uDDF4\uDDF5])|(?:\uD83C[\uDFC3\uDFC4\uDFCA]|\uD83D[\uDC6E\uDC71\uDC73\uDC77\uDC81\uDC82\uDC86\uDC87\uDE45-\uDE47\uDE4B\uDE4D\uDE4E\uDEA3\uDEB4-\uDEB6]|\uD83E[\uDD26\uDD37-\uDD39\uDD3D\uDD3E\uDDB8\uDDB9\uDDCD-\uDDCF\uDDD6-\uDDDD])(?:\uD83C[\uDFFB-\uDFFF])|(?:\u26F9|\uD83C[\uDFCB\uDFCC]|\uD83D\uDD75)(?:\uD83C[\uDFFB-\uDFFF])|(?:[\u261D\u270A-\u270D]|\uD83C[\uDF85\uDFC2\uDFC7]|\uD83D[\uDC42\uDC43\uDC46-\uDC50\uDC66\uDC67\uDC6B-\uDC6D\uDC70\uDC72\uDC74-\uDC76\uDC78\uDC7C\uDC83\uDC85\uDCAA\uDD74\uDD7A\uDD90\uDD95\uDD96\uDE4C\uDE4F\uDEC0\uDECC]|\uD83E[\uDD0F\uDD18-\uDD1C\uDD1E\uDD1F\uDD30-\uDD36\uDDB5\uDDB6\uDDBB\uDDD2-\uDDD5])(?:\uD83C[\uDFFB-\uDFFF])|(?:[\u231A\u231B\u23E9-\u23EC\u23F0\u23F3\u25FD\u25FE\u2614\u2615\u2648-\u2653\u267F\u2693\u26A1\u26AA\u26AB\u26BD\u26BE\u26C4\u26C5\u26CE\u26D4\u26EA\u26F2\u26F3\u26F5\u26FA\u26FD\u2705\u270A\u270B\u2728\u274C\u274E\u2753-\u2755\u2757\u2795-\u2797\u27B0\u27BF\u2B1B\u2B1C\u2B50\u2B55]|\uD83C[\uDC04\uDCCF\uDD8E\uDD91-\uDD9A\uDDE6-\uDDFF\uDE01\uDE1A\uDE2F\uDE32-\uDE36\uDE38-\uDE3A\uDE50\uDE51\uDF00-\uDF20\uDF2D-\uDF35\uDF37-\uDF7C\uDF7E-\uDF93\uDFA0-\uDFCA\uDFCF-\uDFD3\uDFE0-\uDFF0\uDFF4\uDFF8-\uDFFF]|\uD83D[\uDC00-\uDC3E\uDC40\uDC42-\uDCFC\uDCFF-\uDD3D\uDD4B-\uDD4E\uDD50-\uDD67\uDD7A\uDD95\uDD96\uDDA4\uDDFB-\uDE4F\uDE80-\uDEC5\uDECC\uDED0-\uDED2\uDED5\uDEEB\uDEEC\uDEF4-\uDEFA\uDFE0-\uDFEB]|\uD83E[\uDD0D-\uDD3A\uDD3C-\uDD45\uDD47-\uDD71\uDD73-\uDD76\uDD7A-\uDDA2\uDDA5-\uDDAA\uDDAE-\uDDCA\uDDCD-\uDDFF\uDE70-\uDE73\uDE78-\uDE7A\uDE80-\uDE82\uDE90-\uDE95])|(?:[#\*0-9\xA9\xAE\u203C\u2049\u2122\u2139\u2194-\u2199\u21A9\u21AA\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA\u24C2\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE\u2600-\u2604\u260E\u2611\u2614\u2615\u2618\u261D\u2620\u2622\u2623\u2626\u262A\u262E\u262F\u2638-\u263A\u2640\u2642\u2648-\u2653\u265F\u2660\u2663\u2665\u2666\u2668\u267B\u267E\u267F\u2692-\u2697\u2699\u269B\u269C\u26A0\u26A1\u26AA\u26AB\u26B0\u26B1\u26BD\u26BE\u26C4\u26C5\u26C8\u26CE\u26CF\u26D1\u26D3\u26D4\u26E9\u26EA\u26F0-\u26F5\u26F7-\u26FA\u26FD\u2702\u2705\u2708-\u270D\u270F\u2712\u2714\u2716\u271D\u2721\u2728\u2733\u2734\u2744\u2747\u274C\u274E\u2753-\u2755\u2757\u2763\u2764\u2795-\u2797\u27A1\u27B0\u27BF\u2934\u2935\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55\u3030\u303D\u3297\u3299]|\uD83C[\uDC04\uDCCF\uDD70\uDD71\uDD7E\uDD7F\uDD8E\uDD91-\uDD9A\uDDE6-\uDDFF\uDE01\uDE02\uDE1A\uDE2F\uDE32-\uDE3A\uDE50\uDE51\uDF00-\uDF21\uDF24-\uDF93\uDF96\uDF97\uDF99-\uDF9B\uDF9E-\uDFF0\uDFF3-\uDFF5\uDFF7-\uDFFF]|\uD83D[\uDC00-\uDCFD\uDCFF-\uDD3D\uDD49-\uDD4E\uDD50-\uDD67\uDD6F\uDD70\uDD73-\uDD7A\uDD87\uDD8A-\uDD8D\uDD90\uDD95\uDD96\uDDA4\uDDA5\uDDA8\uDDB1\uDDB2\uDDBC\uDDC2-\uDDC4\uDDD1-\uDDD3\uDDDC-\uDDDE\uDDE1\uDDE3\uDDE8\uDDEF\uDDF3\uDDFA-\uDE4F\uDE80-\uDEC5\uDECB-\uDED2\uDED5\uDEE0-\uDEE5\uDEE9\uDEEB\uDEEC\uDEF0\uDEF3-\uDEFA\uDFE0-\uDFEB]|\uD83E[\uDD0D-\uDD3A\uDD3C-\uDD45\uDD47-\uDD71\uDD73-\uDD76\uDD7A-\uDDA2\uDDA5-\uDDAA\uDDAE-\uDDCA\uDDCD-\uDDFF\uDE70-\uDE73\uDE78-\uDE7A\uDE80-\uDE82\uDE90-\uDE95])\uFE0F|(?:[\u261D\u26F9\u270A-\u270D]|\uD83C[\uDF85\uDFC2-\uDFC4\uDFC7\uDFCA-\uDFCC]|\uD83D[\uDC42\uDC43\uDC46-\uDC50\uDC66-\uDC78\uDC7C\uDC81-\uDC83\uDC85-\uDC87\uDC8F\uDC91\uDCAA\uDD74\uDD75\uDD7A\uDD90\uDD95\uDD96\uDE45-\uDE47\uDE4B-\uDE4F\uDEA3\uDEB4-\uDEB6\uDEC0\uDECC]|\uD83E[\uDD0F\uDD18-\uDD1F\uDD26\uDD30-\uDD39\uDD3C-\uDD3E\uDDB5\uDDB6\uDDB8\uDDB9\uDDBB\uDDCD-\uDDCF\uDDD1-\uDDDD])/g;
    };
  }
});

// ../../node_modules/.pnpm/string-width@4.2.3/node_modules/string-width/index.js
var require_string_width = __commonJS({
  "../../node_modules/.pnpm/string-width@4.2.3/node_modules/string-width/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var stripAnsi2 = require_strip_ansi();
    var isFullwidthCodePoint = require_is_fullwidth_code_point();
    var emojiRegex2 = require_emoji_regex2();
    var stringWidth2 = (string) => {
      if (typeof string !== "string" || string.length === 0) {
        return 0;
      }
      string = stripAnsi2(string);
      if (string.length === 0) {
        return 0;
      }
      string = string.replace(emojiRegex2(), "  ");
      let width = 0;
      for (let i = 0; i < string.length; i++) {
        const code = string.codePointAt(i);
        if (code <= 31 || code >= 127 && code <= 159) {
          continue;
        }
        if (code >= 768 && code <= 879) {
          continue;
        }
        if (code > 65535) {
          i++;
        }
        width += isFullwidthCodePoint(code) ? 2 : 1;
      }
      return width;
    };
    module2.exports = stringWidth2;
    module2.exports.default = stringWidth2;
  }
});

// ../../node_modules/.pnpm/ansi-align@3.0.1/node_modules/ansi-align/index.js
var require_ansi_align = __commonJS({
  "../../node_modules/.pnpm/ansi-align@3.0.1/node_modules/ansi-align/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    var stringWidth2 = require_string_width();
    function ansiAlign2(text, opts) {
      if (!text)
        return text;
      opts = opts || {};
      const align = opts.align || "center";
      if (align === "left")
        return text;
      const split = opts.split || "\n";
      const pad = opts.pad || " ";
      const widthDiffFn = align !== "right" ? halfDiff : fullDiff;
      let returnString = false;
      if (!Array.isArray(text)) {
        returnString = true;
        text = String(text).split(split);
      }
      let width;
      let maxWidth = 0;
      text = text.map(function(str) {
        str = String(str);
        width = stringWidth2(str);
        maxWidth = Math.max(width, maxWidth);
        return {
          str,
          width
        };
      }).map(function(obj) {
        return new Array(widthDiffFn(maxWidth, obj.width) + 1).join(pad) + obj.str;
      });
      return returnString ? text.join(split) : text;
    }
    ansiAlign2.left = function left(text) {
      return ansiAlign2(text, { align: "left" });
    };
    ansiAlign2.center = function center(text) {
      return ansiAlign2(text, { align: "center" });
    };
    ansiAlign2.right = function right(text) {
      return ansiAlign2(text, { align: "right" });
    };
    module2.exports = ansiAlign2;
    function halfDiff(maxWidth, curWidth) {
      return Math.floor((maxWidth - curWidth) / 2);
    }
    function fullDiff(maxWidth, curWidth) {
      return maxWidth - curWidth;
    }
  }
});

// ../../node_modules/.pnpm/ci-info@3.3.2/node_modules/ci-info/vendors.json
var require_vendors = __commonJS({
  "../../node_modules/.pnpm/ci-info@3.3.2/node_modules/ci-info/vendors.json"(exports, module2) {
    module2.exports = [
      {
        name: "AppVeyor",
        constant: "APPVEYOR",
        env: "APPVEYOR",
        pr: "APPVEYOR_PULL_REQUEST_NUMBER"
      },
      {
        name: "Azure Pipelines",
        constant: "AZURE_PIPELINES",
        env: "SYSTEM_TEAMFOUNDATIONCOLLECTIONURI",
        pr: "SYSTEM_PULLREQUEST_PULLREQUESTID"
      },
      {
        name: "Appcircle",
        constant: "APPCIRCLE",
        env: "AC_APPCIRCLE"
      },
      {
        name: "Bamboo",
        constant: "BAMBOO",
        env: "bamboo_planKey"
      },
      {
        name: "Bitbucket Pipelines",
        constant: "BITBUCKET",
        env: "BITBUCKET_COMMIT",
        pr: "BITBUCKET_PR_ID"
      },
      {
        name: "Bitrise",
        constant: "BITRISE",
        env: "BITRISE_IO",
        pr: "BITRISE_PULL_REQUEST"
      },
      {
        name: "Buddy",
        constant: "BUDDY",
        env: "BUDDY_WORKSPACE_ID",
        pr: "BUDDY_EXECUTION_PULL_REQUEST_ID"
      },
      {
        name: "Buildkite",
        constant: "BUILDKITE",
        env: "BUILDKITE",
        pr: { env: "BUILDKITE_PULL_REQUEST", ne: "false" }
      },
      {
        name: "CircleCI",
        constant: "CIRCLE",
        env: "CIRCLECI",
        pr: "CIRCLE_PULL_REQUEST"
      },
      {
        name: "Cirrus CI",
        constant: "CIRRUS",
        env: "CIRRUS_CI",
        pr: "CIRRUS_PR"
      },
      {
        name: "AWS CodeBuild",
        constant: "CODEBUILD",
        env: "CODEBUILD_BUILD_ARN"
      },
      {
        name: "Codefresh",
        constant: "CODEFRESH",
        env: "CF_BUILD_ID",
        pr: { any: ["CF_PULL_REQUEST_NUMBER", "CF_PULL_REQUEST_ID"] }
      },
      {
        name: "Codeship",
        constant: "CODESHIP",
        env: { CI_NAME: "codeship" }
      },
      {
        name: "Drone",
        constant: "DRONE",
        env: "DRONE",
        pr: { DRONE_BUILD_EVENT: "pull_request" }
      },
      {
        name: "dsari",
        constant: "DSARI",
        env: "DSARI"
      },
      {
        name: "Expo Application Services",
        constant: "EAS",
        env: "EAS_BUILD"
      },
      {
        name: "GitHub Actions",
        constant: "GITHUB_ACTIONS",
        env: "GITHUB_ACTIONS",
        pr: { GITHUB_EVENT_NAME: "pull_request" }
      },
      {
        name: "GitLab CI",
        constant: "GITLAB",
        env: "GITLAB_CI",
        pr: "CI_MERGE_REQUEST_ID"
      },
      {
        name: "GoCD",
        constant: "GOCD",
        env: "GO_PIPELINE_LABEL"
      },
      {
        name: "LayerCI",
        constant: "LAYERCI",
        env: "LAYERCI",
        pr: "LAYERCI_PULL_REQUEST"
      },
      {
        name: "Hudson",
        constant: "HUDSON",
        env: "HUDSON_URL"
      },
      {
        name: "Jenkins",
        constant: "JENKINS",
        env: ["JENKINS_URL", "BUILD_ID"],
        pr: { any: ["ghprbPullId", "CHANGE_ID"] }
      },
      {
        name: "Magnum CI",
        constant: "MAGNUM",
        env: "MAGNUM"
      },
      {
        name: "Netlify CI",
        constant: "NETLIFY",
        env: "NETLIFY",
        pr: { env: "PULL_REQUEST", ne: "false" }
      },
      {
        name: "Nevercode",
        constant: "NEVERCODE",
        env: "NEVERCODE",
        pr: { env: "NEVERCODE_PULL_REQUEST", ne: "false" }
      },
      {
        name: "Render",
        constant: "RENDER",
        env: "RENDER",
        pr: { IS_PULL_REQUEST: "true" }
      },
      {
        name: "Sail CI",
        constant: "SAIL",
        env: "SAILCI",
        pr: "SAIL_PULL_REQUEST_NUMBER"
      },
      {
        name: "Semaphore",
        constant: "SEMAPHORE",
        env: "SEMAPHORE",
        pr: "PULL_REQUEST_NUMBER"
      },
      {
        name: "Screwdriver",
        constant: "SCREWDRIVER",
        env: "SCREWDRIVER",
        pr: { env: "SD_PULL_REQUEST", ne: "false" }
      },
      {
        name: "Shippable",
        constant: "SHIPPABLE",
        env: "SHIPPABLE",
        pr: { IS_PULL_REQUEST: "true" }
      },
      {
        name: "Solano CI",
        constant: "SOLANO",
        env: "TDDIUM",
        pr: "TDDIUM_PR_ID"
      },
      {
        name: "Strider CD",
        constant: "STRIDER",
        env: "STRIDER"
      },
      {
        name: "TaskCluster",
        constant: "TASKCLUSTER",
        env: ["TASK_ID", "RUN_ID"]
      },
      {
        name: "TeamCity",
        constant: "TEAMCITY",
        env: "TEAMCITY_VERSION"
      },
      {
        name: "Travis CI",
        constant: "TRAVIS",
        env: "TRAVIS",
        pr: { env: "TRAVIS_PULL_REQUEST", ne: "false" }
      },
      {
        name: "Vercel",
        constant: "VERCEL",
        env: "NOW_BUILDER"
      },
      {
        name: "Visual Studio App Center",
        constant: "APPCENTER",
        env: "APPCENTER_BUILD_ID"
      }
    ];
  }
});

// ../../node_modules/.pnpm/ci-info@3.3.2/node_modules/ci-info/index.js
var require_ci_info = __commonJS({
  "../../node_modules/.pnpm/ci-info@3.3.2/node_modules/ci-info/index.js"(exports) {
    "use strict";
    init_cjs_shims();
    var vendors = require_vendors();
    var env3 = process.env;
    Object.defineProperty(exports, "_vendors", {
      value: vendors.map(function(v) {
        return v.constant;
      })
    });
    exports.name = null;
    exports.isPR = null;
    vendors.forEach(function(vendor) {
      const envs = Array.isArray(vendor.env) ? vendor.env : [vendor.env];
      const isCI = envs.every(function(obj) {
        return checkEnv(obj);
      });
      exports[vendor.constant] = isCI;
      if (isCI) {
        exports.name = vendor.name;
        switch (typeof vendor.pr) {
          case "string":
            exports.isPR = !!env3[vendor.pr];
            break;
          case "object":
            if ("env" in vendor.pr) {
              exports.isPR = vendor.pr.env in env3 && env3[vendor.pr.env] !== vendor.pr.ne;
            } else if ("any" in vendor.pr) {
              exports.isPR = vendor.pr.any.some(function(key) {
                return !!env3[key];
              });
            } else {
              exports.isPR = checkEnv(vendor.pr);
            }
            break;
          default:
            exports.isPR = null;
        }
      }
    });
    exports.isCI = !!(env3.CI || env3.CONTINUOUS_INTEGRATION || env3.BUILD_NUMBER || env3.RUN_ID || exports.name || false);
    function checkEnv(obj) {
      if (typeof obj === "string")
        return !!env3[obj];
      return Object.keys(obj).every(function(k) {
        return env3[k] === obj[k];
      });
    }
  }
});

// ../../node_modules/.pnpm/is-ci@3.0.1/node_modules/is-ci/index.js
var require_is_ci = __commonJS({
  "../../node_modules/.pnpm/is-ci@3.0.1/node_modules/is-ci/index.js"(exports, module2) {
    "use strict";
    init_cjs_shims();
    module2.exports = require_ci_info().isCI;
  }
});

// src/utils/init-cli.ts
var init_cli_exports = {};
__export(init_cli_exports, {
  initCLI: () => initCLI
});
module.exports = __toCommonJS(init_cli_exports);
init_cjs_shims();
var import_cli_check_node = __toESM(require("cli-check-node"));
var import_cli_welcome = __toESM(require("cli-welcome"));

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/update-notifier.js
init_cjs_shims();
var import_node_process7 = __toESM(require("process"), 1);
var import_node_child_process = require("child_process");
var import_node_url5 = require("url");
var import_node_path3 = __toESM(require("path"), 1);
var import_node_util4 = require("util");

// ../../node_modules/.pnpm/configstore@6.0.0/node_modules/configstore/index.js
init_cjs_shims();
var import_path2 = __toESM(require("path"), 1);
var import_os2 = __toESM(require("os"), 1);
var import_graceful_fs = __toESM(require_graceful_fs(), 1);

// ../../node_modules/.pnpm/xdg-basedir@5.1.0/node_modules/xdg-basedir/index.js
init_cjs_shims();
var import_os = __toESM(require("os"), 1);
var import_path = __toESM(require("path"), 1);
var homeDirectory = import_os.default.homedir();
var { env } = process;
var xdgData = env.XDG_DATA_HOME || (homeDirectory ? import_path.default.join(homeDirectory, ".local", "share") : void 0);
var xdgConfig = env.XDG_CONFIG_HOME || (homeDirectory ? import_path.default.join(homeDirectory, ".config") : void 0);
var xdgState = env.XDG_STATE_HOME || (homeDirectory ? import_path.default.join(homeDirectory, ".local", "state") : void 0);
var xdgCache = env.XDG_CACHE_HOME || (homeDirectory ? import_path.default.join(homeDirectory, ".cache") : void 0);
var xdgRuntime = env.XDG_RUNTIME_DIR || void 0;
var xdgDataDirectories = (env.XDG_DATA_DIRS || "/usr/local/share/:/usr/share/").split(":");
if (xdgData) {
  xdgDataDirectories.unshift(xdgData);
}
var xdgConfigDirectories = (env.XDG_CONFIG_DIRS || "/etc/xdg").split(":");
if (xdgConfig) {
  xdgConfigDirectories.unshift(xdgConfig);
}

// ../../node_modules/.pnpm/configstore@6.0.0/node_modules/configstore/index.js
var import_write_file_atomic = __toESM(require_write_file_atomic(), 1);
var import_dot_prop = __toESM(require_dot_prop(), 1);

// ../../node_modules/.pnpm/unique-string@3.0.0/node_modules/unique-string/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/crypto-random-string@4.0.0/node_modules/crypto-random-string/index.js
init_cjs_shims();
var import_util = require("util");
var import_crypto = __toESM(require("crypto"), 1);
var randomBytesAsync = (0, import_util.promisify)(import_crypto.default.randomBytes);
var urlSafeCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~".split("");
var numericCharacters = "0123456789".split("");
var distinguishableCharacters = "CDEHKMPRTUWXY012458".split("");
var asciiPrintableCharacters = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~".split("");
var alphanumericCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".split("");
var generateForCustomCharacters = (length, characters) => {
  const characterCount = characters.length;
  const maxValidSelector = Math.floor(65536 / characterCount) * characterCount - 1;
  const entropyLength = 2 * Math.ceil(1.1 * length);
  let string = "";
  let stringLength = 0;
  while (stringLength < length) {
    const entropy = import_crypto.default.randomBytes(entropyLength);
    let entropyPosition = 0;
    while (entropyPosition < entropyLength && stringLength < length) {
      const entropyValue = entropy.readUInt16LE(entropyPosition);
      entropyPosition += 2;
      if (entropyValue > maxValidSelector) {
        continue;
      }
      string += characters[entropyValue % characterCount];
      stringLength++;
    }
  }
  return string;
};
var generateForCustomCharactersAsync = async (length, characters) => {
  const characterCount = characters.length;
  const maxValidSelector = Math.floor(65536 / characterCount) * characterCount - 1;
  const entropyLength = 2 * Math.ceil(1.1 * length);
  let string = "";
  let stringLength = 0;
  while (stringLength < length) {
    const entropy = await randomBytesAsync(entropyLength);
    let entropyPosition = 0;
    while (entropyPosition < entropyLength && stringLength < length) {
      const entropyValue = entropy.readUInt16LE(entropyPosition);
      entropyPosition += 2;
      if (entropyValue > maxValidSelector) {
        continue;
      }
      string += characters[entropyValue % characterCount];
      stringLength++;
    }
  }
  return string;
};
var generateRandomBytes = (byteLength, type, length) => import_crypto.default.randomBytes(byteLength).toString(type).slice(0, length);
var generateRandomBytesAsync = async (byteLength, type, length) => {
  const buffer = await randomBytesAsync(byteLength);
  return buffer.toString(type).slice(0, length);
};
var allowedTypes = /* @__PURE__ */ new Set([
  void 0,
  "hex",
  "base64",
  "url-safe",
  "numeric",
  "distinguishable",
  "ascii-printable",
  "alphanumeric"
]);
var createGenerator = (generateForCustomCharacters2, generateRandomBytes2) => ({ length, type, characters }) => {
  if (!(length >= 0 && Number.isFinite(length))) {
    throw new TypeError("Expected a `length` to be a non-negative finite number");
  }
  if (type !== void 0 && characters !== void 0) {
    throw new TypeError("Expected either `type` or `characters`");
  }
  if (characters !== void 0 && typeof characters !== "string") {
    throw new TypeError("Expected `characters` to be string");
  }
  if (!allowedTypes.has(type)) {
    throw new TypeError(`Unknown type: ${type}`);
  }
  if (type === void 0 && characters === void 0) {
    type = "hex";
  }
  if (type === "hex" || type === void 0 && characters === void 0) {
    return generateRandomBytes2(Math.ceil(length * 0.5), "hex", length);
  }
  if (type === "base64") {
    return generateRandomBytes2(Math.ceil(length * 0.75), "base64", length);
  }
  if (type === "url-safe") {
    return generateForCustomCharacters2(length, urlSafeCharacters);
  }
  if (type === "numeric") {
    return generateForCustomCharacters2(length, numericCharacters);
  }
  if (type === "distinguishable") {
    return generateForCustomCharacters2(length, distinguishableCharacters);
  }
  if (type === "ascii-printable") {
    return generateForCustomCharacters2(length, asciiPrintableCharacters);
  }
  if (type === "alphanumeric") {
    return generateForCustomCharacters2(length, alphanumericCharacters);
  }
  if (characters.length === 0) {
    throw new TypeError("Expected `characters` string length to be greater than or equal to 1");
  }
  if (characters.length > 65536) {
    throw new TypeError("Expected `characters` string length to be less or equal to 65536");
  }
  return generateForCustomCharacters2(length, characters.split(""));
};
var cryptoRandomString = createGenerator(generateForCustomCharacters, generateRandomBytes);
cryptoRandomString.async = createGenerator(generateForCustomCharactersAsync, generateRandomBytesAsync);
var crypto_random_string_default = cryptoRandomString;

// ../../node_modules/.pnpm/unique-string@3.0.0/node_modules/unique-string/index.js
function uniqueString() {
  return crypto_random_string_default({ length: 32 });
}

// ../../node_modules/.pnpm/configstore@6.0.0/node_modules/configstore/index.js
var configDirectory = xdgConfig || import_path2.default.join(import_os2.default.tmpdir(), uniqueString());
var permissionError = "You don't have access to this file.";
var mkdirOptions = { mode: 448, recursive: true };
var writeFileOptions = { mode: 384 };
var Configstore = class {
  constructor(id, defaults2, options = {}) {
    const pathPrefix = options.globalConfigPath ? import_path2.default.join(id, "config.json") : import_path2.default.join("configstore", `${id}.json`);
    this._path = options.configPath || import_path2.default.join(configDirectory, pathPrefix);
    if (defaults2) {
      this.all = {
        ...defaults2,
        ...this.all
      };
    }
  }
  get all() {
    try {
      return JSON.parse(import_graceful_fs.default.readFileSync(this._path, "utf8"));
    } catch (error) {
      if (error.code === "ENOENT") {
        return {};
      }
      if (error.code === "EACCES") {
        error.message = `${error.message}
${permissionError}
`;
      }
      if (error.name === "SyntaxError") {
        import_write_file_atomic.default.sync(this._path, "", writeFileOptions);
        return {};
      }
      throw error;
    }
  }
  set all(value) {
    try {
      import_graceful_fs.default.mkdirSync(import_path2.default.dirname(this._path), mkdirOptions);
      import_write_file_atomic.default.sync(this._path, JSON.stringify(value, void 0, "	"), writeFileOptions);
    } catch (error) {
      if (error.code === "EACCES") {
        error.message = `${error.message}
${permissionError}
`;
      }
      throw error;
    }
  }
  get size() {
    return Object.keys(this.all || {}).length;
  }
  get(key) {
    return import_dot_prop.default.get(this.all, key);
  }
  set(key, value) {
    const config = this.all;
    if (arguments.length === 1) {
      for (const k of Object.keys(key)) {
        import_dot_prop.default.set(config, k, key[k]);
      }
    } else {
      import_dot_prop.default.set(config, key, value);
    }
    this.all = config;
  }
  has(key) {
    return import_dot_prop.default.has(this.all, key);
  }
  delete(key) {
    const config = this.all;
    import_dot_prop.default.delete(config, key);
    this.all = config;
  }
  clear() {
    this.all = {};
  }
  get path() {
    return this._path;
  }
};

// ../../node_modules/.pnpm/chalk@5.0.1/node_modules/chalk/source/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/chalk@5.0.1/node_modules/chalk/source/vendor/ansi-styles/index.js
init_cjs_shims();
var ANSI_BACKGROUND_OFFSET = 10;
var wrapAnsi16 = (offset = 0) => (code) => `\x1B[${code + offset}m`;
var wrapAnsi256 = (offset = 0) => (code) => `\x1B[${38 + offset};5;${code}m`;
var wrapAnsi16m = (offset = 0) => (red, green, blue) => `\x1B[${38 + offset};2;${red};${green};${blue}m`;
function assembleStyles() {
  const codes = /* @__PURE__ */ new Map();
  const styles3 = {
    modifier: {
      reset: [0, 0],
      bold: [1, 22],
      dim: [2, 22],
      italic: [3, 23],
      underline: [4, 24],
      overline: [53, 55],
      inverse: [7, 27],
      hidden: [8, 28],
      strikethrough: [9, 29]
    },
    color: {
      black: [30, 39],
      red: [31, 39],
      green: [32, 39],
      yellow: [33, 39],
      blue: [34, 39],
      magenta: [35, 39],
      cyan: [36, 39],
      white: [37, 39],
      blackBright: [90, 39],
      redBright: [91, 39],
      greenBright: [92, 39],
      yellowBright: [93, 39],
      blueBright: [94, 39],
      magentaBright: [95, 39],
      cyanBright: [96, 39],
      whiteBright: [97, 39]
    },
    bgColor: {
      bgBlack: [40, 49],
      bgRed: [41, 49],
      bgGreen: [42, 49],
      bgYellow: [43, 49],
      bgBlue: [44, 49],
      bgMagenta: [45, 49],
      bgCyan: [46, 49],
      bgWhite: [47, 49],
      bgBlackBright: [100, 49],
      bgRedBright: [101, 49],
      bgGreenBright: [102, 49],
      bgYellowBright: [103, 49],
      bgBlueBright: [104, 49],
      bgMagentaBright: [105, 49],
      bgCyanBright: [106, 49],
      bgWhiteBright: [107, 49]
    }
  };
  styles3.color.gray = styles3.color.blackBright;
  styles3.bgColor.bgGray = styles3.bgColor.bgBlackBright;
  styles3.color.grey = styles3.color.blackBright;
  styles3.bgColor.bgGrey = styles3.bgColor.bgBlackBright;
  for (const [groupName, group] of Object.entries(styles3)) {
    for (const [styleName, style] of Object.entries(group)) {
      styles3[styleName] = {
        open: `\x1B[${style[0]}m`,
        close: `\x1B[${style[1]}m`
      };
      group[styleName] = styles3[styleName];
      codes.set(style[0], style[1]);
    }
    Object.defineProperty(styles3, groupName, {
      value: group,
      enumerable: false
    });
  }
  Object.defineProperty(styles3, "codes", {
    value: codes,
    enumerable: false
  });
  styles3.color.close = "\x1B[39m";
  styles3.bgColor.close = "\x1B[49m";
  styles3.color.ansi = wrapAnsi16();
  styles3.color.ansi256 = wrapAnsi256();
  styles3.color.ansi16m = wrapAnsi16m();
  styles3.bgColor.ansi = wrapAnsi16(ANSI_BACKGROUND_OFFSET);
  styles3.bgColor.ansi256 = wrapAnsi256(ANSI_BACKGROUND_OFFSET);
  styles3.bgColor.ansi16m = wrapAnsi16m(ANSI_BACKGROUND_OFFSET);
  Object.defineProperties(styles3, {
    rgbToAnsi256: {
      value: (red, green, blue) => {
        if (red === green && green === blue) {
          if (red < 8) {
            return 16;
          }
          if (red > 248) {
            return 231;
          }
          return Math.round((red - 8) / 247 * 24) + 232;
        }
        return 16 + 36 * Math.round(red / 255 * 5) + 6 * Math.round(green / 255 * 5) + Math.round(blue / 255 * 5);
      },
      enumerable: false
    },
    hexToRgb: {
      value: (hex) => {
        const matches = /(?<colorString>[a-f\d]{6}|[a-f\d]{3})/i.exec(hex.toString(16));
        if (!matches) {
          return [0, 0, 0];
        }
        let { colorString } = matches.groups;
        if (colorString.length === 3) {
          colorString = [...colorString].map((character) => character + character).join("");
        }
        const integer = Number.parseInt(colorString, 16);
        return [
          integer >> 16 & 255,
          integer >> 8 & 255,
          integer & 255
        ];
      },
      enumerable: false
    },
    hexToAnsi256: {
      value: (hex) => styles3.rgbToAnsi256(...styles3.hexToRgb(hex)),
      enumerable: false
    },
    ansi256ToAnsi: {
      value: (code) => {
        if (code < 8) {
          return 30 + code;
        }
        if (code < 16) {
          return 90 + (code - 8);
        }
        let red;
        let green;
        let blue;
        if (code >= 232) {
          red = ((code - 232) * 10 + 8) / 255;
          green = red;
          blue = red;
        } else {
          code -= 16;
          const remainder = code % 36;
          red = Math.floor(code / 36) / 5;
          green = Math.floor(remainder / 6) / 5;
          blue = remainder % 6 / 5;
        }
        const value = Math.max(red, green, blue) * 2;
        if (value === 0) {
          return 30;
        }
        let result = 30 + (Math.round(blue) << 2 | Math.round(green) << 1 | Math.round(red));
        if (value === 2) {
          result += 60;
        }
        return result;
      },
      enumerable: false
    },
    rgbToAnsi: {
      value: (red, green, blue) => styles3.ansi256ToAnsi(styles3.rgbToAnsi256(red, green, blue)),
      enumerable: false
    },
    hexToAnsi: {
      value: (hex) => styles3.ansi256ToAnsi(styles3.hexToAnsi256(hex)),
      enumerable: false
    }
  });
  return styles3;
}
var ansiStyles = assembleStyles();
var ansi_styles_default = ansiStyles;

// ../../node_modules/.pnpm/chalk@5.0.1/node_modules/chalk/source/vendor/supports-color/index.js
init_cjs_shims();
var import_node_process = __toESM(require("process"), 1);
var import_node_os = __toESM(require("os"), 1);
var import_node_tty = __toESM(require("tty"), 1);
function hasFlag(flag, argv = import_node_process.default.argv) {
  const prefix = flag.startsWith("-") ? "" : flag.length === 1 ? "-" : "--";
  const position = argv.indexOf(prefix + flag);
  const terminatorPosition = argv.indexOf("--");
  return position !== -1 && (terminatorPosition === -1 || position < terminatorPosition);
}
var { env: env2 } = import_node_process.default;
var flagForceColor;
if (hasFlag("no-color") || hasFlag("no-colors") || hasFlag("color=false") || hasFlag("color=never")) {
  flagForceColor = 0;
} else if (hasFlag("color") || hasFlag("colors") || hasFlag("color=true") || hasFlag("color=always")) {
  flagForceColor = 1;
}
function envForceColor() {
  if ("FORCE_COLOR" in env2) {
    if (env2.FORCE_COLOR === "true") {
      return 1;
    }
    if (env2.FORCE_COLOR === "false") {
      return 0;
    }
    return env2.FORCE_COLOR.length === 0 ? 1 : Math.min(Number.parseInt(env2.FORCE_COLOR, 10), 3);
  }
}
function translateLevel(level) {
  if (level === 0) {
    return false;
  }
  return {
    level,
    hasBasic: true,
    has256: level >= 2,
    has16m: level >= 3
  };
}
function _supportsColor(haveStream, { streamIsTTY, sniffFlags = true } = {}) {
  const noFlagForceColor = envForceColor();
  if (noFlagForceColor !== void 0) {
    flagForceColor = noFlagForceColor;
  }
  const forceColor = sniffFlags ? flagForceColor : noFlagForceColor;
  if (forceColor === 0) {
    return 0;
  }
  if (sniffFlags) {
    if (hasFlag("color=16m") || hasFlag("color=full") || hasFlag("color=truecolor")) {
      return 3;
    }
    if (hasFlag("color=256")) {
      return 2;
    }
  }
  if (haveStream && !streamIsTTY && forceColor === void 0) {
    return 0;
  }
  const min = forceColor || 0;
  if (env2.TERM === "dumb") {
    return min;
  }
  if (import_node_process.default.platform === "win32") {
    const osRelease = import_node_os.default.release().split(".");
    if (Number(osRelease[0]) >= 10 && Number(osRelease[2]) >= 10586) {
      return Number(osRelease[2]) >= 14931 ? 3 : 2;
    }
    return 1;
  }
  if ("CI" in env2) {
    if (["TRAVIS", "CIRCLECI", "APPVEYOR", "GITLAB_CI", "GITHUB_ACTIONS", "BUILDKITE", "DRONE"].some((sign) => sign in env2) || env2.CI_NAME === "codeship") {
      return 1;
    }
    return min;
  }
  if ("TEAMCITY_VERSION" in env2) {
    return /^(9\.(0*[1-9]\d*)\.|\d{2,}\.)/.test(env2.TEAMCITY_VERSION) ? 1 : 0;
  }
  if ("TF_BUILD" in env2 && "AGENT_NAME" in env2) {
    return 1;
  }
  if (env2.COLORTERM === "truecolor") {
    return 3;
  }
  if ("TERM_PROGRAM" in env2) {
    const version = Number.parseInt((env2.TERM_PROGRAM_VERSION || "").split(".")[0], 10);
    switch (env2.TERM_PROGRAM) {
      case "iTerm.app":
        return version >= 3 ? 3 : 2;
      case "Apple_Terminal":
        return 2;
    }
  }
  if (/-256(color)?$/i.test(env2.TERM)) {
    return 2;
  }
  if (/^screen|^xterm|^vt100|^vt220|^rxvt|color|ansi|cygwin|linux/i.test(env2.TERM)) {
    return 1;
  }
  if ("COLORTERM" in env2) {
    return 1;
  }
  return min;
}
function createSupportsColor(stream2, options = {}) {
  const level = _supportsColor(stream2, {
    streamIsTTY: stream2 && stream2.isTTY,
    ...options
  });
  return translateLevel(level);
}
var supportsColor = {
  stdout: createSupportsColor({ isTTY: import_node_tty.default.isatty(1) }),
  stderr: createSupportsColor({ isTTY: import_node_tty.default.isatty(2) })
};
var supports_color_default = supportsColor;

// ../../node_modules/.pnpm/chalk@5.0.1/node_modules/chalk/source/utilities.js
init_cjs_shims();
function stringReplaceAll(string, substring, replacer) {
  let index = string.indexOf(substring);
  if (index === -1) {
    return string;
  }
  const substringLength = substring.length;
  let endIndex = 0;
  let returnValue = "";
  do {
    returnValue += string.substr(endIndex, index - endIndex) + substring + replacer;
    endIndex = index + substringLength;
    index = string.indexOf(substring, endIndex);
  } while (index !== -1);
  returnValue += string.slice(endIndex);
  return returnValue;
}
function stringEncaseCRLFWithFirstIndex(string, prefix, postfix, index) {
  let endIndex = 0;
  let returnValue = "";
  do {
    const gotCR = string[index - 1] === "\r";
    returnValue += string.substr(endIndex, (gotCR ? index - 1 : index) - endIndex) + prefix + (gotCR ? "\r\n" : "\n") + postfix;
    endIndex = index + 1;
    index = string.indexOf("\n", endIndex);
  } while (index !== -1);
  returnValue += string.slice(endIndex);
  return returnValue;
}

// ../../node_modules/.pnpm/chalk@5.0.1/node_modules/chalk/source/index.js
var { stdout: stdoutColor, stderr: stderrColor } = supports_color_default;
var GENERATOR = Symbol("GENERATOR");
var STYLER = Symbol("STYLER");
var IS_EMPTY = Symbol("IS_EMPTY");
var levelMapping = [
  "ansi",
  "ansi",
  "ansi256",
  "ansi16m"
];
var styles = /* @__PURE__ */ Object.create(null);
var applyOptions = (object, options = {}) => {
  if (options.level && !(Number.isInteger(options.level) && options.level >= 0 && options.level <= 3)) {
    throw new Error("The `level` option should be an integer from 0 to 3");
  }
  const colorLevel = stdoutColor ? stdoutColor.level : 0;
  object.level = options.level === void 0 ? colorLevel : options.level;
};
var chalkFactory = (options) => {
  const chalk2 = (...strings) => strings.join(" ");
  applyOptions(chalk2, options);
  Object.setPrototypeOf(chalk2, createChalk.prototype);
  return chalk2;
};
function createChalk(options) {
  return chalkFactory(options);
}
Object.setPrototypeOf(createChalk.prototype, Function.prototype);
for (const [styleName, style] of Object.entries(ansi_styles_default)) {
  styles[styleName] = {
    get() {
      const builder = createBuilder(this, createStyler(style.open, style.close, this[STYLER]), this[IS_EMPTY]);
      Object.defineProperty(this, styleName, { value: builder });
      return builder;
    }
  };
}
styles.visible = {
  get() {
    const builder = createBuilder(this, this[STYLER], true);
    Object.defineProperty(this, "visible", { value: builder });
    return builder;
  }
};
var getModelAnsi = (model, level, type, ...arguments_) => {
  if (model === "rgb") {
    if (level === "ansi16m") {
      return ansi_styles_default[type].ansi16m(...arguments_);
    }
    if (level === "ansi256") {
      return ansi_styles_default[type].ansi256(ansi_styles_default.rgbToAnsi256(...arguments_));
    }
    return ansi_styles_default[type].ansi(ansi_styles_default.rgbToAnsi(...arguments_));
  }
  if (model === "hex") {
    return getModelAnsi("rgb", level, type, ...ansi_styles_default.hexToRgb(...arguments_));
  }
  return ansi_styles_default[type][model](...arguments_);
};
var usedModels = ["rgb", "hex", "ansi256"];
for (const model of usedModels) {
  styles[model] = {
    get() {
      const { level } = this;
      return function(...arguments_) {
        const styler = createStyler(getModelAnsi(model, levelMapping[level], "color", ...arguments_), ansi_styles_default.color.close, this[STYLER]);
        return createBuilder(this, styler, this[IS_EMPTY]);
      };
    }
  };
  const bgModel = "bg" + model[0].toUpperCase() + model.slice(1);
  styles[bgModel] = {
    get() {
      const { level } = this;
      return function(...arguments_) {
        const styler = createStyler(getModelAnsi(model, levelMapping[level], "bgColor", ...arguments_), ansi_styles_default.bgColor.close, this[STYLER]);
        return createBuilder(this, styler, this[IS_EMPTY]);
      };
    }
  };
}
var proto = Object.defineProperties(() => {
}, {
  ...styles,
  level: {
    enumerable: true,
    get() {
      return this[GENERATOR].level;
    },
    set(level) {
      this[GENERATOR].level = level;
    }
  }
});
var createStyler = (open, close, parent) => {
  let openAll;
  let closeAll;
  if (parent === void 0) {
    openAll = open;
    closeAll = close;
  } else {
    openAll = parent.openAll + open;
    closeAll = close + parent.closeAll;
  }
  return {
    open,
    close,
    openAll,
    closeAll,
    parent
  };
};
var createBuilder = (self, _styler, _isEmpty) => {
  const builder = (...arguments_) => applyStyle(builder, arguments_.length === 1 ? "" + arguments_[0] : arguments_.join(" "));
  Object.setPrototypeOf(builder, proto);
  builder[GENERATOR] = self;
  builder[STYLER] = _styler;
  builder[IS_EMPTY] = _isEmpty;
  return builder;
};
var applyStyle = (self, string) => {
  if (self.level <= 0 || !string) {
    return self[IS_EMPTY] ? "" : string;
  }
  let styler = self[STYLER];
  if (styler === void 0) {
    return string;
  }
  const { openAll, closeAll } = styler;
  if (string.includes("\x1B")) {
    while (styler !== void 0) {
      string = stringReplaceAll(string, styler.close, styler.open);
      styler = styler.parent;
    }
  }
  const lfIndex = string.indexOf("\n");
  if (lfIndex !== -1) {
    string = stringEncaseCRLFWithFirstIndex(string, closeAll, openAll, lfIndex);
  }
  return openAll + string + closeAll;
};
Object.defineProperties(createChalk.prototype, styles);
var chalk = createChalk();
var chalkStderr = createChalk({ level: stderrColor ? stderrColor.level : 0 });
var source_default = chalk;

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/update-notifier.js
var import_semver3 = __toESM(require_semver2(), 1);

// ../../node_modules/.pnpm/semver-diff@4.0.0/node_modules/semver-diff/index.js
init_cjs_shims();
var import_semver = __toESM(require_semver2(), 1);
function semverDiff(versionA, versionB) {
  versionA = import_semver.default.parse(versionA);
  versionB = import_semver.default.parse(versionB);
  if (import_semver.default.compareBuild(versionA, versionB) >= 0) {
    return;
  }
  return import_semver.default.diff(versionA, versionB) || "build";
}

// ../../node_modules/.pnpm/latest-version@7.0.0/node_modules/latest-version/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/package-json@8.1.0/node_modules/package-json/index.js
init_cjs_shims();
var import_node_http3 = require("http");
var import_node_https2 = require("https");

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/create.js
init_cjs_shims();

// ../../node_modules/.pnpm/@sindresorhus+is@5.3.0/node_modules/@sindresorhus/is/dist/index.js
init_cjs_shims();
var typedArrayTypeNames = [
  "Int8Array",
  "Uint8Array",
  "Uint8ClampedArray",
  "Int16Array",
  "Uint16Array",
  "Int32Array",
  "Uint32Array",
  "Float32Array",
  "Float64Array",
  "BigInt64Array",
  "BigUint64Array"
];
function isTypedArrayName(name) {
  return typedArrayTypeNames.includes(name);
}
var objectTypeNames = [
  "Function",
  "Generator",
  "AsyncGenerator",
  "GeneratorFunction",
  "AsyncGeneratorFunction",
  "AsyncFunction",
  "Observable",
  "Array",
  "Buffer",
  "Blob",
  "Object",
  "RegExp",
  "Date",
  "Error",
  "Map",
  "Set",
  "WeakMap",
  "WeakSet",
  "WeakRef",
  "ArrayBuffer",
  "SharedArrayBuffer",
  "DataView",
  "Promise",
  "URL",
  "FormData",
  "URLSearchParams",
  "HTMLElement",
  "NaN",
  ...typedArrayTypeNames
];
function isObjectTypeName(name) {
  return objectTypeNames.includes(name);
}
var primitiveTypeNames = [
  "null",
  "undefined",
  "string",
  "number",
  "bigint",
  "boolean",
  "symbol"
];
function isPrimitiveTypeName(name) {
  return primitiveTypeNames.includes(name);
}
function isOfType(type) {
  return (value) => typeof value === type;
}
var { toString } = Object.prototype;
var getObjectType = (value) => {
  const objectTypeName = toString.call(value).slice(8, -1);
  if (/HTML\w+Element/.test(objectTypeName) && is.domElement(value)) {
    return "HTMLElement";
  }
  if (isObjectTypeName(objectTypeName)) {
    return objectTypeName;
  }
  return void 0;
};
var isObjectOfType = (type) => (value) => getObjectType(value) === type;
function is(value) {
  if (value === null) {
    return "null";
  }
  switch (typeof value) {
    case "undefined":
      return "undefined";
    case "string":
      return "string";
    case "number":
      return Number.isNaN(value) ? "NaN" : "number";
    case "boolean":
      return "boolean";
    case "function":
      return "Function";
    case "bigint":
      return "bigint";
    case "symbol":
      return "symbol";
    default:
  }
  if (is.observable(value)) {
    return "Observable";
  }
  if (is.array(value)) {
    return "Array";
  }
  if (is.buffer(value)) {
    return "Buffer";
  }
  const tagType = getObjectType(value);
  if (tagType) {
    return tagType;
  }
  if (value instanceof String || value instanceof Boolean || value instanceof Number) {
    throw new TypeError("Please don't use object wrappers for primitive types");
  }
  return "Object";
}
is.undefined = isOfType("undefined");
is.string = isOfType("string");
var isNumberType = isOfType("number");
is.number = (value) => isNumberType(value) && !is.nan(value);
is.bigint = isOfType("bigint");
is.function_ = isOfType("function");
is.null_ = (value) => value === null;
is.class_ = (value) => is.function_(value) && value.toString().startsWith("class ");
is.boolean = (value) => value === true || value === false;
is.symbol = isOfType("symbol");
is.numericString = (value) => is.string(value) && !is.emptyStringOrWhitespace(value) && !Number.isNaN(Number(value));
is.array = (value, assertion) => {
  if (!Array.isArray(value)) {
    return false;
  }
  if (!is.function_(assertion)) {
    return true;
  }
  return value.every((element) => assertion(element));
};
is.buffer = (value) => value?.constructor?.isBuffer?.(value) ?? false;
is.blob = (value) => isObjectOfType("Blob")(value);
is.nullOrUndefined = (value) => is.null_(value) || is.undefined(value);
is.object = (value) => !is.null_(value) && (typeof value === "object" || is.function_(value));
is.iterable = (value) => is.function_(value?.[Symbol.iterator]);
is.asyncIterable = (value) => is.function_(value?.[Symbol.asyncIterator]);
is.generator = (value) => is.iterable(value) && is.function_(value?.next) && is.function_(value?.throw);
is.asyncGenerator = (value) => is.asyncIterable(value) && is.function_(value.next) && is.function_(value.throw);
is.nativePromise = (value) => isObjectOfType("Promise")(value);
var hasPromiseApi = (value) => is.function_(value?.then) && is.function_(value?.catch);
is.promise = (value) => is.nativePromise(value) || hasPromiseApi(value);
is.generatorFunction = isObjectOfType("GeneratorFunction");
is.asyncGeneratorFunction = (value) => getObjectType(value) === "AsyncGeneratorFunction";
is.asyncFunction = (value) => getObjectType(value) === "AsyncFunction";
is.boundFunction = (value) => is.function_(value) && !value.hasOwnProperty("prototype");
is.regExp = isObjectOfType("RegExp");
is.date = isObjectOfType("Date");
is.error = isObjectOfType("Error");
is.map = (value) => isObjectOfType("Map")(value);
is.set = (value) => isObjectOfType("Set")(value);
is.weakMap = (value) => isObjectOfType("WeakMap")(value);
is.weakSet = (value) => isObjectOfType("WeakSet")(value);
is.weakRef = (value) => isObjectOfType("WeakRef")(value);
is.int8Array = isObjectOfType("Int8Array");
is.uint8Array = isObjectOfType("Uint8Array");
is.uint8ClampedArray = isObjectOfType("Uint8ClampedArray");
is.int16Array = isObjectOfType("Int16Array");
is.uint16Array = isObjectOfType("Uint16Array");
is.int32Array = isObjectOfType("Int32Array");
is.uint32Array = isObjectOfType("Uint32Array");
is.float32Array = isObjectOfType("Float32Array");
is.float64Array = isObjectOfType("Float64Array");
is.bigInt64Array = isObjectOfType("BigInt64Array");
is.bigUint64Array = isObjectOfType("BigUint64Array");
is.arrayBuffer = isObjectOfType("ArrayBuffer");
is.sharedArrayBuffer = isObjectOfType("SharedArrayBuffer");
is.dataView = isObjectOfType("DataView");
is.enumCase = (value, targetEnum) => Object.values(targetEnum).includes(value);
is.directInstanceOf = (instance, class_) => Object.getPrototypeOf(instance) === class_.prototype;
is.urlInstance = (value) => isObjectOfType("URL")(value);
is.urlString = (value) => {
  if (!is.string(value)) {
    return false;
  }
  try {
    new URL(value);
    return true;
  } catch {
    return false;
  }
};
is.truthy = (value) => Boolean(value);
is.falsy = (value) => !value;
is.nan = (value) => Number.isNaN(value);
is.primitive = (value) => is.null_(value) || isPrimitiveTypeName(typeof value);
is.integer = (value) => Number.isInteger(value);
is.safeInteger = (value) => Number.isSafeInteger(value);
is.plainObject = (value) => {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const prototype = Object.getPrototypeOf(value);
  return (prototype === null || prototype === Object.prototype || Object.getPrototypeOf(prototype) === null) && !(Symbol.toStringTag in value) && !(Symbol.iterator in value);
};
is.typedArray = (value) => isTypedArrayName(getObjectType(value));
var isValidLength = (value) => is.safeInteger(value) && value >= 0;
is.arrayLike = (value) => !is.nullOrUndefined(value) && !is.function_(value) && isValidLength(value.length);
is.inRange = (value, range) => {
  if (is.number(range)) {
    return value >= Math.min(0, range) && value <= Math.max(range, 0);
  }
  if (is.array(range) && range.length === 2) {
    return value >= Math.min(...range) && value <= Math.max(...range);
  }
  throw new TypeError(`Invalid range: ${JSON.stringify(range)}`);
};
var NODE_TYPE_ELEMENT = 1;
var DOM_PROPERTIES_TO_CHECK = [
  "innerHTML",
  "ownerDocument",
  "style",
  "attributes",
  "nodeValue"
];
is.domElement = (value) => is.object(value) && value.nodeType === NODE_TYPE_ELEMENT && is.string(value.nodeName) && !is.plainObject(value) && DOM_PROPERTIES_TO_CHECK.every((property) => property in value);
is.observable = (value) => {
  if (!value) {
    return false;
  }
  if (value === value[Symbol.observable]?.()) {
    return true;
  }
  if (value === value["@@observable"]?.()) {
    return true;
  }
  return false;
};
is.nodeStream = (value) => is.object(value) && is.function_(value.pipe) && !is.observable(value);
is.infinite = (value) => value === Number.POSITIVE_INFINITY || value === Number.NEGATIVE_INFINITY;
var isAbsoluteMod2 = (remainder) => (value) => is.integer(value) && Math.abs(value % 2) === remainder;
is.evenInteger = isAbsoluteMod2(0);
is.oddInteger = isAbsoluteMod2(1);
is.emptyArray = (value) => is.array(value) && value.length === 0;
is.nonEmptyArray = (value) => is.array(value) && value.length > 0;
is.emptyString = (value) => is.string(value) && value.length === 0;
var isWhiteSpaceString = (value) => is.string(value) && !/\S/.test(value);
is.emptyStringOrWhitespace = (value) => is.emptyString(value) || isWhiteSpaceString(value);
is.nonEmptyString = (value) => is.string(value) && value.length > 0;
is.nonEmptyStringAndNotWhitespace = (value) => is.string(value) && !is.emptyStringOrWhitespace(value);
is.emptyObject = (value) => is.object(value) && !is.map(value) && !is.set(value) && Object.keys(value).length === 0;
is.nonEmptyObject = (value) => is.object(value) && !is.map(value) && !is.set(value) && Object.keys(value).length > 0;
is.emptySet = (value) => is.set(value) && value.size === 0;
is.nonEmptySet = (value) => is.set(value) && value.size > 0;
is.emptyMap = (value) => is.map(value) && value.size === 0;
is.nonEmptyMap = (value) => is.map(value) && value.size > 0;
is.propertyKey = (value) => is.any([is.string, is.number, is.symbol], value);
is.formData = (value) => isObjectOfType("FormData")(value);
is.urlSearchParams = (value) => isObjectOfType("URLSearchParams")(value);
var predicateOnArray = (method, predicate, values) => {
  if (!is.function_(predicate)) {
    throw new TypeError(`Invalid predicate: ${JSON.stringify(predicate)}`);
  }
  if (values.length === 0) {
    throw new TypeError("Invalid number of values");
  }
  return method.call(values, predicate);
};
is.any = (predicate, ...values) => {
  const predicates = is.array(predicate) ? predicate : [predicate];
  return predicates.some((singlePredicate) => predicateOnArray(Array.prototype.some, singlePredicate, values));
};
is.all = (predicate, ...values) => predicateOnArray(Array.prototype.every, predicate, values);
var assertType = (condition, description, value, options = {}) => {
  if (!condition) {
    const { multipleValues } = options;
    const valuesMessage = multipleValues ? `received values of types ${[
      ...new Set(value.map((singleValue) => `\`${is(singleValue)}\``))
    ].join(", ")}` : `received value of type \`${is(value)}\``;
    throw new TypeError(`Expected value which is \`${description}\`, ${valuesMessage}.`);
  }
};
var assert = {
  undefined: (value) => assertType(is.undefined(value), "undefined", value),
  string: (value) => assertType(is.string(value), "string", value),
  number: (value) => assertType(is.number(value), "number", value),
  bigint: (value) => assertType(is.bigint(value), "bigint", value),
  function_: (value) => assertType(is.function_(value), "Function", value),
  null_: (value) => assertType(is.null_(value), "null", value),
  class_: (value) => assertType(is.class_(value), "Class", value),
  boolean: (value) => assertType(is.boolean(value), "boolean", value),
  symbol: (value) => assertType(is.symbol(value), "symbol", value),
  numericString: (value) => assertType(is.numericString(value), "string with a number", value),
  array: (value, assertion) => {
    const assert2 = assertType;
    assert2(is.array(value), "Array", value);
    if (assertion) {
      value.forEach(assertion);
    }
  },
  buffer: (value) => assertType(is.buffer(value), "Buffer", value),
  blob: (value) => assertType(is.blob(value), "Blob", value),
  nullOrUndefined: (value) => assertType(is.nullOrUndefined(value), "null or undefined", value),
  object: (value) => assertType(is.object(value), "Object", value),
  iterable: (value) => assertType(is.iterable(value), "Iterable", value),
  asyncIterable: (value) => assertType(is.asyncIterable(value), "AsyncIterable", value),
  generator: (value) => assertType(is.generator(value), "Generator", value),
  asyncGenerator: (value) => assertType(is.asyncGenerator(value), "AsyncGenerator", value),
  nativePromise: (value) => assertType(is.nativePromise(value), "native Promise", value),
  promise: (value) => assertType(is.promise(value), "Promise", value),
  generatorFunction: (value) => assertType(is.generatorFunction(value), "GeneratorFunction", value),
  asyncGeneratorFunction: (value) => assertType(is.asyncGeneratorFunction(value), "AsyncGeneratorFunction", value),
  asyncFunction: (value) => assertType(is.asyncFunction(value), "AsyncFunction", value),
  boundFunction: (value) => assertType(is.boundFunction(value), "Function", value),
  regExp: (value) => assertType(is.regExp(value), "RegExp", value),
  date: (value) => assertType(is.date(value), "Date", value),
  error: (value) => assertType(is.error(value), "Error", value),
  map: (value) => assertType(is.map(value), "Map", value),
  set: (value) => assertType(is.set(value), "Set", value),
  weakMap: (value) => assertType(is.weakMap(value), "WeakMap", value),
  weakSet: (value) => assertType(is.weakSet(value), "WeakSet", value),
  weakRef: (value) => assertType(is.weakRef(value), "WeakRef", value),
  int8Array: (value) => assertType(is.int8Array(value), "Int8Array", value),
  uint8Array: (value) => assertType(is.uint8Array(value), "Uint8Array", value),
  uint8ClampedArray: (value) => assertType(is.uint8ClampedArray(value), "Uint8ClampedArray", value),
  int16Array: (value) => assertType(is.int16Array(value), "Int16Array", value),
  uint16Array: (value) => assertType(is.uint16Array(value), "Uint16Array", value),
  int32Array: (value) => assertType(is.int32Array(value), "Int32Array", value),
  uint32Array: (value) => assertType(is.uint32Array(value), "Uint32Array", value),
  float32Array: (value) => assertType(is.float32Array(value), "Float32Array", value),
  float64Array: (value) => assertType(is.float64Array(value), "Float64Array", value),
  bigInt64Array: (value) => assertType(is.bigInt64Array(value), "BigInt64Array", value),
  bigUint64Array: (value) => assertType(is.bigUint64Array(value), "BigUint64Array", value),
  arrayBuffer: (value) => assertType(is.arrayBuffer(value), "ArrayBuffer", value),
  sharedArrayBuffer: (value) => assertType(is.sharedArrayBuffer(value), "SharedArrayBuffer", value),
  dataView: (value) => assertType(is.dataView(value), "DataView", value),
  enumCase: (value, targetEnum) => assertType(is.enumCase(value, targetEnum), "EnumCase", value),
  urlInstance: (value) => assertType(is.urlInstance(value), "URL", value),
  urlString: (value) => assertType(is.urlString(value), "string with a URL", value),
  truthy: (value) => assertType(is.truthy(value), "truthy", value),
  falsy: (value) => assertType(is.falsy(value), "falsy", value),
  nan: (value) => assertType(is.nan(value), "NaN", value),
  primitive: (value) => assertType(is.primitive(value), "primitive", value),
  integer: (value) => assertType(is.integer(value), "integer", value),
  safeInteger: (value) => assertType(is.safeInteger(value), "integer", value),
  plainObject: (value) => assertType(is.plainObject(value), "plain object", value),
  typedArray: (value) => assertType(is.typedArray(value), "TypedArray", value),
  arrayLike: (value) => assertType(is.arrayLike(value), "array-like", value),
  domElement: (value) => assertType(is.domElement(value), "HTMLElement", value),
  observable: (value) => assertType(is.observable(value), "Observable", value),
  nodeStream: (value) => assertType(is.nodeStream(value), "Node.js Stream", value),
  infinite: (value) => assertType(is.infinite(value), "infinite number", value),
  emptyArray: (value) => assertType(is.emptyArray(value), "empty array", value),
  nonEmptyArray: (value) => assertType(is.nonEmptyArray(value), "non-empty array", value),
  emptyString: (value) => assertType(is.emptyString(value), "empty string", value),
  emptyStringOrWhitespace: (value) => assertType(is.emptyStringOrWhitespace(value), "empty string or whitespace", value),
  nonEmptyString: (value) => assertType(is.nonEmptyString(value), "non-empty string", value),
  nonEmptyStringAndNotWhitespace: (value) => assertType(is.nonEmptyStringAndNotWhitespace(value), "non-empty string and not whitespace", value),
  emptyObject: (value) => assertType(is.emptyObject(value), "empty object", value),
  nonEmptyObject: (value) => assertType(is.nonEmptyObject(value), "non-empty object", value),
  emptySet: (value) => assertType(is.emptySet(value), "empty set", value),
  nonEmptySet: (value) => assertType(is.nonEmptySet(value), "non-empty set", value),
  emptyMap: (value) => assertType(is.emptyMap(value), "empty map", value),
  nonEmptyMap: (value) => assertType(is.nonEmptyMap(value), "non-empty map", value),
  propertyKey: (value) => assertType(is.propertyKey(value), "PropertyKey", value),
  formData: (value) => assertType(is.formData(value), "FormData", value),
  urlSearchParams: (value) => assertType(is.urlSearchParams(value), "URLSearchParams", value),
  evenInteger: (value) => assertType(is.evenInteger(value), "even integer", value),
  oddInteger: (value) => assertType(is.oddInteger(value), "odd integer", value),
  directInstanceOf: (instance, class_) => assertType(is.directInstanceOf(instance, class_), "T", instance),
  inRange: (value, range) => assertType(is.inRange(value, range), "in range", value),
  any: (predicate, ...values) => assertType(is.any(predicate, ...values), "predicate returns truthy for any value", values, { multipleValues: true }),
  all: (predicate, ...values) => assertType(is.all(predicate, ...values), "predicate returns truthy for all values", values, { multipleValues: true })
};
Object.defineProperties(is, {
  class: {
    value: is.class_
  },
  function: {
    value: is.function_
  },
  null: {
    value: is.null_
  }
});
Object.defineProperties(assert, {
  class: {
    value: assert.class_
  },
  function: {
    value: assert.function_
  },
  null: {
    value: assert.null_
  }
});
var dist_default = is;

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/as-promise/index.js
init_cjs_shims();
var import_node_events2 = require("events");

// ../../node_modules/.pnpm/p-cancelable@3.0.0/node_modules/p-cancelable/index.js
init_cjs_shims();
var CancelError = class extends Error {
  constructor(reason) {
    super(reason || "Promise was canceled");
    this.name = "CancelError";
  }
  get isCanceled() {
    return true;
  }
};
var PCancelable = class {
  static fn(userFunction) {
    return (...arguments_) => {
      return new PCancelable((resolve, reject, onCancel) => {
        arguments_.push(onCancel);
        userFunction(...arguments_).then(resolve, reject);
      });
    };
  }
  constructor(executor) {
    this._cancelHandlers = [];
    this._isPending = true;
    this._isCanceled = false;
    this._rejectOnCancel = true;
    this._promise = new Promise((resolve, reject) => {
      this._reject = reject;
      const onResolve = (value) => {
        if (!this._isCanceled || !onCancel.shouldReject) {
          this._isPending = false;
          resolve(value);
        }
      };
      const onReject = (error) => {
        this._isPending = false;
        reject(error);
      };
      const onCancel = (handler) => {
        if (!this._isPending) {
          throw new Error("The `onCancel` handler was attached after the promise settled.");
        }
        this._cancelHandlers.push(handler);
      };
      Object.defineProperties(onCancel, {
        shouldReject: {
          get: () => this._rejectOnCancel,
          set: (boolean) => {
            this._rejectOnCancel = boolean;
          }
        }
      });
      executor(onResolve, onReject, onCancel);
    });
  }
  then(onFulfilled, onRejected) {
    return this._promise.then(onFulfilled, onRejected);
  }
  catch(onRejected) {
    return this._promise.catch(onRejected);
  }
  finally(onFinally) {
    return this._promise.finally(onFinally);
  }
  cancel(reason) {
    if (!this._isPending || this._isCanceled) {
      return;
    }
    this._isCanceled = true;
    if (this._cancelHandlers.length > 0) {
      try {
        for (const handler of this._cancelHandlers) {
          handler();
        }
      } catch (error) {
        this._reject(error);
        return;
      }
    }
    if (this._rejectOnCancel) {
      this._reject(new CancelError(reason));
    }
  }
  get isCanceled() {
    return this._isCanceled;
  }
};
Object.setPrototypeOf(PCancelable.prototype, Promise.prototype);

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/errors.js
init_cjs_shims();
function isRequest(x) {
  return dist_default.object(x) && "_onResponse" in x;
}
var RequestError = class extends Error {
  constructor(message, error, self) {
    super(message);
    Object.defineProperty(this, "input", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "code", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "stack", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "response", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "request", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "timings", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Error.captureStackTrace(this, this.constructor);
    this.name = "RequestError";
    this.code = error.code ?? "ERR_GOT_REQUEST_ERROR";
    this.input = error.input;
    if (isRequest(self)) {
      Object.defineProperty(this, "request", {
        enumerable: false,
        value: self
      });
      Object.defineProperty(this, "response", {
        enumerable: false,
        value: self.response
      });
      this.options = self.options;
    } else {
      this.options = self;
    }
    this.timings = this.request?.timings;
    if (dist_default.string(error.stack) && dist_default.string(this.stack)) {
      const indexOfMessage = this.stack.indexOf(this.message) + this.message.length;
      const thisStackTrace = this.stack.slice(indexOfMessage).split("\n").reverse();
      const errorStackTrace = error.stack.slice(error.stack.indexOf(error.message) + error.message.length).split("\n").reverse();
      while (errorStackTrace.length > 0 && errorStackTrace[0] === thisStackTrace[0]) {
        thisStackTrace.shift();
      }
      this.stack = `${this.stack.slice(0, indexOfMessage)}${thisStackTrace.reverse().join("\n")}${errorStackTrace.reverse().join("\n")}`;
    }
  }
};
var MaxRedirectsError = class extends RequestError {
  constructor(request) {
    super(`Redirected ${request.options.maxRedirects} times. Aborting.`, {}, request);
    this.name = "MaxRedirectsError";
    this.code = "ERR_TOO_MANY_REDIRECTS";
  }
};
var HTTPError = class extends RequestError {
  constructor(response) {
    super(`Response code ${response.statusCode} (${response.statusMessage})`, {}, response.request);
    this.name = "HTTPError";
    this.code = "ERR_NON_2XX_3XX_RESPONSE";
  }
};
var CacheError = class extends RequestError {
  constructor(error, request) {
    super(error.message, error, request);
    this.name = "CacheError";
    this.code = this.code === "ERR_GOT_REQUEST_ERROR" ? "ERR_CACHE_ACCESS" : this.code;
  }
};
var UploadError = class extends RequestError {
  constructor(error, request) {
    super(error.message, error, request);
    this.name = "UploadError";
    this.code = this.code === "ERR_GOT_REQUEST_ERROR" ? "ERR_UPLOAD" : this.code;
  }
};
var TimeoutError = class extends RequestError {
  constructor(error, timings, request) {
    super(error.message, error, request);
    Object.defineProperty(this, "timings", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "event", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    this.name = "TimeoutError";
    this.event = error.event;
    this.timings = timings;
  }
};
var ReadError = class extends RequestError {
  constructor(error, request) {
    super(error.message, error, request);
    this.name = "ReadError";
    this.code = this.code === "ERR_GOT_REQUEST_ERROR" ? "ERR_READING_RESPONSE_STREAM" : this.code;
  }
};
var RetryError = class extends RequestError {
  constructor(request) {
    super("Retrying", {}, request);
    this.name = "RetryError";
    this.code = "ERR_RETRYING";
  }
};
var AbortError = class extends RequestError {
  constructor(request) {
    super("This operation was aborted.", {}, request);
    this.code = "ERR_ABORTED";
    this.name = "AbortError";
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/index.js
init_cjs_shims();
var import_node_process3 = __toESM(require("process"), 1);
var import_node_buffer2 = require("buffer");
var import_node_stream3 = require("stream");
var import_node_url3 = require("url");
var import_node_http2 = __toESM(require("http"), 1);

// ../../node_modules/.pnpm/@szmarczak+http-timer@5.0.1/node_modules/@szmarczak/http-timer/dist/source/index.js
init_cjs_shims();
var import_events = require("events");
var import_util2 = require("util");
var import_defer_to_connect = __toESM(require_source(), 1);
var timer = (request) => {
  if (request.timings) {
    return request.timings;
  }
  const timings = {
    start: Date.now(),
    socket: void 0,
    lookup: void 0,
    connect: void 0,
    secureConnect: void 0,
    upload: void 0,
    response: void 0,
    end: void 0,
    error: void 0,
    abort: void 0,
    phases: {
      wait: void 0,
      dns: void 0,
      tcp: void 0,
      tls: void 0,
      request: void 0,
      firstByte: void 0,
      download: void 0,
      total: void 0
    }
  };
  request.timings = timings;
  const handleError = (origin) => {
    origin.once(import_events.errorMonitor, () => {
      timings.error = Date.now();
      timings.phases.total = timings.error - timings.start;
    });
  };
  handleError(request);
  const onAbort = () => {
    timings.abort = Date.now();
    timings.phases.total = timings.abort - timings.start;
  };
  request.prependOnceListener("abort", onAbort);
  const onSocket = (socket) => {
    timings.socket = Date.now();
    timings.phases.wait = timings.socket - timings.start;
    if (import_util2.types.isProxy(socket)) {
      return;
    }
    const lookupListener = () => {
      timings.lookup = Date.now();
      timings.phases.dns = timings.lookup - timings.socket;
    };
    socket.prependOnceListener("lookup", lookupListener);
    (0, import_defer_to_connect.default)(socket, {
      connect: () => {
        timings.connect = Date.now();
        if (timings.lookup === void 0) {
          socket.removeListener("lookup", lookupListener);
          timings.lookup = timings.connect;
          timings.phases.dns = timings.lookup - timings.socket;
        }
        timings.phases.tcp = timings.connect - timings.lookup;
      },
      secureConnect: () => {
        timings.secureConnect = Date.now();
        timings.phases.tls = timings.secureConnect - timings.connect;
      }
    });
  };
  if (request.socket) {
    onSocket(request.socket);
  } else {
    request.prependOnceListener("socket", onSocket);
  }
  const onUpload = () => {
    timings.upload = Date.now();
    timings.phases.request = timings.upload - (timings.secureConnect ?? timings.connect);
  };
  if (request.writableFinished) {
    onUpload();
  } else {
    request.prependOnceListener("finish", onUpload);
  }
  request.prependOnceListener("response", (response) => {
    timings.response = Date.now();
    timings.phases.firstByte = timings.response - timings.upload;
    response.timings = timings;
    handleError(response);
    response.prependOnceListener("end", () => {
      request.off("abort", onAbort);
      response.off("aborted", onAbort);
      if (timings.phases.total) {
        return;
      }
      timings.end = Date.now();
      timings.phases.download = timings.end - timings.response;
      timings.phases.total = timings.end - timings.start;
    });
    response.prependOnceListener("aborted", onAbort);
  });
  return timings;
};
var source_default2 = timer;

// ../../node_modules/.pnpm/cacheable-request@10.2.10/node_modules/cacheable-request/dist/index.js
init_cjs_shims();
var import_node_events = __toESM(require("events"), 1);
var import_node_url = __toESM(require("url"), 1);
var import_node_crypto = __toESM(require("crypto"), 1);
var import_node_stream2 = __toESM(require("stream"), 1);

// ../../node_modules/.pnpm/normalize-url@8.0.0/node_modules/normalize-url/index.js
init_cjs_shims();
var DATA_URL_DEFAULT_MIME_TYPE = "text/plain";
var DATA_URL_DEFAULT_CHARSET = "us-ascii";
var testParameter = (name, filters) => filters.some((filter) => filter instanceof RegExp ? filter.test(name) : filter === name);
var supportedProtocols = /* @__PURE__ */ new Set([
  "https:",
  "http:",
  "file:"
]);
var hasCustomProtocol = (urlString) => {
  try {
    const { protocol } = new URL(urlString);
    return protocol.endsWith(":") && !supportedProtocols.has(protocol);
  } catch {
    return false;
  }
};
var normalizeDataURL = (urlString, { stripHash }) => {
  const match = /^data:(?<type>[^,]*?),(?<data>[^#]*?)(?:#(?<hash>.*))?$/.exec(urlString);
  if (!match) {
    throw new Error(`Invalid URL: ${urlString}`);
  }
  let { type, data, hash } = match.groups;
  const mediaType = type.split(";");
  hash = stripHash ? "" : hash;
  let isBase64 = false;
  if (mediaType[mediaType.length - 1] === "base64") {
    mediaType.pop();
    isBase64 = true;
  }
  const mimeType = mediaType.shift()?.toLowerCase() ?? "";
  const attributes = mediaType.map((attribute) => {
    let [key, value = ""] = attribute.split("=").map((string) => string.trim());
    if (key === "charset") {
      value = value.toLowerCase();
      if (value === DATA_URL_DEFAULT_CHARSET) {
        return "";
      }
    }
    return `${key}${value ? `=${value}` : ""}`;
  }).filter(Boolean);
  const normalizedMediaType = [
    ...attributes
  ];
  if (isBase64) {
    normalizedMediaType.push("base64");
  }
  if (normalizedMediaType.length > 0 || mimeType && mimeType !== DATA_URL_DEFAULT_MIME_TYPE) {
    normalizedMediaType.unshift(mimeType);
  }
  return `data:${normalizedMediaType.join(";")},${isBase64 ? data.trim() : data}${hash ? `#${hash}` : ""}`;
};
function normalizeUrl(urlString, options) {
  options = {
    defaultProtocol: "http",
    normalizeProtocol: true,
    forceHttp: false,
    forceHttps: false,
    stripAuthentication: true,
    stripHash: false,
    stripTextFragment: true,
    stripWWW: true,
    removeQueryParameters: [/^utm_\w+/i],
    removeTrailingSlash: true,
    removeSingleSlash: true,
    removeDirectoryIndex: false,
    removeExplicitPort: false,
    sortQueryParameters: true,
    ...options
  };
  if (typeof options.defaultProtocol === "string" && !options.defaultProtocol.endsWith(":")) {
    options.defaultProtocol = `${options.defaultProtocol}:`;
  }
  urlString = urlString.trim();
  if (/^data:/i.test(urlString)) {
    return normalizeDataURL(urlString, options);
  }
  if (hasCustomProtocol(urlString)) {
    return urlString;
  }
  const hasRelativeProtocol = urlString.startsWith("//");
  const isRelativeUrl = !hasRelativeProtocol && /^\.*\//.test(urlString);
  if (!isRelativeUrl) {
    urlString = urlString.replace(/^(?!(?:\w+:)?\/\/)|^\/\//, options.defaultProtocol);
  }
  const urlObject = new URL(urlString);
  if (options.forceHttp && options.forceHttps) {
    throw new Error("The `forceHttp` and `forceHttps` options cannot be used together");
  }
  if (options.forceHttp && urlObject.protocol === "https:") {
    urlObject.protocol = "http:";
  }
  if (options.forceHttps && urlObject.protocol === "http:") {
    urlObject.protocol = "https:";
  }
  if (options.stripAuthentication) {
    urlObject.username = "";
    urlObject.password = "";
  }
  if (options.stripHash) {
    urlObject.hash = "";
  } else if (options.stripTextFragment) {
    urlObject.hash = urlObject.hash.replace(/#?:~:text.*?$/i, "");
  }
  if (urlObject.pathname) {
    const protocolRegex = /\b[a-z][a-z\d+\-.]{1,50}:\/\//g;
    let lastIndex = 0;
    let result = "";
    for (; ; ) {
      const match = protocolRegex.exec(urlObject.pathname);
      if (!match) {
        break;
      }
      const protocol = match[0];
      const protocolAtIndex = match.index;
      const intermediate = urlObject.pathname.slice(lastIndex, protocolAtIndex);
      result += intermediate.replace(/\/{2,}/g, "/");
      result += protocol;
      lastIndex = protocolAtIndex + protocol.length;
    }
    const remnant = urlObject.pathname.slice(lastIndex, urlObject.pathname.length);
    result += remnant.replace(/\/{2,}/g, "/");
    urlObject.pathname = result;
  }
  if (urlObject.pathname) {
    try {
      urlObject.pathname = decodeURI(urlObject.pathname);
    } catch {
    }
  }
  if (options.removeDirectoryIndex === true) {
    options.removeDirectoryIndex = [/^index\.[a-z]+$/];
  }
  if (Array.isArray(options.removeDirectoryIndex) && options.removeDirectoryIndex.length > 0) {
    let pathComponents = urlObject.pathname.split("/");
    const lastComponent = pathComponents[pathComponents.length - 1];
    if (testParameter(lastComponent, options.removeDirectoryIndex)) {
      pathComponents = pathComponents.slice(0, -1);
      urlObject.pathname = pathComponents.slice(1).join("/") + "/";
    }
  }
  if (urlObject.hostname) {
    urlObject.hostname = urlObject.hostname.replace(/\.$/, "");
    if (options.stripWWW && /^www\.(?!www\.)[a-z\-\d]{1,63}\.[a-z.\-\d]{2,63}$/.test(urlObject.hostname)) {
      urlObject.hostname = urlObject.hostname.replace(/^www\./, "");
    }
  }
  if (Array.isArray(options.removeQueryParameters)) {
    for (const key of [...urlObject.searchParams.keys()]) {
      if (testParameter(key, options.removeQueryParameters)) {
        urlObject.searchParams.delete(key);
      }
    }
  }
  if (!Array.isArray(options.keepQueryParameters) && options.removeQueryParameters === true) {
    urlObject.search = "";
  }
  if (Array.isArray(options.keepQueryParameters) && options.keepQueryParameters.length > 0) {
    for (const key of [...urlObject.searchParams.keys()]) {
      if (!testParameter(key, options.keepQueryParameters)) {
        urlObject.searchParams.delete(key);
      }
    }
  }
  if (options.sortQueryParameters) {
    urlObject.searchParams.sort();
    try {
      urlObject.search = decodeURIComponent(urlObject.search);
    } catch {
    }
  }
  if (options.removeTrailingSlash) {
    urlObject.pathname = urlObject.pathname.replace(/\/$/, "");
  }
  if (options.removeExplicitPort && urlObject.port) {
    urlObject.port = "";
  }
  const oldUrlString = urlString;
  urlString = urlObject.toString();
  if (!options.removeSingleSlash && urlObject.pathname === "/" && !oldUrlString.endsWith("/") && urlObject.hash === "") {
    urlString = urlString.replace(/\/$/, "");
  }
  if ((options.removeTrailingSlash || urlObject.pathname === "/") && urlObject.hash === "" && options.removeSingleSlash) {
    urlString = urlString.replace(/\/$/, "");
  }
  if (hasRelativeProtocol && !options.normalizeProtocol) {
    urlString = urlString.replace(/^http:\/\//, "//");
  }
  if (options.stripProtocol) {
    urlString = urlString.replace(/^(?:https?:)?\/\//, "");
  }
  return urlString;
}

// ../../node_modules/.pnpm/cacheable-request@10.2.10/node_modules/cacheable-request/dist/index.js
var import_get_stream = __toESM(require_get_stream(), 1);
var import_http_cache_semantics = __toESM(require_http_cache_semantics(), 1);

// ../../node_modules/.pnpm/responselike@3.0.0/node_modules/responselike/index.js
init_cjs_shims();
var import_node_stream = require("stream");

// ../../node_modules/.pnpm/lowercase-keys@3.0.0/node_modules/lowercase-keys/index.js
init_cjs_shims();
function lowercaseKeys(object) {
  return Object.fromEntries(Object.entries(object).map(([key, value]) => [key.toLowerCase(), value]));
}

// ../../node_modules/.pnpm/responselike@3.0.0/node_modules/responselike/index.js
var Response = class extends import_node_stream.Readable {
  statusCode;
  headers;
  body;
  url;
  constructor({ statusCode, headers, body, url }) {
    if (typeof statusCode !== "number") {
      throw new TypeError("Argument `statusCode` should be a number");
    }
    if (typeof headers !== "object") {
      throw new TypeError("Argument `headers` should be an object");
    }
    if (!(body instanceof Uint8Array)) {
      throw new TypeError("Argument `body` should be a buffer");
    }
    if (typeof url !== "string") {
      throw new TypeError("Argument `url` should be a string");
    }
    super({
      read() {
        this.push(body);
        this.push(null);
      }
    });
    this.statusCode = statusCode;
    this.headers = lowercaseKeys(headers);
    this.body = body;
    this.url = url;
  }
};

// ../../node_modules/.pnpm/cacheable-request@10.2.10/node_modules/cacheable-request/dist/index.js
var import_keyv = __toESM(require_src(), 1);

// ../../node_modules/.pnpm/mimic-response@4.0.0/node_modules/mimic-response/index.js
init_cjs_shims();
var knownProperties = [
  "aborted",
  "complete",
  "headers",
  "httpVersion",
  "httpVersionMinor",
  "httpVersionMajor",
  "method",
  "rawHeaders",
  "rawTrailers",
  "setTimeout",
  "socket",
  "statusCode",
  "statusMessage",
  "trailers",
  "url"
];
function mimicResponse(fromStream, toStream) {
  if (toStream._readableState.autoDestroy) {
    throw new Error("The second stream must have the `autoDestroy` option set to `false`");
  }
  const fromProperties = /* @__PURE__ */ new Set([...Object.keys(fromStream), ...knownProperties]);
  const properties = {};
  for (const property of fromProperties) {
    if (property in toStream) {
      continue;
    }
    properties[property] = {
      get() {
        const value = fromStream[property];
        const isFunction2 = typeof value === "function";
        return isFunction2 ? value.bind(fromStream) : value;
      },
      set(value) {
        fromStream[property] = value;
      },
      enumerable: true,
      configurable: false
    };
  }
  Object.defineProperties(toStream, properties);
  fromStream.once("aborted", () => {
    toStream.destroy();
    toStream.emit("aborted");
  });
  fromStream.once("close", () => {
    if (fromStream.complete) {
      if (toStream.readable) {
        toStream.once("end", () => {
          toStream.emit("close");
        });
      } else {
        toStream.emit("close");
      }
    } else {
      toStream.emit("close");
    }
  });
  return toStream;
}

// ../../node_modules/.pnpm/cacheable-request@10.2.10/node_modules/cacheable-request/dist/types.js
init_cjs_shims();
var RequestError2 = class extends Error {
  constructor(error) {
    super(error.message);
    Object.assign(this, error);
  }
};
var CacheError2 = class extends Error {
  constructor(error) {
    super(error.message);
    Object.assign(this, error);
  }
};

// ../../node_modules/.pnpm/cacheable-request@10.2.10/node_modules/cacheable-request/dist/index.js
var CacheableRequest = class {
  constructor(cacheRequest, cacheAdapter) {
    this.hooks = /* @__PURE__ */ new Map();
    this.request = () => (options, cb) => {
      let url;
      if (typeof options === "string") {
        url = normalizeUrlObject(import_node_url.default.parse(options));
        options = {};
      } else if (options instanceof import_node_url.default.URL) {
        url = normalizeUrlObject(import_node_url.default.parse(options.toString()));
        options = {};
      } else {
        const [pathname, ...searchParts] = (options.path ?? "").split("?");
        const search = searchParts.length > 0 ? `?${searchParts.join("?")}` : "";
        url = normalizeUrlObject({ ...options, pathname, search });
      }
      options = {
        headers: {},
        method: "GET",
        cache: true,
        strictTtl: false,
        automaticFailover: false,
        ...options,
        ...urlObjectToRequestOptions(url)
      };
      options.headers = Object.fromEntries(entries(options.headers).map(([key2, value]) => [key2.toLowerCase(), value]));
      const ee = new import_node_events.default();
      const normalizedUrlString = normalizeUrl(import_node_url.default.format(url), {
        stripWWW: false,
        removeTrailingSlash: false,
        stripAuthentication: false
      });
      let key = `${options.method}:${normalizedUrlString}`;
      if (options.body && options.method !== void 0 && ["POST", "PATCH", "PUT"].includes(options.method)) {
        if (options.body instanceof import_node_stream2.default.Readable) {
          options.cache = false;
        } else {
          key += `:${import_node_crypto.default.createHash("md5").update(options.body).digest("hex")}`;
        }
      }
      let revalidate = false;
      let madeRequest = false;
      const makeRequest = (options_) => {
        madeRequest = true;
        let requestErrored = false;
        let requestErrorCallback = () => {
        };
        const requestErrorPromise = new Promise((resolve) => {
          requestErrorCallback = () => {
            if (!requestErrored) {
              requestErrored = true;
              resolve();
            }
          };
        });
        const handler = async (response) => {
          if (revalidate) {
            response.status = response.statusCode;
            const revalidatedPolicy = import_http_cache_semantics.default.fromObject(revalidate.cachePolicy).revalidatedPolicy(options_, response);
            if (!revalidatedPolicy.modified) {
              response.resume();
              await new Promise((resolve) => {
                response.once("end", resolve);
              });
              const headers = convertHeaders(revalidatedPolicy.policy.responseHeaders());
              response = new Response({ statusCode: revalidate.statusCode, headers, body: revalidate.body, url: revalidate.url });
              response.cachePolicy = revalidatedPolicy.policy;
              response.fromCache = true;
            }
          }
          if (!response.fromCache) {
            response.cachePolicy = new import_http_cache_semantics.default(options_, response, options_);
            response.fromCache = false;
          }
          let clonedResponse;
          if (options_.cache && response.cachePolicy.storable()) {
            clonedResponse = cloneResponse(response);
            (async () => {
              try {
                const bodyPromise = import_get_stream.default.buffer(response);
                await Promise.race([
                  requestErrorPromise,
                  new Promise((resolve) => response.once("end", resolve)),
                  new Promise((resolve) => response.once("close", resolve))
                ]);
                const body = await bodyPromise;
                let value = {
                  url: response.url,
                  statusCode: response.fromCache ? revalidate.statusCode : response.statusCode,
                  body,
                  cachePolicy: response.cachePolicy.toObject()
                };
                let ttl2 = options_.strictTtl ? response.cachePolicy.timeToLive() : void 0;
                if (options_.maxTtl) {
                  ttl2 = ttl2 ? Math.min(ttl2, options_.maxTtl) : options_.maxTtl;
                }
                if (this.hooks.size > 0) {
                  for (const key_ of this.hooks.keys()) {
                    value = await this.runHook(key_, value, response);
                  }
                }
                await this.cache.set(key, value, ttl2);
              } catch (error) {
                ee.emit("error", new CacheError2(error));
              }
            })();
          } else if (options_.cache && revalidate) {
            (async () => {
              try {
                await this.cache.delete(key);
              } catch (error) {
                ee.emit("error", new CacheError2(error));
              }
            })();
          }
          ee.emit("response", clonedResponse ?? response);
          if (typeof cb === "function") {
            cb(clonedResponse ?? response);
          }
        };
        try {
          const request_ = this.cacheRequest(options_, handler);
          request_.once("error", requestErrorCallback);
          request_.once("abort", requestErrorCallback);
          request_.once("destroy", requestErrorCallback);
          ee.emit("request", request_);
        } catch (error) {
          ee.emit("error", new RequestError2(error));
        }
      };
      (async () => {
        const get = async (options_) => {
          await Promise.resolve();
          const cacheEntry = options_.cache ? await this.cache.get(key) : void 0;
          if (cacheEntry === void 0 && !options_.forceRefresh) {
            makeRequest(options_);
            return;
          }
          const policy = import_http_cache_semantics.default.fromObject(cacheEntry.cachePolicy);
          if (policy.satisfiesWithoutRevalidation(options_) && !options_.forceRefresh) {
            const headers = convertHeaders(policy.responseHeaders());
            const response = new Response({ statusCode: cacheEntry.statusCode, headers, body: cacheEntry.body, url: cacheEntry.url });
            response.cachePolicy = policy;
            response.fromCache = true;
            ee.emit("response", response);
            if (typeof cb === "function") {
              cb(response);
            }
          } else if (policy.satisfiesWithoutRevalidation(options_) && Date.now() >= policy.timeToLive() && options_.forceRefresh) {
            await this.cache.delete(key);
            options_.headers = policy.revalidationHeaders(options_);
            makeRequest(options_);
          } else {
            revalidate = cacheEntry;
            options_.headers = policy.revalidationHeaders(options_);
            makeRequest(options_);
          }
        };
        const errorHandler = (error) => ee.emit("error", new CacheError2(error));
        if (this.cache instanceof import_keyv.default) {
          const cachek = this.cache;
          cachek.once("error", errorHandler);
          ee.on("error", () => cachek.removeListener("error", errorHandler));
          ee.on("response", () => cachek.removeListener("error", errorHandler));
        }
        try {
          await get(options);
        } catch (error) {
          if (options.automaticFailover && !madeRequest) {
            makeRequest(options);
          }
          ee.emit("error", new CacheError2(error));
        }
      })();
      return ee;
    };
    this.addHook = (name, fn) => {
      if (!this.hooks.has(name)) {
        this.hooks.set(name, fn);
      }
    };
    this.removeHook = (name) => this.hooks.delete(name);
    this.getHook = (name) => this.hooks.get(name);
    this.runHook = async (name, ...args) => this.hooks.get(name)?.(...args);
    if (cacheAdapter instanceof import_keyv.default) {
      this.cache = cacheAdapter;
    } else if (typeof cacheAdapter === "string") {
      this.cache = new import_keyv.default({
        uri: cacheAdapter,
        namespace: "cacheable-request"
      });
    } else {
      this.cache = new import_keyv.default({
        store: cacheAdapter,
        namespace: "cacheable-request"
      });
    }
    this.request = this.request.bind(this);
    this.cacheRequest = cacheRequest;
  }
};
var entries = Object.entries;
var cloneResponse = (response) => {
  const clone = new import_node_stream2.PassThrough({ autoDestroy: false });
  mimicResponse(response, clone);
  return response.pipe(clone);
};
var urlObjectToRequestOptions = (url) => {
  const options = { ...url };
  options.path = `${url.pathname || "/"}${url.search || ""}`;
  delete options.pathname;
  delete options.search;
  return options;
};
var normalizeUrlObject = (url) => ({
  protocol: url.protocol,
  auth: url.auth,
  hostname: url.hostname || url.host || "localhost",
  port: url.port,
  pathname: url.pathname,
  search: url.search
});
var convertHeaders = (headers) => {
  const result = [];
  for (const name of Object.keys(headers)) {
    result[name.toLowerCase()] = headers[name];
  }
  return result;
};
var dist_default2 = CacheableRequest;

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/index.js
var import_decompress_response = __toESM(require_decompress_response(), 1);
var import_get_stream2 = __toESM(require_get_stream(), 1);

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/FormDataEncoder.js
init_cjs_shims();

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/getStreamIterator.js
init_cjs_shims();

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/isFunction.js
init_cjs_shims();
var isFunction = (value) => typeof value === "function";

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/getStreamIterator.js
var isAsyncIterable = (value) => isFunction(value[Symbol.asyncIterator]);
async function* readStream(readable) {
  const reader = readable.getReader();
  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    yield value;
  }
}
var getStreamIterator = (source) => {
  if (isAsyncIterable(source)) {
    return source;
  }
  if (isFunction(source.getReader)) {
    return readStream(source);
  }
  throw new TypeError("Unsupported data source: Expected either ReadableStream or async iterable.");
};

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/createBoundary.js
init_cjs_shims();
var alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
function createBoundary() {
  let size = 16;
  let res = "";
  while (size--) {
    res += alphabet[Math.random() * alphabet.length << 0];
  }
  return res;
}

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/normalizeValue.js
init_cjs_shims();
var normalizeValue = (value) => String(value).replace(/\r|\n/g, (match, i, str) => {
  if (match === "\r" && str[i + 1] !== "\n" || match === "\n" && str[i - 1] !== "\r") {
    return "\r\n";
  }
  return match;
});

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/isPlainObject.js
init_cjs_shims();
var getType = (value) => Object.prototype.toString.call(value).slice(8, -1).toLowerCase();
function isPlainObject(value) {
  if (getType(value) !== "object") {
    return false;
  }
  const pp = Object.getPrototypeOf(value);
  if (pp === null || pp === void 0) {
    return true;
  }
  const Ctor = pp.constructor && pp.constructor.toString();
  return Ctor === Object.toString();
}

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/proxyHeaders.js
init_cjs_shims();
function getProperty(target, prop) {
  if (typeof prop === "string") {
    for (const [name, value] of Object.entries(target)) {
      if (prop.toLowerCase() === name.toLowerCase()) {
        return value;
      }
    }
  }
  return void 0;
}
var proxyHeaders = (object) => new Proxy(object, {
  get: (target, prop) => getProperty(target, prop),
  has: (target, prop) => getProperty(target, prop) !== void 0
});

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/isFormData.js
init_cjs_shims();
var isFormData = (value) => Boolean(value && isFunction(value.constructor) && value[Symbol.toStringTag] === "FormData" && isFunction(value.append) && isFunction(value.getAll) && isFunction(value.entries) && isFunction(value[Symbol.iterator]));

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/escapeName.js
init_cjs_shims();
var escapeName = (name) => String(name).replace(/\r/g, "%0D").replace(/\n/g, "%0A").replace(/"/g, "%22");

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/util/isFile.js
init_cjs_shims();
var isFile = (value) => Boolean(value && typeof value === "object" && isFunction(value.constructor) && value[Symbol.toStringTag] === "File" && isFunction(value.stream) && value.name != null);

// ../../node_modules/.pnpm/form-data-encoder@2.1.4/node_modules/form-data-encoder/lib/FormDataEncoder.js
var __classPrivateFieldSet = function(receiver, state, value, kind, f) {
  if (kind === "m")
    throw new TypeError("Private method is not writable");
  if (kind === "a" && !f)
    throw new TypeError("Private accessor was defined without a setter");
  if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver))
    throw new TypeError("Cannot write private member to an object whose class did not declare it");
  return kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value), value;
};
var __classPrivateFieldGet = function(receiver, state, kind, f) {
  if (kind === "a" && !f)
    throw new TypeError("Private accessor was defined without a getter");
  if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver))
    throw new TypeError("Cannot read private member from an object whose class did not declare it");
  return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _FormDataEncoder_instances;
var _FormDataEncoder_CRLF;
var _FormDataEncoder_CRLF_BYTES;
var _FormDataEncoder_CRLF_BYTES_LENGTH;
var _FormDataEncoder_DASHES;
var _FormDataEncoder_encoder;
var _FormDataEncoder_footer;
var _FormDataEncoder_form;
var _FormDataEncoder_options;
var _FormDataEncoder_getFieldHeader;
var _FormDataEncoder_getContentLength;
var defaultOptions = {
  enableAdditionalHeaders: false
};
var readonlyProp = { writable: false, configurable: false };
var FormDataEncoder = class {
  constructor(form, boundaryOrOptions, options) {
    _FormDataEncoder_instances.add(this);
    _FormDataEncoder_CRLF.set(this, "\r\n");
    _FormDataEncoder_CRLF_BYTES.set(this, void 0);
    _FormDataEncoder_CRLF_BYTES_LENGTH.set(this, void 0);
    _FormDataEncoder_DASHES.set(this, "-".repeat(2));
    _FormDataEncoder_encoder.set(this, new TextEncoder());
    _FormDataEncoder_footer.set(this, void 0);
    _FormDataEncoder_form.set(this, void 0);
    _FormDataEncoder_options.set(this, void 0);
    if (!isFormData(form)) {
      throw new TypeError("Expected first argument to be a FormData instance.");
    }
    let boundary;
    if (isPlainObject(boundaryOrOptions)) {
      options = boundaryOrOptions;
    } else {
      boundary = boundaryOrOptions;
    }
    if (!boundary) {
      boundary = createBoundary();
    }
    if (typeof boundary !== "string") {
      throw new TypeError("Expected boundary argument to be a string.");
    }
    if (options && !isPlainObject(options)) {
      throw new TypeError("Expected options argument to be an object.");
    }
    __classPrivateFieldSet(this, _FormDataEncoder_form, Array.from(form.entries()), "f");
    __classPrivateFieldSet(this, _FormDataEncoder_options, { ...defaultOptions, ...options }, "f");
    __classPrivateFieldSet(this, _FormDataEncoder_CRLF_BYTES, __classPrivateFieldGet(this, _FormDataEncoder_encoder, "f").encode(__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f")), "f");
    __classPrivateFieldSet(this, _FormDataEncoder_CRLF_BYTES_LENGTH, __classPrivateFieldGet(this, _FormDataEncoder_CRLF_BYTES, "f").byteLength, "f");
    this.boundary = `form-data-boundary-${boundary}`;
    this.contentType = `multipart/form-data; boundary=${this.boundary}`;
    __classPrivateFieldSet(this, _FormDataEncoder_footer, __classPrivateFieldGet(this, _FormDataEncoder_encoder, "f").encode(`${__classPrivateFieldGet(this, _FormDataEncoder_DASHES, "f")}${this.boundary}${__classPrivateFieldGet(this, _FormDataEncoder_DASHES, "f")}${__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f").repeat(2)}`), "f");
    const headers = {
      "Content-Type": this.contentType
    };
    const contentLength = __classPrivateFieldGet(this, _FormDataEncoder_instances, "m", _FormDataEncoder_getContentLength).call(this);
    if (contentLength) {
      this.contentLength = contentLength;
      headers["Content-Length"] = contentLength;
    }
    this.headers = proxyHeaders(Object.freeze(headers));
    Object.defineProperties(this, {
      boundary: readonlyProp,
      contentType: readonlyProp,
      contentLength: readonlyProp,
      headers: readonlyProp
    });
  }
  getContentLength() {
    return this.contentLength == null ? void 0 : Number(this.contentLength);
  }
  *values() {
    for (const [name, raw] of __classPrivateFieldGet(this, _FormDataEncoder_form, "f")) {
      const value = isFile(raw) ? raw : __classPrivateFieldGet(this, _FormDataEncoder_encoder, "f").encode(normalizeValue(raw));
      yield __classPrivateFieldGet(this, _FormDataEncoder_instances, "m", _FormDataEncoder_getFieldHeader).call(this, name, value);
      yield value;
      yield __classPrivateFieldGet(this, _FormDataEncoder_CRLF_BYTES, "f");
    }
    yield __classPrivateFieldGet(this, _FormDataEncoder_footer, "f");
  }
  async *encode() {
    for (const part of this.values()) {
      if (isFile(part)) {
        yield* getStreamIterator(part.stream());
      } else {
        yield part;
      }
    }
  }
  [(_FormDataEncoder_CRLF = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_CRLF_BYTES = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_CRLF_BYTES_LENGTH = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_DASHES = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_encoder = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_footer = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_form = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_options = /* @__PURE__ */ new WeakMap(), _FormDataEncoder_instances = /* @__PURE__ */ new WeakSet(), _FormDataEncoder_getFieldHeader = function _FormDataEncoder_getFieldHeader2(name, value) {
    let header = "";
    header += `${__classPrivateFieldGet(this, _FormDataEncoder_DASHES, "f")}${this.boundary}${__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f")}`;
    header += `Content-Disposition: form-data; name="${escapeName(name)}"`;
    if (isFile(value)) {
      header += `; filename="${escapeName(value.name)}"${__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f")}`;
      header += `Content-Type: ${value.type || "application/octet-stream"}`;
    }
    const size = isFile(value) ? value.size : value.byteLength;
    if (__classPrivateFieldGet(this, _FormDataEncoder_options, "f").enableAdditionalHeaders === true && size != null && !isNaN(size)) {
      header += `${__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f")}Content-Length: ${isFile(value) ? value.size : value.byteLength}`;
    }
    return __classPrivateFieldGet(this, _FormDataEncoder_encoder, "f").encode(`${header}${__classPrivateFieldGet(this, _FormDataEncoder_CRLF, "f").repeat(2)}`);
  }, _FormDataEncoder_getContentLength = function _FormDataEncoder_getContentLength2() {
    let length = 0;
    for (const [name, raw] of __classPrivateFieldGet(this, _FormDataEncoder_form, "f")) {
      const value = isFile(raw) ? raw : __classPrivateFieldGet(this, _FormDataEncoder_encoder, "f").encode(normalizeValue(raw));
      const size = isFile(value) ? value.size : value.byteLength;
      if (size == null || isNaN(size)) {
        return void 0;
      }
      length += __classPrivateFieldGet(this, _FormDataEncoder_instances, "m", _FormDataEncoder_getFieldHeader).call(this, name, value).byteLength;
      length += size;
      length += __classPrivateFieldGet(this, _FormDataEncoder_CRLF_BYTES_LENGTH, "f");
    }
    return String(length + __classPrivateFieldGet(this, _FormDataEncoder_footer, "f").byteLength);
  }, Symbol.iterator)]() {
    return this.values();
  }
  [Symbol.asyncIterator]() {
    return this.encode();
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/get-body-size.js
init_cjs_shims();
var import_node_buffer = require("buffer");
var import_node_util = require("util");

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/is-form-data.js
init_cjs_shims();
function isFormData2(body) {
  return dist_default.nodeStream(body) && dist_default.function_(body.getBoundary);
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/get-body-size.js
async function getBodySize(body, headers) {
  if (headers && "content-length" in headers) {
    return Number(headers["content-length"]);
  }
  if (!body) {
    return 0;
  }
  if (dist_default.string(body)) {
    return import_node_buffer.Buffer.byteLength(body);
  }
  if (dist_default.buffer(body)) {
    return body.length;
  }
  if (isFormData2(body)) {
    return (0, import_node_util.promisify)(body.getLength.bind(body))();
  }
  return void 0;
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/proxy-events.js
init_cjs_shims();
function proxyEvents(from, to, events) {
  const eventFunctions = {};
  for (const event of events) {
    const eventFunction = (...args) => {
      to.emit(event, ...args);
    };
    eventFunctions[event] = eventFunction;
    from.on(event, eventFunction);
  }
  return () => {
    for (const [event, eventFunction] of Object.entries(eventFunctions)) {
      from.off(event, eventFunction);
    }
  };
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/timed-out.js
init_cjs_shims();
var import_node_net = __toESM(require("net"), 1);

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/unhandle.js
init_cjs_shims();
function unhandle() {
  const handlers = [];
  return {
    once(origin, event, fn) {
      origin.once(event, fn);
      handlers.push({ origin, event, fn });
    },
    unhandleAll() {
      for (const handler of handlers) {
        const { origin, event, fn } = handler;
        origin.removeListener(event, fn);
      }
      handlers.length = 0;
    }
  };
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/timed-out.js
var reentry = Symbol("reentry");
var noop = () => {
};
var TimeoutError2 = class extends Error {
  constructor(threshold, event) {
    super(`Timeout awaiting '${event}' for ${threshold}ms`);
    Object.defineProperty(this, "event", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: event
    });
    Object.defineProperty(this, "code", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    this.name = "TimeoutError";
    this.code = "ETIMEDOUT";
  }
};
function timedOut(request, delays, options) {
  if (reentry in request) {
    return noop;
  }
  request[reentry] = true;
  const cancelers = [];
  const { once, unhandleAll } = unhandle();
  const addTimeout = (delay2, callback, event) => {
    const timeout = setTimeout(callback, delay2, delay2, event);
    timeout.unref?.();
    const cancel = () => {
      clearTimeout(timeout);
    };
    cancelers.push(cancel);
    return cancel;
  };
  const { host, hostname } = options;
  const timeoutHandler = (delay2, event) => {
    request.destroy(new TimeoutError2(delay2, event));
  };
  const cancelTimeouts = () => {
    for (const cancel of cancelers) {
      cancel();
    }
    unhandleAll();
  };
  request.once("error", (error) => {
    cancelTimeouts();
    if (request.listenerCount("error") === 0) {
      throw error;
    }
  });
  if (typeof delays.request !== "undefined") {
    const cancelTimeout = addTimeout(delays.request, timeoutHandler, "request");
    once(request, "response", (response) => {
      once(response, "end", cancelTimeout);
    });
  }
  if (typeof delays.socket !== "undefined") {
    const { socket } = delays;
    const socketTimeoutHandler = () => {
      timeoutHandler(socket, "socket");
    };
    request.setTimeout(socket, socketTimeoutHandler);
    cancelers.push(() => {
      request.removeListener("timeout", socketTimeoutHandler);
    });
  }
  const hasLookup = typeof delays.lookup !== "undefined";
  const hasConnect = typeof delays.connect !== "undefined";
  const hasSecureConnect = typeof delays.secureConnect !== "undefined";
  const hasSend = typeof delays.send !== "undefined";
  if (hasLookup || hasConnect || hasSecureConnect || hasSend) {
    once(request, "socket", (socket) => {
      const { socketPath } = request;
      if (socket.connecting) {
        const hasPath = Boolean(socketPath ?? import_node_net.default.isIP(hostname ?? host ?? "") !== 0);
        if (hasLookup && !hasPath && typeof socket.address().address === "undefined") {
          const cancelTimeout = addTimeout(delays.lookup, timeoutHandler, "lookup");
          once(socket, "lookup", cancelTimeout);
        }
        if (hasConnect) {
          const timeConnect = () => addTimeout(delays.connect, timeoutHandler, "connect");
          if (hasPath) {
            once(socket, "connect", timeConnect());
          } else {
            once(socket, "lookup", (error) => {
              if (error === null) {
                once(socket, "connect", timeConnect());
              }
            });
          }
        }
        if (hasSecureConnect && options.protocol === "https:") {
          once(socket, "connect", () => {
            const cancelTimeout = addTimeout(delays.secureConnect, timeoutHandler, "secureConnect");
            once(socket, "secureConnect", cancelTimeout);
          });
        }
      }
      if (hasSend) {
        const timeRequest = () => addTimeout(delays.send, timeoutHandler, "send");
        if (socket.connecting) {
          once(socket, "connect", () => {
            once(request, "upload-complete", timeRequest());
          });
        } else {
          once(request, "upload-complete", timeRequest());
        }
      }
    });
  }
  if (typeof delays.response !== "undefined") {
    once(request, "upload-complete", () => {
      const cancelTimeout = addTimeout(delays.response, timeoutHandler, "response");
      once(request, "response", cancelTimeout);
    });
  }
  if (typeof delays.read !== "undefined") {
    once(request, "response", (response) => {
      const cancelTimeout = addTimeout(delays.read, timeoutHandler, "read");
      once(response, "end", cancelTimeout);
    });
  }
  return cancelTimeouts;
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/url-to-options.js
init_cjs_shims();
function urlToOptions(url) {
  url = url;
  const options = {
    protocol: url.protocol,
    hostname: dist_default.string(url.hostname) && url.hostname.startsWith("[") ? url.hostname.slice(1, -1) : url.hostname,
    host: url.host,
    hash: url.hash,
    search: url.search,
    pathname: url.pathname,
    href: url.href,
    path: `${url.pathname || ""}${url.search || ""}`
  };
  if (dist_default.string(url.port) && url.port.length > 0) {
    options.port = Number(url.port);
  }
  if (url.username || url.password) {
    options.auth = `${url.username || ""}:${url.password || ""}`;
  }
  return options;
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/weakable-map.js
init_cjs_shims();
var WeakableMap = class {
  constructor() {
    Object.defineProperty(this, "weakMap", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "map", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    this.weakMap = /* @__PURE__ */ new WeakMap();
    this.map = /* @__PURE__ */ new Map();
  }
  set(key, value) {
    if (typeof key === "object") {
      this.weakMap.set(key, value);
    } else {
      this.map.set(key, value);
    }
  }
  get(key) {
    if (typeof key === "object") {
      return this.weakMap.get(key);
    }
    return this.map.get(key);
  }
  has(key) {
    if (typeof key === "object") {
      return this.weakMap.has(key);
    }
    return this.map.has(key);
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/calculate-retry-delay.js
init_cjs_shims();
var calculateRetryDelay = ({ attemptCount, retryOptions, error, retryAfter, computedValue }) => {
  if (error.name === "RetryError") {
    return 1;
  }
  if (attemptCount > retryOptions.limit) {
    return 0;
  }
  const hasMethod = retryOptions.methods.includes(error.options.method);
  const hasErrorCode = retryOptions.errorCodes.includes(error.code);
  const hasStatusCode = error.response && retryOptions.statusCodes.includes(error.response.statusCode);
  if (!hasMethod || !hasErrorCode && !hasStatusCode) {
    return 0;
  }
  if (error.response) {
    if (retryAfter) {
      if (retryAfter > computedValue) {
        return 0;
      }
      return retryAfter;
    }
    if (error.response.statusCode === 413) {
      return 0;
    }
  }
  const noise = Math.random() * retryOptions.noise;
  return Math.min(2 ** (attemptCount - 1) * 1e3, retryOptions.backoffLimit) + noise;
};
var calculate_retry_delay_default = calculateRetryDelay;

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/options.js
init_cjs_shims();
var import_node_process2 = __toESM(require("process"), 1);
var import_node_util3 = require("util");
var import_node_url2 = require("url");
var import_node_tls = require("tls");
var import_node_http = __toESM(require("http"), 1);
var import_node_https = __toESM(require("https"), 1);

// ../../node_modules/.pnpm/cacheable-lookup@7.0.0/node_modules/cacheable-lookup/source/index.js
init_cjs_shims();
var import_node_dns = require("dns");
var import_node_util2 = require("util");
var import_node_os2 = __toESM(require("os"), 1);
var { Resolver: AsyncResolver } = import_node_dns.promises;
var kCacheableLookupCreateConnection = Symbol("cacheableLookupCreateConnection");
var kCacheableLookupInstance = Symbol("cacheableLookupInstance");
var kExpires = Symbol("expires");
var supportsALL = typeof import_node_dns.ALL === "number";
var verifyAgent = (agent) => {
  if (!(agent && typeof agent.createConnection === "function")) {
    throw new Error("Expected an Agent instance as the first argument");
  }
};
var map4to6 = (entries2) => {
  for (const entry of entries2) {
    if (entry.family === 6) {
      continue;
    }
    entry.address = `::ffff:${entry.address}`;
    entry.family = 6;
  }
};
var getIfaceInfo = () => {
  let has4 = false;
  let has6 = false;
  for (const device of Object.values(import_node_os2.default.networkInterfaces())) {
    for (const iface of device) {
      if (iface.internal) {
        continue;
      }
      if (iface.family === "IPv6") {
        has6 = true;
      } else {
        has4 = true;
      }
      if (has4 && has6) {
        return { has4, has6 };
      }
    }
  }
  return { has4, has6 };
};
var isIterable = (map) => {
  return Symbol.iterator in map;
};
var ignoreNoResultErrors = (dnsPromise) => {
  return dnsPromise.catch((error) => {
    if (error.code === "ENODATA" || error.code === "ENOTFOUND" || error.code === "ENOENT") {
      return [];
    }
    throw error;
  });
};
var ttl = { ttl: true };
var all = { all: true };
var all4 = { all: true, family: 4 };
var all6 = { all: true, family: 6 };
var CacheableLookup = class {
  constructor({
    cache = /* @__PURE__ */ new Map(),
    maxTtl = Infinity,
    fallbackDuration = 3600,
    errorTtl = 0.15,
    resolver = new AsyncResolver(),
    lookup = import_node_dns.lookup
  } = {}) {
    this.maxTtl = maxTtl;
    this.errorTtl = errorTtl;
    this._cache = cache;
    this._resolver = resolver;
    this._dnsLookup = lookup && (0, import_node_util2.promisify)(lookup);
    this.stats = {
      cache: 0,
      query: 0
    };
    if (this._resolver instanceof AsyncResolver) {
      this._resolve4 = this._resolver.resolve4.bind(this._resolver);
      this._resolve6 = this._resolver.resolve6.bind(this._resolver);
    } else {
      this._resolve4 = (0, import_node_util2.promisify)(this._resolver.resolve4.bind(this._resolver));
      this._resolve6 = (0, import_node_util2.promisify)(this._resolver.resolve6.bind(this._resolver));
    }
    this._iface = getIfaceInfo();
    this._pending = {};
    this._nextRemovalTime = false;
    this._hostnamesToFallback = /* @__PURE__ */ new Set();
    this.fallbackDuration = fallbackDuration;
    if (fallbackDuration > 0) {
      const interval = setInterval(() => {
        this._hostnamesToFallback.clear();
      }, fallbackDuration * 1e3);
      if (interval.unref) {
        interval.unref();
      }
      this._fallbackInterval = interval;
    }
    this.lookup = this.lookup.bind(this);
    this.lookupAsync = this.lookupAsync.bind(this);
  }
  set servers(servers) {
    this.clear();
    this._resolver.setServers(servers);
  }
  get servers() {
    return this._resolver.getServers();
  }
  lookup(hostname, options, callback) {
    if (typeof options === "function") {
      callback = options;
      options = {};
    } else if (typeof options === "number") {
      options = {
        family: options
      };
    }
    if (!callback) {
      throw new Error("Callback must be a function.");
    }
    this.lookupAsync(hostname, options).then((result) => {
      if (options.all) {
        callback(null, result);
      } else {
        callback(null, result.address, result.family, result.expires, result.ttl, result.source);
      }
    }, callback);
  }
  async lookupAsync(hostname, options = {}) {
    if (typeof options === "number") {
      options = {
        family: options
      };
    }
    let cached = await this.query(hostname);
    if (options.family === 6) {
      const filtered = cached.filter((entry) => entry.family === 6);
      if (options.hints & import_node_dns.V4MAPPED) {
        if (supportsALL && options.hints & import_node_dns.ALL || filtered.length === 0) {
          map4to6(cached);
        } else {
          cached = filtered;
        }
      } else {
        cached = filtered;
      }
    } else if (options.family === 4) {
      cached = cached.filter((entry) => entry.family === 4);
    }
    if (options.hints & import_node_dns.ADDRCONFIG) {
      const { _iface } = this;
      cached = cached.filter((entry) => entry.family === 6 ? _iface.has6 : _iface.has4);
    }
    if (cached.length === 0) {
      const error = new Error(`cacheableLookup ENOTFOUND ${hostname}`);
      error.code = "ENOTFOUND";
      error.hostname = hostname;
      throw error;
    }
    if (options.all) {
      return cached;
    }
    return cached[0];
  }
  async query(hostname) {
    let source = "cache";
    let cached = await this._cache.get(hostname);
    if (cached) {
      this.stats.cache++;
    }
    if (!cached) {
      const pending = this._pending[hostname];
      if (pending) {
        this.stats.cache++;
        cached = await pending;
      } else {
        source = "query";
        const newPromise = this.queryAndCache(hostname);
        this._pending[hostname] = newPromise;
        this.stats.query++;
        try {
          cached = await newPromise;
        } finally {
          delete this._pending[hostname];
        }
      }
    }
    cached = cached.map((entry) => {
      return { ...entry, source };
    });
    return cached;
  }
  async _resolve(hostname) {
    const [A, AAAA] = await Promise.all([
      ignoreNoResultErrors(this._resolve4(hostname, ttl)),
      ignoreNoResultErrors(this._resolve6(hostname, ttl))
    ]);
    let aTtl = 0;
    let aaaaTtl = 0;
    let cacheTtl = 0;
    const now = Date.now();
    for (const entry of A) {
      entry.family = 4;
      entry.expires = now + entry.ttl * 1e3;
      aTtl = Math.max(aTtl, entry.ttl);
    }
    for (const entry of AAAA) {
      entry.family = 6;
      entry.expires = now + entry.ttl * 1e3;
      aaaaTtl = Math.max(aaaaTtl, entry.ttl);
    }
    if (A.length > 0) {
      if (AAAA.length > 0) {
        cacheTtl = Math.min(aTtl, aaaaTtl);
      } else {
        cacheTtl = aTtl;
      }
    } else {
      cacheTtl = aaaaTtl;
    }
    return {
      entries: [
        ...A,
        ...AAAA
      ],
      cacheTtl
    };
  }
  async _lookup(hostname) {
    try {
      const [A, AAAA] = await Promise.all([
        ignoreNoResultErrors(this._dnsLookup(hostname, all4)),
        ignoreNoResultErrors(this._dnsLookup(hostname, all6))
      ]);
      return {
        entries: [
          ...A,
          ...AAAA
        ],
        cacheTtl: 0
      };
    } catch {
      return {
        entries: [],
        cacheTtl: 0
      };
    }
  }
  async _set(hostname, data, cacheTtl) {
    if (this.maxTtl > 0 && cacheTtl > 0) {
      cacheTtl = Math.min(cacheTtl, this.maxTtl) * 1e3;
      data[kExpires] = Date.now() + cacheTtl;
      try {
        await this._cache.set(hostname, data, cacheTtl);
      } catch (error) {
        this.lookupAsync = async () => {
          const cacheError = new Error("Cache Error. Please recreate the CacheableLookup instance.");
          cacheError.cause = error;
          throw cacheError;
        };
      }
      if (isIterable(this._cache)) {
        this._tick(cacheTtl);
      }
    }
  }
  async queryAndCache(hostname) {
    if (this._hostnamesToFallback.has(hostname)) {
      return this._dnsLookup(hostname, all);
    }
    let query = await this._resolve(hostname);
    if (query.entries.length === 0 && this._dnsLookup) {
      query = await this._lookup(hostname);
      if (query.entries.length !== 0 && this.fallbackDuration > 0) {
        this._hostnamesToFallback.add(hostname);
      }
    }
    const cacheTtl = query.entries.length === 0 ? this.errorTtl : query.cacheTtl;
    await this._set(hostname, query.entries, cacheTtl);
    return query.entries;
  }
  _tick(ms) {
    const nextRemovalTime = this._nextRemovalTime;
    if (!nextRemovalTime || ms < nextRemovalTime) {
      clearTimeout(this._removalTimeout);
      this._nextRemovalTime = ms;
      this._removalTimeout = setTimeout(() => {
        this._nextRemovalTime = false;
        let nextExpiry = Infinity;
        const now = Date.now();
        for (const [hostname, entries2] of this._cache) {
          const expires = entries2[kExpires];
          if (now >= expires) {
            this._cache.delete(hostname);
          } else if (expires < nextExpiry) {
            nextExpiry = expires;
          }
        }
        if (nextExpiry !== Infinity) {
          this._tick(nextExpiry - now);
        }
      }, ms);
      if (this._removalTimeout.unref) {
        this._removalTimeout.unref();
      }
    }
  }
  install(agent) {
    verifyAgent(agent);
    if (kCacheableLookupCreateConnection in agent) {
      throw new Error("CacheableLookup has been already installed");
    }
    agent[kCacheableLookupCreateConnection] = agent.createConnection;
    agent[kCacheableLookupInstance] = this;
    agent.createConnection = (options, callback) => {
      if (!("lookup" in options)) {
        options.lookup = this.lookup;
      }
      return agent[kCacheableLookupCreateConnection](options, callback);
    };
  }
  uninstall(agent) {
    verifyAgent(agent);
    if (agent[kCacheableLookupCreateConnection]) {
      if (agent[kCacheableLookupInstance] !== this) {
        throw new Error("The agent is not owned by this CacheableLookup instance");
      }
      agent.createConnection = agent[kCacheableLookupCreateConnection];
      delete agent[kCacheableLookupCreateConnection];
      delete agent[kCacheableLookupInstance];
    }
  }
  updateInterfaceInfo() {
    const { _iface } = this;
    this._iface = getIfaceInfo();
    if (_iface.has4 && !this._iface.has4 || _iface.has6 && !this._iface.has6) {
      this._cache.clear();
    }
  }
  clear(hostname) {
    if (hostname) {
      this._cache.delete(hostname);
      return;
    }
    this._cache.clear();
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/options.js
var import_http2_wrapper = __toESM(require_source2(), 1);

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/parse-link-header.js
init_cjs_shims();
function parseLinkHeader(link) {
  const parsed = [];
  const items = link.split(",");
  for (const item of items) {
    const [rawUriReference, ...rawLinkParameters] = item.split(";");
    const trimmedUriReference = rawUriReference.trim();
    if (trimmedUriReference[0] !== "<" || trimmedUriReference[trimmedUriReference.length - 1] !== ">") {
      throw new Error(`Invalid format of the Link header reference: ${trimmedUriReference}`);
    }
    const reference = trimmedUriReference.slice(1, -1);
    const parameters = {};
    if (rawLinkParameters.length === 0) {
      throw new Error(`Unexpected end of Link header parameters: ${rawLinkParameters.join(";")}`);
    }
    for (const rawParameter of rawLinkParameters) {
      const trimmedRawParameter = rawParameter.trim();
      const center = trimmedRawParameter.indexOf("=");
      if (center === -1) {
        throw new Error(`Failed to parse Link header: ${link}`);
      }
      const name = trimmedRawParameter.slice(0, center).trim();
      const value = trimmedRawParameter.slice(center + 1).trim();
      parameters[name] = value;
    }
    parsed.push({
      reference,
      parameters
    });
  }
  return parsed;
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/options.js
var [major, minor] = import_node_process2.default.versions.node.split(".").map(Number);
function validateSearchParameters(searchParameters) {
  for (const key in searchParameters) {
    const value = searchParameters[key];
    assert.any([dist_default.string, dist_default.number, dist_default.boolean, dist_default.null_, dist_default.undefined], value);
  }
}
var globalCache = /* @__PURE__ */ new Map();
var globalDnsCache;
var getGlobalDnsCache = () => {
  if (globalDnsCache) {
    return globalDnsCache;
  }
  globalDnsCache = new CacheableLookup();
  return globalDnsCache;
};
var defaultInternals = {
  request: void 0,
  agent: {
    http: void 0,
    https: void 0,
    http2: void 0
  },
  h2session: void 0,
  decompress: true,
  timeout: {
    connect: void 0,
    lookup: void 0,
    read: void 0,
    request: void 0,
    response: void 0,
    secureConnect: void 0,
    send: void 0,
    socket: void 0
  },
  prefixUrl: "",
  body: void 0,
  form: void 0,
  json: void 0,
  cookieJar: void 0,
  ignoreInvalidCookies: false,
  searchParams: void 0,
  dnsLookup: void 0,
  dnsCache: void 0,
  context: {},
  hooks: {
    init: [],
    beforeRequest: [],
    beforeError: [],
    beforeRedirect: [],
    beforeRetry: [],
    afterResponse: []
  },
  followRedirect: true,
  maxRedirects: 10,
  cache: void 0,
  throwHttpErrors: true,
  username: "",
  password: "",
  http2: false,
  allowGetBody: false,
  headers: {
    "user-agent": "got (https://github.com/sindresorhus/got)"
  },
  methodRewriting: false,
  dnsLookupIpVersion: void 0,
  parseJson: JSON.parse,
  stringifyJson: JSON.stringify,
  retry: {
    limit: 2,
    methods: [
      "GET",
      "PUT",
      "HEAD",
      "DELETE",
      "OPTIONS",
      "TRACE"
    ],
    statusCodes: [
      408,
      413,
      429,
      500,
      502,
      503,
      504,
      521,
      522,
      524
    ],
    errorCodes: [
      "ETIMEDOUT",
      "ECONNRESET",
      "EADDRINUSE",
      "ECONNREFUSED",
      "EPIPE",
      "ENOTFOUND",
      "ENETUNREACH",
      "EAI_AGAIN"
    ],
    maxRetryAfter: void 0,
    calculateDelay: ({ computedValue }) => computedValue,
    backoffLimit: Number.POSITIVE_INFINITY,
    noise: 100
  },
  localAddress: void 0,
  method: "GET",
  createConnection: void 0,
  cacheOptions: {
    shared: void 0,
    cacheHeuristic: void 0,
    immutableMinTimeToLive: void 0,
    ignoreCargoCult: void 0
  },
  https: {
    alpnProtocols: void 0,
    rejectUnauthorized: void 0,
    checkServerIdentity: void 0,
    certificateAuthority: void 0,
    key: void 0,
    certificate: void 0,
    passphrase: void 0,
    pfx: void 0,
    ciphers: void 0,
    honorCipherOrder: void 0,
    minVersion: void 0,
    maxVersion: void 0,
    signatureAlgorithms: void 0,
    tlsSessionLifetime: void 0,
    dhparam: void 0,
    ecdhCurve: void 0,
    certificateRevocationLists: void 0
  },
  encoding: void 0,
  resolveBodyOnly: false,
  isStream: false,
  responseType: "text",
  url: void 0,
  pagination: {
    transform(response) {
      if (response.request.options.responseType === "json") {
        return response.body;
      }
      return JSON.parse(response.body);
    },
    paginate({ response }) {
      const rawLinkHeader = response.headers.link;
      if (typeof rawLinkHeader !== "string" || rawLinkHeader.trim() === "") {
        return false;
      }
      const parsed = parseLinkHeader(rawLinkHeader);
      const next = parsed.find((entry) => entry.parameters.rel === "next" || entry.parameters.rel === '"next"');
      if (next) {
        return {
          url: new import_node_url2.URL(next.reference, response.url)
        };
      }
      return false;
    },
    filter: () => true,
    shouldContinue: () => true,
    countLimit: Number.POSITIVE_INFINITY,
    backoff: 0,
    requestLimit: 1e4,
    stackAllItems: false
  },
  setHost: true,
  maxHeaderSize: void 0,
  signal: void 0,
  enableUnixSockets: true
};
var cloneInternals = (internals) => {
  const { hooks, retry } = internals;
  const result = {
    ...internals,
    context: { ...internals.context },
    cacheOptions: { ...internals.cacheOptions },
    https: { ...internals.https },
    agent: { ...internals.agent },
    headers: { ...internals.headers },
    retry: {
      ...retry,
      errorCodes: [...retry.errorCodes],
      methods: [...retry.methods],
      statusCodes: [...retry.statusCodes]
    },
    timeout: { ...internals.timeout },
    hooks: {
      init: [...hooks.init],
      beforeRequest: [...hooks.beforeRequest],
      beforeError: [...hooks.beforeError],
      beforeRedirect: [...hooks.beforeRedirect],
      beforeRetry: [...hooks.beforeRetry],
      afterResponse: [...hooks.afterResponse]
    },
    searchParams: internals.searchParams ? new import_node_url2.URLSearchParams(internals.searchParams) : void 0,
    pagination: { ...internals.pagination }
  };
  if (result.url !== void 0) {
    result.prefixUrl = "";
  }
  return result;
};
var cloneRaw = (raw) => {
  const { hooks, retry } = raw;
  const result = { ...raw };
  if (dist_default.object(raw.context)) {
    result.context = { ...raw.context };
  }
  if (dist_default.object(raw.cacheOptions)) {
    result.cacheOptions = { ...raw.cacheOptions };
  }
  if (dist_default.object(raw.https)) {
    result.https = { ...raw.https };
  }
  if (dist_default.object(raw.cacheOptions)) {
    result.cacheOptions = { ...result.cacheOptions };
  }
  if (dist_default.object(raw.agent)) {
    result.agent = { ...raw.agent };
  }
  if (dist_default.object(raw.headers)) {
    result.headers = { ...raw.headers };
  }
  if (dist_default.object(retry)) {
    result.retry = { ...retry };
    if (dist_default.array(retry.errorCodes)) {
      result.retry.errorCodes = [...retry.errorCodes];
    }
    if (dist_default.array(retry.methods)) {
      result.retry.methods = [...retry.methods];
    }
    if (dist_default.array(retry.statusCodes)) {
      result.retry.statusCodes = [...retry.statusCodes];
    }
  }
  if (dist_default.object(raw.timeout)) {
    result.timeout = { ...raw.timeout };
  }
  if (dist_default.object(hooks)) {
    result.hooks = {
      ...hooks
    };
    if (dist_default.array(hooks.init)) {
      result.hooks.init = [...hooks.init];
    }
    if (dist_default.array(hooks.beforeRequest)) {
      result.hooks.beforeRequest = [...hooks.beforeRequest];
    }
    if (dist_default.array(hooks.beforeError)) {
      result.hooks.beforeError = [...hooks.beforeError];
    }
    if (dist_default.array(hooks.beforeRedirect)) {
      result.hooks.beforeRedirect = [...hooks.beforeRedirect];
    }
    if (dist_default.array(hooks.beforeRetry)) {
      result.hooks.beforeRetry = [...hooks.beforeRetry];
    }
    if (dist_default.array(hooks.afterResponse)) {
      result.hooks.afterResponse = [...hooks.afterResponse];
    }
  }
  if (dist_default.object(raw.pagination)) {
    result.pagination = { ...raw.pagination };
  }
  return result;
};
var getHttp2TimeoutOption = (internals) => {
  const delays = [internals.timeout.socket, internals.timeout.connect, internals.timeout.lookup, internals.timeout.request, internals.timeout.secureConnect].filter((delay2) => typeof delay2 === "number");
  if (delays.length > 0) {
    return Math.min(...delays);
  }
  return void 0;
};
var init = (options, withOptions, self) => {
  const initHooks = options.hooks?.init;
  if (initHooks) {
    for (const hook of initHooks) {
      hook(withOptions, self);
    }
  }
};
var Options = class {
  constructor(input, options, defaults2) {
    Object.defineProperty(this, "_unixOptions", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_internals", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_merging", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_init", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    assert.any([dist_default.string, dist_default.urlInstance, dist_default.object, dist_default.undefined], input);
    assert.any([dist_default.object, dist_default.undefined], options);
    assert.any([dist_default.object, dist_default.undefined], defaults2);
    if (input instanceof Options || options instanceof Options) {
      throw new TypeError("The defaults must be passed as the third argument");
    }
    this._internals = cloneInternals(defaults2?._internals ?? defaults2 ?? defaultInternals);
    this._init = [...defaults2?._init ?? []];
    this._merging = false;
    this._unixOptions = void 0;
    try {
      if (dist_default.plainObject(input)) {
        try {
          this.merge(input);
          this.merge(options);
        } finally {
          this.url = input.url;
        }
      } else {
        try {
          this.merge(options);
        } finally {
          if (options?.url !== void 0) {
            if (input === void 0) {
              this.url = options.url;
            } else {
              throw new TypeError("The `url` option is mutually exclusive with the `input` argument");
            }
          } else if (input !== void 0) {
            this.url = input;
          }
        }
      }
    } catch (error) {
      error.options = this;
      throw error;
    }
  }
  merge(options) {
    if (!options) {
      return;
    }
    if (options instanceof Options) {
      for (const init2 of options._init) {
        this.merge(init2);
      }
      return;
    }
    options = cloneRaw(options);
    init(this, options, this);
    init(options, options, this);
    this._merging = true;
    if ("isStream" in options) {
      this.isStream = options.isStream;
    }
    try {
      let push = false;
      for (const key in options) {
        if (key === "mutableDefaults" || key === "handlers") {
          continue;
        }
        if (key === "url") {
          continue;
        }
        if (!(key in this)) {
          throw new Error(`Unexpected option: ${key}`);
        }
        this[key] = options[key];
        push = true;
      }
      if (push) {
        this._init.push(options);
      }
    } finally {
      this._merging = false;
    }
  }
  get request() {
    return this._internals.request;
  }
  set request(value) {
    assert.any([dist_default.function_, dist_default.undefined], value);
    this._internals.request = value;
  }
  get agent() {
    return this._internals.agent;
  }
  set agent(value) {
    assert.plainObject(value);
    for (const key in value) {
      if (!(key in this._internals.agent)) {
        throw new TypeError(`Unexpected agent option: ${key}`);
      }
      assert.any([dist_default.object, dist_default.undefined], value[key]);
    }
    if (this._merging) {
      Object.assign(this._internals.agent, value);
    } else {
      this._internals.agent = { ...value };
    }
  }
  get h2session() {
    return this._internals.h2session;
  }
  set h2session(value) {
    this._internals.h2session = value;
  }
  get decompress() {
    return this._internals.decompress;
  }
  set decompress(value) {
    assert.boolean(value);
    this._internals.decompress = value;
  }
  get timeout() {
    return this._internals.timeout;
  }
  set timeout(value) {
    assert.plainObject(value);
    for (const key in value) {
      if (!(key in this._internals.timeout)) {
        throw new Error(`Unexpected timeout option: ${key}`);
      }
      assert.any([dist_default.number, dist_default.undefined], value[key]);
    }
    if (this._merging) {
      Object.assign(this._internals.timeout, value);
    } else {
      this._internals.timeout = { ...value };
    }
  }
  get prefixUrl() {
    return this._internals.prefixUrl;
  }
  set prefixUrl(value) {
    assert.any([dist_default.string, dist_default.urlInstance], value);
    if (value === "") {
      this._internals.prefixUrl = "";
      return;
    }
    value = value.toString();
    if (!value.endsWith("/")) {
      value += "/";
    }
    if (this._internals.prefixUrl && this._internals.url) {
      const { href } = this._internals.url;
      this._internals.url.href = value + href.slice(this._internals.prefixUrl.length);
    }
    this._internals.prefixUrl = value;
  }
  get body() {
    return this._internals.body;
  }
  set body(value) {
    assert.any([dist_default.string, dist_default.buffer, dist_default.nodeStream, dist_default.generator, dist_default.asyncGenerator, isFormData, dist_default.undefined], value);
    if (dist_default.nodeStream(value)) {
      assert.truthy(value.readable);
    }
    if (value !== void 0) {
      assert.undefined(this._internals.form);
      assert.undefined(this._internals.json);
    }
    this._internals.body = value;
  }
  get form() {
    return this._internals.form;
  }
  set form(value) {
    assert.any([dist_default.plainObject, dist_default.undefined], value);
    if (value !== void 0) {
      assert.undefined(this._internals.body);
      assert.undefined(this._internals.json);
    }
    this._internals.form = value;
  }
  get json() {
    return this._internals.json;
  }
  set json(value) {
    if (value !== void 0) {
      assert.undefined(this._internals.body);
      assert.undefined(this._internals.form);
    }
    this._internals.json = value;
  }
  get url() {
    return this._internals.url;
  }
  set url(value) {
    assert.any([dist_default.string, dist_default.urlInstance, dist_default.undefined], value);
    if (value === void 0) {
      this._internals.url = void 0;
      return;
    }
    if (dist_default.string(value) && value.startsWith("/")) {
      throw new Error("`url` must not start with a slash");
    }
    const urlString = `${this.prefixUrl}${value.toString()}`;
    const url = new import_node_url2.URL(urlString);
    this._internals.url = url;
    if (url.protocol === "unix:") {
      url.href = `http://unix${url.pathname}${url.search}`;
    }
    if (url.protocol !== "http:" && url.protocol !== "https:") {
      const error = new Error(`Unsupported protocol: ${url.protocol}`);
      error.code = "ERR_UNSUPPORTED_PROTOCOL";
      throw error;
    }
    if (this._internals.username) {
      url.username = this._internals.username;
      this._internals.username = "";
    }
    if (this._internals.password) {
      url.password = this._internals.password;
      this._internals.password = "";
    }
    if (this._internals.searchParams) {
      url.search = this._internals.searchParams.toString();
      this._internals.searchParams = void 0;
    }
    if (url.hostname === "unix") {
      if (!this._internals.enableUnixSockets) {
        throw new Error("Using UNIX domain sockets but option `enableUnixSockets` is not enabled");
      }
      const matches = /(?<socketPath>.+?):(?<path>.+)/.exec(`${url.pathname}${url.search}`);
      if (matches?.groups) {
        const { socketPath, path: path6 } = matches.groups;
        this._unixOptions = {
          socketPath,
          path: path6,
          host: ""
        };
      } else {
        this._unixOptions = void 0;
      }
      return;
    }
    this._unixOptions = void 0;
  }
  get cookieJar() {
    return this._internals.cookieJar;
  }
  set cookieJar(value) {
    assert.any([dist_default.object, dist_default.undefined], value);
    if (value === void 0) {
      this._internals.cookieJar = void 0;
      return;
    }
    let { setCookie, getCookieString } = value;
    assert.function_(setCookie);
    assert.function_(getCookieString);
    if (setCookie.length === 4 && getCookieString.length === 0) {
      setCookie = (0, import_node_util3.promisify)(setCookie.bind(value));
      getCookieString = (0, import_node_util3.promisify)(getCookieString.bind(value));
      this._internals.cookieJar = {
        setCookie,
        getCookieString
      };
    } else {
      this._internals.cookieJar = value;
    }
  }
  get signal() {
    return this._internals.signal;
  }
  set signal(value) {
    assert.object(value);
    this._internals.signal = value;
  }
  get ignoreInvalidCookies() {
    return this._internals.ignoreInvalidCookies;
  }
  set ignoreInvalidCookies(value) {
    assert.boolean(value);
    this._internals.ignoreInvalidCookies = value;
  }
  get searchParams() {
    if (this._internals.url) {
      return this._internals.url.searchParams;
    }
    if (this._internals.searchParams === void 0) {
      this._internals.searchParams = new import_node_url2.URLSearchParams();
    }
    return this._internals.searchParams;
  }
  set searchParams(value) {
    assert.any([dist_default.string, dist_default.object, dist_default.undefined], value);
    const url = this._internals.url;
    if (value === void 0) {
      this._internals.searchParams = void 0;
      if (url) {
        url.search = "";
      }
      return;
    }
    const searchParameters = this.searchParams;
    let updated;
    if (dist_default.string(value)) {
      updated = new import_node_url2.URLSearchParams(value);
    } else if (value instanceof import_node_url2.URLSearchParams) {
      updated = value;
    } else {
      validateSearchParameters(value);
      updated = new import_node_url2.URLSearchParams();
      for (const key in value) {
        const entry = value[key];
        if (entry === null) {
          updated.append(key, "");
        } else if (entry === void 0) {
          searchParameters.delete(key);
        } else {
          updated.append(key, entry);
        }
      }
    }
    if (this._merging) {
      for (const key of updated.keys()) {
        searchParameters.delete(key);
      }
      for (const [key, value2] of updated) {
        searchParameters.append(key, value2);
      }
    } else if (url) {
      url.search = searchParameters.toString();
    } else {
      this._internals.searchParams = searchParameters;
    }
  }
  get searchParameters() {
    throw new Error("The `searchParameters` option does not exist. Use `searchParams` instead.");
  }
  set searchParameters(_value) {
    throw new Error("The `searchParameters` option does not exist. Use `searchParams` instead.");
  }
  get dnsLookup() {
    return this._internals.dnsLookup;
  }
  set dnsLookup(value) {
    assert.any([dist_default.function_, dist_default.undefined], value);
    this._internals.dnsLookup = value;
  }
  get dnsCache() {
    return this._internals.dnsCache;
  }
  set dnsCache(value) {
    assert.any([dist_default.object, dist_default.boolean, dist_default.undefined], value);
    if (value === true) {
      this._internals.dnsCache = getGlobalDnsCache();
    } else if (value === false) {
      this._internals.dnsCache = void 0;
    } else {
      this._internals.dnsCache = value;
    }
  }
  get context() {
    return this._internals.context;
  }
  set context(value) {
    assert.object(value);
    if (this._merging) {
      Object.assign(this._internals.context, value);
    } else {
      this._internals.context = { ...value };
    }
  }
  get hooks() {
    return this._internals.hooks;
  }
  set hooks(value) {
    assert.object(value);
    for (const knownHookEvent in value) {
      if (!(knownHookEvent in this._internals.hooks)) {
        throw new Error(`Unexpected hook event: ${knownHookEvent}`);
      }
      const typedKnownHookEvent = knownHookEvent;
      const hooks = value[typedKnownHookEvent];
      assert.any([dist_default.array, dist_default.undefined], hooks);
      if (hooks) {
        for (const hook of hooks) {
          assert.function_(hook);
        }
      }
      if (this._merging) {
        if (hooks) {
          this._internals.hooks[typedKnownHookEvent].push(...hooks);
        }
      } else {
        if (!hooks) {
          throw new Error(`Missing hook event: ${knownHookEvent}`);
        }
        this._internals.hooks[knownHookEvent] = [...hooks];
      }
    }
  }
  get followRedirect() {
    return this._internals.followRedirect;
  }
  set followRedirect(value) {
    assert.boolean(value);
    this._internals.followRedirect = value;
  }
  get followRedirects() {
    throw new TypeError("The `followRedirects` option does not exist. Use `followRedirect` instead.");
  }
  set followRedirects(_value) {
    throw new TypeError("The `followRedirects` option does not exist. Use `followRedirect` instead.");
  }
  get maxRedirects() {
    return this._internals.maxRedirects;
  }
  set maxRedirects(value) {
    assert.number(value);
    this._internals.maxRedirects = value;
  }
  get cache() {
    return this._internals.cache;
  }
  set cache(value) {
    assert.any([dist_default.object, dist_default.string, dist_default.boolean, dist_default.undefined], value);
    if (value === true) {
      this._internals.cache = globalCache;
    } else if (value === false) {
      this._internals.cache = void 0;
    } else {
      this._internals.cache = value;
    }
  }
  get throwHttpErrors() {
    return this._internals.throwHttpErrors;
  }
  set throwHttpErrors(value) {
    assert.boolean(value);
    this._internals.throwHttpErrors = value;
  }
  get username() {
    const url = this._internals.url;
    const value = url ? url.username : this._internals.username;
    return decodeURIComponent(value);
  }
  set username(value) {
    assert.string(value);
    const url = this._internals.url;
    const fixedValue = encodeURIComponent(value);
    if (url) {
      url.username = fixedValue;
    } else {
      this._internals.username = fixedValue;
    }
  }
  get password() {
    const url = this._internals.url;
    const value = url ? url.password : this._internals.password;
    return decodeURIComponent(value);
  }
  set password(value) {
    assert.string(value);
    const url = this._internals.url;
    const fixedValue = encodeURIComponent(value);
    if (url) {
      url.password = fixedValue;
    } else {
      this._internals.password = fixedValue;
    }
  }
  get http2() {
    return this._internals.http2;
  }
  set http2(value) {
    assert.boolean(value);
    this._internals.http2 = value;
  }
  get allowGetBody() {
    return this._internals.allowGetBody;
  }
  set allowGetBody(value) {
    assert.boolean(value);
    this._internals.allowGetBody = value;
  }
  get headers() {
    return this._internals.headers;
  }
  set headers(value) {
    assert.plainObject(value);
    if (this._merging) {
      Object.assign(this._internals.headers, lowercaseKeys(value));
    } else {
      this._internals.headers = lowercaseKeys(value);
    }
  }
  get methodRewriting() {
    return this._internals.methodRewriting;
  }
  set methodRewriting(value) {
    assert.boolean(value);
    this._internals.methodRewriting = value;
  }
  get dnsLookupIpVersion() {
    return this._internals.dnsLookupIpVersion;
  }
  set dnsLookupIpVersion(value) {
    if (value !== void 0 && value !== 4 && value !== 6) {
      throw new TypeError(`Invalid DNS lookup IP version: ${value}`);
    }
    this._internals.dnsLookupIpVersion = value;
  }
  get parseJson() {
    return this._internals.parseJson;
  }
  set parseJson(value) {
    assert.function_(value);
    this._internals.parseJson = value;
  }
  get stringifyJson() {
    return this._internals.stringifyJson;
  }
  set stringifyJson(value) {
    assert.function_(value);
    this._internals.stringifyJson = value;
  }
  get retry() {
    return this._internals.retry;
  }
  set retry(value) {
    assert.plainObject(value);
    assert.any([dist_default.function_, dist_default.undefined], value.calculateDelay);
    assert.any([dist_default.number, dist_default.undefined], value.maxRetryAfter);
    assert.any([dist_default.number, dist_default.undefined], value.limit);
    assert.any([dist_default.array, dist_default.undefined], value.methods);
    assert.any([dist_default.array, dist_default.undefined], value.statusCodes);
    assert.any([dist_default.array, dist_default.undefined], value.errorCodes);
    assert.any([dist_default.number, dist_default.undefined], value.noise);
    if (value.noise && Math.abs(value.noise) > 100) {
      throw new Error(`The maximum acceptable retry noise is +/- 100ms, got ${value.noise}`);
    }
    for (const key in value) {
      if (!(key in this._internals.retry)) {
        throw new Error(`Unexpected retry option: ${key}`);
      }
    }
    if (this._merging) {
      Object.assign(this._internals.retry, value);
    } else {
      this._internals.retry = { ...value };
    }
    const { retry } = this._internals;
    retry.methods = [...new Set(retry.methods.map((method) => method.toUpperCase()))];
    retry.statusCodes = [...new Set(retry.statusCodes)];
    retry.errorCodes = [...new Set(retry.errorCodes)];
  }
  get localAddress() {
    return this._internals.localAddress;
  }
  set localAddress(value) {
    assert.any([dist_default.string, dist_default.undefined], value);
    this._internals.localAddress = value;
  }
  get method() {
    return this._internals.method;
  }
  set method(value) {
    assert.string(value);
    this._internals.method = value.toUpperCase();
  }
  get createConnection() {
    return this._internals.createConnection;
  }
  set createConnection(value) {
    assert.any([dist_default.function_, dist_default.undefined], value);
    this._internals.createConnection = value;
  }
  get cacheOptions() {
    return this._internals.cacheOptions;
  }
  set cacheOptions(value) {
    assert.plainObject(value);
    assert.any([dist_default.boolean, dist_default.undefined], value.shared);
    assert.any([dist_default.number, dist_default.undefined], value.cacheHeuristic);
    assert.any([dist_default.number, dist_default.undefined], value.immutableMinTimeToLive);
    assert.any([dist_default.boolean, dist_default.undefined], value.ignoreCargoCult);
    for (const key in value) {
      if (!(key in this._internals.cacheOptions)) {
        throw new Error(`Cache option \`${key}\` does not exist`);
      }
    }
    if (this._merging) {
      Object.assign(this._internals.cacheOptions, value);
    } else {
      this._internals.cacheOptions = { ...value };
    }
  }
  get https() {
    return this._internals.https;
  }
  set https(value) {
    assert.plainObject(value);
    assert.any([dist_default.boolean, dist_default.undefined], value.rejectUnauthorized);
    assert.any([dist_default.function_, dist_default.undefined], value.checkServerIdentity);
    assert.any([dist_default.string, dist_default.object, dist_default.array, dist_default.undefined], value.certificateAuthority);
    assert.any([dist_default.string, dist_default.object, dist_default.array, dist_default.undefined], value.key);
    assert.any([dist_default.string, dist_default.object, dist_default.array, dist_default.undefined], value.certificate);
    assert.any([dist_default.string, dist_default.undefined], value.passphrase);
    assert.any([dist_default.string, dist_default.buffer, dist_default.array, dist_default.undefined], value.pfx);
    assert.any([dist_default.array, dist_default.undefined], value.alpnProtocols);
    assert.any([dist_default.string, dist_default.undefined], value.ciphers);
    assert.any([dist_default.string, dist_default.buffer, dist_default.undefined], value.dhparam);
    assert.any([dist_default.string, dist_default.undefined], value.signatureAlgorithms);
    assert.any([dist_default.string, dist_default.undefined], value.minVersion);
    assert.any([dist_default.string, dist_default.undefined], value.maxVersion);
    assert.any([dist_default.boolean, dist_default.undefined], value.honorCipherOrder);
    assert.any([dist_default.number, dist_default.undefined], value.tlsSessionLifetime);
    assert.any([dist_default.string, dist_default.undefined], value.ecdhCurve);
    assert.any([dist_default.string, dist_default.buffer, dist_default.array, dist_default.undefined], value.certificateRevocationLists);
    for (const key in value) {
      if (!(key in this._internals.https)) {
        throw new Error(`HTTPS option \`${key}\` does not exist`);
      }
    }
    if (this._merging) {
      Object.assign(this._internals.https, value);
    } else {
      this._internals.https = { ...value };
    }
  }
  get encoding() {
    return this._internals.encoding;
  }
  set encoding(value) {
    if (value === null) {
      throw new TypeError("To get a Buffer, set `options.responseType` to `buffer` instead");
    }
    assert.any([dist_default.string, dist_default.undefined], value);
    this._internals.encoding = value;
  }
  get resolveBodyOnly() {
    return this._internals.resolveBodyOnly;
  }
  set resolveBodyOnly(value) {
    assert.boolean(value);
    this._internals.resolveBodyOnly = value;
  }
  get isStream() {
    return this._internals.isStream;
  }
  set isStream(value) {
    assert.boolean(value);
    this._internals.isStream = value;
  }
  get responseType() {
    return this._internals.responseType;
  }
  set responseType(value) {
    if (value === void 0) {
      this._internals.responseType = "text";
      return;
    }
    if (value !== "text" && value !== "buffer" && value !== "json") {
      throw new Error(`Invalid \`responseType\` option: ${value}`);
    }
    this._internals.responseType = value;
  }
  get pagination() {
    return this._internals.pagination;
  }
  set pagination(value) {
    assert.object(value);
    if (this._merging) {
      Object.assign(this._internals.pagination, value);
    } else {
      this._internals.pagination = value;
    }
  }
  get auth() {
    throw new Error("Parameter `auth` is deprecated. Use `username` / `password` instead.");
  }
  set auth(_value) {
    throw new Error("Parameter `auth` is deprecated. Use `username` / `password` instead.");
  }
  get setHost() {
    return this._internals.setHost;
  }
  set setHost(value) {
    assert.boolean(value);
    this._internals.setHost = value;
  }
  get maxHeaderSize() {
    return this._internals.maxHeaderSize;
  }
  set maxHeaderSize(value) {
    assert.any([dist_default.number, dist_default.undefined], value);
    this._internals.maxHeaderSize = value;
  }
  get enableUnixSockets() {
    return this._internals.enableUnixSockets;
  }
  set enableUnixSockets(value) {
    assert.boolean(value);
    this._internals.enableUnixSockets = value;
  }
  toJSON() {
    return { ...this._internals };
  }
  [Symbol.for("nodejs.util.inspect.custom")](_depth, options) {
    return (0, import_node_util3.inspect)(this._internals, options);
  }
  createNativeRequestOptions() {
    const internals = this._internals;
    const url = internals.url;
    let agent;
    if (url.protocol === "https:") {
      agent = internals.http2 ? internals.agent : internals.agent.https;
    } else {
      agent = internals.agent.http;
    }
    const { https: https2 } = internals;
    let { pfx } = https2;
    if (dist_default.array(pfx) && dist_default.plainObject(pfx[0])) {
      pfx = pfx.map((object) => ({
        buf: object.buffer,
        passphrase: object.passphrase
      }));
    }
    return {
      ...internals.cacheOptions,
      ...this._unixOptions,
      ALPNProtocols: https2.alpnProtocols,
      ca: https2.certificateAuthority,
      cert: https2.certificate,
      key: https2.key,
      passphrase: https2.passphrase,
      pfx: https2.pfx,
      rejectUnauthorized: https2.rejectUnauthorized,
      checkServerIdentity: https2.checkServerIdentity ?? import_node_tls.checkServerIdentity,
      ciphers: https2.ciphers,
      honorCipherOrder: https2.honorCipherOrder,
      minVersion: https2.minVersion,
      maxVersion: https2.maxVersion,
      sigalgs: https2.signatureAlgorithms,
      sessionTimeout: https2.tlsSessionLifetime,
      dhparam: https2.dhparam,
      ecdhCurve: https2.ecdhCurve,
      crl: https2.certificateRevocationLists,
      lookup: internals.dnsLookup ?? internals.dnsCache?.lookup,
      family: internals.dnsLookupIpVersion,
      agent,
      setHost: internals.setHost,
      method: internals.method,
      maxHeaderSize: internals.maxHeaderSize,
      localAddress: internals.localAddress,
      headers: internals.headers,
      createConnection: internals.createConnection,
      timeout: internals.http2 ? getHttp2TimeoutOption(internals) : void 0,
      h2session: internals.h2session
    };
  }
  getRequestFunction() {
    const url = this._internals.url;
    const { request } = this._internals;
    if (!request && url) {
      return this.getFallbackRequestFunction();
    }
    return request;
  }
  getFallbackRequestFunction() {
    const url = this._internals.url;
    if (!url) {
      return;
    }
    if (url.protocol === "https:") {
      if (this._internals.http2) {
        if (major < 15 || major === 15 && minor < 10) {
          const error = new Error("To use the `http2` option, install Node.js 15.10.0 or above");
          error.code = "EUNSUPPORTED";
          throw error;
        }
        return import_http2_wrapper.default.auto;
      }
      return import_node_https.default.request;
    }
    return import_node_http.default.request;
  }
  freeze() {
    const options = this._internals;
    Object.freeze(options);
    Object.freeze(options.hooks);
    Object.freeze(options.hooks.afterResponse);
    Object.freeze(options.hooks.beforeError);
    Object.freeze(options.hooks.beforeRedirect);
    Object.freeze(options.hooks.beforeRequest);
    Object.freeze(options.hooks.beforeRetry);
    Object.freeze(options.hooks.init);
    Object.freeze(options.https);
    Object.freeze(options.cacheOptions);
    Object.freeze(options.agent);
    Object.freeze(options.headers);
    Object.freeze(options.timeout);
    Object.freeze(options.retry);
    Object.freeze(options.retry.errorCodes);
    Object.freeze(options.retry.methods);
    Object.freeze(options.retry.statusCodes);
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/response.js
init_cjs_shims();
var isResponseOk = (response) => {
  const { statusCode } = response;
  const limitStatusCode = response.request.options.followRedirect ? 299 : 399;
  return statusCode >= 200 && statusCode <= limitStatusCode || statusCode === 304;
};
var ParseError = class extends RequestError {
  constructor(error, response) {
    const { options } = response.request;
    super(`${error.message} in "${options.url.toString()}"`, error, response.request);
    this.name = "ParseError";
    this.code = "ERR_BODY_PARSE_FAILURE";
  }
};
var parseBody = (response, responseType, parseJson, encoding) => {
  const { rawBody } = response;
  try {
    if (responseType === "text") {
      return rawBody.toString(encoding);
    }
    if (responseType === "json") {
      return rawBody.length === 0 ? "" : parseJson(rawBody.toString(encoding));
    }
    if (responseType === "buffer") {
      return rawBody;
    }
  } catch (error) {
    throw new ParseError(error, response);
  }
  throw new ParseError({
    message: `Unknown body type '${responseType}'`,
    name: "Error"
  }, response);
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/is-client-request.js
init_cjs_shims();
function isClientRequest(clientRequest) {
  return clientRequest.writable && !clientRequest.writableEnded;
}
var is_client_request_default = isClientRequest;

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/utils/is-unix-socket-url.js
init_cjs_shims();
function isUnixSocketURL(url) {
  return url.protocol === "unix:" || url.hostname === "unix";
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/core/index.js
var supportsBrotli = dist_default.string(import_node_process3.default.versions.brotli);
var methodsWithoutBody = /* @__PURE__ */ new Set(["GET", "HEAD"]);
var cacheableStore = new WeakableMap();
var redirectCodes = /* @__PURE__ */ new Set([300, 301, 302, 303, 304, 307, 308]);
var proxiedRequestEvents = [
  "socket",
  "connect",
  "continue",
  "information",
  "upgrade"
];
var noop2 = () => {
};
var Request = class extends import_node_stream3.Duplex {
  constructor(url, options, defaults2) {
    super({
      autoDestroy: false,
      highWaterMark: 0
    });
    Object.defineProperty(this, "constructor", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_noPipe", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "options", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "response", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "requestUrl", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "redirectUrls", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "retryCount", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_stopRetry", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_downloadedSize", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_uploadedSize", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_stopReading", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_pipedServerResponses", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_request", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_responseSize", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_bodySize", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_unproxyEvents", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_isFromCache", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_cannotHaveBody", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_triggerRead", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_cancelTimeouts", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_removeListeners", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_nativeResponse", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_flushed", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_aborted", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_requestInitialized", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    this._downloadedSize = 0;
    this._uploadedSize = 0;
    this._stopReading = false;
    this._pipedServerResponses = /* @__PURE__ */ new Set();
    this._cannotHaveBody = false;
    this._unproxyEvents = noop2;
    this._triggerRead = false;
    this._cancelTimeouts = noop2;
    this._removeListeners = noop2;
    this._jobs = [];
    this._flushed = false;
    this._requestInitialized = false;
    this._aborted = false;
    this.redirectUrls = [];
    this.retryCount = 0;
    this._stopRetry = noop2;
    this.on("pipe", (source) => {
      if (source.headers) {
        Object.assign(this.options.headers, source.headers);
      }
    });
    this.on("newListener", (event) => {
      if (event === "retry" && this.listenerCount("retry") > 0) {
        throw new Error("A retry listener has been attached already.");
      }
    });
    try {
      this.options = new Options(url, options, defaults2);
      if (!this.options.url) {
        if (this.options.prefixUrl === "") {
          throw new TypeError("Missing `url` property");
        }
        this.options.url = "";
      }
      this.requestUrl = this.options.url;
    } catch (error) {
      const { options: options2 } = error;
      if (options2) {
        this.options = options2;
      }
      this.flush = async () => {
        this.flush = async () => {
        };
        this.destroy(error);
      };
      return;
    }
    const { body } = this.options;
    if (dist_default.nodeStream(body)) {
      body.once("error", (error) => {
        if (this._flushed) {
          this._beforeError(new UploadError(error, this));
        } else {
          this.flush = async () => {
            this.flush = async () => {
            };
            this._beforeError(new UploadError(error, this));
          };
        }
      });
    }
    if (this.options.signal) {
      const abort = () => {
        this.destroy(new AbortError(this));
      };
      if (this.options.signal.aborted) {
        abort();
      } else {
        this.options.signal.addEventListener("abort", abort);
        this._removeListeners = () => {
          this.options.signal.removeEventListener("abort", abort);
        };
      }
    }
  }
  async flush() {
    if (this._flushed) {
      return;
    }
    this._flushed = true;
    try {
      await this._finalizeBody();
      if (this.destroyed) {
        return;
      }
      await this._makeRequest();
      if (this.destroyed) {
        this._request?.destroy();
        return;
      }
      for (const job of this._jobs) {
        job();
      }
      this._jobs.length = 0;
      this._requestInitialized = true;
    } catch (error) {
      this._beforeError(error);
    }
  }
  _beforeError(error) {
    if (this._stopReading) {
      return;
    }
    const { response, options } = this;
    const attemptCount = this.retryCount + (error.name === "RetryError" ? 0 : 1);
    this._stopReading = true;
    if (!(error instanceof RequestError)) {
      error = new RequestError(error.message, error, this);
    }
    const typedError = error;
    void (async () => {
      if (response?.readable && !response.rawBody && !this._request?.socket?.destroyed) {
        response.setEncoding(this.readableEncoding);
        const success = await this._setRawBody(response);
        if (success) {
          response.body = response.rawBody.toString();
        }
      }
      if (this.listenerCount("retry") !== 0) {
        let backoff;
        try {
          let retryAfter;
          if (response && "retry-after" in response.headers) {
            retryAfter = Number(response.headers["retry-after"]);
            if (Number.isNaN(retryAfter)) {
              retryAfter = Date.parse(response.headers["retry-after"]) - Date.now();
              if (retryAfter <= 0) {
                retryAfter = 1;
              }
            } else {
              retryAfter *= 1e3;
            }
          }
          const retryOptions = options.retry;
          backoff = await retryOptions.calculateDelay({
            attemptCount,
            retryOptions,
            error: typedError,
            retryAfter,
            computedValue: calculate_retry_delay_default({
              attemptCount,
              retryOptions,
              error: typedError,
              retryAfter,
              computedValue: retryOptions.maxRetryAfter ?? options.timeout.request ?? Number.POSITIVE_INFINITY
            })
          });
        } catch (error_) {
          void this._error(new RequestError(error_.message, error_, this));
          return;
        }
        if (backoff) {
          await new Promise((resolve) => {
            const timeout = setTimeout(resolve, backoff);
            this._stopRetry = () => {
              clearTimeout(timeout);
              resolve();
            };
          });
          if (this.destroyed) {
            return;
          }
          try {
            for (const hook of this.options.hooks.beforeRetry) {
              await hook(typedError, this.retryCount + 1);
            }
          } catch (error_) {
            void this._error(new RequestError(error_.message, error, this));
            return;
          }
          if (this.destroyed) {
            return;
          }
          this.destroy();
          this.emit("retry", this.retryCount + 1, error, (updatedOptions) => {
            const request = new Request(options.url, updatedOptions, options);
            request.retryCount = this.retryCount + 1;
            import_node_process3.default.nextTick(() => {
              void request.flush();
            });
            return request;
          });
          return;
        }
      }
      void this._error(typedError);
    })();
  }
  _read() {
    this._triggerRead = true;
    const { response } = this;
    if (response && !this._stopReading) {
      if (response.readableLength) {
        this._triggerRead = false;
      }
      let data;
      while ((data = response.read()) !== null) {
        this._downloadedSize += data.length;
        const progress = this.downloadProgress;
        if (progress.percent < 1) {
          this.emit("downloadProgress", progress);
        }
        this.push(data);
      }
    }
  }
  _write(chunk, encoding, callback) {
    const write = () => {
      this._writeRequest(chunk, encoding, callback);
    };
    if (this._requestInitialized) {
      write();
    } else {
      this._jobs.push(write);
    }
  }
  _final(callback) {
    const endRequest = () => {
      if (!this._request || this._request.destroyed) {
        callback();
        return;
      }
      this._request.end((error) => {
        if (this._request._writableState?.errored) {
          return;
        }
        if (!error) {
          this._bodySize = this._uploadedSize;
          this.emit("uploadProgress", this.uploadProgress);
          this._request.emit("upload-complete");
        }
        callback(error);
      });
    };
    if (this._requestInitialized) {
      endRequest();
    } else {
      this._jobs.push(endRequest);
    }
  }
  _destroy(error, callback) {
    this._stopReading = true;
    this.flush = async () => {
    };
    this._stopRetry();
    this._cancelTimeouts();
    this._removeListeners();
    if (this.options) {
      const { body } = this.options;
      if (dist_default.nodeStream(body)) {
        body.destroy();
      }
    }
    if (this._request) {
      this._request.destroy();
    }
    if (error !== null && !dist_default.undefined(error) && !(error instanceof RequestError)) {
      error = new RequestError(error.message, error, this);
    }
    callback(error);
  }
  pipe(destination, options) {
    if (destination instanceof import_node_http2.ServerResponse) {
      this._pipedServerResponses.add(destination);
    }
    return super.pipe(destination, options);
  }
  unpipe(destination) {
    if (destination instanceof import_node_http2.ServerResponse) {
      this._pipedServerResponses.delete(destination);
    }
    super.unpipe(destination);
    return this;
  }
  async _finalizeBody() {
    const { options } = this;
    const { headers } = options;
    const isForm = !dist_default.undefined(options.form);
    const isJSON = !dist_default.undefined(options.json);
    const isBody = !dist_default.undefined(options.body);
    const cannotHaveBody = methodsWithoutBody.has(options.method) && !(options.method === "GET" && options.allowGetBody);
    this._cannotHaveBody = cannotHaveBody;
    if (isForm || isJSON || isBody) {
      if (cannotHaveBody) {
        throw new TypeError(`The \`${options.method}\` method cannot be used with a body`);
      }
      const noContentType = !dist_default.string(headers["content-type"]);
      if (isBody) {
        if (isFormData(options.body)) {
          const encoder = new FormDataEncoder(options.body);
          if (noContentType) {
            headers["content-type"] = encoder.headers["Content-Type"];
          }
          if ("Content-Length" in encoder.headers) {
            headers["content-length"] = encoder.headers["Content-Length"];
          }
          options.body = encoder.encode();
        }
        if (isFormData2(options.body) && noContentType) {
          headers["content-type"] = `multipart/form-data; boundary=${options.body.getBoundary()}`;
        }
      } else if (isForm) {
        if (noContentType) {
          headers["content-type"] = "application/x-www-form-urlencoded";
        }
        const { form } = options;
        options.form = void 0;
        options.body = new import_node_url3.URLSearchParams(form).toString();
      } else {
        if (noContentType) {
          headers["content-type"] = "application/json";
        }
        const { json } = options;
        options.json = void 0;
        options.body = options.stringifyJson(json);
      }
      const uploadBodySize = await getBodySize(options.body, options.headers);
      if (dist_default.undefined(headers["content-length"]) && dist_default.undefined(headers["transfer-encoding"]) && !cannotHaveBody && !dist_default.undefined(uploadBodySize)) {
        headers["content-length"] = String(uploadBodySize);
      }
    }
    if (options.responseType === "json" && !("accept" in options.headers)) {
      options.headers.accept = "application/json";
    }
    this._bodySize = Number(headers["content-length"]) || void 0;
  }
  async _onResponseBase(response) {
    if (this.isAborted) {
      return;
    }
    const { options } = this;
    const { url } = options;
    this._nativeResponse = response;
    if (options.decompress) {
      response = (0, import_decompress_response.default)(response);
    }
    const statusCode = response.statusCode;
    const typedResponse = response;
    typedResponse.statusMessage = typedResponse.statusMessage ?? import_node_http2.default.STATUS_CODES[statusCode];
    typedResponse.url = options.url.toString();
    typedResponse.requestUrl = this.requestUrl;
    typedResponse.redirectUrls = this.redirectUrls;
    typedResponse.request = this;
    typedResponse.isFromCache = this._nativeResponse.fromCache ?? false;
    typedResponse.ip = this.ip;
    typedResponse.retryCount = this.retryCount;
    typedResponse.ok = isResponseOk(typedResponse);
    this._isFromCache = typedResponse.isFromCache;
    this._responseSize = Number(response.headers["content-length"]) || void 0;
    this.response = typedResponse;
    response.once("end", () => {
      this._responseSize = this._downloadedSize;
      this.emit("downloadProgress", this.downloadProgress);
    });
    response.once("error", (error) => {
      this._aborted = true;
      response.destroy();
      this._beforeError(new ReadError(error, this));
    });
    response.once("aborted", () => {
      this._aborted = true;
      this._beforeError(new ReadError({
        name: "Error",
        message: "The server aborted pending request",
        code: "ECONNRESET"
      }, this));
    });
    this.emit("downloadProgress", this.downloadProgress);
    const rawCookies = response.headers["set-cookie"];
    if (dist_default.object(options.cookieJar) && rawCookies) {
      let promises = rawCookies.map(async (rawCookie) => options.cookieJar.setCookie(rawCookie, url.toString()));
      if (options.ignoreInvalidCookies) {
        promises = promises.map(async (promise) => {
          try {
            await promise;
          } catch {
          }
        });
      }
      try {
        await Promise.all(promises);
      } catch (error) {
        this._beforeError(error);
        return;
      }
    }
    if (this.isAborted) {
      return;
    }
    if (options.followRedirect && response.headers.location && redirectCodes.has(statusCode)) {
      response.resume();
      this._cancelTimeouts();
      this._unproxyEvents();
      if (this.redirectUrls.length >= options.maxRedirects) {
        this._beforeError(new MaxRedirectsError(this));
        return;
      }
      this._request = void 0;
      const updatedOptions = new Options(void 0, void 0, this.options);
      const serverRequestedGet = statusCode === 303 && updatedOptions.method !== "GET" && updatedOptions.method !== "HEAD";
      const canRewrite = statusCode !== 307 && statusCode !== 308;
      const userRequestedGet = updatedOptions.methodRewriting && canRewrite;
      if (serverRequestedGet || userRequestedGet) {
        updatedOptions.method = "GET";
        updatedOptions.body = void 0;
        updatedOptions.json = void 0;
        updatedOptions.form = void 0;
        delete updatedOptions.headers["content-length"];
      }
      try {
        const redirectBuffer = import_node_buffer2.Buffer.from(response.headers.location, "binary").toString();
        const redirectUrl = new import_node_url3.URL(redirectBuffer, url);
        if (!isUnixSocketURL(url) && isUnixSocketURL(redirectUrl)) {
          this._beforeError(new RequestError("Cannot redirect to UNIX socket", {}, this));
          return;
        }
        if (redirectUrl.hostname !== url.hostname || redirectUrl.port !== url.port) {
          if ("host" in updatedOptions.headers) {
            delete updatedOptions.headers.host;
          }
          if ("cookie" in updatedOptions.headers) {
            delete updatedOptions.headers.cookie;
          }
          if ("authorization" in updatedOptions.headers) {
            delete updatedOptions.headers.authorization;
          }
          if (updatedOptions.username || updatedOptions.password) {
            updatedOptions.username = "";
            updatedOptions.password = "";
          }
        } else {
          redirectUrl.username = updatedOptions.username;
          redirectUrl.password = updatedOptions.password;
        }
        this.redirectUrls.push(redirectUrl);
        updatedOptions.prefixUrl = "";
        updatedOptions.url = redirectUrl;
        for (const hook of updatedOptions.hooks.beforeRedirect) {
          await hook(updatedOptions, typedResponse);
        }
        this.emit("redirect", updatedOptions, typedResponse);
        this.options = updatedOptions;
        await this._makeRequest();
      } catch (error) {
        this._beforeError(error);
        return;
      }
      return;
    }
    if (options.isStream && options.throwHttpErrors && !isResponseOk(typedResponse)) {
      this._beforeError(new HTTPError(typedResponse));
      return;
    }
    response.on("readable", () => {
      if (this._triggerRead) {
        this._read();
      }
    });
    this.on("resume", () => {
      response.resume();
    });
    this.on("pause", () => {
      response.pause();
    });
    response.once("end", () => {
      this.push(null);
    });
    if (this._noPipe) {
      const success = await this._setRawBody();
      if (success) {
        this.emit("response", response);
      }
      return;
    }
    this.emit("response", response);
    for (const destination of this._pipedServerResponses) {
      if (destination.headersSent) {
        continue;
      }
      for (const key in response.headers) {
        const isAllowed = options.decompress ? key !== "content-encoding" : true;
        const value = response.headers[key];
        if (isAllowed) {
          destination.setHeader(key, value);
        }
      }
      destination.statusCode = statusCode;
    }
  }
  async _setRawBody(from = this) {
    if (from.readableEnded) {
      return false;
    }
    try {
      const rawBody = await (0, import_get_stream2.buffer)(from);
      if (!this.isAborted) {
        this.response.rawBody = rawBody;
        return true;
      }
    } catch {
    }
    return false;
  }
  async _onResponse(response) {
    try {
      await this._onResponseBase(response);
    } catch (error) {
      this._beforeError(error);
    }
  }
  _onRequest(request) {
    const { options } = this;
    const { timeout, url } = options;
    source_default2(request);
    if (this.options.http2) {
      request.setTimeout(0);
    }
    this._cancelTimeouts = timedOut(request, timeout, url);
    const responseEventName = options.cache ? "cacheableResponse" : "response";
    request.once(responseEventName, (response) => {
      void this._onResponse(response);
    });
    request.once("error", (error) => {
      this._aborted = true;
      request.destroy();
      error = error instanceof TimeoutError2 ? new TimeoutError(error, this.timings, this) : new RequestError(error.message, error, this);
      this._beforeError(error);
    });
    this._unproxyEvents = proxyEvents(request, this, proxiedRequestEvents);
    this._request = request;
    this.emit("uploadProgress", this.uploadProgress);
    this._sendBody();
    this.emit("request", request);
  }
  async _asyncWrite(chunk) {
    return new Promise((resolve, reject) => {
      super.write(chunk, (error) => {
        if (error) {
          reject(error);
          return;
        }
        resolve();
      });
    });
  }
  _sendBody() {
    const { body } = this.options;
    const currentRequest = this.redirectUrls.length === 0 ? this : this._request ?? this;
    if (dist_default.nodeStream(body)) {
      body.pipe(currentRequest);
    } else if (dist_default.generator(body) || dist_default.asyncGenerator(body)) {
      (async () => {
        try {
          for await (const chunk of body) {
            await this._asyncWrite(chunk);
          }
          super.end();
        } catch (error) {
          this._beforeError(error);
        }
      })();
    } else if (!dist_default.undefined(body)) {
      this._writeRequest(body, void 0, () => {
      });
      currentRequest.end();
    } else if (this._cannotHaveBody || this._noPipe) {
      currentRequest.end();
    }
  }
  _prepareCache(cache) {
    if (!cacheableStore.has(cache)) {
      const cacheableRequest = new dist_default2((requestOptions, handler) => {
        const result = requestOptions._request(requestOptions, handler);
        if (dist_default.promise(result)) {
          result.once = (event, handler2) => {
            if (event === "error") {
              (async () => {
                try {
                  await result;
                } catch (error) {
                  handler2(error);
                }
              })();
            } else if (event === "abort") {
              (async () => {
                try {
                  const request = await result;
                  request.once("abort", handler2);
                } catch {
                }
              })();
            } else {
              throw new Error(`Unknown HTTP2 promise event: ${event}`);
            }
            return result;
          };
        }
        return result;
      }, cache);
      cacheableStore.set(cache, cacheableRequest.request());
    }
  }
  async _createCacheableRequest(url, options) {
    return new Promise((resolve, reject) => {
      Object.assign(options, urlToOptions(url));
      let request;
      const cacheRequest = cacheableStore.get(options.cache)(options, async (response) => {
        response._readableState.autoDestroy = false;
        if (request) {
          const fix = () => {
            if (response.req) {
              response.complete = response.req.res.complete;
            }
          };
          response.prependOnceListener("end", fix);
          fix();
          (await request).emit("cacheableResponse", response);
        }
        resolve(response);
      });
      cacheRequest.once("error", reject);
      cacheRequest.once("request", async (requestOrPromise) => {
        request = requestOrPromise;
        resolve(request);
      });
    });
  }
  async _makeRequest() {
    const { options } = this;
    const { headers, username, password } = options;
    const cookieJar = options.cookieJar;
    for (const key in headers) {
      if (dist_default.undefined(headers[key])) {
        delete headers[key];
      } else if (dist_default.null_(headers[key])) {
        throw new TypeError(`Use \`undefined\` instead of \`null\` to delete the \`${key}\` header`);
      }
    }
    if (options.decompress && dist_default.undefined(headers["accept-encoding"])) {
      headers["accept-encoding"] = supportsBrotli ? "gzip, deflate, br" : "gzip, deflate";
    }
    if (username || password) {
      const credentials = import_node_buffer2.Buffer.from(`${username}:${password}`).toString("base64");
      headers.authorization = `Basic ${credentials}`;
    }
    if (cookieJar) {
      const cookieString = await cookieJar.getCookieString(options.url.toString());
      if (dist_default.nonEmptyString(cookieString)) {
        headers.cookie = cookieString;
      }
    }
    options.prefixUrl = "";
    let request;
    for (const hook of options.hooks.beforeRequest) {
      const result = await hook(options);
      if (!dist_default.undefined(result)) {
        request = () => result;
        break;
      }
    }
    if (!request) {
      request = options.getRequestFunction();
    }
    const url = options.url;
    this._requestOptions = options.createNativeRequestOptions();
    if (options.cache) {
      this._requestOptions._request = request;
      this._requestOptions.cache = options.cache;
      this._requestOptions.body = options.body;
      this._prepareCache(options.cache);
    }
    const fn = options.cache ? this._createCacheableRequest : request;
    try {
      let requestOrResponse = fn(url, this._requestOptions);
      if (dist_default.promise(requestOrResponse)) {
        requestOrResponse = await requestOrResponse;
      }
      if (dist_default.undefined(requestOrResponse)) {
        requestOrResponse = options.getFallbackRequestFunction()(url, this._requestOptions);
        if (dist_default.promise(requestOrResponse)) {
          requestOrResponse = await requestOrResponse;
        }
      }
      if (is_client_request_default(requestOrResponse)) {
        this._onRequest(requestOrResponse);
      } else if (this.writable) {
        this.once("finish", () => {
          void this._onResponse(requestOrResponse);
        });
        this._sendBody();
      } else {
        void this._onResponse(requestOrResponse);
      }
    } catch (error) {
      if (error instanceof CacheError2) {
        throw new CacheError(error, this);
      }
      throw error;
    }
  }
  async _error(error) {
    try {
      if (error instanceof HTTPError && !this.options.throwHttpErrors) {
      } else {
        for (const hook of this.options.hooks.beforeError) {
          error = await hook(error);
        }
      }
    } catch (error_) {
      error = new RequestError(error_.message, error_, this);
    }
    this.destroy(error);
  }
  _writeRequest(chunk, encoding, callback) {
    if (!this._request || this._request.destroyed) {
      return;
    }
    this._request.write(chunk, encoding, (error) => {
      if (!error && !this._request.destroyed) {
        this._uploadedSize += import_node_buffer2.Buffer.byteLength(chunk, encoding);
        const progress = this.uploadProgress;
        if (progress.percent < 1) {
          this.emit("uploadProgress", progress);
        }
      }
      callback(error);
    });
  }
  get ip() {
    return this.socket?.remoteAddress;
  }
  get isAborted() {
    return this._aborted;
  }
  get socket() {
    return this._request?.socket ?? void 0;
  }
  get downloadProgress() {
    let percent;
    if (this._responseSize) {
      percent = this._downloadedSize / this._responseSize;
    } else if (this._responseSize === this._downloadedSize) {
      percent = 1;
    } else {
      percent = 0;
    }
    return {
      percent,
      transferred: this._downloadedSize,
      total: this._responseSize
    };
  }
  get uploadProgress() {
    let percent;
    if (this._bodySize) {
      percent = this._uploadedSize / this._bodySize;
    } else if (this._bodySize === this._uploadedSize) {
      percent = 1;
    } else {
      percent = 0;
    }
    return {
      percent,
      transferred: this._uploadedSize,
      total: this._bodySize
    };
  }
  get timings() {
    return this._request?.timings;
  }
  get isFromCache() {
    return this._isFromCache;
  }
  get reusedSocket() {
    return this._request?.reusedSocket;
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/as-promise/types.js
init_cjs_shims();
var CancelError2 = class extends RequestError {
  constructor(request) {
    super("Promise was canceled", {}, request);
    this.name = "CancelError";
    this.code = "ERR_CANCELED";
  }
  get isCanceled() {
    return true;
  }
};

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/as-promise/index.js
var proxiedRequestEvents2 = [
  "request",
  "response",
  "redirect",
  "uploadProgress",
  "downloadProgress"
];
function asPromise(firstRequest) {
  let globalRequest;
  let globalResponse;
  let normalizedOptions;
  const emitter = new import_node_events2.EventEmitter();
  const promise = new PCancelable((resolve, reject, onCancel) => {
    onCancel(() => {
      globalRequest.destroy();
    });
    onCancel.shouldReject = false;
    onCancel(() => {
      reject(new CancelError2(globalRequest));
    });
    const makeRequest = (retryCount) => {
      onCancel(() => {
      });
      const request = firstRequest ?? new Request(void 0, void 0, normalizedOptions);
      request.retryCount = retryCount;
      request._noPipe = true;
      globalRequest = request;
      request.once("response", async (response) => {
        const contentEncoding = (response.headers["content-encoding"] ?? "").toLowerCase();
        const isCompressed = contentEncoding === "gzip" || contentEncoding === "deflate" || contentEncoding === "br";
        const { options } = request;
        if (isCompressed && !options.decompress) {
          response.body = response.rawBody;
        } else {
          try {
            response.body = parseBody(response, options.responseType, options.parseJson, options.encoding);
          } catch (error) {
            response.body = response.rawBody.toString();
            if (isResponseOk(response)) {
              request._beforeError(error);
              return;
            }
          }
        }
        try {
          const hooks = options.hooks.afterResponse;
          for (const [index, hook] of hooks.entries()) {
            response = await hook(response, async (updatedOptions) => {
              options.merge(updatedOptions);
              options.prefixUrl = "";
              if (updatedOptions.url) {
                options.url = updatedOptions.url;
              }
              options.hooks.afterResponse = options.hooks.afterResponse.slice(0, index);
              throw new RetryError(request);
            });
            if (!(dist_default.object(response) && dist_default.number(response.statusCode) && !dist_default.nullOrUndefined(response.body))) {
              throw new TypeError("The `afterResponse` hook returned an invalid value");
            }
          }
        } catch (error) {
          request._beforeError(error);
          return;
        }
        globalResponse = response;
        if (!isResponseOk(response)) {
          request._beforeError(new HTTPError(response));
          return;
        }
        request.destroy();
        resolve(request.options.resolveBodyOnly ? response.body : response);
      });
      const onError = (error) => {
        if (promise.isCanceled) {
          return;
        }
        const { options } = request;
        if (error instanceof HTTPError && !options.throwHttpErrors) {
          const { response } = error;
          request.destroy();
          resolve(request.options.resolveBodyOnly ? response.body : response);
          return;
        }
        reject(error);
      };
      request.once("error", onError);
      const previousBody = request.options?.body;
      request.once("retry", (newRetryCount, error) => {
        firstRequest = void 0;
        const newBody = request.options.body;
        if (previousBody === newBody && dist_default.nodeStream(newBody)) {
          error.message = "Cannot retry with consumed body stream";
          onError(error);
          return;
        }
        normalizedOptions = request.options;
        makeRequest(newRetryCount);
      });
      proxyEvents(request, emitter, proxiedRequestEvents2);
      if (dist_default.undefined(firstRequest)) {
        void request.flush();
      }
    };
    makeRequest(0);
  });
  promise.on = (event, fn) => {
    emitter.on(event, fn);
    return promise;
  };
  promise.off = (event, fn) => {
    emitter.off(event, fn);
    return promise;
  };
  const shortcut = (responseType) => {
    const newPromise = (async () => {
      await promise;
      const { options } = globalResponse.request;
      return parseBody(globalResponse, responseType, options.parseJson, options.encoding);
    })();
    Object.defineProperties(newPromise, Object.getOwnPropertyDescriptors(promise));
    return newPromise;
  };
  promise.json = () => {
    if (globalRequest.options) {
      const { headers } = globalRequest.options;
      if (!globalRequest.writableFinished && !("accept" in headers)) {
        headers.accept = "application/json";
      }
    }
    return shortcut("json");
  };
  promise.buffer = () => shortcut("buffer");
  promise.text = () => shortcut("text");
  return promise;
}

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/create.js
var delay = async (ms) => new Promise((resolve) => {
  setTimeout(resolve, ms);
});
var isGotInstance = (value) => dist_default.function_(value);
var aliases = [
  "get",
  "post",
  "put",
  "patch",
  "head",
  "delete"
];
var create = (defaults2) => {
  defaults2 = {
    options: new Options(void 0, void 0, defaults2.options),
    handlers: [...defaults2.handlers],
    mutableDefaults: defaults2.mutableDefaults
  };
  Object.defineProperty(defaults2, "mutableDefaults", {
    enumerable: true,
    configurable: false,
    writable: false
  });
  const got2 = (url, options, defaultOptions2 = defaults2.options) => {
    const request = new Request(url, options, defaultOptions2);
    let promise;
    const lastHandler = (normalized) => {
      request.options = normalized;
      request._noPipe = !normalized.isStream;
      void request.flush();
      if (normalized.isStream) {
        return request;
      }
      if (!promise) {
        promise = asPromise(request);
      }
      return promise;
    };
    let iteration = 0;
    const iterateHandlers = (newOptions) => {
      const handler = defaults2.handlers[iteration++] ?? lastHandler;
      const result = handler(newOptions, iterateHandlers);
      if (dist_default.promise(result) && !request.options.isStream) {
        if (!promise) {
          promise = asPromise(request);
        }
        if (result !== promise) {
          const descriptors = Object.getOwnPropertyDescriptors(promise);
          for (const key in descriptors) {
            if (key in result) {
              delete descriptors[key];
            }
          }
          Object.defineProperties(result, descriptors);
          result.cancel = promise.cancel;
        }
      }
      return result;
    };
    return iterateHandlers(request.options);
  };
  got2.extend = (...instancesOrOptions) => {
    const options = new Options(void 0, void 0, defaults2.options);
    const handlers = [...defaults2.handlers];
    let mutableDefaults;
    for (const value of instancesOrOptions) {
      if (isGotInstance(value)) {
        options.merge(value.defaults.options);
        handlers.push(...value.defaults.handlers);
        mutableDefaults = value.defaults.mutableDefaults;
      } else {
        options.merge(value);
        if (value.handlers) {
          handlers.push(...value.handlers);
        }
        mutableDefaults = value.mutableDefaults;
      }
    }
    return create({
      options,
      handlers,
      mutableDefaults: Boolean(mutableDefaults)
    });
  };
  const paginateEach = async function* (url, options) {
    let normalizedOptions = new Options(url, options, defaults2.options);
    normalizedOptions.resolveBodyOnly = false;
    const { pagination } = normalizedOptions;
    assert.function_(pagination.transform);
    assert.function_(pagination.shouldContinue);
    assert.function_(pagination.filter);
    assert.function_(pagination.paginate);
    assert.number(pagination.countLimit);
    assert.number(pagination.requestLimit);
    assert.number(pagination.backoff);
    const allItems = [];
    let { countLimit } = pagination;
    let numberOfRequests = 0;
    while (numberOfRequests < pagination.requestLimit) {
      if (numberOfRequests !== 0) {
        await delay(pagination.backoff);
      }
      const response = await got2(void 0, void 0, normalizedOptions);
      const parsed = await pagination.transform(response);
      const currentItems = [];
      assert.array(parsed);
      for (const item of parsed) {
        if (pagination.filter({ item, currentItems, allItems })) {
          if (!pagination.shouldContinue({ item, currentItems, allItems })) {
            return;
          }
          yield item;
          if (pagination.stackAllItems) {
            allItems.push(item);
          }
          currentItems.push(item);
          if (--countLimit <= 0) {
            return;
          }
        }
      }
      const optionsToMerge = pagination.paginate({
        response,
        currentItems,
        allItems
      });
      if (optionsToMerge === false) {
        return;
      }
      if (optionsToMerge === response.request.options) {
        normalizedOptions = response.request.options;
      } else {
        normalizedOptions.merge(optionsToMerge);
        assert.any([dist_default.urlInstance, dist_default.undefined], optionsToMerge.url);
        if (optionsToMerge.url !== void 0) {
          normalizedOptions.prefixUrl = "";
          normalizedOptions.url = optionsToMerge.url;
        }
      }
      numberOfRequests++;
    }
  };
  got2.paginate = paginateEach;
  got2.paginate.all = async (url, options) => {
    const results = [];
    for await (const item of paginateEach(url, options)) {
      results.push(item);
    }
    return results;
  };
  got2.paginate.each = paginateEach;
  got2.stream = (url, options) => got2(url, { ...options, isStream: true });
  for (const method of aliases) {
    got2[method] = (url, options) => got2(url, { ...options, method });
    got2.stream[method] = (url, options) => got2(url, { ...options, method, isStream: true });
  }
  if (!defaults2.mutableDefaults) {
    Object.freeze(defaults2.handlers);
    defaults2.options.freeze();
  }
  Object.defineProperty(got2, "defaults", {
    value: defaults2,
    writable: false,
    configurable: false,
    enumerable: true
  });
  return got2;
};
var create_default = create;

// ../../node_modules/.pnpm/got@12.6.0/node_modules/got/dist/source/index.js
var defaults = {
  options: new Options(),
  handlers: [],
  mutableDefaults: false
};
var got = create_default(defaults);
var source_default3 = got;

// ../../node_modules/.pnpm/registry-url@6.0.1/node_modules/registry-url/index.js
init_cjs_shims();
var import_rc = __toESM(require_rc(), 1);
function registryUrl(scope) {
  const result = (0, import_rc.default)("npm", { registry: "https://registry.npmjs.org/" });
  const url = result[`${scope}:registry`] || result.config_registry || result.registry;
  return url.slice(-1) === "/" ? url : `${url}/`;
}

// ../../node_modules/.pnpm/package-json@8.1.0/node_modules/package-json/index.js
var import_registry_auth_token = __toESM(require_registry_auth_token(), 1);
var import_semver2 = __toESM(require_semver2(), 1);
var agentOptions = {
  keepAlive: true,
  maxSockets: 50
};
var httpAgent = new import_node_http3.Agent(agentOptions);
var httpsAgent = new import_node_https2.Agent(agentOptions);
var PackageNotFoundError = class extends Error {
  constructor(packageName) {
    super(`Package \`${packageName}\` could not be found`);
    this.name = "PackageNotFoundError";
  }
};
var VersionNotFoundError = class extends Error {
  constructor(packageName, version) {
    super(`Version \`${version}\` for package \`${packageName}\` could not be found`);
    this.name = "VersionNotFoundError";
  }
};
async function packageJson(packageName, options) {
  options = {
    version: "latest",
    ...options
  };
  const scope = packageName.split("/")[0];
  const registryUrl_ = options.registryUrl || registryUrl(scope);
  const packageUrl = new URL(encodeURIComponent(packageName).replace(/^%40/, "@"), registryUrl_);
  const authInfo = (0, import_registry_auth_token.default)(registryUrl_.toString(), { recursive: true });
  const headers = {
    accept: "application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8, */*"
  };
  if (options.fullMetadata) {
    delete headers.accept;
  }
  if (authInfo) {
    headers.authorization = `${authInfo.type} ${authInfo.token}`;
  }
  const gotOptions = {
    headers,
    agent: {
      http: httpAgent,
      https: httpsAgent
    }
  };
  if (options.agent) {
    gotOptions.agent = options.agent;
  }
  let data;
  try {
    data = await source_default3(packageUrl, gotOptions).json();
  } catch (error) {
    if (error.response.statusCode === 404) {
      throw new PackageNotFoundError(packageName);
    }
    throw error;
  }
  if (options.allVersions) {
    return data;
  }
  let { version } = options;
  const versionError = new VersionNotFoundError(packageName, version);
  if (data["dist-tags"][version]) {
    data = data.versions[data["dist-tags"][version]];
  } else if (version) {
    if (!data.versions[version]) {
      const versions = Object.keys(data.versions);
      version = import_semver2.default.maxSatisfying(versions, version);
      if (!version) {
        throw versionError;
      }
    }
    data = data.versions[version];
    if (!data) {
      throw versionError;
    }
  }
  return data;
}

// ../../node_modules/.pnpm/latest-version@7.0.0/node_modules/latest-version/index.js
async function latestVersion(packageName, options) {
  const { version } = await packageJson(packageName.toLowerCase(), options);
  return version;
}

// ../../node_modules/.pnpm/is-npm@6.0.0/node_modules/is-npm/index.js
init_cjs_shims();
var import_node_process4 = __toESM(require("process"), 1);
var packageJson2 = import_node_process4.default.env.npm_package_json;
var userAgent = import_node_process4.default.env.npm_config_user_agent;
var isNpm6 = Boolean(userAgent && userAgent.startsWith("npm"));
var isNpm7 = Boolean(packageJson2 && packageJson2.endsWith("package.json"));
var isNpm = isNpm6 || isNpm7;
var isYarn = Boolean(userAgent && userAgent.startsWith("yarn"));
var isNpmOrYarn = isNpm || isYarn;

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/update-notifier.js
var import_is_installed_globally = __toESM(require_is_installed_globally(), 1);

// ../../node_modules/.pnpm/is-yarn-global@0.4.1/node_modules/is-yarn-global/dist/index.js
init_cjs_shims();
var import_node_path = __toESM(require("path"), 1);
var import_node_url4 = require("url");
var isYarnGlobal = () => {
  const __dirname3 = import_node_path.default.dirname((0, import_node_url4.fileURLToPath)(importMetaUrl));
  const isWindows = process.platform === "win32";
  const yarnPath = isWindows ? import_node_path.default.join("Yarn", "config", "global") : import_node_path.default.join(".config", "yarn", "global");
  return __dirname3.includes(yarnPath);
};
var dist_default3 = isYarnGlobal;

// ../../node_modules/.pnpm/has-yarn@3.0.0/node_modules/has-yarn/index.js
init_cjs_shims();
var import_node_process5 = __toESM(require("process"), 1);
var import_node_path2 = __toESM(require("path"), 1);
var import_node_fs = __toESM(require("fs"), 1);
function hasYarn(cwd = import_node_process5.default.cwd()) {
  return import_node_fs.default.existsSync(import_node_path2.default.resolve(cwd, "yarn.lock"));
}

// ../../node_modules/.pnpm/boxen@7.0.2/node_modules/boxen/index.js
init_cjs_shims();
var import_node_process6 = __toESM(require("process"), 1);

// ../../node_modules/.pnpm/string-width@5.1.2/node_modules/string-width/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/strip-ansi@7.0.1/node_modules/strip-ansi/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/ansi-regex@6.0.1/node_modules/ansi-regex/index.js
init_cjs_shims();
function ansiRegex({ onlyFirst = false } = {}) {
  const pattern = [
    "[\\u001B\\u009B][[\\]()#;?]*(?:(?:(?:(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]+)*|[a-zA-Z\\d]+(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]*)*)?\\u0007)",
    "(?:(?:\\d{1,4}(?:;\\d{0,4})*)?[\\dA-PR-TZcf-ntqry=><~]))"
  ].join("|");
  return new RegExp(pattern, onlyFirst ? void 0 : "g");
}

// ../../node_modules/.pnpm/strip-ansi@7.0.1/node_modules/strip-ansi/index.js
function stripAnsi(string) {
  if (typeof string !== "string") {
    throw new TypeError(`Expected a \`string\`, got \`${typeof string}\``);
  }
  return string.replace(ansiRegex(), "");
}

// ../../node_modules/.pnpm/string-width@5.1.2/node_modules/string-width/index.js
var import_eastasianwidth = __toESM(require_eastasianwidth(), 1);
var import_emoji_regex = __toESM(require_emoji_regex(), 1);
function stringWidth(string, options = {}) {
  if (typeof string !== "string" || string.length === 0) {
    return 0;
  }
  options = {
    ambiguousIsNarrow: true,
    ...options
  };
  string = stripAnsi(string);
  if (string.length === 0) {
    return 0;
  }
  string = string.replace((0, import_emoji_regex.default)(), "  ");
  const ambiguousCharacterWidth = options.ambiguousIsNarrow ? 1 : 2;
  let width = 0;
  for (const character of string) {
    const codePoint = character.codePointAt(0);
    if (codePoint <= 31 || codePoint >= 127 && codePoint <= 159) {
      continue;
    }
    if (codePoint >= 768 && codePoint <= 879) {
      continue;
    }
    const code = import_eastasianwidth.default.eastAsianWidth(character);
    switch (code) {
      case "F":
      case "W":
        width += 2;
        break;
      case "A":
        width += ambiguousCharacterWidth;
        break;
      default:
        width += 1;
    }
  }
  return width;
}

// ../../node_modules/.pnpm/widest-line@4.0.1/node_modules/widest-line/index.js
init_cjs_shims();
function widestLine(string) {
  let lineWidth = 0;
  for (const line of string.split("\n")) {
    lineWidth = Math.max(lineWidth, stringWidth(line));
  }
  return lineWidth;
}

// ../../node_modules/.pnpm/boxen@7.0.2/node_modules/boxen/index.js
var import_cli_boxes = __toESM(require_cli_boxes(), 1);

// ../../node_modules/.pnpm/camelcase@7.0.1/node_modules/camelcase/index.js
init_cjs_shims();
var UPPERCASE = /[\p{Lu}]/u;
var LOWERCASE = /[\p{Ll}]/u;
var LEADING_CAPITAL = /^[\p{Lu}](?![\p{Lu}])/gu;
var IDENTIFIER = /([\p{Alpha}\p{N}_]|$)/u;
var SEPARATORS = /[_.\- ]+/;
var LEADING_SEPARATORS = new RegExp("^" + SEPARATORS.source);
var SEPARATORS_AND_IDENTIFIER = new RegExp(SEPARATORS.source + IDENTIFIER.source, "gu");
var NUMBERS_AND_IDENTIFIER = new RegExp("\\d+" + IDENTIFIER.source, "gu");
var preserveCamelCase = (string, toLowerCase, toUpperCase, preserveConsecutiveUppercase2) => {
  let isLastCharLower = false;
  let isLastCharUpper = false;
  let isLastLastCharUpper = false;
  let isLastLastCharPreserved = false;
  for (let index = 0; index < string.length; index++) {
    const character = string[index];
    isLastLastCharPreserved = index > 2 ? string[index - 3] === "-" : true;
    if (isLastCharLower && UPPERCASE.test(character)) {
      string = string.slice(0, index) + "-" + string.slice(index);
      isLastCharLower = false;
      isLastLastCharUpper = isLastCharUpper;
      isLastCharUpper = true;
      index++;
    } else if (isLastCharUpper && isLastLastCharUpper && LOWERCASE.test(character) && (!isLastLastCharPreserved || preserveConsecutiveUppercase2)) {
      string = string.slice(0, index - 1) + "-" + string.slice(index - 1);
      isLastLastCharUpper = isLastCharUpper;
      isLastCharUpper = false;
      isLastCharLower = true;
    } else {
      isLastCharLower = toLowerCase(character) === character && toUpperCase(character) !== character;
      isLastLastCharUpper = isLastCharUpper;
      isLastCharUpper = toUpperCase(character) === character && toLowerCase(character) !== character;
    }
  }
  return string;
};
var preserveConsecutiveUppercase = (input, toLowerCase) => {
  LEADING_CAPITAL.lastIndex = 0;
  return input.replace(LEADING_CAPITAL, (m1) => toLowerCase(m1));
};
var postProcess = (input, toUpperCase) => {
  SEPARATORS_AND_IDENTIFIER.lastIndex = 0;
  NUMBERS_AND_IDENTIFIER.lastIndex = 0;
  return input.replace(SEPARATORS_AND_IDENTIFIER, (_, identifier) => toUpperCase(identifier)).replace(NUMBERS_AND_IDENTIFIER, (m) => toUpperCase(m));
};
function camelCase(input, options) {
  if (!(typeof input === "string" || Array.isArray(input))) {
    throw new TypeError("Expected the input to be `string | string[]`");
  }
  options = {
    pascalCase: false,
    preserveConsecutiveUppercase: false,
    ...options
  };
  if (Array.isArray(input)) {
    input = input.map((x) => x.trim()).filter((x) => x.length).join("-");
  } else {
    input = input.trim();
  }
  if (input.length === 0) {
    return "";
  }
  const toLowerCase = options.locale === false ? (string) => string.toLowerCase() : (string) => string.toLocaleLowerCase(options.locale);
  const toUpperCase = options.locale === false ? (string) => string.toUpperCase() : (string) => string.toLocaleUpperCase(options.locale);
  if (input.length === 1) {
    if (SEPARATORS.test(input)) {
      return "";
    }
    return options.pascalCase ? toUpperCase(input) : toLowerCase(input);
  }
  const hasUpperCase = input !== toLowerCase(input);
  if (hasUpperCase) {
    input = preserveCamelCase(input, toLowerCase, toUpperCase, options.preserveConsecutiveUppercase);
  }
  input = input.replace(LEADING_SEPARATORS, "");
  input = options.preserveConsecutiveUppercase ? preserveConsecutiveUppercase(input, toLowerCase) : toLowerCase(input);
  if (options.pascalCase) {
    input = toUpperCase(input.charAt(0)) + input.slice(1);
  }
  return postProcess(input, toUpperCase);
}

// ../../node_modules/.pnpm/boxen@7.0.2/node_modules/boxen/index.js
var import_ansi_align = __toESM(require_ansi_align(), 1);

// ../../node_modules/.pnpm/wrap-ansi@8.1.0/node_modules/wrap-ansi/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/ansi-styles@6.2.1/node_modules/ansi-styles/index.js
init_cjs_shims();
var ANSI_BACKGROUND_OFFSET2 = 10;
var wrapAnsi162 = (offset = 0) => (code) => `\x1B[${code + offset}m`;
var wrapAnsi2562 = (offset = 0) => (code) => `\x1B[${38 + offset};5;${code}m`;
var wrapAnsi16m2 = (offset = 0) => (red, green, blue) => `\x1B[${38 + offset};2;${red};${green};${blue}m`;
var styles2 = {
  modifier: {
    reset: [0, 0],
    bold: [1, 22],
    dim: [2, 22],
    italic: [3, 23],
    underline: [4, 24],
    overline: [53, 55],
    inverse: [7, 27],
    hidden: [8, 28],
    strikethrough: [9, 29]
  },
  color: {
    black: [30, 39],
    red: [31, 39],
    green: [32, 39],
    yellow: [33, 39],
    blue: [34, 39],
    magenta: [35, 39],
    cyan: [36, 39],
    white: [37, 39],
    blackBright: [90, 39],
    gray: [90, 39],
    grey: [90, 39],
    redBright: [91, 39],
    greenBright: [92, 39],
    yellowBright: [93, 39],
    blueBright: [94, 39],
    magentaBright: [95, 39],
    cyanBright: [96, 39],
    whiteBright: [97, 39]
  },
  bgColor: {
    bgBlack: [40, 49],
    bgRed: [41, 49],
    bgGreen: [42, 49],
    bgYellow: [43, 49],
    bgBlue: [44, 49],
    bgMagenta: [45, 49],
    bgCyan: [46, 49],
    bgWhite: [47, 49],
    bgBlackBright: [100, 49],
    bgGray: [100, 49],
    bgGrey: [100, 49],
    bgRedBright: [101, 49],
    bgGreenBright: [102, 49],
    bgYellowBright: [103, 49],
    bgBlueBright: [104, 49],
    bgMagentaBright: [105, 49],
    bgCyanBright: [106, 49],
    bgWhiteBright: [107, 49]
  }
};
var modifierNames = Object.keys(styles2.modifier);
var foregroundColorNames = Object.keys(styles2.color);
var backgroundColorNames = Object.keys(styles2.bgColor);
var colorNames = [...foregroundColorNames, ...backgroundColorNames];
function assembleStyles2() {
  const codes = /* @__PURE__ */ new Map();
  for (const [groupName, group] of Object.entries(styles2)) {
    for (const [styleName, style] of Object.entries(group)) {
      styles2[styleName] = {
        open: `\x1B[${style[0]}m`,
        close: `\x1B[${style[1]}m`
      };
      group[styleName] = styles2[styleName];
      codes.set(style[0], style[1]);
    }
    Object.defineProperty(styles2, groupName, {
      value: group,
      enumerable: false
    });
  }
  Object.defineProperty(styles2, "codes", {
    value: codes,
    enumerable: false
  });
  styles2.color.close = "\x1B[39m";
  styles2.bgColor.close = "\x1B[49m";
  styles2.color.ansi = wrapAnsi162();
  styles2.color.ansi256 = wrapAnsi2562();
  styles2.color.ansi16m = wrapAnsi16m2();
  styles2.bgColor.ansi = wrapAnsi162(ANSI_BACKGROUND_OFFSET2);
  styles2.bgColor.ansi256 = wrapAnsi2562(ANSI_BACKGROUND_OFFSET2);
  styles2.bgColor.ansi16m = wrapAnsi16m2(ANSI_BACKGROUND_OFFSET2);
  Object.defineProperties(styles2, {
    rgbToAnsi256: {
      value: (red, green, blue) => {
        if (red === green && green === blue) {
          if (red < 8) {
            return 16;
          }
          if (red > 248) {
            return 231;
          }
          return Math.round((red - 8) / 247 * 24) + 232;
        }
        return 16 + 36 * Math.round(red / 255 * 5) + 6 * Math.round(green / 255 * 5) + Math.round(blue / 255 * 5);
      },
      enumerable: false
    },
    hexToRgb: {
      value: (hex) => {
        const matches = /[a-f\d]{6}|[a-f\d]{3}/i.exec(hex.toString(16));
        if (!matches) {
          return [0, 0, 0];
        }
        let [colorString] = matches;
        if (colorString.length === 3) {
          colorString = [...colorString].map((character) => character + character).join("");
        }
        const integer = Number.parseInt(colorString, 16);
        return [
          integer >> 16 & 255,
          integer >> 8 & 255,
          integer & 255
        ];
      },
      enumerable: false
    },
    hexToAnsi256: {
      value: (hex) => styles2.rgbToAnsi256(...styles2.hexToRgb(hex)),
      enumerable: false
    },
    ansi256ToAnsi: {
      value: (code) => {
        if (code < 8) {
          return 30 + code;
        }
        if (code < 16) {
          return 90 + (code - 8);
        }
        let red;
        let green;
        let blue;
        if (code >= 232) {
          red = ((code - 232) * 10 + 8) / 255;
          green = red;
          blue = red;
        } else {
          code -= 16;
          const remainder = code % 36;
          red = Math.floor(code / 36) / 5;
          green = Math.floor(remainder / 6) / 5;
          blue = remainder % 6 / 5;
        }
        const value = Math.max(red, green, blue) * 2;
        if (value === 0) {
          return 30;
        }
        let result = 30 + (Math.round(blue) << 2 | Math.round(green) << 1 | Math.round(red));
        if (value === 2) {
          result += 60;
        }
        return result;
      },
      enumerable: false
    },
    rgbToAnsi: {
      value: (red, green, blue) => styles2.ansi256ToAnsi(styles2.rgbToAnsi256(red, green, blue)),
      enumerable: false
    },
    hexToAnsi: {
      value: (hex) => styles2.ansi256ToAnsi(styles2.hexToAnsi256(hex)),
      enumerable: false
    }
  });
  return styles2;
}
var ansiStyles2 = assembleStyles2();
var ansi_styles_default2 = ansiStyles2;

// ../../node_modules/.pnpm/wrap-ansi@8.1.0/node_modules/wrap-ansi/index.js
var ESCAPES = /* @__PURE__ */ new Set([
  "\x1B",
  "\x9B"
]);
var END_CODE = 39;
var ANSI_ESCAPE_BELL = "\x07";
var ANSI_CSI = "[";
var ANSI_OSC = "]";
var ANSI_SGR_TERMINATOR = "m";
var ANSI_ESCAPE_LINK = `${ANSI_OSC}8;;`;
var wrapAnsiCode = (code) => `${ESCAPES.values().next().value}${ANSI_CSI}${code}${ANSI_SGR_TERMINATOR}`;
var wrapAnsiHyperlink = (uri) => `${ESCAPES.values().next().value}${ANSI_ESCAPE_LINK}${uri}${ANSI_ESCAPE_BELL}`;
var wordLengths = (string) => string.split(" ").map((character) => stringWidth(character));
var wrapWord = (rows, word, columns) => {
  const characters = [...word];
  let isInsideEscape = false;
  let isInsideLinkEscape = false;
  let visible = stringWidth(stripAnsi(rows[rows.length - 1]));
  for (const [index, character] of characters.entries()) {
    const characterLength = stringWidth(character);
    if (visible + characterLength <= columns) {
      rows[rows.length - 1] += character;
    } else {
      rows.push(character);
      visible = 0;
    }
    if (ESCAPES.has(character)) {
      isInsideEscape = true;
      isInsideLinkEscape = characters.slice(index + 1).join("").startsWith(ANSI_ESCAPE_LINK);
    }
    if (isInsideEscape) {
      if (isInsideLinkEscape) {
        if (character === ANSI_ESCAPE_BELL) {
          isInsideEscape = false;
          isInsideLinkEscape = false;
        }
      } else if (character === ANSI_SGR_TERMINATOR) {
        isInsideEscape = false;
      }
      continue;
    }
    visible += characterLength;
    if (visible === columns && index < characters.length - 1) {
      rows.push("");
      visible = 0;
    }
  }
  if (!visible && rows[rows.length - 1].length > 0 && rows.length > 1) {
    rows[rows.length - 2] += rows.pop();
  }
};
var stringVisibleTrimSpacesRight = (string) => {
  const words = string.split(" ");
  let last = words.length;
  while (last > 0) {
    if (stringWidth(words[last - 1]) > 0) {
      break;
    }
    last--;
  }
  if (last === words.length) {
    return string;
  }
  return words.slice(0, last).join(" ") + words.slice(last).join("");
};
var exec = (string, columns, options = {}) => {
  if (options.trim !== false && string.trim() === "") {
    return "";
  }
  let returnValue = "";
  let escapeCode;
  let escapeUrl;
  const lengths = wordLengths(string);
  let rows = [""];
  for (const [index, word] of string.split(" ").entries()) {
    if (options.trim !== false) {
      rows[rows.length - 1] = rows[rows.length - 1].trimStart();
    }
    let rowLength = stringWidth(rows[rows.length - 1]);
    if (index !== 0) {
      if (rowLength >= columns && (options.wordWrap === false || options.trim === false)) {
        rows.push("");
        rowLength = 0;
      }
      if (rowLength > 0 || options.trim === false) {
        rows[rows.length - 1] += " ";
        rowLength++;
      }
    }
    if (options.hard && lengths[index] > columns) {
      const remainingColumns = columns - rowLength;
      const breaksStartingThisLine = 1 + Math.floor((lengths[index] - remainingColumns - 1) / columns);
      const breaksStartingNextLine = Math.floor((lengths[index] - 1) / columns);
      if (breaksStartingNextLine < breaksStartingThisLine) {
        rows.push("");
      }
      wrapWord(rows, word, columns);
      continue;
    }
    if (rowLength + lengths[index] > columns && rowLength > 0 && lengths[index] > 0) {
      if (options.wordWrap === false && rowLength < columns) {
        wrapWord(rows, word, columns);
        continue;
      }
      rows.push("");
    }
    if (rowLength + lengths[index] > columns && options.wordWrap === false) {
      wrapWord(rows, word, columns);
      continue;
    }
    rows[rows.length - 1] += word;
  }
  if (options.trim !== false) {
    rows = rows.map((row) => stringVisibleTrimSpacesRight(row));
  }
  const pre = [...rows.join("\n")];
  for (const [index, character] of pre.entries()) {
    returnValue += character;
    if (ESCAPES.has(character)) {
      const { groups } = new RegExp(`(?:\\${ANSI_CSI}(?<code>\\d+)m|\\${ANSI_ESCAPE_LINK}(?<uri>.*)${ANSI_ESCAPE_BELL})`).exec(pre.slice(index).join("")) || { groups: {} };
      if (groups.code !== void 0) {
        const code2 = Number.parseFloat(groups.code);
        escapeCode = code2 === END_CODE ? void 0 : code2;
      } else if (groups.uri !== void 0) {
        escapeUrl = groups.uri.length === 0 ? void 0 : groups.uri;
      }
    }
    const code = ansi_styles_default2.codes.get(Number(escapeCode));
    if (pre[index + 1] === "\n") {
      if (escapeUrl) {
        returnValue += wrapAnsiHyperlink("");
      }
      if (escapeCode && code) {
        returnValue += wrapAnsiCode(code);
      }
    } else if (character === "\n") {
      if (escapeCode && code) {
        returnValue += wrapAnsiCode(escapeCode);
      }
      if (escapeUrl) {
        returnValue += wrapAnsiHyperlink(escapeUrl);
      }
    }
  }
  return returnValue;
};
function wrapAnsi(string, columns, options) {
  return String(string).normalize().replace(/\r\n/g, "\n").split("\n").map((line) => exec(line, columns, options)).join("\n");
}

// ../../node_modules/.pnpm/boxen@7.0.2/node_modules/boxen/index.js
var import_cli_boxes2 = __toESM(require_cli_boxes(), 1);
var NEWLINE = "\n";
var PAD = " ";
var BORDERS_WIDTH = 2;
var terminalColumns = () => {
  const { env: env3, stdout, stderr } = import_node_process6.default;
  if (stdout?.columns) {
    return stdout.columns;
  }
  if (stderr?.columns) {
    return stderr.columns;
  }
  if (env3.COLUMNS) {
    return Number.parseInt(env3.COLUMNS, 10);
  }
  return 80;
};
var getObject = (detail) => typeof detail === "number" ? {
  top: detail,
  right: detail * 3,
  bottom: detail,
  left: detail * 3
} : {
  top: 0,
  right: 0,
  bottom: 0,
  left: 0,
  ...detail
};
var getBorderChars = (borderStyle) => {
  const sides = [
    "topLeft",
    "topRight",
    "bottomRight",
    "bottomLeft",
    "left",
    "right",
    "top",
    "bottom"
  ];
  let characters;
  if (typeof borderStyle === "string") {
    characters = import_cli_boxes.default[borderStyle];
    if (!characters) {
      throw new TypeError(`Invalid border style: ${borderStyle}`);
    }
  } else {
    if (typeof borderStyle?.vertical === "string") {
      borderStyle.left = borderStyle.vertical;
      borderStyle.right = borderStyle.vertical;
    }
    if (typeof borderStyle?.horizontal === "string") {
      borderStyle.top = borderStyle.horizontal;
      borderStyle.bottom = borderStyle.horizontal;
    }
    for (const side of sides) {
      if (!borderStyle[side] || typeof borderStyle[side] !== "string") {
        throw new TypeError(`Invalid border style: ${side}`);
      }
    }
    characters = borderStyle;
  }
  return characters;
};
var makeTitle = (text, horizontal, alignement) => {
  let title = "";
  const textWidth = stringWidth(text);
  switch (alignement) {
    case "left":
      title = text + horizontal.slice(textWidth);
      break;
    case "right":
      title = horizontal.slice(textWidth) + text;
      break;
    default:
      horizontal = horizontal.slice(textWidth);
      if (horizontal.length % 2 === 1) {
        horizontal = horizontal.slice(Math.floor(horizontal.length / 2));
        title = horizontal.slice(1) + text + horizontal;
      } else {
        horizontal = horizontal.slice(horizontal.length / 2);
        title = horizontal + text + horizontal;
      }
      break;
  }
  return title;
};
var makeContentText = (text, { padding, width, textAlignment, height }) => {
  text = (0, import_ansi_align.default)(text, { align: textAlignment });
  let lines = text.split(NEWLINE);
  const textWidth = widestLine(text);
  const max = width - padding.left - padding.right;
  if (textWidth > max) {
    const newLines = [];
    for (const line of lines) {
      const createdLines = wrapAnsi(line, max, { hard: true });
      const alignedLines = (0, import_ansi_align.default)(createdLines, { align: textAlignment });
      const alignedLinesArray = alignedLines.split("\n");
      const longestLength = Math.max(...alignedLinesArray.map((s) => stringWidth(s)));
      for (const alignedLine of alignedLinesArray) {
        let paddedLine;
        switch (textAlignment) {
          case "center":
            paddedLine = PAD.repeat((max - longestLength) / 2) + alignedLine;
            break;
          case "right":
            paddedLine = PAD.repeat(max - longestLength) + alignedLine;
            break;
          default:
            paddedLine = alignedLine;
            break;
        }
        newLines.push(paddedLine);
      }
    }
    lines = newLines;
  }
  if (textAlignment === "center" && textWidth < max) {
    lines = lines.map((line) => PAD.repeat((max - textWidth) / 2) + line);
  } else if (textAlignment === "right" && textWidth < max) {
    lines = lines.map((line) => PAD.repeat(max - textWidth) + line);
  }
  const paddingLeft = PAD.repeat(padding.left);
  const paddingRight = PAD.repeat(padding.right);
  lines = lines.map((line) => paddingLeft + line + paddingRight);
  lines = lines.map((line) => {
    if (width - stringWidth(line) > 0) {
      switch (textAlignment) {
        case "center":
          return line + PAD.repeat(width - stringWidth(line));
        case "right":
          return line + PAD.repeat(width - stringWidth(line));
        default:
          return line + PAD.repeat(width - stringWidth(line));
      }
    }
    return line;
  });
  if (padding.top > 0) {
    lines = [...Array.from({ length: padding.top }).fill(PAD.repeat(width)), ...lines];
  }
  if (padding.bottom > 0) {
    lines = [...lines, ...Array.from({ length: padding.bottom }).fill(PAD.repeat(width))];
  }
  if (height && lines.length > height) {
    lines = lines.slice(0, height);
  } else if (height && lines.length < height) {
    lines = [...lines, ...Array.from({ length: height - lines.length }).fill(PAD.repeat(width))];
  }
  return lines.join(NEWLINE);
};
var boxContent = (content, contentWidth, options) => {
  const colorizeBorder = (border) => {
    const newBorder = options.borderColor ? getColorFn(options.borderColor)(border) : border;
    return options.dimBorder ? source_default.dim(newBorder) : newBorder;
  };
  const colorizeContent = (content2) => options.backgroundColor ? getBGColorFn(options.backgroundColor)(content2) : content2;
  const chars = getBorderChars(options.borderStyle);
  const columns = terminalColumns();
  let marginLeft = PAD.repeat(options.margin.left);
  if (options.float === "center") {
    const marginWidth = Math.max((columns - contentWidth - BORDERS_WIDTH) / 2, 0);
    marginLeft = PAD.repeat(marginWidth);
  } else if (options.float === "right") {
    const marginWidth = Math.max(columns - contentWidth - options.margin.right - BORDERS_WIDTH, 0);
    marginLeft = PAD.repeat(marginWidth);
  }
  const top = colorizeBorder(NEWLINE.repeat(options.margin.top) + marginLeft + chars.topLeft + (options.title ? makeTitle(options.title, chars.top.repeat(contentWidth), options.titleAlignment) : chars.top.repeat(contentWidth)) + chars.topRight);
  const bottom = colorizeBorder(marginLeft + chars.bottomLeft + chars.bottom.repeat(contentWidth) + chars.bottomRight + NEWLINE.repeat(options.margin.bottom));
  const lines = content.split(NEWLINE);
  const middle = lines.map((line) => marginLeft + colorizeBorder(chars.left) + colorizeContent(line) + colorizeBorder(chars.right)).join(NEWLINE);
  return top + NEWLINE + middle + NEWLINE + bottom;
};
var sanitizeOptions = (options) => {
  if (options.fullscreen && import_node_process6.default?.stdout) {
    let newDimensions = [import_node_process6.default.stdout.columns, import_node_process6.default.stdout.rows];
    if (typeof options.fullscreen === "function") {
      newDimensions = options.fullscreen(...newDimensions);
    }
    if (!options.width) {
      options.width = newDimensions[0];
    }
    if (!options.height) {
      options.height = newDimensions[1];
    }
  }
  if (options.width) {
    options.width = Math.max(1, options.width - BORDERS_WIDTH);
  }
  if (options.height) {
    options.height = Math.max(1, options.height - BORDERS_WIDTH);
  }
  return options;
};
var determineDimensions = (text, options) => {
  options = sanitizeOptions(options);
  const widthOverride = options.width !== void 0;
  const columns = terminalColumns();
  const maxWidth = columns - options.margin.left - options.margin.right - BORDERS_WIDTH;
  const widest = widestLine(wrapAnsi(text, columns - BORDERS_WIDTH, { hard: true, trim: false })) + options.padding.left + options.padding.right;
  if (options.title && widthOverride) {
    options.title = options.title.slice(0, Math.max(0, options.width - 2));
    if (options.title) {
      options.title = ` ${options.title} `;
    }
  } else if (options.title) {
    options.title = options.title.slice(0, Math.max(0, maxWidth - 2));
    if (options.title) {
      options.title = ` ${options.title} `;
      if (stringWidth(options.title) > widest) {
        options.width = stringWidth(options.title);
      }
    }
  }
  options.width = options.width ? options.width : widest;
  if (!widthOverride) {
    if (options.margin.left && options.margin.right && options.width > maxWidth) {
      const spaceForMargins = columns - options.width - BORDERS_WIDTH;
      const multiplier = spaceForMargins / (options.margin.left + options.margin.right);
      options.margin.left = Math.max(0, Math.floor(options.margin.left * multiplier));
      options.margin.right = Math.max(0, Math.floor(options.margin.right * multiplier));
    }
    options.width = Math.min(options.width, columns - BORDERS_WIDTH - options.margin.left - options.margin.right);
  }
  if (options.width - (options.padding.left + options.padding.right) <= 0) {
    options.padding.left = 0;
    options.padding.right = 0;
  }
  if (options.height && options.height - (options.padding.top + options.padding.bottom) <= 0) {
    options.padding.top = 0;
    options.padding.bottom = 0;
  }
  return options;
};
var isHex = (color) => color.match(/^#(?:[0-f]{3}){1,2}$/i);
var isColorValid = (color) => typeof color === "string" && (source_default[color] ?? isHex(color));
var getColorFn = (color) => isHex(color) ? source_default.hex(color) : source_default[color];
var getBGColorFn = (color) => isHex(color) ? source_default.bgHex(color) : source_default[camelCase(["bg", color])];
function boxen(text, options) {
  options = {
    padding: 0,
    borderStyle: "single",
    dimBorder: false,
    textAlignment: "left",
    float: "left",
    titleAlignment: "left",
    ...options
  };
  if (options.align) {
    options.textAlignment = options.align;
  }
  if (options.borderColor && !isColorValid(options.borderColor)) {
    throw new Error(`${options.borderColor} is not a valid borderColor`);
  }
  if (options.backgroundColor && !isColorValid(options.backgroundColor)) {
    throw new Error(`${options.backgroundColor} is not a valid backgroundColor`);
  }
  options.padding = getObject(options.padding);
  options.margin = getObject(options.margin);
  options = determineDimensions(text, options);
  text = makeContentText(text, options);
  return boxContent(text, options.width, options);
}

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/update-notifier.js
var import_is_ci = __toESM(require_is_ci(), 1);

// ../../node_modules/.pnpm/pupa@3.1.0/node_modules/pupa/index.js
init_cjs_shims();

// ../../node_modules/.pnpm/escape-goat@4.0.0/node_modules/escape-goat/index.js
init_cjs_shims();
var _htmlEscape = (string) => string.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/'/g, "&#39;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
function htmlEscape(strings, ...values) {
  if (typeof strings === "string") {
    return _htmlEscape(strings);
  }
  let output = strings[0];
  for (const [index, value] of values.entries()) {
    output = output + _htmlEscape(String(value)) + strings[index + 1];
  }
  return output;
}

// ../../node_modules/.pnpm/pupa@3.1.0/node_modules/pupa/index.js
var MissingValueError = class extends Error {
  constructor(key) {
    super(`Missing a value for ${key ? `the placeholder: ${key}` : "a placeholder"}`, key);
    this.name = "MissingValueError";
    this.key = key;
  }
};
function pupa(template, data, { ignoreMissing = false, transform = ({ value }) => value } = {}) {
  if (typeof template !== "string") {
    throw new TypeError(`Expected a \`string\` in the first argument, got \`${typeof template}\``);
  }
  if (typeof data !== "object") {
    throw new TypeError(`Expected an \`object\` or \`Array\` in the second argument, got \`${typeof data}\``);
  }
  const replace = (placeholder, key) => {
    let value = data;
    for (const property of key.split(".")) {
      value = value ? value[property] : void 0;
    }
    const transformedValue = transform({ value, key });
    if (transformedValue === void 0) {
      if (ignoreMissing) {
        return placeholder;
      }
      throw new MissingValueError(key);
    }
    return String(transformedValue);
  };
  const composeHtmlEscape = (replacer) => (...args) => htmlEscape(replacer(...args));
  const doubleBraceRegex = /{{(\d+|[a-z$_][\w\-$]*?(?:\.[\w\-$]*?)*?)}}/gi;
  if (doubleBraceRegex.test(template)) {
    template = template.replace(doubleBraceRegex, composeHtmlEscape(replace));
  }
  const braceRegex = /{(\d+|[a-z$_][\w\-$]*?(?:\.[\w\-$]*?)*?)}/gi;
  return template.replace(braceRegex, replace);
}

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/update-notifier.js
var __dirname2 = import_node_path3.default.dirname((0, import_node_url5.fileURLToPath)(importMetaUrl));
var ONE_DAY = 1e3 * 60 * 60 * 24;
var UpdateNotifier = class {
  config;
  update;
  _packageName;
  _shouldNotifyInNpmScript;
  #options;
  #packageVersion;
  #updateCheckInterval;
  #isDisabled;
  constructor(options = {}) {
    this.#options = options;
    options.pkg = options.pkg || {};
    options.distTag = options.distTag || "latest";
    options.pkg = {
      name: options.pkg.name || options.packageName,
      version: options.pkg.version || options.packageVersion
    };
    if (!options.pkg.name || !options.pkg.version) {
      throw new Error("pkg.name and pkg.version required");
    }
    this._packageName = options.pkg.name;
    this.#packageVersion = options.pkg.version;
    this.#updateCheckInterval = typeof options.updateCheckInterval === "number" ? options.updateCheckInterval : ONE_DAY;
    this.#isDisabled = "NO_UPDATE_NOTIFIER" in import_node_process7.default.env || import_node_process7.default.env.NODE_ENV === "test" || import_node_process7.default.argv.includes("--no-update-notifier") || import_is_ci.default;
    this._shouldNotifyInNpmScript = options.shouldNotifyInNpmScript;
    if (!this.#isDisabled) {
      try {
        this.config = new Configstore(`update-notifier-${this._packageName}`, {
          optOut: false,
          lastUpdateCheck: Date.now()
        });
      } catch {
        const message = source_default.yellow((0, import_node_util4.format)(" %s update check failed ", options.pkg.name)) + (0, import_node_util4.format)("\n Try running with %s or get access ", source_default.cyan("sudo")) + "\n to the local update config store via \n" + source_default.cyan((0, import_node_util4.format)(" sudo chown -R $USER:$(id -gn $USER) %s ", xdgConfig));
        import_node_process7.default.on("exit", () => {
          console.error(boxen(message, { textAlignment: "center" }));
        });
      }
    }
  }
  check() {
    if (!this.config || this.config.get("optOut") || this.#isDisabled) {
      return;
    }
    this.update = this.config.get("update");
    if (this.update) {
      this.update.current = this.#packageVersion;
      this.config.delete("update");
    }
    if (Date.now() - this.config.get("lastUpdateCheck") < this.#updateCheckInterval) {
      return;
    }
    (0, import_node_child_process.spawn)(import_node_process7.default.execPath, [import_node_path3.default.join(__dirname2, "check.js"), JSON.stringify(this.#options)], {
      detached: true,
      stdio: "ignore"
    }).unref();
  }
  async fetchInfo() {
    const { distTag } = this.#options;
    const latest = await latestVersion(this._packageName, { version: distTag });
    return {
      latest,
      current: this.#packageVersion,
      type: semverDiff(this.#packageVersion, latest) || distTag,
      name: this._packageName
    };
  }
  notify(options) {
    const suppressForNpm = !this._shouldNotifyInNpmScript && isNpmOrYarn;
    if (!import_node_process7.default.stdout.isTTY || suppressForNpm || !this.update || !import_semver3.default.gt(this.update.latest, this.update.current)) {
      return this;
    }
    options = {
      isGlobal: import_is_installed_globally.default,
      isYarnGlobal: dist_default3(),
      ...options
    };
    let installCommand;
    if (options.isYarnGlobal) {
      installCommand = `yarn global add ${this._packageName}`;
    } else if (options.isGlobal) {
      installCommand = `npm i -g ${this._packageName}`;
    } else if (hasYarn()) {
      installCommand = `yarn add ${this._packageName}`;
    } else {
      installCommand = `npm i ${this._packageName}`;
    }
    const defaultTemplate = "Update available " + source_default.dim("{currentVersion}") + source_default.reset(" \u2192 ") + source_default.green("{latestVersion}") + " \nRun " + source_default.cyan("{updateCommand}") + " to update";
    const template = options.message || defaultTemplate;
    options.boxenOptions = options.boxenOptions || {
      padding: 1,
      margin: 1,
      textAlignment: "center",
      borderColor: "yellow",
      borderStyle: "round"
    };
    const message = boxen(
      pupa(template, {
        packageName: this._packageName,
        currentVersion: this.update.current,
        latestVersion: this.update.latest,
        updateCommand: installCommand
      }),
      options.boxenOptions
    );
    if (options.defer === false) {
      console.error(message);
    } else {
      import_node_process7.default.on("exit", () => {
        console.error(message);
      });
      import_node_process7.default.on("SIGINT", () => {
        console.error("");
        import_node_process7.default.exit();
      });
    }
    return this;
  }
};

// ../../node_modules/.pnpm/update-notifier@6.0.2/node_modules/update-notifier/index.js
function updateNotifier(options) {
  const updateNotifier2 = new UpdateNotifier(options);
  updateNotifier2.check();
  return updateNotifier2;
}

// src/utils/init-cli.ts
var import_cli_handle_unhandled = __toESM(require("cli-handle-unhandled"));

// package.json
var package_default = {
  name: "@chakra-ui/cli",
  version: "2.4.1",
  description: "Generate theme typings for autocomplete",
  keywords: [
    "theme",
    "theming",
    "typings",
    "generator",
    "chakra ui",
    "ui mode",
    "ui"
  ],
  author: "Tim Kolberger <tim@kolberger.eu>",
  homepage: "https://github.com/chakra-ui/chakra-ui#readme",
  license: "MIT",
  main: "dist/index.js",
  sideEffects: false,
  files: [
    "dist",
    "bin"
  ],
  publishConfig: {
    access: "public"
  },
  repository: {
    type: "git",
    url: "git+https://github.com/chakra-ui/chakra-ui.git"
  },
  bugs: {
    url: "https://github.com/chakra-ui/chakra-ui/issues"
  },
  bin: {
    "chakra-cli": "bin/index.js"
  },
  scripts: {
    clean: "rimraf dist .turbo",
    build: "tsup src --dts --format cjs --clean --shims",
    dev: "pnpm build --watch",
    typecheck: " tsc --noEmit"
  },
  dependencies: {
    chokidar: "^3.5.3",
    "cli-check-node": "^1.3.4",
    "cli-handle-unhandled": "^1.1.1",
    "cli-welcome": "^2.2.2",
    commander: "^9.3.0",
    esbuild: "^0.17.18",
    prettier: "^2.8.8"
  },
  devDependencies: {
    "@types/ora": "^3.2.0",
    "@types/update-notifier": "6.0.2",
    ora: "^6.3.0",
    "update-notifier": "^6.0.2"
  }
};

// src/utils/init-cli.ts
async function initCLI() {
  (0, import_cli_check_node.default)(`12`);
  await (0, import_cli_handle_unhandled.default)();
  (0, import_cli_welcome.default)({
    title: "Chakra UI CLI",
    tagLine: `by Chakra UI
${package_default.description}`,
    bgColor: `#319795`,
    color: `#FFFFFF`,
    bold: true,
    clear: false,
    version: package_default.version
  });
  updateNotifier({
    pkg: package_default,
    shouldNotifyInNpmScript: true,
    updateCheckInterval: 1e3 * 60 * 60 * 24 * 3
  }).notify({ isGlobal: true });
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  initCLI
});
/*!
 * @description Recursive object extending
 * @author Viacheslav Lotsmanov <lotsmanov89@gmail.com>
 * @license MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2018 Viacheslav Lotsmanov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * @preserve
 * JS Implementation of incremental MurmurHash3 (r150) (as of May 10, 2013)
 *
 * @author <a href="mailto:jensyt@gmail.com">Jens Taylor</a>
 * @see http://github.com/homebrewing/brauhaus-diff
 * @author <a href="mailto:gary.court@gmail.com">Gary Court</a>
 * @see http://github.com/garycourt/murmurhash-js
 * @author <a href="mailto:aappleby@gmail.com">Austin Appleby</a>
 * @see http://sites.google.com/site/murmurhash/
 */
