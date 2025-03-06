<template>
  <el-card class="video-generator">
    <h2>视频生成工具</h2>
    <el-form label-width="100px">
      <el-form-item label="文本描述">
        <el-input v-model="prompt" placeholder="请输入视频的文本描述" type="textarea" :rows="3"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="generateVideo" :disabled="isGenerating || !prompt">
          {{ isGenerating ? '生成中...' : '生成视频' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 生成状态 -->
    <div v-if="isGenerating" class="status">
      <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''"></el-progress>
      <p>正在生成视频，请稍候...</p>
    </div>

    <!-- 视频展示和保存按钮 -->
    <div v-if="videoUrl" class="video-container">
      <video controls :src="videoUrl" width="100%"></video>
      <el-button type="success" @click="saveVideo" style="margin-top: 10px;">保存到本地</el-button>
    </div>

    <!-- 错误信息 -->
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" class="error"></el-alert>
  </el-card>
</template>

<script setup>
import { ref, onUnmounted } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';

// 响应式数据
const apiKey = ref('9e177b6c123f461f8541ff2e479a587a.GEIhgRLbIrY1Mi7l'); // 请确保此API Key有效
const prompt = ref('');
const taskId = ref('');
const videoUrl = ref('');
const isGenerating = ref(false);
const errorMessage = ref('');
const progress = ref(0);

// 发起视频生成请求
const generateVideo = async () => {
  isGenerating.value = true;
  errorMessage.value = '';
  videoUrl.value = '';
  progress.value = 0;

  try {
    const response = await axios.post(
        'https://open.bigmodel.cn/api/paas/v4/videos/generations',
        {
          model: 'cogvideox-2',  // 默认使用cogvideox-2模型
          prompt: prompt.value,
          quality: 'speed',     // 快速生成
          with_audio: true,     // 生成AI音效
          size: '1920x1080',    // 分辨率
          fps: 30               // 帧率
        },
        {
          headers: {
            'Authorization': `Bearer ${apiKey.value}`,
            'Content-Type': 'application/json'
          }
        }
    );

    taskId.value = response.data.id;
    checkTaskStatus();
  } catch (error) {
    errorMessage.value = '生成视频失败：' + (error.response?.data?.message || error.message);
    isGenerating.value = false;
  }
};

// 轮询检查任务状态
const checkTaskStatus = () => {
  const interval = setInterval(async () => {
    try {
      const response = await axios.get(
          `https://open.bigmodel.cn/api/paas/v4/async-result/${taskId.value}`,
          {
            headers: {
              'Authorization': `Bearer ${apiKey.value}`
            }
          }
      );

      const status = response.data.task_status;
      progress.value = status === 'PROCESSING' ? Math.min(progress.value + 10, 90) : 100;

      if (status === 'SUCCESS') {
        videoUrl.value = response.data.video_result[0].url; // 假设返回字段为video_result
        isGenerating.value = false;
        clearInterval(interval);
      } else if (status === 'FAIL') {
        errorMessage.value = '视频生成失败';
        isGenerating.value = false;
        clearInterval(interval);
      }
    } catch (error) {
      errorMessage.value = '查询任务状态失败：' + (error.response?.data?.message || error.message);
      isGenerating.value = false;
      clearInterval(interval);
    }
  }, 5000); // 每5秒查询一次

  // 组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(interval);
  });
};

// 保存视频到本地
const saveVideo = async () => {
  if (!videoUrl.value) {
    ElMessage.error('请先生成视频');
    return;
  }

  try {
    const response = await fetch(videoUrl.value);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'generated_video.mp4'; // 默认文件名，可根据需要修改
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    ElMessage.success('视频已保存到本地');
  } catch (error) {
    console.error('Error saving video:', error);
    ElMessage.error('保存视频失败：' + error.message);
  }
};
</script>

<style scoped>
.video-generator {
  max-width: 800px;
  margin: 20px auto;

}

.status {
  margin-top: 20px;
}

.video-container {
  margin-top: 20px;
}

.error {
  margin-top: 20px;
}
</style>
