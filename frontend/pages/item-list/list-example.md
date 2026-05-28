# 物品列表页 - 图片懒加载使用示例

## 一、微信小程序原生方式（推荐）

### WXML 模板

```html
<view class="item-list">
  <view class="item-card" wx:for="{{items}}" wx:key="id" bindtap="goToDetail" data-id="{{item.id}}">
    <image
      class="item-cover"
      src="{{item.coverImage}}"
      lazy-load="true"
      mode="aspectFill"
      show-menu-by-longpress="{{false}}"
    />
    <view class="item-info">
      <text class="item-title">{{item.title}}</text>
      <text class="item-price">¥{{item.price}}</text>
    </view>
  </view>
</view>
```

### WXSS 样式

```css
.item-list {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx;
  gap: 20rpx;
}

.item-card {
  width: calc(50% - 10rpx);
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.item-cover {
  width: 100%;
  height: 340rpx;
  display: block;
  background: #f5f5f5;
}

.item-info {
  padding: 20rpx;
}

.item-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b00;
  margin-top: 12rpx;
  display: block;
}
```

### JS 配置

```javascript
Page({
  data: {
    items: []
  },

  onLoad() {
    this.loadItems()
  },

  async loadItems() {
    const res = await wx.request({
      url: 'https://your-api.com/api/item/list',
      data: { pageNum: 1, pageSize: 20 }
    })
    this.setData({
      items: res.data.data.records
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/item-detail/detail?id=${id}`
    })
  }
})
```

---

## 二、uni-app Vue 方式

### 使用 LazyImage 组件

```vue
<template>
  <view class="item-list">
    <view
      class="item-card"
      v-for="item in items"
      :key="item.id"
      @click="goToDetail(item.id)"
    >
      <LazyImage
        :src="item.coverImage"
        :width="340"
        :height="340"
        :border-radius="16"
        mode="aspectFill"
        @load="onImageLoad(item.id)"
        @error="onImageError(item.id)"
      />
      <view class="item-info">
        <text class="item-title">{{ item.title }}</text>
        <text class="item-price">¥{{ item.price }}</text>
        <view class="item-meta">
          <text class="view-count">👁 {{ item.viewCount }}</text>
          <text class="favorite-count">❤️ {{ item.favoriteCount }}</text>
        </view>
      </view>
    </view>

    <view class="load-more" v-if="hasMore">
      <uni-load-more :status="loadingStatus" />
    </view>
  </view>
</template>

<script>
import LazyImage from '@/components/LazyImage/index.vue'

export default {
  name: 'ItemList',
  components: { LazyImage },
  data() {
    return {
      items: [],
      pageNum: 1,
      pageSize: 20,
      hasMore: true,
      loadingStatus: 'more'
    }
  },
  onLoad() {
    this.loadItems()
  },
  onReachBottom() {
    if (this.hasMore) {
      this.pageNum++
      this.loadItems()
    }
  },
  methods: {
    async loadItems() {
      this.loadingStatus = 'loading'
      try {
        const res = await uni.request({
          url: 'https://your-api.com/api/item/list',
          data: {
            pageNum: this.pageNum,
            pageSize: this.pageSize
          }
        })
        const data = res[1].data.data
        if (this.pageNum === 1) {
          this.items = data.records
        } else {
          this.items = [...this.items, ...data.records]
        }
        this.hasMore = this.items.length < data.total
        this.loadingStatus = this.hasMore ? 'more' : 'noMore'
      } catch (e) {
        this.loadingStatus = 'more'
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    goToDetail(id) {
      uni.navigateTo({
        url: `/pages/item-detail/detail?id=${id}`
      })
    },
    onImageLoad(id) {
      console.log('图片加载完成:', id)
    },
    onImageError(id) {
      console.error('图片加载失败:', id)
    }
  }
}
</script>

<style lang="scss" scoped>
.item-list {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx;
  gap: 20rpx;
}

.item-card {
  width: calc(50% - 10rpx);
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.item-info {
  padding: 20rpx;
}

.item-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b00;
  margin-top: 12rpx;
  display: block;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #999;
}

.load-more {
  width: 100%;
  padding: 30rpx 0;
  text-align: center;
}
</style>
```

---

## 三、lazy-load 属性说明

### 微信小程序 image 组件 lazy-load 特性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| lazy-load | Boolean | false | **图片懒加载**，在即将进入一定范围（上下三屏）时才开始加载 |

### 懒加载优势

1. **提升首屏加载速度**：只加载可视区域内的图片
2. **减少流量消耗**：用户未浏览的图片不会加载
3. **降低服务器压力**：减少并发请求
4. **提升用户体验**：页面加载更流畅

### 注意事项

1. **scroll-view 中使用**：需要自己实现监听滚动位置来判断是否加载
2. **page 页面直接使用**：`lazy-load="true"` 自动生效
3. **占位图建议**：使用本地占位图，减少网络请求
4. **图片尺寸**：建议设置固定宽高，避免页面跳动

---

## 四、全局配置（pages.json）

```json
{
  "pages": [
    {
      "path": "pages/item-list/list",
      "style": {
        "navigationBarTitleText": "物品列表",
        "enablePullDownRefresh": true,
        "onReachBottomDistance": 50
      }
    }
  ]
}
```

---

## 五、性能优化建议

### 1. 图片压缩
- 后端返回合适尺寸的图片（如封面图：300x300）
- 使用 WebP 格式（微信小程序支持）
- 配置 CDN 自动压缩

### 2. 列表虚拟化
- 对于长列表（>100条），使用 `uni-list` 或 `recycle-view`
- 避免一次性加载过多数据

### 3. 缓存策略
- 已加载的图片利用微信小程序自动缓存
- 列表数据实现本地缓存
