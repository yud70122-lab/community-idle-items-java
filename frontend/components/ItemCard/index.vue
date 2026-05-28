<template>
  <view class="item-card" @click="handleClick">
    <image class="item-image" :src="item.coverImage" mode="aspectFill"></image>
    <view class="item-info">
      <rich-text
        class="item-title"
        :nodes="richTitle"
        selectable="false"
      ></rich-text>
      <view class="item-price">
        <text class="price-symbol">¥</text>
        <text class="price-value">{{ item.price }}</text>
        <text v-if="item.originalPrice" class="price-original">¥{{ item.originalPrice }}</text>
      </view>
      <rich-text
        v-if="richDescription"
        class="item-desc"
        :nodes="richDescription"
        selectable="false"
      ></rich-text>
      <view class="item-footer">
        <text class="item-condition">{{ item.conditionName }}</text>
        <text class="item-location" v-if="item.location">{{ item.location }}</text>
      </view>
      <view class="item-stats">
        <text class="stat-item">
          <text class="stat-icon">👁</text> {{ item.viewCount }}
        </text>
        <text class="stat-item">
          <text class="stat-icon">❤</text> {{ item.likeCount }}
        </text>
      </view>
    </view>
  </view>
</template>

<script>
import { convertToRichText, getDisplayTitle, getDisplayDescription } from '../../utils/highlight.js'

export default {
  name: 'ItemCard',
  props: {
    item: {
      type: Object,
      required: true
    },
    highlightColor: {
      type: String,
      default: '#ff6b35'
    }
  },
  computed: {
    richTitle() {
      const title = getDisplayTitle(this.item)
      return convertToRichText(title, this.highlightColor)
    },
    richDescription() {
      const desc = getDisplayDescription(this.item, 60)
      if (!desc) return ''
      return convertToRichText(desc, this.highlightColor)
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.item)
    }
  }
}
</script>

<style lang="scss" scoped>
.item-card {
  display: flex;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.item-image {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #f5f5f5;
}

.item-info {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.item-title {
  font-size: 30rpx;
  font-weight: 500;
  color: #333333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

.item-price {
  margin-top: 12rpx;
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 24rpx;
  color: #ff6b35;
  font-weight: 600;
}

.price-value {
  font-size: 36rpx;
  color: #ff6b35;
  font-weight: 600;
  margin-left: 4rpx;
}

.price-original {
  font-size: 24rpx;
  color: #999999;
  text-decoration: line-through;
  margin-left: 16rpx;
}

.item-desc {
  font-size: 24rpx;
  color: #666666;
  line-height: 1.5;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

.item-footer {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
  flex-wrap: wrap;
  gap: 16rpx;
}

.item-condition {
  font-size: 22rpx;
  color: #00b578;
  background: rgba(0, 181, 120, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.item-location {
  font-size: 22rpx;
  color: #999999;
}

.item-stats {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 8rpx;
}

.stat-item {
  font-size: 22rpx;
  color: #999999;
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.stat-icon {
  font-size: 20rpx;
}
</style>
