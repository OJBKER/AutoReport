<template>
	<div class="text-demo">
		<input v-model="inputText" placeholder="请输入内容" />
		<button @click="sendText">发送到后端</button>
		<div v-if="responseText" class="response">后端返回：{{ responseText }}</div>
			<button @click="exportDoc" style="margin-top:16px;">导出数据库内容为 Word</button>
	</div>
</template>

<script setup>
import { ref } from 'vue'
const inputText = ref('')
const responseText = ref('')

async function sendText() {
	if (!inputText.value.trim()) return
	try {
		const res = await fetch('/api/text', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ text: inputText.value })
		})
		if (!res.ok) throw new Error('后端请求失败')
		const data = await res.json()
		responseText.value = data.text || JSON.stringify(data)
	} catch (e) {
		responseText.value = '请求失败: ' + e.message
	}
}

async function exportDoc() {
	try {
		const res = await fetch('/api/text/export', {
			method: 'POST'
		})
		if (!res.ok) throw new Error('导出失败')
		const blob = await res.blob()
		const url = window.URL.createObjectURL(blob)
		const a = document.createElement('a')
		a.href = url
		a.download = 'export.docx'
		document.body.appendChild(a)
		a.click()
		a.remove()
		window.URL.revokeObjectURL(url)
	} catch (e) {
		responseText.value = '导出失败: ' + e.message
	}
}
</script>

<style scoped>
.text-demo {
	max-width: 400px;
	margin: 60px auto;
	padding: 24px;
	background: #fff;
	border-radius: 8px;
	box-shadow: 0 2px 12px #eee;
	display: flex;
	flex-direction: column;
	gap: 12px;
}
input {
	padding: 6px 10px;
	border: 1px solid #ccc;
	border-radius: 4px;
}
button {
	padding: 6px 18px;
	background: #409eff;
	color: #fff;
	border: none;
	border-radius: 4px;
	cursor: pointer;
	transition: background 0.2s;
}
button:hover {
	background: #1976d2;
}
.response {
	color: #1976d2;
	margin-top: 8px;
}
</style>

