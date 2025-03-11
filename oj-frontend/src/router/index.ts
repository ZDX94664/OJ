import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import { routes } from "@/router/routes";
import store from "@/store";
import ACCESS_ENUM from "@/access/accessEnum";

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

/**
 * 全局路由守卫，在进入页面前检查权限
 */
router.beforeEach(async (to, from, next) => {
  // 如果未登录，自动获取登录信息
  const loginUser = store.state.user.loginUser;
  if (!loginUser || !loginUser.userRole) {
    await store.dispatch("user/getLoginUser");
  }
  const loginUserRole = store.state.user.loginUser.userRole;

  // 如果是需要登录的页面，但未登录，则跳转到登录页面
  if (to.meta?.access === ACCESS_ENUM.USER && loginUserRole === ACCESS_ENUM.NOT_LOGIN) {
    next(`/user/login?redirect=${to.fullPath}`);
    return;
  }

  // 如果是需要管理员权限的页面，但用户权限不足，则跳转到无权限页面
  if (to.meta?.access === ACCESS_ENUM.ADMIN && loginUserRole !== ACCESS_ENUM.ADMIN) {
    next("/noAuth");
    return;
  }

  next();
});

export default router;
