'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var _objectSpread = require('@babel/runtime/helpers/objectSpread2');
var _objectWithoutProperties = require('@babel/runtime/helpers/objectWithoutProperties');
var memoizeOne = require('memoize-one');
var index$1 = require('../../dist/index-d1cb43f3.cjs.dev.js');
var React = require('react');
var _extends = require('@babel/runtime/helpers/extends');
var _slicedToArray = require('@babel/runtime/helpers/slicedToArray');
var reactTransitionGroup = require('react-transition-group');
require('@emotion/react');
require('@babel/runtime/helpers/typeof');
require('@babel/runtime/helpers/taggedTemplateLiteral');
require('@babel/runtime/helpers/defineProperty');
require('react-dom');
require('@floating-ui/dom');
require('use-isomorphic-layout-effect');

function _interopDefault (e) { return e && e.__esModule ? e : { 'default': e }; }

function _interopNamespace(e) {
  if (e && e.__esModule) return e;
  var n = Object.create(null);
  if (e) {
    Object.keys(e).forEach(function (k) {
      if (k !== 'default') {
        var d = Object.getOwnPropertyDescriptor(e, k);
        Object.defineProperty(n, k, d.get ? d : {
          enumerable: true,
          get: function () { return e[k]; }
        });
      }
    });
  }
  n["default"] = e;
  return Object.freeze(n);
}

var memoizeOne__default = /*#__PURE__*/_interopDefault(memoizeOne);
var React__namespace = /*#__PURE__*/_interopNamespace(React);

var _excluded$4 = ["in", "onExited", "appear", "enter", "exit"];
// strip transition props off before spreading onto select component
var AnimatedInput = function AnimatedInput(WrappedComponent) {
  return function (_ref) {
    _ref.in;
      _ref.onExited;
      _ref.appear;
      _ref.enter;
      _ref.exit;
      var props = _objectWithoutProperties(_ref, _excluded$4);
    return /*#__PURE__*/React__namespace.createElement(WrappedComponent, props);
  };
};
var AnimatedInput$1 = AnimatedInput;

var _excluded$3 = ["component", "duration", "in", "onExited"];
var Fade = function Fade(_ref) {
  var Tag = _ref.component,
    _ref$duration = _ref.duration,
    duration = _ref$duration === void 0 ? 1 : _ref$duration,
    inProp = _ref.in;
    _ref.onExited;
    var props = _objectWithoutProperties(_ref, _excluded$3);
  var nodeRef = React.useRef(null);
  var transition = {
    entering: {
      opacity: 0
    },
    entered: {
      opacity: 1,
      transition: "opacity ".concat(duration, "ms")
    },
    exiting: {
      opacity: 0
    },
    exited: {
      opacity: 0
    }
  };
  return /*#__PURE__*/React__namespace.createElement(reactTransitionGroup.Transition, {
    mountOnEnter: true,
    unmountOnExit: true,
    in: inProp,
    timeout: duration,
    nodeRef: nodeRef
  }, function (state) {
    var innerProps = {
      style: _objectSpread({}, transition[state]),
      ref: nodeRef
    };
    return /*#__PURE__*/React__namespace.createElement(Tag, _extends({
      innerProps: innerProps
    }, props));
  });
};

// ==============================
// Collapse Transition
// ==============================

