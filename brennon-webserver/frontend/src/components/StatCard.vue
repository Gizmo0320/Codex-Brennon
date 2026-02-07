<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  color?: string
}>()
</script>

<template>
  <div class="stat-card card">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value" :style="{ color: color || 'var(--text-primary)' }">{{ value }}</div>
  </div>
</template>

<style scoped>
.stat-card { text-align: center; }
.stat-label { color: var(--text-secondary); font-size: 13px; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }
</style>
