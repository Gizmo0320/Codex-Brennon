<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { wsClient } from '../ws'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function login() {
  error.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/login', {
      username: username.value,
      password: password.value
    })
    localStorage.setItem('brennon_token', res.data.token)
    wsClient.connect()
    router.push('/admin')
    window.location.reload()
  } catch {
    error.value = 'Invalid username or password'
  }
  loading.value = false
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <h1>Brennon</h1>
      <p>Network Dashboard</p>
      <form @submit.prevent="login">
        <div class="form-group">
          <label>Username</label>
          <input v-model="username" type="text" autofocus />
        </div>
        <div class="form-group">
          <label>Password</label>
          <input v-model="password" type="password" />
        </div>
        <div v-if="error" class="error">{{ error }}</div>
        <button type="submit" :disabled="loading" style="width: 100%; margin-top: 8px;">
          {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </form>
      <div class="switch-link">
        <router-link to="/player-login">Player Portal</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.login-card {
  width: 360px;
  text-align: center;
}
.login-card h1 { font-size: 28px; margin-bottom: 4px; }
.login-card p { color: var(--text-secondary); margin-bottom: 24px; }
.error { color: var(--danger); font-size: 13px; margin-bottom: 8px; }
.switch-link { margin-top: 16px; font-size: 13px; color: var(--text-secondary); }
</style>
