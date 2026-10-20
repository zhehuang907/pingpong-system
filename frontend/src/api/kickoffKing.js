import request from "@/utils/request";

export function getKickoffKingOverview() {
  return request({
    url: "/kaiqiuwang-ranking/overview",
    method: "get",
  });
}
