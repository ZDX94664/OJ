<template>
  <div class="register-container">
    <h2>欢迎注册</h2>
    <p class="sub-title">在线判题系统</p>
    <a-form
      class="register-form"
      :model="form"
      @submit="handleSubmit"
    >
      <div class="form-content">
        <a-form-item field="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model="form.userAccount" placeholder="请输入账号">
            <template #prefix>
              <icon-user />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item 
          field="userPassword" 
          :rules="[
            { required: true, message: '请输入密码' },
            { minLength: 6, message: '密码不能少于6位' }
          ]"
        >
          <a-input-password
            v-model="form.userPassword"
            placeholder="请输入密码"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item 
          field="checkPassword" 
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validatePassword, message: '两次输入的密码不一致' }
          ]"
        >
          <a-input-password
            v-model="form.checkPassword"
            placeholder="请确认密码"
          >
            <template #prefix>
              <icon-lock />
            </template>
          </a-input-password>
        </a-form-item>
        <div class="button-container">
          <a-button type="primary" html-type="submit" long>
            注册
          </a-button>
        </div>
        <div class="register-options">
          <a-link @click="toLogin">已有账号？立即登录</a-link>
        </div>
      </div>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { UserControllerService } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { IconUser, IconLock } from '@arco-design/web-vue/es/icon';

interface RegisterForm {
  userAccount: string;
  userPassword: string;
  checkPassword: string;
}

const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
} as RegisterForm);

const router = useRouter();

const validatePassword = () => {
  return form.userPassword === form.checkPassword;
};

const handleSubmit = async () => {
  if (form.userPassword !== form.checkPassword) {
    message.error('两次输入的密码不一致');
    return;
  }
  
  const res = await UserControllerService.userRegisterUsingPost({
    userAccount: form.userAccount,
    userPassword: form.userPassword,
    checkPassword: form.checkPassword,
  });
  
  if (res.code === 0) {
    message.success('注册成功');
    await router.push({
      path: '/user/login',
      replace: true,
    });
  } else {
    message.error("注册失败，" + res.message);
  }
};

const toLogin = () => {
  router.push('/user/login');
};
</script>

<style scoped>
.register-container {
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

.register-form {
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

.register-options {
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
