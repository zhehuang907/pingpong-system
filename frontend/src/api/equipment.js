import request from "@/utils/request";

export function getEquipmentList(params) {
  return request({
    url: "/equipment/list",
    method: "get",
    params,
  });
}

export function getEquipmentDetail(id, params) {
  return request({
    url: "/equipment/detail/" + id,
    method: "get",
    params,
  });
}

export function addEquipment(data) {
  return request({
    url: "/equipment",
    method: "post",
    data,
  });
}

export function updateEquipment(data) {
  return request({
    url: "/equipment",
    method: "put",
    data,
  });
}

export function deleteEquipment(id) {
  return request({
    url: "/equipment/" + id,
    method: "delete",
  });
}

export function searchEquipment(params) {
  return request({
    url: "/equipment/search",
    method: "get",
    params,
  });
}

// 同时导出对象形式以兼容组件导入
export const equipmentApi = {
  list: getEquipmentList,
  detail: getEquipmentDetail,
  add: addEquipment,
  update: updateEquipment,
  delete: deleteEquipment,
  search: searchEquipment,
};

export function getStatistics() {
  return request({
    url: "/equipment/statistics",
    method: "get",
  });
}
