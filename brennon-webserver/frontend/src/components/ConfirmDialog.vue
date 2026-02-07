<script setup lang="ts">
defineProps<{
  show: boolean
  title: string
  message: string
}>()
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <teleport to="body">
    <div v-if="show" class="overlay" @click.self="emit('cancel')">
      <div class="dialog card">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="dialog-actions">
          <button @click="emit('cancel')" style="background: var(--bg-input);">Cancel</button>
          <button class="danger" @click="emit('confirm')">Confirm</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.dialog { min-width: 380px; }
.dialog h3 { margin-bottom: 8px; }
.dialog p { color: var(--text-secondary); margin-bottom: 16px; }
.dialog-actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
