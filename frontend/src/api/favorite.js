import request from "@/utils/request";

export function toggleFavorite(equipmentId) {
  return request({
    url: "/favorite/toggle/" + equipmentId,
    method: "post",
  });
}

export function cancelFavorite(equipmentId) {
  return request({
    url: "/favorite/cancel/" + equipmentId,
    method: "delete",
  });
}

export function getFavoriteList() {
  return request({
    url: "/favorite/list",
    method: "get",
  });
}

export function checkFavorite(equipmentId) {
  return request({
    url: "/favorite/check/" + equipmentId,
    method: "get",
  });
}

// 同时导出对象形式以兼容组件导入
export const favoriteApi = {
  toggle: toggleFavorite,
  cancel: cancelFavorite,
  list: getFavoriteList,
  check: checkFavorite,
};
