"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
var bluebird_1 = __importDefault(require("bluebird"));
var debug_1 = __importDefault(require("debug"));
var lodash_1 = __importDefault(require("lodash"));
var path = __importStar(require("path"));
var webpack_1 = __importDefault(require("webpack"));
var utils_1 = __importDefault(require("./lib/utils"));
var typescript_overrides_1 = require("./lib/typescript-overrides");
var debug = (0, debug_1.default)('cypress:webpack');
var debugStats = (0, debug_1.default)('cypress:webpack:stats');
// bundle promises from input spec filename to output bundled file paths
var bundles = {};
// we don't automatically load the rules, so that the babel dependencies are
// not required if a user passes in their own configuration
var getDefaultWebpackOptions = function () {
    debug('load default options');
    return {
        mode: 'development',
        module: {
            rules: [
                {
                    test: /\.jsx?$/,
                    exclude: [/node_modules/],
                    use: [
                        {
                            loader: 'babel-loader',
                            options: {
                                presets: ['@babel/preset-env'],
                            },
                        },
                    ],
                },
            ],
        },
    };
};
var replaceErrMessage = function (err, partToReplace, replaceWith) {
    if (replaceWith === void 0) { replaceWith = ''; }
    err.message = lodash_1.default.trim(err.message.replace(partToReplace, replaceWith));
    if (err.stack) {
        err.stack = lodash_1.default.trim(err.stack.replace(partToReplace, replaceWith));
    }
    return err;
};
var cleanModuleNotFoundError = function (err) {
    var message = err.message;
    if (!message.includes('Module not found'))
        return err;
    // Webpack 5 error messages are much less verbose. No need to clean.
    if ('NormalModule' in webpack_1.default) {
        return err;
    }
    var startIndex = message.lastIndexOf('resolve ');
    var endIndex = message.lastIndexOf("doesn't exist") + "doesn't exist".length;
    var partToReplace = message.substring(startIndex, endIndex);
    var newMessagePart = "Looked for and couldn't find the file at the following paths:";
    return replaceErrMessage(err, partToReplace, newMessagePart);
};
var cleanMultiNonsense = function (err) {
    var message = err.message;
    var startIndex = message.indexOf('@ multi');
    if (startIndex < 0)
        return err;
    var partToReplace = message.substring(startIndex);
    return replaceErrMessage(err, partToReplace);
};
var quietErrorMessage = function (err) {
    if (!err || !err.message)
        return err;
    err = cleanModuleNotFoundError(err);
    err = cleanMultiNonsense(err);
    return err;
};
/**
 * Webpack preprocessor configuration function. Takes configuration object
 * and returns file preprocessor.
 * @example
  ```
  on('file:preprocessor', webpackPreprocessor(options))
  ```
 */
