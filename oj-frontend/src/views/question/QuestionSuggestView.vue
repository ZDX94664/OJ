<template>
  <div id="questionSuggestView">
    <a-row :gutter="[16, 16]">
      <a-col :span="24">
        <a-card title="每日推荐">
          <template #extra>
            <a-button type="primary" @click="refreshRecommendations">
              <template #icon>
                <icon-refresh />
              </template>
              刷新推荐
            </a-button>
          </template>
          <a-table
            :columns="columns"
            :data="recommendList"
            :pagination="false"
          >
            <template #tags="{ record }">
              <a-space wrap>
                <a-tag
                  v-for="(tag, index) of record.tags"
                  :key="index"
                  :style="{
                    backgroundColor: '#e8f7e9',
                    color: '#18a058',
                    border: 'none',
                    padding: '2px 8px',
                    marginRight: '8px'
                  }"
                >
                  {{ tag }}
                </a-tag>
              </a-space>
            </template>
            <template #acceptedRate="{ record }">
              {{
                `${record.submitNum ? ((record.acceptedNum / record.submitNum) * 100).toFixed(1) : "0"}% (${
                  record.acceptedNum
                }/${record.submitNum})`
              }}
            </template>
            <template #optional="{ record }">
              <a-space>
                <a-button type="primary" @click="toQuestionPage(record)">
                  开始做题
                </a-button>
              </a-space>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { QuestionVO, QuestionControllerService } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRouter } from "vue-router";
import { IconRefresh } from '@arco-design/web-vue/es/icon';

// 推荐题目列表
const recommendList = ref<QuestionVO[]>([]);

const columns = [
  {
    title: "题目名称",
    dataIndex: "title",
  },
  {
    title: "标签",
    slotName: "tags",
  },
  {
    title: "通过率",
    slotName: "acceptedRate",
  },
  {
    title: "操作",
    slotName: "optional",
    width: 100,
  },
];

// 加载推荐题目
const loadData = async () => {
  try {
    const res = await QuestionControllerService.getRecommendQuestionsUsingGet();
    if (res.code === 0) {
      // 处理返回的数据，将标签字符串转换为数组
      const processedData = Array.isArray(res.data) ? res.data.map(item => ({
        ...item,
        tags: item.tags.replace(/[[\]"]/g, '').split(',').map(tag => tag.trim()).filter(tag => tag)
      })) : [];
      recommendList.value = processedData;
    } else {
      message.error("获取推荐题目失败：" + res.message);
    }
  } catch (error) {
    message.error("获取推荐题目失败：" + error.message);
  }
};

// 刷新推荐
const refreshRecommendations = () => {
  loadData();
};

const router = useRouter();

// 跳转到做题页面
const toQuestionPage = (question: QuestionVO) => {
  router.push({
    path: `/view/question/${question.id}`,
  });
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
#questionSuggestView {
  max-width: 1280px;
  margin: 0 auto;
}
</style>
