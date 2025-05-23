import fs from 'fs';
import { createResource, createRootResource } from '@percy/cli-command/utils';
export { yieldAll } from '@percy/cli-command/utils';

// Returns root resource and image resource objects based on image properties. The root resource is
// a generated DOM designed to display an image at it's native size without margins or padding.
export async function getImageResources({
  name,
  type,
  width,
  height,
  relativePath,
  absolutePath
}) {
  let rootUrl = `http://local/${encodeURIComponent(name)}`;
  let imageUrl = `http://local/${encodeURIComponent(relativePath)}`;
  let content = await fs.promises.readFile(absolutePath);
  let mimetype = `image/${type}`;
  return [createRootResource(rootUrl, `
      <!doctype html>
      <html lang="en">
        <head>
          <meta charset="utf-8">
          <title>${name}</title>
          <style>
            *, *::before, *::after { margin: 0; padding: 0; font-size: 0; }
            html, body { width: 100%; }
            img { max-width: 100%; }
          </style>
        </head>
        <body>
          <img src="${imageUrl}" width="${width}px" height="${height}px"/>
        </body>
      </html>
    `), createResource(imageUrl, content, mimetype)];
}