<template>
  <div @keyup.enter="handleEnter">
    <u-chat :config="config" style="width: 100%" @submit="submit" @load-more="loadMore">
      <template #header>
        <div style="height: 40px; display: flex; align-items: center;">
          <div>聊天</div>
        </div>
      </template>
    </u-chat>
  </div>

</template>

<script lang="ts" setup>
import {reactive} from 'vue';
import {ChatApi, ChatConfigApi, usePage} from 'undraw-ui';
import axios from 'axios';
import emoji from '../../assets/emoji';
import {useLoginStore} from "../../stores/loginStore";

const userStore = useLoginStore();
console.log(userStore)
const PAGE_SIZE = 4;

const config = reactive<ChatConfigApi>({
  user: {
    id: 1,
    username: '党卓橦',
    avatar: 'https://static.juzicon.com/images/image-231107185110-DFSX.png'
  },
  data: [],
  emoji: emoji
});

const data = [
  {
    id: 1,
    content: '您好，我是小布谷 有什么可以帮助您的吗？',
    uid: 2,
    user: {
      username: '小布谷',
      avatar: 'https://static.juzicon.com/images/image-231107185110-DFSX.png'
    },
    createTime: '2024-05-27 09:01:00'
  }
  // 消息数据
];


function getRandom(min: number, max: number): number {
  return Math.round(Math.random() * (max - min) + min);
}

let n = 0;

async function loadMore(finish: Function) {
  if (n <= Math.ceil(data.length / PAGE_SIZE)) {
    setTimeout(() => {
      finish(usePage(++n, PAGE_SIZE, data));
    }, getRandom(200, 500));
  } else {
    finish([]);
  }
}

let id = 10;

async function submit(val: string, finish: Function) {
  if (!val.trim()) return;

  const userMessage: ChatApi = {
    id: ++id,
    content: val,
    uid: 1,
    user: {
      username: "用户",
      avatar: "https://static.juzicon.com/images/image-231107185110-DFSX.png",
    },
    createTime: new Date().toISOString()
  };

  finish(userMessage);

  // 调用大模型API
  try {
    const response = await axios.post(
        'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', // 替换为实际的API URL
        {
          model: 'qwen-turbo', // 替换为实际的模型ID
          messages: [{role: 'user', content: val}]
        },
        {
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'sk-83868f0404064eba8e4aea77c472a6cb' // 替换为实际的API令牌
          }
        }
    );

    const aiResponseContent = response.data.choices[0].message.content;

    const aiResponse: ChatApi = {
      id: ++id,
      content: aiResponseContent,
      uid: 2,
      user: {
        username: 'AI',
        avatar: 'https://static.juzicon.com/images/image-231107185110-DFSX.png'
      },
      createTime: new Date().toISOString()
    };

    finish(aiResponse);
  } catch (error) {
    console.error('调用大模型API时出错:', error);
  }
}

function handleEnter(event: KeyboardEvent) {
  const inputElement = event.target as HTMLInputElement;
  if (inputElement && inputElement.value.trim()) {
    submit(inputElement.value, (message: ChatApi) => {
      config.data.push(message);
    });
    inputElement.value = ''; // 清空输入框
  }
}
</script>

<style scoped>
/* 在此处添加样式 */
</style>
