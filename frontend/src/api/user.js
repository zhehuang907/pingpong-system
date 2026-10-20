import request from "@/utils/request";
import { buildDeviceProfile } from "@/utils/auth";

export function login(data) {
  const deviceProfile = buildDeviceProfile();
  const payload = {
    ...data,
    rememberMe: Boolean(data?.rememberMe ?? data?.remember),
    deviceFingerprint: data?.deviceFingerprint || deviceProfile.deviceFingerprint,
    deviceName: data?.deviceName || deviceProfile.deviceName,
  };

  return request({
    url: "/user/login",
    method: "post",
    data: payload,
  }).then((res) => ({
    ...res,
    data: res.data?.token || res.data,
    loginPayload: res.data,
  }));
}

export function register(data) {
  return request({
    url: "/user/register",
    method: "post",
    data,
  });
}

export function getUserInfo() {
  return request({
    url: "/user/info",
    method: "get",
  });
}

export function updateUserInfo(data) {
  return request({
    url: "/user/info",
    method: "put",
    data,
  });
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData
  })
}

export function getUserPreferences() {
  return request({
    url: '/user/preferences',
    method: 'get'
  })
}

export function updateUserPreferences(data) {
  return request({
    url: '/user/preferences',
    method: 'put',
    data
  })
}

export function changePassword(data) {
  return request({
    url: "/user/password",
    method: "put",
    data,
  });
}

// 同时导出对象形式以兼容组件导入
export const userApi = {
  login: login,
  register: register,
  info: getUserInfo,
  updateInfo: updateUserInfo,
  changePassword: changePassword,
  uploadAvatar,
  getUserPreferences,
  updateUserPreferences
};

export function checkUsername(username) {
  return request({
    url: "/user/check-username?username=" + username,
    method: "get",
  });
}

export function checkEmail(email) {
  return request({
    url: "/user/check-email?email=" + email,
    method: "get",
  });
}