var collapseDuration = 260;
// wrap each MultiValue with a collapse transition; decreases width until
// finally removing from DOM
var Collapse = function Collapse(_ref2) {
  var children = _ref2.children,
    _in = _ref2.in,
    _onExited = _ref2.onExited;
  var ref = React.useRef(null);
  var _useState = React.useState('auto'),
    _useState2 = _slicedToArray(_useState, 2),
    width = _useState2[0],
    setWidth = _useState2[1];
  React.useEffect(function () {
    var el = ref.current;
    if (!el) return;

    /*
      Here we're invoking requestAnimationFrame with a callback invoking our
      call to getBoundingClientRect and setState in order to resolve an edge case
      around portalling. Certain portalling solutions briefly remove children from the DOM
      before appending them to the target node. This is to avoid us trying to call getBoundingClientrect
      while the Select component is in this state.
    */
    // cannot use `offsetWidth` because it is rounded
    var rafId = window.requestAnimationFrame(function () {
      return setWidth(el.getBoundingClientRect().width);
    });
    return function () {
      return window.cancelAnimationFrame(rafId);
    };
  }, []);
  var getStyleFromStatus = function getStyleFromStatus(status) {
    switch (status) {
      default:
        return {
          width: width
        };
      case 'exiting':
        return {
          width: 0,
          transition: "width ".concat(collapseDuration, "ms ease-out")
        };
      case 'exited':
        return {
          width: 0
        };
    }
  };
  return /*#__PURE__*/React__namespace.createElement(reactTransitionGroup.Transition, {
    enter: false,
    mountOnEnter: true,
    unmountOnExit: true,
    in: _in,
    onExited: function onExited() {
      var el = ref.current;
      if (!el) return;
      _onExited === null || _onExited === void 0 ? void 0 : _onExited(el);
    },
    timeout: collapseDuration,
    nodeRef: ref
  }, function (status) {
    return /*#__PURE__*/React__namespace.createElement("div", {
      ref: ref,
      style: _objectSpread({
        overflow: 'hidden',
        whiteSpace: 'nowrap'
      }, getStyleFromStatus(status))
    }, children);
  });
};

var _excluded$2 = ["in", "onExited"];
// strip transition props off before spreading onto actual component

var AnimatedMultiValue = function AnimatedMultiValue(WrappedComponent) {
  return function (_ref) {
    var inProp = _ref.in,
      onExited = _ref.onExited,
      props = _objectWithoutProperties(_ref, _excluded$2);
    return /*#__PURE__*/React__namespace.createElement(Collapse, {
      in: inProp,
      onExited: onExited
    }, /*#__PURE__*/React__namespace.createElement(WrappedComponent, _extends({
      cropWithEllipsis: inProp
    }, props)));
  };
};
var AnimatedMultiValue$1 = AnimatedMultiValue;

// fade in when last multi-value removed, otherwise instant
var AnimatedPlaceholder = function AnimatedPlaceholder(WrappedComponent) {
  return function (props) {
    return /*#__PURE__*/React__namespace.createElement(Fade, _extends({
      component: WrappedComponent,
      duration: props.isMulti ? collapseDuration : 1
    }, props));
  };
};
var AnimatedPlaceholder$1 = AnimatedPlaceholder;

// instant fade; all transition-group children must be transitions

var AnimatedSingleValue = function AnimatedSingleValue(WrappedComponent) {
  return function (props) {
    return /*#__PURE__*/React__namespace.createElement(Fade, _extends({
      component: WrappedComponent
    }, props));
  };
};
var AnimatedSingleValue$1 = AnimatedSingleValue;

var _excluded$1 = ["component"],
  _excluded2 = ["children"];
