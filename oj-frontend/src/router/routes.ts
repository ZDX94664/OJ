import { RouteRecordRaw } from "vue-router";

import UserLoginView from "@/views/user/UserLoginView.vue";
import UserRegisterView from "@/views/user/UserRegisterView.vue";
import NoAuthView from "@/views/NoAuthView.vue";
import ACCESS_ENUM from "@/access/accessEnum";
import AddQuestionView from "@/views/question/AddQuestionView.vue";
import ManageQuestionView from "@/views/question/ManageQuestionView.vue";
import QuestionsView from "@/views/question/QuestionsView.vue";
import QuestionSuggestView from "@/views/question/QuestionSuggestView.vue";
import ViewQuestionView from "@/views/question/ViewQuestionView.vue";
import UserAnalysisView from "@/views/UserAnalysisView.vue";

export const routes: Array<RouteRecordRaw> = [
  {
    path: "/user/login",
    name: "用户登录",
    component: UserLoginView,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/user/register",
    name: "用户注册",
    component: UserRegisterView,
    meta: {
      hideInMenu: true,
    },
  },
  {
    path: "/",
    name: "首页",
    component: UserAnalysisView,
    meta: {
      access: ACCESS_ENUM.USER,
    },
  },
  {
    path: "/questions",
    name: "浏览题目",
    component: QuestionsView,
  },
  {
    path: "/question_suggest",
    name: "每日推荐",
    component: QuestionSuggestView,
    meta: {
      access: ACCESS_ENUM.USER,
    },
  },
  {
    path: "/view/question/:id",
    name: "查看题目",
    component: ViewQuestionView,
    props: true,
    meta: {
      access: ACCESS_ENUM.USER,
      hideInMenu: true,
    },
  },
  {
    path: "/add/question",
    name: "创建题目",
    component: AddQuestionView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/update/question",
    name: "更新题目",
    component: AddQuestionView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
      hideInMenu: true,
    },
  },
  {
    path: "/manage/question/",
    name: "管理题目",
    component: ManageQuestionView,
    meta: {
      access: ACCESS_ENUM.ADMIN,
    },
  },
  {
    path: "/noAuth",
    name: "无权限",
    component: NoAuthView,
    meta: {
      hideInMenu: true,
    },
  },
  // {
  //   path: "/hide",
  //   name: "隐藏页面",
  //   component: HomeView,
  //   meta: {
  //     hideInMenu: true,
  //   },
  // },
  // {
  //   path: "/admin",
  //   name: "管理员可见",
  //   component: AdminView,
  //   meta: {
  //     access: ACCESS_ENUM.ADMIN,
  //   },
  // },
  // {
  //   path: "/about",
  //   name: "关于我的",
  //   // route level code-splitting
  //   // this generates a separate chunk (about.[hash].js) for this route
  //   // which is lazy-loaded when the route is visited.
  //   component: () =>
  //     import(/* webpackChunkName: "about" */ "../views/AboutView.vue"),
  // },
];
