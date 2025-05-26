<template>
  <div class="search-container">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-input-wrapper">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件..."
          clearable
          @keyup.enter="performSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="performSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div class="search-filters">
        <el-select v-model="searchParams.type" placeholder="文件类型" clearable @change="performSearch">
          <el-option label="全部" value="" />
          <el-option label="图片" value="image" />
          <el-option label="文档" value="document" />
          <el-option label="视频" value="video" />
          <el-option label="音频" value="audio" />
          <el-option label="其他" value="other" />
        </el-select>

        <el-popover
          placement="bottom-start"
          :width="300"
          trigger="click"
        >
          <template #reference>
            <el-button>高级搜索 <el-icon><ArrowDown /></el-icon></el-button>
          </template>
          
          <div class="advanced-search-panel">
            <h4>高级搜索</h4>
            
            <div class="search-option">
              <span>时间范围：</span>
              <el-date-picker
                v-model="timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </div>
            
            <div class="search-option">
              <span>文件大小：</span>
              <el-select v-model="sizeFilter" placeholder="文件大小" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option label="小于 1MB" value="lt1m" />
                <el-option label="1MB ~ 10MB" value="1m-10m" />
                <el-option label="10MB ~ 100MB" value="10m-100m" />
                <el-option label="大于 100MB" value="gt100m" />
              </el-select>
            </div>
            
            <div class="search-option">
              <el-checkbox v-model="searchContent">搜索文件内容</el-checkbox>
            </div>
            
            <div class="search-actions">
              <el-button @click="resetAdvancedSearch">重置</el-button>
              <el-button type="primary" @click="performAdvancedSearch">搜索</el-button>
            </div>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results">
      <div class="search-results-header">
        <h3 v-if="totalResults > 0">搜索结果：共找到 {{ totalResults }} 个文件</h3>
        <h3 v-else-if="hasSearched && !loading">未找到匹配的文件</h3>
        
        <div class="search-sort">
          <el-dropdown @command="handleSort">
            <span class="sort-button">
              排序方式：{{ getSortLabel() }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="name_asc">文件名 (A-Z)</el-dropdown-item>
                <el-dropdown-item command="name_desc">文件名 (Z-A)</el-dropdown-item>
                <el-dropdown-item command="time_asc">修改时间 (最早)</el-dropdown-item>
                <el-dropdown-item command="time_desc">修改时间 (最新)</el-dropdown-item>
                <el-dropdown-item command="size_asc">文件大小 (最小)</el-dropdown-item>
                <el-dropdown-item command="size_desc">文件大小 (最大)</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="hasSearched && searchResults.length === 0" description="未找到匹配的文件">
        <template #image>
          <el-icon :size="64"><Search /></el-icon>
        </template>
        <p>尝试使用不同的关键词或清除过滤条件</p>
      </el-empty>

      <!-- 搜索结果列表 -->
      <div v-else-if="searchResults.length > 0" class="results-list">
        <el-table
          :data="searchResults"
          style="width: 100%"
          @row-click="handleFileClick"
        >
          <el-table-column prop="fileName" label="文件名" min-width="260">
            <template #default="{ row }">
              <div class="file-item">
                <el-icon v-if="row.isDir"><Folder /></el-icon>
                <el-icon v-else-if="row.fileType === 'image'"><Picture /></el-icon>
                <el-icon v-else-if="row.fileType === 'video'"><VideoPlay /></el-icon>
                <el-icon v-else-if="row.fileType === 'audio'"><Headset /></el-icon>
                <el-icon v-else><Document /></el-icon>
                <span class="file-name">{{ row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="filePath" label="路径" min-width="220">
            <template #default="{ row }">
              <span class="file-path">{{ row.filePath || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="修改时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="{ row }">
              {{ row.isDir ? '-' : formatSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button 
                  size="small" 
                  type="primary" 
                  text 
                  @click.stop="handleFileAction(row, 'download')"
                  v-if="!row.isDir"
                >
                  <el-icon><Download /></el-icon>
                </el-button>
                <el-dropdown trigger="click" @command="(command) => handleFileAction(row, command)">
                  <el-button size="small" type="primary" text @click.stop>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="share">分享</el-dropdown-item>
                      <el-dropdown-item command="star">收藏</el-dropdown-item>
                      <el-dropdown-item command="view">查看详情</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="searchParams.page"
            v-model:page-size="searchParams.size"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalResults"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-else class="search-history">
        <div class="history-header">
          <h4>最近搜索</h4>
          <el-button link @click="clearHistory" v-if="searchHistory.length > 0">清空</el-button>
        </div>
        <div class="history-list" v-if="searchHistory.length > 0">
          <div 
            v-for="(item, index) in searchHistory" 
            :key="index" 
            class="history-item" 
            @click="applyHistorySearch(item)"
          >
            <el-icon><Timer /></el-icon>
            <span>{{ item }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无搜索历史" :image-size="100" />
      </div>
    </div>

    <!-- 分享对话框 -->
    <share-dialog
      v-model:visible="shareDialogVisible"
      :file-id="selectedFileId"
      :file-info="selectedFile"
      @share-created="handleShareCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { 
  basicSearch, 
  fullTextSearch, 
  advancedSearch, 
  getSearchHistory, 
  clearSearchHistory,
  saveSearchHistory
} from '../api/search';
import { getFileDownloadUrl } from '../api/file';
import type { SearchParams, SearchResult } from '../api/search';
import type { FileItem } from '../api/file';
import ShareDialog from '../components/share/ShareDialog.vue';

const route = useRoute();
const router = useRouter();

// 搜索状态
const searchKeyword = ref('');
const loading = ref(false);
const hasSearched = ref(false);
const searchResults = ref<FileItem[]>([]);
const totalResults = ref(0);
const searchHistory = ref<string[]>([]);

// 高级搜索选项
const timeRange = ref<[string, string] | null>(null);
const sizeFilter = ref('');
const searchContent = ref(false);

// 搜索参数
const searchParams = reactive<SearchParams>({
  keyword: '',
  page: 1,
  size: 20,
  type: '',
  orderBy: 'time',
  orderDirection: 'desc'
});

// 分享对话框状态
const shareDialogVisible = ref(false);
const selectedFileId = ref(0);
const selectedFile = ref<FileItem | null>(null);

// 从URL获取搜索关键词
onMounted(() => {
  const query = route.query.keyword as string;
  if (query) {
    searchKeyword.value = query;
    searchParams.keyword = query;
    performSearch();
  }
  
  // 加载搜索历史
  loadSearchHistory();
});

// 监听URL参数变化
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword && typeof newKeyword === 'string') {
    searchKeyword.value = newKeyword;
    searchParams.keyword = newKeyword;
    performSearch();
  }
});

// 执行基本搜索
const performSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }
  
  searchParams.keyword = searchKeyword.value;
  searchParams.page = 1; // 重置页码
  loading.value = true;
  hasSearched.value = true;
  
  try {
    const result = await basicSearch(searchParams);
    // 兼容后端返回的不同数据结构
    if (Array.isArray(result)) {
      searchResults.value = result;
      totalResults.value = result.length;
    } else if (result && result.list) {
      searchResults.value = result.list;
      totalResults.value = result.list.length;
    } else if (result) {
      searchResults.value = [result];
      totalResults.value = 1;
    } else {
      searchResults.value = [];
      totalResults.value = 0;
    }
    
    // 保存搜索历史
    await saveSearchHistory(searchKeyword.value);
    await loadSearchHistory();
    
    // 更新URL，但不触发路由变化
    updateQueryParams();
  } catch (error) {
    console.error('搜索失败', error);
    ElMessage.error('搜索失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 执行高级搜索
const performAdvancedSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }
  
  searchParams.keyword = searchKeyword.value;
  searchParams.page = 1; // 重置页码
  
  // 设置时间范围
  if (timeRange.value) {
    searchParams.timeRange = {
      start: timeRange.value[0],
      end: timeRange.value[1]
    };
  } else {
    searchParams.timeRange = undefined;
  }
  
  // 配置参数
  const advancedParams: any = { ...searchParams };
  
  // 文件大小过滤
  if (sizeFilter.value) {
    switch (sizeFilter.value) {
      case 'lt1m':
        advancedParams.sizeRange = { max: 1 * 1024 * 1024 };
        break;
      case '1m-10m':
        advancedParams.sizeRange = { min: 1 * 1024 * 1024, max: 10 * 1024 * 1024 };
        break;
      case '10m-100m':
        advancedParams.sizeRange = { min: 10 * 1024 * 1024, max: 100 * 1024 * 1024 };
        break;
      case 'gt100m':
        advancedParams.sizeRange = { min: 100 * 1024 * 1024 };
        break;
    }
  }
  
  loading.value = true;
  hasSearched.value = true;
  
  try {
    let result;
    
    // 根据是否需要全文检索决定使用哪个API
    if (searchContent.value) {
      result = await fullTextSearch(searchParams);
    } else {
      result = await advancedSearch(advancedParams);
    }
    
    // 兼容后端返回的不同数据结构
    if (Array.isArray(result)) {
      searchResults.value = result;
      totalResults.value = result.length;
    } else if (result && result.list) {
      searchResults.value = result.list;
      totalResults.value = result.list.length;
    } else if (result) {
      searchResults.value = [result];
      totalResults.value = 1;
    } else {
      searchResults.value = [];
      totalResults.value = 0;
    }
    
    // 更新URL，但不触发路由变化
    updateQueryParams();
  } catch (error) {
    console.error('高级搜索失败', error);
    ElMessage.error('搜索失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 重置高级搜索选项
const resetAdvancedSearch = () => {
  timeRange.value = null;
  sizeFilter.value = '';
  searchContent.value = false;
  searchParams.type = '';
};

// 加载搜索历史
const loadSearchHistory = async () => {
  try {
    const history = await getSearchHistory();
    searchHistory.value = history || [];
  } catch (error) {
    console.error('加载搜索历史失败', error);
  }
};

// 清空搜索历史
const clearHistory = async () => {
  try {
    await clearSearchHistory();
    searchHistory.value = [];
    ElMessage.success('已清空搜索历史');
  } catch (error) {
    console.error('清空搜索历史失败', error);
    ElMessage.error('清空搜索历史失败');
  }
};

// 应用历史搜索
const applyHistorySearch = (keyword: string) => {
  searchKeyword.value = keyword;
  searchParams.keyword = keyword;
  performSearch();
};

// 处理排序
const handleSort = (command: string) => {
  const [key, order] = command.split('_');
  searchParams.orderBy = key;
  searchParams.orderDirection = order as 'asc' | 'desc';
  performSearch();
};

// 获取排序方式显示标签
const getSortLabel = () => {
  const { orderBy, orderDirection } = searchParams;
  
  if (orderBy === 'name') {
    return `文件名 ${orderDirection === 'asc' ? '(A-Z)' : '(Z-A)'}`;
  } else if (orderBy === 'time') {
    return `修改时间 ${orderDirection === 'asc' ? '(最早)' : '(最新)'}`;
  } else if (orderBy === 'size') {
    return `文件大小 ${orderDirection === 'asc' ? '(最小)' : '(最大)'}`;
  }
  
  return '默认排序';
};

// 处理页面大小变化
const handleSizeChange = (val: number) => {
  searchParams.size = val;
  performSearch();
};

// 处理页码变化
const handleCurrentChange = (val: number) => {
  searchParams.page = val;
  performSearch();
};

// 更新URL查询参数
const updateQueryParams = () => {
  const query = { ...route.query, keyword: searchKeyword.value };
  router.replace({ query });
};

// 处理文件点击
const handleFileClick = (file: FileItem) => {
  if (file.isDir) {
    // 如果是文件夹，跳转到文件夹内部
    router.push(`/folder/${file.id}`);
  } else {
    // 如果是文件，查看文件详情
    router.push(`/detail/${file.id}`);
  }
};

// 处理文件操作
const handleFileAction = async (file: FileItem, action: string) => {
  switch (action) {
    case 'download':
      try {
        const url = await getFileDownloadUrl(file.id);
        const a = document.createElement('a');
        a.href = url as unknown as string;
        a.download = file.fileName;
        a.click();
      } catch (error) {
        console.error('下载失败', error);
        ElMessage.error('下载失败');
      }
      break;
    case 'share':
      selectedFileId.value = file.id;
      selectedFile.value = file;
      shareDialogVisible.value = true;
      break;
    case 'star':
      // TODO: 收藏功能实现
      ElMessage.info('收藏功能即将上线');
      break;
    case 'view':
      router.push(`/detail/${file.id}`);
      break;
    default:
      break;
  }
};

// 处理分享创建成功
const handleShareCreated = (shareResult: any) => {
  ElMessage.success(`分享创建成功，ID: ${shareResult.id}`);
};

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};
</script>

<style scoped>
.search-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.search-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-input-wrapper {
  flex: 1;
  min-width: 300px;
}

.search-filters {
  display: flex;
  gap: 10px;
}

.advanced-search-panel {
  padding: 10px 0;
}

.advanced-search-panel h4 {
  margin-top: 0;
  margin-bottom: 16px;
}

.search-option {
  margin-bottom: 16px;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.search-results {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.search-results-header h3 {
  margin: 0;
}

.sort-button {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}

.loading-container {
  padding: 20px;
}

.results-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-path {
  color: #909399;
  font-size: 13px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.search-history {
  padding: 20px 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-header h4 {
  margin: 0;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.history-item:hover {
  background-color: #e4e7ed;
}
</style> 