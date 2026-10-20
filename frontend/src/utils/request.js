import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";
import { clearAuthState, getToken } from "@/utils/auth";

// 标记是否正在登录流程中
let isLoggingIn = false;

export function setLoggingIn(value) {
  isLoggingIn = value;
}

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

request.interceptors.request.use(
  (config) => {
    const token = getToken();
    console.log("[Request Interceptor] URL:", config.url);
    console.log(
      "[Request Interceptor] Token from localStorage:",
      token ? "存在 (" + token.length + " chars)" : "不存在",
    );
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log("[Request Interceptor] Authorization header set");
    } else {
      console.warn("[Request Interceptor] No token found!");
    }
    
    // 如果是 FormData，删除默认 Content-Type，让浏览器自动设置
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
      console.log("[Request Interceptor] FormData detected, removed Content-Type header");
    }
    
    return config;
  },
  (error) => Promise.reject(error),
);

request.interceptors.response.use(
  (response) => {
    console.log("[Response Interceptor] URL:", response.config.url);
    console.log("[Response Interceptor] Status:", response.status);
    
    // 如果是 blob 类型（文件下载/导出），直接返回
    if (response.config.responseType === 'blob') {
      console.log("[Response Interceptor] Blob response detected, returning directly");
      return response.data;
    }
    
    const res = response.data;
    console.log("[Response Interceptor] Response code:", res.code);
    if (res.code !== 200) {
      ElMessage.error(res.msg || "请求失败");
      if (res.code === 401 && !isLoggingIn) {
        clearAuthState();
        if (router.currentRoute.value.name !== 'Login') {
          router.push("/login");
        }
      }
      return Promise.reject(new Error(res.msg || "请求失败"));
    }
    return res;
  },
  (error) => {
    console.error("[Response Interceptor] Error:", error.message);
    console.error("[Response Interceptor] Response:", error.response);
    
    // 处理超时错误
    if (error.code === 'ECONNABORTED' && error.message.includes('timeout')) {
      ElMessage.warning("AI 顾问思考时间较长，请稍后再试或简化问题");
      return Promise.reject(error);
    }
    
    if (error.response) {
      console.error("[Response Interceptor] Status:", error.response.status);
      console.error("[Response Interceptor] Data:", error.response.data);
      
      if (error.response.status === 401 && !isLoggingIn) {
        clearAuthState();
        if (router.currentRoute.value.name !== 'Login') {
          router.push("/login");
        }
      }
    }
    ElMessage.error(
      error.response?.data?.msg || error.message || "网络错误，请稍后重试",
    );
    return Promise.reject(error);
  },
);

export default request;
