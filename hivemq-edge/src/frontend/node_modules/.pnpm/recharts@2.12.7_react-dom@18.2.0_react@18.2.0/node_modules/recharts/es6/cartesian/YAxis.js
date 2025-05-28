function _extends() { _extends = Object.assign ? Object.assign.bind() : function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }
/**
 * @fileOverview Y Axis
 */
import React from 'react';
import clsx from 'clsx';
import { useChartHeight, useChartWidth, useYAxisOrThrow } from '../context/chartLayoutContext';
import { CartesianAxis } from './CartesianAxis';
import { getTicksOfAxis } from '../util/ChartUtils';
export var YAxis = function YAxis(_ref) {
  var yAxisId = _ref.yAxisId;
  var width = useChartWidth();
  var height = useChartHeight();
  var axisOptions = useYAxisOrThrow(yAxisId);
  if (axisOptions == null) {
    return null;
  }
  return (
    /*#__PURE__*/
    // @ts-expect-error the axisOptions type is not exactly what CartesianAxis is expecting.
    React.createElement(CartesianAxis, _extends({}, axisOptions, {
      className: clsx("recharts-".concat(axisOptions.axisType, " ").concat(axisOptions.axisType), axisOptions.className),
      viewBox: {
        x: 0,
        y: 0,
        width: width,
        height: height
      },
      ticksGenerator: function ticksGenerator(axis) {
        return getTicksOfAxis(axis, true);
      }
    }))
  );
};
YAxis.displayName = 'YAxis';
YAxis.defaultProps = {
  allowDuplicatedCategory: true,
  allowDecimals: true,
  hide: false,
  orientation: 'left',
  width: 60,
  height: 0,
  mirror: false,
  yAxisId: 0,
  tickCount: 5,
  type: 'number',
  padding: {
    top: 0,
    bottom: 0
  },
  allowDataOverflow: false,
  scale: 'auto',
  reversed: false
};