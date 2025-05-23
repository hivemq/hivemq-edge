# bind-event-listener

[![npm](https://img.shields.io/npm/v/bind-event-listener.svg)](https://www.npmjs.com/package/bind-event-listener)
![types](https://img.shields.io/badge/types-typescript-blueviolet)
[![minzip](https://img.shields.io/bundlephobia/minzip/bind-event-listener.svg)](https://www.npmjs.com/package/bind-event-listener)

A well typed utility to make creating and removing DOM event listeners safer and more ergonomic.

```ts
import { bind, UnbindFn } from 'bind-event-listener';

const unbind: UnbindFn = bind(button, {
  type: 'click',
  listener: function onClick(event) {},
});

// when you are all done:
unbind();
```

```ts
import { bindAll } from 'bind-event-listener';

const unbind = bindAll(button, [
  {
    type: 'click',
    listener: function onClick(event) {},
    options: { capture: true },
  },
  {
    type: 'mouseover',
    listener: function onMouseOver(event) {},
  },
]);

// when you are all done:
unbind();
```

## Rationale

When using `addEventListener()`, **correctly unbinding** events with `removeEventListener()` can be tricky.

1. You need to remember to call `removeEventListener` (it can be easy to forget!)

<details>
<summary>Example</summary>

```ts
target.addEventListener('click', onClick, options);

target.removeEventListener('click', onClick, options);
```

</details>

2. You need to pass in the same listener _reference_ to `removeEventListener`

<details>
<summary>Example</summary>

```ts
target.addEventListener(
  'click',
  function onClick() {
    console.log('clicked');
  },
  options,
);

// Even those the functions look the same, they don't have the same reference.
// The original onClick is not unbound!
target.removeEventListener(
  'click',
  function onClick() {
    console.log('clicked');
  },
  options,
);
```

```ts
// Inline arrow functions can never be unbound because you have lost the reference!
target.addEventListener('click', () => console.log('i will never unbind'), options);
target.removeEventListener('click', () => console.log('i will never unbind'), options);
```

</details>

3. You need to pass in the same `capture` value option

<details>
<summary>Example</summary>

```ts
// add a listener: AddEventListenerOptions format
target.addEventListener('click', onClick, { capture: true });

// not unbound: no capture value
target.removeEventListener('click', onClick);

// not unbound: different capture value
target.removeEventListener('click', onClick, { capture: false });

// successfully unbound: same capture value
target.removeEventListener('click', onClick, { capture: true });
// this would also unbind (different notation)
target.removeEventListener('click', onClick, true /* shorthand for { capture: true } */);
```

```ts
// add a listener: boolean capture format
target.addEventListener('click', onClick, true /* shorthand for { capture: true } */);

// not unbound: no capture value
target.addEventListener('click', onClick);
// not unbound: different capture value
target.addEventListener('click', onClick, false);

// successfully unbound: same capture value
target.addEventListener('click', onClick, true);
// this would also unbind (different notation)
target.addEventListener('click', onClick, { capture: true });
```

</details>

**`bind-event-listener` solves these problems**

1. When you bind an event (or events with `bindAll`) you get back a simple `unbind` function
2. The unbind function ensures the same listener _reference_ is passed to `removeEventListener`
3. The unbind function ensures that whatever `capture` value is used with `addEventListener` is used with `removeEventListener`

You will find an even fuller rationale for this project in my course: ["The Ultimate Guide for Understanding DOM Events"](https://egghead.io/courses/the-ultimate-guide-for-understanding-dom-events-6c0c0d23?af=2jc3e4)

[![share-card-dom-events](https://user-images.githubusercontent.com/2182637/120963089-52a45f00-c7a4-11eb-82a7-a04c2731999a.jpg)](https://egghead.io/courses/the-ultimate-guide-for-understanding-dom-events-6c0c0d23?af=2jc3e4)

## Usage

### `bind`: basic

```ts
import { bind, UnbindFn } from 'bind-event-listener';

const unbind: UnbindFn = bind(button, {
  type: 'click',
  listener: onClick,
});

// when your are all done:
unbind();
```

### `bind`: with options

```ts
import { bind } from 'bind-event-listener';

const unbind = bind(button, {
  type: 'click',
  listener: onClick,
  options: { capture: true, passive: false },
});

// when you are all done:
unbind();
```

### `bindAll`: basic

```ts
import { bindAll } from 'bind-event-listener';

const unbind = bindAll(button, [
  {
    type: 'click',
    listener: onClick,
  },
]);

// when you are all done:
unbind();
```

### `bindAll`: with options

```ts
import { bindAll } from 'bind-event-listener';

const unbind = bindAll(button, [
  {
    type: 'click',
    listener: onClick,
    options: { passive: true },
  },
  // default options that are applied to all bindings
  { capture: false },
]);

// when you are all done:
unbind();
```

When using `defaultOptions` for `bindAll`, the `defaultOptions` are merged with the `options` on each binding. Options on the individual bindings will take precedent. You can think of it like this:

```ts
const merged: AddEventListenerOptions = {
  ...defaultOptions,
  ...options,
};
```

> Note: it is a little bit more complicated than just object spreading as the library will also behave correctly when passing in a `boolean` capture argument. An options value can be a boolean `{ options: true }` which is shorthand for `{ options: {capture: true } }`

## Types

Thanks to the great work by [@Ayub-Begimkulov](https://github.com/Ayub-Begimkulov) and [@Andarist](https://github.com/Andarist) `bind-event-listener` has fantastic TypeScript types and auto complete.

> ⚠️ TypeScript 4.1+ is required for types
> ⚠️ TypeScript 5.0+ is required for event name autocompletion

```ts
import invariant from 'tiny-invariant';
import { bind } from 'bind-event-listener';

bind(window, {
  type: 'click',
  function: function onClick(event) {
    // `event` is correctly typed as a 'MouseEvent'
    // `this` is correctly typed as `window` (the event target that the event listener is added to)
  },
});

const button = document.querySelector('button');
invariant(button instanceof HTMLElement);

bind(button, {
  type: 'click',
  function: function onClick(event) {
    // `event` is correctly typed as a 'MouseEvent'
    // `this` is correctly typed as `button` (the event target that the event listener is added to)
  },
});

const object = {
  handleEvent: function onClick(event) {
    // `event` is correctly typed as a 'MouseEvent'
    // `this` is correctly typed as `object` (the event listener object that the event listener is added to)
  },
};

bind(button, {
  type: 'click',
  function: object,
});
```

`bind` and `bindAll` accept type arguments (generics), but it is generally best to let these be inferred

```ts
// with explicit type arguments
bind<HTMLElement, 'click'>(button, {
  type: 'click',
  listener: function onClick() {},
});

// ✨ types will automatically be inferred for you ✨
bind(button, {
  type: 'click',
  listener: function onClick() {},
});

// with explicit type arguments
bindAll<HTMLElement, ['click', 'keydown']>(button, [
  {
    type: 'click',
    listener: function onClick() {},
  },
  {
    type: 'keydown',
    listener: function onKeyDown() {},
  },
]);

// ✨ types will automatically be inferred for you ✨
bindAll(button, [
  {
    type: 'click',
    listener: function onClick() {},
  },
  {
    type: 'keydown',
    listener: function onKeyDown() {},
  },
]);
```

> Typescript built in DOM types: [raw view](https://raw.githubusercontent.com/microsoft/TypeScript/master/lib/lib.dom.d.ts), [pretty view](https://github.com/microsoft/TypeScript/blob/master/lib/lib.dom.d.ts) (warning: pretty view seems to crash Github!)

### Type helpers

```ts
import { Binding, Listener, UnbindFn } from 'bind-event-listener';
```

`Listener`: the `function` or `object` that you provide to the `listener` property of a `Binding`

```ts
bind(button, {
  type: 'click',
  listener: function onClick() {}, // ← `Listener`
});
```

`Binding`: the definition of an event binding.

```ts
bind(
  button,
  // ↓ `Binding`
  {
    type: 'click',
    listener: function onClick() {},
  },
);
```

`UnbindFn`: a named type for `() => void` to make it clearer that the function will unbind the added event listener(s):

```ts
const unbind: UnbindFn = bind(button, { type: 'click', listener: function onClick() {} });
```

## Recipe: [`react`](https://reactjs.org/) effect

You can return a [cleanup function](https://reactjs.org/docs/hooks-reference.html#cleaning-up-an-effect) from [`useEffect`](https://reactjs.org/docs/hooks-reference.html#useeffect) (or [`useLayoutEffect`](https://reactjs.org/docs/hooks-reference.html#uselayouteffect)). `bind-event-listener` makes this super convenient because you can just return the unbind function from your effect.

```ts
import React, { useState, useEffect } from 'react';
import { bind } from 'bind-event-listener';

export default function App() {
  const [clickCount, onClick] = useState(0);

  useEffect(() => {
    const unbind = bind(window, {
      type: 'click',
      listener: () => onClick((value) => value + 1),
    });

    return unbind;
  }, []);

  return <div>Window clicks: {clickCount}</div>;
}
```

> You can play with this [example on codesandbox](https://codesandbox.io/s/bind-event-listener-useeffect-mnfi3)

## Cheers 👋

Brought to you by [@alexandereardon](https://twitter.com/alexandereardon)
