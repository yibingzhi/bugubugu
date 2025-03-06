<template>
  <el-row class="tac">
    <el-col :span="3">
      <h5 class="mb-2">布谷AI</h5>
      <el-menu
          default-active="2"
          class="el-menu-vertical-demo"
          @open="handleOpen"
          @close="handleClose"
          router
      >

        <el-menu-item index="aipage">
          <el-icon><icon-menu /></el-icon>
          <span>Ai文字对话</span>
        </el-menu-item>
        <el-menu-item index="aivedio">
          <el-icon><icon-menu /></el-icon>
          <span>Ai视频生成</span>
        </el-menu-item>
        <el-menu-item index="aitupian">
          <el-icon><icon-menu /></el-icon>
          <span>Ai图片生成</span>
        </el-menu-item>
      </el-menu>
    </el-col>

    <el-col :span="20" >
      <router-view></router-view>
    </el-col>
  </el-row>
</template>

<script lang="ts" setup>
import {
  Document,
  Menu as IconMenu,
  Location,
  Setting,
} from '@element-plus/icons-vue'
const handleOpen = (key: string, keyPath: string[]) => {
  console.log(key, keyPath)
}
const handleClose = (key: string, keyPath: string[]) => {
  console.log(key, keyPath)
}
</script>