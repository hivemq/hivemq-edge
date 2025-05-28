'use strict';

exports.__esModule = true;
exports.default = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _utils = require('./utils');

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var Pseudo = function () {
  function Pseudo() {
    var _ref = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {},
        _ref$languageToPseudo = _ref.languageToPseudo,
        languageToPseudo = _ref$languageToPseudo === undefined ? 'en' : _ref$languageToPseudo,
        _ref$letterMultiplier = _ref.letterMultiplier,
        letterMultiplier = _ref$letterMultiplier === undefined ? 2 : _ref$letterMultiplier,
        _ref$repeatedLetters = _ref.repeatedLetters,
        repeatedLetters = _ref$repeatedLetters === undefined ? _utils.vowels : _ref$repeatedLetters,
        _ref$uglifedLetterObj = _ref.uglifedLetterObject,
        uglifedLetterObject = _ref$uglifedLetterObj === undefined ? _utils.uglifiedAlphabet : _ref$uglifedLetterObj,
        _ref$wrapped = _ref.wrapped,
        wrapped = _ref$wrapped === undefined ? false : _ref$wrapped,
        _ref$enabled = _ref.enabled,
        enabled = _ref$enabled === undefined ? true : _ref$enabled;

    _classCallCheck(this, Pseudo);

    this.name = 'pseudo';
    this.type = 'postProcessor';
    this.options = {
      languageToPseudo: languageToPseudo,
      letterMultiplier: letterMultiplier,
      wrapped: wrapped,
      repeatedLetters: repeatedLetters,
      letters: uglifedLetterObject,
      enabled: enabled
    };
  }

  Pseudo.prototype.configurePseudo = function configurePseudo(options) {
    this.options = _extends({}, this.options, options);
  };

  Pseudo.prototype.process = function process(value, key, options, translator) {
    var _this = this;

    if (translator.language && this.options.languageToPseudo !== translator.language || !this.options.enabled) {
      return value;
    }
    var bracketCount = 0;
    var processedValue = value.split('').map(function (letter) {
      if (letter === '}') {
        bracketCount = 0;
        return letter;
      }
      if (letter === '{') {
        bracketCount++;
        return letter;
      }
      if (bracketCount === 2) return letter;

      return _this.options.repeatedLetters.indexOf(letter) !== -1 ? _this.options.letters[letter].repeat(_this.options.letterMultiplier) : _this.options.letters[letter] || letter;
    }).join('');
    return (0, _utils.stringWrapper)({ shouldWrap: this.options.wrapped, string: processedValue });
  };

  return Pseudo;
}();

exports.default = Pseudo;
module.exports = exports['default'];