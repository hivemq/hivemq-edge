import { FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';
import React from 'react';
type Optional<T, K extends keyof T> = Pick<Partial<T>, K> & Omit<T, K>;
export type FloatingMenuProps = Omit<Optional<FloatingMenuPluginProps, 'pluginKey'>, 'element' | 'editor'> & {
    editor: FloatingMenuPluginProps['editor'] | null;
    className?: string;
    children: React.ReactNode;
};
export declare const FloatingMenu: (props: FloatingMenuProps) => React.JSX.Element;
export {};
//# sourceMappingURL=FloatingMenu.d.ts.map