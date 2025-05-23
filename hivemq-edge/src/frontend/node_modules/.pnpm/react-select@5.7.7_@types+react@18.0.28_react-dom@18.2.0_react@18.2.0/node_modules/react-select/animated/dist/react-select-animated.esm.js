import _objectSpread from '@babel/runtime/helpers/esm/objectSpread2';
import _objectWithoutProperties from '@babel/runtime/helpers/esm/objectWithoutProperties';
import memoizeOne from 'memoize-one';
import { F as defaultComponents } from '../../dist/index-a301f526.esm.js';
import * as React from 'react';
import { useRef, useState, useEffect } from 'react';
import _extends from '@babel/runtime/helpers/esm/extends';
import _slicedToArray from '@babel/runtime/helpers/esm/slicedToArray';
import { Transition, TransitionGroup } from 'react-transition-group';
import '@emotion/react';
import '@babel/runtime/helpers/typeof';
import '@babel/runtime/helpers/taggedTemplateLiteral';
import '@babel/runtime/helpers/defineProperty';
import 'react-dom';
import '@floating-ui/dom';
import 'use-isomorphic-layout-effect';

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
    return /*#__PURE__*/React.createElement(WrappedComponent, props);
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
  var nodeRef = useRef(null);
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
  return /*#__PURE__*/React.createElement(Transition, {
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
    return /*#__PURE__*/React.createElement(Tag, _extends({
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
  var ref = useRef(null);
  var _useState = useState('auto'),
    _useState2 = _slicedToArray(_useState, 2),
    width = _useState2[0],
    setWidth = _useState2[1];
  useEffect(function () {
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
  return /*#__PURE__*/React.createElement(Transition, {
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
    return /*#__PURE__*/React.createElement("div", {
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
    return /*#__PURE__*/React.createElement(Collapse, {
      in: inProp,
      onExited: onExited
    }, /*#__PURE__*/React.createElement(WrappedComponent, _extends({
      cropWithEllipsis: inProp
    }, props)));
  };
};
var AnimatedMultiValue$1 = AnimatedMultiValue;

// fade in when last multi-value removed, otherwise instant
var AnimatedPlaceholder = function AnimatedPlaceholder(WrappedComponent) {
  return function (props) {
    return /*#__PURE__*/React.createElement(Fade, _extends({
      component: WrappedComponent,
      duration: props.isMulti ? collapseDuration : 1
    }, props));
  };
};
var AnimatedPlaceholder$1 = AnimatedPlaceholder;

// instant fade; all transition-group children must be transitions

var AnimatedSingleValue = function AnimatedSingleValue(WrappedComponent) {
  return function (props) {
    return /*#__PURE__*/React.createElement(Fade, _extends({
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
    return props.isMulti ? /*#__PURE__*/React.createElement(IsMultiValueContainer, _extends({
      component: WrappedComponent
    }, props)) : /*#__PURE__*/React.createElement(TransitionGroup, _extends({
      component: WrappedComponent
    }, props));
  };
};
var IsMultiValueContainer = function IsMultiValueContainer(_ref) {
  var component = _ref.component,
    restProps = _objectWithoutProperties(_ref, _excluded$1);
  var multiProps = useIsMultiValueContainer(restProps);
  return /*#__PURE__*/React.createElement(TransitionGroup, _extends({
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
  var _useState = useState(isMulti && controlShouldRenderValue && hasValue),
    _useState2 = _slicedToArray(_useState, 2),
    cssDisplayFlex = _useState2[0],
    setCssDisplayFlex = _useState2[1];
  var _useState3 = useState(false),
    _useState4 = _slicedToArray(_useState3, 2),
    removingValue = _useState4[0],
    setRemovingValue = _useState4[1];
  useEffect(function () {
    if (hasValue && !cssDisplayFlex) {
      setCssDisplayFlex(true);
    }
  }, [hasValue, cssDisplayFlex]);
  useEffect(function () {
    if (removingValue && !hasValue && cssDisplayFlex) {
      setCssDisplayFlex(false);
    }
    setRemovingValue(false);
  }, [removingValue, hasValue, cssDisplayFlex]);
  var onExited = function onExited() {
    return setRemovingValue(true);
  };
  var childMapper = function childMapper(child) {
    if (isMulti && /*#__PURE__*/React.isValidElement(child)) {
      // Add onExited callback to MultiValues
      if (child.type === components.MultiValue) {
        return /*#__PURE__*/React.cloneElement(child, {
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
    children: React.Children.toArray(children).map(childMapper)
  });
  return newProps;
};
var AnimatedValueContainer$1 = AnimatedValueContainer;

var _excluded = ["Input", "MultiValue", "Placeholder", "SingleValue", "ValueContainer"];
var makeAnimated = function makeAnimated() {
  var externalComponents = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
  var components = defaultComponents({
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
var index = memoizeOne(makeAnimated);

export { Input, MultiValue, Placeholder, SingleValue, ValueContainer, index as default };
