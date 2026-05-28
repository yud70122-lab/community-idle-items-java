<template>
  <view class="search-page">
    <view class="search-header">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索闲置物品"
          confirm-type="search"
          @confirm="handleSearch"
          @input="handleInput"
        />
        <text v-if="keyword" class="clear-icon" @click="clearKeyword">✕</text>
      </view>
      <text class="cancel-btn" @click="goBack">取消</text>
    </view>

    <view class="search-content">
      <view v-if="!keyword && !hasSearched" class="search-history">
        <view class="history-header">
          <text class="history-title">搜索历史</text>
          <text class="clear-history" @click="clearHistory">清空</text>
        </view>
        <view class="history-tags">
          <view
            v-for="(item, index) in searchHistory"
            :key="index"
            class="history-tag"
            @click="searchByHistory(item)"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <view v-if="hasSearched" class="search-result">
        <view class="result-header">
          <text class="result-count">共 {{ total }} 件商品</text>
          <view class="sort-tabs">
            <text
              v-for="tab in sortTabs"
              :key="tab.value"
              :class="['sort-tab', { active: sortBy === tab.value }]"
              @click="changeSort(tab.value)"
            >
              {{ tab.label }}
            </text>
          </view>
        </view>

        <view class="item-list">
          <ItemCard
            v-for="item in itemList"
            :key="item.id"
            :item="item"
            :highlight-color="highlightColor"
            @click="goToDetail(item)"
          />
        </view>

        <view v-if="loading" class="loading-more">
          <text>加载中...</text>
        </view>

        <view v-if="!loading && hasMore && itemList.length > 0" class="load-more" @click="loadMore">
          <text>加载更多</text>
        </view>

        <view v-if="!loading && !hasMore && itemList.length > 0" class="no-more">
          <text>没有更多了</text>
        </view>

        <view v-if="!loading && itemList.length === 0" class="empty-state">
          <text class="empty-icon">📭</text>
          <text class="empty-text">没有找到相关商品</text>
          <text class="empty-hint">换个关键词试试吧</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import ItemCard from '../../components/ItemCard/index.vue'

export default {
  components: {
    ItemCard
  },
  data() {
    return {
      keyword: '',
      itemList: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      hasMore: true,
      loading: false,
      hasSearched: false,
      sortBy: 'match',
      highlightColor: '#ff6b35',
      searchHistory: [],
      sortTabs: [
        { label: '智能', value: 'match' },
        { label