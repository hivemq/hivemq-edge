import { BubbleMenuPluginProps } from '@tiptap/extension-bubble-menu';
import React from 'react';
type Optional<T, K extends keyof T> = Pick<Partial<T>, K> & Omit<T, K>;
export type BubbleMenuProps = Omit<Optional<BubbleMenuPluginProps, 'pluginKey'>, 'element' | 'editor'> & {
    editor: BubbleMenuPluginProps['editor'] | null;
    className?: string;
    children: React.ReactNode;
    updateDelay?: number;
};
export declare const BubbleMenu: (props: BubbleMenuProps) => React.JSX.Element;
export {};
//# sourceMappingURL=BubbleMenu.d.ts.map