// make ValueContainer a transition group
var AnimatedValueContainer = function AnimatedValueContainer(WrappedComponent) {
  return function (props) {
    return props.isMulti ? /*#__PURE__*/React__namespace.createElement(IsMultiValueContainer, _extends({
      component: WrappedComponent
    }, props)) : /*#__PURE__*/React__namespace.createElement(reactTransitionGroup.TransitionGroup, _extends({
      component: WrappedComponent
    }, props));
  };
};
var IsMultiValueContainer = function IsMultiValueContainer(_ref) {
  var component = _ref.component,
    restProps = _objectWithoutProperties(_ref, _excluded$1);
  var multiProps = useIsMultiValueContainer(restProps);
  return /*#__PURE__*/React__namespace.createElement(reactTransitionGroup.TransitionGroup, _extends({
    component: component
  }, multiProps));
};
var useIsMultiValueContainer = function useIsMultiValueContainer(_ref2) {
  var children = _ref2.children,
    props = _objectWithoutProperties(_ref2, _excluded2);
  var isMulti = props.isMulti,
    hasValue = props.hasValue,
    innerProps = props.innerProps,
    _props$selectProps = props.selectProps,
    components = _props$selectProps.components,
    controlShouldRenderValue = _props$selectProps.controlShouldRenderValue;
  var _useState = React.useState(isMulti && controlShouldRenderValue && hasValue),
    _useState2 = _slicedToArray(_useState, 2),
    cssDisplayFlex = _useState2[0],
    setCssDisplayFlex = _useState2[1];
  var _useState3 = React.useState(false),
    _useState4 = _slicedToArray(_useState3, 2),
    removingValue = _useState4[0],
    setRemovingValue = _useState4[1];
  React.useEffect(function () {
    if (hasValue && !cssDisplayFlex) {
      setCssDisplayFlex(true);
    }
  }, [hasValue, cssDisplayFlex]);
  React.useEffect(function () {
    if (removingValue && !hasValue && cssDisplayFlex) {
      setCssDisplayFlex(false);
    }
    setRemovingValue(false);
  }, [removingValue, hasValue, cssDisplayFlex]);
  var onExited = function onExited() {
    return setRemovingValue(true);
  };
  var childMapper = function childMapper(child) {
    if (isMulti && /*#__PURE__*/React__namespace.isValidElement(child)) {
      // Add onExited callback to MultiValues
      if (child.type === components.MultiValue) {
        return /*#__PURE__*/React__namespace.cloneElement(child, {
          onExited: onExited
        });
      }
      // While container flexed, Input cursor is shown after Placeholder text,
      // so remove Placeholder until display is set back to grid
      if (child.type === components.Placeholder && cssDisplayFlex) {
        return null;
      }
    }
    return child;
  };
  var newInnerProps = _objectSpread(_objectSpread({}, innerProps), {}, {
    style: _objectSpread(_objectSpread({}, innerProps === null || innerProps === void 0 ? void 0 : innerProps.style), {}, {
      display: isMulti && hasValue || cssDisplayFlex ? 'flex' : 'grid'
    })
  });
  var newProps = _objectSpread(_objectSpread({}, props), {}, {
    innerProps: newInnerProps,
    children: React__namespace.Children.toArray(children).map(childMapper)
  });
  return newProps;
};
var AnimatedValueContainer$1 = AnimatedValueContainer;

var _excluded = ["Input", "MultiValue", "Placeholder", "SingleValue", "ValueContainer"];
var makeAnimated = function makeAnimated() {
  var externalComponents = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
  var components = index$1.defaultComponents({
    components: externalComponents
  });
  var Input = components.Input,
    MultiValue = components.MultiValue,
    Placeholder = components.Placeholder,
    SingleValue = components.SingleValue,
    ValueContainer = components.ValueContainer,
    rest = _objectWithoutProperties(components, _excluded);
  return _objectSpread({
    Input: AnimatedInput$1(Input),
    MultiValue: AnimatedMultiValue$1(MultiValue),
    Placeholder: AnimatedPlaceholder$1(Placeholder),
    SingleValue: AnimatedSingleValue$1(SingleValue),
    ValueContainer: AnimatedValueContainer$1(ValueContainer)
  }, rest);
};
var AnimatedComponents = makeAnimated();
var Input = AnimatedComponents.Input;
var MultiValue = AnimatedComponents.MultiValue;
var Placeholder = AnimatedComponents.Placeholder;
var SingleValue = AnimatedComponents.SingleValue;
var ValueContainer = AnimatedComponents.ValueContainer;
var index = memoizeOne__default["default"](makeAnimated);

exports.Input = Input;
exports.MultiValue = MultiValue;
exports.Placeholder = Placeholder;
exports.SingleValue = SingleValue;
exports.ValueContainer = ValueContainer;
exports["default"] = index;
