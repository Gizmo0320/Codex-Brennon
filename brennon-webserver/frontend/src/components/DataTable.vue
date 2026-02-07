<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  columns: { key: string; label: string }[]
  data: any[]
  searchKey?: string
}>()

const emit = defineEmits(['row-click'])
const search = ref('')

const filtered = computed(() => {
  if (!search.value || !props.searchKey) return props.data
  const q = search.value.toLowerCase()
  return props.data.filter(row => {
    const val = String(row[props.searchKey!] || '').toLowerCase()
    return val.includes(q)
  })
})
</script>

<template>
  <div>
    <input
      v-if="searchKey"
      v-model="search"
      :placeholder="`Search by ${searchKey}...`"
      style="margin-bottom: 12px"
    />
    <table>
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key">{{ col.label }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in filtered" :key="i" @click="emit('row-click', row)" style="cursor: pointer;">
          <td v-for="col in columns" :key="col.key">
            <slot :name="col.key" :row="row" :value="row[col.key]">
              {{ row[col.key] }}
            </slot>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td :colspan="columns.length" style="text-align: center; color: var(--text-secondary);">
            No data found
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
