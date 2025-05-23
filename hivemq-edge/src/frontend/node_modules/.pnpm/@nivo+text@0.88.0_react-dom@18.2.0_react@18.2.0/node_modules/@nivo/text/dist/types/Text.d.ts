import { PropsWithChildren, ComponentType } from 'react';
import { animated } from '@react-spring/web';
import { TextStyle as ThemeStyle } from '@nivo/core';
type GetComponentProps<T> = T extends ComponentType<infer P> ? P : never;
type AnimatedComponentProps = GetComponentProps<(typeof animated)['text']>;
type TextProps = PropsWithChildren<Omit<AnimatedComponentProps, 'style'> & {
    style: AnimatedComponentProps['style'] & Pick<ThemeStyle, 'outlineWidth' | 'outlineColor' | 'outlineOpacity'>;
}>;
export declare const Text: ({ style: fullStyle, children, ...attributes }: TextProps) => JSX.Element;
export {};
//# sourceMappingURL=Text.d.ts.map