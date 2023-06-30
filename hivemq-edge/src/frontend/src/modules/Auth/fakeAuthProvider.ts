export const fakeAuthProvider = {
  isAuthenticated: false,
  login(callback: VoidFunction) {
    fakeAuthProvider.isAuthenticated = true
    setTimeout(callback, 1) // fake async
  },
  logout(callback: VoidFunction) {
    fakeAuthProvider.isAuthenticated = false
    setTimeout(callback, 1)
  },
}
