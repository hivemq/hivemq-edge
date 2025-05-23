import React, { ReactElement, SVGProps } from 'react';
import { ChartOffset, D3Scale } from '../util/types';
import { Props as XAxisProps } from './XAxis';
import { Props as YAxisProps } from './YAxis';
type XAxisWithD3Scale = Omit<XAxisProps, 'scale'> & {
    scale: D3Scale<string | number>;
};
type YAxisWithD3Scale = Omit<YAxisProps, 'scale'> & {
    scale: D3Scale<string | number>;
};
export type GridLineTypeFunctionProps = Omit<LineItemProps, 'key'> & {
    key: LineItemProps['key'] | undefined;
    offset: ChartOffset;
    xAxis: null | XAxisWithD3Scale;
    yAxis: null | YAxisWithD3Scale;
};
type GridLineType = SVGProps<SVGLineElement> | ReactElement<SVGElement> | ((props: GridLineTypeFunctionProps) => ReactElement<SVGElement>) | boolean;
export type HorizontalCoordinatesGenerator = (props: {
    yAxis: any;
    width: number;
    height: number;
    offset: ChartOffset;
}, syncWithTicks: boolean) => number[];
export type VerticalCoordinatesGenerator = (props: {
    xAxis: any;
    width: number;
    height: number;
    offset: ChartOffset;
}, syncWithTicks: boolean) => number[];
interface InternalCartesianGridProps {
    width?: number;
    height?: number;
    horizontalCoordinatesGenerator?: HorizontalCoordinatesGenerator;
    verticalCoordinatesGenerator?: VerticalCoordinatesGenerator;
}
interface CartesianGridProps extends InternalCartesianGridProps {
    x?: number;
    y?: number;
    horizontal?: GridLineType;
    vertical?: GridLineType;
    horizontalPoints?: number[];
    verticalPoints?: number[];
    verticalFill?: string[];
    horizontalFill?: string[];
    syncWithTicks?: boolean;
    horizontalValues?: number[] | string[];
    verticalValues?: number[] | string[];
}
type AcceptedSvgProps = Omit<SVGProps<SVGElement>, 'offset'>;
export type Props = AcceptedSvgProps & CartesianGridProps;
type LineItemProps = Props & {
    offset: ChartOffset;
    xAxis: null | XAxisWithD3Scale;
    yAxis: null | YAxisWithD3Scale;
    x1: number;
    y1: number;
    x2: number;
    y2: number;
    key: string;
    index: number;
};
export declare function CartesianGrid(props: Props): React.JSX.Element;
export declare namespace CartesianGrid {
    var displayName: string;
}
export {};
