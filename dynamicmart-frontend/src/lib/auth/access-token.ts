/**
 * Access token chỉ tồn tại trong bộ nhớ của tab đang mở.
 * Không dùng localStorage/sessionStorage vì token sẽ dễ bị lấy qua XSS.
 */
let accessToken: string | null = null;

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}
