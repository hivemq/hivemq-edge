import React from 'react';
import { ActiveShape } from './types';
import { BarProps } from '../cartesian/Bar';
type BarRectangleProps = {
    option: ActiveShape<BarProps, SVGPathElement>;
    isActive: boolean;
} & BarProps;
export declare function BarRectangle(props: BarRectangleProps): React.JSX.Element;
export type MinPointSize = number | ((value: number, index: number) => number);
export declare const minPointSizeCallback: (minPointSize: MinPointSize, defaultValue?: number) => (value: unknown, index: number) => number;
export {};
