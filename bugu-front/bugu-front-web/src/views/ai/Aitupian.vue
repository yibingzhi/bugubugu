<template>
  <el-card class="image-generator">
    <h2>图片生成工具</h2>
    <el-form label-width="100px">
      <el-form-item label="文本描述">
        <el-input v-model="prompt" placeholder="请输入图片的文本描述" type="textarea" :rows="3"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="generateImage" :disabled="isGenerating || !prompt">
          {{ isGenerating ? '生成中...' : '生成图片' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 生成状态 -->
    <div v-if="isGenerating" class="status">
      <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''"></el-progress>
      <p>正在生成图片，请稍候...</p>
    </div>

    <!-- 图片展示和保存按钮 -->
    <div v-if="imageUrl" class="image-container">
      <img :src="imageUrl" alt="Generated Image" style="max-width: 100%;" />
      <el-button type="success" @click="saveImage" style="margin-top: 10px;">保存到本地</el-button>
    </div>

    <!-- 错误信息 -->
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" class="error"></el-alert>
  </el-card>
</template>

<script setup>
import {ref} from 'vue';
import axios from 'axios';
import {ElMessage} from 'element-plus';

// 响应式数据
const apiKey = ref('9e177b6c123f461f8541ff2e479a587a.GEIhgRLbIrY1Mi7l'); // 请替换为您的API Key
const prompt = ref('');
const imageUrl = ref('');
const isGenerating = ref(false);
const errorMessage = ref('');
const progress = ref(0);

// 发起图片生成请求
const generateImage = async () => {
  isGenerating.value = true;
  errorMessage.value = '';
  imageUrl.value = '';
  progress.value = 0;

  try {
    const response = await axios.post(
        'https://open.bigmodel.cn/api/paas/v4/images/generations',
        {
          model: 'cogview-4-250304', // 使用指定的CogView-4模型
          prompt: prompt.value,
          size: '1024x1024', // 默认图片尺寸
          user_id: 'user123' // 可选的用户ID
        },
        {
          headers: {
            'Authorization': `Bearer ${apiKey.value}`,
            'Content-Type': 'application/json'
          }
        }
    );

    if (response.data && response.data.data && response.data.data[0] && response.data.data[0].url) {
      imageUrl.value = response.data.data[0].url;
      progress.value = 100;
    } else {
      errorMessage.value = '生成图片失败：未返回图片URL';
    }
  } catch (error) {
    errorMessage.value = '生成图片失败：' + (error.response?.data?.message || error.message);
  } finally {
    isGenerating.value = false;
  }
};

// 保存图片到本地
const saveImage = async () => {
  if (!imageUrl.value) {
    ElMessage.error('请先生成图片');
    return;
  }

  try {
    const response = await fetch(imageUrl.value);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'generated_image.png'; // 设置下载文件名
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    ElMessage.success('图片已保存到本地');
  } catch (error) {
    console.error('Error saving image:', error);
    ElMessage.error('保存图片失败：' + error.message);
  }
};
</script>

<style scoped>
.image-generator {
  max-width: 800px;
  margin: 20px auto;
}

.status {
  margin-top: 20px;
}

.image-container {
  margin-top: 20px;
}

.error {
  margin-top: 20px;
}
</style>
