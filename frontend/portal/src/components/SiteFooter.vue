<script setup lang="ts">
/**
 * 门户页脚:深藏青底,法定备案信息必留(DESIGN.md 页脚规范)。
 */
const year = new Date().getFullYear()
</script>

<template>
  <footer class="ef-footer">
    <div>
      主办单位:×××××× 人事考试中心&nbsp;|&nbsp;技术支持:exam-flow 在线考试系统
    </div>
    <div>
      <a href="#">联系我们</a>&nbsp;|&nbsp;京ICP备XXXXXXXX号&nbsp;|&nbsp;京公网安备 XXXXXXXXXXXX 号
    </div>
    <div>© {{ year }} 国家考试信息网 版权所有</div>
  </footer>
</template>
