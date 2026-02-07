<script setup lang="ts">
import { ref } from 'vue'
import api from '../api'

const emit = defineEmits(['select'])
const query = ref('')
const results = ref<any[]>([])
const loading = ref(false)

async function search() {
  if (query.value.length < 2) {
    results.value = []
    return
  }
  loading.value = true
  try {
    const res = await api.get(`/players/name/${query.value}`)
    results.value = [res.data]
    emit('select', res.data)
  } catch {
    results.value = []
  }
  loading.value = false
}
</script>

<template>
  <div class="player-search">
    <div style="display: flex; gap: 8px;">
      <input v-model="query" placeholder="Player name..." @keyup.enter="search" />
      <button @click="search" :disabled="loading">Search</button>
    </div>
  </div>
</template>
