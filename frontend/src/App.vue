<template>
  <div id="template_app">
    <HelloWorld msg="欢迎使用 HelloWorld 组件！" />
    <h2>姓名提交演示</h2>
    <form @submit.prevent="submit">
      <label>姓名：<input v-model="name" required /></label>
      <button type="submit">提交</button>
    </form>
    <div v-if="msg" style="margin-top:10px;color:green;">{{ msg }}</div>
    <h3 style="margin-top:30px;">已提交姓名列表：</h3>
    <ul>
      <li v-for="item in names" :key="item">{{ item }}</li>
    </ul>
    <h2 style="margin-top:40px;">DeepSeek API 测试</h2>
    <DeepseekTest />
  </div>
</template>

<script>

import HelloWorld from './components/HelloWorld.vue';
import DeepseekTest from './components/DeepseekTest.vue';

export default {
  components: {
    HelloWorld,
    DeepseekTest
  },
  data() {
    return {
      name: '',
      msg: '',
      names: []
    }
  },
  mounted() {
    this.fetchNames();
  },
  methods: {
    async submit() {
      const res = await fetch('/api/names', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: this.name })
      });
      const data = await res.json();
      this.msg = '提交成功！';
      this.names = data;
      this.name = '';
    },
    async fetchNames() {
      const res = await fetch('/api/names');
      this.names = await res.json();
    }
  }
}
</script>

<style scoped>
body { font-family: Arial, sans-serif; margin: 40px; }
#app { max-width: 400px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 8px; }
button { margin-top: 10px; }
</style>
