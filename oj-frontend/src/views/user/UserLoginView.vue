<template>
  <div class="login-container">
    <h2>欢迎登录</h2>
    <p class="sub-title">在线判题系统</p>
    <a-form
      class="login-form"
      :model="form"
      @submit="handleSubmit"
    >
      <div class="form-content">
        <a-form-item field="userAccount">
          <a-input v-model="form.userAccount" placeholder="请输入账号">
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item field="userPassword" tooltip="密码不少于 6 位">
          <a-input-password
            v-model="form.userPassword"
            placeholder="请输入密码"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>
        <div class="button-container">
          <a-button type="primary" html-type="submit" long>
            登录
          </a-button>
        </div>
        <div class="login-options">
          <a-link @click="toRegister">还没有账号？立即注册</a-link>
        </div>
      </div>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { UserControllerService, UserLoginRequest } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useStore } from "vuex";
import { IconUser, IconLock } from '@arco-design/web-vue/es/icon';

const router = useRouter();
const route = useRoute();
const store = useStore();

const form = ref({
  userAccount: "",
  userPassword: "",
} as UserLoginRequest);

const toRegister = () => {
  router.push("/user/register");
};

const handleSubmit = async () => {
  if (!form.value.userAccount || !form.value.userPassword) {
    message.error('请输入账号和密码');
    return;
  }
  
  const res = await UserControllerService.userLoginUsingPost(form.value);
  if (res.code === 0) {
    await store.dispatch("user/getLoginUser");
    message.success("登录成功");
    // 获取重定向地址
    const redirect = route.query.redirect as string;
    router.push(redirect || "/");
  } else {
    message.error("登录失败，" + res.message);
  }
};
</script>

<style scoped>
.login-container {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  text-align: center;
}

h2 {
  color: #1d2129;
  margin-bottom: 8px;
  font-size: 24px;
}

.sub-title {
  color: #86909c;
  margin-bottom: 32px;
}

.login-form {
  width: 100%;
}

.form-content {
  width: 280px;
  margin: 0 auto;
}

.form-content :deep(.arco-form-item) {
  margin-bottom: 24px;
}

.button-container {
  margin-top: 32px;
}

.login-options {
  margin-top: 16px;
}

:deep(.arco-input-wrapper) {
  background-color: #f2f3f5;
  height: 40px;
}

:deep(.arco-btn-primary) {
  height: 40px;
  font-size: 16px;
}

:deep(.arco-link) {
  font-size: 14px;
}
</style>
