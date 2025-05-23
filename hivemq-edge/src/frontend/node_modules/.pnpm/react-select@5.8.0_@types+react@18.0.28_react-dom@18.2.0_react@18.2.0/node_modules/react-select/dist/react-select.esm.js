import { u as useStateManager } from './useStateManager-7e1e8489.esm.js';
export { u as useStateManager } from './useStateManager-7e1e8489.esm.js';
import _extends from '@babel/runtime/helpers/esm/extends';
import * as React from 'react';
import { forwardRef, useMemo } from 'react';
import { S as Select } from './Select-49a62830.esm.js';
export { c as createFilter, d as defaultTheme, m as mergeStyles } from './Select-49a62830.esm.js';
import { CacheProvider } from '@emotion/react';
import createCache from '@emotion/cache';
export { c as components } from './index-a301f526.esm.js';
import '@babel/runtime/helpers/objectSpread2';
import '@babel/runtime/helpers/slicedToArray';
import '@babel/runtime/helpers/objectWithoutProperties';
import '@babel/runtime/helpers/classCallCheck';
import '@babel/runtime/helpers/createClass';
import '@babel/runtime/helpers/inherits';
import '@babel/runtime/helpers/createSuper';
import '@babel/runtime/helpers/toConsumableArray';
import 'memoize-one';
import '@babel/runtime/helpers/typeof';
import '@babel/runtime/helpers/taggedTemplateLiteral';
import '@babel/runtime/helpers/defineProperty';
import 'react-dom';
import '@floating-ui/dom';
import 'use-isomorphic-layout-effect';

var StateManagedSelect = /*#__PURE__*/forwardRef(function (props, ref) {
  var baseSelectProps = useStateManager(props);
  return /*#__PURE__*/React.createElement(Select, _extends({
    ref: ref
  }, baseSelectProps));
});
var StateManagedSelect$1 = StateManagedSelect;

var NonceProvider = (function (_ref) {
  var nonce = _ref.nonce,
    children = _ref.children,
    cacheKey = _ref.cacheKey;
  var emotionCache = useMemo(function () {
    return createCache({
      key: cacheKey,
      nonce: nonce
    });
  }, [cacheKey, nonce]);
  return /*#__PURE__*/React.createElement(CacheProvider, {
    value: emotionCache
  }, children);
});

export { NonceProvider, StateManagedSelect$1 as default };
