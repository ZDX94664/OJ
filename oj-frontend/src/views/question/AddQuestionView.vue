<template>
  <!-- 加载指示器 -->
  <div id="loading-indicator" style="display:none;">
    <div class="loader"></div>
  </div>
  <div id="addQuestionView">
    <h2>创建题目</h2>
    <a-form :model="form" label-align="left">
      <a-form-item field="title" label="标题">
        <a-input v-model="form.title" placeholder="请输入标题" />
        <a-button type="primary" shape="round" @click="onCreate">一键生成</a-button>
      </a-form-item>
      <a-form-item field="tags" label="标签">
        <a-input-tag v-model="form.tags" placeholder="请选择标签" allow-clear />
      </a-form-item>
      <a-form-item field="content" label="题目内容">
        <MdEditor :value="form.content" :handle-change="onContentChange" />
      </a-form-item>
      <a-form-item field="answer" label="答案">
        <MdEditor :value="form.answer" :handle-change="onAnswerChange" />
      </a-form-item>
      <a-form-item label="判题配置" :content-flex="false" :merge-props="false">
        <a-space direction="vertical" style="min-width: 480px">
          <a-form-item field="judgeConfig.timeLimit" label="时间限制">
            <a-input-number v-model="form.judgeConfig.timeLimit" placeholder="请输入时间限制" mode="button" min="0"
              size="large" />
          </a-form-item>
          <a-form-item field="judgeConfig.memoryLimit" label="内存限制">
            <a-input-number v-model="form.judgeConfig.memoryLimit" placeholder="请输入内存限制" mode="button" min="0"
              size="large" />
          </a-form-item>
          <a-form-item field="judgeConfig.stackLimit" label="堆栈限制">
            <a-input-number v-model="form.judgeConfig.stackLimit" placeholder="请输入堆栈限制" mode="button" min="0"
              size="large" />
          </a-form-item>
        </a-space>
      </a-form-item>
      <a-form-item label="测试用例配置" :content-flex="false" :merge-props="false">
        <a-form-item v-for="(judgeCaseItem, index) of form.judgeCase" :key="index" no-style>
          <a-space direction="vertical" style="min-width: 640px">
            <a-form-item :field="`form.judgeCase[${index}].input`" :label="`输入用例-${index}`" :key="index">
              <a-input v-model="judgeCaseItem.input" placeholder="请输入测试输入用例" />
            </a-form-item>
            <a-form-item :field="`form.judgeCase[${index}].output`" :label="`输出用例-${index}`" :key="index">
              <a-input v-model="judgeCaseItem.output" placeholder="请输入测试输出用例" />
            </a-form-item>
            <a-button status="danger" @click="handleDelete(index)">
              删除
            </a-button>
          </a-space>
        </a-form-item>
        <div style="margin-top: 32px">
          <a-button @click="handleAdd" type="outline" status="success">新增测试用例
          </a-button>
        </div>
      </a-form-item>
      <div style="margin-top: 16px" />
      <a-form-item>
        <a-button type="primary" style="min-width: 200px" @click="doSubmit">提交
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import MdEditor from "@/components/MdEditor.vue";
import { QuestionControllerService } from "../../../generated";
import message from "@arco-design/web-vue/es/message";
import { useRoute } from "vue-router";

const route = useRoute();
// 如果页面地址包含 update，视为更新页面
const updatePage = route.path.includes("update");

let form = ref({
  title: "",
  tags: [],
  answer: "",
  content: "",
  judgeConfig: {
    memoryLimit: 1000,
    stackLimit: 1000,
    timeLimit: 1000,
  },
  judgeCase: [
    {
      input: "",
      output: "",
    },
  ],
});

/**
 * 根据题目 id 获取老的数据
 */
const loadData = async () => {
  const id = route.query.id;
  if (!id) {
    return;
  }
  const res = await QuestionControllerService.getQuestionByIdUsingGet(
    id as any
  );
  if (res.code === 0) {
    form.value = res.data as any;
    // json 转 js 对象
    if (!form.value.judgeCase) {
      form.value.judgeCase = [
        {
          input: "",
          output: "",
        },
      ];
    } else {
      form.value.judgeCase = JSON.parse(form.value.judgeCase as any);
    }
    if (!form.value.judgeConfig) {
      form.value.judgeConfig = {
        memoryLimit: 1000,
        stackLimit: 1000,
        timeLimit: 1000,
      };
    } else {
      form.value.judgeConfig = JSON.parse(form.value.judgeConfig as any);
    }
    if (!form.value.tags) {
      form.value.tags = [];
    } else {
      form.value.tags = JSON.parse(form.value.tags as any);
    }
  } else {
    message.error("加载失败，" + res.message);
  }
};

onMounted(() => {
  loadData();
});

const onCreate = async () => {
  // 显示加载指示器
  document.getElementById('loading-indicator').style.display = 'block';

  try {
    const response = await fetch('http://localhost:8101/api/question/getQuestionByAi', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    if (!response.ok) {
      throw new Error('网络请求失败');
    }
    const data = await response.json();
    const assistantResponse = data.choices[0].message.content;
    // form.value.title = assistantResponse.title;
    // console.log("title:"+assistantResponse.title);
    let dataObject;
    try {
      // 注意：这里的字符串需要是合法的JSON格式。如果你直接从assistantResponse变量解析，需要去掉外部的双引号以及对内部双引号的转义。
      // 下面的代码假设已经进行了这样的处理
      dataObject = JSON.parse(assistantResponse.replace(/\\n/g, "\\n").replace(/\\'/g, "\\'").replace(/\\"/g, '\\"').replace(/\\\\/g, "\\\\"));
    } catch (e) {
      console.error("Error parsing JSON string:", e);
    }
    form.value.title = dataObject.title;
    form.value.content = dataObject.content;
    form.value.answer = dataObject.answer;
    form.value.tags = dataObject.tags;
    form.value.judgeCase = dataObject.judgeCase;
    form.value.judgeConfig = dataObject.judgeConfig;
    console.log(dataObject.title);
    console.log(assistantResponse);
  } catch (error) {
    if (error instanceof Error) {
      console.error('Error:', error.message);
    } else if (error instanceof TypeError) {
      console.error('TypeError:', error.message);
    } else {
      console.error('发生未知错误:', error);
    }
  } finally {
    // 隐藏加载指示器
    document.getElementById('loading-indicator').style.display = 'none';
  }
};



const doSubmit = async () => {
  console.log(form.value);
  // 区分更新还是创建
  if (updatePage) {
    const res = await QuestionControllerService.updateQuestionUsingPost(
      form.value
    );
    if (res.code === 0) {
      message.success("更新成功");
    } else {
      message.error("更新失败，" + res.message);
    }
  } else {
    const res = await QuestionControllerService.addQuestionUsingPost(
      form.value
    );
    if (res.code === 0) {
      message.success("创建成功");
    } else {
      message.error("创建失败，" + res.message);
    }
  }
};

/**
 * 新增判题用例
 */
const handleAdd = () => {
  form.value.judgeCase.push({
    input: "",
    output: "",
  });
};

/**
 * 删除判题用例
 */
const handleDelete = (index: number) => {
  form.value.judgeCase.splice(index, 1);
};

const onContentChange = (value: string) => {
  form.value.content = value;
};

const onAnswerChange = (value: string) => {
  form.value.answer = value;
};
</script>

<style scoped>
#addQuestionView {
}

#loading-indicator {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.loader {
  border: 8px solid #f3f3f3;
  border-top: 8px solid #3498db;
  border-radius: 50%;
  width: 60px;
  height: 60px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}
</style>
