<template>
  <div>
    <input v-model="apiKey" placeholder="请输入 DeepSeek API Key" style="width:300px;" />
    <button @click="callDeepseek">测试 DeepSeek API</button>
    <div v-if="result">{{ result }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const result = ref('')
const apiKey = ref('')

async function callDeepseek() {
  try {
    const payload = {
      messages: [{ role: 'system', content: 'You are a helpful assistant.' }],
      model: 'deepseek-chat',
      apiKey: apiKey.value.trim()
    }
    const res = await fetch('/api/deepseek/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    if (!res.ok) throw new Error('后端请求失败')
    const data = await res.json()
    // 解析 deepseek 返回内容
    let content = ''
    if (data.choices && data.choices[0] && data.choices[0].message && data.choices[0].message.content) {
      content = data.choices[0].message.content
    } else {
      content = JSON.stringify(data)
    }
    result.value = content
  } catch (e) {
    result.value = '请求失败: ' + e.message
  }
}
</script>