// @ts-ignore
var preprocessor = function (options) {
    if (options === void 0) { options = {}; }
    debug('user options: %o', options);
    // we return function that accepts the arguments provided by
    // the event 'file:preprocessor'
    //
    // this function will get called for the support file when a project is loaded
    // (if the support file is not disabled)
    // it will also get called for a spec file when that spec is requested by
    // the Cypress runner
    //
    // when running in the GUI, it will likely get called multiple times
    // with the same filePath, as the user could re-run the tests, causing
    // the supported file and spec file to be requested again
    return function (file) {
        var filePath = file.filePath;
        debug('get', filePath);
        // since this function can get called multiple times with the same
        // filePath, we return the cached bundle promise if we already have one
        // since we don't want or need to re-initiate webpack for it
        if (bundles[filePath]) {
            debug("already have bundle for ".concat(filePath));
            return bundles[filePath].promise;
        }
        var defaultWebpackOptions = getDefaultWebpackOptions();
        // we're provided a default output path that lives alongside Cypress's
        // app data files so we don't have to worry about where to put the bundled
        // file on disk
        var outputPath = path.extname(file.outputPath) === '.js'
            ? file.outputPath
            : "".concat(file.outputPath, ".js");
        var entry = [filePath].concat(options.additionalEntries || []);
        var watchOptions = options.watchOptions || {};
        // user can override the default options
        var webpackOptions = lodash_1.default
            .chain(options.webpackOptions)
            .defaultTo(defaultWebpackOptions)
            .defaults({
            mode: defaultWebpackOptions.mode,
        })
            .assign({
            // we need to set entry and output
            entry: entry,
            output: {
                // disable automatic publicPath
                publicPath: '',
                path: path.dirname(outputPath),
                filename: path.basename(outputPath),
            },
        })
            .tap(function (opts) {
            if (opts.devtool === false) {
                // disable any overrides if we've explicitly turned off sourcemaps
                (0, typescript_overrides_1.overrideSourceMaps)(false, options.typescript);
                return;
            }
            debug('setting devtool to inline-source-map');
            opts.devtool = 'inline-source-map';
            // override typescript to always generate proper source maps
            (0, typescript_overrides_1.overrideSourceMaps)(true, options.typescript);
            // To support dynamic imports, we have to disable any code splitting.
            debug('Limiting number of chunks to 1');
            opts.plugins = (opts.plugins || []).concat(new webpack_1.default.optimize.LimitChunkCountPlugin({ maxChunks: 1 }));
        })
            .value();
        debug('webpackOptions: %o', webpackOptions);
        debug('watchOptions: %o', watchOptions);
        if (options.typescript)
            debug('typescript: %s', options.typescript);
        debug("input: ".concat(filePath));
        debug("output: ".concat(outputPath));
        var compiler = (0, webpack_1.default)(webpackOptions);
        var firstBundle = utils_1.default.createDeferred();
        // cache the bundle promise, so it can be returned if this function
        // is invoked again with the same filePath
        bundles[filePath] = {
            promise: firstBundle.promise,
            // we will resolve all reject everything in this array when a compile completes in the `handle` function
            deferreds: [firstBundle],
            initial: true,
        };
        var rejectWithErr = function (err) {
            err = quietErrorMessage(err);
            // @ts-ignore
            err.filePath = filePath;
            debug("errored bundling ".concat(outputPath), err.message);
            var lastBundle = bundles[filePath].deferreds[bundles[filePath].deferreds.length - 1];
            lastBundle.reject(err);
            bundles[filePath].deferreds.length = 0;
        };
        // this function is called when bundling is finished, once at the start
        // and, if watching, each time watching triggers a re-bundle
        var handle = function (err, stats) {
            if (err) {
                debug('handle - had error', err.message);
                return rejectWithErr(err);
            }
            var jsonStats = stats.toJson();
            // these stats are really only useful for debugging
            if (jsonStats.warnings.length > 0) {
                debug("warnings for ".concat(outputPath, " %o"), jsonStats.warnings);
            }
            if (stats.hasErrors()) {
                err = new Error('Webpack Compilation Error');
                var errorsToAppend = jsonStats.errors
                    // remove stack trace lines since they're useless for debugging
                    .map(cleanseError)
                    // multiple errors separated by newline
                    .join('\n\n');
                err.message += "\n".concat(errorsToAppend);
                debug('stats had error(s) %o', jsonStats.errors);
                return rejectWithErr(err);
            }
            debug('finished bundling', outputPath);
            if (debugStats.enabled) {
                /* eslint-disable-next-line no-console */
                console.error(stats.toString({ colors: true }));
            }
            // seems to be a race condition where changing file before next tick
            // does not cause build to rerun
            bluebird_1.default.delay(0).then(function () {
                if (!bundles[filePath]) {
                    return;
                }
                bundles[filePath].deferreds.forEach(function (deferred) {
                    // resolve with the outputPath so Cypress knows where to serve
                    // the file from
                    deferred.resolve(outputPath);
                });
                bundles[filePath].deferreds.length = 0;
            });
        };
        var plugin = { name: 'CypressWebpackPreprocessor' };
        // this event is triggered when watching and a file is saved
        var onCompile = function () {
            debug('compile', filePath);
            /**
             * Webpack 5 fix:
             * If the bundle is the initial bundle, do not create the deferred promise
             * as we already have one from above. Creating additional deferments on top of
             * the first bundle causes reference issues with the first bundle returned, meaning
             * the promise that is resolved/rejected is different from the one that is returned, which
             * makes the preprocessor permanently hang
             */
            if (!bundles[filePath].initial) {
                var nextBundle = utils_1.default.createDeferred();
                bundles[filePath].promise = nextBundle.promise;
                bundles[filePath].deferreds.push(nextBundle);
            }
            bundles[filePath].promise.finally(function () {
                debug('- compile finished for %s, initial? %s', filePath, bundles[filePath].initial);
                // when the bundling is finished, emit 'rerun' to let Cypress
                // know to rerun the spec, but NOT when it is the initial
                // bundling of the file
                if (!bundles[filePath].initial) {
                    file.emit('rerun');
                }
                bundles[filePath].initial = false;
            })
                // we suppress unhandled rejections so they don't bubble up to the
                // unhandledRejection handler and crash the process. Cypress will
                // eventually take care of the rejection when the file is requested.
                // note that this does not work if attached to latestBundle.promise
                // for some reason. it only works when attached after .finally  ¯\_(ツ)_/¯
                .suppressUnhandledRejections();
        };
        // when we should watch, we hook into the 'compile' hook so we know when
        // to rerun the tests
        if (file.shouldWatch) {
            if (compiler.hooks) {
                // TODO compile.tap takes "string | Tap"
                // so seems we just need to pass plugin.name
                // @ts-ignore
                compiler.hooks.compile.tap(plugin, onCompile);
            }
            else if ('plugin' in compiler) {
                // @ts-ignore
                compiler.plugin('compile', onCompile);
            }
        }
        var bundler = file.shouldWatch ? compiler.watch(watchOptions, handle) : compiler.run(handle);
        // when the spec or project is closed, we need to clean up the cached
        // bundle promise and stop the watcher via `bundler.close()`
        file.on('close', function (cb) {
            if (cb === void 0) { cb = function () { }; }
            debug('close', filePath);
            delete bundles[filePath];
            if (file.shouldWatch) {
                // in this case the bundler is webpack.Compiler.Watching
                if (bundler && 'close' in bundler) {
                    bundler.close(cb);
                }
            }
        });
        // return the promise, which will resolve with the outputPath or reject
        // with any error encountered
        return bundles[filePath].promise;
    };
};
// provide a clone of the default options
Object.defineProperty(preprocessor, 'defaultOptions', {
    get: function () {
        debug('get default options');
        return {
            webpackOptions: getDefaultWebpackOptions(),
            watchOptions: {},
        };
    },
});
// for testing purposes, but do not add this to the typescript interface
// @ts-ignore
preprocessor.__reset = function () {
    bundles = {};
};
// for testing purposes, but do not add this to the typescript interface
// @ts-ignore
preprocessor.__bundles = function () {
    return bundles;
};
// @ts-ignore - webpack.StatsError is unique to webpack 5
// TODO: Remove this when we update to webpack 5.
function cleanseError(err) {
    var msg = typeof err === 'string' ? err : err.message;
    return msg.replace(/\n\s*at.*/g, '').replace(/From previous event:\n?/g, '');
}
module.exports = preprocessor;
