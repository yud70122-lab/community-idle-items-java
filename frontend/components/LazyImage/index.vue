<template>
  <image
    :src="loaded ? src : placeholder"
    :lazy-load="true"
    :mode="mode"
    :show-menu-by-longpress="showMenuByLongpress"
    :style="imageStyle"
    class="lazy-image"
    @load="onLoad"
    @error="onError"
  />
</template>

<script>
export default {
  name: 'LazyImage',
  props: {
    src: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '/static/images/placeholder.png'
    },
    mode: {
      type: String,
      default: 'aspectFill'
    },
    width: {
      type: [String, Number],
      default: '100%'
    },
    height: {
      type: [String, Number],
      default: '200rpx'
    },
    borderRadius: {
      type: [String, Number],
      default: '8rpx'
    },
    showMenuByLongpress: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loaded: false,
      error: false
    }
  },
  computed: {
    imageStyle() {
      return {
        width: typeof this.width === 'number' ? `${this.width}rpx` : this.width,
        height: typeof this.height === 'number' ? `${this.height}rpx` : this.height,
        borderRadius: typeof this.borderRadius === 'number' ? `${this.borderRadius}rpx` : this.borderRadius,
        backgroundColor: '#f5f5f5'
      }
    }
  },
  methods: {
    onLoad(e) {
      this.loaded = true
      this.$emit('load', e)
    },
    onError(e) {
      this.error = true
      this.loaded = false
      this.$emit('error', e)
    }
  }
}
</script>

<style lang="scss" scoped>
.lazy-image {
  display: block;
  transition: opacity 0.3s ease;
  opacity: 0.8;

  &[src*="placeholder"] {
    opacity: 0.5;
  }
}
</style>
