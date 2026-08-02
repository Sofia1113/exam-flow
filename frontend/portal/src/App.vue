<script setup lang="ts">
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
</script>

<template>
  <SiteHeader />
  <main class="ef-main">
    <div class="ef-container">
      <router-view />
    </div>
  </main>
  <SiteFooter />
</template>
