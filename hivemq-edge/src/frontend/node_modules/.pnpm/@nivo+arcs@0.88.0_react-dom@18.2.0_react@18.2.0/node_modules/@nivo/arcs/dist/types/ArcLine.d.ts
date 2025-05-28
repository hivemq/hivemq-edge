/// <reference types="react" />
import { animated, AnimatedProps } from '@react-spring/web';
import { ExtractProps } from '@nivo/core';
type ArcLineProps = {
    animated: AnimatedProps<{
        radius: number;
        startAngle: number;
        endAngle: number;
        opacity: number;
    }>;
} & ExtractProps<typeof animated.path>;
export declare const ArcLine: ({ animated: animatedProps, ...rest }: ArcLineProps) => JSX.Element;
export {};
//# sourceMappingURL=ArcLine.d.ts.map