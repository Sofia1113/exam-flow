<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

/**
 * 登录页:账号密码登录(JWT 双令牌)。遵循 DESIGN.md 政企风格。
 */
const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    router.push('/')
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-banner">
      <h1>考试管理系统</h1>
      <p>考试流程全生命周期管理 · 报名 / 组卷 / 考试 / 阅卷 / 成绩</p>
    </div>
    <div class="ef-container login-container">
      <div class="ef-card login-card">
        <h2 class="login-title">管理员登录</h2>
        <form @submit.prevent="submit">
          <div style="margin-bottom: 16px">
            <label class="ef-form-label required">账号</label>
            <input v-model="form.username" class="ef-input" placeholder="请输入登录账号" required />
          </div>
          <div style="margin-bottom: 20px">
            <label class="ef-form-label required">密码</label>
            <input v-model="form.password" type="password" class="ef-input" placeholder="请输入密码" required />
          </div>
          <p v-if="error" class="login-error">{{ error }}</p>
          <button type="submit" class="ef-btn ef-btn-primary" style="width: 100%" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--ef-surface-1);
}

.login-banner {
  background: var(--ef-primary);
  color: var(--ef-inverse-ink);
  padding: 64px 0;
  text-align: center;
}

.login-banner h1 {
  font-family: var(--ef-font-heiti);
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.login-banner p {
  font-size: 15px;
}

.login-container {
  display: flex;
  justify-content: center;
  padding-top: 48px;
}

.login-card {
  width: 420px;
  border-top: 3px solid var(--ef-primary);
}

.login-title {
  font-family: var(--ef-font-heiti);
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
  padding-left: 12px;
  border-left: 4px solid var(--ef-primary);
}

.login-error {
  color: var(--ef-error);
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
