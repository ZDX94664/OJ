<template>
  <a-row id="globalHeader" align="center" :wrap="false">
    <a-col flex="auto">
      <a-menu
        mode="horizontal"
        :selected-keys="selectedKeys"
        @menu-item-click="doMenuClick"
      >
        <a-menu-item
          key="0"
          :style="{ padding: 0, marginRight: '38px' }"
          disabled
        >
          <div class="title-bar">
            <div class="title">在线判题</div>
          </div>
        </a-menu-item>
        <a-menu-item v-for="item in visibleRoutes" :key="item.path">
          {{ item.name }}
        </a-menu-item>
      </a-menu>
    </a-col>
    <a-col flex="100px">
      <div v-if="store.state.user.loginUser?.userRole && store.state.user.loginUser.userRole !== ACCESS_ENUM.NOT_LOGIN" class="user-info">
        <a-dropdown @select="handleSelect">
          <div class="user-dropdown">
            <img src="@/assets/默认头像.png" alt="avatar" class="user-avatar" />
            <!-- <a-button type="text">{{ store.state.user.loginUser.userName }}</a-button> -->
          </div>
          <template #content>
            <a-doption value="logout">退出登录</a-doption>
          </template>
        </a-dropdown>
      </div>
      <div v-else class="login-button">
        <a-button type="primary" @click="toLogin">登录/注册</a-button>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { routes } from "../router/routes";
import { useRoute, useRouter } from "vue-router";
import { computed, ref } from "vue";
import { useStore } from "vuex";
import checkAccess from "@/access/checkAccess";
import ACCESS_ENUM from "@/access/accessEnum";
import { Message } from "@arco-design/web-vue";

const router = useRouter();
const store = useStore();

// 展示在菜单的路由数组
const visibleRoutes = computed(() => {
  return routes.filter((item, index) => {
    if (item.meta?.hideInMenu) {
      return false;
    }
    // 根据权限过滤菜单
    if (
      !checkAccess(store.state.user.loginUser, item?.meta?.access as string)
    ) {
      return false;
    }
    return true;
  });
});

// 默认主页
const selectedKeys = ref(["/"]);

// 路由跳转后，更新选中的菜单项
router.afterEach((to, from, failure) => {
  selectedKeys.value = [to.path];
});

const doMenuClick = (key: string) => {
  router.push({
    path: key,
  });
};

const toLogin = () => {
  router.push('/user/login');
};

const handleSelect = (value: string) => {
  if (value === 'logout') {
    store.dispatch("user/logout");
    Message.success("退出登录成功");
    router.push("/user/login");
  }
};
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: #444;
  margin-left: 16px;
}

.logo {
  height: 48px;
}

.user-info {
  padding: 0 16px;
  color: #666;
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
}

.login-button {
  padding: 0 16px;
}
</style>
