# 将之前的代码移动到这个组件中，并做一些调整
<template>
  <div>
    <!-- 悬浮图标 -->
    <div class="floating-icon" :style="{ left: position.x + 'px', top: position.y + 'px' }" @mousedown="startDrag"
      @click="showChat = true">
      <robot-outlined class="icon" />
    </div>

    <a-modal v-model:visible="showChat" :footer="null" width="800px" :mask-closable="true" :destroyOnClose="false"
      class="chat-modal" :closable="true" :maskStyle="{ backgroundColor: 'rgba(0, 0, 0, 0.45)' }"
      :bodyStyle="{ padding: '0', height: '600px' }" @cancel="closeChat" :centered="true">
      <template #title>
        <div class="modal-title">
          <robot-outlined /> 智能助手
          <a-tooltip title="最小化">
            <minus-outlined class="minimize-btn" @click="minimizeChat" />
          </a-tooltip>
        </div>
      </template>
      <div class="chat-body">
        <div class="chat-container" ref="chatContainer">
          <div v-for="(message, index) in messages" :key="index" class="message" :class="message.type">
            <a-avatar :size="36" :icon="message.type === 'user' ? 'user' : h(RobotOutlined)" class="message-avatar"
              :class="message.type" />
            <div class="message-content">
              <!-- 思考过程展示 -->
              <div v-if="message.reasoning" class="reasoning-content" :class="message.type">
                <div class="thinking-label">思考过程：</div>
                {{ message.reasoning }}
              </div>

              <!-- 主要消息内容（支持 Markdown） -->
              <div class="message-text" :class="message.type" v-html="marked.parse(message.content)"></div>

              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>
        </div>
        <div class="input-container">
          <div class="input-wrapper">
            <a-textarea v-model="inputMessage" placeholder="请输入您的问题..." :auto-size="{ minRows: 1, maxRows: 4 }"
              @keydown.enter.exact.prevent="sendMessage" class="chat-input" :bordered="false" :maxLength="2048" />
            <a-button type="primary" @click="sendMessage" :loading="loading" class="send-btn">
              <template #icon><send-outlined /></template>
            </a-button>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onBeforeUnmount, h } from 'vue';
import { RobotOutlined, SendOutlined, MinusOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { marked } from 'marked';


interface ChatMessage {
  content: string;
  type: 'user' | 'assistant';
  time: string;
  reasoning?: string; // 新增思考链字段
}
const showChat = ref(false);
const position = ref({ x: window.innerWidth - 100, y: window.innerHeight / 2 });
const isDragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });

const messages = ref<ChatMessage[]>([
  {
    content: '你好！我是你的智能助手，有什么我可以帮你的吗？',
    type: 'assistant',
    time: new Date().toLocaleTimeString(),
  },
]);

const inputMessage = ref('');
const loading = ref(false);
const chatContainer = ref<HTMLElement | null>(null);
const isInThinkTag = ref(false);
let eventSource: EventSource | null = null;

// 自动滚动逻辑
const scrollToBottom = async () => {
  await nextTick();
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};



const startDrag = (e: MouseEvent) => {
  isDragging.value = true;
  dragOffset.value = {
    x: e.clientX - position.value.x,
    y: e.clientY - position.value.y,
  };
  
  // 添加全局事件监听
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
};

const onDrag = (e: MouseEvent) => {
  if (isDragging.value) {
    const newX = e.clientX - dragOffset.value.x;
    const newY = e.clientY - dragOffset.value.y;
    
    // 确保图标不会超出屏幕边界
    position.value = {
      x: Math.min(Math.max(0, newX), window.innerWidth - 60),
      y: Math.min(Math.max(0, newY), window.innerHeight - 60),
    };
  }
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};



// SSE 数据解析
const parseSSEData = (data: string) => {
  try {
    const parsed = JSON.parse(data);

    // 处理思考标签逻辑
    const content = parsed.choices?.[0]?.delta?.content || '';
    const reasoning = parsed.choices?.[0]?.delta?.reasoning_content || '';

    // 如果有直接返回的 reasoning_content
    if (reasoning) {
      return { content, reasoning };
    }

    // 处理 <think> 标签包裹的情况
    if (content.includes('<think>')) {
      isInThinkTag.value = true;
      const startIndex = content.indexOf('<think>') + '<think>'.length;
      return {
        content: content.substring(0, content.indexOf('<think>')),
        reasoning: content.substring(startIndex)
      };
    }

    if (content.includes('</think>')) {
      isInThinkTag.value = false;
      const endIndex = content.indexOf('</think>');
      return {
        content: content.substring(endIndex + '</think>'.length),
        reasoning: content.substring(0, endIndex)
      };
    }

    // 根据状态决定内容归属
    return {
      content: isInThinkTag.value ? '' : content,
      reasoning: isInThinkTag.value ? content : ''
    };
  } catch (e) {
    console.error('解析JSON失败:', e);
    return { content: '', reasoning: '' };
  }
};



