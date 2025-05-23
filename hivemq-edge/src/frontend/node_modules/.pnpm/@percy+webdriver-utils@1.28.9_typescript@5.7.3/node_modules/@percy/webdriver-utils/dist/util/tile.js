export default class Tile {
  constructor({
    content,
    statusBarHeight,
    navBarHeight,
    headerHeight,
    footerHeight,
    fullscreen,
    sha
  }) {
    this.content = content;
    this.statusBarHeight = statusBarHeight;
    this.navBarHeight = navBarHeight;
    this.headerHeight = headerHeight;
    this.footerHeight = footerHeight;
    this.fullscreen = fullscreen;
    this.sha = sha;
  }
}