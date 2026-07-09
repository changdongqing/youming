<template>
  <div class="layout-logo" v-if="setShowLogo" @click="onThemeConfigChange">
    <img v-if="logoSrc" :src="logoSrc" class="layout-logo-img" @error="onLogoError" alt="logo" />
    <span :style="{color:setFontColor}">{{ siteConfig.title }}</span>
  </div>
  <div class="layout-logo-size" v-else @click="onThemeConfigChange">
    <img :src="logoSrc || logoMini" class="layout-logo-size-img"/>
  </div>
</template>

<script setup lang="ts" name="layoutLogo">
import {useThemeConfig} from '/@/stores/themeConfig';
import {useSiteConfig} from '/@/stores/siteConfig';
import { useWindowSize } from '@vueuse/core';
import { baseURL } from '/@/utils/globalProperties';
import logoMini from '/@/assets/logo-mini.svg';

const {themeConfig} = storeToRefs(useThemeConfig());
const {siteConfig} = storeToRefs(useSiteConfig());
const { width: windowWidth } = useWindowSize();

// logo 优先取站点配置上传图, 失败回退到标题/内置 SVG; 相对路径拼 baseURL(/api) 走 dev 代理到后端免鉴权取图端点
const logoError = ref(false);
const logoSrc = computed(() => {
  const logo = siteConfig.value.logo;
  if (!logo || logoError.value) return '';
  return logo.includes('http') ? logo : baseURL + logo;
});
const onLogoError = () => { logoError.value = true; };

const setShowLogo = computed(() => {
  const {isCollapse, layout} = themeConfig.value;
  return !isCollapse || layout === 'classic' || windowWidth.value < 1000;
});

const setFontColor = computed(() => {
  const {layout} = themeConfig.value;
  return layout === 'classic' || layout === 'transverse' ? `var(--next-bg-topBarColor)` : 'var(--el-color-primary)';
});

const onThemeConfigChange = () => {
  if (themeConfig.value.layout === 'transverse') return;
  themeConfig.value.isCollapse = !themeConfig.value.isCollapse;
};
</script>

<style scoped lang="scss">
.layout-logo {
  width: 220px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  box-shadow: rgb(0 21 41 / 2%) 0px 1px 4px;
  font-size: 16px;
  cursor: pointer;
  animation: logoAnimation 0.3s ease-in-out;

  span {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: inline-block;
    font-size: 21.5px;
    font-weight: 700;
  }

  &-img {
    flex-shrink: 0;
    max-height: 32px;
    max-width: 40px;
    object-fit: contain;
  }

  &:hover {
    span {
      color: var(--color-primary-light-2);
    }
  }
}

.layout-logo-size {
  width: 100%;
  height: 50px;
  display: flex;
  cursor: pointer;
  animation: logoAnimation 0.3s ease-in-out;

  &-img {
    width: 20px;
    margin: auto;
  }

  &:hover {
    img {
      animation: logoAnimation 0.3s ease-in-out;
    }
  }
}
</style>