const sendMessage = () => {
  if (!inputMessage.value.trim()) {
    message.warning('请输入内容');
    return;
  }

  const userMessage = inputMessage.value.trim();

  // 添加用户消息
  messages.value.push({
    content: userMessage,
    type: 'user',
    time: new Date().toLocaleTimeString(),
  });

  // 添加初始助手消息
  messages.value.push({
    content: '',
    type: 'assistant',
    time: new Date().toLocaleTimeString(),
    reasoning: ''
  });

  inputMessage.value = '';
  loading.value = true;

  // 关闭已有连接
  if (eventSource) {
    eventSource.close();
  }

  // 创建 SSE 连接
  const url = new URL('http://localhost:8101/api/ai/chat');
  url.searchParams.set('prompt', userMessage);

  eventSource = new EventSource(url.toString());

  eventSource.onmessage = (event) => {
    const { content, reasoning } = parseSSEData(event.data);

    // 更新最后一条助手消息
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg.type === 'assistant') {
      lastMsg.content += content;
      lastMsg.reasoning += reasoning;

      // 触发响应式更新
      messages.value = [...messages.value];
      scrollToBottom();
    }
  };

  eventSource.onerror = (err) => {
    console.error('SSE Error:', err);
    message.error('连接中断');
    eventSource?.close();
    loading.value = false;
  };
};

// 组件卸载时关闭连接
onBeforeUnmount(() => {
  eventSource?.close();
});

const minimizeChat = () => {
  showChat.value = false;
};

const closeChat = () => {
  showChat.value = false;
};

onMounted(() => {
  scrollToBottom();
});

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
});
</script>

<style scoped>
/* 思考过程样式 */
.reasoning-content {
  background: #f8f8f8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  font-size: 0.9em;
  color: #666;
  border-left: 3px solid #1890ff;
}

.reasoning-content.user {
  background: #e6f7ff;
  border-left-color: #096dd9;
}

.thinking-label {
  font-weight: 500;
  color: #1890ff;
  margin-bottom: 4px;
}

/* Markdown 内容样式 */
.message-text :deep() {

  h1,
  h2,
  h3 {
    font-size: 1.2em;
    margin: 0.5em 0;
  }

  code {
    background: rgba(150, 150, 150, 0.1);
    padding: 2px 4px;
    border-radius: 3px;
  }

  pre {
    background: #f8f8f8;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;

    code {
      background: none;
      padding: 0;
    }
  }

  ul,
  ol {
    padding-left: 1.5em;
  }

  li {
    margin: 0.3em 0;
  }
}

/* 保持原有消息样式 */
.message-content {
  max-width: 85%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  line-height: 1.6;
}

.message-text.assistant {
  background-color: #e6f7ff;
}

.message-text.user {
  background-color: #1890ff;
  color: white;
}
.floating-icon {
  position: fixed;
  z-index: 1000;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid white;
}

.floating-icon:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.icon {
  color: white;
  font-size: 24px;
  transition: transform 0.3s ease;
}

.floating-icon:hover .icon {
  transform: scale(1.1);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #1f1f1f;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.minimize-btn {
  margin-left: auto;
  color: #999;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.3s;
}

.minimize-btn:hover {
  color: #1890ff;
}

.chat-modal {
  :deep(.ant-modal-content) {
    border-radius: 8px;
    overflow: hidden;
  }
  
  :deep(.ant-modal-header) {
    padding: 0;
    border-bottom: none;
    margin-bottom: 0;
  }

  :deep(.ant-modal-body) {
    padding: 0;
    height: 600px;
    display: flex;
    flex-direction: column;
  }

  :deep(.ant-modal-close) {
    top: 14px;
  }
}

.chat-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f7f7f7;
  min-height: 0; /* 重要：防止内容溢出 */
}

.input-container {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px;
  z-index: 1;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 8px;
}

.chat-input {
  flex: 1;
  background-color: transparent;
  margin: 0;
}

:deep(.ant-input) {
  background-color: transparent;
  font-size: 14px;
  padding: 8px 12px;
  resize: none;
  border: none;
  box-shadow: none !important;
}

:deep(.ant-input:focus) {
  box-shadow: none !important;
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message {
  display: flex;
  margin-bottom: 24px;
  gap: 12px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-avatar.user {
  background-color: #1890ff;
}

.message-avatar.assistant {
  background-color: #52c41a;
}

.message-content {
  max-width: 70%;
}

.message.user .message-content {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  font-size: 14px;
  line-height: 1.6;
  position: relative;
}

.message-text.assistant {
  background-color: #e6f7ff;
  border-top-left-radius: 4px;
}

.message-text.user {
  background-color: #1890ff;
  color: white;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
</style>
